import request from '@/utils/request'

// 查询分切记录列表
export function listSlitting(query: any) {
  return request({ url: '/mes/pro/slitting/list', method: 'get', params: query })
}

// 查询分切记录详情（含子卷列表）
export function getSlitting(slitId: number) {
  return request({ url: '/mes/pro/slitting/' + slitId, method: 'get' })
}

// 执行分切作业（领料出库 + 自动建母卷/子卷 + 库存 + 追溯 + 报工）
export function executeSlitting(data: any) {
  return request({ url: '/mes/pro/slitting/execute', method: 'post', data })
}

// 查询物料在库库存（供前端选领料物料时展示可用批次）
export function listAvailableStock(itemId: number) {
  return request({ url: '/mes/pro/slitting/availableStock', method: 'get', params: { itemId } })
}

// 查询可发料母卷（外协发料时按物料筛选在库纸卷）
export function listAvailableParentRolls(itemId?: number) {
  return request({ url: '/mes/pro/slitting/availableParentRolls', method: 'get', params: { itemId } })
}

// 外协厂商录分切结果（建子卷，状态 ISSUED→SLITTING）
export function recordOutsourceResult(slitId: number, data: { childRolls: any[] }) {
  return request({ url: `/mes/pro/slitting/${slitId}/result`, method: 'post', data })
}

// 我方外协收货（子卷入库 + 母卷消耗 + 报工 + 追溯，状态 SLITTING→RECEIVED）
export function receiveOutsource(slitId: number, data: any) {
  return request({ url: `/mes/pro/slitting/${slitId}/receive`, method: 'post', data })
}
