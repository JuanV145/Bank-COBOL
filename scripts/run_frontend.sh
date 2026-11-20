#!/usr/bin/env bash
# ==================================================================
#  run_frontend.sh : arranca Angular en el puerto 4200.
#  No copies node_modules desde Windows: npm instala los binarios
#  nativos de Linux en el primer arranque.
# ==================================================================
set -euo pipefail

cd "$(dirname "$0")/../frontend"

if ! command -v npm >/dev/null 2>&1; then
    echo "[ERROR] No se encontro npm. Instala Node.js 22+." >&2
    echo "Fedora: sudo dnf install -y nodejs npm" >&2
    exit 1
fi

if [ ! -d node_modules ]; then
    if [ -f package-lock.json ]; then
        npm ci
    else
        npm install
    fi
fi

exec npx ng serve --port 4201
