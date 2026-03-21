#!/usr/bin/env python3
from __future__ import annotations

import sys
import xml.etree.ElementTree as ET
from pathlib import Path
import re


NS = {"m": "http://maven.apache.org/POM/4.0.0"}
ROOT = Path(__file__).resolve().parents[2]
PARENT_ROOT = ROOT / "cdd-parent"
ROOT_POM = PARENT_ROOT / "pom.xml"

REQUIRED_MODULES = {
    "cdd-common-core",
    "cdd-common-db",
    "cdd-common-redis",
    "cdd-common-security",
    "cdd-common-web",
    "cdd-db-migration",
    "cdd-api-auth",
    "cdd-api-merchant",
    "cdd-api-decoration",
    "cdd-api-product",
    "cdd-api-order",
    "cdd-api-marketing",
    "cdd-api-release",
    "cdd-api-report",
    "cdd-api-config",
    "cdd-api-pay",
    "cdd-pay-core",
    "cdd-gateway",
    "cdd-auth-service",
    "cdd-merchant-service",
    "cdd-decoration-service",
    "cdd-product-service",
    "cdd-order-service",
    "cdd-marketing-service",
    "cdd-release-service",
    "cdd-report-service",
    "cdd-config-service",
}
OPTIONAL_MODULES = {
    "cdd-agent-core",
}

EXPECTED_PROFILES = {"local", "dev", "test", "prod"}
EXPECTED_EXECUTABLE_MODULES = {
    "cdd-gateway",
    "cdd-auth-service",
    "cdd-merchant-service",
    "cdd-decoration-service",
    "cdd-product-service",
    "cdd-order-service",
    "cdd-marketing-service",
    "cdd-release-service",
    "cdd-report-service",
    "cdd-config-service",
    "cdd-db-migration",
}


def parse_xml(path: Path) -> ET.Element:
    return ET.parse(path).getroot()


def text_of(element: ET.Element | None, default: str = "") -> str:
    if element is None or element.text is None:
        return default
    return element.text.strip()


def project_dependencies(pom_path: Path) -> list[str]:
    root = parse_xml(pom_path)
    deps = []
    for dependency in root.findall("./m:dependencies/m:dependency", NS):
        group_id = text_of(dependency.find("m:groupId", NS))
        artifact_id = text_of(dependency.find("m:artifactId", NS))
        if group_id == "com.cdd" and artifact_id:
            deps.append(artifact_id)
    return deps


def project_properties(pom_path: Path) -> dict[str, str]:
    root = parse_xml(pom_path)
    properties = root.find("./m:properties", NS)
    if properties is None:
        return {}
    result: dict[str, str] = {}
    for child in list(properties):
        tag = child.tag.split("}", 1)[-1]
        result[tag] = text_of(child)
    return result


def project_plugins(pom_path: Path) -> set[str]:
    root = parse_xml(pom_path)
    plugins = set()
    for plugin in root.findall("./m:build/m:plugins/m:plugin", NS):
        artifact_id = text_of(plugin.find("m:artifactId", NS))
        if artifact_id:
            plugins.add(artifact_id)
    return plugins


def project_artifact_id(pom_path: Path) -> str:
    root = parse_xml(pom_path)
    artifact_id = text_of(root.find("./m:artifactId", NS))
    if artifact_id:
        return artifact_id
    parent = root.find("./m:parent", NS)
    return text_of(parent.find("m:artifactId", NS) if parent is not None else None)


def module_category(module_name: str) -> str:
    if module_name.startswith("cdd-common-"):
        return "common"
    if module_name == "cdd-agent-core":
        return "agent"
    if module_name == "cdd-db-migration":
        return "tool"
    if module_name.startswith("cdd-api-"):
        return "api"
    if module_name == "cdd-pay-core":
        return "pay-core"
    if module_name == "cdd-gateway":
        return "gateway"
    if module_name.endswith("-service"):
        return "service"
    if module_name == "cdd-parent":
        return "parent"
    return "unknown"


def validate_root_modules() -> list[str]:
    root = parse_xml(ROOT_POM)
    modules = {
        text_of(module)
        for module in root.findall("./m:modules/m:module", NS)
        if text_of(module)
    }
    errors = []
    missing = sorted(REQUIRED_MODULES - modules)
    extra = sorted(modules - REQUIRED_MODULES - OPTIONAL_MODULES)
    if missing:
        errors.append(f"根聚合模块缺失: {', '.join(missing)}")
    if extra:
        errors.append(f"根聚合模块存在未登记项: {', '.join(extra)}")
    return errors


