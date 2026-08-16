import request from '../utils/request'

/**
 * 留言反馈相关接口
 */

// 家属发表留言
export function addMessage(data) {
  return request.post('/messages', data)
}

// 留言分页查询
export function getMessages(params) {
  return request.get('/messages', { params })
}

// 机构回复留言
export function replyMessage(id, data) {
  return request.put(`/messages/${id}/reply`, data)
}