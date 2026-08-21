<template>
  <div class="ai-page">
    <div class="ai-container">
      <!-- 会话列表 -->
      <aside class="session-panel">
        <div class="session-head">
          <span>AI 客服</span>
          <el-button type="primary" size="small" :icon="Plus" circle @click="newSession" />
        </div>
        <div class="session-list">
          <div
            v-for="s in sessions"
            :key="s.sessionId"
            class="session-item"
            :class="{ active: s.sessionId === currentSession }"
            @click="switchSession(s.sessionId)"
          >
            <p class="s-title">{{ s.title }}</p>
            <p class="s-time">{{ formatTime(s.updatedAt) }}</p>
          </div>
        </div>
        <div class="session-tips">
          <p>试试问我：</p>
          <ul>
            <li>我有哪些订单？</li>
            <li>查询订单 ORD202608180001</li>
            <li>推荐一款手机</li>
          </ul>
        </div>
      </aside>

      <!-- 聊天窗口 -->
      <main class="chat-panel">
        <div ref="msgBox" class="msg-list">
          <div v-for="(m, i) in messages" :key="i" class="msg-row" :class="m.role">
            <div class="avatar">{{ m.role === 'user' ? '我' : 'AI' }}</div>
            <div class="bubble">{{ m.content }}</div>
          </div>
          <div v-if="thinking" class="msg-row assistant">
            <div class="avatar">AI</div>
            <div class="bubble thinking">正在思考...</div>
          </div>
        </div>
        <div class="input-bar">
          <el-input
            v-model="input"
            placeholder="输入您的问题，例如：我的订单到哪了？"
            size="large"
            @keyup.enter="send"
          />
          <el-button type="primary" size="large" :loading="thinking" @click="send">发送</el-button>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { aiApi } from '../api'

const sessions = ref([])
const currentSession = ref('')
const messages = ref([])
const input = ref('')
const thinking = ref(false)
const msgBox = ref()

async function loadSessions() {
  sessions.value = await aiApi.listSessions()
}

async function newSession() {
  const data = await aiApi.createSession()
  currentSession.value = data.sessionId
  messages.value = []
  await loadSessions()
}

async function switchSession(sessionId) {
  currentSession.value = sessionId
  messages.value = await aiApi.history(sessionId)
  scrollBottom()
}

async function send() {
  const text = input.value.trim()
  if (!text || thinking.value) return
  messages.value.push({ role: 'user', content: text })
  input.value = ''
  thinking.value = true
  scrollBottom()
  try {
    const data = await aiApi.chat({ sessionId: currentSession.value, message: text })
    messages.value.push({ role: 'assistant', content: data.reply })
    await loadSessions()
  } finally {
    thinking.value = false
    scrollBottom()
  }
}

function scrollBottom() {
  nextTick(() => {
    if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
  })
}

function formatTime(t) {
  return t ? String(t).replace('T', ' ').slice(5, 16) : ''
}

onMounted(async () => {
  await loadSessions()
  if (sessions.value.length) {
    await switchSession(sessions.value[0].sessionId)
  } else {
    await newSession()
  }
})
</script>

<style scoped>
.ai-page {
  max-width: 1200px;
  margin: 20px auto;
  padding: 0 16px;
  height: calc(100vh - 170px);
}

.ai-container {
  display: flex;
  height: 100%;
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.session-panel {
  width: 240px;
  flex-shrink: 0;
  background: #fafafa;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
}

.session-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  font-weight: 600;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
}

.session-item {
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
}

.session-item:hover {
  background: #f0f0f0;
}

.session-item.active {
  background: #ffe8df;
}

.s-title {
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.s-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.session-tips {
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
  font-size: 12px;
  color: #999;
}

.session-tips ul {
  margin-top: 6px;
  padding-left: 16px;
  line-height: 1.9;
}

.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.msg-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: linear-gradient(180deg, #f8f9fa 0%, #fff 100%);
}

.msg-row {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.msg-row.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #fff;
}

.msg-row.assistant .avatar {
  background: linear-gradient(135deg, #ff5000, #ff8c42);
}

.msg-row.user .avatar {
  background: #409eff;
}

.bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.msg-row.assistant .bubble {
  background: #fff;
  border: 1px solid #eee;
}

.msg-row.user .bubble {
  background: #409eff;
  color: #fff;
}

.thinking {
  color: #999;
}

.input-bar {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
}
</style>
