#!/usr/bin/env bash
# Backs up the database of the acceptance environment, encrypted, then applies the retention.
#
# pg_dump runs inside the postgres container, with the user and the database of the container's own
# environment: neither compose.yaml nor deploy/.env is read, so the backup still works when the compose
# file no longer resolves. Its output is encrypted on the fly with age for a public key: the dump never
# lies in clear on the disk, and the private key never exists on this server.
#
# Usage: backup.sh [--label NAME] [--prune-only]
#   --label NAME   added to the file name, e.g. predeploy (lowercase letters and digits)
#   --prune-only   applies the retention only
# Environment: BACKUP_DIR, BACKUP_RECIPIENT_FILE, COMPOSE_PROJECT; BACKUP_TODAY (YYYY-MM-DD) for tests.
# Procedure: docs/exploitation/sauvegardes.md
set -euo pipefail
umask 077

# shellcheck source=deploy/backup-common.sh
source "$(dirname "$0")/backup-common.sh"

RECIPIENT_FILE="${BACKUP_RECIPIENT_FILE:-/srv/auclairdeso/backup-recipient.txt}"
PROJECT="${COMPOSE_PROJECT:-auclairdeso}"
# Retention: every backup of the last 14 days, plus the newest of each of the last 4 Sundays, nothing
# older (27 days at most): an erased client leaves the backups within a month. The 3 newest valid
# backups are never deleted, whatever their age, so that a stopped timer never ends with nothing.
KEEP_DAYS=14
KEEP_SUNDAYS=4
KEEP_VALID=3

label=""
prune_only=false
attempted=false
container=""
tmp=""
fifo=""
check=""
failure=""

usage() {
  echo "Usage: $0 [--label NAME] [--prune-only]"
}

warn() {
  printf 'WARN: %s\n' "$*" >&2
}

fail() {
  failure=$1
  exit 1
}

# key=value lines, written atomically; read by backup-status.sh. Never any content of the database.
write_status() {
  local result=$1 reason=$2 file=${3:-} size=${4:-}
  {
    printf 'finished_at=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    printf 'result=%s\n' "$result"
    printf 'file=%s\n' "$file"
    printf 'size=%s\n' "$size"
    printf 'reason=%s\n' "$reason"
  } >"$BACKUP_STATUS_FILE.tmp" && mv -f -- "$BACKUP_STATUS_FILE.tmp" "$BACKUP_STATUS_FILE"
}

on_exit() {
  local code=$?
  local file
  for file in "$tmp" "$fifo" "$check"; do
    if [[ -n $file ]]; then
      rm -f -- "$file"
    fi
  done
  if ((code != 0)); then
    local reason=${failure:-"unexpected error (exit code $code)"}
    echo "Backup FAILED: $reason" >&2
    if $attempted; then
      write_status failed "$reason" 2>/dev/null || warn "could not write $BACKUP_STATUS_FILE"
    fi
  fi
}
trap on_exit EXIT

while (($#)); do
  case $1 in
    --label)
      label=${2:-}
      shift 2 || { usage >&2; exit 64; }
      ;;
    --prune-only)
      prune_only=true
      shift
      ;;
    -h | --help)
      usage
      exit 0
      ;;
    *)
      usage >&2
      exit 64
      ;;
  esac
done
[[ -z $label || $label =~ ^[a-z0-9]+$ ]] || fail "the label must be lowercase letters and digits"

check_folder() {
  [[ -d $BACKUP_DIR && -w $BACKUP_DIR ]] || fail "the backup folder $BACKUP_DIR is missing or not writable"
  local mode
  mode=$(stat -c %a -- "$BACKUP_DIR")
  [[ $mode == 700 ]] || warn "$BACKUP_DIR has mode $mode: it should be 700"
}

check_recipient() {
  [[ -r $RECIPIENT_FILE ]] ||
    fail "the recipient file $RECIPIENT_FILE is missing (see docs/exploitation/sauvegardes.md)"
  if grep -q 'AGE-SECRET-KEY' -- "$RECIPIENT_FILE"; then
    fail "$RECIPIENT_FILE holds a PRIVATE key: delete it from this server and keep only the public key"
  fi
  grep -Eq '^age1[0-9a-z]+$' -- "$RECIPIENT_FILE" ||
    fail "no age public key (a line starting with age1) in $RECIPIENT_FILE"
}

find_postgres() {
  local found
  if ! found=$(docker ps --quiet \
    --filter "label=com.docker.compose.project=$PROJECT" \
    --filter "label=com.docker.compose.service=postgres"); then
    fail "Docker cannot be reached (is $(id -un) in the docker group?)"
  fi
  [[ -n $found ]] || fail "no running postgres container in the compose project $PROJECT"
  [[ $found != *$'\n'* ]] || fail "several postgres containers in the compose project $PROJECT"
  container=$found
}

