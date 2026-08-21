<template>
  <div class="page-card">
    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="nickname" label="昵称" width="140" />
      <el-table-column prop="phone" label="手机号" width="150" />
      <el-table-column label="会员等级" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="levelType(row.level)">Lv{{ row.level }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="角色" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.role === 1" type="danger" size="small">管理员</el-tag>
          <el-tag v-else type="info" size="small">普通用户</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="170">
        <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
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
import { adminApi } from '../api'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

const levelType = (lv) => (lv >= 3 ? 'danger' : lv === 2 ? 'warning' : 'info')
const fmt = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '-')

async function load() {
  loading.value = true
  try {
    const data = await adminApi.users({ page: page.value, size: size.value })
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

onMounted(load)
</script>

<style scoped>
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
