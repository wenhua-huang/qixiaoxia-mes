import request from '@/utils/request'

// 查询工装夹具类型列表
export function listType(query) {
  return request({
    url: '/mes/tm/tool-type/list',
    method: 'get',
    params: query
  })
}

// 查询工装夹具类型详细
export function getType(toolTypeId) {
  return request({
    url: '/mes/tm/tool-type/' + toolTypeId,
    method: 'get'
  })
}

// 新增工装夹具类型
export function addType(data) {
  return request({
    url: '/mes/tm/tool-type',
    method: 'post',
    data: data
  })
}

// 修改工装夹具类型
export function updateType(data) {
  return request({
    url: '/mes/tm/tool-type',
    method: 'put',
    data: data
  })
}

// 删除工装夹具类型
export function delType(toolTypeId) {
  return request({
    url: '/mes/tm/tool-type/' + toolTypeId,
    method: 'delete'
  })
}
