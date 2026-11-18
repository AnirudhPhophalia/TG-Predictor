#!/usr/bin/env sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
if [ -f .env ]; then
    set -a
    . ./.env
    set +a
fi
case "${1:-}" in
    demo) export SPRING_PROFILES_ACTIVE=demo; shift ;;
    postgres) export SPRING_PROFILES_ACTIVE=postgres; shift ;;
esac
exec ./mvnw spring-boot:run "$@"
