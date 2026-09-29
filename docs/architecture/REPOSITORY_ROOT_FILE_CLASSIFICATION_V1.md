# RafGitTools — Root Loose and Unusual Text Files Inventory V1

State: `ROOT_AND_TWO_LEVEL_DIRECTORY_LISTINGS_OBSERVED / RECURSIVE_SCAN_PARTIAL`
claim_allowed: false

## Scope and source

Observed repository: `rafaelmeloreisnovo/RafGitTools`, default branch `main`, after PR #576 merge commit `695bed743c5b22427ebc7f6cf4b71169208001bc` (2026-09-29).

A provider root listing returned 53 direct root files and 45 directories. This inventory now includes the root and two directory levels (see addendum); it still does not cover the 52 third-level directories found in the observed listings and does not mean any item is untracked. Full recursive classification remains `PENDING`.

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

- `RECURSIVE_FILE_LIST=PARTIAL`: the root and two directory levels were listed; 52 third-level directories remain unread (see addendum).
- Per-file SHA-256 values and Git blob IDs for the table above: `TOKEN_VAZIO`.
- Exact encoding validation for the text files: `TOKEN_VAZIO`.
- Whether `COPYING` and `LICENSE` are intentionally separate license notices: `TOKEN_VAZIO`.
- Intended native format/producer for `sss.mfd`: `TOKEN_VAZIO`.
- Whether `Lincense4.md` is referenced elsewhere and may safely be renamed: `TOKEN_VAZIO`.

F_next: create and run a recursive, read-only tracked-file inventory that records path, byte count, detected text/binary category, suffix anomaly, Git blob ID and SHA-256; then attach its exact commit and receipt here. No corpus body should be copied into the public repository by this inventory.



## Addendum — nested directory and text-as-script findings (2026-09-29)

### Read coverage

The root listing at source main commit 695bed743c5b22427ebc7f6cf4b71169208001bc returned 53 files and 45 directories. Directory listings were completed for those 45 directories and their 101 immediate child directories: 146 distinct non-root directory listings, plus the root listing. Across these levels, 1,022 nested file entries and 53 root file entries were returned (1,075 entries total).

The 101 child-directory listings revealed 52 further directories. Their contents were not read: the GitHub repository reader returned “GitHub read limit reached (150 reads an hour).” Therefore this is a two-level partial inventory, not a recursive repository census. Deeper files, including any additional unusual suffixes, remain TOKEN_VAZIO.

### Confirmed no-extension and suffix anomalies

| Path | Observed content/role | Git blob SHA or state |
|---|---|---|
| IaCopiler/.new | One line-feed byte; empty placeholder, intended role unknown | 8b137891791fe96927ad78e64b0aad7bded08bdc |
| _incoming/.keep | One line-feed byte; empty placeholder | 8b137891791fe96927ad78e64b0aad7bded08bdc |
| _upcoming/1 | One line-feed byte; empty placeholder | 8b137891791fe96927ad78e64b0aad7bded08bdc |
| Livro/LICENSE | Text legal notice naming Creative Commons Attribution 4.0 | 7bfdce1d08f37dfdf31a40fec9b0aa7c3edcc0a7 |
| BrowserRaf/internal/Makefile | Makefile build recipe | d97022b5557eb17a3417971cae627dbf1ad35330 |
| _incoming/Makefile.client | Text Makefile for the freestanding raf_client build; .client is a role suffix, not a format | 055d845988c7bee9774114ea083dc5664538a9b6 |
| sss.mfd | Readable mixed prose and shell-script instructions; format/producer unresolved | 491129cbffb6cf931db4f025df239a05d4deb244 |
| uniao.txt, Livro/uniao.txt | Both begin with a Bash shebang; same Git blob, so these two paths are byte-identical | 765338f676d36aacfc12c629e9651dbd197e1ecb |

Additional extensionless build files named Makefile were listed at: native/energy_cascade_l0_v1/Makefile, native/knowledge_campus_l0_v1/Makefile, native/rafcode_federation_v1/Makefile, native/rafcode_route_exec_v1/Makefile, native/rafcode_route_federation_bridge_v1/Makefile, native/rafcode_route_v1/Makefile, native/silicon_light_v1/Makefile, rafaelia/block1/Makefile, and rafaelia/omega_hybrid/Makefile. A path named Makefile is classified by role, not as a generic text note.

The root COPYING and LICENSE both begin with GPL-3.0 text, but their byte counts and Git blob SHAs differ; they are not duplicates by Git identity. Do not delete or merge them without reviewing the full legal content and repository references. Root Lincense4.md has a spelling anomaly in its filename; no rename was made.

### .txt may contain executable source

Repository code search returned 50 paths ending in .txt; this is an indexed result, not a complete tracked-file count. At least these 17 paths have a shell shebang at the beginning of the file, so they are script/generator source stored with a .txt suffix:

- Livro/bundle.txt
- BrowserRaf/GaiaPhi.txt
- Livro/whitepaper.txt
- Livro/benchmark.txt
- Livro/TOTAL.txt
- Livro/gap.txt
- Livro/bibliaCorpus.txt
- Livro/iaRAF.txt
- Livro/pipeline.txt
- uniao.txt and its byte-identical path Livro/uniao.txt
- Livro/MATIBHE.txt
- Livro/geolm.txt
- Livro/MATIBHE_MIL.txt
- Livro/MATIBHE_PLUS.txt
- Livro/IMPLEMENTA.txt
- _incoming/RAF_STATECOMP.txt

Other search matches contain embedded or generated scripts rather than a file-leading shebang. Do not infer executability from the .txt suffix or from a code fragment inside prose. Review the full body and invocation contract before any execution.

### Receipt and remaining gate

The findings above bind selected unusual files to Git blob IDs. Those IDs are Git object identities, not SHA-256 content digests. Full per-file SHA-256, encoding validation for every text file, reference graph for all loose paths, and content reads for the 52 deeper directories remain pending. No file was renamed, deleted, or executed.

F_ok: extensionless placeholders, build files, selected unusual suffixes, and selected .txt scripts are identified with path/content evidence.
F_gap: 52 deeper directories not read; complete recursive path list, file hashes, and all-format classification not produced.
F_next: rerun a recursive read-only tracked-file inventory after the provider read limit resets; emit manifest with path, size, Git blob SHA, SHA-256, MIME/sniff result, suffix/role, and evidence; update this document from that receipt.
