#!/bin/sh
set -eu

repo_dir="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$repo_dir"

if [ ! -f .env ]; then
  umask 077
  postgres_password="$(openssl rand -hex 32)"
  jwt_secret="$(openssl rand -base64 64 | tr -d '\n')"

  {
    printf 'POSTGRES_USER=gradinator\n'
    printf 'POSTGRES_PASSWORD=%s\n' "$postgres_password"
    printf 'POSTGRES_DB=postgres\n'
    printf '\n'
    printf 'JWT_SECRET=%s\n' "$jwt_secret"
    printf 'JWT_EXPIRATION=900000\n'
    printf 'JWT_ACCESS_EXPIRATION=900000\n'
    printf 'JWT_REFRESH_EXPIRATION=2592000000\n'
    printf '\n'
    printf 'EMAIL_VERIFICATION_ENABLED=false\n'
    printf 'EMAIL_VERIFICATION_TOKEN_TTL=PT24H\n'
    printf 'PUBLIC_URL=http://localhost:3042\n'
    printf 'MAIL_HOST=localhost\n'
    printf 'MAIL_PORT=587\n'
    printf 'MAIL_USERNAME=\n'
    printf 'MAIL_PASSWORD=\n'
    printf 'MAIL_FROM=gradinator@localhost\n'
    printf 'MAIL_SMTP_AUTH=true\n'
    printf 'MAIL_STARTTLS=true\n'
  } > .env

  echo 'Created protected .env file. Configure SMTP before enabling email verification.'
else
  echo 'Kept existing .env file.'
fi

chmod 600 .env
docker compose -f compose.prod.yml config --quiet

COMPOSE_PARALLEL_LIMIT=1 docker compose -f compose.prod.yml build g-api
COMPOSE_PARALLEL_LIMIT=1 docker compose -f compose.prod.yml build g-core
COMPOSE_PARALLEL_LIMIT=1 docker compose -f compose.prod.yml build g-web

docker compose -f compose.prod.yml up -d
