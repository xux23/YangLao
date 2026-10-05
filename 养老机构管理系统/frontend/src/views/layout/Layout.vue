<template>
  <el-container class="layout">
    <!-- 左侧：深色侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '210px'" class="aside">
      <div class="brand">
        <span class="brand-mark">养</span>
        <span v-show="!isCollapse" class="brand-name">养老机构管理系统</span>
      </div>

      <el-menu
        :default-active="route.path"
        :collapse="isCollapse"
        :collapse-transition="false"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        class="menu"
      >
        <el-menu-item v-for="item in menuList" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部：折叠按钮 + 面包屑 + 用户区 -->
      <el-header class="header" height="50px">
        <div class="header-left">
          <el-icon class="collapse-btn" :size="18" @click="isCollapse = !isCollapse">
            <Expand v-if="isCollapse" />
            <Fold v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-tag size="small" effect="plain">{{ roleMeta.name }}</el-tag>
          <el-dropdown @command="handleCommand">
            <span class="user-chip">
              <el-avatar :size="30">{{ avatarText }}</el-avatar>
              <span class="user-name">{{ userStore.realName || userStore.user?.username }}</span>
              <el-icon :size="12" color="#909399"><ArrowDown /></el-icon>
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
import { ArrowDown, Expand, Fold } from '@element-plus/icons-vue'
import { useUserStore } from '../../store/user'
import { changePassword, logout as logoutApi } from '../../api/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 菜单折叠
const isCollapse = ref(false)

// 不同角色的菜单（按角色过滤）
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

const roleMeta = computed(() => ({
  admin: { name: '管理员' },
  nurse: { name: '护理人员' },
  family: { name: '家属' }
})[userStore.role] || { name: '用户' })

const currentTitle = computed(() => route.meta.title || '')

const avatarText = computed(() => (userStore.realName || userStore.user?.username || '?').charAt(0))

// 退出登录 / 修改密码
function handleCommand(command) {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
      .then(async () => {
        // 先通知服务端拉黑当前令牌，再清除本地登录态（失败也不阻塞退出）
        try {
          await logoutApi()
        } catch (e) { /* 忽略 */ }
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
    // 旧令牌一并拉黑，强制重新登录
    try {
      await logoutApi()
    } catch (e) { /* 忽略 */ }
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

/* ---------- 侧边栏 ---------- */
.aside {
  display: flex;
  flex-direction: column;
  background: var(--sidebar-bg);
  transition: width 0.2s;
  overflow: hidden;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 50px;
  padding: 0 14px;
  background: #263445;
  white-space: nowrap;
}

.brand-mark {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--sidebar-active);
  border-radius: 4px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
}

.brand-name {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
}

.menu {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  border-right: none;
}

.menu:not(.el-menu--collapse) {
  width: 210px;
}

.menu :deep(.el-menu-item) {
  height: 46px;
  line-height: 46px;
}

.menu :deep(.el-menu-item:hover) {
  background: var(--sidebar-hover-bg);
}

.menu :deep(.el-menu-item.is-active) {
  background: var(--sidebar-hover-bg);
}

/* ---------- 顶栏 ---------- */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: #fff;
  box-shadow: var(--navbar-shadow);
  position: relative;
  z-index: 1;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  color: var(--text-sub);
}

.collapse-btn:hover {
  color: var(--sidebar-active);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-chip {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name {
  font-size: 14px;
  color: var(--text-main);
}

/* ---------- 主内容 ---------- */
.main {
  background: var(--page-bg);
  padding: 16px 20px;
  overflow: auto;
}
</style>
