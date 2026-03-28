<template>
  <WorkspaceLayout
    eyebrow="Catalog"
    title="商品管理"
    description="统一查看商品列表，并在页内完成新增、编辑、上下架和库存调整。"
  >
    <section :class="$style.filters">
      <div :class="$style.filterBar">
        <UiInput v-model="keyword" placeholder="搜索商品名称、SPU、SKU..." prefix="搜" />
        <div :class="$style.filterActions">
          <UiButton variant="secondary" size="sm" @click="resetKeyword">重置关键词</UiButton>
          <UiButton @click="openCreatePanel">新增商品</UiButton>
        </div>
      </div>
      <div :class="$style.statusBar">
        <button
          v-for="option in statusOptions"
          :key="option.value"
          type="button"
          :class="[$style.statusChip, statusFilter === option.value ? $style.statusChipActive : '']"
          @click="applyStatusFilter(option.value)"
        >
          <span>{{ option.label }}</span>
          <strong>{{ option.count }}</strong>
        </button>
      </div>
      <div :class="$style.filterMeta">
        <span>当前筛选：{{ activeStatusLabel }}</span>
        <span>当前页 {{ products.length }} 项</span>
        <span>总计 {{ productPagination.total }} 项</span>
      </div>
    </section>

    <UiStatePanel
      v-if="productLoadState.loading"
      tone="loading"
      title="正在加载商品列表"
      description="正在读取商品摘要、SKU 价格和库存信息。"
    />
    <UiStatePanel
      v-else-if="productLoadState.errorMessage"
      tone="error"
      title="商品列表加载失败"
      :description="productLoadState.errorMessage"
    />

    <UiStatePanel
      v-if="actionMessage"
      :tone="actionTone"
      title="操作结果"
      :description="actionMessage"
    />

    <section :class="$style.workspace">
      <section :class="$style.stats">
        <UiCard v-for="stat in productStats" :key="stat.label" elevated :class="$style.statCard">
          <div :class="$style.statLabel">{{ stat.label }}</div>
          <div :class="$style.statValue">{{ stat.value }}</div>
          <div :class="[$style.statTone, $style[stat.tone]]"></div>
        </UiCard>
      </section>

      <div v-if="panelMode" ref="panelAnchor" :class="$style.editorAnchor">
        <UiCard elevated :class="$style.editorPanel">
          <div :class="$style.panelHead">
            <div>
              <div :class="$style.panelEyebrow">
                {{ panelMode === 'create' ? '创建商品' : '编辑商品' }}
              </div>
              <h3 :class="$style.panelTitle">
                {{ panelMode === 'create' ? '新增商品' : detail?.product_name || selectedProduct?.name || '编辑商品' }}
              </h3>
            </div>
            <UiButton variant="secondary" size="sm" @click="closePanel">收起面板</UiButton>
          </div>

          <UiStatePanel
            v-if="panelLoading"
            tone="loading"
            title="正在准备面板数据"
            description="正在加载分类、商品详情和 SKU 信息。"
          />

          <template v-else-if="panelMode === 'create'">
            <UiStatePanel
              v-if="!createCategoryOptions.length"
              tone="empty"
              title="暂无可用商品分类"
              description="请先在商品分类中初始化或创建分类，再回来新增商品。"
            />

            <div :class="$style.sectionTabs">
              <button
                v-for="tab in createSectionTabs"
                :key="tab.value"
                type="button"
                :class="[$style.sectionTab, editorSection === tab.value ? $style.sectionTabActive : '']"
                @click="editorSection = tab.value"
              >
                {{ tab.label }}
              </button>
            </div>

            <div v-if="editorSection === 'basic'" :class="$style.formSection">
              <div :class="$style.sectionHeader">
                <div :class="$style.sectionTitle">基础信息</div>
              </div>
              <div :class="$style.formGrid">
                <UiHierarchySelect
                  v-model="createForm.categoryId"
                  label="商品分类"
                  :options="createCategoryOptions"
                  placeholder="请选择分类"
                  search-placeholder="搜索分类名称或路径"
                  empty-text="请先选择商品分类"
                />
                <UiInput v-model="createForm.productName" label="商品名称" placeholder="请输入商品名称" />
                <UiInput v-model="createForm.productSubTitle" label="商品副标题" placeholder="请输入商品副标题" />
              </div>
            </div>

            <div v-else-if="editorSection === 'sku'" :class="$style.formSection">
              <div :class="$style.sectionHeader">
                <div :class="$style.sectionTitle">初始 SKU</div>
              </div>
              <div :class="$style.formGrid">
                <UiInput v-model="createForm.skuName" label="SKU 名称" placeholder="例如 标准装" />
                <UiInput v-model="createForm.salePrice" label="销售价" placeholder="例如 99.00" />
                <UiInput v-model="createForm.availableStock" label="初始库存" placeholder="例如 100" />
              </div>
            </div>

            <div :class="$style.actionFooter">
              <UiButton variant="secondary" size="sm" @click="resetCreateForm">清空表单</UiButton>
              <UiButton :disabled="submitting || !canCreateProduct" @click="handleCreateProduct">
                {{ submitting ? '正在创建...' : '提交创建' }}
              </UiButton>
            </div>
          </template>

          <template v-else-if="detail">
            <section :class="$style.detailSummary">
              <div>
                <div :class="$style.summaryLabel">商品 ID</div>
                <div :class="$style.summaryValue">{{ detail.id }}</div>
              </div>
              <div>
                <div :class="$style.summaryLabel">SPU 编码</div>
                <div :class="$style.summaryValue">{{ detail.product_code || `SPU-${detail.id}` }}</div>
              </div>
              <div>
                <div :class="$style.summaryLabel">商品分类</div>
                <div :class="$style.summaryValue">{{ resolveCategoryPath(detail.category_id) }}</div>
              </div>
              <div>
                <div :class="$style.summaryLabel">当前状态</div>
                <div :class="$style.summaryValue">{{ selectedProduct?.status || detail.status }}</div>
              </div>
            </section>

            <div v-if="detail.product_sub_title" :class="$style.summaryNote">
              {{ detail.product_sub_title }}
            </div>

            <div :class="$style.sectionTabs">
              <button
                v-for="tab in editSectionTabs"
                :key="tab.value"
                type="button"
                :class="[$style.sectionTab, editorSection === tab.value ? $style.sectionTabActive : '']"
                @click="editorSection = tab.value"
              >
                {{ tab.label }}
              </button>
            </div>

            <div v-if="editorSection === 'basic'" :class="$style.formSection">
              <div :class="$style.sectionHeader">
                <div :class="$style.sectionTitle">基础信息</div>
              </div>
              <div :class="$style.formGrid">
                <UiHierarchySelect
                  v-model="editForm.categoryId"
                  label="商品分类"
                  :options="editCategoryOptions"
                  placeholder="请选择分类"
                  search-placeholder="搜索分类名称或路径"
                  empty-text="请先选择商品分类"
                />
                <UiInput v-model="editForm.productName" label="商品名称" placeholder="请输入商品名称" />
                <UiInput v-model="editForm.productSubTitle" label="商品副标题" placeholder="请输入商品副标题" />
              </div>
            </div>

            <div v-else-if="editorSection === 'snapshot'" :class="$style.formSection">
              <div :class="$style.sectionHeader">
                <div :class="$style.sectionTitle">规格属性</div>
              </div>
              <div v-if="detail.skus.length" :class="$style.skuSnapshotGrid">
                <article v-for="sku in detail.skus" :key="sku.id" :class="$style.skuSnapshotCard">
                  <div :class="$style.skuSnapshotHead">
                    <div>
                      <div :class="$style.skuSnapshotName">{{ sku.sku_name }}</div>
                      <div :class="$style.skuSnapshotCode">
                        编码 {{ sku.sku_code || '系统自动生成' }}
                      </div>
                    </div>
                    <UiTag :tone="stockStatusTone(sku.stock_status)">
                      {{ stockStatusLabel(sku.stock_status) }}
                    </UiTag>
                  </div>
                  <div :class="$style.skuSnapshotMeta">
                    <span>售价 {{ formatCurrency(sku.sale_price) }}</span>
                    <span>可售 {{ sku.available_stock }}</span>
                    <span>锁定 {{ sku.locked_stock }}</span>
                  </div>
                </article>
              </div>
              <UiStatePanel
                v-else
                tone="empty"
                title="当前商品还没有 SKU"
                description="可以切换到 SKU 编辑标签，先补充商品规格后再维护库存。"
              />
            </div>

            <div v-else-if="editorSection === 'sku'" :class="$style.formSection">
              <div :class="$style.skuSectionHead">
                <div :class="$style.sectionTitle">SKU 编辑</div>
                <UiButton variant="secondary" size="sm" @click="addEditSku">新增 SKU</UiButton>
              </div>
              <article v-for="sku in editForm.skus" :key="sku.clientId" :class="$style.skuEditorCard">
                <div :class="$style.skuEditorGrid">
                  <UiInput v-model="sku.skuName" label="SKU 名称" placeholder="例如 标准装" />
                  <UiInput v-model="sku.salePrice" label="销售价" placeholder="例如 99.00" />
                  <UiInput v-model="sku.availableStock" label="可售库存" placeholder="例如 100" />
                </div>
                <div :class="$style.panelActions">
                  <UiButton
                    variant="secondary"
                    size="sm"
                    :disabled="editForm.skus.length <= 1"
                    @click="removeEditSku(sku.clientId)"
                  >
                    删除 SKU
                  </UiButton>
                </div>
              </article>
            </div>

            <div v-else-if="editorSection === 'stock'" :class="$style.formSection">
              <div :class="$style.sectionTitle">库存调整</div>
              <UiStatePanel
                v-if="!detail.skus.length"
                tone="empty"
                title="当前商品没有可调整的 SKU"
                description="请先补充至少一个 SKU，再进行库存补货、扣减或盘点修正。"
              />
              <div v-else :class="$style.formGrid">
                <label :class="$style.field">
                  <span :class="$style.fieldLabel">目标 SKU</span>
                  <select v-model="stockForm.skuId" :class="$style.select">
                    <option value="">请选择 SKU</option>
                    <option v-for="sku in detail.skus" :key="sku.id" :value="String(sku.id)">
                      {{ sku.sku_name }}（可售 {{ sku.available_stock }} / 锁定 {{ sku.locked_stock }}）
                    </option>
                  </select>
                </label>
                <UiInput v-model="stockForm.deltaStock" label="调整数量" placeholder="正数补货，负数扣减" />
                <label :class="$style.fieldWide">
                  <span :class="$style.fieldLabel">调整原因</span>
                  <textarea
                    v-model="stockForm.reason"
                    :class="$style.textarea"
                    placeholder="请输入库存调整原因"
                  />
                </label>
              </div>
              <div v-if="detail.skus.length" :class="$style.sectionActions">
                <UiButton variant="secondary" size="sm" :disabled="submitting || !canAdjustStock" @click="handleAdjustStock">
                  {{ submitting ? '正在提交...' : '提交库存调整' }}
                </UiButton>
              </div>
            </div>

            <div v-if="editorSection !== 'stock'" :class="$style.actionFooter">
              <UiButton variant="secondary" size="sm" :disabled="submitting || !canUpdateProduct" @click="handleUpdateProduct">
                {{ submitting ? '正在保存...' : '保存商品信息' }}
              </UiButton>
              <UiButton
                size="sm"
                :variant="publishActionVariant(selectedProduct?.statusRaw)"
                :disabled="submitting || !selectedProduct"
                @click="selectedProduct && handleTogglePublish(selectedProduct)"
              >
                {{ submitting ? '正在处理...' : publishActionLabel(selectedProduct?.statusRaw, true) }}
              </UiButton>
            </div>
          </template>
        </UiCard>
      </div>

      <UiCard elevated :class="$style.listSummaryCard">
        <div>
          <div :class="$style.panelEyebrow">商品列表</div>
          <h3 :class="$style.listSummaryTitle">当前筛选下的商品</h3>
          <div :class="$style.listSummaryText">
            {{ activeStatusLabel }} · 关键词“{{ keyword.trim() || '全部' }}” · 共 {{ productPagination.total }} 项
          </div>
        </div>
        <div :class="$style.listSummaryMeta">
          <span>当前页 {{ products.length }} 项</span>
          <span>每页 {{ productPagination.pageSize }} 项</span>
          <span>第 {{ productPagination.page }} 页</span>
        </div>
      </UiCard>

      <section :class="$style.list">
        <UiCard v-for="product in products" :key="product.id" elevated :class="$style.productCard">
          <div :class="$style.productMain">
            <div :class="$style.productHead">
              <div :class="$style.productIdentity">
                <div :class="$style.cover">
                  <div :class="$style.coverCode">{{ product.productCode }}</div>
                  <div :class="$style.coverMeta">{{ product.status }}</div>
                </div>
                <div :class="$style.productCopy">
                  <h3 :class="$style.productName">{{ product.name }}</h3>
                  <div :class="$style.productPath">{{ product.categoryPath }}</div>
                  <div v-if="product.subtitle" :class="$style.productSubtitle">{{ product.subtitle }}</div>
                </div>
              </div>
              <div :class="$style.productBadgeGroup">
                <UiTag :tone="product.statusTone as 'default' | 'primary' | 'info'">
                  {{ product.status }}
                </UiTag>
                <UiTag tone="default">SKU {{ product.skuCount }} 个</UiTag>
              </div>
            </div>
            <div :class="$style.skuPreviewPanel">
              <div :class="$style.metaText">规格摘要</div>
              <div :class="$style.skuPreview">{{ product.skuPreview }}</div>
            </div>
            <div :class="$style.metricGrid">
              <div :class="$style.metricCard">
                <div :class="$style.metaText">售价区间</div>
                <div :class="$style.price">{{ product.price }}</div>
              </div>
              <div :class="$style.metricCard">
                <div :class="$style.metaText">库存摘要</div>
                <div :class="$style.inventory">{{ product.inventory }}</div>
              </div>
              <div :class="$style.metricCard">
                <div :class="$style.metaText">近期开单</div>
                <div :class="$style.metricValue">{{ product.sales }}</div>
              </div>
              <div :class="$style.metricCard">
                <div :class="$style.metaText">分类</div>
                <div :class="$style.metricValue">{{ product.categoryName }}</div>
              </div>
            </div>
            <div :class="$style.productFooter">
              <div :class="$style.actionHint">编辑商品维护基础信息和 SKU；库存调整用于补货、扣减和盘点修正。</div>
              <div :class="$style.actions">
                <UiButton variant="secondary" size="sm" @click="openDetailPanel(product)">编辑商品</UiButton>
                <UiButton variant="secondary" size="sm" @click="openStockPanel(product)">库存调整</UiButton>
                <UiButton
                  size="sm"
                  :variant="publishActionVariant(product.statusRaw)"
                  @click="handleTogglePublish(product)"
                >
                  {{ publishActionLabel(product.statusRaw) }}
                </UiButton>
              </div>
            </div>
          </div>
        </UiCard>

        <UiStatePanel
          v-if="!productLoadState.loading && products.length === 0"
          tone="empty"
          title="没有找到匹配商品"
          description="可以重置关键词后重试，或直接创建新商品。"
        >
          <UiButton variant="secondary" size="sm" @click="resetKeyword">清空筛选</UiButton>
          <UiButton @click="openCreatePanel">新增商品</UiButton>
        </UiStatePanel>
      </section>

      <UiPagination
        :page="productPagination.page"
        :page-size="productPagination.pageSize"
        :total="productPagination.total"
        :disabled="productLoadState.loading"
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </section>
  </WorkspaceLayout>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, nextTick, onMounted, reactive, ref, watch } from 'vue';
