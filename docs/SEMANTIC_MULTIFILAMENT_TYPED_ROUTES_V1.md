# RAFAELIA Omega — Semantic multi-filament route inspector V1

**Status:** source-side read-only candidate; no automatic promotion/merge.
**Authority:** RafGitTools is consumer/inspector; Mapa remains route authority; producers own their source; external proofs do not arise from graph traversal.

## DELTA

Reuses the existing data/governance/complex-network-design-relations.v1.json and the established epistemic interop ontology. No second ungoverned evidence graph is introduced. The new deterministic standard-library inspector discovers *typed multi-edge candidate paths*, retaining each original relation_id, relation_type, uncertainty_state, evidence_effect, contradiction flag and the next required falsification gate. No confidence score is fabricated, and no causal, legal, scientific or cryptographic inference is made by transitive traversal.

Existing source semantics cover mathematical objects, cross-domain analogy, declared evidence boundaries, memory, rights, and explicit TOKEN_VAZIO gaps. V1 does not measure verbal, nonverbal or physical signals. A future producer-approved successor may attach modality-specific evidence and reversible transformation contracts as typed deltas, not silently reclassify original sources.

Missing route = NOT_OBSERVED_NOT_NEGATIVE_PROOF, not zero and not demonstrated false. Source reference string = declared locator, NOT independently authenticated. Input caps: graph 256 KiB, <=4096 nodes, <=8192 edges; each route depth <=5 and output <=32 paths; BFS queued states <=2048 and expanded states <=16384. Over-budget traversal fails closed rather than producing a potentially incomplete pass. JSON duplicate keys and softened policies fail closed; graph traversal does not re-enter a node already visited in the same path.

## Direct and reverse math; conditions

For x > 1 define f(x)=ln(ln(x)); derivative f'(x)=1/[x ln(x)] and inverse f^{-1}(y)=exp(exp(y)) wherever representable. The antiderivative of f' is f(x)+C, but this is NOT the antiderivative of f. For x > 1 an antiderivative of f itself is x ln(ln(x)) - li(x) + C, where li is the logarithmic integral (not implemented in V1). Integration loses an arbitrary constant: no unique inverse-antiderivative can be inferred without boundary conditions.

For n nested logarithms the valid real domain requires every logarithm input >0; derivative f_n'(x)=1/[x ln(x) ... ln^(n-1)(x)] with valid domain. Inverse requires n nested exponentials on the appropriate range; numeric overflow is explicitly rejected. Near-boundary indicators are heuristics only, not security measurements or proofs.

NIST mathematical references: https://dlmf.nist.gov/6.2 and https://dlmf.nist.gov/4.10.

## Reproduce on the exact source tree

python3 tools/semantic_multifilament_v1.py --from-id N:RAFAELIA:B7 --to-id N:T2 --max-depth 3 --math-x 4 --math-depth 2
python3 -m unittest tests/test_semantic_multifilament_v1.py -v

The CLI outputs to stdout only. It does not mutate source artifacts, repository governance, release credentials or Drive evidence.

## Security and falsification

SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM; TOKEN_VAZIO != 0; IMPLEMENTED_UNTESTED != PASS. Traversal cannot promote an analogy, cooccurrence or missing evidence to proof. Governance/data/privacy/security are non-compensatory and are never unlocked by mathematical route discovery.

17 adversarial tests cover: canonical graph, B7 -> T2 unresolved route, analogy without evidence, contradiction preservation, unknown route, dangling endpoints, repeated ids/JSON keys, policy numeric-boolean spoof, attempted promotion of token-vazio and analogy, cycle termination, log domains, finite differences, inverse roundtrips, integration constants, overflow, and exponential path-explosion budget rejection.

No CI or independent review claim from source existence. A PASS requires exact-head run metadata and executed steps. No certificate validation, Play APK verification or rights proof is claimed.

Rollback: close the draft PR and leave main + producer source unchanged. Provider enforcement and independent review are separate gates.
