#!/usr/bin/env python3
import json
import os
import sys
import urllib.error
import urllib.request


REPO = os.environ.get("GITHUB_REPOSITORY", "F91KO/ChengDD")
TOKEN = os.environ.get("GITHUB_TOKEN")
API_BASE = f"https://api.github.com/repos/{REPO}"


def build_issues(epic_ref: str):
    return [
        {
            "title": "Bootstrap cdd-parent and shared Maven build conventions",
            "branch": "feature-bootstrap-parent",
            "body": f"""## Goal
Bootstrap `cdd-parent` and the shared Maven build baseline for the backend skeleton.

## Scope
- Create the parent module and root build aggregation
- Lock dependency and plugin management conventions
- Define encoding, Java version, test, packaging, and repository rules

## Deliverables
- Parent POM structure is ready for child modules
- Build conventions are centralized
- Other skeleton tasks can depend on this issue

## Dependency
- Parent epic: {epic_ref}

## Suggested branch
- `feature-bootstrap-parent`
""",
        },
        {
            "title": "Create common modules for core, db, web, security, redis",
            "branch": "feature-common-modules",
            "body": f"""## Goal
Create the shared common modules used by all backend services.

## Scope
- `cdd-common-core`
- `cdd-common-db`
- `cdd-common-web`
- `cdd-common-security`
- `cdd-common-redis`

## Deliverables
- Module shells exist and are wired to the parent build
- Shared package conventions are fixed
- Service modules can depend on common modules without cross-service coupling

## Dependency
- Parent epic: {epic_ref}
- Depends on parent/bootstrap task

## Suggested branch
- `feature-common-modules`
""",
        },
        {
            "title": "Create API modules for service contracts",
            "branch": "feature-api-modules",
            "body": f"""## Goal
Create protocol/API modules that hold inter-service contracts and DTOs.

## Scope
- `cdd-api-auth`
- `cdd-api-merchant`
- `cdd-api-product`
- `cdd-api-order`
- `cdd-api-release`
- `cdd-api-decoration`
- `cdd-api-marketing`
- `cdd-api-report`
- `cdd-api-config`
- `cdd-api-pay`

## Deliverables
- API modules are separated from service implementation modules
- Dependency direction is fixed before service development begins

## Dependency
- Parent epic: {epic_ref}
- Depends on parent/bootstrap task

## Suggested branch
- `feature-api-modules`
""",
        },
        {
            "title": "Integrate Flyway with db/migration scripts",
            "branch": "feature-db-migration-bootstrap",
            "body": f"""## Goal
Connect the application skeleton to the existing `db/migration` scripts through Flyway.

## Scope
- Define the migration execution entrypoint
- Wire `V1` to `V9` into the runtime/build conventions
- Ensure migration location and ordering are stable

## Deliverables
- Backend skeleton has one agreed migration mechanism
- Teams stop using the raw schema draft as the execution source

## Dependency
- Parent epic: {epic_ref}
- Depends on parent/bootstrap task

## Suggested branch
- `feature-db-migration-bootstrap`
""",
        },
        {
            "title": "Create gateway skeleton and shared entry conventions",
            "branch": "feature-gateway-skeleton",
            "body": f"""## Goal
Create the gateway module skeleton and define shared entry conventions.

## Scope
- `cdd-gateway`
- Base application entrypoint
- Health endpoint and minimal gateway bootstrap

## Deliverables
- Gateway module can start as an empty service
- Common startup conventions are reusable by other services

## Dependency
- Parent epic: {epic_ref}
- Depends on parent/common/api baseline

## Suggested branch
- `feature-gateway-skeleton`
""",
        },
        {
            "title": "Create auth service skeleton",
            "branch": "feature-auth-skeleton",
            "body": f"""## Goal
Create the authentication service skeleton.

## Scope
- `cdd-auth-service`
- Standard package structure
- Base configuration and startup shell

## Deliverables
- Empty auth service can build and start
- Module depends only on common/api layers, not other service implementations

## Dependency
- Parent epic: {epic_ref}
- Depends on parent/common/api/db baseline

## Suggested branch
- `feature-auth-skeleton`
""",
        },
        {
            "title": "Create merchant service skeleton",
            "branch": "feature-merchant-skeleton",
            "body": f"""## Goal
Create the merchant service skeleton for onboarding-related development.

## Scope
- `cdd-merchant-service`
- Standard package structure
- Base configuration and startup shell

## Deliverables
- Merchant service can build and start
- Ready to receive onboarding, store, and mini-program implementation work

## Dependency
- Parent epic: {epic_ref}
- Depends on parent/common/api/db baseline

## Suggested branch
- `feature-merchant-skeleton`
""",
        },
        {
            "title": "Create product and order service skeletons",
            "branch": "feature-product-order-skeleton",
            "body": f"""## Goal
Create product and order service skeletons together to lock their dependency boundaries early.

## Scope
- `cdd-product-service`
- `cdd-order-service`
- Shared package structure and startup shells

## Deliverables
- Both modules can build and start
- Product/order boundary stays aligned with current architecture docs

## Dependency
- Parent epic: {epic_ref}
- Depends on parent/common/api/db baseline

## Suggested branch
- `feature-product-order-skeleton`
""",
        },
        {
            "title": "Create release and config service skeletons",
            "branch": "feature-release-config-skeleton",
            "body": f"""## Goal
Create release and config service skeletons that support status flow and compensation follow-up work.

## Scope
- `cdd-release-service`
- `cdd-config-service`
- Base startup and package structure

## Deliverables
- Release/config modules can build and start
- Ready for release-task, feature-switch, and compensation follow-up implementation

## Dependency
- Parent epic: {epic_ref}
- Depends on parent/common/api/db baseline

## Suggested branch
- `feature-release-config-skeleton`
""",
        },
        {
            "title": "Create decoration, marketing, and report service skeletons",
            "branch": "feature-supporting-services-skeleton",
            "body": f"""## Goal
Create the remaining supporting service skeletons.

## Scope
- `cdd-decoration-service`
- `cdd-marketing-service`
- `cdd-report-service`

## Deliverables
- Supporting services can build and start
- Architecture-level module coverage is complete

## Dependency
- Parent epic: {epic_ref}
- Depends on parent/common/api/db baseline

## Suggested branch
- `feature-supporting-services-skeleton`
""",
        },
        {
            "title": "Validate module dependency boundaries and startup baseline",
            "branch": "feature-skeleton-validation",
            "body": f"""## Goal
Validate the backend skeleton after all module shells are in place.

## Scope
- Aggregated build validation
- Module dependency direction checks
- Empty service startup baseline
- Migration discovery baseline

## Deliverables
- Skeleton quality gate is explicit
- Future business implementation starts from a verified base

## Dependency
- Parent epic: {epic_ref}
- Depends on all skeleton module issues

## Suggested branch
- `feature-skeleton-validation`
""",
        },
    ]


