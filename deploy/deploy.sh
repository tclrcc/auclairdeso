#!/usr/bin/env bash
# Updates the code from GitHub and (re)starts the acceptance environment.
set -euo pipefail

cd "$(dirname "$0")/.."
git pull --ff-only

cd deploy
docker compose up -d --build --remove-orphans
# git replaces nginx.conf with a new file: the gateway only sees it after a restart.
docker compose restart gateway
docker image prune -f

docker compose ps
