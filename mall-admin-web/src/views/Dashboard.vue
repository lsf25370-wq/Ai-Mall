<template>
  <div v-loading="loading">
    <!-- 核心指标 -->
    <div class="stat-cards">
      <div class="stat-card">
        <p class="stat-label">用户总数</p>
        <p class="stat-value">{{ overview.userCount || 0 }}</p>
      </div>
      <div class="stat-card">
        <p class="stat-label">商品总数</p>
        <p class="stat-value">{{ overview.productCount || 0 }}</p>
      </div>
      <div class="stat-card">
        <p class="stat-label">成交订单</p>
        <p class="stat-value">{{ overview.orderCount || 0 }}</p>
      </div>
      <div class="stat-card">
        <p class="stat-label">总销售额（元）</p>
        <p class="stat-value accent">{{ overview.gmv || 0 }}</p>
      </div>
    </div>

    <!-- 图表 -->
    <div class="charts-row">
      <div class="chart-card">
        <h3>近 7 日销售趋势</h3>
        <div ref="trendRef" class="chart" />
      </div>
      <div class="chart-card">
        <h3>分类销量占比</h3>
        <div ref="catRef" class="chart" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import { adminApi } from '../api'

const loading = ref(false)
const overview = ref({})
const trendRef = ref()
const catRef = ref()

async function load() {
  loading.value = true
  try {
    overview.value = await adminApi.overview()
    renderCharts()
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  // 销售趋势
  const trend = echarts.init(trendRef.value)
  const trendData = overview.value.trend || []
  trend.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: trendData.map((t) => t.date) },
    yAxis: { type: 'value', name: '销售额(元)' },
    series: [
      {
        name: '销售额',
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.15 },
        itemStyle: { color: '#ff5000' },
        data: trendData.map((t) => t.amount),
      },
    ],
  })

  // 分类占比
  const cat = echarts.init(catRef.value)
  cat.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['40%', '65%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}: {c}' },
        data: overview.value.categorySales || [],
      },
    ],
  })

  window.addEventListener('resize', () => {
    trend.resize()
    cat.resize()
  })
}

onMounted(load)
</script>

<style scoped>
.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}

.stat-label {
  color: #999;
  font-size: 13px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  margin-top: 8px;
}

.stat-value.accent {
  color: #ff5000;
}

.charts-row {
  display: grid;
  grid-template-columns: 3fr 2fr;
  gap: 16px;
}

.chart-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}

.chart-card h3 {
  font-size: 15px;
  margin-bottom: 12px;
}

.chart {
  height: 320px;
}
</style>