import UiButton from '@/components/base/UiButton.vue';
import UiCard from '@/components/base/UiCard.vue';
import UiHierarchySelect from '@/components/base/UiHierarchySelect.vue';
import UiInput from '@/components/base/UiInput.vue';
import UiPagination from '@/components/base/UiPagination.vue';
import UiStatePanel from '@/components/base/UiStatePanel.vue';
import UiTag from '@/components/base/UiTag.vue';
import WorkspaceLayout from '@/components/layout/WorkspaceLayout.vue';
import {
  buildCategoryOptions,
  findHierarchyOption,
  type HierarchyOption,
} from '@/modules/categories/tree';
import {
  adjustProductStock,
  createProduct,
  fetchAllCategoryList,
  fetchProductDetail,
  publishProduct,
  unpublishProduct,
  updateProduct,
} from '@/services/product';
import {
  type ProductCard,
  loadProducts,
  productLoadState,
  productPagination,
  productStats,
  products,
} from '@/modules/products/state';
import { useAuthStore } from '@/stores/auth';
import type { ProductCategoryResponseRaw, ProductDetailResponseRaw } from '@/types/product';

type PanelMode = 'create' | 'edit' | null;
type ProductStatusFilter = 'all' | 'on_shelf' | 'draft' | 'off_shelf';
type EditorSection = 'basic' | 'sku' | 'snapshot' | 'stock';
type EditableSkuForm = {
  clientId: string;
  skuCode: string;
  skuName: string;
  salePrice: string;
  availableStock: string;
};

