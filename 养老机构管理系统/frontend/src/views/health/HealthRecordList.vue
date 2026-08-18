<template>
  <div>
    <!-- 家属：关联老人信息卡片 -->
    <el-card v-if="isFamily && myElder" shadow="hover" class="elder-card">
      <div class="elder-info">
        <el-avatar :size="56" class="elder-avatar">{{ myElder.name.charAt(0) }}</el-avatar>
        <div>
          <div class="elder-name">{{ myElder.name }} <el-tag size="small" type="success">在住</el-tag></div>
          <div class="elder-detail">
            房间 {{ myElder.roomNo }} - {{ myElder.bedNo }} ｜ 入住日期 {{ myElder.checkinTime }}
          </div>
          <div class="elder-health">健康概况：{{ myElder.healthSummary || '暂无记录' }}</div>
        </div>
      </div>
    </el-card>

    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>体征记录</span>
          <div v-if="isStaff">
            <el-button type="primary" @click="openDialog()">
              <el-icon><Plus /></el-icon>新增体征记录
            </el-button>
          </div>
        </div>
      </template>

      <!-- 查询区 -->
      <el-form inline>
        <el-form-item v-if="isStaff" label="老人">
          <el-select v-model="query.elderId" placeholder="选择老人" clearable filterable style="width: 150px"
                     @change="handleElderChange">
            <el-option v-for="e in elderOptions" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="query.startTime"
            type="datetime"
            placeholder="开始时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 190px"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="query.endTime"
            type="datetime"
            placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 190px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>

      <!-- 体征趋势图 -->
      <el-card shadow="never" class="trend-card">
        <div class="trend-header">
          <span>体征趋势（近 30 天）</span>
          <el-radio-group v-model="metric" size="small" @change="loadTrend">
            <el-radio-button value="bloodPressure">血压</el-radio-button>
            <el-radio-button value="heartRate">心率</el-radio-button>
            <el-radio-button value="temperature">体温</el-radio-button>
            <el-radio-button value="bloodSugar">血糖</el-radio-button>
          </el-radio-group>
        </div>
        <div v-if="showChart" ref="trendChartRef" class="chart-box"></div>
        <el-empty v-else description="请先选择老人查看趋势图" :image-size="80" />
      </el-card>

      <!-- 体征记录表格 -->
      <el-table :data="tableData" border stripe v-loading="loading" style="margin-top: 16px">
        <el-table-column prop="elderName" label="老人" width="90" />
        <el-table-column prop="bloodPressure" label="血压(mmHg)" width="110" />
        <el-table-column prop="heartRate" label="心率(次/分)" width="100" />
        <el-table-column prop="temperature" label="体温(℃)" width="100" />
        <el-table-column prop="bloodSugar" label="血糖(mmol/L)" width="110" />
        <el-table-column prop="recordTime" label="测量时间" width="160" />
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column v-if="isStaff" label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 新增/编辑体征记录对话框 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑体征记录' : '新增体征记录'" width="520px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="老人" prop="elderId">
          <el-select v-model="form.elderId" filterable placeholder="选择老人" style="width: 100%">
            <el-option v-for="e in elderOptions" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="血压">
          <el-input v-model="form.bloodPressure" placeholder="如 128/82" style="width: 150px" />
        </el-form-item>
        <el-form-item label="心率">
          <el-input-number v-model="form.heartRate" :min="0" :max="250" placeholder="次/分" style="width: 150px" />
        </el-form-item>
        <el-form-item label="体温">
          <el-input-number v-model="form.temperature" :min="30" :max="45" :precision="1" :step="0.1" placeholder="℃" style="width: 150px" />
        </el-form-item>
        <el-form-item label="血糖">
          <el-input-number v-model="form.bloodSugar" :min="0" :max="40" :precision="1" :step="0.1" placeholder="mmol/L" style="width: 150px" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { useUserStore } from '../../store/user'
import { getHealthRecords, addHealthRecord, updateHealthRecord, deleteHealthRecord } from '../../api/health'
import { getElders, getMyElder } from '../../api/elder'
import { getHealthTrend } from '../../api/stats'

const userStore = useUserStore()
const isStaff = userStore.role !== 'family'
const isFamily = userStore.role === 'family'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const elderOptions = ref([])
const myElder = ref(null)

const query = reactive({
  page: 1,
  size: 10,
  elderId: isStaff ? null : null,
  startTime: '',
  endTime: ''
})

const currentElderId = ref(null)
const metric = ref('bloodPressure')
const showChart = ref(false)
const trendChartRef = ref(null)
let trendChart = null

