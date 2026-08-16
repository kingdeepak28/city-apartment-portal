#!/usr/bin/env bash
# Run daily from the host crontab (see DEPLOYMENT.md). The `certbot` service in
# docker-compose.prod.yml already renews the certificate on its own schedule when it's due -
# this just makes nginx pick up a renewed certificate by reloading its config (a hot reload,
# zero downtime). Safe to run even when nothing renewed that day.
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose -f docker-compose.prod.yml exec -T frontend nginx -s reload
