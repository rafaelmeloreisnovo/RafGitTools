# RAFAELIA Ω — Semantic Chaos Lab V1 (isolated experiment)

**Status:** `EXPERIMENTAL / HOLD / claim_allowed=false`.  
**Scope:** only `tools/semantic_chaos_lab/`, no changes to existing application, Android builds, licenses, or earlier receipts.  
**Base:** `main@0c6bbb32acda3a729652e1fb2559a5976dd0a86c` (snapshot before this branch).

This is a **typed token-anomaly + reversible capture experiment**, not an LLM tokenizer, a trained model, a verified cross-language translator, a proven new compression algorithm, nor a scientific proof of syntropy.

## μREAD START — five small fragments

1. [Corpus Ω16–Ω60](corpus/omega_adversarial_v1.jsonl) — 45 synthetic, mixed-script JSONL records, **not verified translations**.
2. [Six original micro-poems](poemas_originais_v1.md) — inspired by dialogue about `molho`; **not Tao Te Ching quotations/translations**.
3. [Hosted chaos gate](chaos_gate.py) — stdlib Python, deterministic shuffle + exact restoration + fail-closed receipt.
4. [Freestanding C99 core](omega_token_core.h) — typed byte views and noncryptographic fingerprint, no runtime lib, no normalization.
5. [Tests](tests/selftest.sh) — host harness isolated from production, negative gates and ELF-object undefined-symbol check.

## Token kinds — lossless distinctions

| Class | Payload contract | Why it must stay different |
|---|---|---|
| `EMPTY_GLYPH` | literal `∅`, UTF-8 E2 88 85 | written symbol, not absence |
| `EMPTY_STRING` | `""` | present zero-length string |
| `SPACE_ONLY` | 1+ ASCII U+0020, e.g. `"    "` | four actual spaces are content |
| `NUL_BYTE` | JSON escaped `"\\u0000"` | actual byte zero in decoded value |
| `JSON_NULL` | `null` | explicitly present null |
| `MISSING_FIELD` | payload key **absent** | absence of member, not null |
| `ZERO_NUMBER` | integer `0` (not false) | mathematical zero |
| `UNKNOWN`, `INDETERMINATE` | nonempty string | ignorance is not failure |
| `NOISE`, `ABORTED`, `IGNORED`, `CENSORED` | nonempty string | different provenance & decisions |
| `REVERSE`, `INVERSE`, `ANTIDERIVED`, `ANTITENSOR` | nonempty string | test labels, **not literal calculus** |
| `HEURISTIC`, `INVARIANT`, `UNWEIGHED`, `UNARY`, `ZERO_ORDER`, `RECLUSE`, `UNTOKENIZED`, `DETERMINATE`, `RAW_TEXT` | nonempty string | experimental annotation only |

Original transport representation is UTF-8 JSONL, one JSON object per line; `id` unique; `source=CREATED_SYNTHETIC` and `privacy_class=SYNTHETIC` mandatory. The gate rejects unknown kinds, malformed UTF-8, unbounded input, bad null/space/zero distinctions, or attempts to upload real/private content into this published experiment. The `language_hint=UNVERIFIED` field is not a language identification claim.

The C core intentionally uses a separate typed data view; `JSON_NULL`, `MISSING_FIELD`, `ZERO_NUMBER` are encoded as distinct **type tags with zero byte payload**. The (kind, provenance_id, original_order, len, bytes) tuple differentiates inputs. `fingerprint32` is FNV-1a for **diagnostics only**; never substitute it for SHA-256 provenance.

## Pipeline: chaos is an intermediate state, not a final source of truth

```text
Ω input JSONL — synthetic / read-only
  → type validation (fail-closed)
  → capture exact UTF-8 lines and SHA-256
  → deterministic xorshift32 Fisher–Yates permutation (seed fixed)
  → chaos.jsonl + order.json (new output dir, never overwrite)
  → inverse permutation and exact raw-byte comparison
  → receipt.json (SHA-256, entropy bits/byte, zlib9 baselines, claim_allowed=false)
  → optional independent test + human review
```

**No translation/normalization, model calls, model weight writes, actual traceroute/routing, causal network claims, or automatic training.** Reordering *whole records* preserves the byte histogram; therefore Shannon byte entropy is theoretically invariant under this shuffle. Any entropy improvement claim from shuffling alone is rejected. Changes in `zlib9` compressed length may reflect local adjacency and dictionary effects only; they are **not evidence of semantic compression**.