const authStore = useAuthStore();
const keyword = ref('');
const statusFilter = ref<ProductStatusFilter>('all');
const actionMessage = ref('');
const actionTone = ref<'info' | 'error'>('info');
const panelMode = ref<PanelMode>(null);
const editorSection = ref<EditorSection>('basic');
const panelLoading = ref(false);
const submitting = ref(false);
const categories = ref<ProductCategoryResponseRaw[]>([]);
const selectedProduct = ref<ProductCard | null>(null);
const detail = ref<ProductDetailResponseRaw | null>(null);
const panelAnchor = ref<HTMLElement | null>(null);
let searchTimer: ReturnType<typeof setTimeout> | null = null;

const categoryOptions = computed(() => buildCategoryOptions(categories.value));
const enabledCategoryOptions = computed(() => buildCategoryOptions(categories.value, (item) => item.is_enabled));
const enabledCategoryIdSet = computed(() => new Set(categories.value.filter((item) => item.is_enabled).map((item) => item.id)));
const createCategoryOptions = computed(() => enabledCategoryOptions.value);
const editCategoryOptions = computed(() => withSelectedCategoryOption(enabledCategoryOptions.value, editForm.categoryId));
const canCreateProduct = computed(() => (
  createCategoryOptions.value.length > 0
  && createForm.categoryId.trim().length > 0
  && createForm.productName.trim().length > 0
  && createForm.skuName.trim().length > 0
  && createForm.salePrice.trim().length > 0
  && createForm.availableStock.trim().length > 0
));
const canUpdateProduct = computed(() => (
  Boolean(selectedProduct.value && detail.value)
  && editForm.categoryId.trim().length > 0
  && editForm.productName.trim().length > 0
  && editForm.skus.length > 0
  && editForm.skus.every((sku) => (
    sku.skuName.trim().length > 0
    && sku.salePrice.trim().length > 0
    && sku.availableStock.trim().length > 0
  ))
));
const canAdjustStock = computed(() => (
  Boolean(selectedProduct.value && detail.value)
  && detail.value!.skus.length > 0
  && stockForm.skuId.trim().length > 0
  && stockForm.deltaStock.trim().length > 0
  && stockForm.reason.trim().length > 0
));
const statusOptions = computed(() => [
  { value: 'all' as const, label: '全部商品', count: String(productPagination.total) },
  { value: 'on_shelf' as const, label: '在售中', count: productStats[0]?.value ?? '0' },
  { value: 'draft' as const, label: '待发布', count: productStats[1]?.value ?? '0' },
  { value: 'off_shelf' as const, label: '已下架', count: productStats[2]?.value ?? '0' },
]);
const activeStatusLabel = computed(() =>
  statusOptions.value.find((item) => item.value === statusFilter.value)?.label ?? '全部商品',
);
const createSectionTabs = [
  { value: 'basic' as const, label: '基础信息' },
  { value: 'sku' as const, label: '初始 SKU' },
];
const editSectionTabs = [
  { value: 'basic' as const, label: '基础信息' },
  { value: 'snapshot' as const, label: '规格属性' },
  { value: 'sku' as const, label: 'SKU 编辑' },
  { value: 'stock' as const, label: '库存调整' },
];

