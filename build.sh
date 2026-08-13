#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
source "$ROOT/java-env.sh"

OUT="$ROOT/out/classes"
mkdir -p "$OUT"
find "$ROOT/src" -name '*.java' > "$ROOT/out/sources.txt"
"$JAVAC_BIN" -d "$OUT" @"$ROOT/out/sources.txt"
echo "Build OK -> $OUT (javac: $JAVAC_BIN)"
