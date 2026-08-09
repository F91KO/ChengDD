#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture_root="$(mktemp -d /tmp/chengdd-runtime-tree-inspection.XXXXXX)"
fixture_bin="$fixture_root/bin"
state_file="$fixture_root/launcher.env"
term_trace="$fixture_root/term.trace"
pgrep_pid_file="$fixture_root/pgrep.pid"
mkdir -p "$fixture_bin"

cleanup() {
  local code=$?
  for fixture_pid in "${root_pid:-}" "${child_pid:-}"; do
    [[ -n "$fixture_pid" ]] || continue
    kill -KILL "$fixture_pid" >/dev/null 2>&1 || true
    wait "$fixture_pid" >/dev/null 2>&1 || true
  done
  [[ ! -s "$pgrep_pid_file" ]] || kill -KILL "$(<"$pgrep_pid_file")" >/dev/null 2>&1 || true
  kill -KILL "${runner_pid:-}" >/dev/null 2>&1 || true
  wait "${runner_pid:-}" >/dev/null 2>&1 || true
  rm -rf "$fixture_root"
  exit "$code"
}
trap cleanup EXIT

cat >"$fixture_bin/pgrep" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' "$$" >"$CDD_TEST_PGREP_PID_FILE"
trap '' TERM
while :; do :; done
EOF
cat >"$fixture_bin/ps" <<'EOF'
#!/usr/bin/env bash
pid=""
previous=""
for argument in "$@"; do if [[ "$previous" == "-p" ]]; then pid="$argument"; fi; previous="$argument"; done
printf 'marker-%s\n' "$pid"
EOF
chmod +x "$fixture_bin"/*

bash -c 'trap '\''printf "root\\n" >>"$1"'\'' TERM; while :; do :; done' _ "$term_trace" &
root_pid=$!
bash -c 'trap '\''printf "child\\n" >>"$1"'\'' TERM; while :; do :; done' _ "$term_trace" &
child_pid=$!
printf 'prior-valid-state\n' >"$state_file"

PATH="$fixture_bin:$PATH" CDD_TEST_PGREP_PID_FILE="$pgrep_pid_file" bash -c '
  source "$1"
  backend_runtime_terminate_owned_tree "$2" "marker-$2" "$(( $(date +%s) + 3 ))" "hung inspection fixture"
' _ "$repo_root/scripts/local/backend_runtime_guard.sh" "$root_pid" &
runner_pid=$!
watchdog_deadline=$(( $(date +%s) + 5 ))
while kill -0 "$runner_pid" >/dev/null 2>&1 && (( $(date +%s) < watchdog_deadline )); do sleep 0.1; done
if kill -0 "$runner_pid" >/dev/null 2>&1; then
  echo "Assertion failed: descendant inspection exceeded its deadline." >&2
  exit 1
fi
runner_status=0
wait "$runner_pid" || runner_status=$?
[[ "$runner_status" -ne 0 ]] || { echo "Assertion failed: unprovable descendant inspection returned success." >&2; exit 1; }
for fixture_pid in "$root_pid" "$child_pid"; do
  kill -0 "$fixture_pid" >/dev/null 2>&1 || { echo "Assertion failed: unprovable inspection killed pid ${fixture_pid}." >&2; exit 1; }
done
[[ ! -s "$term_trace" ]] || { echo "Assertion failed: unprovable inspection signalled a process." >&2; exit 1; }
[[ "$(<"$state_file")" == "prior-valid-state" ]] || { echo "Assertion failed: unprovable inspection changed state." >&2; exit 1; }
[[ -s "$pgrep_pid_file" ]] || { echo "Assertion failed: hung pgrep fixture did not run." >&2; exit 1; }
if kill -0 "$(<"$pgrep_pid_file")" >/dev/null 2>&1; then
  echo "Assertion failed: bounded descendant inspection left pgrep alive." >&2
  exit 1
fi

echo "runtime tree inspection safety checks passed"
