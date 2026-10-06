#!/bin/sh
set -eu
repo=/home/yor/apps/Gradinator
case "${SSH_ORIGINAL_COMMAND:-}" in
  load)
    exec docker image load
    ;;
  deploy\ *)
    revision=${SSH_ORIGINAL_COMMAND#deploy }
    case "$revision" in *[!0-9a-f]*|'') exit 64;; esac
    test "${#revision}" -eq 40 || exit 64
    exec 8>/home/yor/apps/.gradinator-action.lock
    flock -w 120 8 || exit 1
    cd "$repo"
    test -z "$(git status --porcelain --untracked-files=no)" || { echo 'Production checkout is dirty' >&2; exit 1; }
    git fetch --prune origin main
    test "$(git rev-parse origin/main)" = "$revision" || { echo 'Deployment was superseded by a newer main revision' >&2; exit 1; }
    git merge --ff-only "$revision"
    export DEPLOY_IMAGE_TAG="$revision"
    exec sh deploy/deploy.sh
    ;;
  *)
    echo 'Only image loading and revision deployment are allowed' >&2
    exit 64
    ;;
esac
