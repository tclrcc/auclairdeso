# Shared by backup.sh, backup-status.sh and restore-test.sh: where the backups are, how they are named,
# and which ones are valid. Sourced, never executed.
# The variables below are used by the scripts that source this file.
# shellcheck shell=bash disable=SC2034

BACKUP_DIR="${BACKUP_DIR:-/srv/auclairdeso/sauvegardes}"
BACKUP_STATUS_FILE="$BACKUP_DIR/last-backup.status"
# auclairdeso-20261113T024500Z.dump.age, or auclairdeso-20261113T024500Z-predeploy.dump.age.
# The time is in UTC: a name never repeats or jumps when the clocks change.
BACKUP_NAME_RE='^auclairdeso-([0-9]{8}T[0-9]{6}Z)(-[a-z0-9]+)?\.dump\.age$'
BACKUP_MIN_BYTES=1024
BACKUP_ZONE=Europe/Paris

# Prints the time of a backup, in seconds since the epoch, read from its name; fails for any other name.
backup_epoch() {
  local name=${1##*/} stamp
  [[ $name =~ $BACKUP_NAME_RE ]] || return 1
  stamp=${BASH_REMATCH[1]}
  date -u -d "${stamp:0:4}-${stamp:4:2}-${stamp:6:2} ${stamp:9:2}:${stamp:11:2}:${stamp:13:2}" +%s 2>/dev/null
}

# Whether a file starts like an age file. Its content cannot be checked without the private key.
backup_has_age_header() {
  [[ $(head -c 21 -- "$1" 2>/dev/null) == "age-encryption.org/v1" ]]
}

# A valid backup: named like one, a regular file, not too small, and encrypted with age.
backup_is_valid() {
  local file=$1
  backup_epoch "$file" >/dev/null || return 1
  [[ -f $file && ! -L $file ]] || return 1
  (( $(stat -c %s -- "$file") >= BACKUP_MIN_BYTES )) || return 1
  backup_has_age_header "$file"
}

# Every file named like a backup, valid or not, as "epoch<TAB>path", newest first.
backup_list_all() {
  local file epoch
  for file in "$BACKUP_DIR"/auclairdeso-*.dump.age; do
    if epoch=$(backup_epoch "$file"); then
      printf '%s\t%s\n' "$epoch" "$file"
    fi
  done | sort -rn
}

# The valid backups, newest first, one path per line.
backup_list_valid() {
  local epoch file
  while IFS=$'\t' read -r epoch file; do
    if backup_is_valid "$file"; then
      printf '%s\n' "$file"
    fi
  done < <(backup_list_all)
}
