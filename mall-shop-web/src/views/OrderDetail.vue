<template>
  <div class="page-container">
    <h2 class="page-title">订单详情</h2>
    <div v-loading="loading" class="detail-card">
      <template v-if="detail.order">
        <div class="status-bar">
          <el-tag :type="statusType(detail.order.status)" size="large">
            {{ statusText(detail.order.status) }}
          </el-tag>
          <span class="order-no">订单号：{{ detail.order.orderNo }}</span>
        </div>

        <div class="info-grid">
          <div class="info-item">
            <p class="label">收货信息</p>
            <p class="value">{{ detail.order.addressSnapshot }}</p>
          </div>
          <div class="info-item">
            <p class="label">下单时间</p>
            <p class="value">{{ formatTime(detail.order.createdAt) }}</p>
          </div>
          <div class="info-item" v-if="detail.order.payTime">
            <p class="label">支付时间</p>
            <p class="value">{{ formatTime(detail.order.payTime) }}</p>
          </div>
          <div class="info-item" v-if="detail.order.shipTime">
            <p class="label">发货时间</p>
            <p class="value">{{ formatTime(detail.order.shipTime) }}</p>
          </div>
        </div>

        <el-table :data="detail.items" class="item-table">
          <el-table-column label="商品">
            <template #default="{ row }">
              <div class="product-cell">
                <img :src="row.productImage" class="thumb" />
                <span>{{ row.productName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="price" label="单价" width="120" />
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column label="小计" width="130">
            <template #default="{ row }">
              <span class="price-red">¥{{ row.totalPrice }}</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="total-bar">
          实付金额：<span class="pay-amount">¥{{ detail.order.payAmount }}</span>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { orderApi } from '../api'

const route = useRoute()
const detail = ref({})
const loading = ref(false)

const STATUS_MAP = {
  0: '待付款', 1: '待发货', 2: '已发货', 3: '已完成', 4: '已取消', 5: '退款中', 6: '已退款',
}
const statusText = (s) => STATUS_MAP[s] || '未知'
const statusType = (s) =>
  ({ 0: 'danger', 1: 'warning', 2: 'primary', 3: 'success', 4: 'info', 6: 'info' })[s] || 'info'

function formatTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '-'
}

onMounted(async () => {
  loading.value = true
  try {
    detail.value = await orderApi.detail(route.params.id)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.detail-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
}

.status-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.order-no {
  color: #666;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  background: #fafafa;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 20px;
}

.info-item .label {
  color: #999;
  font-size: 13px;
}

.info-item .value {
  margin-top: 6px;
  color: #333;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.thumb {
  width: 52px;
  height: 52px;
  border-radius: 6px;
  object-fit: cover;
}

.price-red {
  color: #ff5000;
}

.total-bar {
  text-align: right;
  padding-top: 16px;
  font-size: 15px;
}

.pay-amount {
  color: #ff5000;
  font-size: 24px;
  font-weight: 700;
}
</style>
