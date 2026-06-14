import request from '@/utils/request'

// 查询班组排班明细列表
export function listTeamshift(query) {
  return request({
    url: '/mes/cal/teamshift/list',
    method: 'get',
    params: query
  })
}

// 查询班组排班明细详细
export function getTeamshift(teamshiftId) {
  return request({
    url: '/mes/cal/teamshift/' + teamshiftId,
    method: 'get'
  })
}

// 新增班组排班明细
export function addTeamshift(data) {
  return request({
    url: '/mes/cal/teamshift',
    method: 'post',
    data: data
  })
}

// 修改班组排班明细
export function updateTeamshift(data) {
  return request({
    url: '/mes/cal/teamshift',
    method: 'put',
    data: data
  })
}

// 删除班组排班明细
export function delTeamshift(teamshiftId) {
  return request({
    url: '/mes/cal/teamshift/' + teamshiftId,
    method: 'delete'
  })
}
