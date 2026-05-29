---
name: termuxforge
description: "Skill do raf_termux_forge — compilador APK bare-metal C para Android. Ativar sempre que o usuário trabalhar com: (1) compilação direta de C para APK ARM64 sem Android Studio, Gradle ou NDK clássico — apenas clang/lld + zipalign + apksigner; (2) alinhamento de página 16 KiB obrigatório no Android 15+ (compile flags -Wl,-z,max-page-size=16384); (3) DEX bytecode generation a partir de Java mínimo ou Smali bruto, ou bridge JNI para C puro (estratégia 'one Activity → libnative.so'); (4) JNI bridge rafaelia_bridge.c — gap conhecido, precisa ser criado do zero; (5) AndroidManifest.xml mínimo viável, MainActivity em uma única classe, layout XML inline; (6) toolchain Termux — pkg install clang aapt2 apksigner zipalign ecj, build sem desktop; (7) assinatura debug/release com keystore local (apksigner sign --ks ks.jks); (8) instalação direta no device (adb install ou pm install local) sem Play Store; (9) target Android 15 API 35 com fallback API 21+ via minSdkVersion. Usar também quando o usuário pedir 'APK do Termux', 'compilar C para Android sem Gradle', 'Vectras Android build', 16 KiB page alignment, ou qemu_rafaelia Android packaging."
---

# TERMUX FORGE — Compilador APK Bare-Metal

## Premissa

Construir um APK Android **direto do Termux**, sem Android Studio, sem Gradle, sem `./gradlew assembleDebug`. A toolchain inteira são quatro ferramentas: `clang`, `aapt2`, `apksigner`, `zipalign`. Para DEX: `ecj` + `d8` (ou Smali).

**Alvo:** Android 15 (API 35), ARM64-v8a, **16 KiB page alignment**.

```
.c → .so (bare-metal C com JNI) → resources.apk → unsigned.apk → aligned.apk → signed.apk
```

---

## 1. Pré-requisitos Termux

```bash
pkg update
pkg install clang lld make aapt2 apksigner zipalign ecj
# opcional: smali baksmali p/ caminhos não-Java
```

Verificar versões:
```bash
clang --version          # >= 17 recomendado para 16KB page
aapt2 version            # >= build-tools 33+
apksigner --version
```

---

## 2. Estrutura Mínima do Projeto

```
forge_demo/
├── AndroidManifest.xml
├── src/
│   └── org/rafaelia/forge/
│       └── MainActivity.java        ← uma única classe Java mínima
├── jni/
│   └── rafaelia_bridge.c            ← código C bare-metal + JNI
├── res/
│   └── values/
│       └── strings.xml
├── build/                            ← saída intermediária
└── forge.sh                          ← script único de build
```

---

## 3. AndroidManifest.xml Mínimo

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          package="org.rafaelia.forge"
          android:versionCode="1"
          android:versionName="1.0">
    <uses-sdk android:minSdkVersion="21" android:targetSdkVersion="35"/>
    <application android:label="@string/app_name">
        <activity android:name=".MainActivity"
                  android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
    </application>
</manifest>
```

---

## 4. MainActivity.java Mínima

```java
package org.rafaelia.forge;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
    static { System.loadLibrary("rafaelia_bridge"); }
    public native String rafaelia_seal();      // ← chama C

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        TextView tv = new TextView(this);
        tv.setText(rafaelia_seal());
        setContentView(tv);
    }
}
```

---

## 5. rafaelia_bridge.c (JNI bare-metal)

**Este é o arquivo que NÃO EXISTE no projeto atual — gap crítico.**

```c
#include <jni.h>
#include <string.h>
#include "gaia.h"           // ← integração com gaia_core_v2

JNIEXPORT jstring JNICALL
Java_org_rafaelia_forge_MainActivity_rafaelia_1seal(JNIEnv *env, jobject self) {
    static const char seal[] = "RAFCODE-Φ-∆RafaelVerboΩ-𓂀ΔΦΩ";
    return (*env)->NewStringUTF(env, seal);
}

