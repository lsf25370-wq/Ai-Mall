<template>
  <div class="page-container">
    <div class="toolbar">
      <el-radio-group v-model="status" @change="load()">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button :value="1">待发货</el-radio-button>
        <el-radio-button :value="2">已发货</el-radio-button>
        <el-radio-button :value="3">已完成</el-radio-button>
        <el-radio-button :value="5">退款处理</el-radio-button>
      </el-radio-group>
    </div>

    <div v-loading="loading" class="order-list">
      <div v-for="item in orders" :key="item.order.id" class="order-card">
        <div class="order-head">
          <span class="order-no">订单号：{{ item.order.orderNo }}</span>
          <el-tag :type="statusType(item.order.status)" size="small">
            {{ statusText(item.order.status) }}
          </el-tag>
        </div>
        <div class="order-body">
          <div v-for="oi in item.items" :key="oi.id" class="item-row">
            <img :src="oi.productImage" class="item-img" />
            <div class="item-info">
              <p class="item-name">{{ oi.productName }}</p>
              <p class="item-price">¥{{ oi.price }} × {{ oi.quantity }}</p>
            </div>
          </div>
          <div class="order-amount">
            <p class="amount">¥{{ item.order.payAmount }}</p>
            <p class="addr" :title="item.order.addressSnapshot">{{ item.order.addressSnapshot }}</p>
          </div>
        </div>
        <div class="order-actions">
          <el-button
            v-if="item.order.status === 1"
            size="small"
            type="primary"
            @click="ship(item.order.id)"
          >
            立即发货
          </el-button>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && orders.length === 0" description="暂无相关订单" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi } from '../api'

const status = ref('all')
const orders = ref([])
const loading = ref(false)

const STATUS_MAP = { 0: '待付款', 1: '待发货', 2: '已发货', 3: '已完成', 4: '已取消', 5: '退款中', 6: '已退款' }
const statusText = (s) => STATUS_MAP[s] || '未知'
const statusType = (s) =>
  ({ 0: 'danger', 1: 'warning', 2: 'primary', 3: 'success', 4: 'info', 6: 'info' })[s] || 'info'

async function load() {
  loading.value = true
  try {
    const params = status.value === 'all' ? {} : { status: Number(status.value) }
    orders.value = await orderApi.list(params)
  } finally {
    loading.value = false
  }
}

async function ship(id) {
  await ElMessageBox.confirm('确认该订单已发货？', '发货确认', { type: 'warning' })
  await orderApi.ship(id)
  ElMessage.success('发货成功')
  load()
}

async function approveRefund(id) {
  await ElMessageBox.confirm('确认同意退款？退款成功后商品库存将自动恢复。', '退款审批', { type: 'warning' })
  await orderApi.approveRefund(id)
  ElMessage.success('退款已通过')
  load()
}

async function rejectRefund(id) {
  await ElMessageBox.confirm('确认拒绝该退款申请？', '退款审批', { type: 'warning' })
  await orderApi.rejectRefund(id)
  ElMessage.success('已拒绝退款申请')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  margin-bottom: 14px;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-card {
  background: #fff;
  border-radius: 8px;
  padding: 14px 18px;
}

.order-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f5f5f5;
  padding-bottom: 10px;
  color: #666;
  font-size: 13px;
}

.order-body {
  padding: 10px 0;
  display: flex;
  align-items: center;
  gap: 16px;
}

.item-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  max-width: 420px;
}

.item-img {
  width: 52px;
  height: 52px;
  border-radius: 6px;
  object-fit: cover;
  background: #f5f5f5;
  flex-shrink: 0;
}

.item-name {
  font-size: 13px;
  color: #333;
}

.item-price {
  color: #999;
  font-size: 12px;
  margin-top: 4px;
}

.order-amount {
  margin-left: auto;
  text-align: right;
  max-width: 260px;
}

.amount {
  color: #ff5000;
  font-weight: 700;
  font-size: 16px;
}

.addr {
  color: #999;
  font-size: 11px;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #f5f5f5;
  padding-top: 10px;
}
</style>
