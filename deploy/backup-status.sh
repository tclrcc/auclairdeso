#!/usr/bin/env bash
# Says how old the last valid backup is, and how the last attempt went.
#
# Exit code: 0 when a valid backup is younger than MAX_AGE_HOURS (26 by default), 1 when none is,
# 2 when one is but the last attempt failed. Made to be wired later to an alert (see the procedure).
# Environment: BACKUP_DIR, MAX_AGE_HOURS.
# Procedure: docs/exploitation/sauvegardes.md
set -euo pipefail

# shellcheck source=deploy/backup-common.sh
source "$(dirname "$0")/backup-common.sh"

MAX_AGE_HOURS="${MAX_AGE_HOURS:-26}"
[[ $MAX_AGE_HOURS =~ ^[0-9]+$ ]] || { echo "MAX_AGE_HOURS must be a number of hours" >&2; exit 64; }

if [[ ! -d $BACKUP_DIR || ! -r $BACKUP_DIR || ! -x $BACKUP_DIR ]]; then
  echo "ERROR: the backup folder $BACKUP_DIR is missing or not readable"
  exit 1
fi

code=0
mapfile -t valid < <(backup_list_valid)

if ((${#valid[@]} == 0)); then
  echo "ERROR: no valid backup in $BACKUP_DIR"
  code=1
else
  latest=${valid[0]}
  epoch=$(backup_epoch "$latest")
  age=$(($(date +%s) - epoch))
  taken=$(TZ=$BACKUP_ZONE date -d "@$epoch" '+%Y-%m-%d %H:%M %Z')
  printf 'Last valid backup: %s\n' "${latest##*/}"
  printf '  taken %s, %dh%02d ago, %s\n' "$taken" $((age / 3600)) $((age % 3600 / 60)) \
    "$(numfmt --to=iec "$(stat -c %s -- "$latest")")"
  printf '  %d valid backup(s) kept\n' "${#valid[@]}"
  if ((age > MAX_AGE_HOURS * 3600)); then
    echo "ERROR: the last valid backup is older than $MAX_AGE_HOURS h"
    code=1
  fi
fi

# key=value lines written by backup.sh: read, never sourced.
if [[ -r $BACKUP_STATUS_FILE ]]; then
  declare -A last=()
  while IFS='=' read -r key value; do
    last[$key]=$value
  done <"$BACKUP_STATUS_FILE"
  printf 'Last attempt: %s at %s\n' "${last[result]:-unknown}" "${last[finished_at]:-unknown}"
  if [[ ${last[result]:-} != ok ]]; then
    printf '  reason: %s\n' "${last[reason]:-unknown}"
    echo "  journal: journalctl -u auclairdeso-backup.service -n 50"
    if ((code == 0)); then
      code=2
    fi
  fi
else
  echo "Last attempt: unknown (no $BACKUP_STATUS_FILE yet)"
fi

exit "$code"
