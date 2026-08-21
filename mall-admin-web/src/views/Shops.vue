<template>
  <div class="page-container">
    <div class="toolbar">
      <el-radio-group v-model="status" @change="load()">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button :value="0">待审核</el-radio-button>
        <el-radio-button :value="1">营业中</el-radio-button>
        <el-radio-button :value="2">已停业</el-radio-button>
      </el-radio-group>
    </div>

    <el-table v-loading="loading" :data="shops" stripe>
      <el-table-column label="店铺" min-width="240">
        <template #default="{ row }">
          <div class="shop-cell">
            <img :src="row.logo || defaultLogo" class="shop-logo" />
            <div>
              <p class="shop-name">{{ row.name }}</p>
              <p class="shop-desc">{{ row.description || '—' }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="店主ID" prop="ownerUserId" width="90" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small" effect="dark">
            {{ statusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" width="150">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 0"
            size="small"
            type="success"
            @click="audit(row, 1)"
          >
            审核通过
          </el-button>
          <el-button
            v-if="row.status === 1"
            size="small"
            type="warning"
            plain
            @click="audit(row, 2)"
          >
            停业
          </el-button>
          <el-button
            v-if="row.status === 2"
            size="small"
            type="primary"
            plain
            @click="audit(row, 1)"
          >
            恢复营业
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        layout="prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page="page"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../api'

const shops = ref([])
const status = ref('all')
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const defaultLogo =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48"><rect width="48" height="48" rx="8" fill="%23ff5000"/><text x="24" y="30" font-size="18" fill="white" text-anchor="middle" font-weight="bold">店</text></svg>'

const statusText = (s) => ({ 0: '待审核', 1: '营业中', 2: '已停业' })[s] || '未知'
const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'info' })[s] || 'info'

function formatTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : ''
}

async function load(p = page.value) {
  loading.value = true
  try {
    const params = { page: p, size: size.value }
    if (status.value !== 'all') params.status = Number(status.value)
    const data = await adminApi.shops(params)
    shops.value = data.records
    total.value = data.total
    page.value = data.current
  } finally {
    loading.value = false
  }
}

async function audit(row, target) {
  const action = target === 1 ? '审核通过' : target === 2 ? '停业' : '恢复营业'
  await ElMessageBox.confirm(`确认对店铺「${row.name}」执行【${action}】操作？`, '提示', { type: 'warning' })
  await adminApi.updateShopStatus(row.id, target)
  ElMessage.success('操作成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  margin-bottom: 14px;
}

.shop-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.shop-logo {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  object-fit: cover;
  background: #f5f5f5;
  flex-shrink: 0;
}

.shop-name {
  font-weight: 600;
  color: #333;
}

.shop-desc {
  color: #999;
  font-size: 12px;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
