#!/usr/bin/env python3
"""semantic_vocab.py — vocabulário semântico operacional RAFCODE-Φ.

Camada pé-no-chão para usar os métodos do diretório Livro:
- tokenização por script Unicode;
- clusters semânticos multilíngues;
- vocabulário técnico de VM, bytecode, AST, scheduler e baixo nível;
- seleção de métodos por intenção;
- expansão semântica auditável com score phi;
- emissão de dicas RVM para integração com falas.sh / vm_runtime.txt.

Não tenta ser LLM. É uma camada determinística de léxico, contexto e coerência.
"""
from __future__ import annotations

import argparse
import json
import re
import unicodedata
from dataclasses import dataclass, asdict
from typing import Dict, Iterable, List, Mapping, Sequence, Tuple

VERSION = "1.0.0"

SCRIPT_RANGES: Mapping[str, Tuple[Tuple[int, int], ...]] = {
    "he": ((0x0590, 0x05FF), (0xFB1D, 0xFB4F)),
    "ar": ((0x0600, 0x06FF), (0x0750, 0x077F)),
    "el": ((0x0370, 0x03FF), (0x1F00, 0x1FFF)),
    "cn": ((0x3400, 0x4DBF), (0x4E00, 0x9FFF)),
    "jp": ((0x3040, 0x30FF), (0x31F0, 0x31FF)),
    "ru": ((0x0400, 0x04FF),),
}

METHODS: Mapping[str, Mapping[str, object]] = {
    "tokenize": {
        "intent": ("separar", "token", "lexer", "normalizar", "unicode", "script"),
        "stage": "FALA→FONEMA→TOKEN",
        "output": "tokens normalizados com script e phi",
        "phi_base": 0.82,
    },
    "parse_ast": {
        "intent": ("parse", "ast", "sintaxe", "gramática", "estrutura", "dependência"),
        "stage": "TOKEN→AST",
        "output": "árvore sintática mínima e relações sujeito/verbo/objeto",
        "phi_base": 0.80,
    },
    "semantic_expand": {
        "intent": ("semântica", "sentido", "cluster", "expandir", "vocabulário", "glossa"),
        "stage": "TOKEN→CLUSTER→CONTEXTO",
        "output": "conceitos relacionados, aliases e traduções",
        "phi_base": 0.86,
    },
    "compile_rvm": {
        "intent": ("bytecode", "vm", "opcode", "rvm", "compilar", "baixo nível"),
        "stage": "AST→BYTECODE→VM",
        "output": "sequência de opcodes sugerida",
        "phi_base": 0.84,
    },
    "audit_phi": {
        "intent": ("validar", "auditar", "coerência", "phi", "hash", "assinatura"),
        "stage": "OUTPUT→VERIFICAÇÃO",
        "output": "score phi e lacunas explícitas",
        "phi_base": 0.88,
    },
    "agent_loop": {
        "intent": ("agente", "loop", "planejar", "executar", "observar", "corrigir"),
        "stage": "PLAN→ACT→OBSERVE→VERIFY",
        "output": "próximo método recomendado por estado",
        "phi_base": 0.83,
    },
}

