import request from '@/utils/request'

export interface QcIndex {
  indexId?: number
  indexCode?: string
  indexName?: string
  indexType?: string
  qcTool?: string
  qcResultType?: string
  dictType?: string
  qcResultSpc?: string
  enableFlag?: string
  remark?: string
}

export const listIndex = (query?: any) => request({ url: '/mes/qc/index/list', method: 'get', params: query })
export const getIndex = (indexId: number) => request({ url: '/mes/qc/index/' + indexId, method: 'get' })
export const addIndex = (data: QcIndex) => request({ url: '/mes/qc/index', method: 'post', data })
export const updateIndex = (data: QcIndex) => request({ url: '/mes/qc/index', method: 'put', data })
export const delIndex = (indexIds: number | number[]) => request({ url: '/mes/qc/index/' + indexIds, method: 'delete' })
