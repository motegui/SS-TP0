#!/usr/bin/env bash
# Encuentra binarios de Java/javac que funcionen de verdad (evita el stub de
# macOS en /usr/bin/java que solo muestra un diálogo para instalar Java).
find_working_bin() {
  local name="$1"
  local candidates=(
    "$(command -v "$name" 2>/dev/null || true)"
    "/opt/homebrew/opt/openjdk/bin/$name"
    "/usr/local/opt/openjdk/bin/$name"
    "$(/usr/libexec/java_home 2>/dev/null)/bin/$name"
  )
  for c in "${candidates[@]}"; do
    if [ -n "$c" ] && [ -x "$c" ] && "$c" -version >/dev/null 2>&1; then
      echo "$c"
      return 0
    fi
  done
  return 1
}

JAVA_BIN="$(find_working_bin java)" || {
  echo "No se encontró una JVM funcional. Instalá un JDK 17+ (por ejemplo: brew install openjdk)." >&2
  exit 1
}
JAVAC_BIN="$(find_working_bin javac)" || {
  echo "No se encontró un javac funcional. Instalá un JDK 17+ (por ejemplo: brew install openjdk)." >&2
  exit 1
}
export JAVA_BIN JAVAC_BIN
