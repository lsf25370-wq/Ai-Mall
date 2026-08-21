<template>
  <div class="page-container">
    <div class="profile-wrap">
      <!-- 用户信息 -->
      <div class="user-card">
        <el-avatar :size="80" :src="userStore.user?.avatar">{{ (userStore.user?.nickname || '用')[0] }}</el-avatar>
        <div class="user-info">
          <h3>{{ userStore.user?.nickname }}</h3>
          <p>用户名：{{ userStore.user?.username }}</p>
          <p>手机号：{{ userStore.user?.phone || '未绑定' }}</p>
          <el-tag size="small" :type="roleType">{{ roleText }}</el-tag>
        </div>
      </div>

      <!-- 积分卡片 -->
      <div class="points-card">
        <div class="points-num">
          <span class="label">我的积分</span>
          <b>{{ points.overview?.points ?? 0 }}</b>
        </div>
        <div class="points-mid">
          <span>会员等级</span>
          <el-tag size="small" type="warning">Lv.{{ points.overview?.level ?? 1 }}</el-tag>
          <el-progress
            v-if="points.overview?.nextLevelPoints > 0"
            :percentage="points.overview?.progressPercent || 0"
            :stroke-width="8"
            class="level-bar"
          />
          <span v-else class="max-level">已达最高等级</span>
        </div>
        <div class="points-links">
          <el-button size="small" plain type="warning" @click="$router.push('/coupons')">领券中心</el-button>
          <el-button size="small" plain type="danger" @click="$router.push('/seckill')">限时秒杀</el-button>
        </div>
      </div>

      <!-- 快捷入口 -->
      <div class="entry-grid">
        <div class="entry-item" @click="$router.push('/orders')">
          <span class="entry-icon">📦</span>
          <span>我的订单</span>
        </div>
        <div class="entry-item" @click="$router.push('/coupons')">
          <span class="entry-icon">🎟️</span>
          <span>我的优惠券</span>
        </div>
        <div class="entry-item" @click="$router.push('/favorites')">
          <span class="entry-icon">⭐</span>
          <span>我的收藏</span>
        </div>
        <div class="entry-item" @click="$router.push('/reviews')">
          <span class="entry-icon">💬</span>
          <span>我的评价</span>
        </div>
        <div class="entry-item" @click="goSeller">
          <span class="entry-icon">🏪</span>
          <span>{{ userStore.isSeller ? '卖家中心' : '申请开店' }}</span>
        </div>
        <div v-if="userStore.isAdmin" class="entry-item" @click="openAdmin">
          <span class="entry-icon">🛡️</span>
          <span>管理后台</span>
        </div>
      </div>

      <!-- 地址管理 -->
      <div class="addr-block">
        <div class="addr-head">
          <h3>收货地址</h3>
          <el-button size="small" type="primary" @click="showAdd = true">+ 新增地址</el-button>
        </div>
        <div v-for="addr in addresses" :key="addr.id" class="addr-item">
          <div class="addr-main">
            <p>
              <b>{{ addr.receiver }}</b>
              <span class="phone">{{ addr.phone }}</span>
              <el-tag v-if="addr.isDefault === 1" size="small" type="warning">默认</el-tag>
            </p>
            <p class="addr-text">{{ addr.province }}{{ addr.city }}{{ addr.district }} {{ addr.detail }}</p>
          </div>
          <el-button link type="danger" @click="remove(addr.id)">删除</el-button>
        </div>
        <el-empty v-if="!addresses.length" description="暂无地址" :image-size="60" />
      </div>
    </div>

    <el-dialog v-model="showAdd" title="新增地址" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="收货人"><el-input v-model="form.receiver" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="省份"><el-input v-model="form.province" /></el-form-item>
        <el-form-item label="城市"><el-input v-model="form.city" /></el-form-item>
        <el-form-item label="区县"><el-input v-model="form.district" /></el-form-item>
        <el-form-item label="详细地址"><el-input v-model="form.detail" /></el-form-item>
        <el-form-item><el-checkbox v-model="isDefault">设为默认地址</el-checkbox></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" @click="add">保存</el-button>
      </template>
    </el-dialog>

    <!-- 申请开店 -->
    <el-dialog v-model="showApply" title="申请开店" width="480px">
      <el-form :model="applyForm" label-width="80px">
        <el-form-item label="店铺名称"><el-input v-model="applyForm.name" placeholder="如：健身装备小店" /></el-form-item>
        <el-form-item label="店铺简介">
          <el-input v-model="applyForm.description" type="textarea" :rows="3" placeholder="介绍一下你的店铺经营内容" />
        </el-form-item>
        <el-form-item label="店铺 Logo">
          <el-input v-model="applyForm.logo" placeholder="图片地址（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showApply = false">取消</el-button>
        <el-button type="primary" :loading="applying" @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addressApi, sellerApi, pointsApi } from '../api'
