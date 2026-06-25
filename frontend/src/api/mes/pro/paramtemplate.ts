import request from '@/utils/request'

// 查询工序参数模版列表
export function listParamTemplate(query) {
  return request({
    url: '/mes/pro/paramtemplate/list',
    method: 'get',
    params: query
  })
}

// 根据工序ID查询参数模版
export function listParamTemplateByProcessId(processId) {
  return request({
    url: '/mes/pro/paramtemplate/listByProcessId/' + processId,
    method: 'get'
  })
}

// 查询工序参数模版详细
export function getParamTemplate(templateId) {
  return request({
    url: '/mes/pro/paramtemplate/' + templateId,
    method: 'get'
  })
}

// 新增工序参数模版
export function addParamTemplate(data) {
  return request({
    url: '/mes/pro/paramtemplate',
    method: 'post',
    data: data
  })
}

// 修改工序参数模版
export function updateParamTemplate(data) {
  return request({
    url: '/mes/pro/paramtemplate',
    method: 'put',
    data: data
  })
}

// 删除工序参数模版
export function delParamTemplate(templateId) {
  return request({
    url: '/mes/pro/paramtemplate/' + templateId,
    method: 'delete'
  })
}
