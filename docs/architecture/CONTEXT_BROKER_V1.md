# ContextBroker V1 — explicit bounded context assembly

State: `IMPLEMENTED_SOURCE / STACKED_ON_WORKSPACE_SESSION / TEST_PENDING`  
claim_allowed: `false`

## Purpose

Connect the shared WorkspaceSession to ContextBundle V2 without turning file browsing, clipboard history or a model into automatic memory ingestion.

```text
human opens source
→ explicit Add to context
→ ContextBroker
→ bounded in-memory segment
→ ContextBundle V2
```

## Limits

```text
max segment  = 32768 characters
max total    = 131072 characters
max segments = 16
```

Oversized material is rejected. It is **not silently truncated**. Binary material is rejected in V1.

## Provenance

Each segment keeps ResourceRef, source_ref, SHA-256 of materialized text, privacy class and deterministic segment ID. The broker stores text only in process memory; it does not persist clipboard history or corpus bodies.

## Privacy

Local Git resources default to `PRIVATE`. Public resources can be explicitly marked public. Unknown privacy remains `TOKEN_VAZIO` and is not promoted.

## FileBrowser slice

The FileBrowser can explicitly add the currently open text file to context. The action is never triggered by file open alone.

```text
OPEN != ADD_TO_CONTEXT
```

## Next

```text
ContextBroker
├── LOCAL_GIT (this slice)
├── NOVOexport/Navigator (next)
├── GAIA relation refs (future bounded provider)
└── Rafaelia_Private / RafPolimata typed providers
```

The next runtime step is read-only NOVOexport retrieval through the existing governed Termux/RafPolimata architecture, not direct access to another app's private filesystem.
