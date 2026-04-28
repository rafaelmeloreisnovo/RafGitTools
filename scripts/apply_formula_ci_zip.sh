#!/usr/bin/env bash
set -euo pipefail

ZIP_PATH="${1:-formula_ci.zip}"

if [ ! -f "$ZIP_PATH" ]; then
  echo "CI overlay zip not found at '$ZIP_PATH'; skipping."
  exit 0
fi

echo "Applying CI overlay from $ZIP_PATH"
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT

unzip -q -o "$ZIP_PATH" -d "$workdir"

mapfile -t top_dirs < <(find "$workdir" -mindepth 1 -maxdepth 1 -type d ! -name "__MACOSX" -print)
mapfile -t top_files < <(find "$workdir" -mindepth 1 -maxdepth 1 -type f ! -name ".DS_Store" ! -name "Thumbs.db" ! -name "desktop.ini" ! -name "._*" -print)

if [ "${#top_dirs[@]}" -eq 1 ] && [ "${#top_files[@]}" -eq 0 ]; then
  rootdir="${top_dirs[0]}"
  if [ -f "$rootdir/gradlew" ] || [ -f "$rootdir/settings.gradle" ] || [ -f "$rootdir/settings.gradle.kts" ] || [ -d "$rootdir/.github" ] || [ -d "$rootdir/gradle" ]; then
    (cd "$rootdir" && tar -cf - .) | tar -xf - -C .
  else
    (cd "$workdir" && tar -cf - .) | tar -xf - -C .
  fi
else
  (cd "$workdir" && tar -cf - .) | tar -xf - -C .
fi

rm -rf __MACOSX || true
find . -name ".DS_Store" -delete || true
find . -name "._*" -delete || true

echo "CI overlay applied."
