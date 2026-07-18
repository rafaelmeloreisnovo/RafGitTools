# RAFAELIA Knowledge Antiderivative v1

Status: `DRIVE_FIRST / POINTER_ONLY / TESTED_FINITE_DECLARED_DOMAIN / CLAIM_ALLOWED=false`.

## Purpose

This contract turns a large, heterogeneous Drive history into a bounded and
reviewable control-plane object.  It does **not** copy private corpus bodies into
GitHub.  It stores only Drive identifiers, classifications, state boundaries and
finite mathematical checks.

The transformation is:

```text
expression
→ typed term
→ source pointer
→ authority
→ implementation duty
→ test
→ evidence
→ temporal state
→ rollback
```

## Drive-first outputs

The first canonical pair was written inside the Drive semantic navigation tree:

```text
folder:
  1H7VtNdhcopTcT8mhJcJsnG-EkML7WxSe

document:
  RAFAELIA — Antiderivada do VAZIO e Invariante Toroidal — 2026-07-17
  id: 1wxi_LKwyl8D0XLatBbpyOfkfw5S3v1Rmt4XQFrlFF6I

knowledge matrix:
  RAFAELIA Knowledge Matrix — VAZIO/Toroidal — 2026-07-17
  id: 1KY4_uaZTo9pbTan_K19jlwxvUP4RHnjpPSDyAfJBlOE
```

GitHub remains a validator and pointer registry; Drive remains the corpus and
revision authority.

## Security finding

A historical environment dump in Drive contains a credential-shaped GitHub
secret in plaintext.  The value is intentionally absent from every artifact in
this change.

Required response:

1. revoke or rotate the exposed credential;
2. classify raw environment dumps as `RESTRICTED`;
3. scan and redact before ingestion;
4. reject credential patterns in machine-readable manifests;
5. never log secret values in validation reports.

The validator contains adversarial tests using synthetic tokens only.

## Typed empty states

`TOKEN_VAZIO` is not a number and cannot be averaged, logged, compared with zero
or promoted to success.

```text
NOT_EXAMINED  = not investigated yet
TOKEN_VAZIO   = investigated but legitimate evidence is insufficient
OPEN_GAP      = the missing requirement is known
BLOCKED       = an explicit prerequisite prevents execution
CONTRADICTION = incompatible evidence under a comparable contract
CLOSED        = exit criteria satisfied
NOT_APPLICABLE= the dimension does not belong to the domain
```

Every unresolved state requires:

```text
reason
owner
next_action
exit_criteria
```

## Longitudinal operators

### Direct derivative

\[
D X_t = X_{t+1}-X_t
\]

A local delta between sessions, commits, artifacts or claim states.

### Reverse causal traversal

Walk explicit predecessor edges from a result.  This is not a guaranteed unique
inverse because multiple causes may produce the same observation.

### Historical antiderivative

\[
\mathcal A_n=\mathcal A_0+\sum_{k=0}^{n-1}\Delta_k
\]

with deltas for commits, sessions, evidence, contradictions and rollbacks.
Historical negative evidence is retained after correction.

### Logarithmic missingness antiderivative

\[
\mu_V(t)=\sum_i w_i\mathbf 1[s_i=\mathrm{TOKEN\_VAZIO}]
\]

\[
L_V(n)=\sum_{t\le n}\log(1+\mu_V(t))
\]

Only observed non-negative weights may be used.  The system never evaluates
`log(TOKEN_VAZIO)`; an unknown weight remains an unresolved record.

### Recursive and multiscale

The same state contract is applied through:

```text
symbol → claim → file → repository → federation
```

### Counterfactual removal

Remove one source, node, edge or model candidate and retest the bounded domain.
Survival is finite robustness evidence, not a universal theorem.

## Toroidal flux invariant

For a discrete torus:

\[
X_t\in(\mathbb Z_q)^d,
\qquad
X_{t+1}=X_t+F(X_t,\omega,\phi)\pmod q.
\]

For oriented edge flow `J`:

\[
\operatorname{div}J(v)=
\sum_{e\to v}J_e-
\sum_{e\leftarrow v}J_e.
\]

On a closed graph, when every edge is accounted for consistently:

\[
\sum_v\operatorname{div}J(v)=0.
\]

This is a mathematical accounting identity for the declared graph.  It is not
evidence that the physical Universe is toroidal.

## Hexagonal longitudinal model

The finite verifier uses an axial six-neighbor lattice with toroidal wrap.  The
longitudinal axis means time or version, not automatically physical distance.

A prior prototype used a weak rule equivalent to “three edges remain” as a
reconstruction indication.  That is not a graph-theoretic proof.  A serious
claim must test vertex connectivity `κ(G)` or declare an erasure-reconstruction
contract.

The implemented finite check establishes only:

- every node of the chosen `5×4` toroidal hex lattice has six neighbors;
- deterministic edge-flow accounting has zero global divergence;
- all twenty single-vertex removals preserve connectivity in that fixture.

It does not establish arbitrary `k`-removal reconstruction.

## Permutation contract

An admissible permutation changes input order only.  Canonicalization sorts by
stable ID, serializes UTF-8 JSON with fixed separators and appends one newline.
Therefore:

\[
H(\pi K)=H(K)
\]

for admissible reorderings.  A semantic mutation must change the digest.

## Terms and non-alias rules

The Drive knowledge matrix records forty requested terms and variants.  Important
boundaries include:

```text
ZRF observed != ZFR resolved
OMNI third-party docs != OMINI defined
numeric occurrence 888 != canonical meaning
999 symbolic governance threshold != physical constant
hexagonal label != implemented hexagonal algorithm
quantum vocabulary != measured quantum effect
kernel name != operating-system kernel execution
baremetal documentation != bare-metal runtime proof
```

`ZFR`, `OMINI`, `888` and `CVV` remain unresolved or not examined.  Silent
spelling repair and semantic merging are prohibited.

## Repository authority

```text
RafGitTools → control plane, security, schema, validation, reports
Matem-tica- → definitions, proofs, counterexamples, finite verification
ChipQuantum → C/fixed-point implementation, ABI and KAT
papers      → academic synthesis, limitations and falsifiers
Mapa        → pointer-only navigation and rollout
Drive       → corpus, revision history and knowledge matrix
```

## Run

```bash
python3 scripts/federation/knowledge_antiderivative.py \
  --profile configs/knowledge-antiderivative-v1.json \
  --report artifacts/knowledge-antiderivative-report.json

python3 -m pytest -q tests/federation/test_knowledge_antiderivative.py
```

## Expected finite evidence

```text
hex torus degree set                 = [6]
global flow divergence              = 0
circular-shift integer energy       = preserved
canonical digest under reordering   = preserved
typed TOKEN_VAZIO round-trip        = preserved
single-vertex removals in fixture   = connected
```

## Claim boundary

```text
Drive corpus fully scanned                      = false
private payload copied into GitHub              = false
secret value copied                             = false
typed VAZIO contract implemented                = true
finite toroidal/hexagonal checks implemented    = true
physical toroidal universe proven               = false
universal reconstruction theorem proven         = false
Voynich decoded                                  = false
external academic validation                    = TOKEN_VAZIO
ARM32 runtime for these checks                   = TOKEN_VAZIO
claim_allowed                                    = false
```
