<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <div class="brand-mark"><el-icon :size="22"><Sunny /></el-icon></div>
        <h2>养老机构管理系统</h2>
        <p>老有所养 · 老有所依 · 老有所乐</p>
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

      <div class="demo-title">演示账号（点击自动填充，密码均为 123456）</div>
      <div class="demo-chips">
        <button type="button" class="demo-chip" @click="fillDemo('admin')">
          <i class="chip-dot admin" />管理员
        </button>
        <button type="button" class="demo-chip" @click="fillDemo('nurse01')">
          <i class="chip-dot nurse" />护理
        </button>
        <button type="button" class="demo-chip" @click="fillDemo('family01')">
          <i class="chip-dot family" />家属
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Sunny, User } from '@element-plus/icons-vue'
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

// 演示账号一键填充
function fillDemo(username) {
  form.username = username
  form.password = '123456'
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
  background: linear-gradient(150deg, #f8f4ec 0%, #f2e9d8 60%, #ecdfc9 100%);
}

.login-card {
  width: 400px;
  padding: 40px 38px 30px;
  background: #fff;
  border-radius: 22px;
  box-shadow: 0 20px 50px -16px rgba(74, 58, 32, 0.22);
}

.login-brand {
  text-align: center;
  margin-bottom: 26px;
}

.brand-mark {
  width: 46px;
  height: 46px;
  margin: 0 auto 12px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #e08a54, #c2571f);
  box-shadow: 0 8px 18px -6px rgba(194, 87, 31, 0.45);
}

.login-brand h2 {
  font-family: var(--font-display);
  font-size: 21px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: 2px;
}

.login-brand p {
  margin-top: 8px;
  font-size: 12px;
  letter-spacing: 2px;
  color: #a89d8a;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  letter-spacing: 6px;
  border: none;
  background: linear-gradient(135deg, #e08a54, #c2571f);
  box-shadow: 0 10px 20px -8px rgba(194, 87, 31, 0.55);
}

.login-btn:hover,
.login-btn:focus {
  background: linear-gradient(135deg, #d97c42, #ad4d1a);
}

.demo-title {
  text-align: center;
  font-size: 12px;
  color: #a89d8a;
}

.demo-chips {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 10px;
}

.demo-chip {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: #faf7f0;
  font-size: 12px;
  color: var(--ink-2);
  cursor: pointer;
  transition: all 0.2s;
}

.demo-chip:hover {
  border-color: #e8c9ae;
  background: var(--brand-soft);
  color: var(--brand-deep);
}

.chip-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}
.chip-dot.admin { background: #d96f3a; }
.chip-dot.nurse { background: #2f5d50; }
.chip-dot.family { background: #5a6dbf; }
</style>