async function loadData() {
  loading.value = true
  try {
    const res = await getHealthRecords(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

// 家属：获取关联老人信息
async function loadMyElder() {
  const res = await getMyElder()
  myElder.value = res.data
  currentElderId.value = res.data.id
  showChart.value = true
  loadTrend()
}

// 机构：加载老人下拉框
async function loadElders() {
  const res = await getElders({ page: 1, size: 100, status: 1 })
  elderOptions.value = res.data.records
}

function handleElderChange(id) {
  currentElderId.value = id
  showChart.value = !!id
  if (id) {
    loadTrend()
  }
}

// 体征趋势图
async function loadTrend() {
  if (!currentElderId.value) return
  const res = await getHealthTrend(currentElderId.value, { days: 30, metric: metric.value })
  renderTrend(res.data.dates, res.data.values)
}

function renderTrend(dates, values) {
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  // 血压是 "收缩压/舒张压" 字符串，拆成两条线；其他指标是数值
  let series = []
  if (metric.value === 'bloodPressure') {
    const systolic = values.map(v => (v ? Number(v.split('/')[0]) : null))
    const diastolic = values.map(v => (v ? Number(v.split('/')[1]) : null))
    series = [
      { name: '收缩压', type: 'line', smooth: true, data: systolic },
      { name: '舒张压', type: 'line', smooth: true, data: diastolic }
    ]
  } else {
    series = [{ name: metricName[metric.value], type: 'line', smooth: true, data: values }]
  }
  trendChart.setOption({
    color: ['#d96f3a', '#3f7d6b'],
    tooltip: { trigger: 'axis' },
    legend: series.length > 1 ? { data: ['收缩压', '舒张压'], icon: 'circle', itemWidth: 8 } : undefined,
    grid: { left: 45, right: 20, top: 40, bottom: 30 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLine: { lineStyle: { color: '#d8ceba' } },
      axisLabel: { color: '#8b8175' }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#efe8da', type: 'dashed' } },
      axisLabel: { color: '#8b8175' }
    },
    series: series.map((s, i) => ({
      ...s,
      symbol: 'circle',
      symbolSize: 6,
      showSymbol: false,
      lineStyle: { width: 3 },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: i === 0 ? 'rgba(217,111,58,0.2)' : 'rgba(63,125,107,0.18)' },
            { offset: 1, color: 'rgba(255,255,255,0)' }
          ]
        }
      }
    }))
  }, true)
}

const metricName = {
  heartRate: '心率(次/分)',
  temperature: '体温(℃)',
  bloodSugar: '血糖(mmol/L)'
}

function handleResize() {
  trendChart && trendChart.resize()
}

// 新增/编辑表单
const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null, elderId: null, bloodPressure: '', heartRate: null, temperature: null, bloodSugar: null, remark: ''
})

const formRules = {
  elderId: [{ required: true, message: '请选择老人', trigger: 'change' }],
  bloodPressure: [
    {
      validator: (rule, value, callback) => {
        if (!value && !form.heartRate && !form.temperature && !form.bloodSugar) {
          callback(new Error('至少填写一项体征数据'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

function openDialog(row) {
  if (row) {
    Object.assign(form, {
      id: row.id, elderId: row.elderId, bloodPressure: row.bloodPressure || '',
      heartRate: row.heartRate, temperature: row.temperature, bloodSugar: row.bloodSugar, remark: row.remark
    })
  } else {
    Object.assign(form, {
      id: null, elderId: null, bloodPressure: '', heartRate: null, temperature: null, bloodSugar: null, remark: ''
    })
  }
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  // 空字符串转为 null，避免后端报错
  const data = {
    ...form,
    bloodPressure: form.bloodPressure || null,
    heartRate: form.heartRate,
    temperature: form.temperature,
    bloodSugar: form.bloodSugar
  }
  if (form.id) {
    await updateHealthRecord(form.id, data)
    ElMessage.success('修改成功')
  } else {
    await addHealthRecord(data)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadData()
}

function handleDelete(row) {
  ElMessageBox.confirm('确定删除这条体征记录吗？', '提示', { type: 'warning' })
    .then(async () => {
      await deleteHealthRecord(row.id)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}

onMounted(() => {
  loadData()
  if (isFamily) {
    loadMyElder()
  } else {
    loadElders()
    handleElderChange(query.elderId)
  }
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart && trendChart.dispose()
})
</script>

<style scoped>
.elder-card {
  margin-bottom: 16px;
}

.elder-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.elder-avatar {
  background: #2b6cb0;
  font-size: 22px;
}

.elder-name {
  font-size: 18px;
  font-weight: bold;
}

.elder-detail {
  color: #666;
  font-size: 13px;
  margin: 4px 0;
}

.elder-health {
  color: #999;
  font-size: 13px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.trend-card {
  margin-bottom: 4px;
}

.trend-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-weight: bold;
}

.chart-box {
  height: 300px;
}
</style>