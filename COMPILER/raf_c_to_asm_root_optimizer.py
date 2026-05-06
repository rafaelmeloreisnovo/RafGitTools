#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
RAFAELIA ROOT C→ASM OPTIMIZER
=============================

Versão com RAIZ: parser explícito, AST mínimo, IR auditável, grafo corrigido,
scheduler por bloco básico, branchless real para if/else simples, e backends
separados para x86-64 e ARM64/AArch64.

Escopo honesto:
- NÃO é compilador C completo.
- É um otimizador/conversor para um subconjunto seguro de C-like:
  atribuições simples, expressões binárias, if/else com comparação simples.
- Preserva rastreio de origem por linha, hashes e relatório auditável.

Exemplo aceito:

    x = 10;
    y = x + 5;
    if (y < 20) {
        z = y + 10;
    } else {
        z = y - 5;
    }
    w = z + x;

Uso:
    python raf_c_to_asm_root_optimizer.py --target arm64 input.c
    python raf_c_to_asm_root_optimizer.py --target x86_64 input.c --audit audit.json
    cat input.c | python raf_c_to_asm_root_optimizer.py --target arm64

Licença:
    Preserve a licença do código de entrada. Este arquivo é uma base técnica
    experimental e auditável.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import defaultdict, deque
from dataclasses import dataclass, field, asdict
from enum import Enum
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Sequence, Set, Tuple, Union


# =============================================================================
# ERROS
# =============================================================================

class CompileError(Exception):
    """Erro determinístico de parser/IR/backend."""
    pass


# =============================================================================
# AST — RAIZ SEMÂNTICA
# =============================================================================

@dataclass(frozen=True)
class SourceLoc:
    line: int
    text: str


@dataclass(frozen=True)
class Term:
    value: str
    is_const: bool = False


@dataclass(frozen=True)
class Expr:
    left: Term
    op: Optional[str] = None
    right: Optional[Term] = None


@dataclass(frozen=True)
class Cond:
    left: Term
    op: str
    right: Term


@dataclass(frozen=True)
class Assign:
    dst: str
    expr: Expr
    loc: SourceLoc


@dataclass(frozen=True)
class IfElse:
    cond: Cond
    then_body: Tuple["Stmt", ...]
    else_body: Tuple["Stmt", ...]
    loc: SourceLoc


Stmt = Union[Assign, IfElse]


# =============================================================================
# PARSER C-LIKE — PEQUENO, EXPLÍCITO, SEM FINGIR C COMPLETO
# =============================================================================

