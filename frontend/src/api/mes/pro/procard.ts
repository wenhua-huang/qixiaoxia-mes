import request from '@/utils/request'

// 查询列表
export function listProcard(query: any) {
  return request({ url: '/mes/pro/procard/list', method: 'get', params: query })
}
// 查询详细
export function getProcard(cardId: number) { return request({ url: '/mes/pro/procard/' + cardId, method: 'get' }) }
// 新增
export function addProcard(data: any) { return request({ url: '/mes/pro/procard', method: 'post', data }) }
// 修改
export function updateProcard(data: any) { return request({ url: '/mes/pro/procard', method: 'put', data }) }
// 删除
export function delProcard(ids: any) { return request({ url: '/mes/pro/procard/' + ids, method: 'delete' }) }