const createForm = reactive({
  categoryId: '',
  productName: '',
  productSubTitle: '',
  skuName: '',
  skuCode: '',
  salePrice: '',
  availableStock: '',
});

const editForm = reactive({
  categoryId: '',
  productName: '',
  productSubTitle: '',
  skus: [] as EditableSkuForm[],
});

const stockForm = reactive({
  skuId: '',
  deltaStock: '',
  reason: '',
});

function setActionMessage(message: string, tone: 'info' | 'error' = 'info') {
  actionMessage.value = message;
  actionTone.value = tone;
}

function resolveCategoryName(categoryId: number): string {
  return categories.value.find((item) => item.id === categoryId)?.category_name ?? '分类缺失';
}

function resolveCategoryPath(categoryId: number): string {
  return findHierarchyOption(categoryOptions.value, String(categoryId))?.pathLabel ?? resolveCategoryName(categoryId);
}

function withSelectedCategoryOption(options: HierarchyOption[], categoryId: string): HierarchyOption[] {
  if (!categoryId.trim()) {
    return options;
  }
  if (options.some((item) => item.value === categoryId)) {
    return options;
  }
  const selectedOption = findHierarchyOption(categoryOptions.value, categoryId);
  if (!selectedOption) {
    return options;
  }
  return [selectedOption, ...options];
}

function formatCurrency(value: number | string): string {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) {
    return '¥0.00';
  }
  return `¥${parsed.toFixed(2)}`;
}

function formatProductStatus(status: string): ProductCard['status'] {
  if (status === 'on_shelf') {
    return '在售中';
  }
  if (status === 'off_shelf') {
    return '已下架';
  }
  return '待发布';
}