class MiniCParser:
    _name = r"[A-Za-z_][A-Za-z0-9_]*"
    _int = r"-?(?:0x[0-9A-Fa-f]+|\d+)"

    def __init__(self, code: str):
        self.original_code = code
        self.lines: List[Tuple[int, str]] = []
        self.pos = 0
        self._prepare_lines(code)

    @staticmethod
    def _strip_comments(code: str) -> str:
        # Remove // e /* */ de forma simples, suficiente para o subconjunto.
        code = re.sub(r"/\*.*?\*/", "", code, flags=re.S)
        code = re.sub(r"//.*", "", code)
        return code

    def _prepare_lines(self, code: str) -> None:
        code = self._strip_comments(code)
        prepared: List[Tuple[int, str]] = []

        for line_no, raw in enumerate(code.splitlines(), 1):
            # Mantém origem aproximada por linha.
            expanded = (
                raw.replace("{", "\n{\n")
                   .replace("}", "\n}\n")
                   .replace(";", ";\n")
            )
            for part in expanded.splitlines():
                text = part.strip()
                if text:
                    prepared.append((line_no, text))
        self.lines = prepared

    def parse(self) -> List[Stmt]:
        stmts = self._parse_block(stop_on_closing=False)
        if self.pos != len(self.lines):
            line_no, text = self.lines[self.pos]
            raise CompileError(f"Linha {line_no}: token inesperado: {text!r}")
        return stmts

    def _peek(self) -> Optional[Tuple[int, str]]:
        return self.lines[self.pos] if self.pos < len(self.lines) else None

    def _pop(self) -> Tuple[int, str]:
        if self.pos >= len(self.lines):
            raise CompileError("Fim inesperado do arquivo")
        item = self.lines[self.pos]
        self.pos += 1
        return item

    def _parse_block(self, stop_on_closing: bool) -> List[Stmt]:
        out: List[Stmt] = []
        while self.pos < len(self.lines):
            line_no, text = self._peek()  # type: ignore[misc]
            if text == "}":
                if stop_on_closing:
                    self._pop()
                    return out
                raise CompileError(f"Linha {line_no}: '}}' sem '{{' correspondente")

            if text.startswith("if"):
                out.append(self._parse_if())
                continue

            out.append(self._parse_assign())
        if stop_on_closing:
            raise CompileError("Bloco aberto sem '}' de fechamento")
        return out

    def _parse_if(self) -> IfElse:
        line_no, text = self._pop()
        m = re.match(rf"if\s*\(\s*({self._name}|{self._int})\s*(==|!=|<=|>=|<|>)\s*({self._name}|{self._int})\s*\)\s*$", text)
        if not m:
            raise CompileError(f"Linha {line_no}: if fora do subconjunto suportado: {text!r}")

        cond = Cond(self._term(m.group(1)), m.group(2), self._term(m.group(3)))
        self._expect("{", after=f"if da linha {line_no}")
        then_body = tuple(self._parse_block(stop_on_closing=True))

        else_body: Tuple[Stmt, ...] = ()
        nxt = self._peek()
        if nxt and nxt[1] == "else":
            self._pop()
            self._expect("{", after=f"else da linha {line_no}")
            else_body = tuple(self._parse_block(stop_on_closing=True))

        return IfElse(cond=cond, then_body=then_body, else_body=else_body, loc=SourceLoc(line_no, text))

    def _parse_assign(self) -> Assign:
        line_no, text = self._pop()
        if text.endswith(";"):
            text = text[:-1].strip()

        m = re.match(rf"^({self._name})\s*=\s*(.+?)\s*$", text)
        if not m:
            raise CompileError(f"Linha {line_no}: statement fora do subconjunto suportado: {text!r}")

        dst, expr_text = m.group(1), m.group(2)
        return Assign(dst=dst, expr=self._parse_expr(expr_text, line_no), loc=SourceLoc(line_no, text))

    def _expect(self, expected: str, after: str = "") -> None:
        line_no, text = self._pop()
        if text != expected:
            suffix = f" após {after}" if after else ""
            raise CompileError(f"Linha {line_no}: esperado {expected!r}{suffix}, recebido {text!r}")

    def _term(self, token: str) -> Term:
        token = token.strip()
        if re.fullmatch(self._int, token):
            return Term(token, is_const=True)
        if re.fullmatch(self._name, token):
            return Term(token, is_const=False)
        raise CompileError(f"Termo inválido: {token!r}")

    def _parse_expr(self, text: str, line_no: int) -> Expr:
        text = text.strip()
        # Operadores em ordem para capturar shifts antes de < >
        ops = ["<<", ">>", "+", "-", "*", "&", "^", "|"]
        for op in ops:
            # Não tratar '-' inicial de constante como operador.
            idx = self._find_top_level_op(text, op)
            if idx is not None:
                left = text[:idx].strip()
                right = text[idx + len(op):].strip()
                if not left or not right:
                    raise CompileError(f"Linha {line_no}: expressão inválida: {text!r}")
                return Expr(self._term(left), op, self._term(right))
        return Expr(self._term(text))

    @staticmethod
    def _find_top_level_op(text: str, op: str) -> Optional[int]:
        start = 1 if text.startswith("-") else 0
        i = start
        while i <= len(text) - len(op):
            if text[i:i+len(op)] == op:
                return i
            i += 1
        return None


# =============================================================================
# IR — INSTRUÇÕES VIRTUAIS AUDITÁVEIS
# =============================================================================

class IROp(Enum):
    MOV = "MOV"
    BIN = "BIN"
    CMP = "CMP"
    CSEL = "CSEL"
    LABEL = "LABEL"
    JMP = "JMP"
    CJMP = "CJMP"
    RET = "RET"


