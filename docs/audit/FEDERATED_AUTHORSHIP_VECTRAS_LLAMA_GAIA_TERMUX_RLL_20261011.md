# RafGitTools — upstream and authorial import gate for Vectras / Termux / Llama / GAIA / RLL

**State:** METADATA_ONLY / NO_SOURCE_TRANSPLANT / HOLD_NO_PROMOTION
**Source authority:** producer repositories retain copyright, history and rights.
**Federated authoritative catalog:** `rafaelmeloreisnovo/Mapa` PR #751,
`data/assurance/federated-authorship-intake-20261011.v1.json`.
**This PR:** adds guidance, no engine, third-party code, access token,
Google Drive corpus, app/Gradle/JNI/build config, new provider admin action.

## Verified origin distinction

- Official Vectras original `xoureldeen/Vectras-VM-Android`: GPL-2.0-only
  root; local `rafaelmeloreisnovo/Vectras-VM-Android` acknowledges official
  author. Local `AboutActivity.java` carries upstream substantial structure
  (`UPSTREAM_MODIFIED`); avoid exclusive authorial attribution.
- Local Vectras `engine/rmr/src/rmr_vector_field.c`:
  project-local traceable AI-assisted addition with PR #982 and #1142;
  file SPDX `GPL-2.0-only`; first observed code blob
  `8dfb230410b20e36236dbc3863d301e16a4c595d`.
  This is not universal mathematical priority. RafGitTools root uses
  `GPL-3.0-or-later`: do **not** copy GPL-2.0-only source as a GPLv3
  derivative absent explicit applicable rights/exception/legal review.
- Termux app base `termux/termux-app`: GPL-3.0-only plus file/component
  exceptions. `termux-packages`: recipes/patches inherit package license;
  infrastructure Apache-2.0. No flattening into one authorial license.
- llamaRafaelia upstream ggml llama.cpp MIT, while local BitStack uses
  stdlib/string/stdio/math/heap and cannot be called freestanding L0.
- GAIA_phi root conventional MIT vs contradictory `LICENSE.md` combining
  clauses and Python source; BL0 C no headers is a candidate for independent
  verification only. Gaia network implementation invokes OS services.
- Rafaelia_Private custom proposed license applies to independently owned
  files only; never traverse, copy or disclose private data/corpus or secrets.
- RLL scholarly math/research has CC BY-SA 4.0 research-text terms;
  software file-level rights and science validation remain separate.

## Governance/Zero Trust integration boundary

RafGitTools can inspect provider metadata, compare source references, record
read-only provenance and preserve typed blockers. It does not gain authority
to change upstream copyright, merge PRs or declare proof of rights.

`SOURCE_ORIGIN -> GIT_BLOB -> LICENSE_FILE_SCOPE -> CONTRIBUTOR_HISTORY ->
REVIEW_AUTHORITY -> CI_EXACT_HEAD -> DISTRIBUTION_RIGHTS -> DECISION`

For new integration contract receipts preserve:
`origin_repository`, `upstream_path`, `upstream_blob`, `local_path`,
`local_blob`, `origin_class`, `license_expression`,
`obligations`, `copy_allowed=false`, `private_data_excluded=true`,
`review_state=TOKEN_VAZIO`, `claim_allowed=false`.

The Mapa registry is **pointer-only** and itself does not authenticate hashes,
legal ownership, licenses or source implementation. A validation PASS on
that JSON would prove internal schema consistency, NOT provider enforcement
or authorial rights. No data/telemetry or scanner credential included.

No direct source copy from Vectras into RafGitTools. A separately distributed
runtime/process adapter and a versioned wire protocol MAY be studied only with
technical, legal and ABI review. New code must have its own provenance and
test receipts. Physical Android/QEMU/Termux operation remains unproven in
this audit.

## F_gap / F_next
P0: per-span authorship and rights for selected Vectras local modules;
GPL2-only/GPL3 redistribution compatibility; GAIA secondary license;
RafPolimata root rights; origin of third-party binary/assets; independent
review and Mapa native protection/CodeScan gates. These remain BLOCKED.
F_next: review the Mapa registry with a genuinely independent reviewer,
then adopt only metadata-consumer/test contracts; no auto-promote, no
retroactive relicensing, no secrets or provider mutations.

Rollback: close this doc-only PR; no main/app runtime changes.
