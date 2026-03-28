<template>
  <WorkspaceLayout
    eyebrow="Category"
    title="商品分类"
    description="维护当前商家的分类树，可从平台模板初始化并按经营需要扩展。"
  >
    <UiStatePanel
      v-if="pageState"
      :tone="pageState.tone"
      :title="pageState.title"
      :description="pageState.description"
    />

    <UiStatePanel
      v-if="actionMessage"
      :tone="actionTone"
      title="操作结果"
      :description="actionMessage"
    />

    <section :class="$style.stats">
      <UiCard v-for="stat in stats" :key="stat.label" elevated :class="$style.statCard">
        <div :class="$style.statLabel">{{ stat.label }}</div>
        <div :class="$style.statValue">{{ stat.value }}</div>
      </UiCard>
    </section>

    <UiCard elevated :class="$style.setupPanel">
      <div :class="$style.panelHead">
        <div>
          <div :class="$style.eyebrowText">模板初始化</div>
          <h3 :class="$style.titleText">先选模板，再初始化基础分类树</h3>
        </div>
        <div :class="$style.panelActions">
          <UiButton :disabled="submitting === 'init' || !selectedTemplate" @click="handleInitialize">
            {{ submitting === 'init' ? '正在初始化...' : '使用模板初始化' }}
          </UiButton>
        </div>
      </div>

      <div :class="$style.setupGrid">
        <div :class="$style.setupMain">
          <UiHierarchySelect
            v-model="selectedTemplateValue"
            label="分类模板"
            :options="templateOptions"
            placeholder="请选择模板"
            search-placeholder="搜索模板名称、行业或版本"
            empty-text="请先选择分类模板"
          />
          <div v-if="selectedTemplate" :class="$style.templateMeta">
            <UiTag :tone="templateTone(selectedTemplate.status)">{{ templateStatusLabel(selectedTemplate.status) }}</UiTag>
            <span>{{ industryLabel(selectedTemplate.industry_code) }}</span>
            <span>最大层级 {{ selectedTemplate.max_level }}</span>
            <span>节点 {{ selectedTemplate.categories.length }} 个</span>
          </div>
          <p v-if="selectedTemplate?.template_desc" :class="$style.templateDesc">
            {{ selectedTemplate.template_desc }}
          </p>
          <UiStatePanel
            v-if="!selectedTemplate"
            tone="empty"
            title="当前没有可用模板"
            description="平台后台还没有可初始化的分类模板，请先维护模板后再初始化商家分类树。"
          />
        </div>

        <div v-if="selectedTemplate" :class="$style.previewPanel">
          <CategoryTemplateTreePreview
            :rows="selectedTemplateRows"
            title="模板预览"
            :summary="`共 ${selectedTemplateRows.length} 个节点`"
            compact
            :show-enabled="false"
            visible-label="前台展示"
            hidden-label="前台隐藏"
          />
        </div>
        <UiStatePanel
          v-else
          tone="empty"
          title="模板预览不可用"
          description="选择模板后，这里会显示完整节点结构和层级预览。"
        />
      </div>
    </UiCard>

    <section :class="$style.grid">
      <ProductCategoryWorkbench
        :rows="pagedCategoryRows"
        :selected-category-id="selectedCategory?.id ?? null"
        :selected-category-name="selectedCategory?.category_name ?? ''"
        :search-keyword="searchKeyword"
        :form-mode="formMode"
        :total-categories="categories.length"
        :visible-count="categoryRows.length"
        :current-page-count="pagedCategoryRows.length"
        :page="categoryPagination.page"
        :page-size="categoryPagination.pageSize"
        :total="categoryPageTotal"
        :loading="loading"
        @update:search-keyword="searchKeyword = $event"
        @update:page="handleCategoryPageChange"
        @update:page-size="handleCategoryPageSizeChange"
        @refresh="loadPage"
        @create-root="openCreateCategory(0)"
        @create-child="openCreateCategory"
        @edit="openEditCategory"
        @toggle="toggleCategoryRow"
      />

      <UiCard elevated :class="$style.sidePanel">
        <div :class="$style.panelHead">
          <div>
            <div :class="$style.eyebrowText">{{ formMode === 'edit' ? '编辑分类' : '新增分类' }}</div>
            <h3 :class="$style.titleText">
              {{ formMode === 'edit' && selectedCategory ? selectedCategory.category_name : '分类信息' }}
            </h3>
          </div>
        </div>

        <div :class="$style.editorHint">
          {{ formMode === 'edit' ? '点击左侧分类即可快速切换编辑对象。' : '从左侧列表选择父级，或直接创建一级分类。' }}
        </div>

        <UiStatePanel
          v-if="formMode === 'edit' && !selectedCategory"
          tone="empty"
          title="当前没有选中分类"
          description="请先在左侧列表中选择一个分类，再查看并编辑分类详情。"
        />

        <UiStatePanel
          v-if="!categories.length && formMode === 'create'"
          tone="empty"
          title="当前商家还没有分类"
          description="可以先使用模板初始化，也可以直接创建一级分类作为根节点。"
        />

        <div v-if="selectedCategory && formMode === 'edit'" :class="$style.formSection">
          <div :class="$style.sectionHeader">
            <div :class="$style.sectionTitle">当前分类信息</div>
          </div>
          <div :class="$style.detailGrid">
            <div>
              <div :class="$style.detailLabel">分类 ID</div>
              <div :class="$style.detailValue">{{ selectedCategory.id }}</div>
            </div>
            <div>
              <div :class="$style.detailLabel">来源</div>
              <div :class="$style.detailValue">{{ selectedCategory.template_id ? '模板同步' : '商家新增' }}</div>
            </div>
            <div>
              <div :class="$style.detailLabel">父级分类</div>
              <div :class="$style.detailValue">{{ parentCategoryName(selectedCategory.parent_id) }}</div>
            </div>
            <div>
              <div :class="$style.detailLabel">层级</div>
              <div :class="$style.detailValue">{{ selectedCategory.category_level }}</div>
            </div>
            <div>
              <div :class="$style.detailLabel">完整路径</div>
              <div :class="$style.detailValue">{{ selectedCategoryPathLabel }}</div>
            </div>
          </div>
        </div>

        <div :class="$style.formSection">
          <div :class="$style.sectionHeader">
            <div :class="$style.sectionTitle">基础信息</div>
          </div>
          <div :class="$style.formGrid">
            <UiHierarchySelect
              v-model="form.parentId"
              label="父级分类"
              :options="parentOptions"
              placeholder="请选择父级分类"
              search-placeholder="搜索父级分类名称或路径"
              empty-text="尚未选择父级分类"
              :disabled="formMode === 'edit'"
            />
            <UiInput v-model="form.categoryName" label="分类名称" placeholder="请输入分类名称" />
            <UiInput v-model="form.sortOrder" label="排序值" placeholder="例如：10" />
          </div>
        </div>

        <div :class="$style.formSection">
          <div :class="$style.sectionHeader">
            <div :class="$style.sectionTitle">展示与状态</div>
          </div>
          <div :class="$style.switchList">
            <label :class="$style.switchItem">
              <input v-model="form.enabled" type="checkbox" />
              <span>启用分类</span>
            </label>
            <label :class="$style.switchItem">
              <input v-model="form.visible" type="checkbox" />
              <span>前台展示</span>
            </label>
          </div>
        </div>

        <div :class="$style.actionFooter">
          <UiButton variant="secondary" size="sm" @click="resetForm">
            重置表单
          </UiButton>
          <UiButton :disabled="Boolean(submitting) || !canSubmitForm" @click="handleSubmit">
            {{ submitting === 'save' ? '正在保存...' : formMode === 'edit' ? '保存分类' : '创建分类' }}
          </UiButton>
        </div>
      </UiCard>
    </section>
  </WorkspaceLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import CategoryTemplateTreePreview from '@/components/category/CategoryTemplateTreePreview.vue';
