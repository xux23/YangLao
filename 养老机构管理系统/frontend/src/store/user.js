import { defineStore } from 'pinia'

/**
 * 用户状态：token 与用户信息存 localStorage，刷新页面不丢失登录态
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || '{}')
  }),
  getters: {
    role: state => state.user.role || '',
    realName: state => state.user.realName || ''
  },
  actions: {
    // 登录成功后保存登录态
    setLoginInfo(token, user) {
      this.token = token
      this.user = user
      localStorage.setItem('token', token)
      localStorage.setItem('user', JSON.stringify(user))
    },
    // 退出登录，清空登录态
    logout() {
      this.token = ''
      this.user = {}
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})