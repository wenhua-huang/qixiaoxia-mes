import request from '@/utils/request'

// 查询列表
export function listRtIssueLine(query: any) {
  return request({ url: '/mes/wm/rtissueline/list', method: 'get', params: query })
}
// 查询详细
export function getRtIssueLine(lineId: number) { return request({ url: '/mes/wm/rtissueline/' + lineId, method: 'get' }) }
// 新增
export function addRtIssueLine(data: any) { return request({ url: '/mes/wm/rtissueline', method: 'post', data }) }
// 修改
export function updateRtIssueLine(data: any) { return request({ url: '/mes/wm/rtissueline', method: 'put', data }) }
// 删除
export function delRtIssueLine(ids: any) { return request({ url: '/mes/wm/rtissueline/' + ids, method: 'delete' }) }
// 根据退料单ID查询行列表
export function listRtIssueLineByRtId(rtId: number) { return request({ url: '/mes/wm/rtissueline/listByRtId/' + rtId, method: 'get' }) }
