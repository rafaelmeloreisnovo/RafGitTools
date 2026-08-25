# RafGitTools — Source/Build Evidence — 2026-08-14 — V1

Status deste registro: **APPEND-ONLY EVIDENCE SNAPSHOT**

Este documento congela a evidência observada do primeiro BUILD atual verificável do RafGitTools. Ele não deve ser reescrito para representar estados futuros; novos estados devem gerar novos receipts/checkpoints.

## Identidade

- repository: `rafaelmeloreisnovo/RafGitTools`
- base main observada: `d62bb58f33624ecad888f86e9f95e33deb2f91be`
- candidate branch: `hardening/first-compile-run-triangle-20260814`
- executed commit: `bbdb556a59c06a23cc2f6df6ba0ae7c98466a4fa`
- PR: `#346`
- observed date: `2026-08-14`

## GitHub Actions evidence

- workflow: `Android Client Build`
- run id: `31821491676`
- job id: `94835531838`
- runner id: `1000132315`
- conclusion: `success`

### Steps observados como PASS

1. source checkout;
2. Java 17 configuration;
3. Android SDK configuration;
4. custody/structural unit tests;
5. authentication unit tests;
6. full dev unit tests;
7. Android lint;
8. `assembleDevDebug`;
9. APK verification + build receipt;
10. artifact upload.

## Artifact custody

- artifact name: `RafGitTools-devDebug`
- artifact id: `9227343409`
- GitHub archive digest: `sha256:2f92034fc4a4a1c9242453798c8eae6e1d68b134e8ff39b46ea4c283c976eb09`
- artifact expiration observed: `2026-09-13T17:02:07Z`

O digest do ZIP de artifact do GitHub **não substitui** o SHA-256 do APK interno.

## APK identity

- file: `app-dev-debug.apk`
- bytes: `24,672,130`
- SHA-256: `115b9cb1e71f53f16b2648924a09549b8e5e0b9e453280cab2e7f183a411ebf6`
- ZIP CRC: PASS
- `armeabi-v7a`: PRESENT
- `arm64-v8a`: PRESENT
- dual ABI gate: PASS

## Build receipt

- schema: `rafgittools.android-build-receipt.v1`
- receipt SHA-256: `f124ac18a9f1e158aa764a12b49a25dbf54cc870cca8359e0355416bee5219a5`
- artifact gate: PASS
- runtime state: `TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED`
- `claim_allowed=false`
- `release_allowed=false`

## Triângulo

```text
SOURCE(commit exact) = PASS
BUILD(APK/hash/ABI)   = PASS
DEVICE(same bytes)    = TOKEN_VAZIO
```

Este checkpoint **não afirma** instalação, launch, estabilidade, rede real, credencial real, Git remote real, provider externo real ou release readiness.

## Divergências documentais corrigidas

No estado documental anterior ainda apareciam como atuais:

- Actions bloqueado antes de runner;
- APK devDebug inexistente;
- multi-provider como stub;
- offline queue sem workers/storage;
- testes do head como não executados.

Após o run `31821491676`, essas descrições não podem mais ser usadas como estado atual. Elas permanecem apenas como histórico/proveniência.

## Próxima evidência válida

O próximo checkpoint deve conter, para **o mesmo APK SHA-256**:

- device/model;
- Android version;
- ABI observada;
- package/application id;
- install exit/result;
- launch exit/result;
- runtime receipt;
- commit revalidado;
- APK SHA-256 revalidado.

Se os bytes ou commit mudarem, inicia-se outra cadeia; não se reutiliza este BUILD como prova.
