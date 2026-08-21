<template>
  <div class="page-container">
    <h2 class="page-title">购物车</h2>
    <div class="cart-card">
      <el-table :data="items" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column label="商品" min-width="260">
          <template #default="{ row }">
            <div class="product-cell" @click="$router.push(`/product/${row.productId}`)">
              <img :src="row.productImage" class="thumb" />
              <span class="pname">{{ row.productName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="120">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column label="数量" width="160">
          <template #default="{ row }">
            <el-input-number
              v-model="row.quantity"
              :min="1"
              size="small"
              @change="updateQuantity(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="小计" width="120">
          <template #default="{ row }">
            <span class="subtotal">¥{{ (row.price * row.quantity).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button link type="danger" @click="removeItem(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="settle-bar">
        <span class="summary">
          已选 <b>{{ selectedIds.length }}</b> 件，合计：
          <span class="total-price">¥{{ totalPrice }}</span>
        </span>
        <el-button type="danger" size="large" :disabled="selectedIds.length === 0" @click="checkout">
          去结算
        </el-button>
      </div>
    </div>
    <el-empty v-if="items.length === 0" description="购物车空空如也，去逛逛吧">
      <el-button type="primary" @click="$router.push('/')">去首页逛逛</el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { cartApi } from '../api'

const router = useRouter()
const items = ref([])
const selectedIds = ref([])

async function load() {
  items.value = await cartApi.list()
  // 同步勾选状态
  const checked = items.value.filter((i) => i.checked === 1).map((i) => i.id)
  if (checked.length) selectedIds.value = checked
}

async function updateQuantity(row) {
  await cartApi.updateQuantity(row.id, row.quantity)
}

async function removeItem(row) {
  await cartApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

function onSelectionChange(rows) {
  selectedIds.value = rows.map((r) => r.id)
  rows.forEach((r) => cartApi.updateChecked(r.id, 1))
  items.value
    .filter((i) => !rows.includes(i))
    .forEach((i) => cartApi.updateChecked(i.id, 0))
}

const totalPrice = computed(() => {
  const selected = items.value.filter((i) => selectedIds.value.includes(i.id))
  return selected.reduce((sum, i) => sum + i.price * i.quantity, 0).toFixed(2)
})

function checkout() {
  router.push('/checkout')
}

onMounted(load)
</script>

<style scoped>
.cart-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.thumb {
  width: 56px;
  height: 56px;
  border-radius: 6px;
  object-fit: cover;
  background: #f5f5f5;
}

.subtotal {
  color: #ff5000;
  font-weight: 600;
}

.settle-bar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 20px;
  padding: 16px 4px 0;
  border-top: 1px solid #f0f0f0;
  margin-top: 16px;
}

.total-price {
  color: #ff5000;
  font-size: 22px;
  font-weight: 700;
}
</style>
