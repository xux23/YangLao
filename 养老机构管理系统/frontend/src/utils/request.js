import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 创建 axios 实例，统一配置请求地址与超时时间
const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器：自动附加令牌
service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器：统一处理错误
service.interceptors.response.use(
  response => {
    // 文件流（Excel 导出）直接返回
    if (response.data instanceof Blob) {
      return response.data
    }
    const res = response.data
    if (res.code === 200) {
      return res
    } else if (res.code === 401) {
      // 登录过期：清除登录态并跳转登录页
      handleLogout()
      return Promise.reject(new Error(res.message))
    } else {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
  },
  error => {
    const status = error.response ? error.response.status : 0
    if (status === 401) {
      handleLogout()
    } else {
      const msg = error.response && error.response.data
        ? (error.response.data.message || '网络异常，请稍后重试')
        : '网络异常，请检查后端服务是否启动'
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

// 清除登录态并跳转登录页
function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  ElMessage.warning('登录已过期，请重新登录')
  if (router.currentRoute.value.path !== '/login') {
    router.push('/login')
  }
}

// 通用导出方法：接收 Blob 文件并触发浏览器下载
export function downloadFile(data, fileName) {
  const url = window.URL.createObjectURL(new Blob([data]))
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', fileName)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

export default service