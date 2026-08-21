<template>
  <div class="page-card">
    <div class="table-toolbar">
      <el-radio-group v-model="status" @change="load">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="0">待付款</el-radio-button>
        <el-radio-button value="1">待发货</el-radio-button>
        <el-radio-button value="2">已发货</el-radio-button>
        <el-radio-button value="3">已完成</el-radio-button>
        <el-radio-button value="6">已退款</el-radio-button>
      </el-radio-group>
    </div>
    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="orderNo" label="订单号" width="200" />
      <el-table-column prop="userId" label="用户ID" width="90" />
      <el-table-column label="金额" width="120">
        <template #default="{ row }">¥{{ row.payAmount }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="下单时间" width="170">
        <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="收货地址" min-width="220">
        <template #default="{ row }">
          <span class="addr">{{ row.addressSnapshot }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="110">
        <template #default="{ row }">
          <el-button v-if="row.status === 1" size="small" type="primary" @click="ship(row)">
            发货
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager">
      <el-pagination
        layout="total, prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page="page"
        @current-change="changePage"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '../api'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const status = ref('')
const loading = ref(false)

const STATUS_MAP = { 0: '待付款', 1: '待发货', 2: '已发货', 3: '已完成', 4: '已取消', 5: '退款中', 6: '已退款' }
const statusText = (s) => STATUS_MAP[s] || '未知'
const statusType = (s) =>
  ({ 0: 'danger', 1: 'warning', 2: 'primary', 3: 'success', 4: 'info', 6: 'info' })[s] || 'info'

const fmt = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '-')

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (status.value !== '') params.status = Number(status.value)
    const data = await adminApi.orders(params)
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function changePage(p) {
  page.value = p
  load()
}

async function ship(row) {
  await adminApi.ship(row.id)
  ElMessage.success('已发货')
  load()
}

onMounted(load)
</script>

<style scoped>
.addr {
  color: #666;
  font-size: 13px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
