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

const defaultTrendLabels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
const defaultTrendValues = [5.2, 4.8, 6.5, 7.1, 6.7, 8.6, 9.4];

export const dashboardMetrics: DashboardMetricItem[] = [
  { label: '今日订单', value: '128', delta: '+12%', tone: 'primary' },
  { label: '今日营收', value: '¥8,240', delta: '+5.4%', tone: 'primary' },
  { label: '待发货', value: '42', delta: '需跟进', tone: 'default' },
  { label: '售后处理中', value: '5', delta: '-2%', tone: 'danger' },
  { label: '在售商品', value: '312', delta: '稳定', tone: 'success' },
  { label: '发布异常', value: '1', delta: '需修复', tone: 'danger' },
];

export const dashboardTasks: DashboardTaskItem[] = [
  {
    title: '2 个紧急订单未发货',
    detail: '超出 24 小时未处理，需要优先分配仓配资源。',
    tone: 'danger',
  },
  {
    title: '1 个退款申请待审核',
    detail: '买家已上传凭证，建议 30 分钟内完成复核。',
    tone: 'default',
  },
  {
    title: '平台模板版本待发布',
    detail: 'v1.2.3 已通过验证，可安排低峰窗口灰度。',
    tone: 'info',
  },
];

export const quickActions = [
  '新增商品',
  '发布模板',
  '导出订单',
  '同步配置',
];

export function buildDashboardTrendOption(
  labels: string[] = defaultTrendLabels,
  values: number[] = defaultTrendValues,
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

export const dashboardTrendOption = buildDashboardTrendOption();
