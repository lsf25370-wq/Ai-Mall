import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const http = axios.create({
  baseURL: '/api',
  timeout: 30000,
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
    ElMessage.error(error.response?.data?.message || '网络异常')
    return Promise.reject(error)
  },
)

export const userApi = {
  login: (data) => http.post('/user/login', data),
}

export const adminApi = {
  overview: () => http.get('/admin/overview'),
  products: (params) => http.get('/admin/products', { params }),
  updateProductStatus: (id, status) => http.put(`/admin/product/${id}/status`, null, { params: { status } }),
  orders: (params) => http.get('/admin/orders', { params }),
  ship: (id) => http.post(`/admin/order/${id}/ship`),
  users: (params) => http.get('/admin/users', { params }),
  shops: (params) => http.get('/admin/shops', { params }),
  updateShopStatus: (id, status) => http.put(`/admin/shop/${id}/status`, null, { params: { status } }),
}
