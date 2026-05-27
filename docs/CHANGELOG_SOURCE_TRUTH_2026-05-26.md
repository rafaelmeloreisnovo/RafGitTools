# Source Truth Changelog — 2026-05-26

## Changed files
- README.md — separa filosofia, roadmap e estado técnico verificável.
- docs/STATUS_REPORT.md — atualização para labels baseadas em evidência.
- docs/CURRENT_SOURCE_STATE_2026-05-26.md — relatório de estado atual auditável.
- docs/ARM32_TERMUX_STATE.md — formaliza escopo oficial vs experimental no ARM32/Termux.
- docs/CODEX_WORKPLAN_RAFGITTOOLS.md — checklist objetivo para agentes/Codex.
- app/src/main/cpp/native_bridge.c — adiciona `nativeAbiMask` para sanity/ABI health.
- app/src/main/cpp/CMakeLists.txt — comentários ABI + warnings seguros de compilação.
- scripts/termux/arm32_runtime_check.sh — novo caminho oficial para runtime check.
- scripts/termux_arm32_runtime_check.sh — wrapper de compatibilidade.
- scripts/native/verify_apks.sh — relatório Markdown de ABIs e modos `REQUIRE_APKS`/`VERIFY_STRICT_ABI`.

## Validation
- `./gradlew tasks` → FAIL (SDK Android indisponível neste ambiente).
- `./gradlew assembleDevDebug` → FAIL (SDK Android indisponível neste ambiente).
- `./gradlew assembleProductionDebug` → FAIL (SDK Android indisponível neste ambiente).
- `./gradlew test` → FAIL (SDK Android indisponível neste ambiente).
- `bash scripts/native/verify_apks.sh` → PASS (sem APKs; mensagem orientativa emitida).
- `bash -n scripts/termux/arm32_runtime_check.sh` → PASS.
- `bash scripts/termux/arm32_runtime_check.sh || true` → PASS (host não ARM, resultado informativo).

## Environment limitation
Build not executed in this environment because Android SDK/Gradle environment is unavailable.

## Remaining work
- implementar kernel ARM32 real além de retornos de health/stub;
- expandir testes unitários, integração e UI;
- consolidar CI para evidência contínua;
- implementar Terminal;
- implementar GPG;
- implementar LFS;
- implementar Worktree;
- implementar Webhooks;
- consolidar trilha de release final.
