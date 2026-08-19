import request from '@/utils/request'

export interface QcTemplateIndexRow { recordId?: number; templateId?: number; indexId: number | undefined; indexCode?: string; indexName?: string; indexType?: string; qcTool?: string; qcResultType?: string; checkMethod?: string; standerVal?: number; unitOfMeasure?: string; thresholdMin?: number; thresholdMax?: number; orderNum?: number }
export interface QcTemplateProductRow { recordId?: number; templateId?: number; itemId: number | undefined; itemCode?: string; itemName?: string; specification?: string; processId?: number; processCode?: string; processName?: string; quantityCheck?: number; quantityUnqualified?: number; crRate?: number; majRate?: number; minRate?: number }
export interface QcTemplate { templateId?: number; templateCode?: string; templateName?: string; qcTypes?: string; enableFlag?: string; remark?: string; indexRows?: QcTemplateIndexRow[]; productRows?: QcTemplateProductRow[] }
export const listTemplate = (query?: any) => request({ url: '/mes/qc/template/list', method: 'get', params: query })
export const getTemplate = (templateId: number) => request({ url: '/mes/qc/template/' + templateId, method: 'get' })
export const addTemplate = (data: QcTemplate) => request({ url: '/mes/qc/template', method: 'post', data })
export const updateTemplate = (data: QcTemplate) => request({ url: '/mes/qc/template', method: 'put', data })
export const delTemplate = (templateIds: number | number[]) => request({ url: '/mes/qc/template/' + templateIds, method: 'delete' })
