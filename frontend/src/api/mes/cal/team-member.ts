import request from '@/utils/request'

// 查询班组成员列表
export function listMember(query) {
  return request({
    url: '/mes/cal/team-member/list',
    method: 'get',
    params: query
  })
}

// 查询班组成员详细
export function getMember(memberId) {
  return request({
    url: '/mes/cal/team-member/' + memberId,
    method: 'get'
  })
}

// 新增班组成员
export function addMember(data) {
  return request({
    url: '/mes/cal/team-member',
    method: 'post',
    data: data
  })
}

// 修改班组成员
export function updateMember(data) {
  return request({
    url: '/mes/cal/team-member',
    method: 'put',
    data: data
  })
}

// 删除班组成员
export function delMember(memberId) {
  return request({
    url: '/mes/cal/team-member/' + memberId,
    method: 'delete'
  })
}