import ProductCategoryWorkbench from '@/components/category/ProductCategoryWorkbench.vue';
import UiButton from '@/components/base/UiButton.vue';
import UiCard from '@/components/base/UiCard.vue';
import UiHierarchySelect from '@/components/base/UiHierarchySelect.vue';
import UiInput from '@/components/base/UiInput.vue';
import UiStatePanel from '@/components/base/UiStatePanel.vue';
import UiTag from '@/components/base/UiTag.vue';
import WorkspaceLayout from '@/components/layout/WorkspaceLayout.vue';
import { buildCategoryOptions, findHierarchyOption } from '@/modules/categories/tree';
import {
  createCategory,
  fetchAllCategoryList,
  fetchCategoryTemplateList,
  initializeCategoryTree,
  updateCategory,
} from '@/services/product';
import { useAuthStore } from '@/stores/auth';
import type {
  ProductCategoryResponseRaw,
  ProductCategoryTemplateNodeResponseRaw,
  ProductCategoryTemplateResponseRaw,
} from '@/types/product';

type SubmitMode = '' | 'init' | 'save';
type FormMode = 'create' | 'edit';
type CategoryRowView = ProductCategoryResponseRaw & {
  depth: number;
  parentName: string;
  pathLabel: string;
  hasChildren: boolean;
  isExpanded: boolean;
  isMatched: boolean;
};

