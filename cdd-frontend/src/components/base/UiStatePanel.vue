<template>
  <section :class="[$style.panel, $style[tone]]">
    <div :class="$style.icon">{{ iconMap[tone] }}</div>
    <div :class="$style.body">
      <h3 :class="$style.title">{{ title }}</h3>
      <p :class="$style.description">{{ description }}</p>
      <div v-if="$slots.default" :class="$style.actions">
        <slot />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    tone?: 'loading' | 'empty' | 'error' | 'info' | 'success';
    title: string;
    description: string;
  }>(),
  {
    tone: 'info',
  },
);

const iconMap = {
  loading: '载',
  empty: '空',
  error: '警',
  info: '知',
  success: '成',
} as const;

const tone = props.tone;
</script>

<style module>
.panel {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
  padding: 18px 20px;
  border-radius: 20px;
}

.icon {
  width: 56px;
  height: 56px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  font-size: 18px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.body {
  min-width: 0;
}

.title {
  margin: 0;
  font-size: 16px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.description {
  margin: 8px 0 0;
  color: var(--cdd-text-soft);
  font-size: 13px;
  line-height: 1.7;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.loading {
  background:
    linear-gradient(135deg, rgba(255, 241, 223, 0.96), rgba(255, 255, 255, 0.9)),
    var(--cdd-warning-soft);
}

.loading .icon {
  color: var(--cdd-primary-deep);
  background: rgba(255, 107, 0, 0.12);
}

.empty {
  background:
    linear-gradient(135deg, rgba(237, 244, 255, 0.9), rgba(255, 255, 255, 0.98)),
    var(--cdd-surface-low);
}

.empty .icon {
  color: var(--cdd-info);
  background: rgba(0, 98, 161, 0.12);
}

.error {
  background:
    linear-gradient(135deg, rgba(255, 218, 214, 0.95), rgba(255, 255, 255, 0.94)),
    var(--cdd-danger-soft);
}

.error .icon {
  color: var(--cdd-danger);
  background: rgba(186, 26, 26, 0.1);
}

.info {
  background:
    linear-gradient(135deg, rgba(208, 228, 255, 0.88), rgba(255, 255, 255, 0.96)),
    var(--cdd-info-soft);
}

.info .icon {
  color: var(--cdd-info);
  background: rgba(0, 98, 161, 0.12);
}

.success {
  background:
    linear-gradient(135deg, rgba(223, 247, 230, 0.92), rgba(255, 255, 255, 0.96)),
    var(--cdd-success-soft);
}

.success .icon {
  color: var(--cdd-success);
  background: rgba(52, 168, 83, 0.12);
}

@media (max-width: 640px) {
  .panel {
    grid-template-columns: 1fr;
  }

  .icon {
    width: 48px;
    height: 48px;
  }
}
</style>