def github_request(method: str, url: str, payload: dict | None = None):
    headers = {
        "Accept": "application/vnd.github+json",
        "Authorization": f"Bearer {TOKEN}",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "chengdd-bootstrap-issues",
    }
    data = None
    if payload is not None:
        data = json.dumps(payload).encode("utf-8")
        headers["Content-Type"] = "application/json"

    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode("utf-8"))


def create_issue(title: str, body: str):
    payload = {"title": title, "body": body}
    return github_request("POST", f"{API_BASE}/issues", payload)


def main():
    if not TOKEN:
        print("Missing GITHUB_TOKEN", file=sys.stderr)
        sys.exit(1)

    print(f"Creating issues in {REPO}...")

    epic = create_issue(
        "Project skeleton: Java Spring multi-module backend",
        """## Goal
Create the backend project skeleton for ChengDD using Java Spring + Maven multi-module architecture.

## Scope
- Track the skeleton bootstrap work as an umbrella issue
- Use the existing docs and migration scripts as the implementation baseline
- Split work into focused bootstrap issues with one branch per issue

## Success criteria
- Core modules and service shells are fully planned and tracked
- Migration strategy is aligned with `db/migration`
- Dependency direction is fixed before business coding starts
""",
    )

    epic_ref = f"#{epic['number']}"
    print(f"Created epic {epic_ref}: {epic['html_url']}")

    issues = build_issues(epic_ref)
    for issue in issues:
        body = issue["body"].rstrip() + "\n"
        created = create_issue(issue["title"], body)
        print(
            f"Created #{created['number']}: {issue['title']} "
            f"[branch: {issue['branch']}] {created['html_url']}"
        )


if __name__ == "__main__":
    main()