const authStore = useAuthStore();

const loading = ref(false);
const submitting = ref<SubmitMode>('');
const actionMessage = ref('');
const actionTone = ref<'info' | 'error'>('info');
const templates = ref<ProductCategoryTemplateResponseRaw[]>([]);
const categories = ref<ProductCategoryResponseRaw[]>([]);
const selectedCategoryId = ref<number | null>(null);
const selectedTemplateValue = ref('');
const formMode = ref<FormMode>('create');
const searchKeyword = ref('');
const expandedCategoryIds = ref<Set<number>>(new Set());
const categoryPagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
});

const form = reactive({
  parentId: '0',
  categoryName: '',
  sortOrder: '10',
  enabled: true,
  visible: true,
});

const pageState = computed(() => {
  if (loading.value) {
    return {
      tone: 'loading' as const,
      title: '正在加载分类与模板',
      description: '正在读取当前商家分类树和平台模板列表。',
    };
  }
  return null;
});

const selectedCategory = computed(() =>
  categories.value.find((item) => item.id === selectedCategoryId.value) ?? null,
);

const selectedTemplate = computed(() =>
  templates.value.find((item) => item.id === Number(selectedTemplateValue.value)) ?? null,
);
const canSubmitForm = computed(() => (
  form.categoryName.trim().length > 0
  && form.sortOrder.trim().length > 0
));

const stats = computed(() => [
  { label: '分类总数', value: String(categories.value.length) },
  { label: '已启用', value: String(categories.value.filter((item) => item.is_enabled).length) },
  { label: '前台展示', value: String(categories.value.filter((item) => item.is_visible).length) },
  { label: '模板同步', value: String(categories.value.filter((item) => item.template_id).length) },
]);

const allCategoryOptions = computed(() => buildCategoryOptions(categories.value));

const parentOptions = computed(() => [
  {
    value: '0',
    id: 0,
    parentId: 0,
    depth: 0,
    label: '顶级分类',
    pathLabel: '顶级分类',
    searchText: '顶级分类 顶层 根 0',
  },
  ...buildCategoryOptions(categories.value, (item) => item.category_level < 3),
]);

const templateOptions = computed(() =>
  templates.value.map((template) => ({
    value: String(template.id),
    label: `${template.template_name} · ${template.template_version}`,
    pathLabel: `${template.template_name} / ${industryLabel(template.industry_code)} / ${template.template_version}`,
    searchText: [
      template.template_name,
      template.template_version,
      template.industry_code,
      industryLabel(template.industry_code),
      template.template_desc ?? '',
    ]
      .join(' ')
      .toLowerCase(),
  })),
);

const categoryChildrenMap = computed(() => {
  const map = new Map<number, ProductCategoryResponseRaw[]>();
  categories.value.forEach((item) => {
    const current = map.get(item.parent_id) ?? [];
    current.push(item);
    map.set(item.parent_id, current);
  });
  map.forEach((items) => items.sort(sortCategories));
  return map;
});

