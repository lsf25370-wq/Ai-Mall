import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const http = axios.create({
  baseURL: '/api',
  timeout: 60000,
})

// 请求拦截：携带 token
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：统一错误处理
http.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    }
    if (res.code === 401) {
      localStorage.removeItem('token')
      router.push('/login')
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message))
  },
  (error) => {
    ElMessage.error(error.response?.data?.message || '网络异常，请稍后重试')
    return Promise.reject(error)
  },
)

// ===== 用户 =====
export const userApi = {
  register: (data) => http.post('/user/register', data),
  login: (data) => http.post('/user/login', data),
  info: () => http.get('/user/info'),
}

// ===== 商品 =====
export const productApi = {
  categories: () => http.get('/category/list'),
  list: (params) => http.get('/product/list', { params }),
  detail: (id) => http.get(`/product/${id}`),
}

// ===== 购物车 =====
export const cartApi = {
  add: (data) => http.post('/cart/add', data),
  list: () => http.get('/cart/list'),
  updateQuantity: (id, quantity) => http.put(`/cart/quantity/${id}`, null, { params: { quantity } }),
  updateChecked: (id, checked) => http.put(`/cart/checked/${id}`, null, { params: { checked } }),
  remove: (id) => http.delete(`/cart/${id}`),
}

// ===== 地址 =====
export const addressApi = {
  list: () => http.get('/address/list'),
  add: (data) => http.post('/address/add', data),
  delete: (id) => http.delete(`/address/${id}`),
}

// ===== 订单 =====
export const orderApi = {
  create: (data) => http.post('/order/create', data),
  pay: (id) => http.post(`/order/pay/${id}`),
  cancel: (id) => http.post(`/order/cancel/${id}`),
  confirm: (id) => http.post(`/order/confirm/${id}`),
  refund: (id) => http.post(`/order/refund/${id}`),
  list: (params) => http.get('/order/list', { params }),
  detail: (id) => http.get(`/order/${id}`),
}

// ===== AI 客服 =====
export const aiApi = {
  createSession: () => http.post('/ai/session/create'),
  listSessions: () => http.get('/ai/session/list'),
  history: (sessionId) => http.get('/ai/history', { params: { sessionId } }),
  chat: (data) => http.post('/ai/chat', data),
}

// ===== 店铺（公开）=====
export const shopApi = {
  detail: (id) => http.get(`/shop/${id}`),
  products: (id, params) => http.get(`/shop/${id}/products`, { params }),
}

// ===== 评价 =====
export const reviewApi = {
  create: (data) => http.post('/review/create', data),
  byProduct: (productId) => http.get(`/review/product/${productId}`),
  mine: () => http.get('/review/mine'),
}

// ===== 收藏 =====
export const favoriteApi = {
  list: () => http.get('/favorite/list'),
  add: (productId) => http.post('/favorite/add', { productId }),
  remove: (productId) => http.delete('/favorite/remove', { params: { productId } }),
  check: (productId) => http.get('/favorite/check', { params: { productId } }),
}

// ===== 卖家中心（开店/店铺信息）=====
export const sellerApi = {
  myShop: () => http.get('/seller/shop'),
  apply: (data) => http.post('/seller/shop', data),
}

// ===== 秒杀 =====
export const seckillApi = {
  list: () => http.get('/seckill/list'),
  detail: (id) => http.get(`/seckill/${id}`),
  buy: (id, addressId) => http.post(`/seckill/${id}/buy`, null, { params: { addressId } }),
}

// ===== 优惠券 =====
export const couponApi = {
  available: () => http.get('/coupon/available'),
  claim: (id) => http.post(`/coupon/${id}/claim`),
  my: () => http.get('/coupon/my'),
}

// ===== 积分 =====
export const pointsApi = {
  overview: () => http.get('/points/overview'),
  logs: () => http.get('/points/logs'),
}
