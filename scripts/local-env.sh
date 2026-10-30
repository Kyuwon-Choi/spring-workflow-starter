#!/usr/bin/env bash
# Source after changing to the starter root. Never print the signing key.
umask 077
mkdir -p .local-data
if [[ -z "${KIT_JWT_SECRET:-}" ]]; then
  if [[ ! -s .local-data/jwt-secret ]]; then
    openssl rand -base64 64 > .local-data/jwt-secret
  fi
  export KIT_JWT_SECRET
  KIT_JWT_SECRET="$(cat .local-data/jwt-secret)"
fi
