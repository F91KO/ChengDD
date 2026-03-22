export type AppMenuItem = {
  label: string;
  path: string;
  shortLabel: string;
  badge?: string;
};

export const appMenuItems: AppMenuItem[] = [
  { label: '工作台', path: '/dashboard', shortLabel: '台' },
  { label: '商品管理', path: '/products', shortLabel: '品' },
  { label: '订单管理', path: '/orders', shortLabel: '单', badge: '24' },
  { label: '售后处理', path: '/aftersales', shortLabel: '售', badge: '5' },
  { label: '配置中心', path: '/config', shortLabel: '配' },
];
