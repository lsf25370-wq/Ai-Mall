<template>
  <div class="page-card">
    <div class="table-toolbar">
      <el-input v-model="keyword" placeholder="搜索商品名称" clearable style="width: 240px" @keyup.enter="load" @clear="load" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column label="ID" prop="id" width="70" />
      <el-table-column label="商品">
        <template #default="{ row }">
          <div class="product-cell">
            <img :src="row.mainImage" class="thumb" />
            <div>
              <p>{{ row.name }}</p>
              <p class="sub">{{ row.subtitle }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="价格" width="110">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column prop="stock" label="库存" width="90" />
      <el-table-column prop="sales" label="销量" width="90" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '在售' : '已下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="toggle(row)">
            {{ row.status === 1 ? '下架' : '上架' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager">
      <el-pagination
        layout="total, prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page="page"
        @current-change="changePage"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '../api'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await adminApi.products({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function changePage(p) {
  page.value = p
  load()
}

async function toggle(row) {
  await adminApi.updateProductStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success('操作成功')
  load()
}

onMounted(load)
</script>

<style scoped>
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

.sub {
  color: #999;
  font-size: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
