<template>
  <div class="page-container">
    <h2 class="page-title">🎟️ 领券中心</h2>

    <!-- 可领券 -->
    <div class="section">
      <h3 class="section-title">可领取</h3>
      <div class="coupon-grid">
        <div v-for="c in available" :key="c.id" class="coupon-card">
          <div class="coupon-left" :class="c.type === 2 ? 'discount' : 'cut'">
            <template v-if="c.type === 1">
              <b>¥{{ c.amount }}</b>
              <span>满 {{ c.threshold }} 可用</span>
            </template>
            <template v-else>
              <b>{{ (Number(c.discount) * 10).toFixed(1) }}折</b>
              <span>全场通用</span>
            </template>
          </div>
          <div class="coupon-right">
            <p class="coupon-name">{{ c.name }}</p>
            <p class="coupon-meta">有效期 {{ c.validDays }} 天 · 已领 {{ c.claimedCount }}/{{ c.totalCount }}</p>
            <el-button
              size="small"
              type="warning"
              :disabled="c.claimedCount >= c.totalCount"
              @click="claim(c)"
            >
              {{ c.claimedCount >= c.totalCount ? '已抢完' : '立即领取' }}
            </el-button>
          </div>
        </div>
      </div>
      <el-empty v-if="!available.length" description="暂无可以领取的优惠券" :image-size="60" />
    </div>

    <!-- 我的券 -->
    <div class="section">
      <h3 class="section-title">我的优惠券（{{ myCoupons.length }}）</h3>
      <div v-if="myCoupons.length" class="my-coupon-list">
        <div
          v-for="m in myCoupons"
          :key="m.id"
          class="my-coupon"
          :class="'st-' + m.status"
        >
          <div class="mc-left">
            <template v-if="m.type === 1">
              <b>¥{{ m.amount }}</b>
              <span>满 {{ m.threshold }} 可用</span>
            </template>
            <template v-else>
              <b>{{ (Number(m.discount) * 10).toFixed(1) }}折</b>
              <span>全场通用</span>
            </template>
          </div>
          <div class="mc-right">
            <p class="mc-name">{{ m.name }}</p>
            <p class="mc-expire">有效期至 {{ formatTime(m.expireTime) }}</p>
          </div>
          <el-tag size="small" :type="statusType(m.status)">{{ statusText(m.status) }}</el-tag>
        </div>
      </div>
      <el-empty v-else description="还没有优惠券，快去领取吧" :image-size="60" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { couponApi } from '../api'

const available = ref([])
const myCoupons = ref([])

function formatTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : ''
}

function statusText(s) {
  return s === 1 ? '已使用' : s === 2 ? '已过期' : '未使用'
}

function statusType(s) {
  return s === 1 ? 'info' : s === 2 ? 'danger' : 'success'
}

async function load() {
  available.value = await couponApi.available()
  myCoupons.value = await couponApi.my()
}

async function claim(c) {
  await couponApi.claim(c.id)
  ElMessage.success(`「${c.name}」领取成功`)
  load()
}

onMounted(load)
</script>

<style scoped>
.section {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  margin-bottom: 16px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 16px;
}

.coupon-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.coupon-card {
  display: flex;
  border: 1px solid #ffe0cc;
  border-radius: 8px;
  overflow: hidden;
}

.coupon-left {
  width: 100px;
  background: #fff6f0;
  color: #ff5000;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  flex-shrink: 0;
}

.coupon-left b {
  font-size: 22px;
}

.coupon-left span {
  font-size: 11px;
}

.coupon-left.discount {
  background: #fff7e6;
  color: #e6a23c;
}

.coupon-right {
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.coupon-name {
  font-weight: 600;
  font-size: 14px;
}

.coupon-meta {
  color: #999;
  font-size: 12px;
}

.my-coupon-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.my-coupon {
  display: flex;
  align-items: center;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 10px 16px;
  gap: 16px;
}

.my-coupon.st-2 {
  opacity: 0.55;
}

.mc-left {
  width: 90px;
  color: #ff5000;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.mc-left b {
  font-size: 20px;
}

.mc-left span {
  font-size: 11px;
  color: #999;
}

.mc-right {
  flex: 1;
}

.mc-name {
  font-weight: 600;
}

.mc-expire {
  color: #999;
  font-size: 12px;
  margin-top: 4px;
}

@media (max-width: 1000px) {
  .coupon-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
