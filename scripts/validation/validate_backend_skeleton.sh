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

work_repo="${CDD_MAVEN_REPO:-$repo_root/.m2/repository}"
settings_file="${CDD_MAVEN_SETTINGS:-}"

cleanup_files=()
PIDS_TO_KILL=()

cleanup() {
  local code=$?
  for pid in "${PIDS_TO_KILL[@]-}"; do
    kill "$pid" >/dev/null 2>&1 || true
    wait "$pid" >/dev/null 2>&1 || true
  done
  for file in "${cleanup_files[@]-}"; do
    rm -f "$file"
  done
  exit "$code"
}
trap cleanup EXIT

mktemp_file() {
  mktemp "${TMPDIR:-/tmp}/$1.XXXXXX"
}

if [[ -z "$settings_file" ]]; then
  settings_file="$(mktemp_file chengdd-mvn-settings)"
  cleanup_files+=("$settings_file")
  cat >"$settings_file" <<'EOF'
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
</settings>
EOF
fi

mvn_base=(mvn -q -s "$settings_file" "-Dmaven.repo.local=$work_repo")

run_boot_check() {
  local module="$1"
  local main_class="$2"
  local log_file
  log_file="$(mktemp_file "$module")"
  cleanup_files+=("$log_file")

  "${mvn_base[@]}" -f "${parent_root}/${module}/pom.xml" spring-boot:run -Dspring-boot.run.arguments=--server.port=0 >"$log_file" 2>&1 &
  local pid=$!
  PIDS_TO_KILL+=("$pid")

  for _ in $(seq 1 90); do
    if grep -q "Started ${main_class}" "$log_file"; then
      echo "Boot check passed for ${module}"
      kill "$pid" >/dev/null 2>&1 || true
      wait "$pid" >/dev/null 2>&1 || true
      return 0
    fi
    if ! kill -0 "$pid" >/dev/null 2>&1; then
      echo "Boot check failed for ${module}" >&2
      cat "$log_file" >&2
      return 1
    fi
    sleep 1
  done

  echo "Boot check timed out for ${module}" >&2
  cat "$log_file" >&2
  return 1
}

echo "Validating module boundaries..."
python3 scripts/validation/check_module_boundaries.py

echo "Running Maven validate..."
"${mvn_base[@]}" -f "${parent_root}/pom.xml" -DskipTests validate

echo "Running Maven compile..."
"${mvn_base[@]}" -f "${parent_root}/pom.xml" -DskipTests compile

echo "Running Maven install..."
"${mvn_base[@]}" -f "${parent_root}/pom.xml" -DskipTests install

echo "Checking migration config..."
test -f config/db-migration/application-db-migration.yml

echo "Running startup smoke checks..."
run_boot_check "cdd-auth-service" "AuthServiceApplication"
run_boot_check "cdd-gateway" "GatewayApplication"
run_boot_check "cdd-merchant-service" "MerchantServiceApplication"

echo "Backend skeleton validation passed."
