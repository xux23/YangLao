<template>
  <el-container class="layout">
    <!-- 左侧菜单 -->
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon size="24"><FirstAidKit /></el-icon>
        <span>养老机构管理系统</span>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#1f2d3d"
        text-color="#a7b1c2"
        active-text-color="#409EFF"
      >
        <template v-for="item in menuList" :key="item.path">
          <el-menu-item :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部栏 -->
      <el-header class="header">
        <div class="header-title">{{ currentTitle }}</div>
        <div class="header-right">
          <span class="role-tag">{{ roleName }}</span>
          <el-dropdown @command="handleCommand">
            <span class="user-name">
              {{ userStore.realName || userStore.user.username }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <!-- 修改密码对话框 -->
  <el-dialog v-model="passwordDialogVisible" title="修改密码" width="420px">
    <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="90px">
      <el-form-item label="原密码" prop="oldPassword">
        <el-input v-model="passwordForm.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="passwordForm.newPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="passwordDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="passwordLoading" @click="handleChangePassword">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, FirstAidKit } from '@element-plus/icons-vue'
import { useUserStore } from '../../store/user'
import { changePassword } from '../../api/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 不同角色的菜单
const allMenus = {
  admin: [
    { path: '/dashboard', title: '首页看板', icon: 'DataAnalysis' },
    { path: '/elders', title: '老人档案', icon: 'User' },
    { path: '/care-record', title: '护理记录', icon: 'FirstAidKit' },
    { path: '/health-record', title: '体征记录', icon: 'Monitor' },
    { path: '/medicine-plan', title: '用药计划', icon: 'Box' },
    { path: '/medicine-task', title: '用药任务', icon: 'AlarmClock' },
    { path: '/visit-audit', title: '探访审核', icon: 'ChatLineSquare' },
    { path: '/message', title: '留言反馈', icon: 'Message' },
    { path: '/sys/users', title: '用户管理', icon: 'Setting' },
    { path: '/sys/logs', title: '操作日志', icon: 'Document' }
  ],
  nurse: [
    { path: '/elders', title: '老人档案', icon: 'User' },
    { path: '/care-record', title: '护理记录', icon: 'FirstAidKit' },
    { path: '/health-record', title: '体征记录', icon: 'Monitor' },
    { path: '/medicine-plan', title: '用药计划', icon: 'Box' },
    { path: '/medicine-task', title: '用药任务', icon: 'AlarmClock' },
    { path: '/visit-audit', title: '探访审核', icon: 'ChatLineSquare' },
    { path: '/message', title: '留言反馈', icon: 'Message' }
  ],
  family: [
    { path: '/health-record', title: '老人健康', icon: 'Monitor' },
    { path: '/care-record', title: '护理记录', icon: 'FirstAidKit' },
    { path: '/visit', title: '探访预约', icon: 'ChatLineSquare' },
    { path: '/message', title: '留言反馈', icon: 'Message' }
  ]
}

const menuList = computed(() => allMenus[userStore.role] || [])
const roleName = computed(() => ({
  admin: '管理员',
  nurse: '护理人员',
  family: '家属'
})[userStore.role] || '')

const currentTitle = computed(() => route.meta.title || '')

// 退出登录
function handleCommand(command) {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
      .then(() => {
        userStore.logout()
        router.push('/login')
      })
      .catch(() => {})
  } else if (command === 'password') {
    passwordDialogVisible.value = true
  }
}

// 修改密码
const passwordDialogVisible = ref(false)
const passwordLoading = ref(false)
const passwordFormRef = ref(null)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需为 6~20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function handleChangePassword() {
  await passwordFormRef.value.validate()
  passwordLoading.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordDialogVisible.value = false
    userStore.logout()
    router.push('/login')
  } finally {
    passwordLoading.value = false
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  background-color: #1f2d3d;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  background-color: #1a2533;
}

.aside .el-menu {
  border-right: none;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
}

.header-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.role-tag {
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  color: #2b6cb0;
  background: #e8f1fb;
}

.user-name {
  cursor: pointer;
  display: flex;
  align-items: center;
  color: #333;
}

.main {
  background: #f0f2f5;
  padding: 16px;
  overflow: auto;
}
</style>