@dataclass
class IRInstr:
    uid: int
    op: IROp
    mnemonic: str
    dst: Optional[str] = None
    src1: Optional[str] = None
    src2: Optional[str] = None
    imm: Optional[int] = None
    cond: Optional[str] = None
    label: Optional[str] = None
    true_label: Optional[str] = None
    false_label: Optional[str] = None
    cycles: int = 1
    defines: Set[str] = field(default_factory=set)
    uses: Set[str] = field(default_factory=set)
    source_line: int = 0
    source_text: str = ""
    origin_hash: str = ""

    def barrier(self) -> bool:
        return self.op in {IROp.CMP, IROp.CSEL, IROp.LABEL, IROp.JMP, IROp.CJMP, IROp.RET}


class IRBuilder:
    def __init__(self, branchless: bool = True):
        self.uid = 0
        self.label_id = 0
        self.ir: List[IRInstr] = []
        self.branchless = branchless

    def build(self, stmts: Sequence[Stmt]) -> List[IRInstr]:
        for stmt in stmts:
            self._emit_stmt(stmt)
        return self.ir

    def _next_uid(self) -> int:
        self.uid += 1
        return self.uid

    def _new_label(self, prefix: str) -> str:
        self.label_id += 1
        return f".L_{prefix}_{self.label_id}"

    def _hash_origin(self, loc: SourceLoc, kind: str) -> str:
        payload = f"{loc.line}:{kind}:{loc.text}".encode()
        return hashlib.sha256(payload).hexdigest()[:16]

    def _term_uses(self, term: Term) -> Set[str]:
        return set() if term.is_const else {term.value}

    def _term_operand(self, term: Term) -> Tuple[Optional[str], Optional[int]]:
        if term.is_const:
            return None, int(term.value, 0)
        return term.value, None

    def _emit_stmt(self, stmt: Stmt) -> None:
        if isinstance(stmt, Assign):
            self._emit_assign(stmt)
        elif isinstance(stmt, IfElse):
            self._emit_if(stmt)
        else:
            raise CompileError(f"AST desconhecida: {stmt!r}")

    def _emit_assign(self, node: Assign) -> None:
        expr = node.expr
        if expr.op is None:
            src, imm = self._term_operand(expr.left)
            self.ir.append(IRInstr(
                uid=self._next_uid(), op=IROp.MOV, mnemonic="mov",
                dst=node.dst, src1=src, imm=imm,
                defines={node.dst}, uses=self._term_uses(expr.left),
                source_line=node.loc.line, source_text=node.loc.text,
                origin_hash=self._hash_origin(node.loc, "assign.mov")
            ))
            return

        if expr.right is None:
            raise CompileError(f"Linha {node.loc.line}: expressão binária sem lado direito")

        src1, imm1 = self._term_operand(expr.left)
        src2, imm2 = self._term_operand(expr.right)

        # Primeiro move src1/imm1 para dst, depois aplica operação.
        self.ir.append(IRInstr(
            uid=self._next_uid(), op=IROp.MOV, mnemonic="mov",
            dst=node.dst, src1=src1, imm=imm1,
            defines={node.dst}, uses=self._term_uses(expr.left),
            source_line=node.loc.line, source_text=node.loc.text,
            origin_hash=self._hash_origin(node.loc, "assign.seed")
        ))
        self.ir.append(IRInstr(
            uid=self._next_uid(), op=IROp.BIN, mnemonic=self._op_to_mnemonic(expr.op),
            dst=node.dst, src1=node.dst, src2=src2, imm=imm2,
            defines={node.dst}, uses={node.dst} | self._term_uses(expr.right),
            source_line=node.loc.line, source_text=node.loc.text,
            origin_hash=self._hash_origin(node.loc, f"assign.{expr.op}")
        ))

    def _emit_if(self, node: IfElse) -> None:
        if self.branchless and self._can_branchless(node):
            self._emit_branchless_if(node)
            return

        then_label = self._new_label("then")
        else_label = self._new_label("else")
        end_label = self._new_label("endif")
        self._emit_cmp(node.cond, node.loc)
        self.ir.append(IRInstr(
            uid=self._next_uid(), op=IROp.CJMP, mnemonic="cjmp",
            cond=node.cond.op, true_label=then_label, false_label=else_label,
            source_line=node.loc.line, source_text=node.loc.text,
            origin_hash=self._hash_origin(node.loc, "if.cjmp")
        ))
        self._emit_label(then_label, node.loc)
        for s in node.then_body:
            self._emit_stmt(s)
        self.ir.append(IRInstr(uid=self._next_uid(), op=IROp.JMP, mnemonic="jmp", label=end_label,
                               source_line=node.loc.line, source_text=node.loc.text,
                               origin_hash=self._hash_origin(node.loc, "if.jmp_end")))
        self._emit_label(else_label, node.loc)
        for s in node.else_body:
            self._emit_stmt(s)
        self._emit_label(end_label, node.loc)

    def _can_branchless(self, node: IfElse) -> bool:
        # Branchless seguro apenas para:
        # if (cond) { dst = expr; } else { dst = expr; }
        return (
            len(node.then_body) == 1 and
            len(node.else_body) == 1 and
            isinstance(node.then_body[0], Assign) and
            isinstance(node.else_body[0], Assign) and
            node.then_body[0].dst == node.else_body[0].dst
        )

    def _emit_branchless_if(self, node: IfElse) -> None:
        then_a = node.then_body[0]
        else_a = node.else_body[0]
        assert isinstance(then_a, Assign) and isinstance(else_a, Assign)

        final_dst = then_a.dst
        then_tmp = f"__then_{final_dst}_{self.label_id + 1}"
        else_tmp = f"__else_{final_dst}_{self.label_id + 1}"

        # Gera as duas expressões em temporários, compara e seleciona.
        self._emit_assign(Assign(then_tmp, then_a.expr, then_a.loc))
        self._emit_assign(Assign(else_tmp, else_a.expr, else_a.loc))
        self._emit_cmp(node.cond, node.loc)

        self.ir.append(IRInstr(
            uid=self._next_uid(), op=IROp.CSEL, mnemonic="csel",
            dst=final_dst, src1=then_tmp, src2=else_tmp,
            cond=node.cond.op,
            defines={final_dst},
            uses={then_tmp, else_tmp} | self._term_uses(node.cond.left) | self._term_uses(node.cond.right),
            source_line=node.loc.line, source_text=node.loc.text,
            origin_hash=self._hash_origin(node.loc, "if.branchless.csel")
        ))

    def _emit_cmp(self, cond: Cond, loc: SourceLoc) -> None:
        lhs, imm1 = self._term_operand(cond.left)
        rhs, imm2 = self._term_operand(cond.right)
        if imm1 is not None:
            # Para manter simplicidade de backend, materializa constante esquerda.
            lhs_name = f"__cmp_lhs_{self.uid + 1}"
            self.ir.append(IRInstr(
                uid=self._next_uid(), op=IROp.MOV, mnemonic="mov",
                dst=lhs_name, imm=imm1, defines={lhs_name},
                source_line=loc.line, source_text=loc.text,
                origin_hash=self._hash_origin(loc, "cmp.lhs_const")
            ))
            lhs = lhs_name

        self.ir.append(IRInstr(
            uid=self._next_uid(), op=IROp.CMP, mnemonic="cmp",
            src1=lhs, src2=rhs, imm=imm2, cond=cond.op,
            uses=(set() if lhs is None else {lhs}) | (set() if rhs is None else {rhs}),
            source_line=loc.line, source_text=loc.text,
            origin_hash=self._hash_origin(loc, "cmp")
        ))

    def _emit_label(self, label: str, loc: SourceLoc) -> None:
        self.ir.append(IRInstr(uid=self._next_uid(), op=IROp.LABEL, mnemonic="label", label=label,
                               source_line=loc.line, source_text=loc.text,
                               origin_hash=self._hash_origin(loc, f"label.{label}")))

    @staticmethod
    def _op_to_mnemonic(op: str) -> str:
        table = {
            "+": "add", "-": "sub", "*": "mul",
            "&": "and", "|": "or", "^": "xor",
            "<<": "shl", ">>": "shr",
        }
        if op not in table:
            raise CompileError(f"Operador não suportado: {op}")
        return table[op]


