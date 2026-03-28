<template>
  <WorkspaceLayout
    eyebrow="Template"
    title="分类模板"
    description="平台后台维护统一分类模板，供商家侧初始化分类树时使用。"
    brand-meta="平台后台"
    subject-label="当前后台"
    subject-value="平台模板治理"
    :nav-items="platformMenuItems"
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
      <UiCard v-for="stat in stats" :key="stat.label" elevated :class="[$style.statCard, stat.emphasis ? $style.statCardEmphasis : '']">
        <div :class="$style.statLabel">{{ stat.label }}</div>
        <div :class="$style.statValue">{{ stat.value }}</div>
        <div v-if="stat.hint" :class="$style.statHint">{{ stat.hint }}</div>
      </UiCard>
    </section>

    <section :class="$style.grid">
      <UiCard elevated :class="$style.panel">
        <div :class="$style.panelHead">
          <div>
            <div :class="$style.eyebrowText">模板列表</div>
            <h3 :class="$style.titleText">当前可用分类模板</h3>
          </div>
          <div :class="$style.panelActions">
            <UiButton variant="secondary" @click="loadTemplates">刷新模板</UiButton>
            <UiButton variant="secondary" @click="toggleCreateForm">
              {{ showCreateForm ? '收起创建表单' : '新建模板' }}
            </UiButton>
          </div>
        </div>

        <div :class="$style.panelSubHead">
          <h4 :class="$style.subTitle">核心模板库</h4>
          <div :class="$style.sortText">排序方式：最近更新</div>
        </div>

        <div :class="$style.templateList">
          <button
            v-for="template in pagedTemplates"
            :key="template.id"
            type="button"
            :class="[$style.templateCard, selectedTemplate?.id === template.id ? $style.templateCardActive : '']"
            @click="selectedTemplateId = template.id"
          >
            <div :class="$style.templateCardHead">
              <div>
                <div :class="$style.templateName">{{ template.template_name }}</div>
                <div :class="$style.templateMeta">
                  {{ industryLabel(template.industry_code) }} · {{ template.template_version }}
                </div>
              </div>
              <UiTag :tone="templateTone(template.status)">{{ templateStatusLabel(template.status) }}</UiTag>
            </div>
            <div :class="$style.templateDesc">{{ template.template_desc || '暂无模板说明。' }}</div>
            <div :class="$style.templateFoot">
              <span>最大层级 {{ template.max_level }}</span>
              <span>节点 {{ template.categories.length }} 个</span>
              <span>ID {{ template.id }}</span>
            </div>
          </button>
        </div>

        <UiPagination
          v-if="templatePagination.total"
          :page="templatePagination.page"
          :page-size="templatePagination.pageSize"
          :total="templatePagination.total"
          :disabled="loading"
          @update:page="handleTemplatePageChange"
          @update:page-size="handleTemplatePageSizeChange"
        />
      </UiCard>

      <UiCard elevated :class="$style.sidePanel">
        <div v-if="selectedTemplate" :class="$style.detailWrap">
          <div :class="$style.eyebrowText">模板详情</div>
          <h3 :class="$style.titleText">{{ selectedTemplate.template_name }}</h3>

          <div :class="$style.detailGrid">
            <div>
              <div :class="$style.detailLabel">行业</div>
              <div :class="$style.detailValue">{{ industryLabel(selectedTemplate.industry_code) }}</div>
            </div>
            <div>
              <div :class="$style.detailLabel">版本</div>
              <div :class="$style.detailValue">{{ selectedTemplate.template_version }}</div>
            </div>
            <div>
              <div :class="$style.detailLabel">状态</div>
              <div :class="$style.detailValue">{{ templateStatusLabel(selectedTemplate.status) }}</div>
            </div>
            <div>
              <div :class="$style.detailLabel">最大层级</div>
              <div :class="$style.detailValue">{{ selectedTemplate.max_level }}</div>
            </div>
          </div>

          <p v-if="selectedTemplate.template_desc" :class="$style.description">
            {{ selectedTemplate.template_desc }}
          </p>

          <div :class="$style.previewHead">
            <div :class="$style.previewTitle">结构预览</div>
            <UiTag :tone="templateTone(selectedTemplate.status)">
              {{ templateStatusLabel(selectedTemplate.status) }}
            </UiTag>
          </div>

          <div :class="$style.previewList">
            <div
              v-for="node in selectedTemplateRows"
              :key="`${selectedTemplate.id}-${node.template_category_code}`"
              :class="$style.previewRow"
            >
              <span :style="{ paddingInlineStart: `${node.depth * 22}px` }" :class="$style.previewName">
                {{ node.category_name }}
              </span>
              <div :class="$style.previewTags">
                <UiTag :tone="node.is_enabled ? 'success' : 'danger'">
                  {{ node.is_enabled ? '启用' : '停用' }}
                </UiTag>
                <UiTag :tone="node.is_visible ? 'info' : 'default'">
                  {{ node.is_visible ? '展示' : '隐藏' }}
                </UiTag>
              </div>
            </div>
          </div>
        </div>
        <UiStatePanel
          v-else
          tone="empty"
          title="暂无模板"
          description="请先创建模板，或刷新后重试。"
        />
      </UiCard>
    </section>

    <UiCard v-if="showCreateForm" elevated :class="$style.createPanel">
      <div :class="$style.panelHead">
        <div>
          <div :class="$style.eyebrowText">新建模板</div>
          <h3 :class="$style.titleText">创建分类模板骨架</h3>
        </div>
      </div>

      <div :class="$style.formGrid">
        <UiInput v-model="form.templateName" label="模板名称" placeholder="例如：社区精选模板" />
        <UiInput v-model="form.industryCode" label="行业编码" placeholder="例如：fresh_retail" />
        <UiInput v-model="form.templateVersion" label="模板版本" placeholder="例如：v1.0.1" />
        <UiInput v-model="form.maxLevel" label="最大层级" placeholder="例如：3" />
        <label :class="$style.fieldWide">
          <span :class="$style.fieldLabel">模板说明</span>
          <textarea v-model="form.templateDesc" :class="$style.textarea" placeholder="请输入模板说明" />
        </label>
      </div>

      <div :class="$style.nodeSectionHead">
        <div :class="$style.sectionTitle">模板节点</div>
        <UiButton variant="secondary" @click="addNode">新增节点</UiButton>
      </div>

      <div :class="$style.nodeList">
        <article v-for="node in form.nodes" :key="node.clientId" :class="$style.nodeCard">
          <div :class="$style.nodeGrid">
            <UiInput v-model="node.templateCategoryCode" label="分类编码" placeholder="例如：fresh_fruit" />
            <UiInput v-model="node.parentTemplateCategoryCode" label="父级编码" placeholder="顶级可留空" />
            <UiInput v-model="node.categoryName" label="分类名称" placeholder="例如：水果优选" />
            <UiInput v-model="node.sortOrder" label="排序值" placeholder="例如：10" />
          </div>
          <div :class="$style.nodeActions">
            <label :class="$style.switchItem">
              <input v-model="node.enabled" type="checkbox" />
              <span>启用</span>
            </label>
            <label :class="$style.switchItem">
              <input v-model="node.visible" type="checkbox" />
              <span>前台展示</span>
            </label>
            <UiButton variant="secondary" :disabled="form.nodes.length <= 1" @click="removeNode(node.clientId)">
              删除节点
            </UiButton>
          </div>
        </article>
      </div>

      <div :class="$style.panelActions">
        <UiButton variant="secondary" @click="resetForm">重置表单</UiButton>
        <UiButton :disabled="submitting" @click="handleCreateTemplate">
          {{ submitting ? '正在创建...' : '提交模板' }}
        </UiButton>
      </div>
    </UiCard>
  </WorkspaceLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import UiButton from '@/components/base/UiButton.vue';
