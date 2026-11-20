#!/usr/bin/env bash
# ==================================================================
#  build_dll.sh : compila los 5 programas COBOL en una biblioteca
#  compartida de Linux (dll/libCOBOLBANK.so).
#
#  Requiere Fedora:
#    sudo dnf install -y gcc gnucobol
# ==================================================================
set -euo pipefail

cd "$(dirname "$0")/.."

if ! command -v cobc >/dev/null 2>&1; then
    echo "[ERROR] No se encontro 'cobc' (GnuCOBOL)." >&2
    echo "Fedora: sudo dnf install -y gcc gnucobol" >&2
    exit 1
fi

if ! command -v gcc >/dev/null 2>&1; then
    echo "[ERROR] No se encontro 'gcc'." >&2
    echo "Fedora: sudo dnf install -y gcc" >&2
    exit 1
fi

PROGRAMAS=(PGDEPOSIT PGRETIRO PGTRAN PGSALDO PGCIERRE)
mkdir -p obj dll
OBJETOS=()

for programa in "${PROGRAMAS[@]}"; do
    echo "Compilando ${programa}.cbl ..."
    cobc -m -c -A -fPIC -o "obj/${programa}.o" "cobol/${programa}.cbl" -I cobol/copy
    OBJETOS+=("obj/${programa}.o")
done

# Enlazar con el runtime del sistema. Si existe libcob.so.4 pero no el
# enlace de desarrollo libcob.so, se usa la ruta real que reporta ldconfig.
COB_LIB=""
if command -v ldconfig >/dev/null 2>&1; then
    COB_LIB="$(ldconfig -p 2>/dev/null | awk '/libcob\.so/ {print $NF; exit}')"
fi

if [ -n "${COB_LIB}" ]; then
    ENLACE=("${COB_LIB}")
    echo "Runtime GnuCOBOL: ${COB_LIB}"
else
    ENLACE=(-lcob)
fi

echo "Enlazando dll/libCOBOLBANK.so ..."
gcc -shared -o dll/libCOBOLBANK.so "${OBJETOS[@]}" "${ENLACE[@]}"

echo
echo "[OK] dll/libCOBOLBANK.so generado con: ${PROGRAMAS[*]}"
