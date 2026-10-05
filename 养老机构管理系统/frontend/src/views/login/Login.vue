<template>
  <div class="login-page">
    <div class="login-card">
      <h2 class="login-title">养老机构管理系统</h2>

      <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin">
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
        <el-form-item prop="captchaCode">
          <div class="captcha-row">
            <el-input
              v-model="form.captchaCode"
              placeholder="请输入验证码"
              :prefix-icon="Key"
              maxlength="4"
            />
            <img
              v-if="captchaImage"
              :src="captchaImage"
              class="captcha-img"
              title="看不清？点击刷新"
              alt="验证码"
              @click="loadCaptcha"
            />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="demo-line">
        演示账号：
        <el-link type="primary" :underline="false" @click="fillDemo('admin')">admin（管理员）</el-link>
        <el-divider direction="vertical" />
        <el-link type="primary" :underline="false" @click="fillDemo('nurse01')">nurse01（护理）</el-link>
        <el-divider direction="vertical" />
        <el-link type="primary" :underline="false" @click="fillDemo('family01')">family01（家属）</el-link>
      </div>
      <div class="demo-password">密码均为 123456</div>
    </div>

    <div class="login-footer">Copyright © 2026 养老机构管理系统</div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Key, Lock, User } from '@element-plus/icons-vue'
import { getCaptcha, login } from '../../api/auth'
import { useUserStore } from '../../store/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)
const captchaImage = ref('')
const captchaId = ref('')

const form = reactive({
  username: '',
  password: '',
  captchaCode: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

// 加载图形验证码（点击图片或登录失败后刷新）
async function loadCaptcha() {
  form.captchaCode = ''
  const res = await getCaptcha()
  captchaId.value = res.data.captchaId
  captchaImage.value = res.data.image
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
    const res = await login({
      username: form.username,
      password: form.password,
      captchaId: captchaId.value,
      captchaCode: form.captchaCode
    })
    userStore.setLoginInfo(res.data.token, res.data.user)
    ElMessage.success('登录成功')
    router.push(getHomePath(res.data.user.role))
  } catch (e) {
    // 验证码一次性使用，无论失败原因都换一张
    loadCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #2d3a4b;
  position: relative;
}

.login-card {
  width: 400px;
  padding: 34px 38px 26px;
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.25);
}

.login-title {
  text-align: center;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 26px;
}

.login-btn {
  width: 100%;
}

.captcha-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.captcha-img {
  height: 32px;
  width: 110px;
  border-radius: 4px;
  border: 1px solid var(--line);
  cursor: pointer;
  flex-shrink: 0;
}

.demo-line {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-hint);
}

.demo-password {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-hint);
}

.login-footer {
  position: absolute;
  bottom: 16px;
  width: 100%;
  text-align: center;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
}
</style>
