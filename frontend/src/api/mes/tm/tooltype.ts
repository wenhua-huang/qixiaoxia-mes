import request from '@/utils/request'

// 查询工装夹具类型列表
export function listType(query) {
  return request({
    url: '/mes/tm/tooltype/list',
    method: 'get',
    params: query
  })
}

// 查询所有启用的工装夹具类型（下拉框用）
export function listAllType() {
  return request({
    url: '/mes/tm/tooltype/listAll',
    method: 'get'
  })
}

// 查询工装夹具类型详细
export function getType(toolTypeId) {
  return request({
    url: '/mes/tm/tooltype/' + toolTypeId,
    method: 'get'
  })
}

// 新增工装夹具类型
export function addType(data) {
  return request({
    url: '/mes/tm/tooltype',
    method: 'post',
    data: data
  })
}

// 修改工装夹具类型
export function updateType(data) {
  return request({
    url: '/mes/tm/tooltype',
    method: 'put',
    data: data
  })
}

// 删除工装夹具类型
export function delType(toolTypeId) {
  return request({
    url: '/mes/tm/tooltype/' + toolTypeId,
    method: 'delete'
  })
}