const matchedCategoryIds = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();
  if (!keyword) {
    return new Set<number>();
  }
  return new Set(
    categories.value
      .filter((item) => {
        const pathLabel = findHierarchyOption(allCategoryOptions.value, String(item.id))?.pathLabel ?? item.category_name;
        return `${item.category_name} ${pathLabel}`.toLowerCase().includes(keyword);
      })
      .map((item) => item.id),
  );
});

const searchVisibleIds = computed(() => {
  if (!searchKeyword.value.trim()) {
    return null;
  }
  const visible = new Set<number>();
  const categoryMap = new Map(categories.value.map((item) => [item.id, item]));
  matchedCategoryIds.value.forEach((id) => {
    let current = categoryMap.get(id) ?? null;
    while (current) {
      if (visible.has(current.id)) {
        break;
      }
      visible.add(current.id);
      current = current.parent_id ? (categoryMap.get(current.parent_id) ?? null) : null;
    }
  });
  return visible;
});

function buildVisibleCategoryRows(parentId: number, depth: number, rows: CategoryRowView[]) {
  const children = categoryChildrenMap.value.get(parentId) ?? [];
  children.forEach((item) => {
    const pathLabel = findHierarchyOption(allCategoryOptions.value, String(item.id))?.pathLabel ?? item.category_name;
    const hasChildren = (categoryChildrenMap.value.get(item.id)?.length ?? 0) > 0;
    const isExpanded = expandedCategoryIds.value.has(item.id);
    const visibleIdSet = searchVisibleIds.value;
    const shouldShow = !visibleIdSet || visibleIdSet.has(item.id);
    if (shouldShow) {
      rows.push({
        ...item,
        depth,
        parentName: parentCategoryName(item.parent_id),
        pathLabel,
        hasChildren,
        isExpanded,
        isMatched: isCategoryMatchedByPath(pathLabel),
      });
    }
    if (hasChildren && (searchKeyword.value.trim() ? hasVisibleDescendant(item.id, searchVisibleIds.value) : isExpanded)) {
      buildVisibleCategoryRows(item.id, depth + 1, rows);
    }
  });
}

const categoryRows = computed<CategoryRowView[]>(() => {
  const rows: CategoryRowView[] = [];
  buildVisibleCategoryRows(0, 0, rows);
  return rows;
});

const filteredCategoryRows = computed(() => categoryRows.value);
const categoryRootSegments = computed<CategoryRowView[][]>(() => {
  const segments: CategoryRowView[][] = [];
  let current: CategoryRowView[] = [];

  filteredCategoryRows.value.forEach((row) => {
    if (row.depth === 0 && current.length) {
      segments.push(current);
      current = [row];
      return;
    }
    current.push(row);
  });

  if (current.length) {
    segments.push(current);
  }

  return segments;
});
const categoryPageTotal = computed(() => categoryRootSegments.value.length);

const pagedCategoryRows = computed(() => {
  const page = Math.max(1, categoryPagination.page);
  const pageSize = Math.max(1, categoryPagination.pageSize);
  const start = (page - 1) * pageSize;
  return categoryRootSegments.value
    .slice(start, start + pageSize)
    .flat();
});

const selectedTemplateRows = computed(() =>
  flattenTemplateNodes(selectedTemplate.value?.categories ?? []).map((node) => ({
    ...node,
    key: `${selectedTemplate.value?.id ?? 'template'}-${node.template_category_code}`,
  })),
);

const selectedCategoryPathLabel = computed(() => {
  if (!selectedCategory.value) {
    return '未选择分类';
  }
  return findHierarchyOption(allCategoryOptions.value, String(selectedCategory.value.id))?.pathLabel
    ?? selectedCategory.value.category_name;
});

function sortCategories(left: ProductCategoryResponseRaw, right: ProductCategoryResponseRaw) {
  if (left.sort_order !== right.sort_order) {
    return left.sort_order - right.sort_order;
  }
  return left.id - right.id;
}

function normalizeTemplateParentCode(parentCode: string | null | undefined): string {
  return parentCode?.trim() ? parentCode.trim() : 'ROOT';
}

