#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/local-env.sh
exec ./mvnw -ntp -Dskip.installnodenpm -Dskip.npm spring-boot:run -Dspring-boot.run.profiles=dev,practice "$@"