import UiCard from '@/components/base/UiCard.vue';
import UiInput from '@/components/base/UiInput.vue';
import UiPagination from '@/components/base/UiPagination.vue';
import UiStatePanel from '@/components/base/UiStatePanel.vue';
import UiTag from '@/components/base/UiTag.vue';
import { platformMenuItems } from '@/app/menu';
import WorkspaceLayout from '@/components/layout/WorkspaceLayout.vue';
import { createCategoryTemplate, fetchCategoryTemplateList } from '@/services/product';
import type { ProductCategoryTemplateNodeResponseRaw, ProductCategoryTemplateResponseRaw } from '@/types/product';

type TemplateNodeForm = {
  clientId: string;
  templateCategoryCode: string;
  parentTemplateCategoryCode: string;
  categoryName: string;
  sortOrder: string;
  enabled: boolean;
  visible: boolean;
};

const loading = ref(false);
const submitting = ref(false);
const actionMessage = ref('');
const actionTone = ref<'info' | 'error'>('info');
const templates = ref<ProductCategoryTemplateResponseRaw[]>([]);
const selectedTemplateId = ref<number | null>(null);
const showCreateForm = ref(false);
const templatePagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0,
});

const form = reactive({
  templateName: '',
  industryCode: 'fresh_retail',
  templateVersion: 'v1.0.1',
  maxLevel: '3',
  templateDesc: '',
  nodes: [] as TemplateNodeForm[],
});