SEMANTIC_CLUSTERS: Mapping[str, Mapping[str, object]] = {
    "CRIAR": {
        "aliases": ("criar", "gerar", "emitir", "produzir", "build", "create", "ברא", "ποιέω", "creare"),
        "domain": "ação",
        "stage": "CREATE",
        "rvm": (0x10, 0x11, 0xFF),
        "gloss": "fazer existir uma forma operacional",
    },
    "FALA": {
        "aliases": ("fala", "voz", "speech", "utterance", "som", "áudio", "phoneme", "fonema"),
        "domain": "entrada",
        "stage": "FALA→FONEMA",
        "rvm": (0x12,),
        "gloss": "entrada humana ou acústica convertida em unidade processável",
    },
    "TOKEN": {
        "aliases": ("token", "lexer", "lexema", "símbolo", "palavra", "chunk"),
        "domain": "representação",
        "stage": "FONEMA→TOKEN",
        "rvm": (0x03,),
        "gloss": "unidade mínima de processamento com identidade local",
    },
    "AST": {
        "aliases": ("ast", "árvore", "parse", "sintaxe", "gramática", "dependência"),
        "domain": "estrutura",
        "stage": "TOKEN→AST",
        "rvm": (0x01, 0x02),
        "gloss": "organização relacional dos tokens em estrutura navegável",
    },
    "BYTECODE": {
        "aliases": ("bytecode", "opcode", "rvm", "vm", "instrução", "instrucao"),
        "domain": "execução",
        "stage": "AST→BYTECODE",
        "rvm": (0x03, 0xFE, 0xFF),
        "gloss": "forma compacta executável por VM",
    },
    "ASM": {
        "aliases": ("asm", "assembly", "arm64", "aarch64", "neon", "syscall", "bare metal", "low level"),
        "domain": "baixo_nível",
        "stage": "BYTECODE→ASM",
        "rvm": (0xFE,),
        "gloss": "representação próxima do hardware e do custo real",
    },
    "COERENCIA": {
        "aliases": ("coerência", "coerencia", "phi", "estabilidade", "validação", "consistência", "integridade"),
        "domain": "verificação",
        "stage": "VERIFY",
        "rvm": (0x16, 0xFF),
        "gloss": "capacidade de manter forma sem contradição sob variação",
    },
    "RUIDO": {
        "aliases": ("ruído", "ruido", "erro", "lacuna", "gap", "entropia", "ambiguidade"),
        "domain": "diagnóstico",
        "stage": "AUDIT",
        "rvm": (0x22, 0x23),
        "gloss": "sinal de fronteira entre forma estável e forma incompleta",
    },
    "SCHEDULER": {
        "aliases": ("scheduler", "orquestrador", "agenda", "latência", "cache", "reuse", "thread", "pipeline"),
        "domain": "operação",
        "stage": "PLAN→ACT",
        "rvm": (0x24,),
        "gloss": "decisão de quando executar, reaproveitar, pular ou validar",
    },
    "BIBLIA_CORPUS": {
        "aliases": ("gênesis", "genesis", "bíblia", "biblia", "logos", "hebraico", "grego", "latim", "aramaico"),
        "domain": "corpus",
        "stage": "CORPUS→GLOSSA→CLUSTER",
        "rvm": (0xF0,),
        "gloss": "corpus multilíngue usado como material de alinhamento semântico",
    },
    "AGENTE": {
        "aliases": ("agente", "agent", "loop", "plano", "ação", "observação", "feedback"),
        "domain": "controle",
        "stage": "PLAN→ACT→OBSERVE→VERIFY",
        "rvm": (0x23, 0x24, 0xFF),
        "gloss": "ciclo operacional que retroalimenta decisão por evidência",
    },
}

DOMAIN_PRIORITIES: Mapping[str, Tuple[str, ...]] = {
    "entrada": ("tokenize", "semantic_expand", "audit_phi"),
    "representação": ("tokenize", "parse_ast", "semantic_expand"),
    "estrutura": ("parse_ast", "semantic_expand", "audit_phi"),
    "execução": ("compile_rvm", "audit_phi", "agent_loop"),
    "baixo_nível": ("compile_rvm", "audit_phi", "agent_loop"),
    "verificação": ("audit_phi", "semantic_expand", "agent_loop"),
    "diagnóstico": ("audit_phi", "semantic_expand", "parse_ast"),
    "operação": ("agent_loop", "compile_rvm", "audit_phi"),
    "corpus": ("semantic_expand", "tokenize", "parse_ast"),
    "controle": ("agent_loop", "audit_phi", "compile_rvm"),
}

@dataclass(frozen=True)
class Token:
    raw: str
    norm: str
    script: str
    cluster: str | None
    phi: float

@dataclass(frozen=True)
class MethodPlan:
    method: str
    stage: str
    reason: str
    phi: float


def normalize(text: str) -> str:
    """Normalização estável: Unicode NFKC + colapso de espaço."""
    text = unicodedata.normalize("NFKC", text)
    text = re.sub(r"\s+", " ", text, flags=re.UNICODE).strip()
    return text


def detect_script(char: str) -> str:
    cp = ord(char)
    for script, ranges in SCRIPT_RANGES.items():
        if any(lo <= cp <= hi for lo, hi in ranges):
            return script
    if char.isdigit():
        return "num"
    if char.isalpha():
        return "latn"
    return "sep"


def cluster_for(term: str) -> str | None:
    low = term.lower().strip()
    for name, data in SEMANTIC_CLUSTERS.items():
        aliases = tuple(str(x).lower() for x in data.get("aliases", ()))
        if low == name.lower() or low in aliases:
            return name
    return None


def tokenize(text: str) -> List[Token]:
    text = normalize(text)
    parts = re.findall(r"[\w\u0370-\u03FF\u0590-\u05FF\u0600-\u06FF\u1F00-\u1FFF]+", text, re.UNICODE)
    tokens: List[Token] = []
    for part in parts:
        scripts = [detect_script(ch) for ch in part if detect_script(ch) != "sep"]
        script = max(set(scripts), key=scripts.count) if scripts else "sep"
        norm = part.lower()
        cluster = cluster_for(norm)
        phi = 0.92 if cluster else 0.72 if script != "sep" else 0.40
        tokens.append(Token(raw=part, norm=norm, script=script, cluster=cluster, phi=phi))
    return tokens


