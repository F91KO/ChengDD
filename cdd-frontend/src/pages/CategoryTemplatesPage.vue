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
            <UiButton variant="secondary" size="sm" @click="loadTemplates">刷新模板</UiButton>
            <UiButton variant="secondary" size="sm" @click="toggleCreateForm">
              {{ showCreateForm ? '收起创建表单' : '新建模板' }}
            </UiButton>
          </div>
        </div>

        <div :class="$style.listToolbar">
          <UiInput v-model="templateKeyword" placeholder="搜索模板名称、行业、版本..." prefix="搜" />
          <div :class="$style.statusBar">
            <button
              v-for="option in templateStatusOptions"
              :key="option.value"
              type="button"
              :class="[$style.statusChip, templateStatusFilter === option.value ? $style.statusChipActive : '']"
              @click="templateStatusFilter = option.value"
            >
              <span>{{ option.label }}</span>
              <strong>{{ option.count }}</strong>
            </button>
          </div>
        </div>

        <div :class="$style.panelSubHead">
          <h4 :class="$style.subTitle">核心模板库</h4>
          <div :class="$style.sortText">
            {{ templateKeyword.trim() ? `搜索结果 ${filteredTemplates.length} 项` : '排序方式：推荐优先' }}
          </div>
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

        <UiStatePanel
          v-if="!pagedTemplates.length"
          tone="empty"
          title="当前没有匹配模板"
          :description="templateKeyword.trim() || templateStatusFilter !== 'all' ? '请调整搜索词或状态筛选后重试。' : '平台后台还没有可用模板，请先创建模板。'"
        >
          <UiButton variant="secondary" size="sm" @click="resetTemplateFilters">重置筛选</UiButton>
          <UiButton variant="secondary" size="sm" @click="toggleCreateForm">
            {{ showCreateForm ? '收起创建表单' : '新建模板' }}
          </UiButton>
        </UiStatePanel>

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

          <CategoryTemplateTreePreview
            :rows="selectedTemplateRows"
            title="结构预览"
            :summary="templateStatusLabel(selectedTemplate.status)"
          />
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

      <UiStatePanel
        tone="info"
        title="模板会直接进入平台模板库"
        description="建议先录入根节点和一级节点，确认层级结构后再继续扩展详细节点。"
      />

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
        <UiButton variant="secondary" size="sm" @click="addNode">新增节点</UiButton>
      </div>

      <div :class="$style.nodeList">
        <article v-for="node in form.nodes" :key="node.clientId" :class="$style.nodeCard">
          <div :class="$style.nodeGrid">
            <UiInput v-model="node.templateCategoryCode" label="分类编码" placeholder="例如：fresh_fruit" />
            <UiHierarchySelect
              v-model="node.parentTemplateCategoryCode"
              label="父级节点"
              :options="parentNodeOptions(node.clientId)"
              placeholder="顶级节点"
              search-placeholder="搜索父级编码或名称"
              empty-text="顶级节点无需选择父级"
            />
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
            <UiButton variant="secondary" size="sm" :disabled="form.nodes.length <= 1" @click="removeNode(node.clientId)">
              删除节点
            </UiButton>
          </div>
        </article>
      </div>

      <div :class="$style.panelActions">
        <UiButton variant="secondary" size="sm" @click="resetForm">重置表单</UiButton>
        <UiButton :disabled="submitting || !canCreateTemplate" @click="handleCreateTemplate">
          {{ submitting ? '正在创建...' : '提交模板' }}
        </UiButton>
      </div>
    </UiCard>
  </WorkspaceLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watchEffect } from 'vue';
import CategoryTemplateTreePreview from '@/components/category/CategoryTemplateTreePreview.vue';
import UiButton from '@/components/base/UiButton.vue';
import UiCard from '@/components/base/UiCard.vue';
import UiHierarchySelect from '@/components/base/UiHierarchySelect.vue';
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
const templateKeyword = ref('');
const templateStatusFilter = ref<'all' | 'recommended' | 'enabled' | 'disabled'>('all');
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

const selectedTemplateRows = computed(() =>
  flattenTemplateNodes(selectedTemplate.value?.categories ?? []).map((node) => ({
    ...node,
    key: `${selectedTemplate.value?.id ?? 'template'}-${node.template_category_code}`,
  })),
);
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

const filteredTemplates = computed(() => {
  const keyword = templateKeyword.value.trim().toLowerCase();
  return sortedTemplates.value.filter((template) => {
    if (templateStatusFilter.value !== 'all' && template.status !== templateStatusFilter.value) {
      return false;
    }
    if (!keyword) {
      return true;
    }
    return [
      template.template_name,
      template.template_version,
      template.industry_code,
      industryLabel(template.industry_code),
      template.template_desc ?? '',
    ]
      .join(' ')
      .toLowerCase()
      .includes(keyword);
  });
});

