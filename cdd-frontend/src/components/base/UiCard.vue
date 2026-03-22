<template>
  <section :class="[$style.card, elevated ? $style.elevated : '']">
    <slot />
  </section>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    elevated?: boolean;
  }>(),
  {
    elevated: false,
  },
);
</script>

<style module>
.card {
  position: relative;
  border-radius: var(--cdd-radius-lg);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: inset 0 0 0 1px rgba(9, 29, 46, 0.04);
  overflow: clip;
}

.elevated {
  box-shadow: var(--cdd-shadow-soft);
}
</style>
