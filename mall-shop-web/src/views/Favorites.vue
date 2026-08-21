<template>
  <div class="page-container">
    <h2 class="page-title">我的收藏</h2>
    <div v-loading="loading" class="product-grid">
      <div v-for="p in products" :key="p.id" class="fav-card">
        <div class="img-wrap" @click="$router.push(`/product/${p.id}`)">
          <img :src="p.mainImage" :alt="p.name" loading="lazy" />
        </div>
        <div class="info">
          <p class="name" @click="$router.push(`/product/${p.id}`)">{{ p.name }}</p>
          <div class="bottom">
            <span class="price">
              <span class="symbol">¥</span>{{ p.price }}
            </span>
            <el-button link type="danger" size="small" @click="remove(p.id)">取消收藏</el-button>
          </div>
        </div>
      </div>
    </div>
    <el-empty v-if="!loading && products.length === 0" description="还没有收藏的商品，去逛逛吧" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { favoriteApi } from '../api'

const products = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    products.value = await favoriteApi.list()
  } finally {
    loading.value = false
  }
}

async function remove(id) {
  await favoriteApi.remove(id)
  ElMessage.success('已取消收藏')
  load()
}

onMounted(load)
</script>

<style scoped>
.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  min-height: 200px;
}

.fav-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.img-wrap {
  width: 100%;
  aspect-ratio: 1;
  background: #f7f7f7;
  cursor: pointer;
  overflow: hidden;
}

.img-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info {
  padding: 10px 12px 14px;
}

.name {
  font-size: 14px;
  color: #333;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.name:hover {
  color: #ff5000;
}

.bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.price {
  color: #ff5000;
  font-size: 18px;
  font-weight: 600;
}

.symbol {
  font-size: 12px;
}

@media (max-width: 900px) {
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
