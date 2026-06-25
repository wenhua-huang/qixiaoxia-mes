import request from '@/utils/request'

// 查询列表
export function listRtIssue(query: any) {
  return request({ url: '/mes/wm/rtissue/list', method: 'get', params: query })
}
// 查询详细
export function getRtIssue(rtId: number) { return request({ url: '/mes/wm/rtissue/' + rtId, method: 'get' }) }
// 新增
export function addRtIssue(data: any) { return request({ url: '/mes/wm/rtissue', method: 'post', data }) }
// 修改
export function updateRtIssue(data: any) { return request({ url: '/mes/wm/rtissue', method: 'put', data }) }
// 删除
export function delRtIssue(ids: any) { return request({ url: '/mes/wm/rtissue/' + ids, method: 'delete' }) }
