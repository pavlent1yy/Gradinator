#!/bin/sh
set -eu
source_dir="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
test_dir=$(mktemp -d /tmp/gradinator-deploy-test.XXXXXX)
trap 'case "$test_dir" in /tmp/gradinator-deploy-test.*) rm -rf "$test_dir";; esac' EXIT
mkdir -p "$test_dir/bin" "$test_dir/deploy"
cp "$source_dir/deploy.sh" "$test_dir/deploy/deploy.sh"
touch "$test_dir/.env"
export TEST_REVISION=1111111111111111111111111111111111111111
export TEST_LOG="$test_dir/docker.log"
export DEPLOY_BACKUP_ROOT="$test_dir/backups"
export PATH="$test_dir/bin:$PATH"
cat > "$test_dir/bin/git" <<'MOCK'
#!/bin/sh
case "$1" in
  status) test "${TEST_DIRTY:-0}" -eq 0 || printf ' M file\n';;
  rev-parse) printf '%s\n' "$TEST_REVISION";;
  *) exit 1;;
esac
MOCK
cat > "$test_dir/bin/docker" <<'MOCK'
#!/bin/sh
printf '%s tag=%s\n' "$*" "${DEPLOY_IMAGE_TAG:-}" >> "$TEST_LOG"
case "$1 $2" in
  'image inspect') printf '%s\n' "${TEST_IMAGE_REVISION:-$TEST_REVISION}"; exit 0;;
  'image tag') exit 0;;
  'inspect '* ) printf 'sha256:previous-runtime\n'; exit 0;;
esac
test "$1" = compose || exit 1
shift 3
case "$1" in
  ps) printf 'previous-container\n';;
  config) exit 0;;
  exec) printf 'test database dump\n';;
  up)
    case "${DEPLOY_IMAGE_TAG:-}" in rollback-*) exit 0;; esac
    test "${TEST_FAIL_UP:-0}" -eq 0
    ;;
  *) exit 1;;
esac
MOCK
cat > "$test_dir/bin/curl" <<'MOCK'
#!/bin/sh
exit 0
MOCK
chmod +x "$test_dir/bin/git" "$test_dir/bin/docker" "$test_dir/bin/curl"
sh -n "$source_dir/deploy.sh" "$source_dir/ssh-command.sh"

# Reject modified source and mismatched revisions before touching Docker.
if TEST_DIRTY=1 sh "$test_dir/deploy/deploy.sh"; then exit 1; fi
test ! -e "$TEST_LOG"
if DEPLOY_IMAGE_TAG=2222222222222222222222222222222222222222 sh "$test_dir/deploy/deploy.sh"; then exit 1; fi
test ! -e "$TEST_LOG"
if TEST_IMAGE_REVISION=wrong sh "$test_dir/deploy/deploy.sh"; then exit 1; fi
rm -f "$TEST_LOG"

sh "$test_dir/deploy/deploy.sh"
test "$(cat "$test_dir/.deployed-revision")" = "$TEST_REVISION"
test -s "$test_dir"/backups/*/database.sql
rm -f "$test_dir/.deployed-revision" "$TEST_LOG"

if TEST_FAIL_UP=1 sh "$test_dir/deploy/deploy.sh"; then exit 1; fi
test ! -e "$test_dir/.deployed-revision"
grep -q 'up .*tag=rollback-' "$TEST_LOG"
test "$(grep -c 'compose .* up ' "$TEST_LOG")" -eq 2
echo 'Deployment safety and runtime rollback tests passed'