## Usage (host-only, isolated)

```sh
sh tools/semantic_chaos_lab/tests/selftest.sh
python3 tools/semantic_chaos_lab/chaos_gate.py build \
  tools/semantic_chaos_lab/corpus/omega_adversarial_v1.jsonl \
  /tmp/rafaelia-semchaos-new --seed 42
python3 tools/semantic_chaos_lab/chaos_gate.py restore \
  /tmp/rafaelia-semchaos-new/chaos.jsonl \
  /tmp/rafaelia-semchaos-new/order.json \
  /tmp/rafaelia-semchaos-recovered.jsonl
```

Paths under `/tmp` are merely examples; the gate requires new output paths and will not overwrite an existing output. The hosted test wrapper may use `cc`, `nm`, `python3`, `cmp`, `mktemp`; the isolated `omega_token_core.o` is compiled with `-ffreestanding -fno-builtin -fno-stack-protector` and checked for undefined symbols. **Freestanding does not mean executable without a target-specific startup/ABI.**

## Falsifiers, urgency and gates

| Gate | Priority | Accept only when |
|---|---|---|
| TOKEN_KIND | P0 | ∅ / empty / 4 spaces / NUL / JSON null / missing / numeric zero all noninterchangeable |
| PRIVACY_SOURCE | P0 | public test corpus synthetic-only; no actual conversation export |
| EXACT_ROUNDTRIP | P0 | source SHA-256 == restored SHA-256, byte-for-byte |
| TAMPER_REJECTION | P0 | altered chaos or order rejects with no output |
| L0_OBJECT | P1 | no undefined symbols in isolated C core object on tested compiler |
| REPRODUCIBILITY | P1 | same input+seed+version produce identical chaos and order |
| MULTILINGUAL | P1 | UTF-8 strings exact; translation `TOKEN_VAZIO_NOT_VERIFIED` |
| ENTROPY | P1 | raw byte entropy, zlib baseline reported with denominator and method |
| SCIENTIFIC | P2 | compare independent tokenizer/model baselines; pre-register tests/ablation; no SOTA claims |
| AUTHORSHIP/LEGAL | P0 | retain original prompts/user, upstream references, privacy and cultural consent |

## Definition of P0 states

`TOKEN_VAZIO_UNKNOWN` = evidence unavailable; `UNMEASURED` = metric not executed; `UNIDENTIFIABLE` = cannot distinguish with available data; `CONTRADICTORY` = evidence conflicts; `OUT_OF_DOMAIN` = unsupported source; `WITHHELD` = intentionally restricted; `IRRECOVERABLE` = reconstruction infeasible with available source. These are **epistemic receipt states**, not interchangeable with token payload kind `EMPTY_GLYPH`.

`ABORTED` = captured abandoned record (not erased). `IGNORED` = captured intentionally excluded item. `CENSORED` = label without disclosing restricted content. `ANTIDERIVED` is an adversarial relation label, not a proven mathematical operator. Every anomaly retains its stable source ID and order map.

## R3 receipt and regression boundary

**F_ok design:** types, deterministic shuffle, exact reversal, synthetic source gate, byte-level metrics, scoped C core and negative tests.  
**F_gap:** real LLM behavior, downstream semantic quality, device-specific ABI, production privacy and academic novelty remain `TOKEN_VAZIO_NOT_TESTED` unless proven in an exact test.  
**F_next:** independent tests against multilingual byte/BPE baselines; a tokenizer-independent evaluation set; ablations, collisions, language coverage, falsifiers, ethics review.

**Rollback:** revert only this experiment branch/PR. Prior code and documents remain untouched. There are no network operations, SDK installs, repository writes or runtime side effects outside explicit new experiment outputs.

## alphaXiv literature workspace

Folder: `RAFAELIA Ω | Tokens anômalos e falsificabilidade 2026-10-28`. Bibliographic evidence: 2607.26831, 2604.16037, 2606.15044, 2510.09947, 2506.15889, 2405.05417, 2505.14226, 2404.09894. This is an **alphaXiv saved-paper folder**, not submission or peer review of RAFAELIA.
