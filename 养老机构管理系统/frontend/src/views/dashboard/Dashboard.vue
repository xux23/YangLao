<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in statCards" :key="card.label">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" :style="{ background: card.color }">
              <el-icon :size="26" color="#fff"><component :is="card.icon" /></el-icon>
            </div>
            <div>
              <div class="stat-value">{{ card.value }}</div>
              <div class="stat-label">{{ card.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>近 30 天护理 / 探访趋势</template>
          <div ref="trendChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>老人年龄分布</template>
          <div ref="ageChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { getOverview, getAgeDistribution, getActivityTrend } from '../../api/stats'

const trendChartRef = ref(null)
const ageChartRef = ref(null)
let trendChart = null
let ageChart = null

const overview = reactive({
  elderTotal: 0,
  inHouse: 0,
  roomTotal: 0,
  checkInRate: 0,
  todayCareCount: 0,
  todayVisitCount: 0,
  overdueTaskCount: 0
})

const statCards = computed(() => [
  { label: '老人总数', value: overview.elderTotal, icon: 'User', color: '#2b6cb0' },
  { label: '在住人数', value: overview.inHouse, icon: 'House', color: '#38a169' },
  { label: '今日护理次数', value: overview.todayCareCount, icon: 'FirstAidKit', color: '#d69e2e' },
  { label: '逾期用药任务', value: overview.overdueTaskCount, icon: 'Warning', color: '#e53e3e' }
])

onMounted(async () => {
  await loadOverview()
  await loadCharts()
})

onUnmounted(() => {
  // 组件销毁时释放图表实例，防止内存泄漏
  trendChart && trendChart.dispose()
  ageChart && ageChart.dispose()
})

async function loadOverview() {
  const res = await getOverview()
  Object.assign(overview, res.data)
}

async function loadCharts() {
  // 趋势折线图
  const trendRes = await getActivityTrend(30)
  trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['护理次数', '探访次数'] },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: trendRes.data.dates },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '护理次数',
        type: 'line',
        smooth: true,
        data: trendRes.data.careCounts,
        areaStyle: { opacity: 0.1 }
      },
      {
        name: '探访次数',
        type: 'line',
        smooth: true,
        data: trendRes.data.visitCounts,
        areaStyle: { opacity: 0.1 }
      }
    ]
  })

  // 年龄分布饼图
  const ageRes = await getAgeDistribution()
  ageChart = echarts.init(ageChartRef.value)
  ageChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}：{c} 人（{d}%）' },
    legend: { bottom: 0 },
    series: [
      {
        name: '年龄分布',
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['50%', '45%'],
        data: ageRes.data.categories.map((name, index) => ({
          name,
          value: ageRes.data.counts[index]
        }))
      }
    ]
  })

  // 窗口变化时图表自适应
  window.addEventListener('resize', handleResize)
}

function handleResize() {
  trendChart && trendChart.resize()
  ageChart && ageChart.resize()
}
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 13px;
  color: #999;
}

.chart-box {
  height: 340px;
}
</style>