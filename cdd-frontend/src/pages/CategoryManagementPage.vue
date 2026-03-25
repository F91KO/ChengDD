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

    <section :class="$style.grid">
      <div :class="$style.mainColumn">
        <UiCard elevated :class="$style.panel">
          <div :class="$style.panelHead">
            <div>
              <div :class="$style.eyebrowText">模板初始化</div>
              <h3 :class="$style.titleText">选择分类模板并初始化分类树</h3>
            </div>
            <div :class="$style.panelActions">
              <UiButton variant="secondary" @click="loadPage">刷新数据</UiButton>
              <UiButton variant="secondary" :disabled="!selectedTemplate" @click="openCreateCategory(0)">
                新增一级分类
              </UiButton>
              <UiButton :disabled="submitting === 'init' || !selectedTemplate" @click="handleInitialize">
                {{ submitting === 'init' ? '正在初始化...' : '使用模板初始化' }}
              </UiButton>
            </div>
          </div>

          <div :class="$style.templateGrid">
            <label :class="$style.fieldBlock">
              <span :class="$style.fieldLabel">分类模板</span>
              <select v-model="selectedTemplateValue" :class="$style.select">
                <option value="">请选择模板</option>
                <option v-for="template in templates" :key="template.id" :value="String(template.id)">
                  {{ template.template_name }} · {{ template.template_version }}
                </option>
              </select>
            </label>
            <div v-if="selectedTemplate" :class="$style.templateMeta">
              <UiTag :tone="templateTone(selectedTemplate.status)">{{ templateStatusLabel(selectedTemplate.status) }}</UiTag>
              <span>{{ industryLabel(selectedTemplate.industry_code) }}</span>
              <span>最大层级 {{ selectedTemplate.max_level }}</span>
              <span>节点 {{ selectedTemplate.categories.length }} 个</span>
            </div>
          </div>

          <p v-if="selectedTemplate?.template_desc" :class="$style.templateDesc">
            {{ selectedTemplate.template_desc }}
          </p>

          <div v-if="selectedTemplate" :class="$style.previewList">
            <div
              v-for="node in selectedTemplateRows"
              :key="`${selectedTemplate.id}-${node.template_category_code}`"
              :class="$style.previewRow"
            >
              <span :style="{ paddingInlineStart: `${node.depth * 22}px` }" :class="$style.previewName">
                {{ node.category_name }}
              </span>
              <UiTag :tone="node.is_visible ? 'success' : 'default'">
                {{ node.is_visible ? '前台展示' : '前台隐藏' }}
              </UiTag>
            </div>
          </div>
        </UiCard>

        <UiCard elevated :class="$style.panel">
          <div :class="$style.panelHead">
            <div>
              <div :class="$style.eyebrowText">分类列表</div>
              <h3 :class="$style.titleText">当前商家分类树</h3>
            </div>
            <div :class="$style.treeMeta">
              <span>共 {{ categoryRows.length }} 项</span>
              <span>匹配 {{ filteredCategoryRows.length }} 项</span>
            </div>
          </div>

          <UiStatePanel
            v-if="!categoryRows.length"
            tone="empty"
            title="当前尚未初始化分类"
            description="可先选择平台分类模板，一键生成基础分类结构，再继续新增和调整。"
          />

          <template v-else>
            <div :class="$style.searchPanel">
              <span :class="$style.searchIcon">⌕</span>
              <input
                v-model.trim="searchKeyword"
                :class="$style.searchInput"
                type="search"
                placeholder="搜索分类名称..."
              />
            </div>

            <div v-if="searchKeyword && filteredCategoryRows.length" :class="$style.batchHint">
              <div :class="$style.batchHintText">
                已匹配到 {{ filteredCategoryRows.length }} 个相关分类，勾选后可进行批量操作
              </div>
              <button type="button" :class="$style.batchLink" @click="toggleSelectAllFiltered">
                {{ allFilteredSelected ? '取消全选' : '全选结果' }}
              </button>
            </div>

            <UiStatePanel
              v-if="searchKeyword && !filteredCategoryRows.length"
              tone="empty"
              title="未匹配到分类"
              description="可尝试其他关键词，或先新增分类。"
            />

            <div v-else :class="$style.categoryList">
              <div
                v-for="row in filteredCategoryRows"
                :key="row.id"
                :class="[
                  $style.categoryRow,
                  selectedCategory?.id === row.id ? $style.categoryRowActive : '',
                  isCategoryMatched(row) ? $style.categoryRowMatched : '',
                ]"
                @click="selectCategory(row.id)"
              >
                <label :class="$style.selectBox" @click.stop>
                  <input
                    :checked="selectedRowIds.includes(row.id)"
                    type="checkbox"
                    @change="toggleRowSelection(row.id)"
                  />
                </label>

                <div :class="$style.categoryMain" :style="{ paddingInlineStart: `${row.depth * 24}px` }">
                  <div :class="$style.categoryNameLine">
                    <span :class="$style.foldIcon">{{ row.depth > 0 ? '└' : '▸' }}</span>
                    <span :class="$style.categoryName">{{ row.category_name }}</span>
                    <UiTag :tone="row.template_id ? 'primary' : 'default'">
                      {{ row.template_id ? '模板同步' : '商家新增' }}
                    </UiTag>
                    <UiTag :tone="row.is_enabled ? 'success' : 'danger'">
                      {{ row.is_enabled ? '已启用' : '已停用' }}
                    </UiTag>
                    <UiTag :tone="row.is_visible ? 'info' : 'default'">
                      {{ row.is_visible ? '前台展示' : '前台隐藏' }}
                    </UiTag>
                    <span v-if="selectedCategory?.id === row.id" :class="$style.editingBadge">正在编辑</span>
                  </div>
                  <div :class="$style.categoryMeta">
                    <span>层级 {{ row.category_level }}</span>
                    <span>排序 {{ row.sort_order }}</span>
                    <span>父级 {{ row.parentName || '顶级分类' }}</span>
                  </div>
                </div>
                <div :class="$style.inlineRowActions">
                  <UiButton variant="secondary" @click.stop="openCreateCategory(row.id)">新增子分类</UiButton>
                  <UiButton variant="secondary" @click.stop="openEditCategory(row.id)">编辑</UiButton>
                </div>
              </div>
            </div>

            <div v-if="selectedRowIds.length" :class="$style.selectionBar">
              <span>已选择 {{ selectedRowIds.length }} 个分类</span>
              <UiButton variant="secondary" @click="clearSelectedRows">清空选择</UiButton>
            </div>
          </template>
        </UiCard>
      </div>

      <UiCard elevated :class="$style.sidePanel">
        <div :class="$style.panelHead">
          <div>
            <div :class="$style.eyebrowText">{{ formMode === 'edit' ? '编辑分类' : '新增分类' }}</div>
            <h3 :class="$style.titleText">
              {{ formMode === 'edit' && selectedCategory ? selectedCategory.category_name : '分类信息' }}
            </h3>
          </div>
        </div>

        <div v-if="selectedCategory && formMode === 'edit'" :class="$style.detailGrid">
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
        </div>

        <div :class="$style.formGrid">
          <label :class="$style.fieldBlock">
            <span :class="$style.fieldLabel">父级分类</span>
            <select v-model="form.parentId" :class="$style.select" :disabled="formMode === 'edit'">
              <option value="0">顶级分类</option>
              <option v-for="item in parentOptions" :key="item.id" :value="String(item.id)">
                {{ item.category_name }}
              </option>
            </select>
          </label>
          <UiInput v-model="form.categoryName" label="分类名称" placeholder="请输入分类名称" />
          <UiInput v-model="form.sortOrder" label="排序值" placeholder="例如：10" />
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

        <div :class="$style.panelActions">
          <UiButton variant="secondary" @click="resetForm">
            重置表单
          </UiButton>
          <UiButton :disabled="Boolean(submitting)" @click="handleSubmit">
            {{ submitting === 'save' ? '正在保存...' : formMode === 'edit' ? '保存分类' : '创建分类' }}
          </UiButton>
        </div>
      </UiCard>
    </section>
  </WorkspaceLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import UiButton from '@/components/base/UiButton.vue';