# Reads the dump as it streams by: its first 5 bytes, then how many bytes it holds in all.
# GNU head -c reads no more than it prints, so wc counts the rest exactly.
inspect() {
  local magic rest
  magic=$(head -c 5)
  rest=$(wc -c)
  printf '%s %s\n' "${magic:--}" "$((rest + ${#magic}))"
}

backup() {
  attempted=true
  check_recipient
  local stamp name final started magic size
  find_postgres
  command -v age >/dev/null || fail "age is not installed (sudo apt install age)"
  stamp=$(date -u +%Y%m%dT%H%M%SZ)
  name="auclairdeso-$stamp${label:+-$label}.dump.age"
  final="$BACKUP_DIR/$name"
  tmp="$BACKUP_DIR/.$name.tmp"
  fifo="$BACKUP_DIR/.$name.fifo"
  check="$BACKUP_DIR/.$name.check"
  [[ ! -e $final ]] || fail "$name already exists"
  started=$SECONDS

  mkfifo -- "$fifo"
  inspect <"$fifo" >"$check" &
  local inspector=$!
  set +e
  # The variables are expanded by the shell of the container, from its own environment.
  # shellcheck disable=SC2016
  docker exec "$container" sh -c \
    'exec pg_dump --format=custom --compress=zstd:6 --username="$POSTGRES_USER" --dbname="$POSTGRES_DB"' |
    tee -- "$fifo" |
    age --encrypt --recipients-file "$RECIPIENT_FILE" --output "$tmp"
  local status=("${PIPESTATUS[@]}")
  wait "$inspector"
  local inspected=$?
  set -e

  ((status[0] == 0)) || fail "pg_dump failed (exit code ${status[0]})"
  ((status[1] == 0)) || fail "the dump could not be read (tee, exit code ${status[1]})"
  ((status[2] == 0)) || fail "age failed (exit code ${status[2]})"
  ((inspected == 0)) || fail "the dump could not be inspected"
  read -r magic size <"$check"
  [[ $magic == PGDMP ]] || fail "the dump is not a PostgreSQL archive"
  ((size >= BACKUP_MIN_BYTES)) || fail "the dump is empty or too small ($size bytes)"
  backup_has_age_header "$tmp" || fail "the encrypted file has no age header"

  chmod 600 -- "$tmp"
  sync -- "$tmp"
  mv -f -- "$tmp" "$final"
  tmp=""
  sync -- "$BACKUP_DIR"

  local stored
  stored=$(stat -c %s -- "$final")
  write_status ok "" "$name" "$stored"
  echo "Backup written: $name ($(numfmt --to=iec "$stored"), dump of $(numfmt --to=iec "$size"), $((SECONDS - started)) s)"
}

prune() {
  local today first_day last_sunday i epoch file day keep kept=0 deleted=0 valid_seen=0
  local -A sundays=()
  today=${BACKUP_TODAY:-$(TZ=$BACKUP_ZONE date +%F)}
  first_day=$(date -d "$today -$((KEEP_DAYS - 1)) days" +%F)
  last_sunday=$(date -d "$today -$(($(date -d "$today" +%u) % 7)) days" +%F)
  for ((i = 0; i < KEEP_SUNDAYS; i++)); do
    sundays[$(date -d "$last_sunday -$((7 * i)) days" +%F)]=open
  done

  # Newest first: the first valid backup of a Sunday is its newest.
  while IFS=$'\t' read -r epoch file; do
    day=$(TZ=$BACKUP_ZONE date -d "@$epoch" +%F)
    keep=false
    if [[ ! $day < $first_day ]]; then
      keep=true
    elif [[ ${sundays[$day]:-} == open ]] && backup_is_valid "$file"; then
      sundays[$day]=taken
      keep=true
    fi
    if backup_is_valid "$file"; then
      valid_seen=$((valid_seen + 1))
      if ! $keep && ((valid_seen <= KEEP_VALID)); then
        warn "${file##*/} kept beyond the retention: fewer than $KEEP_VALID valid backups would remain"
        keep=true
      fi
    fi
    if $keep; then
      kept=$((kept + 1))
    else
      rm -f -- "$file"
      deleted=$((deleted + 1))
    fi
  done < <(backup_list_all)

  # Leftovers of an interrupted run (encrypted, or the 5 bytes PGDMP): gone after a day.
  find "$BACKUP_DIR" -maxdepth 1 -name '.auclairdeso-*' \( -type f -o -type p \) -mmin +1440 -delete

  echo "Retention: $kept backup(s) kept, $deleted deleted"
}

check_folder
exec 9>"$BACKUP_DIR/.lock"
flock --wait 900 9 || fail "another backup is still running"

if ! $prune_only; then
  backup
fi
prune
