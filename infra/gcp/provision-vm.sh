#!/usr/bin/env bash
set -euo pipefail
[[ $EUID == 0 ]] || { echo 'Run as root on the production VM' >&2; exit 1; }
source_root=$(cd "$(dirname "$0")/../.." && pwd)
if ! id detoxmate-api >/dev/null 2>&1; then
  useradd --system --user-group --no-create-home --shell /usr/sbin/nologin detoxmate-api
fi
install -d -m 0755 /opt/detoxmate
install -d -m 0750 -o root -g detoxmate-api /opt/detoxmate/firebase
install -d -m 0700 /etc/detoxmate
# Reuse the original Compose project, volume and credentials from first boot.
runuser -u detoxmate-deploy -- env HOME=/home/detoxmate-deploy bash "$source_root/infra/gcp/start-mysql.sh"
db_password=$(cat /home/detoxmate-deploy/.config/detoxmate/mysql/app-password)
[[ "$db_password" =~ ^[a-f0-9]{64}$ ]]
umask 077
cat > /etc/detoxmate/mysql.env.tmp <<EOF
PROD_DB_URL=jdbc:mysql://127.0.0.1:3306/detoxmate
PROD_DB_USERNAME=detoxmate
PROD_DB_PASSWORD=$db_password
EOF
mv /etc/detoxmate/mysql.env.tmp /etc/detoxmate/mysql.env
install -m 0644 "$source_root/infra/gcp/detoxmate-api.service" /etc/systemd/system/
systemctl daemon-reload
systemctl enable detoxmate-api.service
if ! command -v caddy >/dev/null; then
  DEBIAN_FRONTEND=noninteractive apt-get install -y caddy
fi
caddy validate --config "$source_root/infra/gcp/Caddyfile" --adapter caddyfile
install -m 0644 "$source_root/infra/gcp/Caddyfile" /etc/caddy/Caddyfile
systemctl enable --now caddy
systemctl reload caddy
if [[ $# == 1 ]]; then
  # OS Login assigns a stable POSIX name to the release service account.
  [[ "$1" =~ ^sa_[0-9]+$ ]]
  chown_id=$(id -u "$1")
  install -d -m 2750 -o "$chown_id" -g detoxmate-api /opt/detoxmate/app{,/releases,/jdks}
  printf '%s ALL=(root) NOPASSWD: /usr/bin/systemctl restart detoxmate-api.service\n' "$1" > /etc/sudoers.d/detoxmate-release.tmp
  chmod 0440 /etc/sudoers.d/detoxmate-release.tmp
  visudo -cf /etc/sudoers.d/detoxmate-release.tmp
  mv /etc/sudoers.d/detoxmate-release{.tmp,}
fi
