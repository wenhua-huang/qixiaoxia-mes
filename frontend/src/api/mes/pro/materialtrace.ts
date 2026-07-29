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
// 正向追溯：从 parent 出发查所有去向
export function traceForward(parentType: string, parentId: number) {
  return request({ url: '/mes/pro/materialtrace/forward', method: 'get', params: { parentType, parentId } })
}
// 反向追溯：从 child 出发查所有来源
export function traceBackward(childType: string, childId: number) {
  return request({ url: '/mes/pro/materialtrace/backward', method: 'get', params: { childType, childId } })
}
// 深度追溯：一次性返回完整链路（替代前端逐跳递归 N+1 查询）
export function traceChain(startType: string, startId: number, direction: 'forward' | 'backward') {
  return request({ url: '/mes/pro/materialtrace/traceChain', method: 'get', params: { startType, startId, direction } })
}
