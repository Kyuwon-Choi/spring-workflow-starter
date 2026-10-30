#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./npmw ci --no-audit --no-fund
./npmw run build
./mvnw -ntp -Dskip.installnodenpm package
