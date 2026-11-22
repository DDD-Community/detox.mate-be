#!/usr/bin/env bash
set -euo pipefail
umask 077
: "${GCP_PROJECT:?}" "${GCP_ZONE:?}" "${GCP_VM:?}" "${GCP_SSH_HOST_KEY:?}"
: "${GITHUB_RUN_ID:?}" "${GITHUB_RUN_ATTEMPT:?}" "${RUNNER_TEMP:?}"
version=$(cat .java-version)
release="$GITHUB_RUN_ID-$GITHUB_RUN_ATTEMPT"
[[ "$version" =~ ^21[.][0-9.]+[+][0-9]+$ && "$release" =~ ^[0-9]+-[0-9]+$ ]]
shopt -s nullglob
jars=()
for file in build/libs/*.jar; do
  [[ "$file" == *-plain.jar ]] || jars+=("$file")
done
[[ ${#jars[@]} == 1 ]]
work=$(mktemp -d "$RUNNER_TEMP/detoxmate.XXXXXXXX")
trap 'rm -rf -- "$work"' EXIT
install -m 600 "${jars[0]}" "$work/app.jar"
cp .java-version "$work/java-version"
cp infra/gcp/{activate-release,install-temurin}.sh "$work/"
cd "$work"
sha256sum app.jar > app.jar.sha256
# systemd EnvironmentFile supports quoted values spanning multiple lines.
python3 - <<'PY'
import os
names = ("JWT_SECRET", "JWT_ACCESS_TOKEN_EXPIRES_IN", "ADMIN_REVIEW_TOKEN",
         "DISCORD_OCR_REPORT_WEBHOOK_URL", "APPLE_CLIENT_ID", "APPLE_TEAM_ID",
         "APPLE_KEY_ID", "APPLE_PRIVATE_KEY", "KAKAO_ADMIN_KEY",
         "PROVIDER_TOKEN_ENCRYPTION_KEY", "APP_PUBLIC_BASE_URL", "STORAGE_IMAGE_READ_BASE_URL",
         "S3_BUCKET_NAME", "AWS_REGION", "S3_PRESIGNED_URL_EXPIRES_IN")
for name in ("JWT_SECRET", "STORAGE_IMAGE_READ_BASE_URL", "S3_BUCKET_NAME"):
    if not os.environ.get(name):
        raise SystemExit(f"Missing prod setting: {name}")
with open("app.env", "x") as output:
    for name in names:
        value = os.environ.get(name)
        if value:
            value = value.replace("\\", "\\\\").replace('"', '\\"')
            output.write(f'{name}="{value}"\n')
PY
ssh-keygen -q -t ed25519 -N '' -f "$work/id"
gcloud compute os-login ssh-keys add --project="$GCP_PROJECT" --key-file="$work/id.pub" --ttl=20m --quiet >/dev/null
login=$(gcloud compute os-login describe-profile --project="$GCP_PROJECT" --format='value(posixAccounts[0].username)')
[[ "$login" =~ ^sa_[0-9]+$ ]]
printf 'detoxmate-prod %s\n' "$GCP_SSH_HOST_KEY" > known_hosts
cat > ssh-config <<EOF
Host detoxmate-prod
  Hostname $GCP_VM
  User $login
  HostKeyAlias detoxmate-prod
  IdentityFile $work/id
  IdentitiesOnly yes
  BatchMode yes
  StrictHostKeyChecking yes
  UserKnownHostsFile $work/known_hosts
  ProxyCommand gcloud compute start-iap-tunnel $GCP_VM 22 --listen-on-stdin --project=$GCP_PROJECT --zone=$GCP_ZONE --verbosity=warning
  ControlMaster auto
  ControlPath $work/ssh-socket
  ControlPersist 45s
EOF
remote="/opt/detoxmate/app/releases/$release"
ssh -F ssh-config detoxmate-prod "umask 027; mkdir -p '$remote'"
scp -F ssh-config app.jar app.jar.sha256 app.env java-version activate-release.sh install-temurin.sh "detoxmate-prod:$remote/"
ssh -F ssh-config detoxmate-prod "bash '$remote/activate-release.sh' '$release'"
