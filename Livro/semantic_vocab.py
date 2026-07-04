#!/usr/bin/env python3
"""semantic_vocab.py — vocabulário + semântica + scheduler RAFCODE-Φ.

Camada determinística para o diretório Livro:
- tokeniza texto por Unicode/script;
- reconhece clusters semânticos operacionais;
- varre arquivos do próprio Livro para extrair vocabulário em uso;
- seleciona métodos coerentes para o estado atual;
- gera hints de bytecode RVM;
- produz decisão de scheduler: resolver, auditar, expandir, compilar ou parar.

A regra é pé-no-chão: quando não há cobertura, o termo vira GAP. Não inventa.
"""
from __future__ import annotations

import argparse
import json
import re
import unicodedata
from collections import Counter, defaultdict
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Mapping, MutableMapping, Sequence, Tuple

VERSION = "2.0.0"

PIPELINE = "FALA→FONEMA→TOKEN→AST→BYTECODE→ASM→VM→OUTPUT"

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
        "intent": ("separar", "token", "lexer", "normalizar", "unicode", "script", "fonema", "fala"),
        "stage": "FALA→FONEMA→TOKEN",
        "output": "tokens normalizados com script, cluster e phi",
        "action": "separar entrada em unidades rastreáveis",
        "phi_base": 0.82,
    },
    "parse_ast": {
        "intent": ("parse", "ast", "sintaxe", "gramática", "estrutura", "dependência", "arvore", "árvore"),
        "stage": "TOKEN→AST",
        "output": "estrutura sintática mínima e relações sujeito/verbo/objeto",
        "action": "organizar tokens em forma estrutural",
        "phi_base": 0.80,
    },
    "semantic_expand": {
        "intent": ("semântica", "semantica", "sentido", "cluster", "expandir", "vocabulário", "vocabulario", "glossa"),
        "stage": "TOKEN→CLUSTER→CONTEXTO",
        "output": "conceitos relacionados, aliases, domínios e glossas",
        "action": "ampliar cobertura semântica sem inventar",
        "phi_base": 0.86,
    },
    "compile_rvm": {
        "intent": ("bytecode", "vm", "opcode", "rvm", "compilar", "baixo nível", "asm", "assembly"),
        "stage": "AST→BYTECODE→VM",
        "output": "sequência de opcodes sugerida",
        "action": "emitir forma executável compacta",
        "phi_base": 0.84,
    },
    "audit_phi": {
        "intent": ("validar", "auditar", "coerência", "coerencia", "phi", "hash", "assinatura", "verificar", "gap"),
        "stage": "OUTPUT→VERIFICAÇÃO",
        "output": "score phi, cobertura, gaps e razão da decisão",
        "action": "bloquear saída fraca e expor lacunas",
        "phi_base": 0.88,
    },
    "agent_loop": {
        "intent": ("agente", "agent", "loop", "planejar", "executar", "observar", "corrigir", "scheduler"),
        "stage": "PLAN→ACT→OBSERVE→VERIFY",
        "output": "próxima ação recomendada por estado",
        "action": "decidir o próximo método operacional",
        "phi_base": 0.83,
    },
    "scan_livro": {
        "intent": ("livro", "corpus", "varrer", "extrair", "arquivos", "metodos", "métodos"),
        "stage": "CORPUS→VOCAB→CLUSTER",
        "output": "vocabulário extraído dos arquivos locais do Livro",
        "action": "aprender termos em uso no próprio repositório",
        "phi_base": 0.85,
    },
}

