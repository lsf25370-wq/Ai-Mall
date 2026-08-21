import { defineStore } from 'pinia'
import { userApi } from '../api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null'),
  }),
  getters: {
    isLogin: (state) => !!state.token,
    isAdmin: (state) => state.user?.role === 1,
    isSeller: (state) => state.user?.role === 2,
  },
  actions: {
    async login(payload) {
      const data = await userApi.login(payload)
      this.token = data.token
      this.user = data.user
      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify(data.user))
    },
    async register(payload) {
      await userApi.register(payload)
    },
    async fetchInfo() {
      const user = await userApi.info()
      this.user = user
      localStorage.setItem('user', JSON.stringify(user))
    },
    // 开店成功后刷新角色（后端返回新 token）
    async applyNewToken(token) {
      this.token = token
      localStorage.setItem('token', token)
      await this.fetchInfo()
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    },
  },
})
