#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root"
parent_root="$repo_root/cdd-parent"

default_modules="cdd-common-core,cdd-common-security,cdd-auth-service,cdd-gateway,cdd-merchant-service,cdd-product-service,cdd-order-service,cdd-release-service,cdd-config-service,cdd-agent-core,cdd-decoration-service,cdd-marketing-service,cdd-report-service"
modules_csv="${CDD_PHASE1_TEST_MODULES:-$default_modules}"
report_file="${CDD_PHASE1_REPORT_FILE:-$repo_root/docs/05-delivery/reports/phase1-acceptance-latest.md}"
run_skeleton=1
reuse_reports=0
prepare_mysql_test_db=1

usage() {
  cat <<'EOF'
用法：
  bash scripts/testing/run_phase1_acceptance.sh [选项]

选项：
  --reuse-existing-results  复用当前 surefire 测试报告，不重新执行 mvn test
  --skip-skeleton           跳过骨架校验脚本 scripts/validation/validate_backend_skeleton.sh
  --skip-prepare-mysql      跳过 MySQL 测试库重建与迁移准备
  --report-file <路径>      指定测试报告输出路径
  --help                    查看帮助

环境变量：
  CDD_PHASE1_TEST_MODULES   一期测试模块列表（逗号分隔）
  CDD_PHASE1_REPORT_FILE    一期测试报告输出路径
  CDD_MAVEN_SETTINGS        自定义 Maven settings.xml
  CDD_MAVEN_REPO            自定义 Maven 本地仓库目录
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --reuse-existing-results)
      reuse_reports=1
      shift
      ;;
    --skip-skeleton)
      run_skeleton=0
      shift
      ;;
    --skip-prepare-mysql)
      prepare_mysql_test_db=0
      shift
      ;;
    --report-file)
      if [[ $# -lt 2 ]]; then
        echo "参数错误：--report-file 需要提供路径。" >&2
        exit 1
      fi
      report_file="$2"
      shift 2
      ;;
    --help)
      usage
      exit 0
      ;;
    *)
      echo "参数错误：不支持的选项 $1" >&2
      usage
      exit 1
      ;;
  esac
done

if command -v /usr/libexec/java_home >/dev/null 2>&1; then
  export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 21)}"
fi

if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "未检测到 JAVA_HOME，且自动发现 JDK 21 失败。" >&2
  exit 1
fi

java_major="$("$JAVA_HOME/bin/java" -version 2>&1 | awk -F[\".] '/version/ {print $2; exit}')"
if [[ "$java_major" != "21" ]]; then
  echo "JDK 版本不符合基线要求，当前检测到 JDK ${java_major:-unknown}，要求 JDK 21。" >&2
  exit 1
fi

work_repo="${CDD_MAVEN_REPO:-$HOME/.m2/repository}"
settings_file="${CDD_MAVEN_SETTINGS:-}"
cleanup_files=()