BASE_SEMANTIC_CLUSTERS: Mapping[str, Mapping[str, object]] = {
    "CRIAR": {
        "aliases": ("criar", "gerar", "emitir", "produzir", "build", "create", "ברא", "ποιέω", "creare"),
        "domain": "ação",
        "stage": "CREATE",
        "rvm": (0x10, 0x11, 0xFF),
        "gloss": "fazer existir uma forma operacional",
    },
    "FALA": {
        "aliases": ("fala", "voz", "speech", "utterance", "som", "áudio", "audio", "phoneme", "fonema"),
        "domain": "entrada",
        "stage": "FALA→FONEMA",
        "rvm": (0x12,),
        "gloss": "entrada humana ou acústica convertida em unidade processável",
    },
    "TOKEN": {
        "aliases": ("token", "lexer", "lexema", "símbolo", "simbolo", "palavra", "chunk"),
        "domain": "representação",
        "stage": "FONEMA→TOKEN",
        "rvm": (0x03,),
        "gloss": "unidade mínima de processamento com identidade local",
    },
    "AST": {
        "aliases": ("ast", "árvore", "arvore", "parse", "sintaxe", "gramática", "gramatica", "dependência", "dependencia"),
        "domain": "estrutura",
        "stage": "TOKEN→AST",
        "rvm": (0x01, 0x02),
        "gloss": "organização relacional dos tokens em estrutura navegável",
    },
    "BYTECODE": {
        "aliases": ("bytecode", "opcode", "rvm", "vm", "instrução", "instrucao", "runtime"),
        "domain": "execução",
        "stage": "AST→BYTECODE",
        "rvm": (0x03, 0xFE, 0xFF),
        "gloss": "forma compacta executável por VM",
    },
    "ASM": {
        "aliases": ("asm", "assembly", "arm64", "aarch64", "neon", "syscall", "bare", "metal", "low", "level"),
        "domain": "baixo_nível",
        "stage": "BYTECODE→ASM",
        "rvm": (0xFE,),
        "gloss": "representação próxima do hardware e do custo real",
    },
    "COERENCIA": {
        "aliases": ("coerência", "coerencia", "phi", "estabilidade", "validação", "validacao", "consistência", "consistencia", "integridade"),
        "domain": "verificação",
        "stage": "VERIFY",
        "rvm": (0x16, 0xFF),
        "gloss": "capacidade de manter forma sem contradição sob variação",
    },
    "RUIDO": {
        "aliases": ("ruído", "ruido", "erro", "lacuna", "gap", "entropia", "ambiguidade", "falha"),
        "domain": "diagnóstico",
        "stage": "AUDIT",
        "rvm": (0x22, 0x23),
        "gloss": "sinal de fronteira entre forma estável e forma incompleta",
    },
    "SCHEDULER": {
        "aliases": ("scheduler", "orquestrador", "agenda", "latência", "latencia", "cache", "reuse", "thread", "pipeline", "ciclo"),
        "domain": "operação",
        "stage": "PLAN→ACT",
        "rvm": (0x24,),
        "gloss": "decisão de quando executar, reaproveitar, pular ou validar",
    },
    "BIBLIA_CORPUS": {
        "aliases": ("gênesis", "genesis", "bíblia", "biblia", "logos", "hebraico", "grego", "latim", "aramaico", "versículo", "versiculo"),
        "domain": "corpus",
        "stage": "CORPUS→GLOSSA→CLUSTER",
        "rvm": (0xF0,),
        "gloss": "corpus multilíngue usado como material de alinhamento semântico",
    },
    "AGENTE": {
        "aliases": ("agente", "agent", "loop", "plano", "ação", "acao", "observação", "observacao", "feedback"),
        "domain": "controle",
        "stage": "PLAN→ACT→OBSERVE→VERIFY",
        "rvm": (0x23, 0x24, 0xFF),
        "gloss": "ciclo operacional que retroalimenta decisão por evidência",
    },
    "TERMUX_ANDROID": {
        "aliases": ("termux", "android", "androidx", "gradle", "apk", "jni", "ndk", "vectras"),
        "domain": "operação",
        "stage": "APP→RUNTIME→VM",
        "rvm": (0x24, 0xFE),
        "gloss": "ambiente prático de execução, build e controle no Android",
    },
    "QEMU_TCG": {
        "aliases": ("qemu", "tcg", "emulação", "emulacao", "jit", "tradução", "traducao", "guest", "host"),
        "domain": "execução",
        "stage": "GUEST→TCG→HOST",
        "rvm": (0x23, 0x24, 0xFE),
        "gloss": "tradução dinâmica de instruções e ponte entre arquiteturas",
    },
    "MEMORIA_CACHE": {
        "aliases": ("memória", "memoria", "cache", "l1", "l2", "buffer", "prefetch", "warm", "cold", "ghost"),
        "domain": "baixo_nível",
        "stage": "STATE→CACHE→LATENCY",
        "rvm": (0x01, 0x02, 0x24),
        "gloss": "estado material de custo, latência e reaproveitamento",
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
    "corpus": ("scan_livro", "semantic_expand", "parse_ast"),
    "controle": ("agent_loop", "audit_phi", "compile_rvm"),
    "ação": ("semantic_expand", "compile_rvm", "audit_phi"),
}

SCAN_FILES = (
    "falas.sh",
    "fala.sh",
    "compiladorlowFala.txt",
    "bundle.txt",
    "agent_loop.txt",
    "vm_runtime.txt",
    "bibliaCorpus.txt",
    "RAFAELIA_WHITEPAPER.md",
    "METHODOLOGY.md",
    "TOTAL.txt",
    "IMPLEMENTA.txt",
    "BOOTSTRAP_LOWLEVEL_RAFAELIA.txt",
    "rafael.md",
    "whitepaper.txt",
    "paper.md",
)

STOPWORDS = {
    "para", "como", "com", "uma", "que", "por", "dos", "das", "the", "and", "or", "def", "return", "static",
    "uint32", "uint8", "char", "int", "void", "this", "from", "true", "false", "none", "null", "não", "nao",
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
    text = unicodedata.normalize("NFKC", text or "")
    text = re.sub(r"\s+", " ", text, flags=re.UNICODE).strip()
    return text


def detect_script(char: str) -> str:
    cp = ord(char)
    for script, ranges in SCRIPT_RANGES.items():
        if any(lo <= cp <= hi for lo, hi in ranges):
            return script
    if char.isdigit():
        return "num"
    if char.isalpha() or char == "_":
        return "latn"
    return "sep"


def safe_read(path: Path, limit: int = 1_500_000) -> str:
    try:
        if not path.exists() or not path.is_file():
            return ""
        data = path.read_text(encoding="utf-8", errors="ignore")
        return data[:limit]
    except OSError:
        return ""


def token_parts(text: str) -> List[str]:
    return re.findall(r"[\w\u0370-\u03FF\u0590-\u05FF\u0600-\u06FF\u1F00-\u1FFF]+", normalize(text), re.UNICODE)


def base_clusters() -> Dict[str, Dict[str, object]]:
    return {k: dict(v) for k, v in BASE_SEMANTIC_CLUSTERS.items()}


def cluster_for(term: str, clusters: Mapping[str, Mapping[str, object]] | None = None) -> str | None:
    clusters = clusters or BASE_SEMANTIC_CLUSTERS
    low = term.lower().strip()
    for name, data in clusters.items():
        aliases = tuple(str(x).lower() for x in data.get("aliases", ()))
        if low == name.lower() or low in aliases:
            return name
    return None


def infer_domain(term: str) -> str:
    low = term.lower()
    if any(x in low for x in ("qemu", "tcg", "jit", "vm", "bytecode", "opcode", "runtime")):
        return "execução"
    if any(x in low for x in ("asm", "arm", "aarch", "neon", "cache", "mem", "prefetch", "kernel")):
        return "baixo_nível"
    if any(x in low for x in ("agent", "loop", "plan", "observe", "scheduler", "thread")):
        return "controle"
    if any(x in low for x in ("biblia", "bible", "genesis", "logos", "hebra", "greg", "latim")):
        return "corpus"
    if any(x in low for x in ("phi", "coer", "audit", "valid", "hash", "seal")):
        return "verificação"
    if any(x in low for x in ("termux", "android", "gradle", "apk", "jni", "ndk")):
        return "operação"
    return "diagnóstico"


def scan_livro(root: str | Path = ".", top_n: int = 80) -> Dict[str, object]:
    root_path = Path(root)
    if root_path.name != "Livro" and (root_path / "Livro").exists():
        root_path = root_path / "Livro"

    source_stats: List[Dict[str, object]] = []
    word_counter: Counter[str] = Counter()
    method_hits: Dict[str, Counter[str]] = {name: Counter() for name in METHODS}

    for rel in SCAN_FILES:
        path = root_path / rel
        content = safe_read(path)
        if not content:
            continue
        parts = [p.lower() for p in token_parts(content)]
        filtered = [p for p in parts if len(p) >= 3 and p not in STOPWORDS]
        word_counter.update(filtered)
        for method_name, method in METHODS.items():
            intent = [str(x).lower() for x in method.get("intent", ())]
            for kw in intent:
                if kw in content.lower():
                    method_hits[method_name][kw] += content.lower().count(kw)
        source_stats.append({"file": rel, "chars": len(content), "terms": len(filtered)})

    clusters = base_clusters()
    learned_terms = []
    for term, count in word_counter.most_common(top_n):
        if cluster_for(term, clusters):
            continue
        domain = infer_domain(term)
        cid = f"TERM_{re.sub(r'[^A-Za-z0-9_]+', '_', term).upper()[:40]}"
        clusters[cid] = {
            "aliases": (term,),
            "domain": domain,
            "stage": "LIVRO→VOCAB→CLUSTER",
            "rvm": (0x03, 0x16),
            "gloss": f"termo extraído do Livro com frequência {count}",
            "source": "scan_livro",
            "count": count,
        }
        learned_terms.append({"term": term, "cluster": cid, "domain": domain, "count": count})

    method_evidence = {
        name: dict(counter.most_common(12)) for name, counter in method_hits.items() if counter
    }
    return {
        "root": str(root_path),
        "files_scanned": source_stats,
        "learned_terms": learned_terms,
        "method_evidence": method_evidence,
        "semantic_clusters": clusters,
    }


def tokenize(text: str, clusters: Mapping[str, Mapping[str, object]] | None = None) -> List[Token]:
    clusters = clusters or BASE_SEMANTIC_CLUSTERS
    tokens: List[Token] = []
    for part in token_parts(text):
        scripts = [detect_script(ch) for ch in part if detect_script(ch) != "sep"]
        script = max(set(scripts), key=scripts.count) if scripts else "sep"
        norm = part.lower()
        cluster = cluster_for(norm, clusters)
        phi = 0.94 if cluster and str(cluster).startswith("TERM_") is False else 0.88 if cluster else 0.72 if script != "sep" else 0.40
        tokens.append(Token(raw=part, norm=norm, script=script, cluster=cluster, phi=round(phi, 3)))
    return tokens


def expand_semantics(tokens: Sequence[Token], clusters: Mapping[str, Mapping[str, object]] | None = None) -> Dict[str, object]:
    clusters = clusters or BASE_SEMANTIC_CLUSTERS
    active: Dict[str, Dict[str, object]] = {}
    gaps: List[str] = []
    for tok in tokens:
        if not tok.cluster:
            gaps.append(tok.raw)
            continue
        data = dict(clusters[tok.cluster])
        data["hits"] = int(data.get("hits", 0)) + 1
        active[tok.cluster] = data
    domains = [str(data.get("domain")) for data in active.values()]
    dominant_domain = max(set(domains), key=domains.count) if domains else "diagnóstico"
    return {
        "clusters": active,
        "dominant_domain": dominant_domain,
        "gaps": gaps,
        "coverage": coverage(tokens),
        "phi": phi_score(tokens, active),
    }


def coverage(tokens: Sequence[Token]) -> float:
    if not tokens:
        return 0.0
    return round(len([t for t in tokens if t.cluster]) / len(tokens), 4)


def phi_score(tokens: Sequence[Token], clusters: Mapping[str, object]) -> float:
    if not tokens:
        return 0.0
    lexical = sum(t.phi for t in tokens) / len(tokens)
    cov = coverage(tokens)
    cluster_density = min(1.0, len(clusters) / max(len(tokens), 1) * 1.5)
    return round((lexical * 0.50) + (cov * 0.35) + (cluster_density * 0.15), 4)


def choose_methods(
    text: str,
    clusters: Mapping[str, Mapping[str, object]] | None = None,
    method_evidence: Mapping[str, Mapping[str, int]] | None = None,
) -> List[MethodPlan]:
    clusters = clusters or BASE_SEMANTIC_CLUSTERS
    tokens = tokenize(text, clusters)
    expanded = expand_semantics(tokens, clusters)
    domain = str(expanded["dominant_domain"])
    candidates = list(DOMAIN_PRIORITIES.get(domain, ("semantic_expand", "audit_phi", "agent_loop")))
    evidence = method_evidence or {}

    low = normalize(text).lower()
    scored: Dict[str, float] = {}
    reasons: Dict[str, str] = {}
    for name, method in METHODS.items():
        hits = [kw for kw in method["intent"] if str(kw).lower() in low]
        base = float(method["phi_base"])
        prior_bonus = 0.08 if name in candidates else 0.0
        hit_bonus = min(0.18, 0.04 * len(hits))
        evidence_bonus = min(0.08, 0.01 * sum(int(v) for v in evidence.get(name, {}).values()))
        scored[name] = round(min(1.0, base + prior_bonus + hit_bonus + evidence_bonus), 4)
        if hits:
            reasons[name] = "hits: " + ", ".join(str(x) for x in hits)
        elif evidence.get(name):
            reasons[name] = "evidência no Livro: " + ", ".join(list(evidence[name])[:4])
        else:
            reasons[name] = f"prioridade por domínio {domain}"

    ordered = sorted(scored, key=scored.get, reverse=True)[:3]
    return [MethodPlan(method=m, stage=str(METHODS[m]["stage"]), reason=reasons[m], phi=scored[m]) for m in ordered]


def rvm_hints(tokens: Sequence[Token], clusters: Mapping[str, Mapping[str, object]] | None = None) -> List[int]:
    clusters = clusters or BASE_SEMANTIC_CLUSTERS
    bytecode: List[int] = [0x10]
    for tok in tokens:
        if tok.cluster:
            rvm = clusters[tok.cluster].get("rvm", (0x03,))
            if isinstance(rvm, tuple):
                bytecode.extend(int(x) for x in rvm if int(x) not in (0x10, 0xFF))
            else:
                bytecode.append(0x03)
        else:
            bytecode.append(0x03)
    bytecode.extend([0x16, 0xFF])
    return bytecode


def scheduler_decision(frame: Mapping[str, object]) -> Dict[str, object]:
    semantic = frame.get("semantic", {}) if isinstance(frame.get("semantic"), Mapping) else {}
    methods = frame.get("methods", []) if isinstance(frame.get("methods"), list) else []
    phi = float(semantic.get("phi", 0.0))
    cov = float(semantic.get("coverage", 0.0))
    gaps = semantic.get("gaps", []) if isinstance(semantic.get("gaps"), list) else []
    next_method = methods[0]["method"] if methods else "audit_phi"

    if phi >= 0.82 and cov >= 0.70:
        state = "F_DE_RESOLVIDO"
        action = next_method
        reason = "cobertura suficiente e phi acima do corte"
    elif gaps:
        state = "F_DE_GAP"
        action = "semantic_expand" if next_method != "semantic_expand" else "scan_livro"
        reason = "existem termos sem cluster; expandir vocabulário antes de compilar"
    else:
        state = "F_DE_NEXT"
        action = next_method
        reason = "sem gaps explícitos, mas phi ainda pede próximo método"

    return {
        "state": state,
        "action": action,
        "reason": reason,
        "phi": round(phi, 4),
        "coverage": round(cov, 4),
        "gap_count": len(gaps),
    }


def context_frame(text: str, root: str | Path = ".", scan: bool = False) -> Dict[str, object]:
    scan_payload = scan_livro(root) if scan else {}
    clusters = scan_payload.get("semantic_clusters", BASE_SEMANTIC_CLUSTERS)
    method_evidence = scan_payload.get("method_evidence", {})
    tokens = tokenize(text, clusters)  # type: ignore[arg-type]
    expanded = expand_semantics(tokens, clusters)  # type: ignore[arg-type]
    methods = choose_methods(text, clusters, method_evidence)  # type: ignore[arg-type]
    frame: Dict[str, object] = {
        "version": VERSION,
        "pipeline": PIPELINE,
        "input": text,
        "tokens": [asdict(t) for t in tokens],
        "semantic": expanded,
        "methods": [asdict(m) for m in methods],
        "rvm_hints_hex": [f"0x{x:02X}" for x in rvm_hints(tokens, clusters)],  # type: ignore[arg-type]
        "resolved": expanded["phi"] >= 0.82 and expanded["coverage"] >= 0.70,
        "gap": expanded["gaps"][:12],
        "next": methods[0].method if methods else "audit_phi",
    }
    frame["scheduler"] = scheduler_decision(frame)
    if scan:
        frame["livro_scan"] = {
            "root": scan_payload.get("root"),
            "files_scanned": scan_payload.get("files_scanned", []),
            "learned_terms_top": scan_payload.get("learned_terms", [])[:30],
            "method_evidence": method_evidence,
        }
    return frame


def export_vocab(root: str | Path = ".", scan: bool = False) -> Dict[str, object]:
    scan_payload = scan_livro(root) if scan else {}
    clusters = scan_payload.get("semantic_clusters", BASE_SEMANTIC_CLUSTERS)
    payload: Dict[str, object] = {
        "version": VERSION,
        "pipeline": PIPELINE,
        "methods": METHODS,
        "semantic_clusters": clusters,
        "domain_priorities": DOMAIN_PRIORITIES,
    }
    if scan:
        payload["livro_scan"] = {
            "root": scan_payload.get("root"),
            "files_scanned": scan_payload.get("files_scanned", []),
            "learned_terms_top": scan_payload.get("learned_terms", [])[:80],
            "method_evidence": scan_payload.get("method_evidence", {}),
        }
    return payload


def explain_methods() -> List[Dict[str, object]]:
    out = []
    for name, data in METHODS.items():
        out.append({
            "method": name,
            "stage": data["stage"],
            "does": data["action"],
            "output": data["output"],
            "intent": list(data["intent"]),
        })
    return out


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Vocabulário semântico + scheduler RAFCODE-Φ")
    parser.add_argument("text", nargs="*", help="texto para analisar")
    parser.add_argument("--root", default=".", help="raiz do repo ou diretório Livro")
    parser.add_argument("--scan-livro", action="store_true", help="varre arquivos do Livro e amplia vocabulário")
    parser.add_argument("--export-vocab", action="store_true", help="imprime vocabulário completo")
    parser.add_argument("--explain-methods", action="store_true", help="explica o que cada método faz")
    parser.add_argument("--schedule", action="store_true", help="imprime só a decisão do scheduler")
    parser.add_argument("--pretty", action="store_true", help="JSON identado")
    args = parser.parse_args(argv)

    text = " ".join(args.text).strip()
    if args.explain_methods:
        payload: object = explain_methods()
    elif args.export_vocab:
        payload = export_vocab(args.root, args.scan_livro)
    else:
        frame = context_frame(text, args.root, args.scan_livro)
        payload = frame["scheduler"] if args.schedule else frame
    print(json.dumps(payload, ensure_ascii=False, indent=2 if args.pretty else None, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
