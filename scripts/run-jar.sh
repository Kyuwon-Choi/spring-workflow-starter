#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/local-env.sh
kit_jar=target/jhipster-sample-application-react-0.0.1-SNAPSHOT.jar
if [[ ! -f "$kit_jar" ]]; then
  echo "JAR not found. Build the frontend, then run ./mvnw -ntp -Dskip.installnodenpm -Dskip.npm verify first." >&2
  exit 1
fi
exec java -jar "$kit_jar" --spring.profiles.active=dev,practice "$@"
