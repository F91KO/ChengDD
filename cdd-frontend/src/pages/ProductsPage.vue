<template>
  <WorkspaceLayout
    eyebrow="Catalog"
    title="商品管理"
    description="围绕商品状态、库存预警和上架动作组织列表，不使用重型组件库，先保留前端拼装灵活度。"
  >
    <section :class="$style.filters">
      <UiInput v-model="keyword" placeholder="搜索商品名称、SKU..." prefix="搜" />
      <UiButton variant="secondary">筛选条件</UiButton>
      <UiButton>新增商品</UiButton>
    </section>

    <UiStatePanel
      v-if="productLoadState.loading"
      tone="loading"
      title="正在加载商品列表"
      description="正在优先请求真实商品接口，若服务不可用会自动回退到演示数据。"
    />
    <UiStatePanel
      v-else
      :tone="productLoadState.source === 'remote' ? 'info' : 'empty'"
      :title="productLoadState.source === 'remote' ? '当前使用真实商品接口' : '当前展示演示商品数据'"
      :description="productLoadState.message"
    />

    <section :class="$style.stats">
      <UiCard v-for="stat in productStats" :key="stat.label" elevated :class="$style.statCard">
        <div :class="$style.statLabel">{{ stat.label }}</div>
        <div :class="$style.statValue">{{ stat.value }}</div>
        <div :class="[$style.statTone, $style[stat.tone]]"></div>
      </UiCard>
    </section>

    <section :class="$style.list">
      <UiCard
        v-for="product in filteredProducts"
        :key="product.sku"
        elevated
        :class="$style.productCard"
      >
        <div :class="$style.cover">{{ product.sku }}</div>
        <div :class="$style.productMain">
          <div :class="$style.productHead">
            <div>
              <h3 :class="$style.productName">{{ product.name }}</h3>
              <div :class="$style.productSku">SKU: {{ product.sku }}</div>
            </div>
            <UiTag :tone="product.statusTone as 'default' | 'primary' | 'info'">
              {{ product.status }}
            </UiTag>
          </div>
          <div :class="$style.productMeta">
            <div>
              <div :class="$style.price">{{ product.price }}</div>
              <div :class="$style.metaText">销量 {{ product.sales }}</div>
            </div>
            <div :class="$style.inventoryBlock">
              <div :class="$style.metaText">库存</div>
              <div :class="$style.inventory">{{ product.inventory }}</div>
            </div>
          </div>
          <div :class="$style.actions">
            <UiButton variant="secondary">编辑</UiButton>
            <UiButton variant="secondary">库存</UiButton>
            <UiButton :variant="product.status === '已下架' ? 'primary' : 'ghost'">
              {{ product.status === '已下架' ? '上架销售' : '下架' }}
            </UiButton>
          </div>
        </div>
      </UiCard>
      <UiStatePanel
        v-if="!productLoadState.loading && filteredProducts.length === 0"
        tone="empty"
        title="没有找到匹配商品"
        description="可以更换关键词、清空筛选条件，或直接新增一个商品补齐目录。"
      >
        <UiButton variant="secondary" @click="keyword = ''">清空筛选</UiButton>
        <UiButton>新增商品</UiButton>
      </UiStatePanel>
    </section>
  </WorkspaceLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import UiButton from '@/components/base/UiButton.vue';
import UiCard from '@/components/base/UiCard.vue';
import UiInput from '@/components/base/UiInput.vue';
import UiStatePanel from '@/components/base/UiStatePanel.vue';
import UiTag from '@/components/base/UiTag.vue';
import WorkspaceLayout from '@/components/layout/WorkspaceLayout.vue';
import { loadProducts, productLoadState, productStats, products } from '@/modules/products/mock';

const keyword = ref('');
const filteredProducts = computed(() => {
  const query = keyword.value.trim().toLowerCase();
  if (!query) {
    return products;
  }

  return products.filter(
    (product) =>
      product.name.toLowerCase().includes(query) || product.sku.toLowerCase().includes(query),
  );
});

onMounted(() => {
  void loadProducts();
});
</script>

<style module>
.filters {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 12px;
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
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
}

@media (max-width: 960px) {
  .filters {
    grid-template-columns: 1fr;
  }

  .stats {
    grid-template-columns: 1fr;
  }

  .productCard {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .productHead,
  .productMeta,
  .actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .actions :global(button) {
    width: 100%;
  }
}
</style>
