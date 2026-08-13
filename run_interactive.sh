#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
"$ROOT/build.sh"
source "$ROOT/java-env.sh"
cd "$ROOT"
exec "$JAVA_BIN" -cp out/classes ss.tp1.InteractiveApp
