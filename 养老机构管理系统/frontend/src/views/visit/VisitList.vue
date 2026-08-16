<template>
  <el-card shadow="hover">
    <template #header>
      <div class="card-header">
        <span>探访预约</span>
        <el-button type="primary" @click="openDialog()">
          <el-icon><Plus /></el-icon>提交预约
        </el-button>
      </div>
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
      <el-button type="primary" @click="loadData">查询</el-button>
    </el-form>

    <!-- 我的预约列表 -->
    <el-table :data="tableData" border stripe v-loading="loading" style="margin-top: 12px">
      <el-table-column prop="elderName" label="老人" width="90" />
      <el-table-column prop="visitDate" label="探访日期" width="120" />
      <el-table-column prop="visitTime" label="探访时段" width="160" />
      <el-table-column prop="persons" label="人数" width="70" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusName(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="auditRemark" label="审核意见" min-width="140" show-overflow-tooltip />
      <el-table-column prop="createTime" label="提交时间" width="160" />
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

  <!-- 提交预约对话框 -->
  <el-dialog v-model="dialogVisible" title="提交探访预约" width="480px">
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
      <el-form-item label="老人">
        <el-input :model-value="myElder ? myElder.name : ''" disabled placeholder="加载中..." />
      </el-form-item>
      <el-form-item label="探访日期" prop="visitDate">
        <el-date-picker
          v-model="form.visitDate"
          type="date"
          value-format="YYYY-MM-DD"
          :disabled-date="disabledDate"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="探访时段" prop="visitTime">
        <el-select v-model="form.visitTime" placeholder="选择时段" style="width: 100%">
          <el-option label="上午 9:00-11:00" value="上午 9:00-11:00" />
          <el-option label="下午 14:00-16:00" value="下午 14:00-16:00" />
          <el-option label="晚上 18:00-20:00" value="晚上 18:00-20:00" />
        </el-select>
      </el-form-item>
      <el-form-item label="探访人数" prop="persons">
        <el-input-number v-model="form.persons" :min="1" :max="5" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="如 带水果探望（选填）" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit">提交</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getVisits, addVisit } from '../../api/visit'
import { getMyElder } from '../../api/elder'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const myElder = ref(null)

const query = reactive({
  page: 1,
  size: 10,
  status: null
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

async function loadMyElder() {
  const res = await getMyElder()
  myElder.value = res.data
}

// 提交预约表单
const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({ visitDate: '', visitTime: '', persons: 1, remark: '' })

const formRules = {
  visitDate: [{ required: true, message: '请选择探访日期', trigger: 'change' }],
  visitTime: [{ required: true, message: '请选择探访时段', trigger: 'change' }],
  persons: [{ required: true, message: '请填写探访人数', trigger: 'blur' }]
}

// 不能选今天之前的日期
function disabledDate(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date < today
}

function openDialog() {
  Object.assign(form, { visitDate: '', visitTime: '', persons: 1, remark: '' })
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  await addVisit({
    elderId: myElder.value.id,
    visitDate: form.visitDate,
    visitTime: form.visitTime,
    persons: form.persons,
    remark: form.remark
  })
  ElMessage.success('预约已提交，等待机构审核')
  dialogVisible.value = false
  loadData()
}

onMounted(() => {
  loadData()
  loadMyElder()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>