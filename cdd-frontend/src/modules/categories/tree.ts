import type { ProductCategoryResponseRaw } from '@/types/product';

export type HierarchyOption = {
  value: string;
  id: number;
  parentId: number;
  depth: number;
  label: string;
  pathLabel: string;
  searchText: string;
};

function sortCategories(left: ProductCategoryResponseRaw, right: ProductCategoryResponseRaw): number {
  if (left.sort_order !== right.sort_order) {
    return left.sort_order - right.sort_order;
  }
  if (left.category_level !== right.category_level) {
    return left.category_level - right.category_level;
  }
  return left.id - right.id;
}

function createOption(
  category: ProductCategoryResponseRaw,
  depth: number,
  ancestorNames: string[],
): HierarchyOption {
  const pathNames = [...ancestorNames, category.category_name];
  return {
    value: String(category.id),
    id: category.id,
    parentId: category.parent_id,
    depth,
    label: `${depth > 0 ? `${'|- '.repeat(depth)}` : ''}${category.category_name}`,
    pathLabel: pathNames.join(' / '),
    searchText: [
      category.category_name,
      pathNames.join(' '),
      String(category.id),
    ]
      .join(' ')
      .toLowerCase(),
  };
}

export function buildCategoryOptions(
  categories: ProductCategoryResponseRaw[],
  predicate?: (category: ProductCategoryResponseRaw) => boolean,
): HierarchyOption[] {
  const normalized = predicate ? categories.filter(predicate) : categories.slice();
  const categoryMap = new Map(normalized.map((item) => [item.id, item]));
  const childrenMap = new Map<number, ProductCategoryResponseRaw[]>();

  normalized.forEach((item) => {
    const parentId = item.parent_id === item.id || !categoryMap.has(item.parent_id) ? 0 : item.parent_id;
    const current = childrenMap.get(parentId) ?? [];
    current.push(item);
    childrenMap.set(parentId, current);
  });

  childrenMap.forEach((items) => items.sort(sortCategories));

  const visited = new Set<number>();
  const rows: HierarchyOption[] = [];

  const walk = (parentId: number, ancestorNames: string[], depth: number) => {
    const children = childrenMap.get(parentId) ?? [];
    children.forEach((child) => {
      if (visited.has(child.id)) {
        return;
      }
      visited.add(child.id);
      rows.push(createOption(child, depth, ancestorNames));
      walk(child.id, [...ancestorNames, child.category_name], depth + 1);
    });
  };

  walk(0, [], 0);

  normalized
    .filter((item) => !visited.has(item.id))
    .sort(sortCategories)
    .forEach((item) => {
      rows.push(createOption(item, Math.max(0, item.category_level - 1), []));
    });

  return rows;
}

export function filterHierarchyOptions(options: HierarchyOption[], keyword: string): HierarchyOption[] {
  const normalizedKeyword = keyword.trim().toLowerCase();
  if (!normalizedKeyword) {
    return options;
  }
  return options.filter((item) => item.searchText.includes(normalizedKeyword));
}

export function findHierarchyOption(options: HierarchyOption[], value: string): HierarchyOption | null {
  return options.find((item) => item.value === value) ?? null;
}