import { useUserStore } from '../store/user'

const userStore = useUserStore()
const addresses = ref([])
const showAdd = ref(false)
const isDefault = ref(false)
const showApply = ref(false)
const applying = ref(false)
const applyForm = reactive({ name: '', description: '', logo: '' })
const form = reactive({ receiver: '', phone: '', province: '', city: '', district: '', detail: '' })
const points = ref({})

const roleText = computed(() =>
  userStore.user?.role === 1 ? '管理员' : userStore.user?.role === 2 ? '卖家' : '普通会员')
const roleType = computed(() =>
  userStore.user?.role === 1 ? 'danger' : userStore.user?.role === 2 ? 'warning' : 'primary')

async function load() {
  addresses.value = await addressApi.list()
  try {
    points.value = await pointsApi.overview()
  } catch {
    points.value = {}
  }
}

async function add() {
  await addressApi.add({ ...form, isDefault: isDefault.value ? 1 : 0 })
  ElMessage.success('保存成功')
  showAdd.value = false
  Object.keys(form).forEach((k) => (form[k] = ''))
  isDefault.value = false
  load()
}

async function remove(id) {
  await ElMessageBox.confirm('确认删除该地址？', '提示', { type: 'warning' })
  await addressApi.delete(id)
  ElMessage.success('已删除')
  load()
}

function openAdmin() {
  window.open('http://localhost:5175', '_blank')
}

function goSeller() {
  if (userStore.isSeller) {
    window.open('http://localhost:5176', '_blank')
  } else {
    showApply.value = true
  }
}

async function submitApply() {
  if (!applyForm.name?.trim()) {
    ElMessage.warning('请填写店铺名称')
    return
  }
  applying.value = true
  try {
    const data = await sellerApi.apply({ ...applyForm })
    await userStore.applyNewToken(data.token)
    ElMessage.success('开店申请已提交，店铺待平台审核')
    showApply.value = false
  } finally {
    applying.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.profile-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.user-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-info h3 {
  font-size: 18px;
}

.user-info p {
  color: #888;
  font-size: 13px;
  margin-top: 6px;
}

.user-info .el-tag {
  margin-top: 8px;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.points-card {
  background: linear-gradient(135deg, #ff9a3d 0%, #ff5000 100%);
  border-radius: 8px;
  padding: 20px 24px;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 32px;
}

.points-num {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.points-num .label {
  font-size: 13px;
  opacity: 0.9;
}

.points-num b {
  font-size: 32px;
  font-weight: 700;
}

.points-mid {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  flex: 1;
  max-width: 360px;
}

.level-bar {
  flex: 1;
}

.points-links {
  display: flex;
  gap: 10px;
}

.entry-item {
  background: #fff;
  border-radius: 8px;
  padding: 18px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  color: #555;
  font-size: 13px;
}

.entry-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.1);
  color: #ff5000;
}

.entry-icon {
  font-size: 26px;
}

.addr-block {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
}

.addr-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.addr-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 14px 16px;
  margin-bottom: 10px;
}

.addr-text {
  color: #666;
  font-size: 13px;
  margin-top: 6px;
}

.phone {
  color: #999;
  margin: 0 10px;
  font-weight: 400;
}

@media (max-width: 900px) {
  .entry-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
