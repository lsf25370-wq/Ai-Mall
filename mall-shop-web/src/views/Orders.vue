<template>
  <div class="page-container">
    <h2 class="page-title">我的订单</h2>
    <el-tabs v-model="activeStatus" @tab-change="load">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="待付款" name="0" />
      <el-tab-pane label="待发货" name="1" />
      <el-tab-pane label="已发货" name="2" />
      <el-tab-pane label="已完成" name="3" />
      <el-tab-pane label="已取消" name="4" />
    </el-tabs>

    <div v-loading="loading" class="order-list">
      <div v-for="item in orders" :key="item.order.id" class="order-card">
        <div class="order-head">
          <span class="order-no">订单号：{{ item.order.orderNo }}</span>
          <el-tag :type="statusType(item.order.status)" size="small">
            {{ statusText(item.order.status) }}
          </el-tag>
        </div>
        <div class="order-body" @click="$router.push(`/order/${item.order.id}`)">
          <img
            v-for="oi in item.items.slice(0, 4)"
            :key="oi.id"
            :src="oi.productImage"
            class="item-img"
          />
          <div class="order-amount">
            <p class="amount">¥{{ item.order.payAmount }}</p>
            <p class="date">{{ formatTime(item.order.createdAt) }}</p>
          </div>
        </div>
        <div class="order-actions">
          <el-button
            v-if="item.order.status === 0"
            size="small"
            type="danger"
            @click="pay(item.order.id)"
          >
            去支付
          </el-button>
          <el-button v-if="item.order.status === 0" size="small" @click="cancel(item.order.id)">
            取消订单
          </el-button>
          <el-button
            v-if="item.order.status === 1 || item.order.status === 2"
            size="small"
            @click="refund(item.order.id)"
          >
            申请退款
          </el-button>
          <el-button
            v-if="item.order.status === 2 || item.order.status === 3"
            size="small"
            type="primary"
            plain
            @click="openReview(item)"
          >
            评价
          </el-button>
          <el-button size="small" @click="$router.push(`/order/${item.order.id}`)">查看详情</el-button>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && orders.length === 0" description="暂无订单" />

    <!-- 评价弹窗 -->
    <el-dialog v-model="reviewVisible" title="发表评价" width="520px">
      <div v-for="oi in reviewItems" :key="oi.id" class="review-item">
        <img :src="oi.productImage" class="rev-img" />
        <div class="rev-main">
          <p class="rev-name">{{ oi.productName }}</p>
          <div class="rev-rate">
            <span>评分：</span>
            <el-rate v-model="rating" />
          </div>
          <el-input
            v-model="content"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="说说商品的使用感受吧~"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi, reviewApi } from '../api'

const activeStatus = ref('all')
const orders = ref([])
const loading = ref(false)
const reviewVisible = ref(false)
const reviewItems = ref([])
const rating = ref(5)
const content = ref('')

const STATUS_MAP = {
  0: '待付款', 1: '待发货', 2: '已发货', 3: '已完成', 4: '已取消', 5: '退款中', 6: '已退款',
}
const statusText = (s) => STATUS_MAP[s] || '未知'
const statusType = (s) =>
  ({ 0: 'danger', 1: 'warning', 2: 'primary', 3: 'success', 4: 'info', 6: 'info' })[s] || 'info'

function formatTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : ''
}

async function load() {
  loading.value = true
  try {
    const params = activeStatus.value === 'all' ? {} : { status: Number(activeStatus.value) }
    orders.value = await orderApi.list(params)
  } finally {
    loading.value = false
  }
}

async function pay(id) {
  await ElMessageBox.confirm('确认支付该订单？', '模拟支付', { type: 'warning' })
  await orderApi.pay(id)
  ElMessage.success('支付成功')
  load()
}

async function cancel(id) {
  await ElMessageBox.confirm('确认取消该订单？', '提示', { type: 'warning' })
  await orderApi.cancel(id)
  ElMessage.success('订单已取消')
  load()
}

async function refund(id) {
  await ElMessageBox.confirm('确认申请退款？提交后需等待卖家审核。', '申请退款', { type: 'warning' })
  await orderApi.refund(id)
  ElMessage.success('退款申请已提交，等待卖家处理')
  load()
}

async function confirm(id) {
  await ElMessageBox.confirm('确认已收到货物？', '确认收货', { type: 'warning' })
  await orderApi.confirm(id)
  ElMessage.success('确认收货成功')
  load()
}

function openReview(item) {
  reviewItems.value = item.items
  rating.value = 5
  content.value = ''
  reviewVisible.value = true
}

async function submitReview() {
  for (const oi of reviewItems.value) {
    await reviewApi.create({
      orderId: oi.orderId,
      orderItemId: oi.id,
      rating: rating.value,
      content: content.value || '此用户没有填写评价内容',
    })
  }
  ElMessage.success('评价成功，感谢您的反馈')
  reviewVisible.value = false
  load()
}

onMounted(load)
</script>

<style scoped>
.order-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
}

.order-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f5f5f5;
  padding-bottom: 12px;
  color: #666;
  font-size: 13px;
}

.order-body {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 0;
  cursor: pointer;
}

.item-img {
  width: 64px;
  height: 64px;
  border-radius: 6px;
  object-fit: cover;
  background: #f5f5f5;
}

.order-amount {
  margin-left: auto;
  text-align: right;
}

.amount {
  color: #ff5000;
  font-weight: 700;
  font-size: 16px;
}

.date {
  color: #999;
  font-size: 12px;
  margin-top: 4px;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid #f5f5f5;
  padding-top: 12px;
}

.review-item {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f7f7f7;
}

.review-item:last-child {
  border-bottom: none;
}

.rev-img {
  width: 56px;
  height: 56px;
  border-radius: 6px;
  object-fit: cover;
  background: #f5f5f5;
  flex-shrink: 0;
}

.rev-main {
  flex: 1;
}

.rev-name {
  font-weight: 500;
  color: #333;
  font-size: 14px;
}

.rev-rate {
  display: flex;
  align-items: center;
  margin: 8px 0;
  font-size: 13px;
  color: #666;
}
</style>
