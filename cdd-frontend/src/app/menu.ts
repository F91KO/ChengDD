export type AppMenuItem = {
  label: string;
  path: string;
  shortLabel: string;
  badge?: string;
  requiredModule?: 'store' | 'product' | 'order' | 'release' | 'config';
  requiredAction?: 'view' | 'edit' | 'publish' | 'export';
  ownerOnly?: boolean;
};

export const appMenuItems: AppMenuItem[] = [
  { label: '工作台', path: '/dashboard', shortLabel: '台', requiredModule: 'store', requiredAction: 'view' },
  { label: '商品管理', path: '/products', shortLabel: '品', requiredModule: 'product', requiredAction: 'view' },
  { label: '商品分类', path: '/categories', shortLabel: '类', requiredModule: 'product', requiredAction: 'view' },
  { label: '权限配置', path: '/permissions', shortLabel: '权', ownerOnly: true },
  { label: '订单管理', path: '/orders', shortLabel: '单', badge: '24', requiredModule: 'order', requiredAction: 'view' },
  { label: '售后处理', path: '/aftersales', shortLabel: '售', badge: '5', requiredModule: 'order', requiredAction: 'view' },
  { label: '发布治理', path: '/releases', shortLabel: '发', requiredModule: 'release', requiredAction: 'view' },
  { label: '配置中心', path: '/config', shortLabel: '配', requiredModule: 'config', requiredAction: 'view' },
];

export const platformMenuItems: AppMenuItem[] = [
  { label: '分类模板', path: '/platform/category-templates', shortLabel: '模' },
];
