import type { EChartsOption } from 'echarts';

export interface DashboardMetricItem {
  label: string;
  value: string;
  delta: string;
  tone: 'primary' | 'default' | 'success' | 'danger';
}

export interface DashboardTaskItem {
  title: string;
  detail: string;
  tone: 'default' | 'danger' | 'info';
}

export const quickActions = ['新增商品', '进入发布治理', '导出订单', '同步配置'];

export function buildDashboardTrendOption(
  labels: string[] = [],
  values: number[] = [],
): EChartsOption {
  return {
    tooltip: {
      trigger: 'axis',
    },
    grid: {
      left: 0,
      right: 0,
      top: 18,
      bottom: 0,
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      axisLine: {
        show: false,
      },
      axisTick: {
        show: false,
      },
      axisLabel: {
        color: '#7c8794',
      },
      data: labels,
    },
    yAxis: {
      type: 'value',
      splitLine: {
        lineStyle: {
          color: 'rgba(9, 29, 46, 0.06)',
        },
      },
      axisLabel: {
        color: '#7c8794',
      },
    },
    series: [
      {
        type: 'line',
        smooth: true,
        data: values,
        symbol: 'circle',
        symbolSize: 10,
        lineStyle: {
          width: 4,
          color: '#ff6b00',
        },
        itemStyle: {
          color: '#ff6b00',
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(255, 107, 0, 0.28)' },
              { offset: 1, color: 'rgba(255, 107, 0, 0.02)' },
            ],
          },
        },
      },
    ],
  };
}