function flattenTemplateNodes(nodes: ProductCategoryTemplateNodeResponseRaw[]) {
  const byParent = new Map<string, ProductCategoryTemplateNodeResponseRaw[]>();
  nodes.forEach((item) => {
    const key = normalizeTemplateParentCode(item.parent_template_category_code);
    const current = byParent.get(key) ?? [];
    current.push(item);
    byParent.set(key, current);
  });
  byParent.forEach((items) => items.sort((left, right) => left.sort_order - right.sort_order || left.id - right.id));

  const rows: Array<ProductCategoryTemplateNodeResponseRaw & { depth: number }> = [];
  const visitedCodes = new Set<string>();
  const walk = (parentCode: string | null, depth: number) => {
    const children = byParent.get(normalizeTemplateParentCode(parentCode)) ?? [];
    children.forEach((child) => {
      if (visitedCodes.has(child.template_category_code)) {
        return;
      }
      visitedCodes.add(child.template_category_code);
      rows.push({ ...child, depth });
      walk(child.template_category_code, depth + 1);
    });
  };
  walk(null, 0);
  nodes.forEach((item) => {
    if (visitedCodes.has(item.template_category_code)) {
      return;
    }
    visitedCodes.add(item.template_category_code);
    rows.push({ ...item, depth: Math.max(0, item.category_level - 1) });
  });
  return rows;
}

function industryLabel(industryCode: string) {
  if (industryCode === 'fresh_retail') {
    return '品质生鲜';
  }
  if (industryCode === 'community_fresh') {
    return '社区到家';
  }
  if (industryCode === 'retail') {
    return '零售通用';
  }
  return industryCode;
}

function templateStatusLabel(status: string) {
  if (status === 'recommended') {
    return '推荐模板';
  }
  if (status === 'enabled') {
    return '已启用';
  }
  if (status === 'disabled') {
    return '已停用';
  }
  return status;
}

function templateTone(status: string) {
  if (status === 'recommended') {
    return 'primary' as const;
  }
  if (status === 'enabled') {
    return 'info' as const;
  }
  if (status === 'disabled') {
    return 'danger' as const;
  }
  return 'default' as const;
}

function setActionMessage(message: string, tone: 'info' | 'error' = 'info') {
  actionMessage.value = message;
  actionTone.value = tone;
}

function resetForm() {
  form.parentId = '0';
  form.categoryName = '';
  form.sortOrder = '10';
  form.enabled = true;
  form.visible = true;
  formMode.value = 'create';
}

function parentCategoryName(parentId: number) {
  if (parentId === 0) {
    return '顶级分类';
  }
  return categories.value.find((item) => item.id === parentId)?.category_name ?? '父级分类缺失';
}

function isCategoryMatchedByPath(pathLabel: string) {
  const keyword = searchKeyword.value.trim().toLowerCase();
  if (!keyword) {
    return false;
  }
  return pathLabel.toLowerCase().includes(keyword);
}

function hasVisibleDescendant(categoryId: number, visibleIds: Set<number> | null): boolean {
  if (!visibleIds) {
    return false;
  }
  const children = categoryChildrenMap.value.get(categoryId) ?? [];
  return children.some((child) => visibleIds.has(child.id) || hasVisibleDescendant(child.id, visibleIds));
}

function initializeExpandedState() {
  expandedCategoryIds.value = new Set(
    categories.value.filter((item) => item.category_level <= 2).map((item) => item.id),
  );
}

function ensureCategoryVisible(categoryId: number) {
  const nextExpanded = new Set(expandedCategoryIds.value);
  let current = categories.value.find((item) => item.id === categoryId) ?? null;
  while (current && current.parent_id) {
    nextExpanded.add(current.parent_id);
    current = categories.value.find((item) => item.id === current?.parent_id) ?? null;
  }
  expandedCategoryIds.value = nextExpanded;
}

function toggleCategoryRow(categoryId: number) {
  const nextExpanded = new Set(expandedCategoryIds.value);
  if (nextExpanded.has(categoryId)) {
    nextExpanded.delete(categoryId);
  } else {
    nextExpanded.add(categoryId);
  }
  expandedCategoryIds.value = nextExpanded;
}

function handleCategoryPageChange(page: number) {
  categoryPagination.page = page;
}