function productStatusTone(status: string): ProductCard['statusTone'] {
  if (status === 'on_shelf') {
    return 'primary';
  }
  if (status === 'off_shelf') {
    return 'info';
  }
  return 'default';
}

function stockStatusLabel(status: string): string {
  if (status === 'normal') {
    return '库存正常';
  }
  if (status === 'low') {
    return '库存偏低';
  }
  if (status === 'empty') {
    return '库存售罄';
  }
  return status || '待确认';
}

function stockStatusTone(status: string): 'default' | 'info' | 'danger' | 'success' | 'primary' {
  if (status === 'normal') {
    return 'success';
  }
  if (status === 'low') {
    return 'primary';
  }
  if (status === 'empty') {
    return 'danger';
  }
  return 'default';
}

function canPublishStatus(status: string | undefined): boolean {
  return status === 'draft' || status === 'off_shelf' || status === '已下架' || status === '待发布';
}

function publishActionLabel(status: string | undefined, inPanel = false): string {
  if (canPublishStatus(status)) {
    return inPanel ? '上架当前商品' : '上架销售';
  }
  return inPanel ? '下架当前商品' : '下架商品';
}

function publishActionVariant(status: string | undefined): 'primary' | 'ghost' {
  return canPublishStatus(status) ? 'primary' : 'ghost';
}

function getRequiredScope() {
  const merchantId = authStore.merchantIdForQuery;
  const storeId = authStore.storeIdForQuery;
  if (!merchantId || !storeId) {
    throw new Error('当前登录上下文缺少商家或门店 ID。');
  }
  return { merchantId, storeId };
}

function currentStatusFilter(): string | undefined {
  return statusFilter.value === 'all' ? undefined : statusFilter.value;
}

async function loadCurrentProducts(force = false, page = productPagination.page, pageSize = productPagination.pageSize) {
  await loadProducts(force, currentStatusFilter(), keyword.value, page, pageSize);
}

function scrollPanelIntoView() {
  void nextTick(() => {
    panelAnchor.value?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  });
}

function resetKeyword() {
  keyword.value = '';
  void loadCurrentProducts(true, 1, productPagination.pageSize);
}

function resetCreateForm() {
  createForm.categoryId = createCategoryOptions.value[0]?.value ?? '';
  createForm.productName = '';
  createForm.productSubTitle = '';
  createForm.skuName = '';
  createForm.skuCode = '';
  createForm.salePrice = '';
  createForm.availableStock = '';
}

function buildEditableSku(detailSku?: ProductDetailResponseRaw['skus'][number], index = 0): EditableSkuForm {
  return {
    clientId: detailSku ? `sku-${detailSku.id}` : `new-${Date.now()}-${index}`,
    skuCode: detailSku?.sku_code ?? '',
    skuName: detailSku?.sku_name ?? '',
    salePrice: detailSku ? String(detailSku.sale_price) : '',
    availableStock: detailSku ? String(detailSku.available_stock) : '',
  };
}

function resetEditForm(detailValue: ProductDetailResponseRaw) {
  editForm.categoryId = String(detailValue.category_id);
  editForm.productName = detailValue.product_name;
  editForm.productSubTitle = detailValue.product_sub_title ?? '';
  editForm.skus = detailValue.skus.length
    ? detailValue.skus.map((sku, index) => buildEditableSku(sku, index))
    : [buildEditableSku(undefined, 0)];
}

function resetStockForm(detailValue: ProductDetailResponseRaw | null) {
  stockForm.skuId = detailValue?.skus[0] ? String(detailValue.skus[0].id) : '';
  stockForm.deltaStock = '';
  stockForm.reason = '';
}

function buildSelectedProductSnapshot(detailValue: ProductDetailResponseRaw, currentProduct: ProductCard | null): ProductCard {
  const salePrices = detailValue.skus
    .map((sku) => Number(sku.sale_price))
    .filter((price) => Number.isFinite(price));
  const minSalePrice = salePrices.length ? Math.min(...salePrices) : null;
  const maxSalePrice = salePrices.length ? Math.max(...salePrices) : null;
  const availableStock = detailValue.skus.reduce((sum, sku) => sum + (Number.isFinite(sku.available_stock) ? sku.available_stock : 0), 0);
  const lockedStock = detailValue.skus.reduce((sum, sku) => sum + (Number.isFinite(sku.locked_stock) ? sku.locked_stock : 0), 0);
  const skuPreview = detailValue.skus.length
    ? detailValue.skus
      .map((sku) => sku.sku_name?.trim())
      .filter((name): name is string => Boolean(name))
      .slice(0, 3)
      .join(' / ')
    : '暂无 SKU 规格';

  return {
    id: detailValue.id,
    merchantId: detailValue.merchant_id,
    storeId: detailValue.store_id,
    categoryId: detailValue.category_id,
    categoryName: resolveCategoryName(detailValue.category_id),
    categoryPath: resolveCategoryPath(detailValue.category_id),
    name: detailValue.product_name,
    subtitle: detailValue.product_sub_title?.trim() ?? '',
    productCode: detailValue.product_code || currentProduct?.productCode || `SPU-${detailValue.id}`,
    sku: detailValue.product_code || currentProduct?.sku || `SPU-${detailValue.id}`,
    skuPreview: skuPreview || `共 ${detailValue.skus.length} 个 SKU`,
    price: minSalePrice === null || maxSalePrice === null
      ? currentProduct?.price ?? '未配置售价'
      : minSalePrice === maxSalePrice
        ? formatCurrency(minSalePrice)
        : `${formatCurrency(minSalePrice)} - ${formatCurrency(maxSalePrice)}`,
    sales: currentProduct?.sales ?? '0 件',
    inventory: lockedStock > 0 ? `${availableStock} 可售 / ${lockedStock} 锁定` : `${availableStock} 可售`,
    status: formatProductStatus(detailValue.status),
    statusRaw: detailValue.status,
    skuCount: detailValue.skus.length,
    statusTone: productStatusTone(detailValue.status),
  };
}

