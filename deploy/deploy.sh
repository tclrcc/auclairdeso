#!/usr/bin/env bash
# Updates the code from GitHub and (re)starts the acceptance environment.
#
# The database is backed up first (deploy/backup.sh): Flyway migrates it as soon as the new backend
# starts. If the backup fails, nothing is deployed.
# Usage: deploy.sh [--skip-backup]
#   --skip-backup   deploys without that backup, knowingly (a warning says so)
set -euo pipefail

skip_backup=false
case "${1:-}" in
  "") ;;
  --skip-backup) skip_backup=true ;;
  *)
    echo "Usage: $0 [--skip-backup]" >&2
    exit 64
    ;;
esac

warn_no_backup() {
  local bar='!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!'
  printf '\n%s\n!!! %s\n!!! %s\n%s\n\n' "$bar" \
    "WARNING: --skip-backup, deployed WITHOUT a backup of the database beforehand." \
    "If a migration goes wrong, the last nightly backup is all there is." "$bar" >&2
}

cd "$(dirname "$0")/.."

if $skip_backup; then
  warn_no_backup
elif [[ -z $(docker ps --quiet --filter label=com.docker.compose.project=auclairdeso \
  --filter label=com.docker.compose.service=postgres) ]]; then
  echo "No running database to back up (first installation?): deploying without a backup."
elif ! deploy/backup.sh --label predeploy; then
  echo "The backup failed: nothing was deployed. Fix it (see docs/exploitation/sauvegardes.md)," >&2
  echo "or deploy without a backup, knowingly: deploy/deploy.sh --skip-backup" >&2
  exit 1
fi

git pull --ff-only

cd deploy
docker compose up -d --build --remove-orphans
# git replaces nginx.conf with a new file: the gateway only sees it after a restart.
docker compose restart gateway
docker image prune -f

docker compose ps

if $skip_backup; then
  warn_no_backup
fi