import UiCard from '@/components/base/UiCard.vue';
import UiInput from '@/components/base/UiInput.vue';
import UiStatePanel from '@/components/base/UiStatePanel.vue';
import UiTag from '@/components/base/UiTag.vue';
import WorkspaceLayout from '@/components/layout/WorkspaceLayout.vue';
import {
  createCategory,
  fetchCategoryList,
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
type CategoryRowView = ProductCategoryResponseRaw & { depth: number; parentName: string };

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
const selectedRowIds = ref<number[]>([]);

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

const stats = computed(() => [
  { label: '分类总数', value: String(categories.value.length) },
  { label: '启用分类', value: String(categories.value.filter((item) => item.is_enabled).length) },
  { label: '前台展示', value: String(categories.value.filter((item) => item.is_visible).length) },
  { label: '模板同步', value: String(categories.value.filter((item) => item.template_id).length) },
]);

const parentOptions = computed(() =>
  categories.value.filter((item) => item.category_level < 3).sort(sortCategories),
);

const categoryRows = computed<CategoryRowView[]>(() => {
  const byParent = new Map<number, ProductCategoryResponseRaw[]>();
  categories.value.forEach((item) => {
    const current = byParent.get(item.parent_id) ?? [];
    current.push(item);
    byParent.set(item.parent_id, current);
  });
  byParent.forEach((items) => items.sort(sortCategories));

  const rows: CategoryRowView[] = [];
  const walk = (parentId: number, depth: number) => {
    const children = byParent.get(parentId) ?? [];
    children.forEach((child) => {
      rows.push({
        ...child,
        depth,
        parentName: parentCategoryName(child.parent_id),
      });
      walk(child.id, depth + 1);
    });
  };
  walk(0, 0);
  return rows;
});

const filteredCategoryRows = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();
  if (!keyword) {
    return categoryRows.value;
  }
  return categoryRows.value.filter((row) => row.category_name.toLowerCase().includes(keyword));
});