function addEditSku() {
  editForm.skus.push(buildEditableSku(undefined, editForm.skus.length));
}

function removeEditSku(clientId: string) {
  if (editForm.skus.length <= 1) {
    setActionMessage('商品至少需要保留 1 个 SKU。', 'error');
    return;
  }

  editForm.skus = editForm.skus.filter((item) => item.clientId !== clientId);
}

async function ensureCategories() {
  if (categories.value.length > 0) {
    return;
  }
  const { merchantId, storeId } = getRequiredScope();
  categories.value = await fetchAllCategoryList({ merchantId, storeId, pageSize: 200 });
}

async function refreshProducts() {
  await loadCurrentProducts(true, productPagination.page, productPagination.pageSize);
}

function handlePageChange(page: number) {
  void loadCurrentProducts(true, page, productPagination.pageSize);
}

function handlePageSizeChange(pageSize: number) {
  void loadCurrentProducts(true, 1, pageSize);
}

function applyStatusFilter(nextStatus: ProductStatusFilter) {
  if (statusFilter.value === nextStatus) {
    return;
  }
  statusFilter.value = nextStatus;
  void loadCurrentProducts(true, 1, productPagination.pageSize);
}

function closePanel() {
  panelMode.value = null;
  editorSection.value = 'basic';
  selectedProduct.value = null;
  detail.value = null;
  panelLoading.value = false;
  submitting.value = false;
}

async function openCreatePanel() {
  panelMode.value = 'create';
  editorSection.value = 'basic';
  selectedProduct.value = null;
  detail.value = null;
  panelLoading.value = true;
  scrollPanelIntoView();
  try {
    await ensureCategories();
    resetCreateForm();
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '加载商品分类失败。', 'error');
    closePanel();
  } finally {
    panelLoading.value = false;
  }
}

async function openDetailPanel(product: ProductCard) {
  panelMode.value = 'edit';
  editorSection.value = 'basic';
  panelLoading.value = true;
  selectedProduct.value = product;
  scrollPanelIntoView();
  try {
    await ensureCategories();
    detail.value = await fetchProductDetail(product.id);
    resetEditForm(detail.value);
    resetStockForm(detail.value);
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '加载商品详情失败。', 'error');
    closePanel();
  } finally {
    panelLoading.value = false;
  }
}

async function openStockPanel(product: ProductCard) {
  await openDetailPanel(product);
  editorSection.value = 'stock';
}

async function refreshSelectedProduct(productId: number) {
  detail.value = await fetchProductDetail(productId);
  selectedProduct.value = products.find((item) => item.id === productId)
    ?? buildSelectedProductSnapshot(detail.value, selectedProduct.value);
  resetEditForm(detail.value);
  resetStockForm(detail.value);
}

async function handleCreateProduct() {
  try {
    submitting.value = true;
    const { merchantId, storeId } = getRequiredScope();
    const categoryId = Number(createForm.categoryId);
    const salePrice = Number(createForm.salePrice);
    const availableStock = Number(createForm.availableStock);

    if (!Number.isFinite(categoryId)) {
      throw new Error('请选择有效的商品分类。');
    }
    if (!enabledCategoryIdSet.value.has(categoryId)) {
      throw new Error('当前分类已禁用，请选择可用分类后再创建商品。');
    }
    if (!createForm.productName.trim()) {
      throw new Error('商品名称不能为空。');
    }
    if (!createForm.skuName.trim()) {
      throw new Error('请填写 SKU 名称。');
    }
    if (!Number.isFinite(salePrice) || salePrice < 0) {
      throw new Error('销售价必须是大于等于 0 的数字。');
    }
    if (!Number.isFinite(availableStock) || availableStock < 0) {
      throw new Error('初始库存必须是大于等于 0 的数字。');
    }

    await createProduct({
      merchantId,
      storeId,
      categoryId,
      productName: createForm.productName.trim(),
      productSubTitle: createForm.productSubTitle.trim(),
      skuCode: createForm.skuCode.trim() || undefined,
      skuName: createForm.skuName.trim(),
      salePrice,
      availableStock,
    });
    await refreshProducts();
    setActionMessage(`商品“${createForm.productName.trim()}”已创建并同步到列表。`);
    closePanel();
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '创建商品失败。', 'error');
  } finally {
    submitting.value = false;
  }
}

