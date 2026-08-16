#!/usr/bin/env bash
# One-time bootstrap: get nginx a real Let's Encrypt certificate.
#
# nginx's config (frontend/nginx/templates/app.conf.template) references
# /etc/letsencrypt/live/$DOMAIN/{fullchain,privkey}.pem, so nginx can't even start until those
# files exist - but certbot can't get a *real* certificate until nginx is up and serving the
# ACME http-01 challenge on port 80. This script breaks that chicken-and-egg loop the standard
# way: boot nginx with a throwaway self-signed cert, use it just long enough to get the real one
# from Let's Encrypt, then restart nginx to pick that up.
#
# Run once per new domain, from the repo root or anywhere: ./deploy/init-letsencrypt.sh
set -euo pipefail
cd "$(dirname "$0")/.."

COMPOSE="docker compose -f docker-compose.prod.yml"

if [ ! -f .env ]; then
  echo "Missing .env in repo root. Copy deploy/.env.prod.example to .env and fill it in first." >&2
  exit 1
fi
set -a; source .env; set +a

: "${DOMAIN:?Set DOMAIN in .env}"
: "${LETSENCRYPT_EMAIL:?Set LETSENCRYPT_EMAIL in .env}"

echo "==> Building images..."
$COMPOSE build

echo "==> Starting postgres + backend (needed before frontend can pass its healthcheck)..."
$COMPOSE up -d postgres backend

echo "==> Writing a throwaway self-signed certificate for $DOMAIN so nginx can boot..."
$COMPOSE run --rm --entrypoint sh frontend -c "
  mkdir -p /etc/letsencrypt/live/$DOMAIN && \
  openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
    -keyout /etc/letsencrypt/live/$DOMAIN/privkey.pem \
    -out /etc/letsencrypt/live/$DOMAIN/fullchain.pem \
    -subj '/CN=$DOMAIN'
"

echo "==> Starting nginx with the throwaway certificate..."
$COMPOSE up -d frontend

echo "==> Requesting the real certificate from Let's Encrypt for $DOMAIN and www.$DOMAIN..."
$COMPOSE run --rm --entrypoint certbot certbot certonly \
  --webroot -w /var/www/certbot \
  -d "$DOMAIN" -d "www.$DOMAIN" \
  --email "$LETSENCRYPT_EMAIL" --agree-tos --no-eff-email \
  --non-interactive --force-renewal

echo "==> Reloading nginx with the real certificate..."
$COMPOSE restart frontend

echo "==> Starting the certbot auto-renewal loop..."
$COMPOSE up -d certbot

echo
echo "Done. https://$DOMAIN should now be serving a trusted certificate."
echo "Certificates auto-renew via the 'certbot' container; nginx itself needs an occasional"
echo "reload to pick up a renewed cert - see deploy/renew-and-reload.sh and DEPLOYMENT.md."
