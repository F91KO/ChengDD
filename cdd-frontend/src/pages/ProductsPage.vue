<template>
  <WorkspaceLayout
    eyebrow="Catalog"
    title="商品管理"
    description="当前页面展示真实商品摘要，并将新增、详情、上下架和库存调整统一收口到页内编辑区。"
  >
    <section :class="$style.filters">
      <UiInput v-model="keyword" placeholder="搜索商品名称、SPU、SKU..." prefix="搜" />
      <UiButton variant="secondary" @click="resetKeyword">重置关键词</UiButton>
      <UiButton @click="openCreatePanel">新增商品</UiButton>
    </section>

    <UiStatePanel
      v-if="productLoadState.loading"
      tone="loading"
      title="正在加载商品列表"
      description="正在读取真实商品摘要、SKU 价格和库存信息。"
    />
    <UiStatePanel
      v-else-if="productLoadState.errorMessage"
      tone="error"
      title="商品接口调用失败"
      :description="productLoadState.errorMessage"
    />
    <UiStatePanel
      v-else
      tone="info"
      title="当前使用真实商品接口"
      :description="productLoadState.message"
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
                {{ panelMode === 'create' ? '新增真实商品' : detail?.product_name || selectedProduct?.name || '编辑商品' }}
              </h3>
            </div>
            <UiButton variant="secondary" @click="closePanel">收起面板</UiButton>
          </div>

          <UiStatePanel
            v-if="panelMode === 'edit'"
            tone="info"
            title="当前已接入真实商品编辑接口"
            description="商品基础字段、SKU、上下架和库存调整都会直接调用后端接口。SKU 编码由系统自动生成，用户无需手填。"
          />

          <UiStatePanel
            v-if="panelLoading"
            tone="loading"
            title="正在准备面板数据"
            description="正在加载分类、商品详情和 SKU 信息。"
          />

          <template v-else-if="panelMode === 'create'">
            <div :class="$style.formGrid">
              <label :class="$style.field">
                <span :class="$style.fieldLabel">商品分类</span>
                <select v-model="createForm.categoryId" :class="$style.select">
                  <option value="">请选择分类</option>
                  <option v-for="category in categories" :key="category.id" :value="String(category.id)">
                    {{ category.category_name }}（ID {{ category.id }}）
                  </option>
                </select>
              </label>
              <UiInput v-model="createForm.productName" label="商品名称" placeholder="请输入商品名称" />
              <UiInput v-model="createForm.productSubTitle" label="商品副标题" placeholder="请输入商品副标题" />
              <UiInput v-model="createForm.skuName" label="SKU 名称" placeholder="例如 标准装" />
              <UiInput v-model="createForm.salePrice" label="销售价" placeholder="例如 99.00" />
              <UiInput v-model="createForm.availableStock" label="初始库存" placeholder="例如 100" />
            </div>
            <div :class="$style.panelActions">
              <UiButton variant="secondary" @click="resetCreateForm">清空表单</UiButton>
              <UiButton :disabled="submitting" @click="handleCreateProduct">
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
                <div :class="$style.summaryLabel">商品分类</div>
                <div :class="$style.summaryValue">{{ resolveCategoryName(detail.category_id) }}</div>
              </div>
              <div>
                <div :class="$style.summaryLabel">当前状态</div>
                <div :class="$style.summaryValue">{{ selectedProduct?.status || detail.status }}</div>
              </div>
            </section>

            <div :class="$style.formGrid">
              <label :class="$style.field">
                <span :class="$style.fieldLabel">商品分类</span>
                <select v-model="editForm.categoryId" :class="$style.select">
                  <option value="">请选择分类</option>
                  <option v-for="category in categories" :key="category.id" :value="String(category.id)">
                    {{ category.category_name }}（ID {{ category.id }}）
                  </option>
                </select>
              </label>
              <UiInput v-model="editForm.productName" label="商品名称" placeholder="请输入商品名称" />
              <UiInput v-model="editForm.productSubTitle" label="商品副标题" placeholder="请输入商品副标题" />
            </div>

            <div :class="$style.skuSection">
              <div :class="$style.skuSectionHead">
                <div :class="$style.sectionTitle">SKU 编辑</div>
                <UiButton variant="secondary" @click="addEditSku">新增 SKU</UiButton>
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
                    :disabled="editForm.skus.length <= 1"
                    @click="removeEditSku(sku.clientId)"
                  >
                    删除 SKU
                  </UiButton>
                </div>
              </article>
            </div>

            <div :class="$style.panelActions">
              <UiButton variant="secondary" :disabled="submitting" @click="handleUpdateProduct">
                {{ submitting ? '正在保存...' : '保存商品信息' }}
              </UiButton>
            </div>

            <div :class="$style.skuSection">
              <div :class="$style.sectionTitle">库存调整</div>
              <div :class="$style.formGrid">
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
            </div>

            <div :class="$style.panelActions">
              <UiButton variant="secondary" :disabled="submitting" @click="handleAdjustStock">
                {{ submitting ? '正在提交...' : '提交库存调整' }}
              </UiButton>
              <UiButton
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

      <section :class="$style.list">
        <UiCard v-for="product in products" :key="product.sku" elevated :class="$style.productCard">
          <div :class="$style.cover">{{ product.sku }}</div>
          <div :class="$style.productMain">
            <div :class="$style.productHead">
              <div>
                <h3 :class="$style.productName">{{ product.name }}</h3>
                <div :class="$style.productSku">
                  {{ product.categoryName }} · SKU {{ product.skuCount }} 个
                </div>
              </div>
              <UiTag :tone="product.statusTone as 'default' | 'primary' | 'info'">
                {{ product.status }}
              </UiTag>
            </div>
            <div :class="$style.productMeta">
              <div>
                <div :class="$style.price">{{ product.price }}</div>
                <div :class="$style.metaText">近日销量 {{ product.sales }}</div>
              </div>
              <div :class="$style.inventoryBlock">
                <div :class="$style.metaText">库存摘要</div>
                <div :class="$style.inventory">{{ product.inventory }}</div>
              </div>
            </div>
            <div :class="$style.actions">
              <UiButton variant="secondary" @click="openDetailPanel(product)">编辑商品</UiButton>
              <UiButton variant="secondary" @click="openStockPanel(product)">库存调整</UiButton>
              <UiButton
                :variant="publishActionVariant(product.statusRaw)"
                @click="handleTogglePublish(product)"
              >
                {{ publishActionLabel(product.statusRaw) }}
              </UiButton>
            </div>
          </div>
        </UiCard>

        <UiStatePanel
          v-if="!productLoadState.loading && products.length === 0"
          tone="empty"
          title="没有找到匹配商品"
          description="当前接口没有返回匹配商品，可以重置关键词后重试，或直接创建新商品。"
        >
          <UiButton variant="secondary" @click="resetKeyword">清空筛选</UiButton>
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
import { onBeforeUnmount, nextTick, onMounted, reactive, ref, watch } from 'vue';
import UiButton from '@/components/base/UiButton.vue';
import UiCard from '@/components/base/UiCard.vue';
import UiInput from '@/components/base/UiInput.vue';
import UiPagination from '@/components/base/UiPagination.vue';
import UiStatePanel from '@/components/base/UiStatePanel.vue';
import UiTag from '@/components/base/UiTag.vue';
import WorkspaceLayout from '@/components/layout/WorkspaceLayout.vue';
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
type EditableSkuForm = {
  clientId: string;
  skuCode: string;
  skuName: string;
  salePrice: string;
  availableStock: string;
};

