import request from '../utils/request'

/**
 * 用户管理相关接口（管理员）
 */

// 用户分页查询
export function getUsers(params) {
  return request.get('/users', { params })
}

// 新增用户
export function addUser(data) {
  return request.post('/users', data)
}

// 修改用户
export function updateUser(id, data) {
  return request.put(`/users/${id}`, data)
}

// 删除用户
export function deleteUser(id) {
  return request.delete(`/users/${id}`)
}

// 重置密码为 123456
export function resetPassword(id) {
  return request.put(`/users/${id}/password`)
}