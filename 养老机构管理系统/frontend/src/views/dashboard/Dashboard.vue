<template>
  <div class="dashboard">
    <!-- 指标卡 -->
    <div class="stat-grid">
      <div v-for="card in statCards" :key="card.label" class="stat-card">
        <div class="stat-top">
          <span class="stat-icon" :style="{ background: card.soft, color: card.color }">
            <el-icon :size="20"><component :is="card.icon" /></el-icon>
          </span>
          <span class="stat-label">{{ card.label }}</span>
        </div>
        <div class="stat-value" :style="{ color: card.color }">{{ card.value }}<span class="stat-unit">{{ card.unit }}</span></div>
        <div class="stat-sub">{{ card.sub }}</div>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="chart-grid">
      <el-card shadow="never" class="chart-card">
        <template #header>
          <div class="chart-title"><i />近 30 天护理 / 探访趋势</div>
        </template>
        <div ref="trendChartRef" class="chart-box"></div>
      </el-card>
      <el-card shadow="never" class="chart-card">
        <template #header>
          <div class="chart-title"><i />老人年龄分布</div>
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
    color: '#2f5d50', soft: '#e8f0ec', sub: `老人总数 ${overview.elderTotal} 人`
  },
  {
    label: '入住率', value: overview.checkInRate, unit: ' %', icon: 'House',
    color: '#d96f3a', soft: '#faeede', sub: `在住房间 ${overview.roomTotal} 间`
  },
  {
    label: '今日护理', value: overview.todayCareCount, unit: ' 次', icon: 'FirstAidKit',
    color: '#b3822b', soft: '#fcf4e7', sub: `今日探访 ${overview.todayVisitCount} 次`
  },
  {
    label: '逾期用药任务', value: overview.overdueTaskCount, unit: ' 项', icon: 'Warning',
    color: '#ad4538', soft: '#faefed', sub: '请提醒护理人员补服 / 补录'
  }
])

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
  // 趋势折线图：暖橘（护理）+ 墨绿（探访）
  const trendRes = await getActivityTrend(30)
  trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    color: ['#d96f3a', '#3f7d6b'],
    tooltip: { trigger: 'axis' },
    legend: { data: ['护理次数', '探访次数'], bottom: 0, icon: 'circle', itemWidth: 8 },
    grid: { left: 40, right: 20, top: 24, bottom: 44 },
    xAxis: { type: 'category', boundaryGap: false, data: trendRes.data.dates },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '护理次数', type: 'line', smooth: true, showSymbol: false, data: trendRes.data.careCounts, areaStyle: { opacity: 0.12 } },
      { name: '探访次数', type: 'line', smooth: true, showSymbol: false, data: trendRes.data.visitCounts, areaStyle: { opacity: 0.12 } }
    ]
  })

  // 年龄分布环形图：暖色系
  const ageRes = await getAgeDistribution()
  ageChart = echarts.init(ageChartRef.value)
  ageChart.setOption({
    color: ['#e08850', '#3f7d6b', '#dcaa4e', '#b9551f'],
    tooltip: { trigger: 'item', formatter: '{b}：{c} 人（{d}%）' },
    legend: { bottom: 0, icon: 'circle', itemWidth: 8 },
    series: [
      {
        name: '年龄分布',
        type: 'pie',
        radius: ['46%', '68%'],
        center: ['50%', '44%'],
        itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 6 },
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
  background: #fff;
  border: 1px solid #efe7d8;
  border-radius: 16px;
  padding: 18px 20px 16px;
  box-shadow: 0 1px 2px rgba(74, 58, 32, 0.03), 0 10px 28px -14px rgba(74, 58, 32, 0.1);
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 10px rgba(74, 58, 32, 0.06), 0 16px 34px -14px rgba(74, 58, 32, 0.16);
}

.stat-top {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stat-icon {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-label {
  font-size: 13px;
  color: var(--ink-2);
  letter-spacing: 1px;
}

.stat-value {
  margin-top: 12px;
  font-size: 30px;
  font-weight: 700;
  font-family: Georgia, 'Times New Roman', serif;
  line-height: 1;
}

.stat-unit {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink-2);
  margin-left: 4px;
}

.stat-sub {
  margin-top: 10px;
  font-size: 12px;
  color: #a89d8a;
}

/* ---------- 图表 ---------- */
.chart-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
  margin-top: 16px;
}

.chart-card {
  min-width: 0;
}

.chart-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: 1px;
}

.chart-title i {
  width: 4px;
  height: 15px;
  border-radius: 2px;
  background: linear-gradient(180deg, #e08a54, #c2571f);
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
