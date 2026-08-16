<template>
  <div>
    <el-card shadow="hover">
      <template #header>
        <span>用药任务</span>
      </template>

      <!-- 查询区 -->
      <el-form inline>
        <el-form-item label="任务日期">
          <el-date-picker v-model="query.date" type="date" value-format="YYYY-MM-DD" style="width: 150px" />
        </el-form-item>
        <el-form-item label="老人">
          <el-select v-model="query.elderId" placeholder="全部老人" clearable filterable style="width: 150px">
            <el-option v-for="e in elderOptions" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="待执行" :value="0" />
            <el-option label="已执行" :value="1" />
            <el-option label="已逾期" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-form-item>
      </el-form>

      <div class="task-tip">
        查询时会自动完成两件事：① 把过期未执行的任务标记为"已逾期"；② 当天没有任务时按最近一天方案自动生成。
      </div>

      <!-- 任务表格 -->
      <el-table :data="tableData" border stripe v-loading="loading" style="margin-top: 10px">
        <el-table-column prop="elderName" label="老人" width="90" />
        <el-table-column prop="medicineName" label="药品名称" min-width="140" />
        <el-table-column prop="dosage" label="剂量" width="110" />
        <el-table-column prop="planDate" label="服药日期" width="110" />
        <el-table-column prop="planTime" label="时间点" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="taskStatusType(row.status)">{{ taskStatusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="confirmTime" label="确认时间" width="160" />
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button
              v-if="isNurse && row.status !== 1"
              link
              type="success"
              @click="handleComplete(row)"
            >确认执行</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 逾期任务提醒 -->
    <el-card shadow="hover" style="margin-top: 16px">
      <template #header>
        <span class="overdue-title">
          <el-icon color="#e53e3e"><Warning /></el-icon>
          逾期任务提醒（{{ overdueList.length }} 条）
        </span>
      </template>
      <el-table :data="overdueList" border stripe size="small">
        <el-table-column prop="elderName" label="老人" width="90" />
        <el-table-column prop="medicineName" label="药品名称" min-width="140" />
        <el-table-column prop="dosage" label="剂量" width="110" />
        <el-table-column prop="planDate" label="应服日期" width="110" />
        <el-table-column prop="planTime" label="时间点" width="90" />
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button v-if="isNurse" link type="success" @click="handleComplete(row)">补服确认</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="overdueList.length === 0" description="暂无逾期任务，请保持按时服药" :image-size="70" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../store/user'
import { getMedicineTasks, completeMedicineTask, getOverdueTasks } from '../../api/medicine'
import { getElders } from '../../api/elder'

const userStore = useUserStore()
const isNurse = userStore.role === 'nurse'

const loading = ref(false)
const tableData = ref([])
const overdueList = ref([])
const elderOptions = ref([])

const query = reactive({
  date: new Date().toISOString().slice(0, 10), // 默认今天
  elderId: null,
  status: null
})

function taskStatusName(status) {
  return { 0: '待执行', 1: '已执行', 2: '已逾期' }[status] || '未知'
}

function taskStatusType(status) {
  return { 0: 'warning', 1: 'success', 2: 'danger' }[status] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const params = { date: query.date, elderId: query.elderId }
    if (query.status !== null && query.status !== undefined && query.status !== '') {
      params.status = query.status
    }
    const res = await getMedicineTasks(params)
    // 前端过滤状态（后端接口按状态筛选时若当天刚生成，会在返回后再次匹配）
    tableData.value = query.status !== null && query.status !== '' && query.status !== undefined
      ? res.data.filter(t => t.status === query.status)
      : res.data
  } finally {
    loading.value = false
  }
}

async function loadOverdue() {
  const res = await getOverdueTasks({ elderId: query.elderId })
  overdueList.value = res.data
}

function handleComplete(row) {
  ElMessageBox.confirm(`确认已完成「${row.elderName}」的「${row.medicineName}」服药吗？`, '确认执行', { type: 'info' })
    .then(async () => {
      await completeMedicineTask(row.id)
      ElMessage.success('已确认执行')
      loadData()
      loadOverdue()
    })
    .catch(() => {})
}

async function loadElders() {
  const res = await getElders({ page: 1, size: 100, status: 1 })
  elderOptions.value = res.data.records
}

function refreshAll() {
  loadData()
  loadOverdue()
}

onMounted(() => {
  loadElders()
  refreshAll()
})
</script>

<style scoped>
.task-tip {
  color: #999;
  font-size: 12px;
  margin-bottom: 4px;
}

.overdue-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #e53e3e;
  font-weight: bold;
}
</style>