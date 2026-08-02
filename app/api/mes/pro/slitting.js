import request from '@/utils/request'

// 分切记录列表（厂商账号后端自动按 vendorId 过滤）
export function listSlitting(query) {
  return request({ url: '/mes/pro/slitting/list', method: 'get', params: query })
}

// 分切记录详情（含母卷+子卷列表）
export function getSlitting(slitId) {
  return request({ url: '/mes/pro/slitting/' + slitId, method: 'get' })
}

// 执行分切（厂内+外协统一入口）
export function executeSlitting(data) {
  return request({ url: '/mes/pro/slitting/execute', method: 'post', data: data })
}

// 查询可发料母卷（外协发料按物料筛选在库纸卷）
export function listAvailableParentRolls(itemId) {
  return request({ url: '/mes/pro/slitting/availableParentRolls', method: 'get', params: { itemId: itemId } })
}

// 厂商录分切结果（建子卷，状态 ISSUED→SLITTING）
export function recordOutsourceResult(slitId, data) {
  return request({ url: '/mes/pro/slitting/' + slitId + '/result', method: 'post', data: data })
}

// 我方外协收货（子卷入库+母卷消耗+报工+追溯，状态 SLITTING→RECEIVED）
export function receiveOutsource(slitId, data) {
  return request({ url: '/mes/pro/slitting/' + slitId + '/receive', method: 'post', data: data })
}

// 查询全部外协厂商（建外协单选厂商用）
export function listOutsourceVendor() {
  return request({ url: '/mes/md/vendor/listAll', method: 'get' })
}
