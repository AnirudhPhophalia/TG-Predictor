#!/usr/bin/env sh
# ./run.sh demo  -> demo database with sample accounts and password sign-in
# ./run.sh       -> real database, Google sign-in (needs .env, see README)
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
if [ -f .env ]; then
    set -a
    . ./.env
    set +a
fi
if [ "${1:-}" = "demo" ]; then
    exec ./mvnw javafx:run -Dapp.args=--demo
fi
exec ./mvnw javafx:run