const allFilteredSelected = computed(() => {
  if (!filteredCategoryRows.value.length) {
    return false;
  }
  return filteredCategoryRows.value.every((row) => selectedRowIds.value.includes(row.id));
});

const selectedTemplateRows = computed(() => flattenTemplateNodes(selectedTemplate.value?.categories ?? []));

function sortCategories(left: ProductCategoryResponseRaw, right: ProductCategoryResponseRaw) {
  if (left.sort_order !== right.sort_order) {
    return left.sort_order - right.sort_order;
  }
  return left.id - right.id;
}

function flattenTemplateNodes(nodes: ProductCategoryTemplateNodeResponseRaw[]) {
  const byParent = new Map<string, ProductCategoryTemplateNodeResponseRaw[]>();
  nodes.forEach((item) => {
    const key = item.parent_template_category_code ?? 'ROOT';
    const current = byParent.get(key) ?? [];
    current.push(item);
    byParent.set(key, current);
  });
  byParent.forEach((items) => items.sort((left, right) => left.sort_order - right.sort_order || left.id - right.id));

  const rows: Array<ProductCategoryTemplateNodeResponseRaw & { depth: number }> = [];
  const walk = (parentCode: string | null, depth: number) => {
    const children = byParent.get(parentCode ?? 'ROOT') ?? [];
    children.forEach((child) => {
      rows.push({ ...child, depth });
      walk(child.template_category_code, depth + 1);
    });
  };
  walk(null, 0);
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
  return categories.value.find((item) => item.id === parentId)?.category_name ?? `分类 ${parentId}`;
}

function selectCategory(categoryId: number) {
  selectedCategoryId.value = categoryId;
}

function isCategoryMatched(row: CategoryRowView) {
  const keyword = searchKeyword.value.trim().toLowerCase();
  if (!keyword) {
    return false;
  }
  return row.category_name.toLowerCase().includes(keyword);
}

