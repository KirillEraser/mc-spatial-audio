#!/usr/bin/env bash
set -euo pipefail
trap 'echo "Release packaging failed at line $LINENO" >&2' ERR
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
VERSION="${1:?Provide the release version}"
mkdir -p dist
cp "build/libs/mc-spatial-audio-$VERSION.jar" dist/
lipo native/build/MCSpatialProbe.app/Contents/MacOS/MCSpatialProbe -info
lipo native/build/MCSpatialProbe.app/Contents/MacOS/MCSpatialProbe -verify_arch arm64 x86_64
codesign --force --sign - native/build/MCSpatialProbe.app
codesign --verify --strict --verbose=2 native/build/MCSpatialProbe.app
ditto -c -k --sequesterRsrc --keepParent native/build/MCSpatialProbe.app dist/MCSpatialProbe-macos-universal.zip
cp docs/INSTALL_PREBUILT.md dist/INSTALL.md
cp docs/RELEASE_NOTES.md dist/RELEASE_NOTES.md
printf '\nBuilt from commit: %s\nVersion: %s\n' "${GITHUB_SHA:-local-build}" "$VERSION" >> dist/RELEASE_NOTES.md
(cd dist && shasum -a 256 *.jar MCSpatialProbe-macos-universal.zip INSTALL.md RELEASE_NOTES.md > SHA256SUMS.txt)
ls -lh dist/
