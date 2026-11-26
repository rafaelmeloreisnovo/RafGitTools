# CommonMark 0.33.0 dependency retention — scoped candidate

Producer: RafGitTools PR #836, successor to combined candidate 2e6c37f2c612cb98fe91770e7b80e76dc1c582d0; #838 remains a separate historical candidate until integration is verified.

## Dependency boundary

- `org.commonmark:commonmark:0.33.0`: Java Markdown parser, hosted Android/JVM runtime dependency (Gradle implementation).
- `org.commonmark:commonmark-ext-gfm-tables:0.33.0`: GFM table syntax extension, same hosted runtime boundary.
- Prior versions: both 0.29.0. Candidate aligns parser and extension atomically; no new artifact family, no native ABI or freestanding claim.
- Upstream: [commonmark-java tag commonmark-parent-0.33.0](https://github.com/commonmark/commonmark-java/tree/commonmark-parent-0.33.0), annotated tag object `66cdff1b0481f202f7615930ad00147b8b0b6a6f`.
- License: BSD-2-Clause, verified in tagged [parent POM](https://github.com/commonmark/commonmark-java/blob/commonmark-parent-0.33.0/pom.xml) blob `0c4b9e4bff61ad6375daf9b89b5563930394a101` and [LICENSE.txt](https://github.com/commonmark/commonmark-java/blob/commonmark-parent-0.33.0/LICENSE.txt) blob `604b777d32c7f9c3b5b905a99ebc4665a5c5add3`. Preserve copyright, conditions and disclaimer in redistributions; packaged notices and actual resolved transitive graph: TOKEN_VAZIO until artifact inspection.

## Rationale and smallest authorial alternative

Retain existing parsing behavior and table extension rather than remove functionality or copy upstream logic. A future independently implemented bounded tokenizer, block/inline parser and explicit table adapter could replace only proven supported syntax. Before substitution, inventory current consumers, build a differential corpus with license-cleared fixtures, verify nesting, escapes, malformed inputs, tables, Unicode and resource limits, and compare output/latency on ARMv7 and ARM64. Do not claim replacement or equivalent behavior from a proposal.

## Evidence and exit

Predecessor individual CI is not combined-head evidence. Combined predecessor workspace run 45858582995 completed success; START 45858582926 was in progress at observation. This successor requires exact-head START, Android build/test/lint, CodeQL and review. Resolved artifact digest, license notices, real device behavior and final successor CI: TOKEN_VAZIO / NOT_RUN until independently observed. No release, production promotion or automatic merge. Rollback: revert the dependency changes together by reviewed corrective successor, preserving original commits and evidence. claim_allowed=false.
