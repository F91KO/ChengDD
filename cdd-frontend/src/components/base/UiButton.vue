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
    size?: 'sm' | 'md' | 'lg';
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
  border: 1px solid transparent;
  border-radius: 14px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease,
    color 0.18s ease,
    border-color 0.18s ease;
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
  box-shadow: 0 10px 18px rgba(255, 107, 0, 0.18);
}

.secondary {
  color: var(--cdd-text);
  background: rgba(255, 255, 255, 0.96);
  border-color: rgba(9, 29, 46, 0.1);
  box-shadow: 0 4px 10px rgba(9, 29, 46, 0.04);
}

.ghost {
  color: var(--cdd-text-soft);
  background: rgba(244, 247, 252, 0.88);
  border-color: rgba(9, 29, 46, 0.06);
  box-shadow: none;
}

.sm {
  min-height: 36px;
  padding: 0 12px;
  font-size: 13px;
}

.md {
  min-height: 40px;
  padding: 0 16px;
  font-size: 14px;
}

.lg {
  min-height: 48px;
  padding: 0 22px;
  font-size: 15px;
}

.block {
  width: 100%;
}
</style>
