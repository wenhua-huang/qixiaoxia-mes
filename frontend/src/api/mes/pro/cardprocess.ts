import request from '@/utils/request'

// 查询列表
export function listCardProcess(query: any) {
  return request({ url: '/mes/pro/cardprocess/list', method: 'get', params: query })
}
// 查询详细
export function getCardProcess(recordId: number) { return request({ url: '/mes/pro/cardprocess/' + recordId, method: 'get' }) }
// 新增
export function addCardProcess(data: any) { return request({ url: '/mes/pro/cardprocess', method: 'post', data }) }
// 修改
export function updateCardProcess(data: any) { return request({ url: '/mes/pro/cardprocess', method: 'put', data }) }
// 删除
export function delCardProcess(ids: any) { return request({ url: '/mes/pro/cardprocess/' + ids, method: 'delete' }) }
// 根据流转卡ID查询工序记录列表
export function listCardProcessByCardId(cardId: number) { return request({ url: '/mes/pro/cardprocess/listByCardId/' + cardId, method: 'get' }) }
