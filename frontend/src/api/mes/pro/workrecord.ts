import request from '@/utils/request'

// 查询列表
export function listWorkrecord(query: any) {
  return request({ url: '/mes/pro/workrecord/list', method: 'get', params: query })
}
// 查询详细
export function getWorkrecord(recordId: number) { return request({ url: '/mes/pro/workrecord/' + recordId, method: 'get' }) }
// 新增
export function addWorkrecord(data: any) { return request({ url: '/mes/pro/workrecord', method: 'post', data }) }
// 修改
export function updateWorkrecord(data: any) { return request({ url: '/mes/pro/workrecord', method: 'put', data }) }
// 删除
export function delWorkrecord(ids: any) { return request({ url: '/mes/pro/workrecord/' + ids, method: 'delete' }) }
