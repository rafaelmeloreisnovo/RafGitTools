# inventarioRAF L0 — license, credits and provenance

## Governing source license

This isolated module is contributed to the existing RafGitTools repository under its root GNU GPL version 3 or later (GPL-3.0-or-later) source policy. Root LICENSE is authoritative. Keep SPDX headers, the LICENSE and the accompanying source available where the GPL requires. No new independent license or relicensing of existing files is asserted here.

## Attribution boundary

New local design/source: inventarioraf.h, inventarioraf.c, test_inventarioraf.c, Makefile, README.md and this scope note, authored in the RAFAELIA/RafGitTools collaboration. Human contribution/ownership beyond the authenticated repository maintainers must be verified separately; generated code is not a claim of exclusive patentability, novelty, or authorship of a third party's underlying APIs/algorithms.

Existing platform and third-party owners remain independent:
- ELF, DEX, ZIP format families and Android APIs are external specifications; implementing a parser does not transfer their ownership or guarantee compatibility.
- Android OS, NativeActivity, ART, APK installer, linker, signing and permission model are Android/platform boundaries. The freestanding core does not copy/replace those systems.
- Existing RafPolimata ApkC bootstrap, RafGitTools Kotlin/Compose stack and silicon_light source remain unmodified, with their existing credit/license records preserved.
- The C host test harness, compiler, linker, readelf and GitHub Actions runner are **host/build-time** tools; do not falsely label them zero-dependency.
- No GPL-incompatible vendored code, external algorithms' implementation files, secrets or private Drive data were imported in this isolated PR.

Before distribution, independently confirm the relevant root LICENSE, source attributions, transitive license obligations, Android application identifiers, signing key custody and artifact/source correspondence.

SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM. claim_allowed=false; release_allowed=false until gates.
