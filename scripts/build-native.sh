#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "The Apple backend must be built on macOS." >&2
  exit 1
fi
if [[ -z "${JAVA_HOME:-}" ]]; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
fi
if [[ ! -f "$JAVA_HOME/include/jni.h" ]]; then
  echo "Set JAVA_HOME to a JDK 21 installation with JNI headers." >&2
  exit 1
fi
cmake -S "$ROOT/native" -B "$ROOT/native/build" \
  -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_OSX_ARCHITECTURES="arm64;x86_64" \
  -DMCSPATIAL_BUILD_MAC_BACKEND=ON \
  -DBUILD_TESTING=ON
cmake --build "$ROOT/native/build" --parallel
ctest --test-dir "$ROOT/native/build" --output-on-failure
# Ad-hoc signing is for local loading only, and does NOT grant the host JVM an entitlement.
codesign --force --sign - "$ROOT/native/build/libmcspatial.dylib"
DEST="$ROOT/src/main/resources/natives/macos/universal"
mkdir -p "$DEST"
cp "$ROOT/native/build/libmcspatial.dylib" "$DEST/libmcspatial.dylib"
echo "Staged $DEST/libmcspatial.dylib"
echo "Probe: $ROOT/native/build/MCSpatialProbe.app"
echo "Probe signing/permissions and macOS system-control behavior remain separate tests."
