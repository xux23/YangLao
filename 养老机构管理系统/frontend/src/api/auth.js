import request from '../utils/request'

/**
 * 认证相关接口
 */

// 获取图形验证码
export function getCaptcha() {
  return request.get('/auth/captcha')
}

// 登录
export function login(data) {
  return request.post('/auth/login', data)
}

// 安全退出（服务端拉黑当前令牌）
export function logout() {
  return request.post('/auth/logout')
}

// 获取当前用户信息
export function getMe() {
  return request.get('/auth/me')
}

// 修改密码
export function changePassword(data) {
  return request.put('/auth/password', data)
}