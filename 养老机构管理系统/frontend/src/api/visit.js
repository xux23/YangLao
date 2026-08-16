import request from '../utils/request'

/**
 * 探访预约相关接口
 */

// 家属提交探访预约
export function addVisit(data) {
  return request.post('/visits', data)
}

// 预约分页查询
export function getVisits(params) {
  return request.get('/visits', { params })
}

// 预约审核：通过 / 驳回
export function auditVisit(id, data) {
  return request.put(`/visits/${id}/audit`, data)
}

// 标记探访完成
export function finishVisit(id) {
  return request.put(`/visits/${id}/finish`)
}