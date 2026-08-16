<template>
  <el-card shadow="hover">
    <template #header>
      <span>探访审核</span>
    </template>

    <!-- 状态筛选 -->
    <el-form inline>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px" @change="loadData">
          <el-option label="待审核" :value="0" />
          <el-option label="已通过" :value="1" />
          <el-option label="已驳回" :value="2" />
          <el-option label="已完成" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item label="老人">
        <el-select v-model="query.elderId" placeholder="全部" clearable filterable style="width: 140px" @change="loadData">
          <el-option v-for="e in elderOptions" :key="e.id" :label="e.name" :value="e.id" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="loadData">查询</el-button>
    </el-form>

    <!-- 预约列表 -->
    <el-table :data="tableData" border stripe v-loading="loading" style="margin-top: 12px">
      <el-table-column prop="elderName" label="老人" width="90" />
      <el-table-column prop="familyName" label="家属" width="90" />
      <el-table-column prop="visitDate" label="探访日期" width="110" />
      <el-table-column prop="visitTime" label="探访时段" width="150" />
      <el-table-column prop="persons" label="人数" width="70" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusName(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="auditRemark" label="审核意见" min-width="130" show-overflow-tooltip />
      <el-table-column prop="createTime" label="提交时间" width="160" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <template v-if="row.status === 0">
            <el-button link type="success" @click="handleAudit(row, 1, '同意探望')">通过</el-button>
            <el-button link type="danger" @click="openAuditDialog(row)">驳回</el-button>
          </template>
          <el-button v-else-if="row.status === 1" link type="primary" @click="handleFinish(row)">标记完成</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 16px; justify-content: flex-end"
      @current-change="loadData"
    />
  </el-card>

  <!-- 驳回对话框 -->
  <el-dialog v-model="auditDialogVisible" title="驳回预约" width="440px">
    <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="90px">
      <el-form-item label="老人/家属">
        <el-input :model-value="`${auditForm.elderName} / ${auditForm.familyName}`" disabled />
      </el-form-item>
      <el-form-item label="驳回原因" prop="auditRemark">
        <el-input v-model="auditForm.auditRemark" type="textarea" :rows="3" placeholder="请填写驳回原因，家属可见" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="auditDialogVisible = false">取消</el-button>
      <el-button type="danger" @click="handleReject">确认驳回</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getVisits, auditVisit, finishVisit } from '../../api/visit'
import { getElders } from '../../api/elder'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const elderOptions = ref([])

const query = reactive({
  page: 1,
  size: 10,
  status: null,
  elderId: null
})

function statusName(status) {
  return { 0: '待审核', 1: '已通过', 2: '已驳回', 3: '已完成' }[status] || '未知'
}

function statusType(status) {
  return { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }[status] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getVisits(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadElders() {
  const res = await getElders({ page: 1, size: 100 })
  elderOptions.value = res.data.records
}

function handleAudit(row, status, remark) {
  ElMessageBox.confirm(`确定${status === 1 ? '通过' : '驳回'}「${row.familyName}」对「${row.elderName}」的探访预约吗？`, '审核', { type: 'warning' })
    .then(async () => {
      await auditVisit(row.id, { status, auditRemark: remark })
      ElMessage.success('审核完成')
      loadData()
    })
    .catch(() => {})
}

function handleFinish(row) {
  ElMessageBox.confirm(`确定将「${row.familyName}」的本次探访标记为已完成吗？`, '提示', { type: 'info' })
    .then(async () => {
      await finishVisit(row.id)
      ElMessage.success('已标记完成')
      loadData()
    })
    .catch(() => {})
}

// 驳回对话框
const auditDialogVisible = ref(false)
const auditFormRef = ref(null)
const auditForm = reactive({ id: null, elderName: '', familyName: '', auditRemark: '' })

const auditRules = {
  auditRemark: [{ required: true, message: '请填写驳回原因', trigger: 'blur' }]
}

function openAuditDialog(row) {
  Object.assign(auditForm, { id: row.id, elderName: row.elderName, familyName: row.familyName, auditRemark: '' })
  auditDialogVisible.value = true
}

async function handleReject() {
  await auditFormRef.value.validate()
  await auditVisit(auditForm.id, { status: 2, auditRemark: auditForm.auditRemark })
  ElMessage.success('已驳回')
  auditDialogVisible.value = false
  loadData()
}

onMounted(() => {
  loadElders()
  loadData()
})
</script>