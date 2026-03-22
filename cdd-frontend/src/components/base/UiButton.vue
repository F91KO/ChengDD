<template>
  <button :class="classes" :type="type">
    <span v-if="leading" :class="$style.leading">{{ leading }}</span>
    <slot />
  </button>
</template>

<script setup lang="ts">
import { computed, useCssModule } from 'vue';

const props = withDefaults(
  defineProps<{
    variant?: 'primary' | 'secondary' | 'ghost';
    size?: 'md' | 'lg';
    type?: 'button' | 'submit' | 'reset';
    block?: boolean;
    leading?: string;
  }>(),
  {
    variant: 'primary',
    size: 'md',
    type: 'button',
    block: false,
    leading: '',
  },
);

const styles = useCssModule();

const classes = computed(() => [
  styles.button,
  styles[props.variant],
  styles[props.size],
  props.block ? styles.block : '',
]);
</script>

<style module>
.button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border: 0;
  border-radius: 18px;
  font-weight: 700;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease,
    color 0.18s ease;
}

.button:hover {
  transform: translateY(-1px);
}

.button:active {
  transform: translateY(0);
}

.button:focus-visible {
  outline: 2px solid rgba(255, 107, 0, 0.24);
  outline-offset: 3px;
}

.button:disabled {
  cursor: not-allowed;
  opacity: 0.68;
  transform: none;
  box-shadow: none;
}

.leading {
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 12px;
}

.primary {
  color: #fff;
  background: linear-gradient(135deg, var(--cdd-primary-deep), var(--cdd-primary));
  box-shadow: 0 16px 28px rgba(255, 107, 0, 0.22);
}

.secondary {
  color: var(--cdd-text);
  background: var(--cdd-surface-low);
  box-shadow: inset 0 0 0 1px rgba(9, 29, 46, 0.05);
}

.ghost {
  color: var(--cdd-text-soft);
  background: rgba(255, 255, 255, 0.72);
  box-shadow: inset 0 0 0 1px rgba(9, 29, 46, 0.08);
}

.md {
  min-height: 44px;
  padding: 0 18px;
  font-size: 14px;
}

.lg {
  min-height: 52px;
  padding: 0 24px;
  font-size: 15px;
}

.block {
  width: 100%;
}
</style>
