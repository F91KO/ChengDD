<template>
  <div v-if="total > 0" :class="$style.wrap">
    <div :class="$style.summary">
      共 {{ total }} 条，第 {{ safePage }} / {{ pageCount }} 页
    </div>

    <div :class="$style.controls">
      <label :class="$style.sizeField">
        <span>每页</span>
        <select :value="pageSize" :class="$style.select" @change="handlePageSizeChange">
          <option v-for="option in mergedPageSizeOptions" :key="option" :value="option">
            {{ option }}
          </option>
        </select>
        <span>条</span>
      </label>

      <button type="button" :class="$style.navButton" :disabled="disabled || safePage <= 1" @click="emitPage(safePage - 1)">
        上一页
      </button>

      <button
        v-for="item in pageItems"
        :key="item.key"
        type="button"
        :class="[item.type === 'page' ? $style.pageButton : $style.ellipsis, item.active ? $style.pageButtonActive : '']"
        :disabled="disabled || item.type !== 'page'"
        @click="item.type === 'page' && emitPage(item.value)"
      >
        {{ item.label }}
      </button>

      <button
        type="button"
        :class="$style.navButton"
        :disabled="disabled || safePage >= pageCount"
        @click="emitPage(safePage + 1)"
      >
        下一页
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

type PageItem =
  | { key: string; type: 'page'; label: string; value: number; active: boolean }
  | { key: string; type: 'ellipsis'; label: string; value: number; active: false };

const props = withDefaults(
  defineProps<{
    page: number;
    pageSize: number;
    total: number;
    pageSizeOptions?: number[];
    disabled?: boolean;
  }>(),
  {
    pageSizeOptions: () => [20, 50, 100],
    disabled: false,
  },
);

const emit = defineEmits<{
  'update:page': [page: number];
  'update:pageSize': [pageSize: number];
}>();

const pageCount = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)));
const safePage = computed(() => Math.min(Math.max(props.page, 1), pageCount.value));
const mergedPageSizeOptions = computed(() =>
  [...new Set([...props.pageSizeOptions, props.pageSize])].sort((left, right) => left - right),
);

const pageItems = computed<PageItem[]>(() => {
  const totalPages = pageCount.value;
  if (totalPages <= 5) {
    return Array.from({ length: totalPages }, (_, index) => {
      const value = index + 1;
      return { key: `page-${value}`, type: 'page', label: String(value), value, active: value === safePage.value };
    });
  }

  const pages = new Set<number>([1, totalPages, safePage.value - 1, safePage.value, safePage.value + 1]);
  const normalized = [...pages]
    .filter((value) => value >= 1 && value <= totalPages)
    .sort((left, right) => left - right);

  const items: PageItem[] = [];
  normalized.forEach((value, index) => {
    const previous = normalized[index - 1];
    if (previous && value - previous > 1) {
      items.push({ key: `ellipsis-${previous}-${value}`, type: 'ellipsis', label: '…', value: -1, active: false });
    }
    items.push({ key: `page-${value}`, type: 'page', label: String(value), value, active: value === safePage.value });
  });
  return items;
});

function emitPage(page: number) {
  const nextPage = Math.min(Math.max(page, 1), pageCount.value);
  if (nextPage !== props.page) {
    emit('update:page', nextPage);
  }
}

function handlePageSizeChange(event: Event) {
  const value = Number((event.target as HTMLSelectElement).value);
  if (!Number.isFinite(value) || value <= 0 || value === props.pageSize) {
    return;
  }
  emit('update:pageSize', value);
}
</script>

<style module>
.wrap {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 18px 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(20px);
  box-shadow:
    inset 0 0 0 1px rgba(9, 29, 46, 0.05),
    0 24px 48px rgba(9, 29, 46, 0.04);
}

.summary {
  color: var(--cdd-text-soft);
  font-size: 13px;
  font-weight: 700;
}

.controls {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: flex-end;
}

.sizeField {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 14px;
  min-height: 42px;
  border-radius: 999px;
  background: rgba(237, 244, 255, 0.82);
  color: var(--cdd-text-soft);
  font-size: 13px;
  font-weight: 700;
}

.select {
  border: 0;
  background: transparent;
  color: var(--cdd-text);
  font-size: 13px;
  font-weight: 700;
}

.navButton,
.pageButton,
.ellipsis {
  min-width: 42px;
  min-height: 42px;
  padding: 0 14px;
  border: 0;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 800;
}

.navButton,
.pageButton {
  background: rgba(237, 244, 255, 0.82);
  color: var(--cdd-text);
  transition:
    transform 0.18s ease,
    background 0.18s ease,
    color 0.18s ease;
}

.navButton:hover,
.pageButton:hover {
  transform: translateY(-1px);
}

.pageButtonActive {
  color: #fff;
  background: linear-gradient(135deg, var(--cdd-primary-deep), var(--cdd-primary));
}

.ellipsis {
  display: grid;
  place-items: center;
  color: var(--cdd-text-faint);
  background: transparent;
}

.navButton:disabled,
.pageButton:disabled {
  opacity: 0.52;
  transform: none;
}

@media (max-width: 960px) {
  .wrap {
    align-items: flex-start;
    flex-direction: column;
  }

  .controls {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
