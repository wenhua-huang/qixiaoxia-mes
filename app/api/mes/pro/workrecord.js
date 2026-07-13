import request from '@/utils/request'

// 上工打卡（body: workstationId 必填，可选 taskId/workorderId）
export function clockIn(data) {
  return request({ url: '/mes/pro/workrecord/clockIn', method: 'put', data: data })
}

// 下工结算（body 可传 recordId，不传则按当前用户结算）
export function clockOut(data) {
  return request({ url: '/mes/pro/workrecord/clockOut', method: 'put', data: data || {} })
}

// 查当前用户在岗会话（进页面判上工/下工按钮）
export function getActiveSession() {
  return request({ url: '/mes/pro/workrecord/activeSession', method: 'get' })
}

// 查当前用户的绑定工位（绑定优先，快捷选择）
export function getMyWorkstations() {
  return request({ url: '/mes/pro/workrecord/myWorkstations', method: 'get' })
}

// 按编码查工作站（扫码/手输编码后解析真实工位）
export function resolveWorkstation(workstationCode) {
  return request({ url: '/mes/pro/workrecord/resolveWorkstation', method: 'get', params: { workstationCode } })
}

// 打卡历史列表（分页）
export function listWorkrecord(query) {
  return request({ url: '/mes/pro/workrecord/list', method: 'get', params: query })
}

// 我的打卡历史（分页，固定按当前登录用户过滤）
export function listMyHistory(query) {
  return request({ url: '/mes/pro/workrecord/myHistory', method: 'get', params: query })
}
