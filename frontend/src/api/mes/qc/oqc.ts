import request from '@/utils/request'
import type { QcOrderLine, QcDefectRecord } from './common'

export type { QcOrderLine, QcDefectRecord } from './common'

/** 出货检验单（字段对齐后端 QcOqc 实体） */
export interface QcOqc {
  oqcId?: number
  oqcCode?: string
  oqcName?: string
  templateId?: number
  sourceDocId?: number
  sourceDocType?: string
  sourceDocCode?: string
  sourceLineId?: number
  clientId?: number
  clientCode?: string
  clientName?: string
  batchCode?: string
  itemId?: number
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  quantityOut?: number
  quantityCheck?: number
  quantityMinCheck?: number
  quantityMaxUnqualified?: number
  quantityQualified?: number
  quantityUnqualified?: number
  crRateLimit?: number
  majRateLimit?: number
  minRateLimit?: number
  crQuantity?: number
  majQuantity?: number
  minQuantity?: number
  crRate?: number
  majRate?: number
  minRate?: number
  /** PASS/FAIL/CONCESSION */
  checkResult?: string
  concessionReason?: string
  outDate?: string
  inspectDate?: string
  inspector?: string
  /** PENDING/INSPECTING/COMPLETED/CLOSED */
  status?: string
  remark?: string
  lines?: QcOrderLine[]
  defectRecords?: QcDefectRecord[]
}

export const listOqc = (query?: any) => request({ url: '/mes/qc/oqc/list', method: 'get', params: query })
export const getOqc = (oqcId: number) => request({ url: '/mes/qc/oqc/' + oqcId, method: 'get' })
export const addOqc = (data: QcOqc) => request({ url: '/mes/qc/oqc', method: 'post', data })
/** 整头编辑：lines/defectRecords null=不动、[]=清空、非空=全删全插 */
export const updateOqc = (data: QcOqc) => request({ url: '/mes/qc/oqc', method: 'put', data })
export const delOqc = (oqcIds: number | number[]) => request({ url: '/mes/qc/oqc/' + oqcIds, method: 'delete' })
/** 执行判定；FAIL 时带 concessionReason 升级为让步接收 */
export const judgeOqc = (oqcId: number, concessionReason?: string) =>
  request({ url: `/mes/qc/oqc/judge/${oqcId}`, method: 'put', data: { concessionReason } })
/** 关闭（作废）检验单，仅 PENDING/INSPECTING 可关 */
export const closeOqc = (oqcId: number) => request({ url: `/mes/qc/oqc/close/${oqcId}`, method: 'put' })
/** 按来源单据查检验单（下游单据页查检验状态用） */
export const listOqcBySource = (sourceDocType: string, sourceDocId: number) =>
  request({ url: '/mes/qc/oqc/listBySource', method: 'get', params: { sourceDocType, sourceDocId } })