const pageState = computed(() => {
  if (loading.value) {
    return {
      tone: 'loading' as const,
      title: '正在加载分类模板',
      description: '正在读取平台分类模板列表和结构节点。',
    };
  }
  return null;
});

const selectedTemplate = computed(() =>
  templates.value.find((item) => item.id === selectedTemplateId.value) ?? templates.value[0] ?? null,
);

const selectedTemplateRows = computed(() => flattenTemplateNodes(selectedTemplate.value?.categories ?? []));
const sortedTemplates = computed(() =>
  [...templates.value].sort((left, right) => {
    const rank = (status: string) => {
      if (status === 'recommended') {
        return 0;
      }
      if (status === 'enabled') {
        return 1;
      }
      return 2;
    };
    const statusDiff = rank(left.status) - rank(right.status);
    if (statusDiff !== 0) {
      return statusDiff;
    }
    return right.id - left.id;
  }),
);

const pagedTemplates = computed(() => {
  return sortedTemplates.value;
});

const stats = computed(() => [
  { label: '模板总数', value: String(templatePagination.total), hint: '平台可选模板', emphasis: false },
  { label: '当前页推荐', value: String(templates.value.filter((item) => item.status === 'recommended').length), hint: '建议优先初始化', emphasis: false },
  { label: '当前页行业覆盖率', value: templates.value.length ? `${Math.round((new Set(templates.value.map((item) => item.industry_code)).size / templates.value.length) * 100)}%` : '0%', hint: '当前页行业编码占比', emphasis: false },
  { label: '当前页启用', value: String(templates.value.filter((item) => item.status !== 'disabled').length), hint: '当前页可初始化', emphasis: true },
]);

