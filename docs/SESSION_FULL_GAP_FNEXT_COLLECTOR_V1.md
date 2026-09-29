# Session Full Gap/F_next Collector V1

State: `IMPLEMENTED_UNTESTED` · `claim_allowed=false`

RafGitTools consumes the full-session route set **read-only** from an exact Mapa
commit. It validates Git HEAD, Git blob identities, exactly 18 unique
`MC-W01..MC-W18` route pins and the non-promotion boundary.

Examples:

```bash
python3 scripts/resolve_session_full_gap_fnext.py --mapa-checkout /path/to/Mapa
python3 scripts/resolve_session_full_gap_fnext.py --mapa-checkout /path/to/Mapa --wave 0
python3 scripts/resolve_session_full_gap_fnext.py --mapa-checkout /path/to/Mapa --workstream MC-W11
```

A successful collection means only that the routing overlay is structurally
reconstructible. It does not execute the agents or producer workstreams.

`AGENT_ASSIGNMENT != AGENT_EXECUTION`
`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`
