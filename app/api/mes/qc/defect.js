import request from '@/utils/request'

// 缺陷字典列表（可按 indexType=IQC/IPQC 过滤）
export function listDefect(query) {
  return request({ url: '/mes/qc/defect/list', method: 'get', params: query })
}
