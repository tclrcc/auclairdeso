#!/usr/bin/env bash
# Updates the code from GitHub and (re)starts the acceptance environment.
set -euo pipefail

cd "$(dirname "$0")/.."
git pull --ff-only

cd deploy
docker compose up -d --build --remove-orphans
docker image prune -f

docker compose ps