const templateStatusOptions = computed(() => [
  { value: 'all' as const, label: '全部模板', count: String(templates.value.length) },
  { value: 'recommended' as const, label: '推荐模板', count: String(templates.value.filter((item) => item.status === 'recommended').length) },
  { value: 'enabled' as const, label: '已启用', count: String(templates.value.filter((item) => item.status === 'enabled').length) },
  { value: 'disabled' as const, label: '已停用', count: String(templates.value.filter((item) => item.status === 'disabled').length) },
]);
const templateNodeMap = computed(() => buildTemplateNodeMap(form.nodes));
const canCreateTemplate = computed(() => (
  form.templateName.trim().length > 0
  && form.industryCode.trim().length > 0
  && form.templateVersion.trim().length > 0
  && form.maxLevel.trim().length > 0
  && form.nodes.length > 0
  && form.nodes.every((node) => (
    node.templateCategoryCode.trim().length > 0
    && node.categoryName.trim().length > 0
    && node.sortOrder.trim().length > 0
  ))
  && !hasDuplicateTemplateNodeCodes(form.nodes)
  && !hasMissingTemplateParents(form.nodes)
  && !hasTemplateNodeCycle(form.nodes)
));

const pagedTemplates = computed(() => {
  const page = Math.max(1, templatePagination.page);
  const pageSize = Math.max(1, templatePagination.pageSize);
  const start = (page - 1) * pageSize;
  return filteredTemplates.value.slice(start, start + pageSize);
});