cleanup() {
  local code=$?
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
start_time="$(date '+%Y-%m-%d %H:%M:%S %z')"
start_epoch="$(date '+%s')"
executed_commands=()
mvn_test_exit_code=0

if [[ "$run_skeleton" -eq 1 ]]; then
  echo "执行骨架校验脚本..."
  executed_commands+=("bash scripts/validation/validate_backend_skeleton.sh")
  bash scripts/validation/validate_backend_skeleton.sh
else
  echo "已按参数跳过骨架校验。"
fi

if [[ "$reuse_reports" -eq 0 && "$prepare_mysql_test_db" -eq 1 ]]; then
  echo "准备 MySQL 测试库..."
  executed_commands+=("source scripts/testing/prepare_mysql_test_db.sh")
  # shellcheck disable=SC1091
  source "$repo_root/scripts/testing/prepare_mysql_test_db.sh"
elif [[ "$prepare_mysql_test_db" -eq 0 ]]; then
  echo "已按参数跳过 MySQL 测试库准备。"
fi

if [[ "$reuse_reports" -eq 0 ]]; then
  echo "执行一期模块测试..."
  export MAVEN_OPTS="${MAVEN_OPTS:-} -Djdk.attach.allowAttachSelf=true"
  executed_commands+=("MAVEN_OPTS='${MAVEN_OPTS}' mvn -q -s ${settings_file} -Dmaven.repo.local=${work_repo} -f cdd-parent/pom.xml -pl ${modules_csv} -am test")
  set +e
  "${mvn_base[@]}" -f "${parent_root}/pom.xml" -pl "$modules_csv" -am test
  mvn_test_exit_code=$?
  set -e
  if [[ "$mvn_test_exit_code" -ne 0 ]]; then
    echo "一期模块测试执行失败，退出码=${mvn_test_exit_code}，将继续生成测试报告。"
  fi
else
  echo "已按参数复用现有 surefire 测试报告，不重新执行 mvn test。"
  executed_commands+=("复用现有 surefire 测试报告")
fi

end_time="$(date '+%Y-%m-%d %H:%M:%S %z')"
mkdir -p "$(dirname "$report_file")"

commands_text="$(printf '%s\n' "${executed_commands[@]-}")"
export PHASE1_REPORT_FILE="$report_file"
export PHASE1_MODULES_CSV="$modules_csv"
export PHASE1_START_TIME="$start_time"
export PHASE1_END_TIME="$end_time"
export PHASE1_START_EPOCH="$start_epoch"
export PHASE1_COMMANDS_TEXT="$commands_text"
export PHASE1_REUSE_REPORTS="$reuse_reports"
export PHASE1_MVN_TEST_EXIT_CODE="$mvn_test_exit_code"

python3 - <<'PY'
from __future__ import annotations

import os
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

repo_root = Path.cwd()
parent_root = repo_root / "cdd-parent"
report_file = Path(os.environ["PHASE1_REPORT_FILE"])
modules = [item.strip() for item in os.environ["PHASE1_MODULES_CSV"].split(",") if item.strip()]
start_time = os.environ.get("PHASE1_START_TIME", "")
end_time = os.environ.get("PHASE1_END_TIME", "")
start_epoch = int(os.environ.get("PHASE1_START_EPOCH", "0"))
commands = [line for line in os.environ.get("PHASE1_COMMANDS_TEXT", "").splitlines() if line.strip()]
reuse_reports = os.environ.get("PHASE1_REUSE_REPORTS", "0") == "1"
mvn_test_exit_code = int(os.environ.get("PHASE1_MVN_TEST_EXIT_CODE", "0"))

module_totals: dict[str, dict[str, int]] = {}
module_has_report = {module: False for module in modules}
failed_modules: list[str] = []
failed_details: list[str] = []

for module in modules:
    module_totals[module] = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    report_dir = parent_root / module / "target" / "surefire-reports"
    if not report_dir.exists():
        continue
    xml_files = sorted(report_dir.glob("TEST-*.xml"))
    if not xml_files:
        continue
    for xml_file in xml_files:
        if not reuse_reports and int(xml_file.stat().st_mtime) < start_epoch:
            continue
        module_has_report[module] = True
        root = ET.parse(xml_file).getroot()
        tests = int(root.attrib.get("tests", "0"))
        failures = int(root.attrib.get("failures", "0"))
        errors = int(root.attrib.get("errors", "0"))
        skipped = int(root.attrib.get("skipped", "0"))
        totals = module_totals[module]
        totals["tests"] += tests
        totals["failures"] += failures
        totals["errors"] += errors
        totals["skipped"] += skipped
        if failures > 0 or errors > 0:
            failed_modules.append(module)
            txt_file = xml_file.with_name(xml_file.name.replace("TEST-", "").replace(".xml", ".txt"))
            detail = f"{module} 测试失败：{xml_file.name}"
            if txt_file.exists():
                content = txt_file.read_text(encoding="utf-8", errors="ignore")
                snippet_lines = [line.strip() for line in content.splitlines() if line.strip()][:8]
                if snippet_lines:
                    detail += "；摘要：" + " | ".join(snippet_lines)
            failed_details.append(detail)

total_tests = sum(item["tests"] for item in module_totals.values())
total_failures = sum(item["failures"] for item in module_totals.values())
total_errors = sum(item["errors"] for item in module_totals.values())
total_skipped = sum(item["skipped"] for item in module_totals.values())
missing_modules = [module for module, has in module_has_report.items() if not has]

pass_items = []
for module in modules:
    item = module_totals[module]
    if module_has_report[module] and item["failures"] == 0 and item["errors"] == 0:
        pass_items.append(
            f"{module}: tests={item['tests']} failures={item['failures']} errors={item['errors']} skipped={item['skipped']}"
        )

fail_items = []
if not reuse_reports and mvn_test_exit_code != 0:
    fail_items.append(f"mvn test 命令执行失败，退出码={mvn_test_exit_code}")
if total_failures > 0 or total_errors > 0:
    fail_items.append(f"失败统计：failures={total_failures}, errors={total_errors}")
if missing_modules:
    fail_items.append(f"未发现测试报告的模块：{', '.join(missing_modules)}")

acceptance_ok = (
    total_failures == 0
    and total_errors == 0
    and not missing_modules
    and total_tests > 0
    and (reuse_reports or mvn_test_exit_code == 0)
)

unexecuted_risks = []
if reuse_reports:
    unexecuted_risks.append("本轮复用了历史 surefire 报告，未重新执行 mvn test，结果可能与当前代码快照存在偏差。")
if not unexecuted_risks and not missing_modules:
    unexecuted_risks.append("无。")

lines = [
    "# 一期自动化测试报告",
    "",
    "## 测试范围",
    "- 一期核心链路：认证、网关、商户、商品、订单、发布、配置与通用权限校验。",
    "- 验证脚本：scripts/validation/check_module_boundaries.py、scripts/validation/validate_backend_skeleton.sh。",
    "",
    "## 执行时间",
    f"- 开始时间：{start_time}",
    f"- 结束时间：{end_time}",
    "",
    "## 执行命令",
]
for command in commands:
    lines.append(f"- `{command}`")

lines.extend(["", "## 通过项"])
if pass_items:
    for item in pass_items:
        lines.append(f"- {item}")
else:
    lines.append("- 无。")

lines.extend(["", "## 失败项与失败原因"])
if fail_items:
    for item in fail_items:
        lines.append(f"- {item}")
else:
    lines.append("- 无。")

lines.extend(["", "## 关键日志或错误摘要"])
if failed_details:
    for item in failed_details:
        lines.append(f"- {item}")
else:
    lines.append("- 无。")

lines.extend(
    [
        "",
        "## 是否达到当前任务验收标准",
        f"- {'是' if acceptance_ok else '否'}",
        f"- 汇总：tests={total_tests}, failures={total_failures}, errors={total_errors}, skipped={total_skipped}",
        "",
        "## 未执行项与风险",
    ]
)
for item in unexecuted_risks:
    lines.append(f"- {item}")

report_file.write_text("\n".join(lines) + "\n", encoding="utf-8")
print(f"一期测试报告已输出：{report_file}")
if not acceptance_ok:
    print("一期自动化验收未达标，请先处理失败项后再提交。", file=sys.stderr)
    sys.exit(1)
PY

echo "一期自动化测试与验收基线执行完成。"
