# RafGitTools — Root Loose and Unusual Text Files Inventory V1

State: `ROOT_DIRECT_ENTRIES_CLASSIFIED_PARTIALLY / RECURSIVE_REPOSITORY_SCAN_NOT_RUN`
claim_allowed: false

## Scope and source

Observed repository: `rafaelmeloreisnovo/RafGitTools`, default branch `main`, after PR #576 merge commit `695bed743c5b22427ebc7f6cf4b71169208001bc` (2026-09-29).

A provider directory read returned 53 direct root files and 45 direct root directories. This note classifies root-level filenames that are extensionless, have an unusual extension, or whose suffix conflicts with the apparent content. It does not inventory the contents of the 45 directories and does not mean any item is untracked. Full recursive classification remains `PENDING`.

## Findings

| Path | Observed size | Evidence from content | Classification | Handling |
|---|---:|---|---|---|
| `.gitignore` | 331 B | Git ignore patterns | Extensionless dotfile; text configuration | Keep as configuration; not corpus text |
| `COPYING` | 35,149 B | Begins with GNU General Public License, version 3 | Extensionless legal text | Preserve; compare full content/hash against other license files before deduplication |
| `LICENSE` | 2,430 B | Begins with GNU General Public License, version 3 | Extensionless legal text | Preserve; different size from `COPYING`, so identical content is not established |
| `Makefile` | 1,583 B | Make targets and shell recipes | Extensionless build instructions | Preserve as build file; never classify as corpus TXT |
| `gradlew` | 8,656 B | Starts with `#!/bin/sh`; Gradle wrapper | Extensionless shell executable text | Preserve executable role; do not rename to `.txt` |
| `Arduíno.txt` | 66,269 B | Portuguese technical/philosophical document about Arduino and low-level code | Plain-text document | Keep under current name; content/encoding hash not independently checked here |
| `uniao.txt` | 59,592 B | Starts with Bash shebang and says it can be run with Bash | Shell-source text stored with a `.txt` suffix | Treat as source text, not ordinary prose; do not execute or rename during inventory |
| `sss.mfd` | 27,476 B | Provider renders readable prose and embedded Bash heredoc/script instructions | Unusual `.mfd` suffix; mixed text/script-like content | Preserve exact path; do not execute; determine intended format/owner before any rename |
| `Lincense4.md` | 11,300 B | Markdown text; filename spelling is anomalous | Markdown document with likely filename typo | Record anomaly; no rename until references and intent are checked |

The root also contains `.zip` entries that the provider identifies as binary archives; those are not TXT and must be handled as archives. Their internal members have not been inventoried in this pass.

## Classification rule

- A missing or unfamiliar suffix is a filename property, not proof that bytes are text.
- A text-like file can be source code, a build recipe, legal text, documentation, or mixed content. Record both observed content type and operational role.
- Do not rename, execute, delete, or deduplicate during classification. Bind any later change to a separate plan, exact hash, tests, and provenance.
- A filename ending in `.txt` is not necessarily prose: `uniao.txt` is shell-source text by its observed shebang.

## Gaps and next evidence

- `RECURSIVE_FILE_LIST=TOKEN_VAZIO`: the 45 child directories have not been recursively enumerated.
- Per-file SHA-256 values and Git blob IDs for the table above: `TOKEN_VAZIO`.
- Exact encoding validation for the text files: `TOKEN_VAZIO`.
- Whether `COPYING` and `LICENSE` are intentionally separate license notices: `TOKEN_VAZIO`.
- Intended native format/producer for `sss.mfd`: `TOKEN_VAZIO`.
- Whether `Lincense4.md` is referenced elsewhere and may safely be renamed: `TOKEN_VAZIO`.

F_next: create and run a recursive, read-only tracked-file inventory that records path, byte count, detected text/binary category, suffix anomaly, Git blob ID and SHA-256; then attach its exact commit and receipt here. No corpus body should be copied into the public repository by this inventory.
