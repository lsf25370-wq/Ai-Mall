<template>
  <div class="layout">
    <!-- 千牛风格深色侧边栏 -->
    <aside class="sidebar">
      <div class="side-logo">
        <span class="logo-icon">AI</span>
        <span>卖家中心</span>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#1f2937"
        text-color="#c9d1d9"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据看板</span>
        </el-menu-item>
        <el-menu-item index="/products">
          <el-icon><Goods /></el-icon>
          <span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/orders">
          <el-icon><Tickets /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/reviews">
          <el-icon><ChatDotRound /></el-icon>
          <span>评价管理</span>
        </el-menu-item>
        <el-menu-item index="/shop">
          <el-icon><Shop /></el-icon>
          <span>店铺设置</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <!-- 主区域 -->
    <div class="main">
      <header class="topbar">
        <div class="page-title">{{ $route.meta.title || '卖家中心' }}</div>
        <div class="topbar-right">
          <el-tag v-if="shop && shop.status === 1" type="success" size="small" effect="dark">营业中</el-tag>
          <el-tag v-else-if="shop && shop.status === 0" type="warning" size="small" effect="dark">待审核</el-tag>
          <span class="user-name">{{ userStore.user?.nickname || '卖家' }}</span>
          <el-dropdown @command="onCommand">
            <el-button size="small" text>···</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="shop">我的店铺</el-dropdown-item>
                <el-dropdown-item command="mall">返回商城</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { DataAnalysis, Goods, Tickets, ChatDotRound, Shop } from '@element-plus/icons-vue'
import { shopApi } from '../api'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()
const shop = ref(null)

onMounted(async () => {
  try {
    shop.value = await shopApi.myShop()
  } catch {
    /* 无店铺时忽略 */
  }
})

function onCommand(command) {
  if (command === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    router.push('/login')
  } else if (command === 'mall') {
    window.open('http://localhost:5173', '_blank')
  } else if (command === 'shop') {
    router.push('/shop')
  }
}
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
}

.sidebar {
  width: 200px;
  background: #1f2937;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.side-logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  background: #111827;
}

.logo-icon {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: linear-gradient(135deg, #ff5000, #ff6a2b);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.sidebar :deep(.el-menu) {
  border-right: none;
  flex: 1;
}

.sidebar :deep(.el-menu-item) {
  height: 48px;
}

.sidebar :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, #ff5000, #ff6a2b) !important;
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.topbar {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  font-size: 14px;
  color: #374151;
  font-weight: 500;
}

.content {
  flex: 1;
  overflow-y: auto;
  background: #f4f6f8;
}
</style>
