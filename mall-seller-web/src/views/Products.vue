<template>
  <div class="page-container">
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索商品名称"
        clearable
        class="search"
        @keyup.enter="load(1)"
        @clear="load(1)"
      />
      <el-button type="primary" @click="openDialog()">+ 新增商品</el-button>
    </div>

    <el-table v-loading="loading" :data="products" stripe>
      <el-table-column label="商品" min-width="260">
        <template #default="{ row }">
          <div class="prod-cell">
            <img :src="row.mainImage" class="prod-img" />
            <div>
              <p class="prod-name">{{ row.name }}</p>
              <p class="prod-sub">{{ row.subtitle }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="price" label="价格" width="90">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column prop="stock" label="库存" width="80" />
      <el-table-column prop="sales" label="销量" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '在售' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button
            size="small"
            :type="row.status === 1 ? 'warning' : 'success'"
            plain
            @click="toggleStatus(row)"
          >
            {{ row.status === 1 ? '下架' : '上架' }}
          </el-button>
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        layout="prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page="page"
        @current-change="load"
      />
    </div>

    <!-- 新增/编辑商品 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑商品' : '新增商品'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="商品名称">
          <el-input v-model="form.name" placeholder="商品名称" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" placeholder="一句话卖点（选填）" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="form.stock" :min="0" :precision="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="主图地址">
          <el-input v-model="form.mainImage" placeholder="https://... 商品图片链接" />
        </el-form-item>
        <el-form-item label="商品详情">
          <el-input v-model="form.detail" type="textarea" :rows="3" placeholder="商品详细介绍" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { productApi } from '../api'

const products = ref([])
const categories = ref([])
const keyword = ref('')
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const emptyForm = { id: null, name: '', subtitle: '', categoryId: null, price: 0.01, stock: 0, mainImage: '', detail: '' }
const form = reactive({ ...emptyForm })

async function load(p = page.value) {
  loading.value = true
  try {
    const params = { page: p, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    const data = await productApi.list(params)
    products.value = data.records
    total.value = data.total
    page.value = data.current
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  if (row) {
    Object.assign(form, emptyForm, {
      id: row.id, name: row.name, subtitle: row.subtitle, categoryId: row.categoryId,
      price: row.price, stock: row.stock, mainImage: row.mainImage, detail: row.detail,
    })
  } else {
    Object.assign(form, emptyForm)
  }
  dialogVisible.value = true
}

async function save() {
  if (!form.name?.trim()) {
    ElMessage.warning('请填写商品名称')
    return
  }
  if (!form.categoryId) {
    ElMessage.warning('请选择分类')
    return
  }
  saving.value = true
  try {
    if (form.id) {
      await productApi.update(form.id, { ...form })
      ElMessage.success('商品已更新')
    } else {
      await productApi.create({ ...form })
      ElMessage.success('商品已创建')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const target = row.status === 1 ? 0 : 1
  await productApi.updateStatus(row.id, target)
  ElMessage.success(target === 1 ? '已上架' : '已下架')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除商品「${row.name}」？`, '提示', { type: 'warning' })
  await productApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  categories.value = await productApi.categories()
  load()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 14px;
  align-items: center;
}

.search {
  width: 260px;
}

.prod-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.prod-img {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  object-fit: cover;
  background: #f5f5f5;
  flex-shrink: 0;
}

.prod-name {
  font-weight: 500;
  color: #333;
}

.prod-sub {
  color: #999;
  font-size: 12px;
  margin-top: 2px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
