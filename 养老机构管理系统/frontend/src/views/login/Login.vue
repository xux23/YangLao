<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-title">
        <h2>养老机构管理系统</h2>
        <p>老人档案 · 护理 · 健康 · 探访 一体化管理</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-tip">演示账号：admin / nurse01 / family01，密码均为 123456</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '../../api/auth'
import { useUserStore } from '../../store/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 登录成功后按角色跳转到对应首页
function getHomePath(role) {
  if (role === 'admin') return '/dashboard'
  if (role === 'family') return '/health-record'
  return '/elders'
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await login({ username: form.username, password: form.password })
    userStore.setLoginInfo(res.data.token, res.data.user)
    ElMessage.success('登录成功')
    router.push(getHomePath(res.data.user.role))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2b6cb0 0%, #48a3c6 100%);
}

.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.2);
}

.login-title {
  text-align: center;
  margin-bottom: 30px;
}

.login-title h2 {
  color: #2b6cb0;
  margin-bottom: 8px;
}

.login-title p {
  color: #999;
  font-size: 13px;
}

.login-btn {
  width: 100%;
}

.login-tip {
  text-align: center;
  color: #999;
  font-size: 12px;
  margin-top: 6px;
}
</style>