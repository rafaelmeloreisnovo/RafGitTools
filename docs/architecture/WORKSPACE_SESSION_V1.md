# WorkspaceSession V1 — shared file-navigation state

State: `IMPLEMENTED_SOURCE / STACKED_ON_CONTEXT_BUNDLE_V2 / TEST_PENDING`  
Parent: ContextBundle V2 PR #479  
claim_allowed: `false`

## Purpose

Provide one app-scoped navigation state shared by the existing FileBrowser now and by ContextBroker/RafGitFS later.

```text
ResourceRef
→ WorkspaceTab
→ active tab
→ back / forward jump history
```

The store is intentionally reference-only:

```text
WorkspaceSession != file-body cache
WorkspaceSession != clipboard history
WorkspaceSession != model memory
WorkspaceSession != write permission
```

It stores source identity, ref/generation, locator, object identity, cursor and dirty metadata.

## V1 provider

The first wired provider is:

```text
LOCAL_GIT
```

FileBrowser opens a tab after the existing JGit read succeeds. Switching a tab reloads the body from the source repository/ref rather than duplicating the file body inside WorkspaceSession.

## Navigation invariants

- same ResourceRef reuses the same tab;
- changing path/ref changes tab identity;
- jump history is bounded to 100 entries;
- closing a tab prunes dead jump entries;
- back/forward restores the captured cursor metadata;
- activating a new branch clears forward history;
- no Git write is introduced.

## UI slice

FileBrowser receives:
- tab strip;
- active-tab selection;
- close-tab;
- back/forward jump controls.

This is the smallest reversible bridge from existing file browsing toward the requested Workbench.

## Next

```text
WorkspaceSession
→ ContextBroker
→ NOVOexportProvider(read-only)
→ ContextBundle V2
→ llamaRafaelia
```

Editing/patch publication remains behind the existing RafGitFS governed write path.
