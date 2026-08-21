<template>
  <div class="page-container">
    <div v-loading="loading">
      <!-- 店铺头部 -->
      <div class="shop-header">
        <img :src="shop.logo || defaultLogo" class="shop-logo" />
        <div class="shop-info">
          <h2>{{ shop.name }}</h2>
          <p>{{ shop.description || '该店铺暂未填写简介' }}</p>
          <span class="shop-since">入驻时间：{{ formatTime(shop.createdAt) }}</span>
        </div>
      </div>

      <!-- 店铺商品 -->
      <h3 class="section-title">店铺商品</h3>
      <div class="product-grid">
        <ProductCard v-for="p in products" :key="p.id" :product="p" />
      </div>
      <el-empty v-if="!loading && products.length === 0" description="该店铺暂无在售商品" />

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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { shopApi } from '../api'
import ProductCard from '../components/ProductCard.vue'

const route = useRoute()
const shop = ref({})
const products = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(12)
const loading = ref(false)
const defaultLogo =
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48"><rect width="48" height="48" rx="8" fill="%23ff5000"/><text x="24" y="30" font-size="18" fill="white" text-anchor="middle" font-weight="bold">店</text></svg>'

function formatTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 10) : ''
}

async function load(p = page.value) {
  loading.value = true
  try {
    const data = await shopApi.products(route.params.id, { page: p, size: size.value })
    products.value = data.records
    total.value = data.total
    page.value = data.current
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    shop.value = await shopApi.detail(route.params.id)
    await load(1)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.shop-header {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 16px;
}

.shop-logo {
  width: 84px;
  height: 84px;
  border-radius: 12px;
  object-fit: cover;
  background: #f5f5f5;
}

.shop-info h2 {
  font-size: 22px;
  color: #333;
}

.shop-info p {
  color: #888;
  font-size: 14px;
  margin-top: 8px;
}

.shop-since {
  display: inline-block;
  margin-top: 10px;
  color: #999;
  font-size: 12px;
  background: #f7f7f7;
  padding: 4px 10px;
  border-radius: 4px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 14px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  min-height: 200px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 900px) {
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
