# RafGitTools × NOVOexport — four-node boundary V1

Status: `PLAN_ONLY`, `claim_allowed=false`, no source/runtime/device/remote integration claimed.

| Node | Real present component | Proposed handoff |
| --- | --- | --- |
| Cellphone | `NovoexportSafInventory` and `NovoexportLibraryCatalogComposer` SAF metadata; `DriveStagingGate` separately validates staged bytes | Export a **metadata-only** bounded observation, omitting raw `content://` capability URIs |
| GitHub | `RafGitTools` as governed orchestration executor; `CONVERSATIONS_CHUNKS_PRIVATE` PR #72 as name-atlas producer | Validate a signed-off plan; add a private derived receipt via RafGitFS only after exact recipient and human approval |
| Drive | NOVOexport raw preservation + prior Name Atlas receipt | Store private source pointers and compact append-only receipts; readback required |
| ChatGPT | Inspection, inference, proposal, source-bound classification | Generate **candidates**, flag conflicts and type TOKEN_VAZIO; no self-authorized execution |

Existing `NovoexportLibraryCatalogComposer` explicitly excludes its own `CAT-NOVO-*.catalog/receipt.json` artifacts from a corpus scan to avoid recursive reindexing. Name history can be cataloged separately in the `name_atlas` metadata projection; this is a different axis from treating generated artifacts as raw corpus. Never change the filter blindly.

## V1 packet and validator

- `contracts/novoexport-four-node-handoff.v1.schema.json` describes the JSON shape.
- `scripts/verify_novoexport_four_node_handoff.py` exists in the **local downloadable pilot bundle only**, not on this GitHub branch. Its attempted publication was blocked by the tool security layer; no bypass was attempted. It makes no network calls and publishes nothing.
- `examples/handoff.plan.v1.json` exists in the **local pilot bundle only** and uses synthetic references, except a public illustrative RafGitTools commit pin; none of these constitute provider proofs.
- `tests/test_verify_novoexport_four_node_handoff.py` exists in the **local pilot bundle only**; 12 local checks succeeded for that bundle. No GitHub CI has tested this branch. Success is `PASS_SCOPED_PLAN_ONLY`, not `PASS_DEVICE`, `PASS_PRIVACY_GLOBAL`, `PASS_SECRET_SECURITY`, nor evidence that a Git SHA actually exists in a specific repository.

The commands below apply **only after extracting the linked local pilot bundle** (not directly from this GitHub branch):

```sh
python3 scripts/verify_novoexport_four_node_handoff.py examples/handoff.plan.v1.json
python3 -m unittest discover -s tests -p test_verify_novoexport_four_node_handoff.py -v
```

No tokens, PATs, Android Keystore content or GitHub Actions secrets are needed or read. GitHub/Drive authentication must stay with each provider, and no token should enter URLs, logs, receipts or source files. The public RafGitTools repo must **not** receive private corpus filenames, object IDs or sensitive relationship edges.

## Next integration without regressions

1. Source producer maintains canonical raw and name lineage: `CONVERSATIONS_CHUNKS_PRIVATE` draft PR #72. It must reconcile its 561 GitHub + 114 Drive scoped snapshots and 69-record pilot SQLite in its own private domain. Exact equivalence remains candidate-only until byte evidence.
2. RafGitTools Android adapter should eventually transform a *user-authorized* `NovoexportLibraryCatalogComposer` metadata item into this strict packet; keep private raw SAF URI inside the Android process, not the packet.
3. RafGitFS handles any GitHub write; do not introduce another PAT or uploader. Bind target repository/branch/path and plan hash under explicit approval; default deny. Names are not permissions.
4. Check provider readback, exact HEAD CI and physical phone receipt on the **actual** revision/artifact. Write the compact Drive `μWRITE` delta after state changes. Mapa retains federated routing authority.
5. Reconcile corrections through `parent/supersedes`; never delete/overwrite historical name or source assets. Reverse sync is blocked until it has its own scoped authority and tested rollback.

Falsifiers: reject secret-looking strings and `content://` URIs; reject public destination; reject autonomous execution; reject fake PASS; reject unpinned refs; reject `TOKEN_VAZIO` converted to 0; do not infer copy/mirror from name alone.

R3 = <local contract validation; physical Android bridge, provider permissions and remote CI remain TOKEN_VAZIO; connect one canary with exact SHA and independently observed receipt>.