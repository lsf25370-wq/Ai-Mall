<template>
  <div class="page-container">
    <!-- 分类导航 -->
    <div class="category-bar">
      <el-tag
        v-for="cat in categories"
        :key="cat.id"
        :effect="activeCategory === cat.id ? 'dark' : 'plain'"
        class="cat-tag"
        @click="switchCategory(cat.id)"
      >
        {{ cat.name }}
      </el-tag>
    </div>

    <!-- 商品网格 -->
    <div v-loading="loading" class="product-grid">
      <ProductCard v-for="p in products" :key="p.id" :product="p" />
    </div>

    <el-empty v-if="!loading && products.length === 0" description="没有找到相关商品" />

    <!-- 分页 -->
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
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { productApi } from '../api'
import ProductCard from '../components/ProductCard.vue'

const route = useRoute()
const categories = ref([])
const activeCategory = ref(null)
const products = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(12)
const loading = ref(false)

async function loadCategories() {
  categories.value = await productApi.categories()
}

async function load(p = page.value) {
  loading.value = true
  try {
    const params = { page: p, size: size.value }
    if (activeCategory.value) params.categoryId = activeCategory.value
    if (route.query.keyword) params.keyword = route.query.keyword
    const data = await productApi.list(params)
    products.value = data.records
    total.value = data.total
    page.value = data.current
  } finally {
    loading.value = false
  }
}

function switchCategory(id) {
  activeCategory.value = id
  load(1)
}

onMounted(() => {
  loadCategories()
  load()
})

watch(() => route.query.keyword, () => load(1))
</script>

<style scoped>
.category-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.cat-tag {
  cursor: pointer;
  font-size: 14px;
  padding: 6px 16px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  min-height: 300px;
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