const stats = computed(() => [
  { label: '模板总数', value: String(templatePagination.total), hint: '平台可选模板', emphasis: false },
  { label: '当前页推荐', value: String(pagedTemplates.value.filter((item) => item.status === 'recommended').length), hint: '建议优先初始化', emphasis: false },
  { label: '当前页行业覆盖率', value: pagedTemplates.value.length ? `${Math.round((new Set(pagedTemplates.value.map((item) => item.industry_code)).size / pagedTemplates.value.length) * 100)}%` : '0%', hint: '当前页行业编码占比', emphasis: false },
  { label: '当前页启用', value: String(pagedTemplates.value.filter((item) => item.status !== 'disabled').length), hint: '当前页可初始化', emphasis: true },
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

function normalizeTemplateNodeCode(value: string | null | undefined): string {
  return value?.trim() ?? '';
}

function buildTemplateNodeMap(nodes: TemplateNodeForm[]) {
  const map = new Map<string, TemplateNodeForm>();
  nodes.forEach((node) => {
    const code = normalizeTemplateNodeCode(node.templateCategoryCode);
    if (!code || map.has(code)) {
      return;
    }
    map.set(code, node);
  });
  return map;
}

function hasDuplicateTemplateNodeCodes(nodes: TemplateNodeForm[]): boolean {
  const codes = new Set<string>();
  return nodes.some((node) => {
    const code = normalizeTemplateNodeCode(node.templateCategoryCode);
    if (!code) {
      return false;
    }
    if (codes.has(code)) {
      return true;
    }
    codes.add(code);
    return false;
  });
}

function hasMissingTemplateParents(nodes: TemplateNodeForm[]): boolean {
  const nodeMap = buildTemplateNodeMap(nodes);
  return nodes.some((node) => {
    const parentCode = normalizeTemplateNodeCode(node.parentTemplateCategoryCode);
    return parentCode.length > 0 && !nodeMap.has(parentCode);
  });
}

function hasTemplateNodeCycle(nodes: TemplateNodeForm[]): boolean {
  const nodeMap = buildTemplateNodeMap(nodes);
  return nodes.some((node) => {
    const code = normalizeTemplateNodeCode(node.templateCategoryCode);
    if (!code) {
      return false;
    }
    const visited = new Set<string>([code]);
    let parentCode = normalizeTemplateNodeCode(node.parentTemplateCategoryCode);
    while (parentCode) {
      if (visited.has(parentCode)) {
        return true;
      }
      visited.add(parentCode);
      const parentNode = nodeMap.get(parentCode);
      if (!parentNode) {
        return false;
      }
      parentCode = normalizeTemplateNodeCode(parentNode.parentTemplateCategoryCode);
    }
    return false;
  });
}

function isTemplateNodeDescendant(nodeCode: string, ancestorCode: string, nodeMap: Map<string, TemplateNodeForm>): boolean {
  const visited = new Set<string>();
  let currentCode = normalizeTemplateNodeCode(nodeCode);
  while (currentCode) {
    if (currentCode === ancestorCode) {
      return true;
    }
    if (visited.has(currentCode)) {
      return false;
    }
    visited.add(currentCode);
    const currentNode = nodeMap.get(currentCode);
    if (!currentNode) {
      return false;
    }
    currentCode = normalizeTemplateNodeCode(currentNode.parentTemplateCategoryCode);
  }
  return false;
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

function parentNodeOptions(currentClientId: string) {
  const currentNode = form.nodes.find((item) => item.clientId === currentClientId) ?? null;
  const currentCode = normalizeTemplateNodeCode(currentNode?.templateCategoryCode);
  return [
    ...form.nodes
      .filter((item) => {
        const code = normalizeTemplateNodeCode(item.templateCategoryCode);
        if (item.clientId === currentClientId || !code) {
          return false;
        }
        if (!currentCode) {
          return true;
        }
        return !isTemplateNodeDescendant(code, currentCode, templateNodeMap.value);
      })
      .map((item) => ({
        value: normalizeTemplateNodeCode(item.templateCategoryCode),
        label: `${normalizeTemplateNodeCode(item.templateCategoryCode)} · ${item.categoryName.trim() || '未命名节点'}`,
        pathLabel: `${normalizeTemplateNodeCode(item.templateCategoryCode)} / ${item.categoryName.trim() || '未命名节点'}`,
        searchText: `${item.templateCategoryCode} ${item.categoryName}`.toLowerCase(),
      })),
  ];
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

function resetTemplateFilters() {
  templateKeyword.value = '';
  templateStatusFilter.value = 'all';
  templatePagination.page = 1;
}

function handleTemplatePageChange(page: number) {
  templatePagination.page = page;
}

function handleTemplatePageSizeChange(pageSize: number) {
  templatePagination.page = 1;
  templatePagination.pageSize = pageSize;
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
      page: 1,
      pageSize: 200,
    });
    templates.value = templatePage.list;
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
    if (hasDuplicateTemplateNodeCodes(form.nodes)) {
      throw new Error('模板分类编码不能重复。');
    }
    if (hasMissingTemplateParents(form.nodes)) {
      throw new Error('存在未匹配的父级节点，请重新选择父级。');
    }
    if (hasTemplateNodeCycle(form.nodes)) {
      throw new Error('模板节点存在循环父子关系，请调整父级节点。');
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

watchEffect(() => {
  templatePagination.total = filteredTemplates.value.length;
  const maxPage = Math.max(1, Math.ceil(Math.max(1, filteredTemplates.value.length) / templatePagination.pageSize));
  if (templatePagination.page > maxPage) {
    templatePagination.page = maxPage;
  }
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
  padding: 18px;
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
  align-items: center;
}

.listToolbar {
  display: grid;
  gap: 10px;
  margin-top: 16px;
  align-items: start;
}

.statusBar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.statusChip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid rgba(9, 29, 46, 0.08);
  border-radius: 14px;
  background: rgba(248, 250, 253, 0.96);
  color: var(--cdd-text-soft);
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition:
    transform 0.16s ease,
    background 0.16s ease,
    color 0.16s ease,
    border-color 0.16s ease,
    box-shadow 0.16s ease;
}

.statusChip:hover {
  transform: translateY(-1px);
}

.statusChipActive {
  border-color: rgba(255, 107, 0, 0.22);
  background: linear-gradient(135deg, rgba(255, 107, 0, 0.1), rgba(255, 249, 244, 0.98));
  color: #9c4304;
  box-shadow: inset 0 0 0 1px rgba(255, 107, 0, 0.08);
}

.panelSubHead {
  margin-top: 16px;
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
  gap: 10px;
  margin-top: 14px;
}

.templateCard {
  border-radius: 18px;
  padding: 16px;
  background: #fff;
  text-align: left;
  border: 1px solid rgba(9, 29, 46, 0.08);
  box-shadow: 0 6px 16px rgba(9, 29, 46, 0.05);
  transition: all 0.18s ease;
}

.templateCard:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(9, 29, 46, 0.08);
  border-color: rgba(255, 107, 0, 0.18);
}

.templateCardActive {
  border-color: rgba(255, 107, 0, 0.28);
  background: linear-gradient(180deg, rgba(255, 248, 242, 0.96), rgba(255, 255, 255, 0.98));
  box-shadow: 0 10px 20px rgba(255, 107, 0, 0.1);
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
  gap: 12px;
  margin-top: 14px;
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
  margin-top: 16px;
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
  padding: 10px 12px;
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
  gap: 12px;
  margin-top: 14px;
}

.fieldWide {
  display: grid;
  gap: 8px;
}

.textarea {
  min-height: 96px;
  border: 1px solid rgba(9, 29, 46, 0.08);
  border-radius: 16px;
  padding: 12px 14px;
  background: #fff;
  resize: vertical;
}

.nodeSectionHead {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-top: 16px;
}

.sectionTitle {
  font-size: 16px;
  font-weight: 800;
}

.nodeCard {
  padding: 12px;
  border-radius: 16px;
  background: rgba(237, 244, 255, 0.72);
  border: 1px solid rgba(9, 29, 46, 0.08);
}

.nodeActions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-top: 12px;
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

  .statusBar {
    flex-direction: column;
    align-items: stretch;
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
