<template>
  <el-card shadow="hover">
    <template #header>
      <div class="card-header">
        <span>用户管理</span>
        <el-button type="primary" @click="openDialog()">
          <el-icon><Plus /></el-icon>新增用户
        </el-button>
      </div>
    </template>

    <!-- 查询区 -->
    <el-form inline>
      <el-form-item label="用户名">
        <el-input v-model="query.username" placeholder="模糊查询" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="query.role" placeholder="全部" clearable style="width: 130px">
          <el-option label="管理员" value="admin" />
          <el-option label="护理人员" value="nurse" />
          <el-option label="家属" value="family" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 用户表格 -->
    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="realName" label="姓名" width="120" />
      <el-table-column prop="role" label="角色" width="110">
        <template #default="{ row }">
          <el-tag :type="roleTagType(row.role)">{{ roleName(row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" min-width="200">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="warning" @click="handleResetPassword(row)">重置密码</el-button>
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

  <!-- 新增/编辑对话框 -->
  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="480px">
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" :disabled="!!form.id" placeholder="登录账号" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          show-password
          :placeholder="form.id ? '不修改请留空' : '默认 123456，可留空'"
        />
      </el-form-item>
      <el-form-item label="姓名" prop="realName">
        <el-input v-model="form.realName" placeholder="真实姓名" />
      </el-form-item>
      <el-form-item label="角色" prop="role">
        <el-select v-model="form.role" style="width: 100%">
          <el-option label="管理员" value="admin" />
          <el-option label="护理人员" value="nurse" />
          <el-option label="家属" value="family" />
        </el-select>
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="form.phone" placeholder="联系电话" />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
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
import { getUsers, addUser, updateUser, deleteUser, resetPassword } from '../../api/user'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 10,
  username: '',
  role: ''
})

function roleName(role) {
  return { admin: '管理员', nurse: '护理人员', family: '家属' }[role] || role
}

function roleTagType(role) {
  return { admin: 'danger', nurse: 'success', family: 'warning' }[role] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getUsers(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleReset() {
  query.username = ''
  query.role = ''
  loadData()
}

// 弹窗表单
const dialogVisible = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, username: '', password: '', realName: '', role: 'nurse', phone: '', status: 1 })

const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  password: [
    {
      validator: (rule, value, callback) => {
        if (!form.id && !value) {
          callback(new Error('请输入密码'))
        } else if (value && (value.length < 6 || value.length > 20)) {
          callback(new Error('密码长度需为 6~20 位'))
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
    Object.assign(form, { id: row.id, username: row.username, password: '', realName: row.realName, role: row.role, phone: row.phone, status: row.status })
  } else {
    Object.assign(form, { id: null, username: '', password: '', realName: '', role: 'nurse', phone: '', status: 1 })
  }
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  if (form.id) {
    // 编辑时密码留空表示不修改
    const { password, ...data } = form
    await updateUser(form.id, data)
    ElMessage.success('修改成功')
  } else {
    await addUser(form)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadData()
}

function handleResetPassword(row) {
  ElMessageBox.confirm(`确定将用户「${row.username}」的密码重置为 123456 吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await resetPassword(row.id)
      ElMessage.success('重置成功，新密码为 123456')
    })
    .catch(() => {})
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除用户「${row.username}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteUser(row.id)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}

onMounted(loadData)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>