function setActionMessage(message: string, tone: 'info' | 'error' = 'info') {
  actionMessage.value = message;
  actionTone.value = tone;
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

function buildNode(seed?: Partial<TemplateNodeForm>, index = 0): TemplateNodeForm {
  return {
    clientId: `${Date.now()}-${index}-${Math.random().toString(16).slice(2, 8)}`,
    templateCategoryCode: seed?.templateCategoryCode ?? '',
    parentTemplateCategoryCode: seed?.parentTemplateCategoryCode ?? '',
    categoryName: seed?.categoryName ?? '',
    sortOrder: seed?.sortOrder ?? '10',
    enabled: seed?.enabled ?? true,
    visible: seed?.visible ?? true,
  };
}

function resetForm() {
  form.templateName = '';
  form.industryCode = 'fresh_retail';
  form.templateVersion = 'v1.0.1';
  form.maxLevel = '3';
  form.templateDesc = '';
  form.nodes = [
    buildNode({ templateCategoryCode: 'fresh_root', categoryName: '生鲜主类', sortOrder: '10' }, 0),
    buildNode({ templateCategoryCode: 'fresh_fruit', parentTemplateCategoryCode: 'fresh_root', categoryName: '水果优选', sortOrder: '20' }, 1),
    buildNode({ templateCategoryCode: 'fresh_vegetable', parentTemplateCategoryCode: 'fresh_root', categoryName: '新鲜蔬菜', sortOrder: '30' }, 2),
  ];
}

function toggleCreateForm() {
  showCreateForm.value = !showCreateForm.value;
  if (showCreateForm.value && !form.nodes.length) {
    resetForm();
  }
}

function handleTemplatePageChange(page: number) {
  templatePagination.page = page;
  void loadTemplates();
}

function handleTemplatePageSizeChange(pageSize: number) {
  templatePagination.page = 1;
  templatePagination.pageSize = pageSize;
  void loadTemplates();
}

function addNode() {
  form.nodes.push(buildNode(undefined, form.nodes.length));
}

function removeNode(clientId: string) {
  if (form.nodes.length <= 1) {
    setActionMessage('模板至少需要保留 1 个节点。', 'error');
    return;
  }
  form.nodes = form.nodes.filter((item) => item.clientId !== clientId);
}

async function loadTemplates() {
  loading.value = true;
  try {
    const templatePage = await fetchCategoryTemplateList({
      page: templatePagination.page,
      pageSize: templatePagination.pageSize,
    });
    templates.value = templatePage.list;
    templatePagination.page = templatePage.page;
    templatePagination.pageSize = templatePage.page_size;
    templatePagination.total = templatePage.total;
    if (!selectedTemplateId.value || !templatePage.list.some((item) => item.id === selectedTemplateId.value)) {
      selectedTemplateId.value = templatePage.list[0]?.id ?? null;
    }
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '加载分类模板失败。', 'error');
  } finally {
    loading.value = false;
  }
}

async function handleCreateTemplate() {
  try {
    const maxLevel = Number(form.maxLevel);
    if (!form.templateName.trim()) {
      throw new Error('模板名称不能为空。');
    }
    if (!form.industryCode.trim()) {
      throw new Error('行业编码不能为空。');
    }
    if (!form.templateVersion.trim()) {
      throw new Error('模板版本不能为空。');
    }
    if (!Number.isFinite(maxLevel) || maxLevel < 1) {
      throw new Error('最大层级必须是大于等于 1 的数字。');
    }
    if (!form.nodes.length) {
      throw new Error('模板节点不能为空。');
    }
    submitting.value = true;
    const created = await createCategoryTemplate({
      templateName: form.templateName.trim(),
      industryCode: form.industryCode.trim(),
      templateVersion: form.templateVersion.trim(),
      maxLevel,
      templateDesc: form.templateDesc.trim(),
      categories: form.nodes.map((node) => {
        const sortOrder = Number(node.sortOrder);
        if (!node.templateCategoryCode.trim()) {
          throw new Error('模板分类编码不能为空。');
        }
        if (!node.categoryName.trim()) {
          throw new Error('模板分类名称不能为空。');
        }
        if (!Number.isFinite(sortOrder)) {
          throw new Error('模板节点排序值必须是数字。');
        }
        return {
          templateCategoryCode: node.templateCategoryCode.trim(),
          parentTemplateCategoryCode: node.parentTemplateCategoryCode.trim(),
          categoryName: node.categoryName.trim(),
          sortOrder,
          enabled: node.enabled,
          visible: node.visible,
        };
      }),
    });
    templatePagination.page = 1;
    await loadTemplates();
    selectedTemplateId.value = templates.value.find((item) => item.id === created.id)?.id ?? templates.value[0]?.id ?? null;
    showCreateForm.value = false;
    resetForm();
    setActionMessage(`模板“${created.template_name}”已创建。`);
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '创建分类模板失败。', 'error');
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  resetForm();
  void loadTemplates();
});
</script>

<style module>
.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
}

