#!/bin/sh
set -eu

repo_dir="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$repo_dir"
health_url="${HEALTH_URL:-http://127.0.0.1:3042/api/core/schedule/groups}"
health_timeout="${HEALTH_TIMEOUT:-240}"
compose="docker compose -f compose.prod.yml"
test -f .env || { echo 'Missing production .env' >&2; exit 1; }
test -z "$(git status --porcelain --untracked-files=no)" || { echo 'Tracked production files have local changes' >&2; exit 1; }

exec 9>"$repo_dir/.deploy.lock"
flock -w 120 9 || { echo 'Another deployment is active' >&2; exit 1; }

current="$(git rev-parse HEAD)"
requested="${DEPLOY_IMAGE_TAG:-$current}"
test "$requested" = "$current" || { echo 'Image revision differs from checkout' >&2; exit 1; }
export DEPLOY_IMAGE_TAG="$requested"
for service in g-api g-core g-web; do
  revision=$(docker image inspect "gradinator-$service:$requested" --format '{{index .Config.Labels "org.opencontainers.image.revision"}}')
  test "$revision" = "$current" || { echo "Unexpected $service image revision" >&2; exit 1; }
done

# Preserve immutable runtime images for rollback without discarding source or data.
rollback="rollback-$(date +%Y%m%d%H%M%S)"
for service in g-api g-core g-web; do
  container=$($compose ps -q "$service")
  test -n "$container" || { echo "Missing previous $service container" >&2; exit 1; }
  docker image tag "$(docker inspect "$container" --format '{{.Image}}')" "gradinator-$service:$rollback"
done
$compose config --quiet

# Keep a private pre-migration dump; application images never contain credentials.
backup_dir="/home/yor/backups/gradinator-deploy-$(date +%Y%m%d%H%M%S)"
umask 077
mkdir -p "$backup_dir"
cp .env "$backup_dir/production.env"
printf '%s\n' "$rollback" > "$backup_dir/runtime-image-tag"
$compose exec -T postgres sh -c 'pg_dumpall -U "$POSTGRES_USER"' > "$backup_dir/database.sql"

wait_healthy() {
  deadline=$(( $(date +%s) + health_timeout ))
  while [ "$(date +%s)" -lt "$deadline" ]; do
    if curl -fsS -o /dev/null --max-time 10 "$health_url" &&
       curl -fsS -o /dev/null --max-time 10 http://127.0.0.1:3042/; then
      return 0
    fi
    sleep 5
  done
  return 1
}

if $compose up -d --no-build --pull never --remove-orphans && wait_healthy; then
  printf '%s\n' "$current" > .deployed-revision
  echo "Deployment verified: $current"
  exit 0
fi

echo "Deployment failed; restoring previous runtime images: $rollback" >&2
# Database migrations must remain backward compatible; never reset source or data.
export DEPLOY_IMAGE_TAG="$rollback"
$compose up -d --no-build --pull never --remove-orphans
wait_healthy || echo 'Rollback health check failed' >&2
exit 1