// Bridge real (esqueleto): expor rmr_hash, rmr_vec_cosine, gaia search
JNIEXPORT jbyteArray JNICALL
Java_org_rafaelia_forge_MainActivity_aetherHash(JNIEnv *env, jclass cls, jbyteArray data) {
    jsize len = (*env)->GetArrayLength(env, data);
    jbyte *p  = (*env)->GetByteArrayElements(env, data, NULL);
    uint8_t digest[32];
    aether_t h = aether((const uint8_t*)p, (size_t)len);
    memcpy(digest, h.blake3, 32);
    (*env)->ReleaseByteArrayElements(env, data, p, JNI_ABORT);
    jbyteArray out = (*env)->NewByteArray(env, 32);
    (*env)->SetByteArrayRegion(env, out, 0, 32, (jbyte*)digest);
    return out;
}
```

---

## 6. forge.sh — Script Único de Build

```bash
#!/bin/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

NAME=forge_demo
PKG=org.rafaelia.forge
SDK=$PREFIX/share/aapt2          # ajustar
PLATFORM=$PREFIX/share/aapt2/android-35.jar
KEYSTORE=ks.jks
KEYALIAS=forge
KEYPASS=android

mkdir -p build/{lib/arm64-v8a,classes,bin}

# 1) compilar .c → .so com 16 KiB page alignment
clang --target=aarch64-linux-android21 \
    -fPIC -O2 -shared \
    -Wl,-z,max-page-size=16384 \
    -Wl,-z,common-page-size=16384 \
    -o build/lib/arm64-v8a/librafaelia_bridge.so \
    jni/rafaelia_bridge.c

# 2) compilar Java → .class (Eclipse Compiler for Java)
ecj -source 17 -target 17 -d build/classes \
    -cp $PLATFORM \
    src/org/rafaelia/forge/MainActivity.java

# 3) .class → .dex (usar d8 do build-tools ou ART)
d8 --release \
    --lib $PLATFORM \
    --output build/bin \
    build/classes/org/rafaelia/forge/MainActivity.class

# 4) compilar recursos com aapt2
aapt2 compile -o build/res.zip --dir res/
aapt2 link \
    -I $PLATFORM \
    --manifest AndroidManifest.xml \
    -o build/${NAME}-unsigned.apk \
    --auto-add-overlay \
    build/res.zip

# 5) injetar classes.dex + libs
cd build
cp bin/classes.dex .
zip -j ${NAME}-unsigned.apk classes.dex
zip -r ${NAME}-unsigned.apk lib/
cd ..

# 6) zipalign — alinhar APK para 16 KiB
zipalign -p -f -v 16384 \
    build/${NAME}-unsigned.apk \
    build/${NAME}-aligned.apk

# 7) assinar
apksigner sign \
    --ks $KEYSTORE \
    --ks-key-alias $KEYALIAS \
    --ks-pass pass:$KEYPASS \
    --key-pass pass:$KEYPASS \
    --out build/${NAME}.apk \
    build/${NAME}-aligned.apk

# 8) verificar
apksigner verify --verbose build/${NAME}.apk
echo "APK pronto: build/${NAME}.apk"
```

---

## 7. Por que 16 KiB Page Alignment

Android 15+ exige que **bibliotecas nativas (.so)** estejam alinhadas a 16 KiB para suportar dispositivos com page size = 16384. Sem isso:

- App **não instala** em devices com kernel 16K.
- App **carrega mas trava** em runtime.

Como verificar:
```bash
unzip -p app.apk lib/arm64-v8a/libfoo.so | head -c 64 | xxd
# ou
zipalign -c -v 16384 app.apk    # exit code 0 = ok
```

**Flags de compilação obrigatórias:**
```
-Wl,-z,max-page-size=16384
-Wl,-z,common-page-size=16384
```

E o `zipalign -p -v 16384` no APK final.

---

## 8. Gerar Keystore Local (uma vez)

```bash
keytool -genkeypair -v \
    -keystore ks.jks \
    -keyalg RSA -keysize 2048 \
    -validity 10000 \
    -alias forge \
    -storepass android -keypass android \
    -dname "CN=RafaelVerboOmega, O=RAFAELIA, C=BR"
