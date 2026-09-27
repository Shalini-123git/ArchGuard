#!/usr/bin/env bash
# Simple daily Postgres backup for ArchGuard's docker-compose stack.
# Add to crontab, e.g.: 0 3 * * * /opt/archguard/backup-db.sh
set -euo pipefail
cd "$(dirname "$0")"
BACKUP_DIR="./backups"
mkdir -p "$BACKUP_DIR"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
docker compose exec -T db pg_dump -U "${POSTGRES_USER:-archguard}" "${POSTGRES_DB:-archguard}" \
  | gzip > "$BACKUP_DIR/archguard-$TIMESTAMP.sql.gz"
# Keep the last 14 backups only
ls -1t "$BACKUP_DIR"/archguard-*.sql.gz | tail -n +15 | xargs -r rm --