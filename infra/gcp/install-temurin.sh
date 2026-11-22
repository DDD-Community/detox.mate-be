#!/usr/bin/env bash
# Official Temurin Linux x64 archive; keep each patched runtime beside older releases.
set -euo pipefail
version=${1:?Temurin build version required}
base=${2:?runtime directory required}
[[ "$version" =~ ^21[.][0-9.]+[+][0-9]+$ && "$(uname -sm)" == "Linux x86_64" && "$base" = /* ]]
mkdir -p "$base"
if [[ ! -x "$base/$version/bin/java" ]]; then
  stage=$(mktemp -d "$base/install.XXXXXXXX")
  trap 'rm -rf -- "$stage"' EXIT
  cd "$stage"
  archive="OpenJDK21U-jdk_x64_linux_hotspot_${version/+/_}.tar.gz"
  url="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-$version/$archive"
  curl -fsSL --retry 3 "$url.sha256.txt" -o digest
  checksum=$(awk 'NR==1 {print $1}' digest)
  [[ "$checksum" =~ ^[0-9a-f]{64}$ ]]
  curl -fsSL --retry 3 "$url" -o "$archive"
  printf '%s  %s\n' "$checksum" "$archive" | sha256sum -c -
  mkdir jdk
  tar --strip-components=1 -xzf "$archive" -C jdk
  runtime=$(jdk/bin/java -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java.runtime.version = //p')
  [[ "${runtime%-LTS}" == "$version" ]]
  chmod -R a+rX jdk
  mv -T jdk "$base/$version"
fi
runtime=$("$base/$version/bin/java" -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java.runtime.version = //p')
[[ "${runtime%-LTS}" == "$version" ]]
"$base/$version/bin/java" -version