# =============================================================================
# GRAFO E SCHEDULER — CORRIGIDO, POR BLOCO BÁSICO
# =============================================================================

class DependencyGraph:
    def __init__(self, instructions: Sequence[IRInstr]):
        self.instructions: Dict[int, IRInstr] = {i.uid: i for i in instructions}
        self.adj: Dict[int, Set[int]] = defaultdict(set)
        self.in_degree: Dict[int, int] = {i.uid: 0 for i in instructions}
        self._build()

    def _add_edge(self, a: int, b: int) -> None:
        if a == b or b in self.adj[a]:
            return
        self.adj[a].add(b)
        self.in_degree[b] = self.in_degree.get(b, 0) + 1

    def _build(self) -> None:
        ordered = list(self.instructions.values())

        for i, a in enumerate(ordered):
            for b in ordered[i + 1:]:
                # RAW: a define, b usa => a antes de b
                if a.defines & b.uses:
                    self._add_edge(a.uid, b.uid)

                # WAW: ambos definem mesmo destino => preservar ordem
                if a.defines & b.defines:
                    self._add_edge(a.uid, b.uid)

                # WAR: a usa, b define => preservar ordem neste MVP
                if a.uses & b.defines:
                    self._add_edge(a.uid, b.uid)

    def schedule(self) -> List[IRInstr]:
        in_deg = {uid: self.in_degree.get(uid, 0) for uid in self.instructions}
        ready = deque(sorted(uid for uid in self.instructions if in_deg[uid] == 0))
        result: List[IRInstr] = []

        while ready:
            # Prioriza maior fan-out, depois uid menor para determinismo.
            uid = max(ready, key=lambda u: (len(self.adj.get(u, set())), -u))
            ready.remove(uid)
            result.append(self.instructions[uid])

            for nxt in sorted(self.adj.get(uid, set())):
                in_deg[nxt] -= 1
                if in_deg[nxt] == 0:
                    ready.append(nxt)

        if len(result) != len(self.instructions):
            raise CompileError("Grafo de dependências contém ciclo inesperado")
        return result


