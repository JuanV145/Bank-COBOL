#!/usr/bin/env bash
# ==================================================================
#  run_backend.sh : arranca Spring Boot en el puerto 8080.
#  - Usa Maven del sistema o el incluido en tools/apache-maven.
#  - Deja dll/ en LD_LIBRARY_PATH para la biblioteca COBOL.
# ==================================================================
set -euo pipefail

cd "$(dirname "$0")/.."

if ! command -v java >/dev/null 2>&1; then
    echo "[ERROR] No se encontro Java." >&2
    echo "Fedora: sudo dnf install -y java-21-openjdk-devel" >&2
    exit 1
fi

export LD_LIBRARY_PATH="$PWD/dll:${LD_LIBRARY_PATH:-}"

if [ ! -f "dll/libCOBOLBANK.so" ]; then
    echo "[AVISO] No existe dll/libCOBOLBANK.so." >&2
    echo "        Ejecuta primero: bash scripts/build_dll.sh" >&2
fi

if command -v mvn >/dev/null 2>&1; then
    MVN=(mvn)
else
    MVN_BIN="$PWD/tools/apache-maven/bin/mvn"
    if [ ! -f "$MVN_BIN" ]; then
        echo "[ERROR] No se encontro Maven." >&2
        echo "Fedora: sudo dnf install -y maven" >&2
        exit 1
    fi
    [ -x "$MVN_BIN" ] || chmod +x "$MVN_BIN" 2>/dev/null || true
    if [ -x "$MVN_BIN" ]; then
        MVN=("$MVN_BIN")
    else
        MVN=(bash "$MVN_BIN")
    fi
fi

echo "Usando Maven: ${MVN[*]}"
exec "${MVN[@]}" -f "$PWD/backend/pom.xml" \
    -Dspring-boot.run.workingDirectory="$PWD" \
    spring-boot:run
