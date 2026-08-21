import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '数据看板' } },
      { path: 'products', name: 'products', component: () => import('../views/Products.vue'), meta: { title: '商品管理' } },
      { path: 'orders', name: 'orders', component: () => import('../views/Orders.vue'), meta: { title: '订单管理' } },
      { path: 'reviews', name: 'reviews', component: () => import('../views/Reviews.vue'), meta: { title: '评价管理' } },
      { path: 'shop', name: 'shop', component: () => import('../views/Shop.vue'), meta: { title: '店铺设置' } },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (to.name !== 'login' && !token) {
    return { name: 'login' }
  }
  return true
})

export default router
