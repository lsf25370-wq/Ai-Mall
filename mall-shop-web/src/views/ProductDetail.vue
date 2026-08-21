<template>
  <div class="page-container">
    <div v-loading="loading" class="detail-wrap">
      <div class="gallery">
        <img :src="product.mainImage" :alt="product.name" />
      </div>
      <div class="main-info">
        <h1 class="name">{{ product.name }}</h1>
        <p class="subtitle">{{ product.subtitle }}</p>
        <div class="price-box">
          <span class="label">价格</span>
          <span class="price">
            <span class="symbol">¥</span>{{ product.price }}
          </span>
          <span class="sales">已售 {{ product.sales }} 件</span>
        </div>
        <div class="stock-box">
          <span>库存：{{ product.stock }}</span>
          <span v-if="product.stock <= 0" class="sold-out">已售罄</span>
        </div>
        <div class="desc">{{ product.detail }}</div>

        <!-- 店铺信息 -->
        <div v-if="shop" class="shop-bar">
          <img :src="shop.logo || defaultLogo" class="shop-logo" />
          <div class="shop-meta">
            <b>{{ shop.name }}</b>
            <span>入驻 AI 商城</span>
          </div>
          <el-button size="small" round @click="$router.push(`/shop/${shop.id}`)">进店逛逛</el-button>
          <el-button
            size="small"
            round
            :type="favorited ? 'warning' : 'default'"
            :icon="favorited ? StarFilled : Star"
            @click="toggleFavorite"
          >
            {{ favorited ? '已收藏' : '收藏' }}
          </el-button>
        </div>

        <div class="buy-row">
          <el-input-number v-model="quantity" :min="1" :max="Math.max(product.stock, 1)" />
          <el-button type="primary" size="large" :disabled="product.stock <= 0" @click="addToCart">
            加入购物车
          </el-button>
          <el-button type="danger" size="large" :disabled="product.stock <= 0" @click="buyNow">
            立即购买
          </el-button>
        </div>
      </div>
    </div>

    <!-- 用户评价 -->
    <div class="review-block">
      <h3 class="review-title">商品评价（{{ reviews.length }}）</h3>
      <div v-if="reviews.length" class="review-list">
        <div v-for="r in reviews" :key="r.review.id" class="review-item">
          <div class="review-head">
            <el-rate :model-value="r.review.rating" disabled size="small" />
            <span class="review-user">{{ r.nickname }}</span>
            <span class="review-time">{{ formatTime(r.review.createdAt) }}</span>
          </div>
          <p class="review-content">{{ r.review.content }}</p>
          <p v-if="r.review.reply" class="review-reply">
            <b>卖家回复：</b>{{ r.review.reply }}
          </p>
        </div>
      </div>
      <el-empty v-else description="暂无评价" :image-size="80" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { productApi, cartApi, shopApi, reviewApi, favoriteApi } from '../api'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const product = ref({})
const shop = ref(null)
const reviews = ref([])
const favorited = ref(false)
const quantity = ref(1)
const loading = ref(false)
const defaultLogo =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48"><rect width="48" height="48" rx="8" fill="%23ff5000"/><text x="24" y="30" font-size="18" fill="white" text-anchor="middle" font-weight="bold">店</text></svg>'

function formatTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : ''
}

onMounted(async () => {
  loading.value = true
  try {
    product.value = await productApi.detail(route.params.id)
    reviews.value = await reviewApi.byProduct(route.params.id)
    if (product.value.shopId) {
      try {
        shop.value = await shopApi.detail(product.value.shopId)
      } catch {
        /* 店铺不存在时忽略 */
      }
    }
    if (userStore.isLogin) {
      try {
        const r = await favoriteApi.check(route.params.id)
        favorited.value = r.favorite
      } catch {
        /* 忽略 */
      }
    }
  } finally {
    loading.value = false
  }
})

function requireLogin() {
  if (!userStore.isLogin) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return false
  }
  return true
}

async function addToCart() {
  if (!requireLogin()) return
  await cartApi.add({ productId: product.value.id, quantity: quantity.value })
  ElMessage.success('已加入购物车')
}

async function buyNow() {
  if (!requireLogin()) return
  await cartApi.add({ productId: product.value.id, quantity: quantity.value })
  router.push('/cart')
}

async function toggleFavorite() {
  if (!requireLogin()) return
  if (favorited.value) {
    await favoriteApi.remove(product.value.id)
    favorited.value = false
    ElMessage.success('已取消收藏')
  } else {
    await favoriteApi.add(product.value.id)
    favorited.value = true
    ElMessage.success('收藏成功')
  }
}
</script>

<style scoped>
.detail-wrap {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  display: flex;
  gap: 32px;
  min-height: 400px;
}

.gallery {
  width: 420px;
  height: 420px;
  flex-shrink: 0;
  background: #f7f7f7;
  border-radius: 8px;
  overflow: hidden;
}

.gallery img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.main-info {
  flex: 1;
}

.name {
  font-size: 22px;
  font-weight: 600;
  color: #333;
}

.subtitle {
  color: #999;
  margin-top: 8px;
}

.price-box {
  background: #fff7f4;
  border-radius: 8px;
  padding: 16px 20px;
  margin-top: 20px;
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.label {
  color: #999;
  font-size: 13px;
}

.price {
  color: #ff5000;
  font-size: 30px;
  font-weight: 700;
}

.symbol {
  font-size: 16px;
}

.sales {
  margin-left: auto;
  color: #999;
  font-size: 13px;
}

.stock-box {
  margin-top: 16px;
  color: #666;
  font-size: 14px;
  display: flex;
  gap: 12px;
}

.sold-out {
  color: #ff5000;
}

.desc {
  margin-top: 16px;
  color: #666;
  font-size: 14px;
  line-height: 1.8;
  padding: 12px;
  background: #fafafa;
  border-radius: 6px;
}

.shop-bar {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: #fafafa;
  border-radius: 8px;
}

.shop-logo {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  object-fit: cover;
  background: #eee;
}

.shop-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.shop-meta span {
  color: #999;
  font-size: 12px;
}

.buy-row {
  margin-top: 24px;
  display: flex;
  gap: 12px;
  align-items: center;
}

.review-block {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  margin-top: 16px;
}

.review-title {
  font-size: 16px;
  font-weight: 600;
  padding-bottom: 12px;
  border-bottom: 1px solid #f5f5f5;
}

.review-item {
  padding: 14px 0;
  border-bottom: 1px solid #f7f7f7;
}

.review-item:last-child {
  border-bottom: none;
}

.review-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.review-user {
  color: #666;
  font-size: 13px;
  font-weight: 500;
}

.review-time {
  margin-left: auto;
  color: #999;
  font-size: 12px;
}

.review-content {
  color: #444;
  font-size: 14px;
  margin-top: 8px;
  line-height: 1.6;
}

.review-reply {
  margin-top: 8px;
  color: #ff7d3c;
  font-size: 13px;
  background: #fff8f4;
  padding: 8px 12px;
  border-radius: 6px;
}
</style>
