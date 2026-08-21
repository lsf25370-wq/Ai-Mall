<template>
  <div class="page-container">
    <h2 class="page-title">确认订单</h2>
    <div class="checkout-wrap">
      <!-- 收货地址 -->
      <div class="block">
        <h3 class="block-title">收货地址</h3>
        <div v-if="addresses.length" class="addr-list">
          <div
            v-for="addr in addresses"
            :key="addr.id"
            class="addr-card"
            :class="{ active: selectedAddressId === addr.id }"
            @click="selectedAddressId = addr.id"
          >
            <p class="receiver">
              {{ addr.receiver }} <span class="phone">{{ addr.phone }}</span>
              <el-tag v-if="addr.isDefault === 1" size="small" type="warning" class="def-tag">默认</el-tag>
            </p>
            <p class="addr-text">
              {{ addr.province }}{{ addr.city }}{{ addr.district }} {{ addr.detail }}
            </p>
          </div>
        </div>
        <el-button v-if="!showAddForm" size="small" @click="showAddForm = true">+ 新增地址</el-button>
        <el-form v-if="showAddForm" :model="addrForm" label-width="80px" inline class="addr-form">
          <el-form-item label="收货人"><el-input v-model="addrForm.receiver" /></el-form-item>
          <el-form-item label="手机号"><el-input v-model="addrForm.phone" /></el-form-item>
          <el-form-item label="省份"><el-input v-model="addrForm.province" /></el-form-item>
          <el-form-item label="城市"><el-input v-model="addrForm.city" /></el-form-item>
          <el-form-item label="区县"><el-input v-model="addrForm.district" /></el-form-item>
          <el-form-item label="详细地址"><el-input v-model="addrForm.detail" style="width: 260px" /></el-form-item>
          <el-form-item>
            <el-checkbox v-model="addrForm.isDefault">设为默认</el-checkbox>
            <el-button type="primary" @click="addAddress">保存</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 商品清单 -->
      <div class="block">
        <h3 class="block-title">商品清单</h3>
        <el-table :data="items">
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
          <el-table-column label="小计" width="120">
            <template #default="{ row }">
              <span class="price-red">¥{{ (row.price * row.quantity).toFixed(2) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 优惠券 -->
      <div class="block">
        <h3 class="block-title">优惠券</h3>
        <div v-if="myCoupons.length" class="coupon-list">
          <el-radio-group v-model="selectedCouponId" class="coupon-radios">
            <el-radio-button :value="null">不使用</el-radio-button>
            <el-radio-button
              v-for="c in usableCoupons"
              :key="c.id"
              :value="c.id"
              :disabled="!couponUsable(c)"
            >
              {{ c.name }}
            </el-radio-button>
          </el-radio-group>
          <p v-if="selectedCouponId" class="coupon-tip">
            {{ selectedCouponDesc }}
          </p>
        </div>
        <el-empty v-else description="暂无可用优惠券，可在「领券中心」领取" :image-size="60">
          <el-button size="small" type="warning" plain @click="router.push({ name: 'coupons' })">去领券</el-button>
        </el-empty>
      </div>

      <div class="pay-bar">
        <div class="pay-summary">
          <span>商品总额：<b class="orig-total">¥{{ total }}</b></span>
          <span v-if="discount > 0">优惠：<b class="discount">-¥{{ discount.toFixed(2) }}</b></span>
          <span>应付总额：<span class="pay-amount">¥{{ payTotal }}</span></span>
        </div>
        <el-button type="danger" size="large" :loading="submitting" @click="submit">
          提交订单
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { cartApi, addressApi, orderApi, couponApi } from '../api'

const router = useRouter()
const addresses = ref([])
const items = ref([])
const selectedAddressId = ref(null)
const showAddForm = ref(false)
const submitting = ref(false)
const myCoupons = ref([])
const selectedCouponId = ref(null)
const addrForm = ref({ receiver: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: false })

async function load() {
  addresses.value = await addressApi.list()
  items.value = (await cartApi.list()).filter((i) => i.checked === 1)
  const def = addresses.value.find((a) => a.isDefault === 1)
  selectedAddressId.value = def ? def.id : addresses.value[0]?.id || null
  myCoupons.value = (await couponApi.my()).filter((c) => c.status === 0)
}

async function addAddress() {
  await addressApi.add({ ...addrForm.value, isDefault: addrForm.value.isDefault ? 1 : 0 })
  ElMessage.success('地址已保存')
  showAddForm.value = false
  addrForm.value = { receiver: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: false }
  load()
}

const total = computed(() =>
  items.value.reduce((s, i) => s + i.price * i.quantity, 0).toFixed(2),
)

/** 购物车中任意店铺的商品总额（优惠券按店铺/全场计算） */
function shopTotalOf(shopId) {
  const ids = items.value.map((i) => i.productId)
  return 0 // 简化：前端仅展示预估，最终以服务端核算为准
}

/** 券是否可用于当前购物车 */
function couponUsable(c) {
  const n = Number(total.value)
  return n >= Number(c.threshold)
}

const usableCoupons = computed(() => myCoupons.value)

const selectedCouponDesc = computed(() => {
  const c = myCoupons.value.find((x) => x.id === selectedCouponId.value)
  if (!c) return ''
  if (c.type === 1) return `满 ¥${c.threshold} 减 ¥${c.amount}`
  return `全场 ${(Number(c.discount) * 10).toFixed(1)} 折`
})

const discount = computed(() => {
  const c = myCoupons.value.find((x) => x.id === selectedCouponId.value)
  if (!c) return 0
  const t = Number(total.value)
  if (c.type === 1) return Math.min(Number(c.amount), t)
  return Number((t * (1 - Number(c.discount))).toFixed(2))
})

const payTotal = computed(() => (Number(total.value) - discount.value).toFixed(2))

async function submit() {
  if (!selectedAddressId.value) {
    ElMessage.warning('请选择收货地址')
    return
  }
  submitting.value = true
  try {
    const data = await orderApi.create({
      cartItemIds: items.value.map((i) => i.id),
      addressId: selectedAddressId.value,
      userCouponId: selectedCouponId.value,
    })
    // 多店铺拆单：对每个子订单逐一模拟支付
    const orders = data.orders && data.orders.length ? data.orders : [{ orderId: data.orderId }]
    for (const o of orders) {
      await orderApi.pay(o.orderId)
    }
    const tip = orders.length > 1 ? `下单成功，已按店铺拆分为 ${orders.length} 笔订单并全部支付` : '下单并支付成功'
    ElMessage.success(tip)
    router.push({ name: 'orders' })
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.block {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
}

.block-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.addr-list {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.addr-card {
  width: 280px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.addr-card.active {
  border-color: #ff5000;
  box-shadow: 0 0 0 2px rgba(255, 80, 0, 0.15);
}

.receiver {
  font-weight: 600;
}

.phone {
  color: #999;
  font-weight: 400;
  margin-left: 8px;
}

.def-tag {
  margin-left: 8px;
}

.addr-text {
  color: #666;
  font-size: 13px;
  margin-top: 6px;
}

.addr-form {
  margin-top: 12px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.thumb {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  object-fit: cover;
}

.price-red {
  color: #ff5000;
}

.pay-bar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 20px;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  font-size: 15px;
}

.pay-summary {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-right: auto;
}

.orig-total {
  color: #999;
  font-weight: 400;
  text-decoration: line-through;
}

.discount {
  color: #ff5000;
}

.pay-amount {
  color: #ff5000;
  font-size: 26px;
  font-weight: 700;
}

.coupon-tip {
  margin-top: 10px;
  color: #ff5000;
  font-size: 13px;
}
</style>