class BasicBlockScheduler:
    @staticmethod
    def schedule(ir: Sequence[IRInstr]) -> Tuple[List[IRInstr], int]:
        out: List[IRInstr] = []
        block: List[IRInstr] = []
        moved = 0

        def flush() -> None:
            nonlocal block, moved
            if not block:
                return
            before = [i.uid for i in block]
            after_instr = DependencyGraph(block).schedule()
            after = [i.uid for i in after_instr]
            if before != after:
                moved += sum(1 for a, b in zip(before, after) if a != b)
            out.extend(after_instr)
            block = []

        for inst in ir:
            if inst.barrier():
                flush()
                out.append(inst)
            else:
                block.append(inst)
        flush()
        return out, moved


# =============================================================================
# REGISTROS VIRTUAIS → REGISTROS FÍSICOS
# =============================================================================

class RegisterAllocator:
    def __init__(self, target: str):
        self.target = target
        if target == "x86_64":
            self.reg_pool = ["rax", "rcx", "rdx", "rsi", "rdi", "r8", "r9", "r10", "r11", "r12", "r13", "r14", "r15"]
        elif target == "arm64":
            self.reg_pool = ["x9", "x10", "x11", "x12", "x13", "x14", "x15", "x16", "x17", "x19", "x20", "x21", "x22"]
        else:
            raise CompileError(f"Target inválido: {target}")
        self.map: Dict[str, str] = {}

    def reg(self, name: str) -> str:
        if name not in self.map:
            if len(self.map) >= len(self.reg_pool):
                # Spill explícito não implementado: melhor erro honesto que ASM errado.
                raise CompileError("Registradores esgotados: spill ainda não implementado neste MVP")
            self.map[name] = self.reg_pool[len(self.map)]
        return self.map[name]


# =============================================================================
# BACKENDS
# =============================================================================

class Backend:
    def generate(self, ir: Sequence[IRInstr], regalloc: RegisterAllocator) -> str:
        raise NotImplementedError


