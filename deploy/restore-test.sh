#!/usr/bin/env bash
# Restores a backup into a disposable PostgreSQL container, checks it, then removes the container.
#
# Usage:
#   restore-test.sh -i IDENTITY_FILE [BACKUP]   decrypts here with age; BACKUP: the latest valid one
#   restore-test.sh --decrypted FILE            an already decrypted dump; FILE may be - (stdin)
#   option: --expect-version N                  the latest Flyway version expected (default: the
#                                               highest V<n> of backend/src/main/resources/db/migration)
#
# Never the database of the acceptance environment: the container belongs to its own compose project
# (restore-test.compose.yaml), has no network and keeps its data in memory. The script never stores
# the private key. It prints table names and counts, never content: the messages of pg_restore are
# hidden, because a failing COPY quotes the row it failed on.
# Procedure: docs/exploitation/sauvegardes.md
set -euo pipefail
umask 077

here=$(cd "$(dirname "$0")" && pwd)
# shellcheck source=deploy/backup-common.sh
source "$here/backup-common.sh"

PROJECT=auclairdeso-restore-test
# --env-file /dev/null: compose would otherwise read deploy/.env, which this test has no use for.
COMPOSE=(docker compose --project-name "$PROJECT" --file "$here/restore-test.compose.yaml" --env-file /dev/null)
MIGRATIONS="$here/../backend/src/main/resources/db/migration"
# Accounts, clients, offerings, appointments, portraits, private notes, bills.
TABLES=(user_account client offering appointment appointment_photo client_note bill)

usage() {
  sed -n '4,9p' "$0" | sed 's/^# \{0,1\}//'
}

identity=""
decrypted=""
backup=""
expected=""
while (($#)); do
  case $1 in
    -i | --identity)
      identity=${2:-}
      shift 2 || { usage >&2; exit 64; }
      ;;
    --decrypted)
      decrypted=${2:-}
      shift 2 || { usage >&2; exit 64; }
      ;;
    --expect-version)
      expected=${2:-}
      shift 2 || { usage >&2; exit 64; }
      ;;
    -h | --help)
      usage
      exit 0
      ;;
    -*)
      usage >&2
      exit 64
      ;;
    *)
      backup=$1
      shift
      ;;
  esac
done

die() {
  echo "ERROR: $*" >&2
  exit 1
}

if [[ -n $identity && -n $decrypted ]] || [[ -z $identity && -z $decrypted ]]; then
  usage >&2
  exit 64
fi
if [[ -n ${COMPOSE_PROJECT:-} || -n ${COMPOSE_PROJECT_NAME:-} ]]; then
  echo "Note: COMPOSE_PROJECT is ignored, this test only ever uses the project $PROJECT" >&2
fi

if [[ -z $expected ]]; then
  [[ -d $MIGRATIONS ]] || die "no migrations folder next to this script: pass --expect-version N"
  expected=$(find "$MIGRATIONS" -maxdepth 1 -name 'V*__*.sql' -printf '%f\n' |
    sed -E 's/^V([0-9]+)__.*/\1/' | sort -n | tail -n 1)
fi
[[ $expected =~ ^[0-9]+$ ]] || die "the expected version must be a number"

if [[ -n $identity ]]; then
  command -v age >/dev/null || die "age is not installed"
  [[ -r $identity ]] || die "cannot read the identity file $identity"
  if [[ -z $backup ]]; then
    mapfile -t valid < <(backup_list_valid)
    ((${#valid[@]} > 0)) || die "no valid backup in $BACKUP_DIR"
    backup=${valid[0]}
  fi
  [[ -r $backup ]] || die "cannot read $backup"
  backup_has_age_header "$backup" || die "$backup is not an age file"
  source_name=${backup##*/}
else
  [[ -z $backup ]] || die "--decrypted takes the dump itself: no backup file besides"
  if [[ $decrypted != - ]]; then
    [[ -r $decrypted ]] || die "cannot read $decrypted"
  fi
  source_name="decrypted dump ($decrypted)"
fi

cleanup() {
  if ! "${COMPOSE[@]}" down --volumes --remove-orphans --timeout 5 >/dev/null 2>&1; then
    echo "WARN: the disposable container may remain: docker compose -p $PROJECT down -v" >&2
  fi
}
trap cleanup EXIT
trap 'exit 130' INT TERM

echo "Restore test of $source_name"
# A container left over by an interrupted run goes first.
cleanup
"${COMPOSE[@]}" up --detach --wait --quiet-pull >/dev/null 2>&1 || die "the disposable PostgreSQL did not start"

restore=(exec -T db pg_restore --username=restore_test --dbname=restore_test
  --no-owner --no-privileges --exit-on-error --single-transaction)
started=$SECONDS
set +e
if [[ -n $identity ]]; then
  age --decrypt --identity "$identity" -- "$backup" | "${COMPOSE[@]}" "${restore[@]}" >/dev/null 2>&1
  status=("${PIPESTATUS[@]}")
  ((status[0] == 0)) || die "age could not decrypt the backup (exit code ${status[0]})"
  restored=${status[1]}
elif [[ $decrypted == - ]]; then
  "${COMPOSE[@]}" "${restore[@]}" >/dev/null 2>&1
  restored=$?
else
  "${COMPOSE[@]}" "${restore[@]}" <"$decrypted" >/dev/null 2>&1
  restored=$?
fi
set -e
# Its messages could quote data: only the exit code is shown.
((restored == 0)) || die "pg_restore failed (exit code $restored); nothing was restored"
echo "  restored in $((SECONDS - started)) s"

sql() {
  "${COMPOSE[@]}" exec -T db psql --username=restore_test --dbname=restore_test \
    --no-psqlrc --tuples-only --no-align --quiet --set=ON_ERROR_STOP=1 --command="$1"
}

failed=0
if [[ $(sql "SELECT to_regclass('public.flyway_schema_history') IS NOT NULL") != t ]]; then
  echo "  flyway_schema_history: MISSING"
  failed=1
else
  latest=$(sql "SELECT max(version::numeric) FROM flyway_schema_history WHERE success AND version IS NOT NULL")
  broken=$(sql "SELECT count(*) FROM flyway_schema_history WHERE NOT success")
  printf '  flyway_schema_history: latest version %s (expected %s), failed migrations %s\n' \
    "${latest:-none}" "$expected" "$broken"
  if [[ $latest != "$expected" || $broken != 0 ]]; then
    failed=1
  fi
fi

for table in "${TABLES[@]}"; do
  if [[ $(sql "SELECT to_regclass('public.$table') IS NOT NULL") != t ]]; then
    printf '  %-18s MISSING\n' "$table"
    failed=1
  else
    printf '  %-18s %s rows\n' "$table" "$(sql "SELECT count(*) FROM public.$table")"
  fi
done

if ((failed)); then
  echo "Restore test FAILED"
  exit 1
fi
echo "Restore test passed"
