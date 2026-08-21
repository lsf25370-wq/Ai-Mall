<template>
  <el-container class="layout">
    <el-aside width="200px" class="aside">
      <div class="brand">
        <span class="brand-icon">AI</span>
        <span>商城管理后台</span>
      </div>
      <el-menu :default-active="$route.path" router background-color="#001529" text-color="#b8c4ce" active-text-color="#fff">
        <el-menu-item index="/">
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
        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/shops">
          <el-icon><Shop /></el-icon>
          <span>店铺审核</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-tag size="small" type="danger">管理员</el-tag>
          <span class="admin-name">{{ nickname }}</span>
        </div>
        <div class="header-right">
          <el-button link type="primary" @click="openShop">返回商城</el-button>
          <el-button link @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { DataAnalysis, Goods, Tickets, User, Shop } from '@element-plus/icons-vue'

const router = useRouter()
const user = JSON.parse(localStorage.getItem('user') || 'null')
const nickname = computed(() => user?.nickname || user?.username || '管理员')

function openShop() {
  window.open('http://localhost:5174', '_blank')
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}

.aside {
  background: #001529;
}

.brand {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-weight: 600;
}

.brand-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: #ff5000;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
}

.aside :deep(.el-menu) {
  border-right: none;
}

.header {
  background: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-name {
  font-weight: 500;
}

.main {
  padding: 20px;
}
</style>
