<template>
  <el-container class="layout">
    <!-- 左侧：品牌 + 菜单 -->
    <el-aside width="220px" class="aside">
      <div class="brand">
        <div class="brand-mark">
          <el-icon :size="22"><Sunny /></el-icon>
        </div>
        <div class="brand-name">养老机构管理系统</div>
      </div>

      <nav class="menu">
        <router-link
          v-for="item in menuList"
          :key="item.path"
          :to="item.path"
          class="menu-item"
          :class="{ active: route.path === item.path }"
        >
          <el-icon :size="17"><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
          <i class="active-dot" />
        </router-link>
      </nav>

      <div class="aside-footer">老有所养 · 老有所依</div>
    </el-aside>

    <el-container>
      <!-- 顶部：页面标题 + 用户区 -->
      <el-header class="header">
        <span class="page-title">{{ currentTitle }}</span>
        <div class="header-right">
          <span class="role-tag" :style="{ background: roleMeta.bg, color: roleMeta.color }">
            {{ roleMeta.name }}
          </span>
          <el-dropdown @command="handleCommand">
            <span class="user-chip">
              <span class="avatar" :style="{ background: roleMeta.grad }">
                {{ (userStore.realName || userStore.user?.username || '?').charAt(0) }}
              </span>
              <span class="user-name">{{ userStore.realName || userStore.user?.username }}</span>
              <el-icon :size="12"><ArrowDown /></el-icon>
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
import { ArrowDown, Sunny } from '@element-plus/icons-vue'
import { useUserStore } from '../../store/user'
import { changePassword } from '../../api/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

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

// 角色徽章：颜色随角色区分
const roleMeta = computed(() => ({
  admin: { name: '管理员', bg: '#faeede', color: '#b9551f', grad: 'linear-gradient(135deg,#e08a54,#c2571f)' },
  nurse: { name: '护理人员', bg: '#e8f0ec', color: '#2f5d50', grad: 'linear-gradient(135deg,#4d8273,#2f5d50)' },
  family: { name: '家属', bg: '#eef0f8', color: '#5a6dbf', grad: 'linear-gradient(135deg,#8b9ad6,#5a6dbf)' }
})[userStore.role] || { name: '', bg: '#f1ebdd', color: '#6f675d', grad: '#b3a893' })

const currentTitle = computed(() => route.meta.title || '')

// 退出登录 / 修改密码
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

/* ---------- 侧边栏 ---------- */
.aside {
  display: flex;
  flex-direction: column;
  background: #fbf8f1;
  border-right: 1px solid var(--line);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px;
}

.brand-mark {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #e08a54, #c2571f);
  box-shadow: 0 6px 14px -4px rgba(194, 87, 31, 0.45);
  flex-shrink: 0;
}

.brand-name {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: 1px;
  white-space: nowrap;
}

.menu {
  flex: 1;
  overflow-y: auto;
  padding: 4px 0 10px;
}

.menu-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  height: 42px;
  margin: 2px 12px;
  padding: 0 14px;
  border-radius: 11px;
  font-size: 14px;
  color: #6a6257;
  transition: background 0.2s, color 0.2s;
}

.menu-item:hover {
  background: #f3ecdd;
  color: var(--ink);
}

.menu-item.active {
  background: linear-gradient(90deg, #fbe9d8, #fdf4e9);
  color: var(--brand-deep);
  font-weight: 600;
  box-shadow: inset 0 0 0 1px #f2d9c2;
}

.active-dot {
  position: absolute;
  right: 14px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--brand);
  opacity: 0;
  transition: opacity 0.2s;
}

.menu-item.active .active-dot {
  opacity: 1;
}

.aside-footer {
  padding: 14px 0 18px;
  text-align: center;
  font-size: 11px;
  letter-spacing: 2px;
  color: #c0b5a0;
}

/* ---------- 顶栏 ---------- */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 28px;
  background: rgba(255, 253, 248, 0.75);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--line);
}

.page-title {
  font-family: var(--font-display);
  font-size: 19px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: 1px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.role-tag {
  padding: 3px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.user-chip {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border-radius: 999px;
  transition: background 0.2s;
}

.user-chip:hover {
  background: #f3ecdd;
}

.avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
}

/* ---------- 主内容 ---------- */
.main {
  background: var(--cream);
  padding: 22px 26px 30px;
  overflow: auto;
}
</style>
