<template>
  <label :class="$style.field">
    <span v-if="label" :class="$style.label">{{ label }}</span>
    <div :class="$style.control">
      <span v-if="prefix" :class="$style.prefix">{{ prefix }}</span>
      <input
        :value="modelValue"
        :class="$style.input"
        :placeholder="placeholder"
        :type="type"
        @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      />
    </div>
  </label>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    modelValue: string;
    label?: string;
    placeholder?: string;
    type?: string;
    prefix?: string;
  }>(),
  {
    label: '',
    placeholder: '',
    type: 'text',
    prefix: '',
  },
);

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();
</script>

<style module>
.field {
  display: grid;
  gap: 10px;
}

.label {
  font-size: 13px;
  font-weight: 700;
  color: var(--cdd-text-soft);
}

.control {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 54px;
  padding: 0 16px;
  border-radius: 18px;
  background: rgba(237, 244, 255, 0.95);
  box-shadow: inset 0 0 0 1px transparent;
  transition:
    box-shadow 0.18s ease,
    background 0.18s ease;
}

.control:focus-within {
  background: rgba(255, 255, 255, 0.98);
  box-shadow: inset 0 0 0 1px rgba(160, 65, 0, 0.24);
}

.prefix {
  color: var(--cdd-text-faint);
  font-size: 13px;
  font-weight: 800;
}

.input {
  flex: 1;
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--cdd-text);
}

.input::placeholder {
  color: #9aa5b1;
}
</style>
