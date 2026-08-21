import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const http = axios.create({
  baseURL: '/api',
  timeout: 60000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

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

// ===== 登录 =====
export const userApi = {
  login: (data) => http.post('/user/login', data),
  info: () => http.get('/user/info'),
}

// ===== 店铺 =====
export const shopApi = {
  myShop: () => http.get('/seller/shop'),
  apply: (data) => http.post('/seller/shop', data),
  update: (data) => http.put('/seller/shop', data),
}

// ===== 商品 =====
export const productApi = {
  categories: () => http.get('/category/list'),
  list: (params) => http.get('/seller/product/list', { params }),
  create: (data) => http.post('/seller/product', data),
  update: (id, data) => http.put(`/seller/product/${id}`, data),
  updateStatus: (id, status) => http.put(`/seller/product/${id}/status`, null, { params: { status } }),
  remove: (id) => http.delete(`/seller/product/${id}`),
}

// ===== 订单 =====
export const orderApi = {
  list: (params) => http.get('/seller/order/list', { params }),
  ship: (id) => http.post(`/seller/order/${id}/ship`),
  approveRefund: (id) => http.post(`/seller/order/${id}/refund/approve`),
  rejectRefund: (id) => http.post(`/seller/order/${id}/refund/reject`),
}

// ===== 数据统计 =====
export const statsApi = {
  overview: () => http.get('/seller/stats'),
  aiAnalysis: () => http.get('/seller/stats/ai-analysis'),
}

// ===== 评价 =====
export const reviewApi = {
  list: () => http.get('/seller/review/list'),
  reply: (id, reply) => http.post(`/seller/review/${id}/reply`, { reply }),
  aiReply: (id) => http.post(`/seller/review/${id}/ai-reply`),
}
