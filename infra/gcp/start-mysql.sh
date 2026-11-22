#!/usr/bin/env bash
set -euo pipefail
source_root=$(cd "$(dirname "$0")/../.." && pwd)
export DETOXMATE_MYSQL_SECRET_DIR="$HOME/.config/detoxmate/mysql"
install -d -m 0700 "$DETOXMATE_MYSQL_SECRET_DIR"
db_config="$HOME/.local/share/detoxmate"
install -d "$db_config/db/manual"
install -m 0644 "$source_root/docker-compose-gcp.yml" "$db_config/"
install -m 0644 "$source_root"/db/manual/2026-10-13-prod-fresh-{schema,seed}.sql "$db_config/db/manual/"
for secret in root-password app-password; do
  if [ ! -s "$DETOXMATE_MYSQL_SECRET_DIR/$secret" ]; then
    if docker volume inspect detoxmate-prod_mysql_data >/dev/null 2>&1; then
      echo "Existing MySQL data requires its original $secret file" >&2
      exit 1
    fi
    secret_tmp=$(mktemp "$DETOXMATE_MYSQL_SECRET_DIR/.new.XXXXXX")
    openssl rand -hex 32 > "$secret_tmp"
    # The host directory is private. MySQL reads the mounted file as its own UID.
    chmod 0444 "$secret_tmp"
    mv "$secret_tmp" "$DETOXMATE_MYSQL_SECRET_DIR/$secret"
  fi
done
cd "$db_config"
docker compose -f "$db_config/docker-compose-gcp.yml" up -d --wait --wait-timeout 240 mysql
