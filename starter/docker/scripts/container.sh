#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
usage() { echo 'Usage: ./scripts/container.sh build | run [host-port]' >&2; exit 2; }
[[ $# -ge 1 ]] || usage
action=$1
port=${2:-8080}
case "$action" in
  build) [[ $# -eq 1 ]] || usage ;;
  run)
    [[ $# -le 2 && "$port" =~ ^[1-9][0-9]{0,4}$ ]] || usage
    (( port <= 65535 )) || usage ;;
  *) usage ;;
esac
if ! command -v docker >/dev/null 2>&1; then
  echo 'Docker CLI is unavailable; container build/run was not performed.' >&2
  exit 2
fi
docker info >/dev/null
image=${SOLUTION_IMAGE:-solution-local}
if [[ "$action" == build ]]; then
  exec docker build --tag "$image" .
fi
volume=${SOLUTION_DATA_VOLUME:-solution-local-data}
[[ "$volume" =~ ^[a-zA-Z0-9][a-zA-Z0-9_.-]*$ ]] || { echo 'Invalid SOLUTION_DATA_VOLUME' >&2; exit 2; }
echo "Publishing host port $port to container 8080. Keep this terminal open."
exec docker run --rm --init --publish "${port}:8080" \
  --mount "type=volume,source=${volume},target=/app/.local-data" \
  --env APP_ADMIN_PASSWORD --env APP_USER_PASSWORD \
  "$image" --server.address=0.0.0.0 --server.port=8080 --app.preview=true
