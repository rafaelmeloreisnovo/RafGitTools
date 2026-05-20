# TERMUX FORGE — Integração ABSOLUTA no RafGitTools

Este documento adapta o fluxo **TERMUX FORGE** para uso direto neste repositório, com foco em:

- Build Android bare-metal sem Gradle para artefato JNI específico;
- Alinhamento **16 KiB** obrigatório (Android 15+);
- Ponte JNI mínima (`rafaelia_bridge.c`) para expor invariantes Toro7D/BitOmega;
- Resultado verificável por comandos reproduzíveis.

## Escopo e premissas

- Repositório alvo: `RafGitTools`.
- API alvo recomendada: `targetSdkVersion=35`, `minSdkVersion=21`.
- ABI primária: `arm64-v8a`.
- Invariantes de projeto preservados:
  - `|A| = 42`
  - `period(BitOmega) = 42`
  - `φ = (1-H)·C`
  - `gcd(Δr, R)=1` e `gcd(Δc, C)=1`

## Estrutura adicionada (neste repositório)

```
tools/termuxforge/
├── AndroidManifest.xml
├── forge.sh
├── jni/
│   └── rafaelia_bridge.c
├── res/
│   └── values/strings.xml
└── src/org/rafaelia/forge/MainActivity.java
```

## Ponte JNI mínima (gap fechado)

A bridge abaixo expõe:

- `rafaelia_seal()` (identidade de execução);
- `phiFromCH(int c_q16, int h_q16)` para `φ = (1-H)·C` em Q16.16;
- `periodGuard(int n)` para checagem do período 42.

> Observação: implementação intencionalmente mínima, sem heap em hot path.

## Build script bare-metal (Termux)

Arquivo: `tools/termuxforge/forge.sh`

Características críticas:

- `clang --target=aarch64-linux-android21 -shared -fPIC`
- `-Wl,-z,max-page-size=16384`
- `-Wl,-z,common-page-size=16384`
- `zipalign -p -f -v 16384`
- `apksigner verify --verbose`

## Equações mapeadas para o artefato JNI

- Estado toroidal: `s ∈ [0,1)^7` (representação em Q16.16 na borda JNI)
- Relaxação EMA:
  - `C_{t+1} = (1-α)C_t + αC_in`
  - `H_{t+1} = (1-α)H_t + αH_in`
  - `α = 0.25`
- Lyapunov local:
  - `φ = (1-H)·C`
- Guarda de período:
  - `x_{n+42} = x_n`

## Verificações obrigatórias

1. **Assinatura APK**
   - `apksigner verify --verbose build/forge_demo.apk`
2. **Alinhamento 16 KiB**
   - `zipalign -c -v 16384 build/forge_demo.apk`
3. **Integridade JNI**
   - `System.loadLibrary("rafaelia_bridge")` sem `UnsatisfiedLinkError`

## Retro (F_ok / F_gap / F_next)

- **F_ok:** pipeline bare-metal definido no próprio repo e reproduzível.
- **F_gap:** integração direta com `gaia.h`, `rmr_*` e coleta de métricas reais ainda é etapa seguinte.
- **F_next:** conectar bridge JNI aos módulos nativos definitivos e adicionar teste de instrumentação Android.