class X86_64Backend(Backend):
    cond_map = {
        "==": "e", "!=": "ne", "<": "l", "<=": "le", ">": "g", ">=": "ge"
    }

    def generate(self, ir: Sequence[IRInstr], regalloc: RegisterAllocator) -> str:
        lines = [
            "; RAFAELIA ROOT C→ASM — x86-64 NASM/Intel",
            "section .text",
            "global optimized_function",
            "optimized_function:",
            "    push rbp",
            "    mov rbp, rsp",
        ]
        for inst in ir:
            lines.extend(self._emit(inst, regalloc))
        lines.extend(["    pop rbp", "    ret"])
        return "\n".join(lines)

    def _op(self, value: Optional[str], imm: Optional[int], ra: RegisterAllocator) -> str:
        if imm is not None:
            return str(imm)
        if value is None:
            raise CompileError("Operando ausente")
        return ra.reg(value)

    def _emit(self, inst: IRInstr, ra: RegisterAllocator) -> List[str]:
        c = f" ; uid={inst.uid} line={inst.source_line} h={inst.origin_hash}"
        if inst.op == IROp.MOV:
            return [f"    mov {ra.reg(inst.dst or '')}, {self._op(inst.src1, inst.imm, ra)}{c}"]
        if inst.op == IROp.BIN:
            dst = ra.reg(inst.dst or "")
            op2 = self._op(inst.src2, inst.imm, ra)
            m = {"mul": "imul", "shl": "shl", "shr": "shr"}.get(inst.mnemonic, inst.mnemonic)
            return [f"    {m} {dst}, {op2}{c}"]
        if inst.op == IROp.CMP:
            return [f"    cmp {ra.reg(inst.src1 or '')}, {self._op(inst.src2, inst.imm, ra)}{c}"]
        if inst.op == IROp.CSEL:
            suffix = self.cond_map[inst.cond or "=="]
            dst = ra.reg(inst.dst or "")
            then_r = ra.reg(inst.src1 or "")
            else_r = ra.reg(inst.src2 or "")
            return [
                f"    mov {dst}, {else_r}{c}",
                f"    cmov{suffix} {dst}, {then_r}{c}",
            ]
        if inst.op == IROp.CJMP:
            suffix = self.cond_map[inst.cond or "=="]
            inv = self._invert_suffix(suffix)
            return [f"    j{suffix} {inst.true_label}{c}", f"    jmp {inst.false_label}{c}"]
        if inst.op == IROp.JMP:
            return [f"    jmp {inst.label}{c}"]
        if inst.op == IROp.LABEL:
            return [f"{inst.label}:"]
        return [f"    ; unsupported {inst.op.value}{c}"]

    @staticmethod
    def _invert_suffix(s: str) -> str:
        return {"e": "ne", "ne": "e", "l": "ge", "le": "g", "g": "le", "ge": "l"}[s]


