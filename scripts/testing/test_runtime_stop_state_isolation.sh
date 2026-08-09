#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture_root="$(mktemp -d /tmp/chengdd-runtime-stop-state-isolation.XXXXXX)"
fixture_bin="$fixture_root/bin"
state_dir="$fixture_root/runtime-state"
mkdir -p "$fixture_bin" "$state_dir"

cleanup() {
  local code=$?
  rm -rf "$fixture_root"
  exit "$code"
}
trap cleanup EXIT

cat >"$fixture_bin/ps" <<'EOF'
#!/usr/bin/env bash
if [[ "$*" == *"-ax"* ]]; then
  printf '424242 /fixture/java -jar %s/cdd-parent/cdd-auth-service/target/cdd-auth-service-0.1.0-SNAPSHOT.jar --server.port=8081\n' "$CDD_TEST_REPO_ROOT"
  exit 0
fi
exit 1
EOF
chmod +x "$fixture_bin/ps"

cat >"$state_dir/gateway.env" <<EOF
SERVICE_NAME=gateway
MODULE_NAME=cdd-gateway
SERVICE_PORT=19080
SERVICE_PID=999999
PROCESS_START_MARKER=dead-marker
JAVA_PATH=/fixture/java
JAR_PATH=${repo_root}/cdd-parent/cdd-gateway/target/cdd-gateway-0.1.0-SNAPSHOT.jar
GIT_HEAD=fixture
BACKEND_FINGERPRINT=fixture
STARTED_AT=0
STARTED_AT_TEXT=fixture
EOF

started_at="$(date +%s)"
if PATH="$fixture_bin:$PATH" \
  CDD_ENV=local CDD_CONFIG_MODE=file \
  CDD_RUNTIME_STATE_DIR="$state_dir" \
  CDD_RUNTIME_STOP_TIMEOUT_SECONDS=1 \
  CDD_TEST_REPO_ROOT="$repo_root" \
  bash "$repo_root/scripts/local/stop_all_services.sh" >/dev/null 2>&1; then
  echo "Assertion failed: stale prior service port hid an unmanaged auth process on 8081." >&2
  exit 1
fi
elapsed_seconds=$(( $(date +%s) - started_at ))
(( elapsed_seconds <= 2 )) || { echo "Assertion failed: stale-port stop fixture exceeded its deadline." >&2; exit 1; }

echo "runtime stop state isolation checks passed"
