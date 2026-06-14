import request from '@/utils/request'

// 查询计划班组关联列表
export function listTeam(query) {
  return request({
    url: '/mes/cal/plan-team/list',
    method: 'get',
    params: query
  })
}

// 查询计划班组关联详细
export function getTeam(recordId) {
  return request({
    url: '/mes/cal/plan-team/' + recordId,
    method: 'get'
  })
}

// 新增计划班组关联
export function addTeam(data) {
  return request({
    url: '/mes/cal/plan-team',
    method: 'post',
    data: data
  })
}

// 修改计划班组关联
export function updateTeam(data) {
  return request({
    url: '/mes/cal/plan-team',
    method: 'put',
    data: data
  })
}

// 删除计划班组关联
export function delTeam(recordId) {
  return request({
    url: '/mes/cal/plan-team/' + recordId,
    method: 'delete'
  })
}
