import request from '@/utils/request'

export interface QcDefect {
  defectId?: number
  defectCode?: string
  defectName?: string
  indexType?: string
  defectLevel?: string
  processMethod?: string
  enableFlag?: string
  remark?: string
}

export const listDefect = (query?: any) => request({ url: '/mes/qc/defect/list', method: 'get', params: query })
export const getDefect = (defectId: number) => request({ url: '/mes/qc/defect/' + defectId, method: 'get' })
export const addDefect = (data: QcDefect) => request({ url: '/mes/qc/defect', method: 'post', data })
export const updateDefect = (data: QcDefect) => request({ url: '/mes/qc/defect', method: 'put', data })
export const delDefect = (defectIds: number | number[]) => request({ url: '/mes/qc/defect/' + defectIds, method: 'delete' })
