#!/bin/sh
set -eu

domain="${DOMAIN:-gradinator.itsyoraaa.su}"
certbot_email="${CERTBOT_EMAIL:?Set CERTBOT_EMAIL before running this script}"
repo_dir="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
source_file="${repo_dir}/deploy/nginx-gradinator.conf"
site_file="/etc/nginx/sites-available/${domain}.conf"
enabled_file="/etc/nginx/sites-enabled/${domain}.conf"

install -o root -g root -m 0644 "$source_file" "$site_file"
ln -sfn "$site_file" "$enabled_file"

/usr/sbin/nginx -t
systemctl reload nginx

certbot --nginx \
  --domain "$domain" \
  --non-interactive \
  --agree-tos \
  --email "$certbot_email" \
  --redirect

/usr/sbin/nginx -t
systemctl reload nginx

echo "Nginx and TLS are configured for https://${domain}"