async function handleUpdateProduct() {
  try {
    if (!selectedProduct.value || !detail.value) {
      throw new Error('请先选择要编辑的商品。');
    }

    const { merchantId, storeId } = getRequiredScope();
    const categoryId = Number(editForm.categoryId);
    if (!Number.isFinite(categoryId)) {
      throw new Error('请选择有效的商品分类。');
    }
    if (!enabledCategoryIdSet.value.has(categoryId)) {
      throw new Error('当前分类已禁用，请先切换到可用分类后再保存商品。');
    }
    if (!editForm.productName.trim()) {
      throw new Error('商品名称不能为空。');
    }
    if (!editForm.skus.length) {
      throw new Error('商品至少需要 1 个 SKU。');
    }

    const normalizedSkus = editForm.skus.map((sku) => {
      const salePrice = Number(sku.salePrice);
      const availableStock = Number(sku.availableStock);
      if (!sku.skuName.trim()) {
        throw new Error('请填写 SKU 名称。');
      }
      if (!Number.isFinite(salePrice) || salePrice < 0) {
        throw new Error('SKU 销售价必须是大于等于 0 的数字。');
      }
      if (!Number.isFinite(availableStock) || availableStock < 0) {
        throw new Error('SKU 库存必须是大于等于 0 的数字。');
      }

      return {
        skuCode: sku.skuCode.trim() || undefined,
        skuName: sku.skuName.trim(),
        salePrice,
        availableStock,
      };
    });

    submitting.value = true;
    await updateProduct({
      productId: selectedProduct.value.id,
      merchantId,
      storeId,
      categoryId,
      productName: editForm.productName.trim(),
      productSubTitle: editForm.productSubTitle.trim(),
      skus: normalizedSkus,
    });
    await refreshProducts();
    await refreshSelectedProduct(selectedProduct.value.id);
    setActionMessage(`商品“${editForm.productName.trim()}”已保存。`);
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '更新商品失败。', 'error');
  } finally {
    submitting.value = false;
  }
}

async function handleAdjustStock() {
  try {
    if (!selectedProduct.value || !detail.value) {
      throw new Error('请先选择要调整库存的商品。');
    }
    const { merchantId, storeId } = getRequiredScope();
    const skuId = Number(stockForm.skuId);
    const deltaStock = Number(stockForm.deltaStock);
    if (!Number.isFinite(skuId)) {
      throw new Error('请选择需要调整的 SKU。');
    }
    if (!Number.isFinite(deltaStock) || deltaStock === 0) {
      throw new Error('调整数量必须是非 0 数字。');
    }
    if (!stockForm.reason.trim()) {
      throw new Error('请填写库存调整原因。');
    }

    submitting.value = true;
    await adjustProductStock({
      merchantId,
      storeId,
      productId: selectedProduct.value.id,
      skuId,
      deltaStock,
      reason: stockForm.reason.trim(),
    });
    await refreshProducts();
    await refreshSelectedProduct(selectedProduct.value.id);
    setActionMessage(`商品“${selectedProduct.value.name}”的库存已更新。`);
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '库存调整失败。', 'error');
  } finally {
    submitting.value = false;
  }
}

async function handleTogglePublish(product: ProductCard) {
  try {
    submitting.value = true;
    if (canPublishStatus(product.statusRaw)) {
      await publishProduct(product.id);
      setActionMessage(`商品“${product.name}”已上架。`);
    } else {
      await unpublishProduct(product.id);
      setActionMessage(`商品“${product.name}”已下架。`);
    }
    await refreshProducts();
    if (selectedProduct.value?.id === product.id) {
      await refreshSelectedProduct(product.id);
    }
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '商品状态更新失败。', 'error');
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  void loadCurrentProducts(false, productPagination.page, productPagination.pageSize);
});

watch(keyword, () => {
  if (searchTimer) {
    clearTimeout(searchTimer);
  }
  searchTimer = setTimeout(() => {
    void loadCurrentProducts(true, 1, productPagination.pageSize);
  }, 350);
});

onBeforeUnmount(() => {
  if (searchTimer) {
    clearTimeout(searchTimer);
  }
});
</script>

<style module>
.filters {
  display: grid;
  gap: 12px;
  padding: 18px;
  border-radius: 20px;
  border: 1px solid rgba(9, 29, 46, 0.08);
  background: linear-gradient(180deg, #ffffff 0%, #f9fbff 100%);
}

.filterBar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: start;
}

.filterActions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: center;
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

.statusChip strong {
  font-size: 13px;
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

.filterMeta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 700;
}

.workspace {
  display: grid;
  gap: 16px;
}

.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
}

.statCard {
  position: relative;
  padding: 18px;
  overflow: hidden;
}

.statLabel {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.statValue {
  margin-top: 16px;
  font-size: 30px;
  font-weight: 800;
  letter-spacing: -0.05em;
}

.statTone {
  position: absolute;
  left: 0;
  top: 0;
  width: 6px;
  height: 100%;
}

.statTone.primary {
  background: var(--cdd-primary);
}

.statTone.info {
  background: var(--cdd-info);
}

.statTone.danger {
  background: var(--cdd-danger);
}

.editorAnchor {
  scroll-margin-top: 24px;
}

.listSummaryCard {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  padding: 18px;
}

.listSummaryTitle {
  margin: 8px 0 0;
  font-size: 20px;
  letter-spacing: -0.04em;
}

.listSummaryText {
  margin-top: 10px;
  color: var(--cdd-text-soft);
  font-size: 12px;
  line-height: 1.6;
}

.listSummaryMeta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px 14px;
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 700;
}

.list {
  display: grid;
  gap: 12px;
}

.productCard {
  display: grid;
  padding: 14px;
}