.statCard {
  padding: 20px;
  border-radius: 18px;
  border: 1px solid rgba(9, 29, 46, 0.08);
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.statLabel {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.statValue {
  margin-top: 10px;
  font-size: 28px;
  font-weight: 800;
}

.statHint {
  margin-top: 6px;
  color: var(--cdd-text-soft);
  font-size: 11px;
  font-weight: 700;
}

.statCardEmphasis {
  background: linear-gradient(135deg, #ff6b00, #ff8c42);
  color: #fff;
  border-color: rgba(255, 107, 0, 0.4);
}

.statCardEmphasis .statLabel,
.statCardEmphasis .statHint {
  color: rgba(255, 255, 255, 0.8);
}

.grid {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(340px, 0.85fr);
  gap: 18px;
}

.panel,
.sidePanel,
.createPanel {
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
  margin-top: 18px;
}

.panelSubHead {
  margin-top: 18px;
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.subTitle {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
}

.sortText {
  color: var(--cdd-text-soft);
  font-size: 12px;
  font-weight: 700;
}

.templateList,
.previewList,
.nodeList {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.templateCard {
  border-radius: 20px;
  padding: 18px;
  background: #fff;
  text-align: left;
  border: 1px solid rgba(9, 29, 46, 0.08);
  box-shadow: 0 8px 22px rgba(9, 29, 46, 0.06);
  transition: all 0.18s ease;
}

.templateCard:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 30px rgba(9, 29, 46, 0.12);
  border-color: rgba(255, 107, 0, 0.28);
}

.templateCardActive {
  border-color: rgba(255, 107, 0, 0.42);
  background: linear-gradient(180deg, rgba(255, 248, 242, 0.96), rgba(255, 255, 255, 0.98));
  box-shadow: 0 14px 30px rgba(255, 107, 0, 0.14);
}

.templateCardHead {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.templateName {
  font-size: 18px;
  font-weight: 800;
}

.templateMeta,
.templateFoot,
.description,
.previewName,
.detailValue {
  color: var(--cdd-text-soft);
}

.templateMeta,
.templateFoot {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
}

.templateDesc {
  margin-top: 12px;
  line-height: 1.8;
  color: var(--cdd-text);
}

.detailGrid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.detailLabel,
.fieldLabel {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.detailValue {
  margin-top: 8px;
  font-weight: 700;
}

.previewHead {
  margin-top: 18px;
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.previewTitle {
  font-size: 14px;
  font-weight: 800;
  color: var(--cdd-text);
}

.previewRow {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(237, 244, 255, 0.72);
  border: 1px solid rgba(9, 29, 46, 0.06);
}

.previewTags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.formGrid,
.nodeGrid {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

.fieldWide {
  display: grid;
  gap: 8px;
}

.textarea {
  min-height: 112px;
  border: 1px solid rgba(9, 29, 46, 0.08);
  border-radius: 16px;
  padding: 14px;
  background: #fff;
  resize: vertical;
}

.nodeSectionHead {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-top: 24px;
}

.sectionTitle {
  font-size: 16px;
  font-weight: 800;
}

.nodeCard {
  padding: 16px;
  border-radius: 18px;
  background: rgba(237, 244, 255, 0.72);
  border: 1px solid rgba(9, 29, 46, 0.08);
}

.nodeActions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-top: 14px;
}

.switchItem {
  display: flex;
  gap: 8px;
  align-items: center;
  font-weight: 700;
}

@media (max-width: 1080px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .sidePanel {
    position: static;
  }

  .panelSubHead {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 720px) {
  .panel,
  .sidePanel,
  .createPanel {
    padding: 18px;
  }

  .panelHead,
  .templateCardHead,
  .previewHead,
  .previewRow,
  .nodeSectionHead,
  .nodeActions {
    flex-direction: column;
    align-items: flex-start;
  }

  .detailGrid {
    grid-template-columns: 1fr;
  }

  .panelActions :global(button),
  .nodeActions :global(button) {
    width: 100%;
  }
}
</style>
