#!/bin/sh
set -eu

repo_dir="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$repo_dir"

branch="${DEPLOY_BRANCH:-main}"
health_url="${HEALTH_URL:-http://127.0.0.1:3042/api/core/schedule/groups}"
health_timeout="${HEALTH_TIMEOUT:-240}"
compose="docker compose -f compose.prod.yml"

if [ ! -f .env ]; then
  echo "Нет .env — сначала выполни deploy/bootstrap.sh" >&2
  exit 1
fi

previous="$(git rev-parse HEAD)"

git fetch --prune origin "$branch"
git checkout -q "$branch"
git merge --ff-only "origin/$branch"

current="$(git rev-parse HEAD)"
echo "Деплой: ${previous} -> ${current}"

build_and_up() {
  $compose config --quiet
  for service in g-api g-core g-web; do
    COMPOSE_PARALLEL_LIMIT=1 $compose build "$service"
  done
  $compose up -d --remove-orphans
}

wait_healthy() {
  deadline=$(( $(date +%s) + health_timeout ))
  while [ "$(date +%s)" -lt "$deadline" ]; do
    if curl -fsS -o /dev/null --max-time 5 "$health_url"; then
      return 0
    fi
    sleep 5
  done
  return 1
}

build_and_up

if wait_healthy; then
  docker image prune -f >/dev/null
  echo "Готово: ${current}"
  exit 0
fi

echo "Сервис не ответил за ${health_timeout} c, откат на ${previous}" >&2
$compose logs --tail=80 g-core g-api g-web >&2 || true

if [ "$previous" != "$current" ]; then
  git reset -q --hard "$previous"
  build_and_up
  if wait_healthy; then
    echo "Откат выполнен: ${previous}" >&2
  else
    echo "Откат тоже не поднялся, нужна ручная проверка" >&2
  fi
fi

exit 1
