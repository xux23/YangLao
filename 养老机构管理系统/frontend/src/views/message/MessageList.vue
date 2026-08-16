<template>
  <el-card shadow="hover">
    <template #header>
      <div class="card-header">
        <span>留言反馈</span>
        <el-button v-if="isFamily" type="primary" @click="openDialog()">
          <el-icon><Plus /></el-icon>发表留言
        </el-button>
      </div>
    </template>

    <!-- 查询区 -->
    <el-form inline>
      <el-form-item v-if="isStaff" label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px" @change="loadData">
          <el-option label="未回复" :value="0" />
          <el-option label="已回复" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="isStaff" label="老人">
        <el-select v-model="query.elderId" placeholder="全部" clearable filterable style="width: 140px" @change="loadData">
          <el-option v-for="e in elderOptions" :key="e.id" :label="e.name" :value="e.id" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="loadData">查询</el-button>
    </el-form>

    <!-- 留言列表 -->
    <el-table :data="tableData" border stripe v-loading="loading" style="margin-top: 12px">
      <el-table-column prop="elderName" label="老人" width="80" />
      <el-table-column prop="familyName" label="家属" width="90" />
      <el-table-column prop="content" label="留言内容" min-width="180" show-overflow-tooltip />
      <el-table-column label="回复" min-width="180">
        <template #default="{ row }">
          <span v-if="row.reply">{{ row.reply }}</span>
          <span v-else class="text-muted">暂未回复</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="留言时间" width="160" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '已回复' : '未回复' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="isStaff" label="操作" width="90">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" link type="primary" @click="openReplyDialog(row)">回复</el-button>
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

  <!-- 家属发表留言对话框 -->
  <el-dialog v-model="dialogVisible" title="发表留言" width="480px">
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
      <el-form-item label="老人">
        <el-input :model-value="myElder ? myElder.name : ''" disabled />
      </el-form-item>
      <el-form-item label="留言内容" prop="content">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="问候老人、意见建议等"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit">发表</el-button>
    </template>
  </el-dialog>

  <!-- 机构回复对话框 -->
  <el-dialog v-model="replyDialogVisible" title="回复留言" width="480px">
    <el-form ref="replyFormRef" :model="replyForm" :rules="replyRules" label-width="90px">
      <el-form-item label="留言内容">
        <el-input :model-value="replyForm.content" type="textarea" :rows="3" disabled />
      </el-form-item>
      <el-form-item label="回复内容" prop="reply">
        <el-input
          v-model="replyForm.reply"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="机构回复内容"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="replyDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleReply">回复</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../store/user'
import { getMessages, addMessage, replyMessage } from '../../api/message'
import { getElders, getMyElder } from '../../api/elder'

const userStore = useUserStore()
const isFamily = userStore.role === 'family'
const isStaff = !isFamily

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const elderOptions = ref([])
const myElder = ref(null)

const query = reactive({
  page: 1,
  size: 10,
  status: null,
  elderId: null
})

async function loadData() {
  loading.value = true
  try {
    const res = await getMessages(query)
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

// 家属发表留言
const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({ content: '' })

const formRules = {
  content: [{ required: true, message: '请输入留言内容', trigger: 'blur' }]
}

function openDialog() {
  form.content = ''
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  await addMessage({ elderId: myElder.value.id, content: form.content })
  ElMessage.success('留言已发表')
  dialogVisible.value = false
  loadData()
}

// 机构回复
const replyDialogVisible = ref(false)
const replyFormRef = ref(null)
const replyForm = reactive({ id: null, content: '', reply: '' })

const replyRules = {
  reply: [{ required: true, message: '请输入回复内容', trigger: 'blur' }]
}

function openReplyDialog(row) {
  Object.assign(replyForm, { id: row.id, content: row.content, reply: '' })
  replyDialogVisible.value = true
}

async function handleReply() {
  await replyFormRef.value.validate()
  await replyMessage(replyForm.id, { reply: replyForm.reply })
  ElMessage.success('回复成功')
  replyDialogVisible.value = false
  loadData()
}

onMounted(() => {
  loadData()
  if (isFamily) {
    getMyElder().then(res => { myElder.value = res.data })
  } else {
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

.text-muted {
  color: #bbb;
}
</style>