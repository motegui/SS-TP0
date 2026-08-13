#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
"$ROOT/build.sh" >/dev/null
source "$ROOT/java-env.sh"
cd "$ROOT"
exec "$JAVA_BIN" -Djava.awt.headless=true -cp out/classes ss.tp1.BenchmarkM "$@"
