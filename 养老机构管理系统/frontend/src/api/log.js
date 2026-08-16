import request from '../utils/request'

/**
 * 操作日志相关接口（管理员）
 */

// 日志分页查询
export function getLogs(params) {
  return request.get('/logs', { params })
}