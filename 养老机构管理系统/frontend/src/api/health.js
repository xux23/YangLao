import request from '../utils/request'

/**
 * 健康体征记录相关接口
 */

// 体征记录分页查询
export function getHealthRecords(params) {
  return request.get('/health-records', { params })
}

// 新增体征记录
export function addHealthRecord(data) {
  return request.post('/health-records', data)
}

// 修改体征记录
export function updateHealthRecord(id, data) {
  return request.put(`/health-records/${id}`, data)
}

// 删除体征记录
export function deleteHealthRecord(id) {
  return request.delete(`/health-records/${id}`)
}