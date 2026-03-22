<template>
  <span :class="[$style.tag, $style[tone]]">
    <slot />
  </span>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    tone?: 'default' | 'primary' | 'success' | 'danger' | 'info';
  }>(),
  {
    tone: 'default',
  },
);
</script>

<style module>
.tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.default {
  color: var(--cdd-text-soft);
  background: #eef2f8;
}

.primary {
  color: #7a3000;
  background: var(--cdd-primary-soft);
}

.success {
  color: var(--cdd-success);
  background: var(--cdd-success-soft);
}

.danger {
  color: var(--cdd-danger);
  background: var(--cdd-danger-soft);
}

.info {
  color: var(--cdd-info);
  background: var(--cdd-info-soft);
}
</style>
