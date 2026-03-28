<template>
  <label :class="$style.field">
    <span v-if="label" :class="$style.label">{{ label }}</span>
    <div :class="$style.searchWrap">
      <span :class="$style.searchIcon">⌕</span>
      <input
        v-model.trim="keyword"
        :class="$style.searchInput"
        :placeholder="searchPlaceholder"
        :disabled="disabled"
        type="search"
      />
    </div>
    <select
      :value="modelValue"
      :class="$style.select"
      :disabled="disabled"
      @change="emit('update:modelValue', ($event.target as HTMLSelectElement).value)"
    >
      <option v-if="placeholder && emptyValue !== null" :value="emptyValue">{{ placeholder }}</option>
      <option v-for="option in visibleOptions" :key="option.value" :value="option.value">
        {{ option.label }}
      </option>
    </select>
    <div :class="$style.meta">
      <span>可选 {{ visibleOptions.length }} 项</span>
      <span v-if="selectedOption">{{ selectedOption.pathLabel }}</span>
      <span v-else>{{ emptyText }}</span>
    </div>
  </label>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';

type HierarchyOption = {
  value: string;
  label: string;
  pathLabel: string;
  searchText: string;
};

const props = withDefaults(
  defineProps<{
    modelValue: string;
    options: HierarchyOption[];
    label?: string;
    placeholder?: string;
    searchPlaceholder?: string;
    emptyText?: string;
    emptyValue?: string | null;
    disabled?: boolean;
  }>(),
  {
    label: '',
    placeholder: '请选择',
    searchPlaceholder: '输入关键词筛选',
    emptyText: '尚未选择',
    emptyValue: '',
    disabled: false,
  },
);

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();

const keyword = ref('');

const selectedOption = computed(() =>
  props.options.find((item) => item.value === props.modelValue) ?? null,
);

const filteredOptions = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase();
  if (!normalizedKeyword) {
    return props.options;
  }
  return props.options.filter((item) => item.searchText.includes(normalizedKeyword));
});

const visibleOptions = computed(() => {
  if (!selectedOption.value) {
    return filteredOptions.value;
  }
  if (filteredOptions.value.some((item) => item.value === selectedOption.value?.value)) {
    return filteredOptions.value;
  }
  return [selectedOption.value, ...filteredOptions.value];
});
</script>

<style module>
.field {
  display: grid;
  gap: 8px;
}

.label {
  font-size: 13px;
  font-weight: 700;
  color: var(--cdd-text-soft);
}

.searchWrap {
  position: relative;
}

.searchIcon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: #ff6b00;
  font-size: 12px;
  font-weight: 800;
}

.searchInput,
.select {
  width: 100%;
  min-height: 50px;
  border: 0;
  border-radius: 16px;
  background: rgba(237, 244, 255, 0.95);
  color: var(--cdd-text);
  font: inherit;
  box-shadow: inset 0 0 0 1px transparent;
  transition:
    box-shadow 0.18s ease,
    background 0.18s ease;
}

.searchInput {
  padding: 0 14px 0 38px;
}

.select {
  padding: 0 14px;
}

.searchInput:focus,
.select:focus {
  outline: 0;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: inset 0 0 0 1px rgba(160, 65, 0, 0.24);
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 700;
  line-height: 1.6;
}
</style>
