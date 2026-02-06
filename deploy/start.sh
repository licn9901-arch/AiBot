#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
docker compose -f "${ROOT_DIR}/deploy/docker-compose.yml" up -d
echo "DB services started."
