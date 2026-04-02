import { createRouter, createWebHistory } from 'vue-router';
import { appMenuItems } from '@/app/menu';
import { useAuthStore } from '@/stores/auth';

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/pages/LoginPage.vue'),
    meta: {
      public: true,
      title: '登录',
    },
  },
  {
    path: '/',
    redirect: '/dashboard',
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('@/pages/DashboardPage.vue'),
    meta: {
      title: '工作台',
      requiredModule: 'store',
      requiredAction: 'view',
    },
  },
  {
    path: '/products',
    name: 'products',
    component: () => import('@/pages/ProductsPage.vue'),
    meta: {
      title: '商品管理',
      requiredModule: 'product',
      requiredAction: 'view',
    },
  },
  {
    path: '/categories',
    name: 'categories',
    component: () => import('@/pages/CategoryManagementPage.vue'),
    meta: {
      title: '商品分类',
      requiredModule: 'product',
      requiredAction: 'view',
    },
  },
  {
    path: '/permissions',
    name: 'permissions',
    component: () => import('@/pages/PermissionManagementPage.vue'),
    meta: {
      title: '权限配置',
      ownerOnly: true,
    },
  },
  {
    path: '/platform/category-templates',
    name: 'platform-category-templates',
    component: () => import('@/pages/CategoryTemplatesPage.vue'),
    meta: {
      title: '分类模板',
      consoleTitle: 'ChengDD 平台后台',
    },
  },
  {
    path: '/orders',
    name: 'orders',
    component: () => import('@/pages/OrdersPage.vue'),
    meta: {
      title: '订单管理',
      requiredModule: 'order',
      requiredAction: 'view',
    },
  },
  {
    path: '/aftersales',
    name: 'aftersales',
    component: () => import('@/pages/AftersalesPage.vue'),
    meta: {
      title: '售后处理',
      requiredModule: 'order',
      requiredAction: 'view',
    },
  },
  {
    path: '/config',
    name: 'config',
    component: () => import('@/pages/ConfigCenterPage.vue'),
    meta: {
      title: '配置中心',
      requiredModule: 'config',
      requiredAction: 'view',
    },
  },
  {
    path: '/releases',
    name: 'releases',
    component: () => import('@/pages/ReleaseGovernancePage.vue'),
    meta: {
      title: '发布治理',
      requiredModule: 'release',
      requiredAction: 'view',
    },
  },
];

export const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 };
  },
});

router.beforeEach((to) => {
  const authStore = useAuthStore();
  authStore.hydrate();

  if (to.meta.public) {
    if ((authStore.isAuthenticated || authStore.authenticating) && to.path === '/login') {
      return '/dashboard';
    }

    return true;
  }

  if (!authStore.isAuthenticated && !authStore.authenticating) {
    return '/login';
  }

  if (!authStore.canAccess(to.meta)) {
    const fallbackPath = appMenuItems.find((item) => authStore.canAccess(item))?.path || '/login';
    return to.path === fallbackPath ? '/login' : fallbackPath;
  }

  void authStore.ensureCurrentContext();
  return true;
});

router.afterEach((to) => {
  const title = typeof to.meta.title === 'string' ? to.meta.title : '商家后台';
  const consoleTitle = typeof to.meta.consoleTitle === 'string' ? to.meta.consoleTitle : 'ChengDD 商家后台';
  document.title = `${title} | ${consoleTitle}`;
});
