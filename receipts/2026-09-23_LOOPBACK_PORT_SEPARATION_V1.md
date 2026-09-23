# Receipt — loopback port separation V1

Date: 2026-09-23
Kind: CROSS_COMPONENT_CONTRADICTION_FIX
claim_allowed: false

## Contradiction observed

Two independent services were configured to bind the same loopback address/port:

- RafGitTools Raf Bridge/Kiwi: `127.0.0.1:8765`
- Termux RAFCODEΦ health server/client: `127.0.0.1:8765`

On the same Android network namespace these listeners cannot be concurrently bound to the same address/port.

## Decision

Preserve the browser bridge on `8765` because its extension manifest, userscript and chat endpoint already use that stable surface.

Move only Termux health to `8766`.

```text
8765 = Raf Bridge / Kiwi / chat
8766 = Termux health read-only
8080 = local OpenAI-compatible llama endpoint (existing default)
```

## RafGitTools delta

- TermuxHealthProbe default endpoint/port -> 8766.
- health-probe tests updated.
- dedicated regression test asserts RafBridgePrefs.BRIDGE_PORT != TermuxHealthProbe.DEFAULT_PORT.
- health contract documents the namespace split.

Paired producer change is required in `rafaelmeloreisnovo/termux-app-rafacodephi`.

## Evidence boundary

SOURCE_IMPLEMENTED in this branch.
CI/device concurrent-bind evidence remains TOKEN_VAZIO until paired branches pass and the same artifacts execute on Android.

## R3

F_ok: collision made explicit and guarded in source.
F_gap: paired Termux branch CI + physical simultaneous listeners.
F_next: update Termux default to 8766, run both suites, then device smoke with both services active.