const authStore = useAuthStore();
const keyword = ref('');
const actionMessage = ref('');
const actionTone = ref<'info' | 'error'>('info');
const panelMode = ref<PanelMode>(null);
const panelLoading = ref(false);
const submitting = ref(false);
const categories = ref<ProductCategoryResponseRaw[]>([]);
const selectedProduct = ref<ProductCard | null>(null);
const detail = ref<ProductDetailResponseRaw | null>(null);
const panelAnchor = ref<HTMLElement | null>(null);
let searchTimer: ReturnType<typeof setTimeout> | null = null;

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

function scrollPanelIntoView() {
  void nextTick(() => {
    panelAnchor.value?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  });
}

function resetKeyword() {
  if (!keyword.value.trim()) {
    void loadProducts(true, undefined, '', 1, productPagination.pageSize);
  }
  keyword.value = '';
}

function resetCreateForm() {
  createForm.categoryId = categories.value[0] ? String(categories.value[0].id) : '';
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
  await loadProducts(true, undefined, keyword.value, productPagination.page, productPagination.pageSize);
}

function handlePageChange(page: number) {
  void loadProducts(true, undefined, keyword.value, page, productPagination.pageSize);
}

function handlePageSizeChange(pageSize: number) {
  void loadProducts(true, undefined, keyword.value, 1, pageSize);
}

