<template>
  <el-card shadow="hover">
    <template #header>
      <div class="card-header">
        <span>老人档案</span>
        <div>
          <el-button type="primary" @click="openDialog()">
            <el-icon><Plus /></el-icon>新增老人
          </el-button>
          <el-button @click="handleExport">
            <el-icon><Download /></el-icon>名单导出
          </el-button>
        </div>
      </div>
    </template>

    <!-- 查询区 -->
    <el-form inline>
      <el-form-item label="姓名">
        <el-input v-model="query.name" placeholder="模糊查询" clearable style="width: 150px" />
      </el-form-item>
      <el-form-item label="房间号">
        <el-input v-model="query.roomNo" placeholder="如 201" clearable style="width: 120px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="在住" :value="1" />
          <el-option label="已退住" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 老人表格 -->
    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="姓名" width="90" />
      <el-table-column label="性别" width="60">
        <template #default="{ row }">{{ row.gender === 1 ? '男' : '女' }}</template>
      </el-table-column>
      <el-table-column prop="birthday" label="出生日期" width="110" />
      <el-table-column prop="phone" label="联系电话" width="120" />
      <el-table-column label="房间/床位" width="100">
        <template #default="{ row }">
          <span v-if="row.roomNo">{{ row.roomNo }} - {{ row.bedNo }}</span>
          <span v-else class="text-muted">未分配</span>
        </template>
      </el-table-column>
      <el-table-column prop="familyName" label="关联家属" width="100">
        <template #default="{ row }">
          <span v-if="row.familyName">{{ row.familyName }}</span>
          <span v-else class="text-muted">无</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '在住' : '已退住' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="checkinTime" label="入住日期" width="110" />
      <el-table-column label="操作" min-width="260">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="success" @click="openHealthDialog(row)">健康档案</el-button>
          <el-button v-if="row.status === 1" link type="warning" @click="openCheckinDialog(row)">入住登记</el-button>
          <el-button v-if="row.status === 1" link type="danger" @click="handleCheckout(row)">退住</el-button>
          <el-button v-if="isAdmin" link type="danger" @click="handleDelete(row)">删除</el-button>
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

  <!-- 新增/编辑老人对话框 -->
  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑老人' : '新增老人'" width="640px">
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
      <el-row>
        <el-col :span="12">
          <el-form-item label="姓名" prop="name">
            <el-input v-model="form.name" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="性别" prop="gender">
            <el-radio-group v-model="form.gender">
              <el-radio :value="1">男</el-radio>
              <el-radio :value="2">女</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="出生日期">
            <el-date-picker v-model="form.birthday" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="身份证号">
            <el-input v-model="form.idCard" maxlength="18" :placeholder="form.id ? '不修改可留空' : ''" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="联系电话">
            <el-input v-model="form.phone" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="紧急联系人">
            <el-input v-model="form.emergencyContact" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="紧急电话">
            <el-input v-model="form.emergencyPhone" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="关联家属">
            <el-select v-model="form.familyId" placeholder="选择家属账号" clearable style="width: 100%">
              <el-option v-for="u in familyUsers" :key="u.id" :label="`${u.realName}（${u.username}）`" :value="u.id" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="健康概况">
        <el-input v-model="form.healthSummary" type="textarea" :rows="3" placeholder="病史、过敏史、注意事项等" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>

  <!-- 入住登记对话框 -->
  <el-dialog v-model="checkinDialogVisible" title="入住登记" width="420px">
    <el-form ref="checkinFormRef" :model="checkinForm" :rules="checkinRules" label-width="90px">
      <el-form-item label="老人">
        <el-input :model-value="checkinForm.elderName" disabled />
      </el-form-item>
      <el-form-item label="房间号" prop="roomNo">
        <el-input v-model="checkinForm.roomNo" placeholder="如 301" />
      </el-form-item>
      <el-form-item label="床位号" prop="bedNo">
        <el-input v-model="checkinForm.bedNo" placeholder="如 A" />
      </el-form-item>
      <el-form-item label="入住日期">
        <el-date-picker v-model="checkinForm.checkinTime" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="checkinDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleCheckin">确认入住</el-button>
    </template>
  </el-dialog>

  <!-- 健康档案对话框 -->
  <el-dialog v-model="healthDialogVisible" :title="`「${healthForm.elderName}」健康档案`" width="560px">
    <el-form label-width="90px">
      <el-form-item label="健康概况">
        <el-input
          v-model="healthForm.healthSummary"
          type="textarea"
          :rows="6"
          placeholder="病史、过敏史、用药禁忌等"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="healthDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSaveHealth">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../store/user'
