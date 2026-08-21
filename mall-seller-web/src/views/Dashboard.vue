<template>
  <div class="page-container">
    <div v-loading="loading">
      <!-- 指标卡 -->
      <div class="stat-grid">
        <div class="stat-card">
          <div class="stat-icon orange">📦</div>
          <div class="stat-info">
            <span>在售商品</span>
            <b>{{ stats.productCount || 0 }}</b>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon blue">💰</div>
          <div class="stat-info">
            <span>累计 GMV</span>
            <b>¥{{ formatMoney(stats.gmv) }}</b>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon green">🛒</div>
          <div class="stat-info">
            <span>累计订单</span>
            <b>{{ stats.orderCount || 0 }}</b>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon red">🚚</div>
          <div class="stat-info">
            <span>待发货</span>
            <b>{{ stats.pendingShip || 0 }}</b>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon purple">⭐</div>
          <div class="stat-info">
            <span>销量合计</span>
            <b>{{ stats.salesTotal || 0 }}</b>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon teal">🏷️</div>
          <div class="stat-info">
            <span>库存合计</span>
            <b>{{ stats.stockTotal || 0 }}</b>
          </div>
        </div>
      </div>

      <!-- 近7日销售趋势 -->
      <div class="chart-card">
        <h3>近 7 日销售趋势</h3>
        <div ref="chartRef" class="chart"></div>
      </div>

      <!-- AI 经营分析 -->
      <div class="chart-card ai-card">
        <div class="ai-head">
          <h3>✨ AI 经营分析</h3>
          <el-button size="small" type="warning" plain :loading="aiLoading" @click="runAiAnalysis">
            生成经营分析
          </el-button>
        </div>
        <p v-if="!analysis" class="ai-empty">点击按钮，AI 将基于店铺经营数据生成趋势解读与优化建议。</p>
        <pre v-else class="ai-content">{{ analysis }}</pre>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { statsApi } from '../api'

const loading = ref(false)
const stats = ref({})
const chartRef = ref(null)
let chart = null

function formatMoney(v) {
  const n = Number(v || 0)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function renderChart(trend) {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: {
      type: 'category',
      data: (trend || []).map((t) => t.date),
      axisLine: { lineStyle: { color: '#ddd' } },
    },
    yAxis: {
      type: 'value',
      name: 'GMV(元)',
      splitLine: { lineStyle: { type: 'dashed' } },
    },
    series: [
      {
        name: '销售额',
        type: 'line',
        smooth: true,
        data: (trend || []).map((t) => Number(t.amount)),
        itemStyle: { color: '#ff5000' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255,80,0,0.25)' },
            { offset: 1, color: 'rgba(255,80,0,0)' },
          ]),
        },
      },
    ],
  })
}

async function load() {
  loading.value = true
  try {
    stats.value = await statsApi.overview()
    renderChart(stats.value.trend)
  } finally {
    loading.value = false
  }
}

onMounted(load)
onBeforeUnmount(() => chart && chart.dispose())
</script>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}

.stat-icon.orange { background: #fff3ec; }
.stat-icon.blue { background: #ecf5ff; }
.stat-icon.green { background: #ecf9f0; }
.stat-icon.red { background: #fef0f0; }
.stat-icon.purple { background: #f5f0ff; }
.stat-icon.teal { background: #ecfaf7; }

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-info span {
  color: #888;
  font-size: 12px;
}

.stat-info b {
  font-size: 20px;
  color: #333;
  margin-top: 4px;
}

.chart-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
}

.chart-card h3 {
  font-size: 15px;
  margin-bottom: 12px;
}

.chart {
  height: 320px;
}

.ai-card {
  margin-top: 16px;
}

.ai-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.ai-empty {
  color: #999;
  font-size: 13px;
  padding: 12px 0;
}

.ai-content {
  background: #fff8f4;
  border-radius: 8px;
  padding: 14px 16px;
  color: #5a4636;
  font-size: 13px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
}

@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