function handleCategoryPageSizeChange(pageSize: number) {
  categoryPagination.page = 1;
  categoryPagination.pageSize = pageSize;
}

function openCreateCategory(parentId: number) {
  formMode.value = 'create';
  ensureCategoryVisible(parentId);
  form.parentId = String(parentId);
  form.categoryName = '';
  form.sortOrder = '10';
  form.enabled = true;
  form.visible = true;
}

function openEditCategory(categoryId: number) {
  const category = categories.value.find((item) => item.id === categoryId);
  if (!category) {
    return;
  }
  selectedCategoryId.value = category.id;
  ensureCategoryVisible(category.id);
  formMode.value = 'edit';
  form.parentId = String(category.parent_id);
  form.categoryName = category.category_name;
  form.sortOrder = String(category.sort_order);
  form.enabled = category.is_enabled;
  form.visible = category.is_visible;
}

function getRequiredScope() {
  const merchantId = authStore.merchantIdForQuery;
  const storeId = authStore.storeIdForQuery;
  if (!merchantId || !storeId) {
    throw new Error('当前登录上下文缺少商家或店铺 ID。');
  }
  return { merchantId, storeId };
}

async function loadPage() {
  loading.value = true;
  try {
    await authStore.ensureCurrentContext();
    const { merchantId, storeId } = getRequiredScope();
    const [templatePage, allCategoryRows] = await Promise.all([
      fetchCategoryTemplateList({ page: 1, pageSize: 200 }),
      fetchAllCategoryList({
        merchantId,
        storeId,
        pageSize: 200,
      }),
    ]);
    templates.value = templatePage.list;
    categories.value = allCategoryRows;
    if (!expandedCategoryIds.value.size) {
      initializeExpandedState();
    }
    if (!selectedTemplateValue.value || !templatePage.list.some((item) => String(item.id) === selectedTemplateValue.value)) {
      selectedTemplateValue.value = templatePage.list[0] ? String(templatePage.list[0].id) : '';
    }
    if (!selectedCategoryId.value || !allCategoryRows.some((item) => item.id === selectedCategoryId.value)) {
      selectedCategoryId.value = allCategoryRows[0]?.id ?? null;
    }
    if (selectedCategoryId.value) {
      ensureCategoryVisible(selectedCategoryId.value);
    }
    const rootCategoryCount = allCategoryRows.filter((item) =>
      item.parent_id === 0 || !allCategoryRows.some((candidate) => candidate.id === item.parent_id)).length;
    const maxPage = Math.max(1, Math.ceil(Math.max(1, rootCategoryCount) / categoryPagination.pageSize));
    if (categoryPagination.page > maxPage) {
      categoryPagination.page = maxPage;
    }
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '加载分类页面失败。', 'error');
  } finally {
    loading.value = false;
  }
}

async function handleInitialize() {
  try {
    const templateId = Number(selectedTemplateValue.value);
    if (!Number.isFinite(templateId)) {
      throw new Error('请选择要初始化的分类模板。');
    }
    submitting.value = 'init';
    const { merchantId, storeId } = getRequiredScope();
    const result = await initializeCategoryTree({ merchantId, storeId, templateId });
    await loadPage();
    setActionMessage(result.message || '分类树初始化完成。');
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '分类初始化失败。', 'error');
  } finally {
    submitting.value = '';
  }
}

async function handleSubmit() {
  try {
    const { merchantId, storeId } = getRequiredScope();
    const sortOrder = Number(form.sortOrder);
    if (!form.categoryName.trim()) {
      throw new Error('分类名称不能为空。');
    }
    if (!Number.isFinite(sortOrder)) {
      throw new Error('排序值必须是有效数字。');
    }
    submitting.value = 'save';
    if (formMode.value === 'edit' && selectedCategory.value) {
      await updateCategory({
        categoryId: selectedCategory.value.id,
        merchantId,
        storeId,
        categoryName: form.categoryName.trim(),
        sortOrder,
        enabled: form.enabled,
        visible: form.visible,
      });
      setActionMessage(`分类“${form.categoryName.trim()}”已更新。`);
    } else {
      const parentId = Number(form.parentId);
      await createCategory({
        merchantId,
        storeId,
        parentId: Number.isFinite(parentId) ? parentId : 0,
        categoryName: form.categoryName.trim(),
        sortOrder,
        enabled: form.enabled,
        visible: form.visible,
      });
      setActionMessage(`分类“${form.categoryName.trim()}”已创建。`);
    }
    resetForm();
    await loadPage();
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '保存分类失败。', 'error');
  } finally {
    submitting.value = '';
  }
}

