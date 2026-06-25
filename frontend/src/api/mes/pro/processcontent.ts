import request from '@/utils/request'

// 查询工序作业内容列表
export function listProcessContent(query) {
  return request({
    url: '/mes/pro/processcontent/list',
    method: 'get',
    params: query
  })
}

// 根据工序ID查询作业内容
export function listProcessContentByProcessId(processId) {
  return request({
    url: '/mes/pro/processcontent/listByProcessId/' + processId,
    method: 'get'
  })
}

// 查询工序作业内容详细
export function getProcessContent(contentId) {
  return request({
    url: '/mes/pro/processcontent/' + contentId,
    method: 'get'
  })
}

// 新增工序作业内容
export function addProcessContent(data) {
  return request({
    url: '/mes/pro/processcontent',
    method: 'post',
    data: data
  })
}

// 修改工序作业内容
export function updateProcessContent(data) {
  return request({
    url: '/mes/pro/processcontent',
    method: 'put',
    data: data
  })
}

// 删除工序作业内容
export function delProcessContent(contentId) {
  return request({
    url: '/mes/pro/processcontent/' + contentId,
    method: 'delete'
  })
}
