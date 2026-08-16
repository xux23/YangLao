import request, { downloadFile } from '../utils/request'

/**
 * 老人档案相关接口
 */

// 老人分页查询
export function getElders(params) {
  return request.get('/elders', { params })
}

// 家属查看自己关联的老人
export function getMyElder() {
  return request.get('/elders/my')
}

// 老人详情
export function getElder(id) {
  return request.get(`/elders/${id}`)
}

// 新增老人
export function addElder(data) {
  return request.post('/elders', data)
}

// 修改老人
export function updateElder(id, data) {
  return request.put(`/elders/${id}`, data)
}

// 删除老人
export function deleteElder(id) {
  return request.delete(`/elders/${id}`)
}

// 入住登记
export function checkinElder(id, data) {
  return request.post(`/elders/${id}/checkin`, data)
}

// 退住登记
export function checkoutElder(id, data) {
  return request.post(`/elders/${id}/checkout`, data)
}

// 老人名单导出 Excel
export async function exportElders(params) {
  const blob = await request.get('/elders/export', { params, responseType: 'blob' })
  downloadFile(blob, '老人名单.xlsx')
}

// 查询老人健康档案
export function getElderHealth(id) {
  return request.get(`/elders/${id}/health`)
}

// 修改老人健康档案
export function updateElderHealth(id, data) {
  return request.put(`/elders/${id}/health`, data)
}