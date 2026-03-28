<template>
  <UiCard elevated :class="$style.panel">
    <div :class="$style.panelHead">
      <div>
        <div :class="$style.eyebrowText">分类列表</div>
        <h3 :class="$style.titleText">当前商家分类树</h3>
      </div>
      <div :class="$style.listHeader">
        <div :class="$style.treeMeta">
          <span>分类总数 {{ totalCategories }} 项</span>
          <span>当前可见 {{ visibleCount }} 项</span>
          <span>当前页 {{ currentPageCount }} 项</span>
        </div>
        <div :class="$style.panelActions">
          <UiButton variant="secondary" @click="emit('refresh')">刷新数据</UiButton>
          <UiButton variant="secondary" @click="emit('createRoot')">新增一级分类</UiButton>
        </div>
      </div>
    </div>

    <UiStatePanel
      v-if="!totalCategories"
      tone="empty"
      title="当前尚未初始化分类"
      description="可先选择平台分类模板，一键生成基础分类结构，再继续新增和调整。"
    />

    <template v-else>
      <div :class="$style.listWorkspace">
        <div :class="$style.listToolbar">
          <div :class="$style.searchPanel">
            <span :class="$style.searchIcon">⌕</span>
            <input
              :value="searchKeyword"
              :class="$style.searchInput"
              type="search"
              placeholder="搜索分类名称..."
              @input="emit('update:searchKeyword', ($event.target as HTMLInputElement).value)"
            />
          </div>
          <div :class="$style.selectionBar">
            <span>当前{{ selectedCategoryName ? `正在编辑「${selectedCategoryName}」` : '未选择分类' }}</span>
            <span>{{ formMode === 'edit' ? '左侧切换对象，右侧保存' : '可直接创建新分类' }}</span>
          </div>
        </div>

        <UiStatePanel
          v-if="searchKeyword.trim() && !rows.length"
          tone="empty"
          title="未匹配到分类"
          description="可尝试其他关键词，或先新增分类。"
        />

        <div v-else :class="$style.listScroller">
          <div :class="$style.categoryList">
            <div
              v-for="row in rows"
              :key="row.id"
              :class="[
                $style.categoryRow,
                selectedCategoryId === row.id ? $style.categoryRowActive : '',
                row.isMatched ? $style.categoryRowMatched : '',
              ]"
              @click="emit('edit', row.id)"
            >
              <div :class="$style.categoryMain" :style="{ paddingInlineStart: `${row.depth * 24}px` }">
                <div :class="$style.categoryNameLine">
                  <button
                    :class="[$style.foldButton, row.hasChildren ? '' : $style.foldButtonEmpty]"
                    type="button"
                    :disabled="!row.hasChildren"
                    @click.stop="emit('toggle', row.id)"
                  >
                    {{ row.hasChildren ? (row.isExpanded ? '▾' : '▸') : '·' }}
                  </button>
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
                  <span v-if="selectedCategoryId === row.id" :class="$style.editingBadge">正在编辑</span>
                </div>
                <div :class="$style.categoryPath">{{ row.pathLabel }}</div>
                <div :class="$style.categoryMeta">
                  <span>层级 {{ row.category_level }}</span>
                  <span>排序 {{ row.sort_order }}</span>
                  <span>父级 {{ row.parentName || '顶级分类' }}</span>
                </div>
              </div>
              <div :class="$style.inlineRowActions">
                <UiButton variant="secondary" @click.stop="emit('createChild', row.id)">新增子分类</UiButton>
                <UiButton variant="secondary" @click.stop="emit('edit', row.id)">编辑</UiButton>
              </div>
            </div>
          </div>
        </div>

        <div :class="$style.paginationBar">
          <UiPagination
            v-if="total"
            :page="page"
            :page-size="pageSize"
            :total="total"
            unit-label="组"
            :disabled="loading"
            @update:page="emit('update:page', $event)"
            @update:page-size="emit('update:pageSize', $event)"
          />
        </div>
      </div>
    </template>
  </UiCard>
</template>

<script setup lang="ts">
import UiButton from '@/components/base/UiButton.vue';
import UiCard from '@/components/base/UiCard.vue';
import UiPagination from '@/components/base/UiPagination.vue';
import UiStatePanel from '@/components/base/UiStatePanel.vue';
import UiTag from '@/components/base/UiTag.vue';

type WorkbenchRow = {
  id: number;
  template_id: number | null;
  category_name: string;
  category_level: number;
  sort_order: number;
  is_enabled: boolean;
  is_visible: boolean;
  depth: number;
  parentName: string;
  pathLabel: string;
  hasChildren: boolean;
  isExpanded: boolean;
  isMatched: boolean;
};

defineProps<{
  rows: WorkbenchRow[];
  selectedCategoryId: number | null;
  selectedCategoryName: string;
  searchKeyword: string;
  formMode: 'create' | 'edit';
  totalCategories: number;
  visibleCount: number;
  currentPageCount: number;
  page: number;
  pageSize: number;
  total: number;
  loading: boolean;
}>();

const emit = defineEmits<{
  'update:searchKeyword': [value: string];
  'update:page': [value: number];
  'update:pageSize': [value: number];
  refresh: [];
  createRoot: [];
  createChild: [categoryId: number];
  edit: [categoryId: number];
  toggle: [categoryId: number];
}>();
</script>

<style module>
.panel {
  padding: 20px;
  border-radius: 20px;
  border: 1px solid rgba(9, 29, 46, 0.08);
  background: linear-gradient(180deg, #ffffff 0%, #f9fbff 100%);
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

.panelActions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.listWorkspace {
  display: grid;
  gap: 16px;
  margin-top: 16px;
}

.listToolbar {
  position: sticky;
  top: 0;
  z-index: 1;
  display: grid;
  gap: 10px;
  padding-bottom: 4px;
  background: linear-gradient(180deg, #f9fbff 0%, rgba(249, 251, 255, 0.92) 100%);
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

.selectionBar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(237, 244, 255, 0.88);
  border: 1px solid rgba(9, 29, 46, 0.08);
  color: var(--cdd-text-soft);
  font-size: 12px;
  font-weight: 700;
}

.listScroller {
  max-height: min(62vh, 920px);
  overflow: auto;
  padding-right: 6px;
}

.categoryList {
  display: grid;
  gap: 10px;
}

.categoryRow {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  border-radius: 18px;
  padding: 14px 16px;
  background: rgba(237, 244, 255, 0.72);
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

.categoryMain {
  min-width: 0;
  flex: 1;
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

.editingBadge {
  margin-left: auto;
  color: #ff6b00;
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.categoryPath {
  margin-top: 8px;
  color: var(--cdd-text-faint);
  font-size: 12px;
  line-height: 1.6;
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

.paginationBar {
  padding-top: 4px;
  border-top: 1px solid rgba(9, 29, 46, 0.06);
}

@media (max-width: 1080px) {
  .listScroller {
    max-height: none;
  }

  .listToolbar {
    position: static;
  }
}

@media (max-width: 720px) {
  .panel {
    padding: 18px;
  }

  .panelHead,
  .categoryRow,
  .selectionBar {
    flex-direction: column;
    align-items: flex-start;
  }

  .listHeader {
    justify-items: stretch;
  }

  .inlineRowActions,
  .panelActions {
    width: 100%;
  }

  .inlineRowActions :global(button),
  .panelActions :global(button) {
    width: 100%;
  }
}
</style>
