import request from '../utils/request'

/**
 * 统计看板相关接口
 */

// 看板总览数据
export function getOverview() {
  return request.get('/stats/overview')
}

// 老人年龄分布
export function getAgeDistribution() {
  return request.get('/stats/age-distribution')
}

// 近 N 天护理/探访趋势
export function getActivityTrend(days) {
  return request.get('/stats/activity-trend', { params: { days } })
}

// 某老人体征趋势
export function getHealthTrend(id, params) {
  return request.get(`/stats/elder/${id}/health-trend`, { params })
}