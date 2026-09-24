import request from '@/utils/request'

// 查询列表
export function listUserWorkstation(query: any) {
  return request({ url: '/mes/pro/userworkstation/list', method: 'get', params: query })
}
// 查询详细
export function getUserWorkstation(recordId: number) { return request({ url: '/mes/pro/userworkstation/' + recordId, method: 'get' }) }
// 新增
export function addUserWorkstation(data: any) { return request({ url: '/mes/pro/userworkstation', method: 'post', data }) }
// 修改
export function updateUserWorkstation(data: any) { return request({ url: '/mes/pro/userworkstation', method: 'put', data }) }
// 删除
export function delUserWorkstation(ids: any) { return request({ url: '/mes/pro/userworkstation/' + ids, method: 'delete' }) }

// 工位选项（批量绑定弹窗用，只含启用工位）
export function workstationOptions() {
  return request({ url: '/mes/pro/userworkstation/workstationOptions', method: 'get' })
}
// 批量绑定（多选人员 × 多选工位）
export function batchBindUserWorkstation(data: { userIds: number[]; workstationIds: number[]; remark?: string }) {
  return request({ url: '/mes/pro/userworkstation/batch', method: 'post', data })
}
