#!/usr/bin/env bash
# Nightly backup: a plain pg_dump of the database plus a tarball of uploaded files, both dropped
# in ./backups with a timestamped name. Run daily from the host crontab (see DEPLOYMENT.md).
# Keeps the last 14 days locally - copy these off-box too (rsync/S3/etc.); a backup that only
# ever lives on the server it's backing up isn't a real backup.
set -euo pipefail
cd "$(dirname "$0")/.."

if [ ! -f .env ]; then
  echo "Missing .env in repo root." >&2
  exit 1
fi
set -a; source .env; set +a

STAMP=$(date +%Y-%m-%d_%H%M%S)
mkdir -p backups

echo "==> Dumping database..."
docker compose -f docker-compose.prod.yml exec -T postgres \
  pg_dump -U "$DB_USERNAME" "$DB_NAME" | gzip > "backups/db_${STAMP}.sql.gz"

echo "==> Archiving uploaded files..."
# Reuse the already-running backend container (it already has the uploads volume mounted at
# /data/uploads) rather than guessing the compose-generated volume name for a fresh `docker run`.
docker compose -f docker-compose.prod.yml exec -T backend tar czf - -C /data uploads \
  > "backups/uploads_${STAMP}.tar.gz"

echo "==> Pruning backups older than 14 days..."
find backups -type f -mtime +14 -delete

echo "Done: backups/db_${STAMP}.sql.gz, backups/uploads_${STAMP}.tar.gz"
