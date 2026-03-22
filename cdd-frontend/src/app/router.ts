import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { loadProducts } from '@/modules/products/mock';
import { loadOrders } from '@/modules/orders/mock';

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
    },
  },
  {
    path: '/products',
    name: 'products',
    component: () => import('@/pages/ProductsPage.vue'),
    meta: {
      title: '商品管理',
    },
  },
  {
    path: '/orders',
    name: 'orders',
    component: () => import('@/pages/OrdersPage.vue'),
    meta: {
      title: '订单管理',
    },
  },
  {
    path: '/aftersales',
    name: 'aftersales',
    component: () => import('@/pages/AftersalesPage.vue'),
    meta: {
      title: '售后处理',
    },
  },
  {
    path: '/config',
    name: 'config',
    component: () => import('@/pages/ConfigCenterPage.vue'),
    meta: {
      title: '配置中心',
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

  void authStore.ensureCurrentContext();
  return true;
});

router.afterEach((to) => {
  const title = typeof to.meta.title === 'string' ? to.meta.title : '商家后台';
  document.title = `${title} | ChengDD 商家后台`;

  if (to.name === 'products') {
    void loadProducts();
    return;
  }
  if (to.name === 'orders') {
    void loadOrders();
    return;
  }
  if (to.name === 'dashboard') {
    void Promise.all([loadProducts(), loadOrders()]);
  }
});
