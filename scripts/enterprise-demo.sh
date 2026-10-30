#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
umask 077
mkdir -p .local-data
if [[ -z "${KIT_JWT_SECRET:-}" ]]; then
  if [[ ! -s .local-data/enterprise-jwt-secret ]]; then
    openssl rand -base64 64 > .local-data/enterprise-jwt-secret
  fi
  export KIT_JWT_SECRET
  KIT_JWT_SECRET="$(cat .local-data/enterprise-jwt-secret)"
fi
exec ./scripts/run-jar.sh --server.port=8082 '--spring.datasource.url=jdbc:h2:file:./.local-data/enterprise;DB_CLOSE_ON_EXIT=FALSE;MODE=MYSQL' --fde.enterprise.demo.enabled=true "$@"
