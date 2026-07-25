#!/bin/sh
# Local RafGitTools fallback APK built through RafPolimata's ApkC subsystem.
# This path deliberately does not invoke Gradle, the Android SDK, Maven, or
# any network operation.
# Contract: docs/native/APKC_HERMETIC_TERMUX.md
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
DEFAULT_RAFPOLIMATA_ROOT="$ROOT/../RafPolimata"
RAFPOLIMATA_ROOT="${RAFPOLIMATA_ROOT:-$DEFAULT_RAFPOLIMATA_ROOT}"
BUILDER="${APKC_HERMETIC_BUILDER:-$RAFPOLIMATA_ROOT/scripts/apkc_termux_hermetic_build.sh}"

OUT_DIR="$ROOT/dist/apkc-hermetic"
SOURCE="$ROOT/tools/termuxforge/rafgittools_bootstrap.s"
PACKAGE_NAME="com.rafgittools.hermetic"
APP_LABEL="RafGitTools Hermetic"
LIB_NAME="rafgittools_hermetic"
ABI="both"
MIN_SDK="24"
TARGET_SDK="35"
SIGN=0

usage() {
    cat <<'EOF'
Usage:
  sh scripts/termux/build_apkc_hermetic.sh [options]

Builds the RafGitTools native bootstrap APK through a local RafPolimata checkout.
It does not clone, download, install packages, call Gradle, or resolve Maven.

Options:
  --rafpolimata-root DIR    local RafPolimata checkout (default: ../RafPolimata)
  --apkc-builder FILE       local ApkC hermetic builder script
  --out DIR                 output directory (default: dist/apkc-hermetic)
  --abi both|armeabi-v7a|arm64-v8a
  --min-sdk N
  --target-sdk N
  --sign                    pass local signing configuration to ApkC builder
  --unsigned                leave the APK unsigned (default)
  --help                    show this help

For --sign, export the same local-only variables used by RafPolimata:
  APKSIGNER_KEYSTORE, APKSIGNER_ALIAS, APKSIGNER_KS_PASS_FILE,
  and optionally APKSIGNER_KEY_PASS_FILE.

This output is a NativeActivity bootstrap with ABI/build evidence. It is not
the full Kotlin/Compose/JGit RafGitTools application.
EOF
}

die() {
    printf 'rafgittools-hermetic: ERROR: %s\n' "$*" >&2
    exit 1
}

need_value() {
    [ "$#" -ge 2 ] || die "missing value for $1"
}

sha256_or_token() {
    if command -v sha256sum >/dev/null 2>&1; then
        set -- $(sha256sum "$1")
        printf '%s\n' "$1"
    else
        printf '%s\n' 'TOKEN_VAZIO_SHA256SUM_ABSENT'
    fi
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --rafpolimata-root) need_value "$@"; RAFPOLIMATA_ROOT=$2; BUILDER="$RAFPOLIMATA_ROOT/scripts/apkc_termux_hermetic_build.sh"; shift 2 ;;
        --apkc-builder) need_value "$@"; BUILDER=$2; shift 2 ;;
        --out) need_value "$@"; OUT_DIR=$2; shift 2 ;;
        --abi) need_value "$@"; ABI=$2; shift 2 ;;
        --min-sdk) need_value "$@"; MIN_SDK=$2; shift 2 ;;
        --target-sdk) need_value "$@"; TARGET_SDK=$2; shift 2 ;;
        --sign) SIGN=1; shift ;;
        --unsigned) SIGN=0; shift ;;
        --help|-h) usage; exit 0 ;;
        *) die "unknown option: $1" ;;
    esac
done

case "$ABI" in
    both|armeabi-v7a|arm64-v8a) ;;
    *) die "--abi must be both, armeabi-v7a, or arm64-v8a" ;;
esac
case "$MIN_SDK" in ''|*[!0-9]*) die "--min-sdk must be numeric" ;; esac
case "$TARGET_SDK" in ''|*[!0-9]*) die "--target-sdk must be numeric" ;; esac

[ -f "$SOURCE" ] || die "bootstrap assembly source missing: $SOURCE"
[ -f "$BUILDER" ] || die "local RafPolimata ApkC builder missing: $BUILDER"

mkdir -p "$OUT_DIR"
if [ "$SIGN" -eq 1 ]; then
    sh "$BUILDER" --source "$SOURCE" --out "$OUT_DIR" --package "$PACKAGE_NAME" \
        --label "$APP_LABEL" --lib "$LIB_NAME" --abi "$ABI" --min-sdk "$MIN_SDK" \
        --target-sdk "$TARGET_SDK" --sign
else
    sh "$BUILDER" --source "$SOURCE" --out "$OUT_DIR" --package "$PACKAGE_NAME" \
        --label "$APP_LABEL" --lib "$LIB_NAME" --abi "$ABI" --min-sdk "$MIN_SDK" \
        --target-sdk "$TARGET_SDK" --unsigned
fi

UNSIGNED="$OUT_DIR/${LIB_NAME}-unsigned.apk"
SIGNED="$OUT_DIR/${LIB_NAME}-signed.apk"
RECEIPT="$OUT_DIR/rafgittools-orchestrator.env"
[ -s "$UNSIGNED" ] || die "RafPolimata builder returned without unsigned APK"

{
    printf 'schema=%s\n' 'raf.rafgittools.apkc-hermetic.v1'
    printf 'state=%s\n' 'BUILD_CREATED'
    printf 'claim_allowed=%s\n' 'false'
    printf 'network=%s\n' 'NOT_USED'
    printf 'gradle=%s\n' 'NOT_USED'
    printf 'android_sdk=%s\n' 'NOT_USED'
    printf 'jvm_d8_aapt=%s\n' 'NOT_USED'
    printf 'apkc_builder=%s\n' "$BUILDER"
    printf 'rafpolimata_root=%s\n' "$RAFPOLIMATA_ROOT"
    printf 'source=%s\n' "$SOURCE"
    printf 'source_sha256=%s\n' "$(sha256_or_token "$SOURCE")"
    printf 'requested_abi=%s\n' "$ABI"
    printf 'package=%s\n' "$PACKAGE_NAME"
    printf 'unsigned_apk=%s\n' "$UNSIGNED"
    printf 'unsigned_apk_sha256=%s\n' "$(sha256_or_token "$UNSIGNED")"
    if [ -s "$SIGNED" ]; then
        printf 'signed_apk=%s\n' "$SIGNED"
        printf 'signed_apk_sha256=%s\n' "$(sha256_or_token "$SIGNED")"
        printf 'signing=%s\n' 'SIGNED_AND_VERIFIED_BY_APKSIGNER'
    else
        printf 'signing=%s\n' 'UNSIGNED'
    fi
    printf 'full_compose_application=%s\n' 'TOKEN_VAZIO_NOT_BUILT_BY_THIS_MODE'
    printf 'structural_validation=%s\n' 'TOKEN_VAZIO'
    printf 'install_and_runtime=%s\n' 'TOKEN_VAZIO'
    printf '%s\n' 'scope=Native bootstrap only; it does not replace RafGitTools Kotlin/Compose/JGit functionality.'
} >"$RECEIPT"

printf 'rafgittools-hermetic: unsigned APK: %s\n' "$UNSIGNED"
if [ -s "$SIGNED" ]; then
    printf 'rafgittools-hermetic: signed APK:   %s\n' "$SIGNED"
fi
printf 'rafgittools-hermetic: receipt:      %s\n' "$RECEIPT"
