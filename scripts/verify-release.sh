#!/usr/bin/env bash
set -euo pipefail
trap 'echo "Release verification failed at line $LINENO" >&2' ERR
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
VERSION="${1:?Provide the release version}"
JAR="build/libs/mc-spatial-audio-$VERSION.jar"
echo "Checking release JAR: $JAR"
ls -lh build/libs/
test -f "$JAR"
mkdir -p logs native/build/abi-smoke
jar tf "$JAR" > logs/jar-contents.txt
grep -Fx 'natives/macos/universal/libmcspatial.dylib' logs/jar-contents.txt
unzip -p "$JAR" natives/macos/universal/libmcspatial.dylib > native/build/packaged-libmcspatial.dylib
lipo native/build/packaged-libmcspatial.dylib -info
lipo native/build/packaged-libmcspatial.dylib -verify_arch arm64 x86_64
codesign --verify --strict --verbose=2 native/build/packaged-libmcspatial.dylib
javac -cp build/classes/java/main -d native/build/abi-smoke native/tests/NativeAbiSmoke.java
java -cp build/classes/java/main:native/build/abi-smoke NativeAbiSmoke native/build/packaged-libmcspatial.dylib
