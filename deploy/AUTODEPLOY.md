# Production deployment

Pushes and merges into `main` run the backend tests, frontend checks, and Docker
builds. GitHub Actions then transfers the exact tested image archives to the
production server over pinned SSH. The server does not compile the application.
Images have immutable commit tags and revision labels; deploy refuses mismatched
images, modified tracked files, and superseded workflow runs.

The deploy key in `~/.ssh/authorized_keys` must use:

```
restrict,command="/home/yor/bin/gradinator-actions-command" ssh-ed25519 ...
```

Install `deploy/ssh-command.sh` at that path with mode 700. Keep the forced command
outside the checkout so Git updates cannot silently replace the key's entrypoint.
It accepts only `load` (Docker archive on stdin) and `deploy <40-character SHA>`.
The production checkout is `/home/yor/apps/Gradinator`, on `main`, with a private
`.env`. Preserve that environment file and the existing named Docker volumes.

Repository secrets: `DEPLOY_HOST`, `DEPLOY_PORT`, `DEPLOY_USER`, `DEPLOY_SSH_KEY`,
`DEPLOY_HOST_FINGERPRINT`. Repository variable `PROD_URL` must be
`https://gradinator.itsyoraaa.su`. The fingerprint must use OpenSSH's `SHA256:` form.
Password and root SSH access remain disabled outside the trusted LAN.

Production profiles: `G_CORE_PROFILES=prod,oauth,oauth-vk`. Google/GitHub
registrations use the `oauth` profile; optional VK uses `oauth-vk`. Provider
credentials stay in `.env` and never enter image builds or Git. Frontend defaults
show Google/GitHub and hide unfinished VK/Yandex login. OAuth callbacks use the
public HTTPS URL and secure session cookies.

Each deployment backs up the environment and databases under a private
`/home/yor/backups/gradinator-deploy-*` directory and tags the previous runtime
images. On failure the previous images are restored without resetting Git or
deleting data. Database migrations must be backward compatible with the previous
runtime; a database restore is a separate manual recovery operation.

After Compose starts the images, both the frontend and groups API must respond.
Actions also verifies them externally. `.deployed-revision` records a successful
revision. Keep `.env` out of version control and keep all tracked production files
clean; changes belong in the repository, not in a persistent server-only patch.
