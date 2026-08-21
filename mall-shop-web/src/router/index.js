import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'home', component: () => import('../views/Home.vue') },
  { path: '/product/:id', name: 'product', component: () => import('../views/ProductDetail.vue') },
  { path: '/shop/:id', name: 'shop', component: () => import('../views/ShopDetail.vue') },
  { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
  {
    path: '/cart',
    name: 'cart',
    component: () => import('../views/Cart.vue'),
    meta: { auth: true },
  },
  {
    path: '/checkout',
    name: 'checkout',
    component: () => import('../views/Checkout.vue'),
    meta: { auth: true },
  },
  {
    path: '/orders',
    name: 'orders',
    component: () => import('../views/Orders.vue'),
    meta: { auth: true },
  },
  {
    path: '/order/:id',
    name: 'orderDetail',
    component: () => import('../views/OrderDetail.vue'),
    meta: { auth: true },
  },
  {
    path: '/seckill',
    name: 'seckill',
    component: () => import('../views/Seckill.vue'),
  },
  {
    path: '/coupons',
    name: 'coupons',
    component: () => import('../views/Coupons.vue'),
    meta: { auth: true },
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('../views/Profile.vue'),
    meta: { auth: true },
  },
  {
    path: '/favorites',
    name: 'favorites',
    component: () => import('../views/Favorites.vue'),
    meta: { auth: true },
  },
  {
    path: '/reviews',
    name: 'reviews',
    component: () => import('../views/MyReviews.vue'),
    meta: { auth: true },
  },
  {
    path: '/ai',
    name: 'ai',
    component: () => import('../views/AiChat.vue'),
    meta: { auth: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 登录守卫
router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (to.meta.auth && !token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
