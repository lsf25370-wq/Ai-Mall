<template>
  <div class="page-container">
    <div v-loading="loading" class="review-list">
      <div v-for="r in reviews" :key="r.id" class="review-card">
        <div class="review-head">
          <el-rate :model-value="r.rating" disabled size="small" />
          <span class="user">买家 #{{ r.userId }}</span>
          <span class="time">{{ formatTime(r.createdAt) }}</span>
        </div>
        <p class="content">{{ r.content }}</p>
        <p v-if="r.reply" class="reply">
          <b>我的回复：</b>{{ r.reply }}
        </p>
        <div class="actions">
          <el-button v-if="!r.reply" size="small" type="primary" plain @click="openReply(r)">
            回复评价
          </el-button>
          <el-button v-else size="small" plain @click="openReply(r)">修改回复</el-button>
          <el-button size="small" type="warning" plain :loading="aiLoading === r.id" @click="aiReply(r)">
            ✨ AI 生成回复
          </el-button>
        </div>
      </div>
    </div>
    <el-empty v-if="!loading && reviews.length === 0" description="暂无评价" />

    <el-dialog v-model="replyVisible" title="回复评价" width="460px">
      <el-input v-model="replyText" type="textarea" :rows="4" maxlength="200" show-word-limit placeholder="回复买家，展现店铺服务态度" />
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReply">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { reviewApi } from '../api'

const reviews = ref([])
const loading = ref(false)
const replyVisible = ref(false)
const replyText = ref('')
const current = ref(null)
const aiLoading = ref(null)

function formatTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : ''
}

async function load() {
  loading.value = true
  try {
    reviews.value = await reviewApi.list()
  } finally {
    loading.value = false
  }
}

function openReply(r) {
  current.value = r
  replyText.value = r.reply || ''
  replyVisible.value = true
}

async function aiReply(r) {
  aiLoading.value = r.id
  try {
    const data = await reviewApi.aiReply(r.id)
    current.value = r
    replyText.value = data.reply || ''
    replyVisible.value = true
    ElMessage.success('AI 已生成回复草稿，可编辑后提交')
  } finally {
    aiLoading.value = null
  }
}

async function submitReply() {
  if (!replyText.value?.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  await reviewApi.reply(current.value.id, replyText.value.trim())
  ElMessage.success('回复成功')
  replyVisible.value = false
  load()
}

onMounted(load)
</script>

<style scoped>
.review-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-card {
  background: #fff;
  border-radius: 8px;
  padding: 14px 18px;
}

.review-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user {
  font-weight: 500;
  color: #333;
  font-size: 13px;
}

.time {
  margin-left: auto;
  color: #999;
  font-size: 12px;
}

.content {
  color: #444;
  font-size: 14px;
  margin-top: 10px;
  line-height: 1.6;
}

.reply {
  margin-top: 10px;
  color: #ff7d3c;
  font-size: 13px;
  background: #fff8f4;
  padding: 8px 12px;
  border-radius: 6px;
}

.actions {
  margin-top: 10px;
  text-align: right;
}
</style>