class ARM64Backend(Backend):
    # AArch64 condition codes for signed integer comparison after cmp.
    cond_map = {
        "==": "eq", "!=": "ne", "<": "lt", "<=": "le", ">": "gt", ">=": "ge"
    }

    def generate(self, ir: Sequence[IRInstr], regalloc: RegisterAllocator) -> str:
        lines = [
            "// RAFAELIA ROOT C→ASM — ARM64/AArch64 GNU as",
            ".text",
            ".global optimized_function",
            ".type optimized_function, %function",
            "optimized_function:",
        ]
        for inst in ir:
            lines.extend(self._emit(inst, regalloc))
        lines.extend(["    ret", ".size optimized_function, .-optimized_function"])
        return "\n".join(lines)

    def _imm_or_reg(self, value: Optional[str], imm: Optional[int], ra: RegisterAllocator) -> Tuple[bool, str]:
        if imm is not None:
            if -4095 <= imm <= 4095:
                return True, f"#{imm}"
            tmp = "x8"
            return False, tmp
        if value is None:
            raise CompileError("Operando ausente")
        return False, ra.reg(value)

    def _emit_load_large_imm(self, imm: int, reg: str = "x8") -> List[str]:
        # Simples e correto para 64 bits assinados via literal pseudo-instruction.
        return [f"    ldr {reg}, ={imm}"]

    def _emit(self, inst: IRInstr, ra: RegisterAllocator) -> List[str]:
        c = f" // uid={inst.uid} line={inst.source_line} h={inst.origin_hash}"

        if inst.op == IROp.MOV:
            dst = ra.reg(inst.dst or "")
            if inst.imm is not None:
                if -65535 <= inst.imm <= 65535:
                    return [f"    mov {dst}, #{inst.imm}{c}"]
                return self._emit_load_large_imm(inst.imm, dst) + [f"    // origin{c}"]
            return [f"    mov {dst}, {ra.reg(inst.src1 or '')}{c}"]

        if inst.op == IROp.BIN:
            dst = ra.reg(inst.dst or "")
            op = {
                "add": "add", "sub": "sub", "mul": "mul",
                "and": "and", "or": "orr", "xor": "eor",
                "shl": "lsl", "shr": "lsr",
            }[inst.mnemonic]

            if inst.imm is not None:
                if op == "mul":
                    lines = self._emit_load_large_imm(inst.imm, "x8") if not (-65535 <= inst.imm <= 65535) else [f"    mov x8, #{inst.imm}"]
                    return lines + [f"    mul {dst}, {dst}, x8{c}"]
                if op in {"lsl", "lsr"}:
                    return [f"    {op} {dst}, {dst}, #{inst.imm}{c}"]
                if -4095 <= inst.imm <= 4095:
                    return [f"    {op} {dst}, {dst}, #{inst.imm}{c}"]
                return self._emit_load_large_imm(inst.imm, "x8") + [f"    {op} {dst}, {dst}, x8{c}"]

            return [f"    {op} {dst}, {dst}, {ra.reg(inst.src2 or '')}{c}"]

        if inst.op == IROp.CMP:
            lhs = ra.reg(inst.src1 or "")
            if inst.imm is not None:
                if -4095 <= inst.imm <= 4095:
                    return [f"    cmp {lhs}, #{inst.imm}{c}"]
                return self._emit_load_large_imm(inst.imm, "x8") + [f"    cmp {lhs}, x8{c}"]
            return [f"    cmp {lhs}, {ra.reg(inst.src2 or '')}{c}"]

        if inst.op == IROp.CSEL:
            cond = self.cond_map[inst.cond or "=="]
            dst = ra.reg(inst.dst or "")
            then_r = ra.reg(inst.src1 or "")
            else_r = ra.reg(inst.src2 or "")
            return [f"    csel {dst}, {then_r}, {else_r}, {cond}{c}"]

        if inst.op == IROp.CJMP:
            cond = self.cond_map[inst.cond or "=="]
            return [f"    b.{cond} {inst.true_label}{c}", f"    b {inst.false_label}{c}"]

        if inst.op == IROp.JMP:
            return [f"    b {inst.label}{c}"]

        if inst.op == IROp.LABEL:
            return [f"{inst.label}:"]

        return [f"    // unsupported {inst.op.value}{c}"]


# =============================================================================
# AUDITORIA
# =============================================================================

@dataclass
class AuditReport:
    target: str
    original_hash: str
    ast_nodes: int
    ir_before_schedule: int
    ir_after_schedule: int
    scheduled_moves: int
    branchless_enabled: bool
    branchless_selects: int
    register_map: Dict[str, str]
    instruction_trace: List[Dict[str, Union[int, str, List[str]]]]
    result_hash: str
    audit_hash: str


class Auditor:
    @staticmethod
    def sha(text: str) -> str:
        return hashlib.sha256(text.encode("utf-8")).hexdigest()

    @classmethod
    def build_report(
        cls,
        *,
        target: str,
        original_code: str,
        ast: Sequence[Stmt],
        ir_before: Sequence[IRInstr],
        ir_after: Sequence[IRInstr],
        moved: int,
        branchless_enabled: bool,
        asm: str,
        register_map: Dict[str, str],
    ) -> AuditReport:
        trace = []
        for inst in ir_after:
            trace.append({
                "uid": inst.uid,
                "op": inst.op.value,
                "mnemonic": inst.mnemonic,
                "defines": sorted(inst.defines),
                "uses": sorted(inst.uses),
                "source_line": inst.source_line,
                "source_text": inst.source_text,
                "origin_hash": inst.origin_hash,
            })

        result_hash = cls.sha(asm)
        payload = {
            "target": target,
            "original_hash": cls.sha(original_code),
            "result_hash": result_hash,
            "trace": trace,
            "register_map": register_map,
        }
        audit_hash = hashlib.sha256(json.dumps(payload, sort_keys=True).encode()).hexdigest()
        return AuditReport(
            target=target,
            original_hash=cls.sha(original_code),
            ast_nodes=len(ast),
            ir_before_schedule=len(ir_before),
            ir_after_schedule=len(ir_after),
            scheduled_moves=moved,
            branchless_enabled=branchless_enabled,
            branchless_selects=sum(1 for i in ir_after if i.op == IROp.CSEL),
            register_map=dict(register_map),
            instruction_trace=trace,
            result_hash=result_hash,
            audit_hash=audit_hash,
        )


