<template>
  <div class="layout">
    <header class="top-nav">
      <div class="nav-inner">
        <router-link to="/" class="logo">
          <span class="logo-icon">AI</span>
          <span class="logo-text">AI商城</span>
        </router-link>
        <div class="search-box">
          <el-input
            v-model="keyword"
            placeholder="搜索商品"
            clearable
            @keyup.enter="doSearch"
          >
            <template #append>
              <el-button :icon="Search" @click="doSearch" />
            </template>
          </el-input>
        </div>
        <nav class="nav-links">
          <router-link to="/" class="nav-link">首页</router-link>
          <router-link to="/seckill" class="nav-link seckill-entry">秒杀</router-link>
          <router-link to="/coupons" class="nav-link">领券</router-link>
          <router-link to="/ai" class="nav-link ai-entry">
            <el-badge is-dot type="danger">AI 客服</el-badge>
          </router-link>
          <router-link to="/orders" class="nav-link">我的订单</router-link>
          <router-link to="/cart" class="nav-link">
            购物车
          </router-link>
          <template v-if="userStore.isLogin">
            <el-dropdown @command="onUserCommand">
              <span class="user-name">{{ userStore.user?.nickname || userStore.user?.username }}</span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="favorites">我的收藏</el-dropdown-item>
                  <el-dropdown-item command="reviews">我的评价</el-dropdown-item>
                  <el-dropdown-item v-if="userStore.isSeller" command="seller" divided>卖家中心</el-dropdown-item>
                  <el-dropdown-item v-if="userStore.isAdmin" command="admin" divided>管理后台</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <router-link v-else to="/login" class="nav-link login-btn">登录/注册</router-link>
        </nav>
      </div>
    </header>

    <main class="main-content">
      <router-view />
    </main>

    <footer class="footer">
      <p>AI 商城 · 技术栈：Spring Boot + Vue3 + LangChain + DeepSeek</p>
    </footer>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { useUserStore } from './store/user'

const router = useRouter()
const userStore = useUserStore()
const keyword = ref('')

function doSearch() {
  if (keyword.value.trim()) {
    router.push({ path: '/', query: { keyword: keyword.value.trim() } })
  }
}

function onUserCommand(command) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/')
  } else if (command === 'admin') {
    window.open('http://localhost:5175', '_blank')
  } else if (command === 'seller') {
    window.open('http://localhost:5176', '_blank')
  } else {
    router.push('/' + command)
  }
}
</script>

<style scoped>
.top-nav {
  background: linear-gradient(90deg, #ff5000 0%, #ff6a2b 100%);
  padding: 10px 0;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.nav-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 24px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #fff;
}

.logo-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #fff;
  color: #ff5000;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 1px;
}

.search-box {
  flex: 1;
  max-width: 460px;
}

.search-box :deep(.el-input__wrapper) {
  border-radius: 4px 0 0 4px;
}

.search-box :deep(.el-input-group__append) {
  background: #fff;
  color: #ff5000;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-left: auto;
  color: #fff;
  font-size: 14px;
}

.nav-link {
  color: #fff;
  transition: opacity 0.2s;
}

.nav-link:hover {
  opacity: 0.85;
}

.ai-entry :deep(.el-badge__content) {
  background: #ffd700;
}

.seckill-entry {
  color: #ffd700;
  font-weight: 700;
}

.user-name {
  cursor: pointer;
  color: #fff;
  font-weight: 500;
}

.main-content {
  min-height: calc(100vh - 130px);
}

.footer {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 13px;
  background: #fff;
  margin-top: 20px;
}
</style>
