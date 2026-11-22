#!/usr/bin/env bash
set -euo pipefail
cd /opt/detoxmate/app
release="releases/${1:?release ID required}"
[[ "$1" =~ ^[0-9]+-[0-9]+$ ]]
cd "$release"
sha256sum --check app.jar.sha256
expected=$(cat java-version)
[[ "$expected" =~ ^21[.][0-9.]+[+][0-9]+$ ]]
jdk="/opt/detoxmate/app/jdks/$expected"
bash install-temurin.sh "$expected" /opt/detoxmate/app/jdks
runtime=$("$jdk/bin/java" -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java.runtime.version = //p')
[[ "${runtime%-LTS}" == "$expected" ]] || { echo "Unexpected Java runtime: $runtime" >&2; exit 1; }
printf 'Runtime: %s\n' "$runtime"
ln -sfn "$jdk" jdk
chmod 0750 .
chmod 0640 app.jar app.env
cd /opt/detoxmate/app
previous=$(readlink current || true)
publish() {
  ln -sfn "$1" ".current-$2"
  mv -Tf ".current-$2" current
}
publish "$release" "$1"
healthy() {
  for ((i=0; i<48; i++)); do
    if curl -fsS --max-time 3 http://127.0.0.1:8081/actuator/health 2>/dev/null | python3 -c 'import json,sys; sys.exit(json.load(sys.stdin).get("status") != "UP")' 2>/dev/null; then
      return 0
    fi
    sleep 3
  done
  return 1
}
if sudo -n /usr/bin/systemctl restart detoxmate-api.service && healthy; then
  echo "API health UP"
  for old in releases/*; do
    [[ -d "$old" && "$old" != "$release" && "$old" != "$previous" ]] || continue
    [[ "${old#releases/}" =~ ^[0-9]+-[0-9]+$ ]] && rm -rf -- "$old"
  done
  exit 0
fi
if [[ -n "$previous" && "$previous" != "$release" ]]; then
  publish "$previous" "$1"
  sudo -n /usr/bin/systemctl restart detoxmate-api.service
  healthy && echo "Previous release restored" >&2
fi
exit 1