onMounted(() => {
  void loadPage();
});

watch(searchKeyword, () => {
  categoryPagination.page = 1;
});

watch(categoryRootSegments, (segments) => {
  categoryPagination.total = segments.length;
  const maxPage = Math.max(1, Math.ceil(Math.max(1, segments.length) / categoryPagination.pageSize));
  if (categoryPagination.page > maxPage) {
    categoryPagination.page = maxPage;
  }
});
</script>

<style module>
.grid {
  display: grid;
  grid-template-columns: minmax(0, 1.65fr) minmax(360px, 0.95fr);
  gap: 18px;
}

.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.statCard {
  padding: 20px;
}

.statLabel {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.statValue {
  margin-top: 12px;
  font-size: 30px;
  font-weight: 800;
}

.setupPanel,
.panel,
.sidePanel {
  padding: 20px;
  border-radius: 20px;
  border: 1px solid rgba(9, 29, 46, 0.08);
  background: linear-gradient(180deg, #ffffff 0%, #f9fbff 100%);
}

.setupPanel {
  display: grid;
  gap: 18px;
}

.sidePanel {
  position: sticky;
  top: 24px;
  align-self: start;
}

.panelHead {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.eyebrowText {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.titleText {
  margin: 8px 0 0;
  font-size: 24px;
  letter-spacing: -0.04em;
}

.panelActions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.listHeader {
  display: grid;
  justify-items: end;
  gap: 12px;
}

.treeMeta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
  color: var(--cdd-text-soft);
}

.setupGrid {
  display: grid;
  grid-template-columns: minmax(280px, 0.92fr) minmax(0, 1.08fr);
  gap: 16px;
}

.setupMain,
.previewPanel,
.formGrid {
  display: grid;
  gap: 12px;
}

.formGrid {
  margin-top: 16px;
}

.fieldBlock {
  display: grid;
  gap: 8px;
}

.fieldLabel,
.detailLabel {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.select {
  min-height: 44px;
  border: 1px solid rgba(9, 29, 46, 0.08);
  border-radius: 14px;
  padding: 0 14px;
  background: #fff;
  color: var(--cdd-text);
}

.templateMeta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  color: var(--cdd-text-soft);
  font-size: 13px;
}

.templateDesc {
  margin: 14px 0 0;
  color: var(--cdd-text-soft);
  line-height: 1.8;
}

.previewPanel {
  padding: 12px;
  border-radius: 16px;
  background: rgba(237, 244, 255, 0.72);
}

.previewHead {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.previewTitle {
  font-size: 15px;
  font-weight: 800;
}

.previewMeta {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 700;
}

.previewListCompact,
.categoryList {
  display: grid;
  gap: 10px;
}

.listWorkspace {
  display: grid;
  gap: 14px;
  margin-top: 16px;
}

.listToolbar {
  position: sticky;
  top: 0;
  z-index: 1;
  display: grid;
  gap: 10px;
  align-items: start;
  padding-bottom: 4px;
  background: linear-gradient(180deg, #f9fbff 0%, rgba(249, 251, 255, 0.92) 100%);
}

.previewListCompact {
  margin-top: 18px;
  max-height: 320px;
  overflow: auto;
  padding-right: 4px;
}

.listScroller {
  max-height: min(62vh, 920px);
  overflow: auto;
  padding-right: 6px;
}

.searchPanel {
  position: relative;
}

.searchIcon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: #ff6b00;
  font-weight: 800;
}

.searchInput {
  width: 100%;
  min-height: 46px;
  border: 1px solid rgba(255, 107, 0, 0.3);
  border-radius: 14px;
  padding: 0 14px 0 40px;
  background: #fff;
  color: var(--cdd-text);
}

.searchInput:focus {
  outline: none;
  border-color: rgba(255, 107, 0, 0.7);
  box-shadow: 0 0 0 3px rgba(255, 107, 0, 0.14);
}

.previewRow,
.categoryRow {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  border-radius: 18px;
  padding: 14px 16px;
  background: rgba(237, 244, 255, 0.72);
}

.categoryRow {
  text-align: left;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.15s ease;
}

.categoryRow:hover {
  transform: translateY(-1px);
  border-color: rgba(9, 29, 46, 0.12);
}

.categoryRowActive {
  border-color: rgba(255, 107, 0, 0.4);
  background: linear-gradient(135deg, rgba(255, 245, 235, 0.96), rgba(255, 255, 255, 0.96));
}

.categoryRowMatched {
  box-shadow: inset 0 0 0 1px rgba(255, 107, 0, 0.2);
}

.previewName,
.categoryMain {
  min-width: 0;
  flex: 1;
}

.selectBox {
  display: flex;
  align-items: center;
}

.selectBox input {
  width: 16px;
  height: 16px;
  accent-color: #ff6b00;
}

.categoryNameLine {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.foldButton {
  width: 24px;
  height: 24px;
  border: 0;
  border-radius: 8px;
  background: rgba(9, 29, 46, 0.06);
  color: #6b7c8c;
  font-size: 12px;
  font-weight: 900;
  cursor: pointer;
}

.foldButtonEmpty {
  background: transparent;
  cursor: default;
}

.categoryName {
  font-size: 16px;
  font-weight: 800;
}

.categoryPath {
  margin-top: 8px;
  color: var(--cdd-text-faint);
  font-size: 12px;
  line-height: 1.6;
}

.editingBadge {
  margin-left: auto;
  color: #ff6b00;
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.categoryMeta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 8px;
  color: var(--cdd-text-soft);
  font-size: 12px;
}

.inlineRowActions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.editorHint {
  margin-top: 12px;
  border-radius: 14px;
  padding: 10px 12px;
  background: rgba(255, 245, 235, 0.72);
  color: #9c4304;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.6;
}

.selectionBar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(237, 244, 255, 0.88);
  border: 1px solid rgba(9, 29, 46, 0.08);
  color: var(--cdd-text-soft);
  font-size: 12px;
  font-weight: 700;
}

.formSection {
  display: grid;
  gap: 12px;
  margin-top: 16px;
  padding: 12px;
  border-radius: 16px;
  background: rgba(237, 244, 255, 0.52);
}

.sectionHeader {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.sectionTitle {
  font-size: 14px;
  font-weight: 800;
  color: var(--cdd-text);
}

.detailGrid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.detailValue {
  margin-top: 8px;
  font-weight: 700;
  line-height: 1.7;
}

.actionFooter {
  position: sticky;
  bottom: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
  padding-top: 12px;
  background: linear-gradient(180deg, rgba(249, 251, 255, 0) 0%, #f9fbff 28%);
}

.paginationBar {
  padding-top: 4px;
  border-top: 1px solid rgba(9, 29, 46, 0.06);
}

.switchList {
  display: grid;
  gap: 10px;
}

.switchItem {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(237, 244, 255, 0.72);
  font-weight: 700;
}

@media (max-width: 1080px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .setupGrid {
    grid-template-columns: 1fr;
  }

  .sidePanel {
    position: static;
  }

  .listScroller {
    max-height: none;
  }

  .actionFooter,
  .listToolbar {
    position: static;
  }
}

@media (max-width: 720px) {
  .setupPanel,
  .panel,
  .sidePanel {
    padding: 18px;
  }

  .panelHead,
  .previewHead,
  .previewRow,
  .categoryRow {
    flex-direction: column;
    align-items: flex-start;
  }

  .listHeader {
    justify-items: stretch;
  }

  .selectionBar {
    flex-direction: column;
    align-items: flex-start;
  }

  .inlineRowActions,
  .panelActions,
  .actionFooter {
    width: 100%;
  }

  .inlineRowActions :global(button),
  .panelActions :global(button),
  .actionFooter :global(button) {
    width: 100%;
  }

  .detailGrid {
    grid-template-columns: 1fr;
  }
}
</style>
