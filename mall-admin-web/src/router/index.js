import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    meta: { auth: true },
    children: [
      { path: '', name: 'dashboard', component: () => import('../views/Dashboard.vue') },
      { path: 'products', name: 'products', component: () => import('../views/Products.vue') },
      { path: 'orders', name: 'orders', component: () => import('../views/Orders.vue') },
      { path: 'users', name: 'users', component: () => import('../views/Users.vue') },
      { path: 'shops', name: 'shops', component: () => import('../views/Shops.vue') },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (to.meta.auth && !token) {
    return { name: 'login' }
  }
  return true
})

export default router
