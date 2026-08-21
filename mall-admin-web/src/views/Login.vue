<template>
  <div class="login-page">
    <div class="login-card">
      <h2>AI 商城管理后台</h2>
      <p class="sub">管理员登录</p>
      <el-form ref="formRef" :model="form" size="large" @submit.prevent>
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-button type="primary" size="large" class="submit" :loading="loading" @click="login">
          登 录
        </el-button>
      </el-form>
      <p class="hint">管理员账号：admin / 123456</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '../api'

const router = useRouter()
const loading = ref(false)
const formRef = ref()
const form = reactive({ username: 'admin', password: '' })

async function login() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const data = await userApi.login({ username: form.username, password: form.password })
    if (data.user?.role !== 1) {
      ElMessage.error('该账号不是管理员')
      return
    }
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(data.user))
    ElMessage.success('登录成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #001529 0%, #0d2c4d 100%);
}

.login-card {
  width: 380px;
  background: #fff;
  border-radius: 12px;
  padding: 40px 36px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

h2 {
  text-align: center;
  color: #001529;
}

.sub {
  text-align: center;
  color: #999;
  margin: 8px 0 24px;
}

.submit {
  width: 100%;
}

.hint {
  text-align: center;
  color: #bbb;
  font-size: 12px;
  margin-top: 14px;
}
</style>
