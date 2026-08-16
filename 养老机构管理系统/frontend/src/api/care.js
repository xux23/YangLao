import request, { downloadFile } from '../utils/request'

/**
 * 护理记录相关接口
 */

// 护理记录分页查询
export function getCareRecords(params) {
  return request.get('/care-records', { params })
}

// 新增护理记录
export function addCareRecord(data) {
  return request.post('/care-records', data)
}

// 修改护理记录
export function updateCareRecord(id, data) {
  return request.put(`/care-records/${id}`, data)
}

// 删除护理记录
export function deleteCareRecord(id) {
  return request.delete(`/care-records/${id}`)
}

// 护理记录导出 Excel
export async function exportCareRecords(params) {
  const blob = await request.get('/care-records/export', { params, responseType: 'blob' })
  downloadFile(blob, '护理记录.xlsx')
}