.cover {
  display: grid;
  align-content: space-between;
  gap: 10px;
  width: 92px;
  min-height: 92px;
  padding: 14px 12px;
  border-radius: 18px;
  background:
    radial-gradient(circle at top left, rgba(255, 107, 0, 0.18), transparent 35%),
    linear-gradient(145deg, #eff5ff, #d9eaff);
  color: var(--cdd-text-soft);
  font-size: 12px;
  font-weight: 800;
}

.coverCode {
  font-size: 13px;
  line-height: 1.5;
  font-weight: 900;
  color: var(--cdd-text);
  word-break: break-all;
}

.coverMeta {
  font-size: 11px;
  color: var(--cdd-text-soft);
}

.productMain {
  display: grid;
  gap: 12px;
}

.productIdentity {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  min-width: 0;
}

.productCopy {
  min-width: 0;
}

.productHead {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.productName {
  margin: 0;
  font-size: 20px;
  letter-spacing: -0.04em;
}

.productSubtitle {
  margin-top: 8px;
  color: var(--cdd-text-soft);
  font-size: 12px;
  line-height: 1.6;
}

.productPath {
  margin-top: 8px;
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 700;
  line-height: 1.6;
}

.productBadgeGroup {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.skuPreviewPanel {
  display: grid;
  gap: 6px;
  padding: 10px 12px;
  border-radius: 16px;
  background: rgba(237, 244, 255, 0.72);
}

.skuPreview {
  font-size: 13px;
  font-weight: 700;
  line-height: 1.6;
  color: var(--cdd-text-soft);
}

.metricGrid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.metricCard {
  display: grid;
  gap: 6px;
  min-height: 80px;
  padding: 10px 12px;
  border-radius: 16px;
  background: rgba(237, 244, 255, 0.72);
}

.price {
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.04em;
  color: var(--cdd-primary);
}

.metaText {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 700;
}

.inventoryBlock {
  text-align: right;
}

.inventory {
  font-size: 16px;
  font-weight: 800;
}

.metricValue {
  font-size: 15px;
  font-weight: 800;
  line-height: 1.6;
  color: var(--cdd-text);
}

.actionHint {
  color: var(--cdd-text-faint);
  font-size: 11px;
  font-weight: 700;
  line-height: 1.6;
}

.productFooter {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding-top: 2px;
}

.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.editorPanel {
  display: grid;
  gap: 16px;
  align-content: start;
  padding: 18px;
}

.panelHead {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.panelEyebrow {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.panelTitle {
  margin: 8px 0 0;
  font-size: 24px;
  letter-spacing: -0.04em;
}

.formGrid {
  display: grid;
  gap: 12px;
}

.sectionTabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.sectionTab {
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

.sectionTab:hover {
  transform: translateY(-1px);
}

.sectionTabActive {
  border-color: rgba(255, 107, 0, 0.22);
  background: linear-gradient(135deg, rgba(255, 107, 0, 0.1), rgba(255, 249, 244, 0.98));
  color: #9c4304;
  box-shadow: inset 0 0 0 1px rgba(255, 107, 0, 0.08);
}

.formSection {
  display: grid;
  gap: 12px;
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

.field,
.fieldWide {
  display: grid;
  gap: 8px;
}

.fieldLabel {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.select,
.textarea {
  min-height: 50px;
  padding: 12px 14px;
  border: 0;
  border-radius: 16px;
  background: rgba(237, 244, 255, 0.95);
  color: var(--cdd-text);
  font: inherit;
  box-shadow: inset 0 0 0 1px transparent;
}

.select:focus,
.textarea:focus {
  outline: 0;
  box-shadow: inset 0 0 0 1px rgba(160, 65, 0, 0.24);
  background: rgba(255, 255, 255, 0.98);
}

.textarea {
  min-height: 96px;
  resize: vertical;
}

.detailSummary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.detailSummary > div {
  padding: 12px;
  border-radius: 16px;
  background: rgba(237, 244, 255, 0.56);
}

.summaryLabel {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.summaryValue {
  margin-top: 8px;
  font-size: 18px;
  font-weight: 800;
  line-height: 1.5;
  word-break: break-word;
}

.summaryNote {
  padding: 10px 12px;
  border-radius: 16px;
  background: rgba(255, 245, 235, 0.72);
  color: #9c4304;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.7;
}

.skuSection {
  display: grid;
  gap: 12px;
}

.skuSectionHead {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.sectionTitle {
  font-size: 15px;
  font-weight: 800;
}

.skuSnapshotGrid {
  display: grid;
  gap: 10px;
}

.skuSnapshotCard {
  display: grid;
  gap: 10px;
  padding: 12px;
  border-radius: 16px;
  background: rgba(237, 244, 255, 0.72);
}

.skuSnapshotHead {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.skuSnapshotName {
  font-size: 15px;
  font-weight: 800;
}

.skuSnapshotCode {
  margin-top: 6px;
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 700;
}

.skuSnapshotMeta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  color: var(--cdd-text-soft);
  font-size: 13px;
  font-weight: 700;
}

.skuEditorCard {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(237, 244, 255, 0.72);
  align-items: flex-end;
}

.skuEditorGrid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.panelActions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.sectionActions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.actionFooter {
  position: sticky;
  bottom: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 12px;
  justify-content: flex-end;
  background: linear-gradient(180deg, rgba(249, 251, 255, 0) 0%, #f9fbff 28%);
}

@media (max-width: 960px) {
  .filterBar,
  .detailSummary,
  .metricGrid,
  .skuEditorGrid {
    grid-template-columns: 1fr;
  }

  .filterActions {
    justify-content: flex-start;
  }

  .listSummaryCard {
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  .cover {
    width: 76px;
    min-height: 76px;
    padding: 12px;
  }

  .productIdentity,
  .productHead,
  .productFooter,
  .productMeta,
  .actions,
  .panelHead,
  .panelActions,
  .filterActions,
  .sectionActions,
  .actionFooter,
  .sectionTabs,
  .skuSectionHead,
  .skuEditorCard,
  .skuSnapshotHead {
    flex-direction: column;
    align-items: flex-start;
  }

  .filterActions :global(button),
  .actions :global(button),
  .panelActions :global(button),
  .sectionActions :global(button),
  .actionFooter :global(button) {
    width: 100%;
  }
}
</style>