import {
  getElders, addElder, updateElder, deleteElder,
  checkinElder, checkoutElder, exportElders,
  getElderHealth, updateElderHealth
} from '../../api/elder'
import { getUsers } from '../../api/user'

const userStore = useUserStore()
const isAdmin = userStore.role === 'admin'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 10,
  name: '',
  roomNo: '',
  status: null
})

async function loadData() {
  loading.value = true
  try {
    const res = await getElders(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleReset() {
  query.name = ''
  query.roomNo = ''
  query.status = null
  loadData()
}

async function handleExport() {
  await exportElders({ name: query.name, status: query.status })
  ElMessage.success('导出成功')
}

// 新增/编辑表单
const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null, name: '', gender: 1, birthday: '', idCard: '', phone: '',
  emergencyContact: '', emergencyPhone: '', familyId: null, healthSummary: ''
})

const formRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }]
}

// 家属账号列表（仅管理员可查）
const familyUsers = ref([])

async function openDialog(row) {
  if (isAdmin && familyUsers.value.length === 0) {
    const res = await getUsers({ page: 1, size: 100, role: 'family' })
    familyUsers.value = res.data.records
  }
  if (row) {
    Object.assign(form, {
      id: row.id, name: row.name, gender: row.gender, birthday: row.birthday,
      idCard: row.idCard, phone: row.phone, emergencyContact: row.emergencyContact,
      emergencyPhone: row.emergencyPhone, familyId: row.familyId, healthSummary: row.healthSummary
    })
  } else {
    Object.assign(form, {
      id: null, name: '', gender: 1, birthday: '', idCard: '', phone: '',
      emergencyContact: '', emergencyPhone: '', familyId: null, healthSummary: ''
    })
  }
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  if (form.id) {
    await updateElder(form.id, form)
    ElMessage.success('修改成功')
  } else {
    await addElder(form)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadData()
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除老人「${row.name}」的档案吗？（仅无业务数据的档案可删除）`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteElder(row.id)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}

// 入住登记
const checkinDialogVisible = ref(false)
const checkinFormRef = ref(null)
const checkinForm = reactive({ elderId: null, elderName: '', roomNo: '', bedNo: '', checkinTime: '' })

const checkinRules = {
  roomNo: [{ required: true, message: '请输入房间号', trigger: 'blur' }],
  bedNo: [{ required: true, message: '请输入床位号', trigger: 'blur' }]
}

function openCheckinDialog(row) {
  Object.assign(checkinForm, {
    elderId: row.id, elderName: row.name, roomNo: row.roomNo || '', bedNo: row.bedNo || '',
    checkinTime: row.checkinTime || ''
  })
  checkinDialogVisible.value = true
}

async function handleCheckin() {
  await checkinFormRef.value.validate()
  await checkinElder(checkinForm.elderId, {
    roomNo: checkinForm.roomNo,
    bedNo: checkinForm.bedNo,
    checkinTime: checkinForm.checkinTime || undefined
  })
  ElMessage.success('入住登记成功')
  checkinDialogVisible.value = false
  loadData()
}

// 退住登记
function handleCheckout(row) {
  ElMessageBox.confirm(`确定为老人「${row.name}」办理退住吗？退住后将释放房间床位。`, '提示', { type: 'warning' })
    .then(async () => {
      await checkoutElder(row.id, { checkoutTime: undefined })
      ElMessage.success('退住登记成功')
      loadData()
    })
    .catch(() => {})
}

// 健康档案
const healthDialogVisible = ref(false)
const healthForm = reactive({ elderId: null, elderName: '', healthSummary: '' })

async function openHealthDialog(row) {
  const res = await getElderHealth(row.id)
  Object.assign(healthForm, { elderId: row.id, elderName: row.name, healthSummary: res.data.healthSummary || '' })
  healthDialogVisible.value = true
}

async function handleSaveHealth() {
  await updateElderHealth(healthForm.elderId, { healthSummary: healthForm.healthSummary })
  ElMessage.success('健康档案已更新')
  healthDialogVisible.value = false
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.text-muted {
  color: #bbb;
  font-size: 12px;
}
</style>