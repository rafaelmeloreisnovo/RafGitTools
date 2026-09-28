# SESSION FULL EXECUTOR ADAPTER V1

Role: local RafGitTools adapter to the Mapa-authoritative session dispatch.

## Authority pointer

The adapter uses an **immutable Git commit** as authority, not an equality
assertion against the moving `main` branch:

- repository: `rafaelmeloreisnovo/Mapa`
- authority commit: `959a64d497c87bc84b35b70dc6918fe036f79147`
- branch context: `main`
- paths: `data/dispatch/session_full_20260928/`

A later `main` commit does not by itself invalidate the pinned dispatch.
Re-evaluate the pin only when the relevant source paths change or an explicit
successor supersedes this authority commit. This prevents an endless
"pin latest main → main advances → stale pin" loop while preserving provenance.

## Local responsibility

- WS01: CI/provider/problem-report correction loop.
- WS03: Templo Vivo public visual-anchor/pointer side.
- WS10: local execution adapter.
- WS04/WS08: supporting receipt/reproduction responsibilities.

Private bodies remain under Rafaelia_Private authority. RafGitTools must not
copy them to public repository state.

Execution contract:

`bind immutable upstream ref → select one work order → resolve SOURCE/AUTHORITY/TARGET/EVIDENCE → smallest reversible action → local gate → cross-edge gate if required → receipt → R3`.

The numeric dimension weights in upstream work orders are scheduling/allocation
metadata only. They are not confidence scores, truth values, authorship proof
or model weights.

No automatic merge/release is authorized by this adapter.
`claim_allowed=false`.