def expand_semantics(tokens: Sequence[Token]) -> Dict[str, object]:
    clusters: Dict[str, Dict[str, object]] = {}
    gaps: List[str] = []
    for tok in tokens:
        if not tok.cluster:
            gaps.append(tok.raw)
            continue
        data = dict(SEMANTIC_CLUSTERS[tok.cluster])
        data["hits"] = int(data.get("hits", 0)) + 1
        clusters[tok.cluster] = data
    domains = [str(data.get("domain")) for data in clusters.values()]
    dominant_domain = max(set(domains), key=domains.count) if domains else "diagnóstico"
    phi = phi_score(tokens, clusters)
    return {
        "clusters": clusters,
        "dominant_domain": dominant_domain,
        "gaps": gaps,
        "phi": phi,
    }


def phi_score(tokens: Sequence[Token], clusters: Mapping[str, object]) -> float:
    if not tokens:
        return 0.0
    lexical = sum(t.phi for t in tokens) / len(tokens)
    coverage = len([t for t in tokens if t.cluster]) / len(tokens)
    cluster_density = min(1.0, len(clusters) / max(len(tokens), 1) * 1.5)
    return round((lexical * 0.50) + (coverage * 0.35) + (cluster_density * 0.15), 4)


def choose_methods(text: str) -> List[MethodPlan]:
    tokens = tokenize(text)
    expanded = expand_semantics(tokens)
    domain = str(expanded["dominant_domain"])
    candidates = list(DOMAIN_PRIORITIES.get(domain, ("semantic_expand", "audit_phi", "agent_loop")))

    low = normalize(text).lower()
    scored: Dict[str, float] = {}
    reasons: Dict[str, str] = {}
    for name, method in METHODS.items():
        hits = [kw for kw in method["intent"] if str(kw).lower() in low]
        base = float(method["phi_base"])
        prior_bonus = 0.08 if name in candidates else 0.0
        hit_bonus = min(0.18, 0.04 * len(hits))
        scored[name] = round(min(1.0, base + prior_bonus + hit_bonus), 4)
        reasons[name] = ", ".join(hits) if hits else f"prioridade por domínio {domain}"

    ordered = sorted(scored, key=scored.get, reverse=True)[:3]
    return [MethodPlan(method=m, stage=str(METHODS[m]["stage"]), reason=reasons[m], phi=scored[m]) for m in ordered]


def rvm_hints(tokens: Sequence[Token]) -> List[int]:
    bytecode: List[int] = [0x10]
    for tok in tokens:
        if tok.cluster:
            rvm = SEMANTIC_CLUSTERS[tok.cluster].get("rvm", (0x03,))
            if isinstance(rvm, tuple):
                bytecode.extend(int(x) for x in rvm if int(x) not in (0x10, 0xFF))
            else:
                bytecode.append(0x03)
        else:
            bytecode.append(0x03)
    bytecode.extend([0x16, 0xFF])
    return bytecode


def context_frame(text: str) -> Dict[str, object]:
    tokens = tokenize(text)
    expanded = expand_semantics(tokens)
    methods = choose_methods(text)
    return {
        "version": VERSION,
        "input": text,
        "tokens": [asdict(t) for t in tokens],
        "semantic": expanded,
        "methods": [asdict(m) for m in methods],
        "rvm_hints_hex": [f"0x{x:02X}" for x in rvm_hints(tokens)],
        "resolved": expanded["phi"] >= 0.78,
        "gap": expanded["gaps"][:12],
        "next": methods[0].method if methods else "audit_phi",
    }


def export_vocab() -> Dict[str, object]:
    return {
        "version": VERSION,
        "methods": METHODS,
        "semantic_clusters": SEMANTIC_CLUSTERS,
        "domain_priorities": DOMAIN_PRIORITIES,
    }


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Vocabulário semântico RAFCODE-Φ")
    parser.add_argument("text", nargs="*", help="texto para analisar")
    parser.add_argument("--export-vocab", action="store_true", help="imprime vocabulário completo")
    parser.add_argument("--pretty", action="store_true", help="JSON identado")
    args = parser.parse_args(argv)

    payload = export_vocab() if args.export_vocab else context_frame(" ".join(args.text))
    print(json.dumps(payload, ensure_ascii=False, indent=2 if args.pretty else None, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
