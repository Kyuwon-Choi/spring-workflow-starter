#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if (( $# > 1 )); then
  echo "Usage: ./scripts/preview.sh [port]" >&2
  exit 2
fi
preview_port="${1:-${FDE_PREVIEW_PORT:-8080}}"
if [[ ! "$preview_port" =~ ^[1-9][0-9]{0,4}$ ]] || (( preview_port > 65535 )); then
  echo "Port must be an integer from 1 to 65535." >&2
  exit 2
fi
printf 'Preview binding: 0.0.0.0:%s\n' "$preview_port"
printf 'After startup, confirm the actual port in the server log. Preview path: /fde\n'
printf 'Keep this terminal running while using the preview. Stop with Ctrl+C.\n'
exec ./scripts/run-jar.sh --server.address=0.0.0.0 "--server.port=$preview_port"
