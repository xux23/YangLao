import request from '../utils/request'

/**
 * 用药计划与用药任务相关接口
 */

// 新增用药计划
export function addMedicinePlan(data) {
  return request.post('/medicine-plans', data)
}

// 在用药计划列表
export function getMedicinePlans(params) {
  return request.get('/medicine-plans', { params })
}

// 停用用药计划
export function disableMedicinePlan(id) {
  return request.put(`/medicine-plans/${id}/disable`)
}

// 查询某日用药任务（自动生成当日任务 + 逾期扫描）
export function getMedicineTasks(params) {
  return request.get('/medicine-tasks', { params })
}

// 任务确认执行
export function completeMedicineTask(id) {
  return request.put(`/medicine-tasks/${id}/complete`)
}

// 逾期任务列表
export function getOverdueTasks(params) {
  return request.get('/medicine-tasks/overdue', { params })
}