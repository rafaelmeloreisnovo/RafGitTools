#!/usr/bin/env bash
set -euo pipefail

NAME=forge_demo
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$ROOT_DIR/build"
PLATFORM_JAR="${PLATFORM_JAR:-$HOME/android-platforms/android-35/android.jar}"
KEYSTORE="${KEYSTORE:-$ROOT_DIR/ks.jks}"
KEYALIAS="${KEYALIAS:-forge}"
KEYPASS="${KEYPASS:-android}"

mkdir -p "$BUILD_DIR/lib/arm64-v8a" "$BUILD_DIR/classes" "$BUILD_DIR/bin"

clang --target=aarch64-linux-android21 \
  -fPIC -O2 -shared \
  -Wl,-z,max-page-size=16384 \
  -Wl,-z,common-page-size=16384 \
  -o "$BUILD_DIR/lib/arm64-v8a/librafaelia_bridge.so" \
  "$ROOT_DIR/jni/rafaelia_bridge.c"

ecj -source 17 -target 17 -d "$BUILD_DIR/classes" \
  -cp "$PLATFORM_JAR" \
  "$ROOT_DIR/src/org/rafaelia/forge/MainActivity.java"

d8 --release --lib "$PLATFORM_JAR" --output "$BUILD_DIR/bin" \
  "$BUILD_DIR/classes/org/rafaelia/forge/MainActivity.class"

aapt2 compile -o "$BUILD_DIR/res.zip" --dir "$ROOT_DIR/res"
aapt2 link -I "$PLATFORM_JAR" \
  --manifest "$ROOT_DIR/AndroidManifest.xml" \
  -o "$BUILD_DIR/${NAME}-unsigned.apk" \
  --auto-add-overlay "$BUILD_DIR/res.zip"

(
  cd "$BUILD_DIR"
  cp bin/classes.dex .
  zip -j "${NAME}-unsigned.apk" classes.dex
  zip -r "${NAME}-unsigned.apk" lib/
)

zipalign -p -f -v 16384 \
  "$BUILD_DIR/${NAME}-unsigned.apk" \
  "$BUILD_DIR/${NAME}-aligned.apk"

apksigner sign \
  --ks "$KEYSTORE" \
  --ks-key-alias "$KEYALIAS" \
  --ks-pass "pass:$KEYPASS" \
  --key-pass "pass:$KEYPASS" \
  --out "$BUILD_DIR/${NAME}.apk" \
  "$BUILD_DIR/${NAME}-aligned.apk"

apksigner verify --verbose "$BUILD_DIR/${NAME}.apk"
zipalign -c -v 16384 "$BUILD_DIR/${NAME}.apk"

echo "APK pronto: $BUILD_DIR/${NAME}.apk"
