import request from '@/utils/request'

// 查询列表
export function listMaterialTrace(query: any) {
  return request({ url: '/mes/pro/materialtrace/list', method: 'get', params: query })
}
// 查询详细
export function getMaterialTrace(traceId: number) { return request({ url: '/mes/pro/materialtrace/' + traceId, method: 'get' }) }
// 新增
export function addMaterialTrace(data: any) { return request({ url: '/mes/pro/materialtrace', method: 'post', data }) }
// 修改
export function updateMaterialTrace(data: any) { return request({ url: '/mes/pro/materialtrace', method: 'put', data }) }
// 删除
export function delMaterialTrace(ids: any) { return request({ url: '/mes/pro/materialtrace/' + ids, method: 'delete' }) }