function toggleRowSelection(categoryId: number) {
  if (selectedRowIds.value.includes(categoryId)) {
    selectedRowIds.value = selectedRowIds.value.filter((id) => id !== categoryId);
    return;
  }
  selectedRowIds.value = [...selectedRowIds.value, categoryId];
}

function toggleSelectAllFiltered() {
  if (allFilteredSelected.value) {
    const filteredSet = new Set(filteredCategoryRows.value.map((item) => item.id));
    selectedRowIds.value = selectedRowIds.value.filter((id) => !filteredSet.has(id));
    return;
  }
  const merged = new Set(selectedRowIds.value);
  filteredCategoryRows.value.forEach((row) => merged.add(row.id));
  selectedRowIds.value = Array.from(merged);
}

function clearSelectedRows() {
  selectedRowIds.value = [];
}

function openCreateCategory(parentId: number) {
  formMode.value = 'create';
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
    const [templateList, categoryList] = await Promise.all([
      fetchCategoryTemplateList(),
      fetchCategoryList({ merchantId, storeId }),
    ]);
    templates.value = templateList;
    categories.value = categoryList;
    if (!selectedTemplateValue.value && templateList[0]) {
      selectedTemplateValue.value = String(templateList[0].id);
    }
    if (!selectedCategoryId.value && categoryList[0]) {
      selectedCategoryId.value = categoryList[0].id;
    }
    const validIds = new Set(categoryList.map((item) => item.id));
    selectedRowIds.value = selectedRowIds.value.filter((id) => validIds.has(id));
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
</script>

<style module>
.grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.86fr);
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

.mainColumn {
  display: grid;
  gap: 18px;
}

.panel,
.sidePanel {
  padding: 20px;
  border-radius: 20px;
  border: 1px solid rgba(9, 29, 46, 0.08);
  background: linear-gradient(180deg, #ffffff 0%, #f9fbff 100%);
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
}

.treeMeta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
  color: var(--cdd-text-soft);
}

.templateGrid,
.formGrid {
  display: grid;
  gap: 14px;
  margin-top: 18px;
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
  min-height: 48px;
  border: 1px solid rgba(9, 29, 46, 0.08);
  border-radius: 16px;
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

.previewList,
.categoryList {
  display: grid;
  gap: 10px;
  margin-top: 18px;
}

.searchPanel {
  position: relative;
  margin-top: 16px;
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

.batchHint {
  margin-top: 12px;
  border-radius: 14px;
  padding: 10px 12px;
  background: rgba(255, 107, 0, 0.12);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.batchHintText {
  color: #9c4304;
  font-size: 12px;
  font-weight: 700;
}

.batchLink {
  border: 0;
  background: transparent;
  color: #ff6b00;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
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

.foldIcon {
  color: #7a8b99;
  width: 14px;
}

.categoryName {
  font-size: 16px;
  font-weight: 800;
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

.selectionBar {
  margin-top: 12px;
  border-radius: 14px;
  padding: 10px 12px;
  background: rgba(237, 244, 255, 0.88);
  border: 1px solid rgba(9, 29, 46, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--cdd-text-soft);
  font-size: 12px;
  font-weight: 700;
}

.detailGrid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.detailValue {
  margin-top: 8px;
  font-weight: 700;
  line-height: 1.7;
}

.switchList {
  display: grid;
  gap: 10px;
  margin-top: 18px;
}

.switchItem {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(237, 244, 255, 0.72);
  font-weight: 700;
}

@media (max-width: 1080px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .sidePanel {
    position: static;
  }

  .selectionBar,
  .batchHint {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 720px) {
  .panel,
  .sidePanel {
    padding: 18px;
  }

  .panelHead,
  .previewRow,
  .categoryRow,
  .selectionBar {
    flex-direction: column;
    align-items: flex-start;
  }

  .inlineRowActions,
  .panelActions {
    width: 100%;
  }

  .inlineRowActions :global(button),
  .panelActions :global(button) {
    width: 100%;
  }

  .detailGrid {
    grid-template-columns: 1fr;
  }
}
</style>
