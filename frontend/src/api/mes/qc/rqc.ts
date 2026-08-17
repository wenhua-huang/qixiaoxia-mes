import request from '@/utils/request'
import type { QcOrderLine, QcDefectRecord } from './common'

export type { QcOrderLine, QcDefectRecord } from './common'

/** 退料检验单（字段对齐后端 QcRqc 实体） */
export interface QcRqc {
  rqcId?: number
  rqcCode?: string
  rqcName?: string
  /** PROD_RETURN/PURCHASE_RETURN/QC_REJECT */
  rqcType?: string
  templateId?: number
  sourceDocId?: number
  sourceDocType?: string
  sourceDocCode?: string
  sourceLineId?: number
  workorderId?: number
  workorderCode?: string
  vendorId?: number
  vendorCode?: string
  vendorName?: string
  itemId?: number
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  batchCode?: string
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
  /** 退料原因（检验员录入） */
  returnReason?: string
  /** SUPPLIER/PRODUCTION/STORAGE/OTHER */
  responsibility?: string
  inspectDate?: string
  inspector?: string
  /** PENDING/INSPECTING/COMPLETED/CLOSED */
  status?: string
  remark?: string
  lines?: QcOrderLine[]
  defectRecords?: QcDefectRecord[]
}

export const listRqc = (query?: any) => request({ url: '/mes/qc/rqc/list', method: 'get', params: query })
export const getRqc = (rqcId: number) => request({ url: '/mes/qc/rqc/' + rqcId, method: 'get' })
export const addRqc = (data: QcRqc) => request({ url: '/mes/qc/rqc', method: 'post', data })
/** 整头编辑：lines/defectRecords null=不动、[]=清空、非空=全删全插 */
export const updateRqc = (data: QcRqc) => request({ url: '/mes/qc/rqc', method: 'put', data })
export const delRqc = (rqcIds: number | number[]) => request({ url: '/mes/qc/rqc/' + rqcIds, method: 'delete' })
/** 执行判定；FAIL 时带 concessionReason 升级为让步接收 */
export const judgeRqc = (rqcId: number, concessionReason?: string) =>
  request({ url: `/mes/qc/rqc/judge/${rqcId}`, method: 'put', data: { concessionReason } })
/** 关闭（作废）检验单，仅 PENDING/INSPECTING 可关 */
export const closeRqc = (rqcId: number) => request({ url: `/mes/qc/rqc/close/${rqcId}`, method: 'put' })
/** 按来源单据查检验单（下游单据页查检验状态用） */
export const listRqcBySource = (sourceDocType: string, sourceDocId: number) =>
  request({ url: '/mes/qc/rqc/listBySource', method: 'get', params: { sourceDocType, sourceDocId } })
