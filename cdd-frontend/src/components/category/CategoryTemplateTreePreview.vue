<template>
  <div :class="[$style.wrap, compact ? $style.compactWrap : '']">
    <div v-if="title || summary" :class="$style.head">
      <div v-if="title" :class="$style.title">{{ title }}</div>
      <div v-if="summary" :class="$style.summary">{{ summary }}</div>
    </div>

    <div :class="[$style.list, compact ? $style.compactList : '']">
      <div
        v-for="node in rows"
        :key="node.key"
        :class="$style.row"
      >
        <span :style="{ paddingInlineStart: `${node.depth * 22}px` }" :class="$style.name">
          {{ node.category_name }}
        </span>
        <div :class="$style.tags">
          <UiTag v-if="showEnabled" :tone="node.is_enabled ? 'success' : 'danger'">
            {{ node.is_enabled ? '启用' : '停用' }}
          </UiTag>
          <UiTag v-if="showVisible" :tone="node.is_visible ? 'info' : 'default'">
            {{ node.is_visible ? visibleLabel : hiddenLabel }}
          </UiTag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import UiTag from '@/components/base/UiTag.vue';

type TemplatePreviewNode = {
  key: string;
  category_name: string;
  depth: number;
  is_enabled: boolean;
  is_visible: boolean;
};

withDefaults(
  defineProps<{
    rows: TemplatePreviewNode[];
    title?: string;
    summary?: string;
    compact?: boolean;
    showEnabled?: boolean;
    showVisible?: boolean;
    visibleLabel?: string;
    hiddenLabel?: string;
  }>(),
  {
    title: '',
    summary: '',
    compact: false,
    showEnabled: true,
    showVisible: true,
    visibleLabel: '展示',
    hiddenLabel: '隐藏',
  },
);
</script>

<style module>
.wrap {
  display: grid;
  gap: 18px;
}

.compactWrap {
  gap: 14px;
}

.head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.title {
  font-size: 14px;
  font-weight: 800;
  color: var(--cdd-text);
}

.summary {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 700;
}

.list {
  display: grid;
  gap: 12px;
}

.compactList {
  max-height: 320px;
  overflow: auto;
  padding-right: 4px;
}

.row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(237, 244, 255, 0.72);
  border: 1px solid rgba(9, 29, 46, 0.06);
}

.name {
  color: var(--cdd-text-soft);
  min-width: 0;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

@media (max-width: 720px) {
  .head,
  .row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
