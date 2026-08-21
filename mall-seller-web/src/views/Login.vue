<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-logo">
        <span class="logo-icon">AI</span>
        <h2>卖家中心</h2>
        <p>AI 商城商家工作台</p>
      </div>
      <el-form :model="form" label-position="top" @keyup.enter="login">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password />
        </el-form-item>
        <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="login">
          登 录
        </el-button>
        <p class="tip">登录后即可管理店铺、商品与订单</p>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '../api'

const router = useRouter()
const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function login() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const data = await userApi.login(form)
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(data.user))
    ElMessage.success('登录成功')
    router.push('/')
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f2937 0%, #111827 100%);
}

.login-card {
  width: 380px;
  background: #fff;
  border-radius: 12px;
  padding: 40px 36px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.login-logo {
  text-align: center;
  margin-bottom: 28px;
}

.logo-icon {
  display: inline-flex;
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: linear-gradient(135deg, #ff5000, #ff6a2b);
  color: #fff;
  font-size: 24px;
  font-weight: 700;
  align-items: center;
  justify-content: center;
}

.login-logo h2 {
  margin-top: 12px;
  font-size: 20px;
}

.login-logo p {
  color: #999;
  font-size: 13px;
  margin-top: 4px;
}

.login-btn {
  width: 100%;
  margin-top: 8px;
}

.tip {
  text-align: center;
  color: #aaa;
  font-size: 12px;
  margin-top: 16px;
}
</style>
