#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo_root"

remote="${1:-origin}"
base_ref="${2:-main}"

branches=(
  "feature-bootstrap-parent"
  "feature-common-modules"
  "feature-api-modules"
  "feature-db-migration-bootstrap"
  "feature-gateway-skeleton"
  "feature-auth-skeleton"
  "feature-merchant-skeleton"
  "feature-product-order-skeleton"
  "feature-release-config-skeleton"
  "feature-supporting-services-skeleton"
  "feature-skeleton-validation"
)

echo "Pushing planned branches from ${base_ref} to ${remote}..."

for branch in "${branches[@]}"; do
  echo "  -> ${branch}"
  git push "${remote}" "${base_ref}:refs/heads/${branch}"
done

echo "Done."
