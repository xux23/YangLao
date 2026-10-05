<template>
  <div class="dashboard">
    <!-- 指标卡 -->
    <div class="stat-grid">
      <el-card v-for="card in statCards" :key="card.label" shadow="never">
        <div class="stat-card">
          <div class="stat-info">
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-value">
              {{ card.value }}<span class="stat-unit">{{ card.unit }}</span>
            </div>
            <div class="stat-sub">{{ card.sub }}</div>
          </div>
          <div class="stat-icon" :style="{ background: card.soft, color: card.color }">
            <el-icon :size="22"><component :is="card.icon" /></el-icon>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 图表区 -->
    <div class="chart-grid">
      <el-card shadow="never">
        <template #header>
          <div class="chart-title">近 30 天护理 / 探访趋势</div>
        </template>
        <div ref="trendChartRef" class="chart-box"></div>
      </el-card>
      <el-card shadow="never">
        <template #header>
          <div class="chart-title">老人年龄分布</div>
        </template>
        <div ref="ageChartRef" class="chart-box"></div>
      </el-card>
    </div>
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
  {
    label: '在住老人', value: overview.inHouse, unit: ' 人', icon: 'User',
    color: '#409eff', soft: '#ecf5ff', sub: `老人总数 ${overview.elderTotal} 人`
  },
  {
    label: '入住率', value: overview.checkInRate, unit: ' %', icon: 'House',
    color: '#67c23a', soft: '#f0f9eb', sub: `在住房间 ${overview.roomTotal} 间`
  },
  {
    label: '今日护理', value: overview.todayCareCount, unit: ' 次', icon: 'FirstAidKit',
    color: '#e6a23c', soft: '#fdf6ec', sub: `今日探访 ${overview.todayVisitCount} 次`
  },
  {
    label: '逾期用药任务', value: overview.overdueTaskCount, unit: ' 项', icon: 'Warning',
    color: '#f56c6c', soft: '#fef0f0', sub: '请提醒护理人员补服 / 补录'
  }
])

// ECharts 坐标轴的通用配色，两张图共用
const axisCommon = {
  axisLine: { lineStyle: { color: '#dcdfe6' } },
  axisLabel: { color: '#909399' },
  axisTick: { show: false }
}

onMounted(async () => {
  await loadOverview()
  await loadCharts()
})

onUnmounted(() => {
  // 组件销毁时释放图表实例，防止内存泄漏
  trendChart && trendChart.dispose()
  ageChart && ageChart.dispose()
  window.removeEventListener('resize', handleResize)
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
    color: ['#409eff', '#67c23a'],
    tooltip: { trigger: 'axis' },
    legend: { data: ['护理次数', '探访次数'], bottom: 0, icon: 'circle', itemWidth: 8, textStyle: { color: '#606266' } },
    grid: { left: 40, right: 20, top: 24, bottom: 44 },
    xAxis: { type: 'category', boundaryGap: false, data: trendRes.data.dates, ...axisCommon },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#e4e7ed' } }, ...axisCommon },
    series: [
      { name: '护理次数', type: 'line', smooth: true, showSymbol: false, data: trendRes.data.careCounts, areaStyle: { opacity: 0.12 } },
      { name: '探访次数', type: 'line', smooth: true, showSymbol: false, data: trendRes.data.visitCounts, areaStyle: { opacity: 0.12 } }
    ]
  })

  // 年龄分布环形图
  const ageRes = await getAgeDistribution()
  ageChart = echarts.init(ageChartRef.value)
  ageChart.setOption({
    color: ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399'],
    tooltip: { trigger: 'item', formatter: '{b}：{c} 人（{d}%）' },
    legend: { bottom: 0, icon: 'circle', itemWidth: 8, textStyle: { color: '#606266' } },
    series: [
      {
        name: '年龄分布',
        type: 'pie',
        radius: ['45%', '68%'],
        center: ['50%', '44%'],
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
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
/* ---------- 指标卡 ---------- */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.stat-label {
  font-size: 13px;
  color: var(--text-hint);
}

.stat-value {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 600;
  line-height: 1;
  color: var(--text-main);
}

.stat-unit {
  font-size: 13px;
  font-weight: 400;
  color: var(--text-hint);
  margin-left: 2px;
}

.stat-sub {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-hint);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ---------- 图表 ---------- */
.chart-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
  margin-top: 16px;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-main);
}

.chart-box {
  height: 340px;
}

@media (max-width: 1100px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
