<template>
  <div class="login-page">
    <div class="login-card">
      <h2 class="title">{{ isLoginMode ? '欢迎登录' : '注册账号' }}</h2>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent>
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <template v-if="!isLoginMode">
          <el-form-item prop="nickname">
            <el-input v-model="form.nickname" placeholder="昵称（可选）" clearable />
          </el-form-item>
          <el-form-item prop="phone">
            <el-input v-model="form.phone" placeholder="手机号（可选）" clearable />
          </el-form-item>
        </template>
        <el-button
          type="primary"
          size="large"
          class="submit-btn"
          :loading="submitting"
          @click="onSubmit"
        >
          {{ isLoginMode ? '登 录' : '注 册' }}
        </el-button>
      </el-form>
      <p class="switch" @click="isLoginMode = !isLoginMode">
        {{ isLoginMode ? '没有账号？去注册' : '已有账号？去登录' }}
      </p>
      <p class="hint">测试账号：zhangsan / 123456</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isLoginMode = ref(true)
const submitting = ref(false)
const formRef = ref()
const form = reactive({ username: '', password: '', nickname: '', phone: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function onSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isLoginMode.value) {
      await userStore.login({ username: form.username, password: form.password })
      ElMessage.success('登录成功')
      if (userStore.isAdmin) {
        window.open('http://localhost:5175', '_blank')
        router.push('/')
        return
      }
      router.push(route.query.redirect || '/')
    } else {
      await userStore.register({
        username: form.username,
        password: form.password,
        nickname: form.nickname,
        phone: form.phone,
      })
      ElMessage.success('注册成功，请登录')
      isLoginMode.value = true
      form.password = ''
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: calc(100vh - 130px);
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fff3ee 0%, #f5f5f5 100%);
}

.login-card {
  width: 400px;
  background: #fff;
  border-radius: 12px;
  padding: 40px 36px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
}

.title {
  text-align: center;
  margin-bottom: 28px;
  color: #333;
}

.submit-btn {
  width: 100%;
}

.switch {
  text-align: center;
  color: #ff5000;
  cursor: pointer;
  margin-top: 16px;
  font-size: 14px;
}

.hint {
  text-align: center;
  color: #bbb;
  font-size: 12px;
  margin-top: 12px;
}
</style>
