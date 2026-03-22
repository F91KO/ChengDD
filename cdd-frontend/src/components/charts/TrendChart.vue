<template>
  <div ref="containerRef" :class="$style.chart"></div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { init, use } from 'echarts/core';
import { LineChart } from 'echarts/charts';
import { GridComponent, TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import type { ECharts, EChartsCoreOption } from 'echarts/core';

use([LineChart, GridComponent, TooltipComponent, CanvasRenderer]);

const props = defineProps<{
  option: EChartsCoreOption;
}>();

const containerRef = ref<HTMLDivElement | null>(null);
let chart: ECharts | undefined;
let observer: ResizeObserver | undefined;

function renderChart() {
  if (!containerRef.value) {
    return;
  }

  if (!chart) {
    chart = init(containerRef.value);
  }

  chart.setOption(props.option);
}

onMounted(() => {
  renderChart();
  observer = new ResizeObserver(() => chart?.resize());
  if (containerRef.value) {
    observer.observe(containerRef.value);
  }
  window.addEventListener('resize', renderChart);
});

watch(
  () => props.option,
  () => {
    renderChart();
  },
  { deep: true },
);

onBeforeUnmount(() => {
  window.removeEventListener('resize', renderChart);
  observer?.disconnect();
  chart?.dispose();
});
</script>

<style module>
.chart {
  width: 100%;
  height: 280px;
}
</style>