# =============================================================================
# ORQUESTRADOR
# =============================================================================

class RafaeliaRootOptimizer:
    def __init__(self, code: str, target: str = "arm64", branchless: bool = True, schedule: bool = True):
        if target not in {"arm64", "x86_64"}:
            raise CompileError("Target deve ser 'arm64' ou 'x86_64'")
        self.code = code
        self.target = target
        self.branchless = branchless
        self.schedule = schedule

    def compile(self) -> Tuple[str, AuditReport]:
        ast = MiniCParser(self.code).parse()
        ir_before = IRBuilder(branchless=self.branchless).build(ast)
        if self.schedule:
            ir_after, moved = BasicBlockScheduler.schedule(ir_before)
        else:
            ir_after, moved = list(ir_before), 0

        regalloc = RegisterAllocator(self.target)
        backend: Backend = ARM64Backend() if self.target == "arm64" else X86_64Backend()
        asm = backend.generate(ir_after, regalloc)

        report = Auditor.build_report(
            target=self.target,
            original_code=self.code,
            ast=ast,
            ir_before=ir_before,
            ir_after=ir_after,
            moved=moved,
            branchless_enabled=self.branchless,
            asm=asm,
            register_map=regalloc.map,
        )
        return asm, report


# =============================================================================
# CLI
# =============================================================================

DEFAULT_EXAMPLE = """
// C-like mínimo aceito pelo RAFAELIA ROOT optimizer
x = 10;
y = x + 5;
if (y < 20) {
    z = y + 10;
} else {
    z = y - 5;
}
w = z + x;
"""


def main(argv: Optional[Sequence[str]] = None) -> int:
    ap = argparse.ArgumentParser(description="RAFAELIA ROOT C→ASM optimizer auditável")
    ap.add_argument("input", nargs="?", help="Arquivo C-like de entrada. Se omitido, lê stdin; se stdin vazio, usa exemplo.")
    ap.add_argument("--target", choices=["arm64", "x86_64"], default="arm64")
    ap.add_argument("--no-branchless", action="store_true", help="Desativa conversão if/else simples para seleção sem branch")
    ap.add_argument("--no-schedule", action="store_true", help="Desativa reordenação por bloco básico")
    ap.add_argument("--audit", help="Caminho para salvar relatório JSON de auditoria")
    ap.add_argument("--emit-ir", action="store_true", help="Inclui traço IR no stderr em JSON")
    args = ap.parse_args(argv)

    if args.input:
        code = Path(args.input).read_text(encoding="utf-8")
    else:
        code = sys.stdin.read()
        if not code.strip():
            code = DEFAULT_EXAMPLE

    try:
        opt = RafaeliaRootOptimizer(
            code,
            target=args.target,
            branchless=not args.no_branchless,
            schedule=not args.no_schedule,
        )
        asm, report = opt.compile()
    except CompileError as e:
        print(f"[ERRO] {e}", file=sys.stderr)
        return 2

    print(asm)

    if args.audit:
        Path(args.audit).write_text(json.dumps(asdict(report), indent=2, ensure_ascii=False), encoding="utf-8")

    if args.emit_ir:
        print(json.dumps(report.instruction_trace, indent=2, ensure_ascii=False), file=sys.stderr)

    print("\n// ---- AUDIT ----", file=sys.stderr)
    print(f"// target={report.target}", file=sys.stderr)
    print(f"// original_hash={report.original_hash}", file=sys.stderr)
    print(f"// result_hash={report.result_hash}", file=sys.stderr)
    print(f"// audit_hash={report.audit_hash}", file=sys.stderr)
    print(f"// branchless_selects={report.branchless_selects}", file=sys.stderr)
    print(f"// scheduled_moves={report.scheduled_moves}", file=sys.stderr)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
