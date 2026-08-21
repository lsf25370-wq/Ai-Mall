<template>
  <div class="page-container">
    <div v-loading="loading">
      <!-- 无店铺 -->
      <div v-if="!shop" class="empty-shop">
        <div class="empty-icon">🏪</div>
        <h3>您还没有店铺</h3>
        <p>申请开店后即可管理商品、订单，开启电商之旅</p>
        <el-button type="primary" size="large" @click="showApply = true">申请开店</el-button>
      </div>

      <!-- 有店铺 -->
      <div v-else class="shop-card">
        <div class="shop-head">
          <img :src="shop.logo || defaultLogo" class="shop-logo" />
          <div>
            <h2>{{ shop.name }}</h2>
            <el-tag :type="statusType(shop.status)" size="small" effect="dark" style="margin-top: 6px">
              {{ statusText(shop.status) }}
            </el-tag>
          </div>
        </div>
        <el-form :model="form" label-width="90px" style="margin-top: 20px">
          <el-form-item label="店铺名称">
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item label="店铺 Logo">
            <el-input v-model="form.logo" placeholder="图片链接" />
          </el-form-item>
          <el-form-item label="店铺简介">
            <el-input v-model="form.description" type="textarea" :rows="4" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="save">保存修改</el-button>
            <el-button @click="preview">预览店铺</el-button>
          </el-form-item>
        </el-form>
        <el-divider />
        <div class="tips">
          <h4>经营提示</h4>
          <ul>
            <li>店铺审核通过后正式营业，买家可在商城搜索到您的商品</li>
            <li>买家下单并付款后，请在「订单管理」中及时发货</li>
            <li>积极回复买家评价，提升店铺口碑</li>
          </ul>
        </div>
      </div>
    </div>

    <!-- 申请开店 -->
    <el-dialog v-model="showApply" title="申请开店" width="480px">
      <el-form :model="applyForm" label-width="80px">
        <el-form-item label="店铺名称"><el-input v-model="applyForm.name" /></el-form-item>
        <el-form-item label="店铺简介">
          <el-input v-model="applyForm.description" type="textarea" :rows="3" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { shopApi } from '../api'

const shop = ref(null)
const loading = ref(false)
const saving = ref(false)
const showApply = ref(false)
const applying = ref(false)
const form = reactive({ name: '', logo: '', description: '' })
const applyForm = reactive({ name: '', description: '', logo: '' })
const defaultLogo =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48"><rect width="48" height="48" rx="8" fill="%23ff5000"/><text x="24" y="30" font-size="18" fill="white" text-anchor="middle" font-weight="bold">店</text></svg>'

const statusText = (s) => ({ 0: '待审核', 1: '营业中', 2: '已停业' })[s] || '未知'
const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'info' })[s] || 'info'

async function load() {
  loading.value = true
  try {
    shop.value = await shopApi.myShop()
    if (shop.value) {
      Object.assign(form, { name: shop.value.name, logo: shop.value.logo || '', description: shop.value.description || '' })
    }
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!form.name?.trim()) {
    ElMessage.warning('店铺名称不能为空')
    return
  }
  saving.value = true
  try {
    shop.value = await shopApi.update({ ...form })
    ElMessage.success('店铺信息已保存')
  } finally {
    saving.value = false
  }
}

function preview() {
  window.open(`http://localhost:5173/shop/${shop.value.id}`, '_blank')
}

async function submitApply() {
  if (!applyForm.name?.trim()) {
    ElMessage.warning('请填写店铺名称')
    return
  }
  applying.value = true
  try {
    const data = await shopApi.apply({ ...applyForm })
    localStorage.setItem('token', data.token)
    ElMessage.success('开店申请已提交，等待平台审核')
    showApply.value = false
    load()
  } finally {
    applying.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.empty-shop {
  background: #fff;
  border-radius: 12px;
  padding: 60px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 56px;
}

.empty-shop h3 {
  margin-top: 16px;
  font-size: 18px;
}

.empty-shop p {
  color: #999;
  font-size: 13px;
  margin: 8px 0 20px;
}

.shop-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  max-width: 720px;
}

.shop-head {
  display: flex;
  align-items: center;
  gap: 16px;
}

.shop-logo {
  width: 72px;
  height: 72px;
  border-radius: 12px;
  object-fit: cover;
  background: #f5f5f5;
}

.shop-head h2 {
  font-size: 20px;
}

.tips {
  background: #fafafa;
  border-radius: 8px;
  padding: 14px 18px;
}

.tips h4 {
  font-size: 14px;
  margin-bottom: 8px;
}

.tips li {
  color: #666;
  font-size: 13px;
  margin: 6px 0 0 16px;
}
</style>