function closePanel() {
  panelMode.value = null;
  selectedProduct.value = null;
  detail.value = null;
  panelLoading.value = false;
  submitting.value = false;
}

async function openCreatePanel() {
  panelMode.value = 'create';
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
}

async function refreshSelectedProduct(productId: number) {
  selectedProduct.value = products.find((item) => item.id === productId) ?? selectedProduct.value;
  detail.value = await fetchProductDetail(productId);
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
  void loadProducts(false, undefined, keyword.value, productPagination.page, productPagination.pageSize);
});

watch(keyword, () => {
  if (searchTimer) {
    clearTimeout(searchTimer);
  }
  searchTimer = setTimeout(() => {
    void loadProducts(true, undefined, keyword.value, 1, productPagination.pageSize);
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
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 12px;
}

.workspace {
  display: grid;
  gap: 18px;
}

.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.statCard {
  position: relative;
  padding: 20px;
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
  font-size: 34px;
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

.list {
  display: grid;
  gap: 16px;
}

.productCard {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 18px;
  padding: 18px;
}

.cover {
  display: grid;
  place-items: center;
  min-height: 164px;
  border-radius: 20px;
  background:
    radial-gradient(circle at top left, rgba(255, 107, 0, 0.18), transparent 35%),
    linear-gradient(145deg, #eff5ff, #d9eaff);
  color: var(--cdd-text-soft);
  font-size: 14px;
  font-weight: 800;
}

.productMain {
  display: grid;
  gap: 18px;
}

.productHead {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.productName {
  margin: 0;
  font-size: 22px;
  letter-spacing: -0.04em;
}

.productSku {
  margin-top: 10px;
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 700;
}

.productMeta {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: end;
}

.price {
  font-size: 28px;
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
  margin-top: 6px;
  font-size: 18px;
  font-weight: 800;
}

.actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.editorPanel {
  display: grid;
  gap: 18px;
  align-content: start;
  padding: 22px;
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
  gap: 14px;
}

.field,
.fieldWide {
  display: grid;
  gap: 10px;
}

.fieldLabel {
  font-size: 13px;
  font-weight: 700;
  color: var(--cdd-text-soft);
}

.select,
.textarea {
  min-height: 54px;
  padding: 14px 16px;
  border: 0;
  border-radius: 18px;
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
  min-height: 108px;
  resize: vertical;
}

.detailSummary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.summaryLabel {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 700;
}

.summaryValue {
  margin-top: 8px;
  font-size: 18px;
  font-weight: 800;
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

.skuEditorCard {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(237, 244, 255, 0.72);
}

.skuEditorGrid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.panelActions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

@media (max-width: 960px) {
  .filters,
  .detailSummary,
  .skuEditorGrid {
    grid-template-columns: 1fr;
  }

  .productCard {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .productHead,
  .productMeta,
  .actions,
  .panelHead,
  .panelActions,
  .skuSectionHead,
  .skuEditorCard {
    flex-direction: column;
    align-items: flex-start;
  }

  .actions :global(button),
  .panelActions :global(button) {
    width: 100%;
  }
}
</style>
