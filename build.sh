#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
source "$ROOT/java-env.sh"

OUT="$ROOT/out/classes"
mkdir -p "$OUT"

# Las rutas pueden contener espacios (p.ej. iCloud Drive: "Mobile Documents"),
# por eso se juntan en un array con -print0 en lugar de un archivo @lista.
SOURCES=()
while IFS= read -r -d '' f; do
  SOURCES+=("$f")
done < <(find "$ROOT/src" -name '*.java' -print0)

if [ ${#SOURCES[@]} -eq 0 ]; then
  echo "No se encontraron fuentes .java en $ROOT/src" >&2
  exit 1
fi

"$JAVAC_BIN" -d "$OUT" "${SOURCES[@]}"
echo "Build OK -> $OUT (javac: $JAVAC_BIN)"
