<template>
  <div class="page-container">
    <h2 class="page-title">我的评价</h2>
    <div v-loading="loading" class="review-list">
      <div v-for="r in reviews" :key="r.review.id" class="review-item">
        <img :src="r.product?.mainImage" class="item-img" @click="r.product && $router.push(`/product/${r.product.id}`)" />
        <div class="review-main">
          <div class="review-head">
            <span class="prod-name" @click="r.product && $router.push(`/product/${r.product.id}`)">
              {{ r.product?.name || '商品' }}
            </span>
            <el-rate :model-value="r.review.rating" disabled size="small" />
            <span class="shop-name">{{ r.shopName }}</span>
            <span class="time">{{ formatTime(r.review.createdAt) }}</span>
          </div>
          <p class="content">{{ r.review.content }}</p>
          <p v-if="r.review.reply" class="reply">
            <b>卖家回复：</b>{{ r.review.reply }}
          </p>
        </div>
      </div>
    </div>
    <el-empty v-if="!loading && reviews.length === 0" description="还没有发表过评价" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { reviewApi } from '../api'

const reviews = ref([])
const loading = ref(false)

function formatTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : ''
}

async function load() {
  loading.value = true
  try {
    reviews.value = await reviewApi.mine()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.review-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-item {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  display: flex;
  gap: 16px;
}

.item-img {
  width: 64px;
  height: 64px;
  border-radius: 6px;
  object-fit: cover;
  background: #f5f5f5;
  cursor: pointer;
  flex-shrink: 0;
}

.review-main {
  flex: 1;
}

.review-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.prod-name {
  font-weight: 600;
  color: #333;
  cursor: pointer;
}

.prod-name:hover {
  color: #ff5000;
}

.shop-name {
  color: #999;
  font-size: 12px;
}

.time {
  margin-left: auto;
  color: #999;
  font-size: 12px;
}

.content {
  color: #444;
  font-size: 14px;
  margin-top: 8px;
  line-height: 1.6;
}

.reply {
  margin-top: 8px;
  color: #ff7d3c;
  font-size: 13px;
  background: #fff8f4;
  padding: 8px 12px;
  border-radius: 6px;
}
</style>