def validate_root_profiles() -> list[str]:
    root = parse_xml(ROOT_POM)
    profiles = {
        text_of(profile.find("m:id", NS))
        for profile in root.findall("./m:profiles/m:profile", NS)
        if text_of(profile.find("m:id", NS))
    }
    missing = sorted(EXPECTED_PROFILES - profiles)
    if missing:
        return [f"根 profile 缺失: {', '.join(missing)}"]
    return []


def validate_project_boundaries(pom_path: Path) -> list[str]:
    artifact_id = project_artifact_id(pom_path)
    category = module_category(artifact_id)
    deps = project_dependencies(pom_path)
    properties = project_properties(pom_path)
    plugins = project_plugins(pom_path)
    errors: list[str] = []

    for dep in deps:
        dep_category = module_category(dep)
        if category == "common" and dep_category in {"api", "service", "gateway", "pay-core"}:
            errors.append(f"{artifact_id} 不允许依赖 {dep}")
        if category == "agent" and dep_category in {"api", "service", "gateway", "pay-core"}:
            errors.append(f"{artifact_id} 不允许依赖 {dep}")
        if category == "api" and dep_category in {"service", "gateway", "pay-core"}:
            errors.append(f"{artifact_id} 不允许依赖 {dep}")
        if category == "pay-core" and dep_category in {"service", "gateway"}:
            errors.append(f"{artifact_id} 不允许依赖 {dep}")
        if category == "gateway" and dep_category == "service":
            errors.append(f"{artifact_id} 不允许依赖 {dep}")
        if category == "service" and dep_category in {"service", "gateway"}:
            errors.append(f"{artifact_id} 不允许依赖 {dep}")

    has_boot_plugin = "spring-boot-maven-plugin" in plugins
    repackage_enabled = properties.get("spring-boot.repackage.skip") == "false"
    if artifact_id in EXPECTED_EXECUTABLE_MODULES:
        if not has_boot_plugin:
            errors.append(f"{artifact_id} 必须声明 spring-boot-maven-plugin")
        if not repackage_enabled:
            errors.append(f"{artifact_id} 必须设置 spring-boot.repackage.skip=false")
    else:
        if has_boot_plugin:
            errors.append(f"{artifact_id} 不应声明 spring-boot-maven-plugin")
        if repackage_enabled:
            errors.append(f"{artifact_id} 不应启用 Spring Boot repackage")

    return errors


def validate_migration_assets() -> list[str]:
    migration_dir = ROOT / "db" / "migration"
    entries: list[tuple[int, str]] = []
    for path in migration_dir.glob("V*.sql"):
        match = re.match(r"^V(\d+)__.+\.sql$", path.name)
        if not match:
            return [f"迁移脚本命名不合法: {path.name}"]
        entries.append((int(match.group(1)), path.name))
    if not entries:
        return ["未找到 db/migration/V*.sql 迁移脚本"]
    entries.sort(key=lambda item: item[0])
    versions = [item[0] for item in entries]
    if versions[0] != 1:
        return ["迁移版本必须从 V1 开始"]
    if len(versions) != len(set(versions)):
        return ["db/migration 下存在重复迁移版本号"]
    for current, nxt in zip(versions, versions[1:]):
        if nxt != current + 1:
            return [f"迁移版本不连续: 缺少 V{current + 1}"]
    return []


def validate_shared_config_assets() -> list[str]:
    expected = [ROOT / "config" / "db-migration" / "application-db-migration.yml"]
    missing = [str(path.relative_to(ROOT)) for path in expected if not path.exists()]
    if missing:
        return [f"缺少配置文件: {', '.join(missing)}"]
    return []


def validate_service_config_assets() -> list[str]:
    errors: list[str] = []
    for module in sorted(EXPECTED_EXECUTABLE_MODULES):
        resource_dir = PARENT_ROOT / module / "src" / "main" / "resources"
        expected = [
            resource_dir / "application.yaml",
            resource_dir / "application-local.yaml",
            resource_dir / "application-dev.yaml",
            resource_dir / "application-test.yaml",
            resource_dir / "application-prod.yaml",
        ]
        missing = [str(path.relative_to(ROOT)) for path in expected if not path.exists()]
        if missing:
            errors.append(f"缺少模块配置文件: {', '.join(missing)}")
    return errors


def main() -> int:
    errors: list[str] = []
    errors.extend(validate_root_modules())
    errors.extend(validate_root_profiles())
    errors.extend(validate_migration_assets())
    errors.extend(validate_shared_config_assets())
    errors.extend(validate_service_config_assets())

    for pom_path in sorted(PARENT_ROOT.glob("cdd-*/pom.xml")):
        errors.extend(validate_project_boundaries(pom_path))

    if errors:
        print("模块边界校验失败:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print("模块边界校验通过。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
