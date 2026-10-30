#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
port="${1:-${PORT:-8080}}"
if [[ $# -gt 1 || ! "$port" =~ ^[1-9][0-9]{0,4}$ ]] || (( port > 65535 )); then
  echo 'Usage: ./scripts/preview.sh [port 1..65535]' >&2; exit 2
fi
jar=target/solution-0.0.1-SNAPSHOT.jar
if [[ ! -f "$jar" ]]; then echo 'Run ./scripts/build.sh first.' >&2; exit 1; fi
printf 'Binding 0.0.0.0:%s; confirm actual port in server log. Keep this terminal open.\n' "$port"
exec java -jar "$jar" --server.address=0.0.0.0 "--server.port=$port" --app.preview=true
