#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root"
parent_root="$repo_root/cdd-parent"

if command -v /usr/libexec/java_home >/dev/null 2>&1; then
  export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 17)}"
fi

if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "JAVA_HOME is not set and JDK 17 was not discovered." >&2
  exit 1
fi

config_file="${CDD_DB_CONFIG:-$repo_root/config/db-migration/application-db-migration.yml}"
if [[ ! -f "$config_file" ]]; then
  echo "Database config file not found: $config_file" >&2
  exit 1
fi

work_repo="${CDD_MAVEN_REPO:-$repo_root/.m2/repository}"
settings_file="${CDD_MAVEN_SETTINGS:-}"
cleanup_files=()

cleanup() {
  local code=$?
  for file in "${cleanup_files[@]:-}"; do
    [[ -n "$file" ]] || continue
    rm -f "$file"
  done
  exit "$code"
}
trap cleanup EXIT

if [[ -z "$settings_file" ]]; then
  settings_file="$(mktemp /tmp/chengdd-mvn-settings.XXXXXX)"
  cleanup_files+=("$settings_file")
  cat >"$settings_file" <<'EOF'
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
</settings>
EOF
fi

mvn -q -s "$settings_file" "-Dmaven.repo.local=$work_repo" -f "${parent_root}/cdd-db-migration/pom.xml" \
  spring-boot:run \
  -Dspring-boot.run.arguments="--spring.config.additional-location=optional:file:${config_file}"
