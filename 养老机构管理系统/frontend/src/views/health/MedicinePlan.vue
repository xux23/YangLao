<template>
  <el-card shadow="hover">
    <template #header>
      <div class="card-header">
        <span>用药计划</span>
        <el-button v-if="isNurse" type="primary" @click="openDialog()">
          <el-icon><Plus /></el-icon>新增用药计划
        </el-button>
      </div>
    </template>

    <!-- 老人选择 -->
    <el-form inline>
      <el-form-item label="老人" style="margin-bottom: 0">
        <el-select v-model="query.elderId" placeholder="选择老人" filterable style="width: 180px" @change="loadData">
          <el-option v-for="e in elderOptions" :key="e.id" :label="e.name" :value="e.id" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="loadData">查询</el-button>
    </el-form>

    <el-empty v-if="!query.elderId" description="请先选择老人查看其用药计划" />

    <template v-else>
      <!-- 在用药计划列表：每个时间点一行 -->
      <el-table :data="tableData" border stripe v-loading="loading" style="margin-top: 12px">
        <el-table-column prop="elderName" label="老人" width="90" />
        <el-table-column prop="medicineName" label="药品名称" min-width="150" />
        <el-table-column prop="dosage" label="剂量" width="120" />
        <el-table-column prop="planTime" label="服药时间点" width="120" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button v-if="isNurse" link type="danger" @click="handleDisable(row)">停用</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="plan-tip">
        提示：录入计划后系统自动生成当日用药任务；次日查询任务时自动按此方案延续。
      </div>
    </template>
  </el-card>

  <!-- 新增用药计划对话框 -->
  <el-dialog v-model="dialogVisible" title="新增用药计划" width="520px">
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
      <el-form-item label="老人" prop="elderId">
        <el-select v-model="form.elderId" filterable placeholder="选择老人" style="width: 100%">
          <el-option v-for="e in elderOptions" :key="e.id" :label="e.name" :value="e.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="药品名称" prop="medicineName">
        <el-input v-model="form.medicineName" placeholder="如 硝苯地平缓释片" />
      </el-form-item>
      <el-form-item label="剂量" prop="dosage">
        <el-input v-model="form.dosage" placeholder="如 每次1片" />
      </el-form-item>
      <el-form-item label="服药时间点" prop="times">
        <el-select v-model="form.times" multiple placeholder="选择时间点（可多选）" style="width: 100%">
          <el-option v-for="t in timeOptions" :key="t" :label="t" :value="t" />
        </el-select>
      </el-form-item>
      <div class="plan-tip">保存后将为上述每个时间点生成今日用药任务。</div>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../store/user'
import { getMedicinePlans, addMedicinePlan, disableMedicinePlan } from '../../api/medicine'
import { getElders } from '../../api/elder'

const userStore = useUserStore()
const isNurse = userStore.role === 'nurse'

const loading = ref(false)
const tableData = ref([])
const elderOptions = ref([])

const query = reactive({ elderId: null })

async function loadElders() {
  const res = await getElders({ page: 1, size: 100, status: 1 })
  elderOptions.value = res.data.records
  if (elderOptions.value.length > 0 && !query.elderId) {
    query.elderId = elderOptions.value[0].id
    loadData()
  }
}

async function loadData() {
  if (!query.elderId) return
  loading.value = true
  try {
    const res = await getMedicinePlans({ elderId: query.elderId })
    tableData.value = res.data
  } finally {
    loading.value = false
  }
}

function handleDisable(row) {
  ElMessageBox.confirm(`确定停用「${row.medicineName}」的用药计划吗？今日及以后的任务将被删除。`, '提示', { type: 'warning' })
    .then(async () => {
      await disableMedicinePlan(row.id)
      ElMessage.success('用药计划已停用')
      loadData()
    })
    .catch(() => {})
}

// 新增计划表单
const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({ elderId: null, medicineName: '', dosage: '', times: [] })

const timeOptions = ['06:00', '07:00', '08:00', '09:00', '10:00', '12:00', '14:00', '16:00', '18:00', '20:00', '22:00']

const formRules = {
  elderId: [{ required: true, message: '请选择老人', trigger: 'change' }],
  medicineName: [{ required: true, message: '请输入药品名称', trigger: 'blur' }],
  dosage: [{ required: true, message: '请输入剂量', trigger: 'blur' }],
  times: [{ required: true, type: 'array', min: 1, message: '至少选择一个服药时间点', trigger: 'change' }]
}

function openDialog() {
  Object.assign(form, { elderId: query.elderId, medicineName: '', dosage: '', times: [] })
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  await addMedicinePlan(form)
  ElMessage.success('用药计划已保存，今日任务已生成')
  dialogVisible.value = false
  loadData()
}

onMounted(loadElders)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.plan-tip {
  color: #999;
  font-size: 12px;
  margin-top: 10px;
}
</style>