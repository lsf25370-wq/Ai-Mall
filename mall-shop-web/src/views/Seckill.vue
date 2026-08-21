<template>
  <div class="page-container">
    <h2 class="page-title">⚡ 限时秒杀</h2>
    <p class="page-sub">秒杀库存由 Redis 预扣，先到先得，每人限购 1 件</p>

    <div v-loading="loading" class="seckill-grid">
      <div v-for="a in activities" :key="a.id" class="seckill-card">
        <div class="seckill-img-wrap">
          <img :src="a.productImage" class="seckill-img" />
          <span class="seckill-badge">秒杀</span>
        </div>
        <div class="seckill-info">
          <h4>{{ a.productName }}</h4>
          <div class="price-row">
            <span class="seckill-price">¥{{ a.seckillPrice }}</span>
            <span class="origin-price">¥{{ originPrice(a) }}</span>
          </div>
          <div class="stock-bar">
            <span class="remain">剩余 {{ detailMap[a.id]?.remainStock ?? remainOf(a) }} 件</span>
            <el-progress
              :percentage="stockPercent(a)"
              :stroke-width="6"
              :show-text="false"
              color="#ff5000"
            />
          </div>
          <el-button
            type="danger"
            :disabled="soldOut(a)"
            :loading="buyingId === a.id"
            class="buy-btn"
            @click="buy(a)"
          >
            {{ soldOut(a) ? '已抢光' : '立即抢购' }}
          </el-button>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && activities.length === 0" description="暂无进行中的秒杀活动" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { seckillApi, addressApi, orderApi, productApi } from '../api'

const router = useRouter()
const activities = ref([])
const detailMap = ref({})
const loading = ref(false)
const buyingId = ref(null)

function originPrice(a) {
  return a.originPrice || 0
}

function remainOf(a) {
  return a.totalStock - a.soldCount
}

function stockPercent(a) {
  return Math.max(0, Math.min(100, Math.round((remainOf(a) / a.totalStock) * 100)))
}

function soldOut(a) {
  return (detailMap.value[a.id]?.remainStock ?? remainOf(a)) <= 0
}

async function load() {
  loading.value = true
  try {
    activities.value = await seckillApi.list()
    for (const a of activities.value) {
      try {
        const d = await seckillApi.detail(a.id)
        detailMap.value[a.id] = d
        const p = await productApi.detail(a.productId)
        a.originPrice = p.price
      } catch {
        /* 忽略详情失败 */
      }
    }
  } finally {
    loading.value = false
  }
}

async function buy(a) {
  const token = localStorage.getItem('token')
  if (!token) {
    router.push({ name: 'login', query: { redirect: '/seckill' } })
    return
  }
  const addrs = await addressApi.list()
  if (!addrs.length) {
    ElMessage.warning('请先在个人中心添加收货地址')
    router.push('/profile')
    return
  }
  const useDefault = addrs.find((x) => x.isDefault === 1) || addrs[0]
  await ElMessageBox.confirm(
    `确认以秒杀价 ¥${a.seckillPrice} 抢购「${a.productName}」？`,
    '秒杀确认',
    { type: 'warning' },
  )
  buyingId.value = a.id
  try {
    const data = await seckillApi.buy(a.id, useDefault.id)
    await orderApi.pay(data.orderId)
    ElMessage.success('🎉 秒杀成功，已生成订单并支付')
    load()
  } catch (e) {
    ElMessage.error(e.message || '抢购失败，请重试')
    load()
  } finally {
    buyingId.value = null
  }
}

onMounted(load)
</script>

<style scoped>
.seckill-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.seckill-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}

.seckill-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 6px 20px rgba(255, 80, 0, 0.12);
}

.seckill-img-wrap {
  position: relative;
}

.seckill-img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  display: block;
}

.seckill-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  background: #ff5000;
  color: #fff;
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 12px;
}

.seckill-info {
  padding: 14px 16px 16px;
}

.seckill-info h4 {
  font-size: 15px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 8px;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 10px;
}

.seckill-price {
  color: #ff5000;
  font-size: 24px;
  font-weight: 700;
}

.origin-price {
  color: #bbb;
  font-size: 13px;
  text-decoration: line-through;
}

.stock-bar {
  margin-bottom: 12px;
}

.remain {
  font-size: 12px;
  color: #999;
}

.buy-btn {
  width: 100%;
}

@media (max-width: 1000px) {
  .seckill-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
