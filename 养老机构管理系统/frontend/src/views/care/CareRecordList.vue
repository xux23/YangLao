<template>
  <el-card shadow="hover">
    <template #header>
      <div class="card-header">
        <span>护理记录</span>
        <div v-if="isStaff">
          <el-button type="primary" @click="openDialog()">
            <el-icon><Plus /></el-icon>新增护理记录
          </el-button>
          <el-button @click="handleExport">
            <el-icon><Download /></el-icon>记录导出
          </el-button>
        </div>
      </div>
    </template>

    <!-- 查询区 -->
    <el-form inline>
      <el-form-item v-if="isStaff" label="老人">
        <el-select v-model="query.elderId" placeholder="全部" clearable filterable style="width: 150px">
          <el-option v-for="e in elderOptions" :key="e.id" :label="e.name" :value="e.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="护理项目">
        <el-input v-model="query.planName" placeholder="如 翻身" clearable style="width: 130px" />
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
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div v-if="isFamily" class="family-tip">
      仅显示您关联老人的护理记录，如需了解老人详细情况可留言反馈
    </div>

    <!-- 护理记录表格 -->
    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="elderName" label="老人" width="90" />
      <el-table-column prop="planName" label="护理项目" width="110" />
      <el-table-column prop="planFrequency" label="频次" width="130" />
      <el-table-column prop="careContent" label="护理内容" min-width="180" show-overflow-tooltip />
      <el-table-column prop="nurseName" label="执行人" width="90" />
      <el-table-column prop="careTime" label="执行时间" width="160" />
      <el-table-column prop="remark" label="交接备注" min-width="120" show-overflow-tooltip />
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

  <!-- 新增/编辑护理记录对话框 -->
  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑护理记录' : '新增护理记录'" width="560px">
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
      <el-form-item label="老人" prop="elderId">
        <el-select v-model="form.elderId" filterable placeholder="选择老人" style="width: 100%">
          <el-option v-for="e in elderOptions" :key="e.id" :label="e.name" :value="e.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="护理项目" prop="planName">
        <el-input v-model="form.planName" placeholder="如 翻身、喂饭、洗澡" />
      </el-form-item>
      <el-form-item label="频次">
        <el-input v-model="form.planFrequency" placeholder="如 每2小时一次" />
      </el-form-item>
      <el-form-item label="护理内容" prop="careContent">
        <el-input v-model="form.careContent" type="textarea" :rows="3" placeholder="护理执行情况描述" />
      </el-form-item>
      <el-form-item label="交接备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="交接班信息（选填）" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../store/user'
import { getCareRecords, addCareRecord, updateCareRecord, deleteCareRecord, exportCareRecords } from '../../api/care'
import { getElders } from '../../api/elder'

const userStore = useUserStore()
const isAdmin = userStore.role === 'admin'
const isStaff = userStore.role !== 'family'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const elderOptions = ref([])

const query = reactive({
  page: 1,
  size: 10,
  elderId: null,
  planName: '',
  startTime: '',
  endTime: ''
})

async function loadElders() {
  const res = await getElders({ page: 1, size: 100, status: 1 })
  elderOptions.value = res.data.records
}

async function loadData() {
  loading.value = true
  try {
    const res = await getCareRecords(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleReset() {
  query.elderId = null
  query.planName = ''
  query.startTime = ''
  query.endTime = ''
  loadData()
}

async function handleExport() {
  await exportCareRecords({ elderId: query.elderId, startTime: query.startTime, endTime: query.endTime })
  ElMessage.success('导出成功')
}

// 新增/编辑表单
const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, elderId: null, planName: '', planFrequency: '', careContent: '', remark: '' })

const formRules = {
  elderId: [{ required: true, message: '请选择老人', trigger: 'change' }],
  planName: [{ required: true, message: '请输入护理项目', trigger: 'blur' }],
  careContent: [{ required: true, message: '请输入护理内容', trigger: 'blur' }]
}

function openDialog(row) {
  if (row) {
    Object.assign(form, {
      id: row.id, elderId: row.elderId, planName: row.planName,
      planFrequency: row.planFrequency, careContent: row.careContent, remark: row.remark
    })
  } else {
    Object.assign(form, { id: null, elderId: null, planName: '', planFrequency: '', careContent: '', remark: '' })
  }
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  if (form.id) {
    await updateCareRecord(form.id, form)
    ElMessage.success('修改成功')
  } else {
    await addCareRecord(form)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadData()
}

function handleDelete(row) {
  ElMessageBox.confirm('确定删除这条护理记录吗？（仅当天记录可删除）', '提示', { type: 'warning' })
    .then(async () => {
      await deleteCareRecord(row.id)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}

onMounted(() => {
  loadData()
  if (isStaff) {
    loadElders()
  }
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.family-tip {
  color: #999;
  font-size: 13px;
  margin-bottom: 12px;
}
</style>