```

---

## 9. Instalar no Device (sem Play Store)

```bash
# via adb (USB ou Wi-Fi debugging)
adb install -r build/forge_demo.apk

# ou local no próprio Termux (root opcional via Termux:API ou Shizuku):
am start -n org.rafaelia.forge/.MainActivity   # após install

# ou: cat APK | nc para tablet (qualquer canal)
```

---

## 10. Integração com qemu_rafaelia / Vectras Android

Para empacotar a Vectras como APK rodável em Android:

```
qemu_rafaelia core (.c, ~7 layers)
   → librafaelia_core.so (16 KiB aligned)
   → JNI bridge rafaelia_bridge.c
   → MainActivity carrega .so
   → VMExecutionScreen.kt consome métricas REAIS (não random())
```

**Gaps de integração (cf. auditoria):**
- JNI bridge não existe → criar `rafaelia_bridge.c` mínimo (este skill).
- ARM64 CPUID stub 8 linhas → expandir leitura `MIDR_EL1` real via inline asm.
- `meson.build` ignora 12 arquivos C → adicionar ao build.
- `VMExecutionScreen.kt` usa `random()` → substituir por chamada JNI real.

---

## 11. Heurística TERMUX FORGE

```
Passo 1 — VERIFICAR TOOLCHAIN:
   clang ≥ 17 · aapt2 · apksigner · zipalign · ecj
   Android SDK platform .jar para API alvo

Passo 2 — ANDROID 15 GATES:
   targetSdkVersion ≥ 31 para AGP novo (mas aqui não usamos AGP)
   16 KiB page alignment SEMPRE (flags + zipalign 16384)

Passo 3 — JNI MÍNIMO:
   library_name = "rafaelia_bridge"
   função Java_<pkg>_<class>_<method> assinatura exata

Passo 4 — BUILD:
   .c → .so → .class → .dex → res.apk → align → sign → verify

Passo 5 — INSTALAR:
   adb install -r ou pm install local

Passo 6 — RETRO:
   F_ok / F_gap / F_next
```

---

## 12. Modelo de Resposta TERMUX FORGE

```
COMPONENTE:        [bridge .c / Activity .java / Manifest / build script]

TARGET:            API 35 · ARM64 · 16 KiB page

FLAGS CRÍTICAS:
  -Wl,-z,max-page-size=16384      ☐
  -Wl,-z,common-page-size=16384   ☐
  zipalign -p 16384                ☐

JNI ASSINATURA:    Java_<pkg>_<class>_<method>

INTEGRAÇÃO:
  gaia.h?          sim/não
  rmr_*?           sim/não
  vectras?         sim/não

VERIFICAÇÕES:
  apksigner verify ✓/✗
  zipalign -c 16384 ✓/✗
  loadLibrary não trava ✓/✗

RETRO[3]:
  F_ok:   ...
  F_gap:  ...
  F_next: ...
```

---

## 13. Anti-padrões TERMUX FORGE

- Esquecer `-Wl,-z,max-page-size=16384` (APK não instala em Android 15+ device 16K).
- Compilar com NDK desktop e tentar copiar pro Termux (path hardcoded explode).
- Usar `gradle` "só pra um passo" — perde determinismo do pipeline.
- Esquecer `zipalign` antes de assinar (apksigner aceita mas tooling moderno reclama).
- Carregar `.so` com nome diferente de `loadLibrary("rafaelia_bridge")` (UnsatisfiedLinkError).
- Misturar JNI assinaturas com `_1` (underscore-1 = escape de `_` em nomes Java) errado.
- Skipar `apksigner verify` — bugs de assinatura só aparecem na install.

---

*TERMUX FORGE ⊂ RAFAELIA · clang + aapt2 + apksigner + zipalign · 16 KiB · sem Gradle · gap JNI bridge crítico*
