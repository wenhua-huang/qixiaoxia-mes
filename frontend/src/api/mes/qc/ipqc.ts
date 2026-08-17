import request from '@/utils/request'
import type { QcOrderLine, QcDefectRecord } from './common'

export type { QcOrderLine, QcDefectRecord } from './common'

/** 过程检验单（字段对齐后端 QcIpqc 实体） */
export interface QcIpqc {
  ipqcId?: number
  ipqcCode?: string
  ipqcName?: string
  /** FIRST_CHECK/TOUR_CHECK/LAST_CHECK/SPOT_CHECK */
  ipqcType?: string
  templateId?: number
  sourceDocId?: number
  sourceDocType?: string
  sourceDocCode?: string
  sourceLineId?: number
  workorderId?: number
  workorderCode?: string
  workorderName?: string
  cardId?: number
  cardCode?: string
  taskId?: number
  taskCode?: string
  processId?: number
  processCode?: string
  processName?: string
  workstationId?: number
  workstationCode?: string
  workstationName?: string
  itemId?: number
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
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
  inspectDate?: string
  inspector?: string
  /** PENDING/INSPECTING/COMPLETED/CLOSED */
  status?: string
  remark?: string
  lines?: QcOrderLine[]
  defectRecords?: QcDefectRecord[]
}

export const listIpqc = (query?: any) => request({ url: '/mes/qc/ipqc/list', method: 'get', params: query })
export const getIpqc = (ipqcId: number) => request({ url: '/mes/qc/ipqc/' + ipqcId, method: 'get' })
export const addIpqc = (data: QcIpqc) => request({ url: '/mes/qc/ipqc', method: 'post', data })
/** 整头编辑：lines/defectRecords null=不动、[]=清空、非空=全删全插 */
export const updateIpqc = (data: QcIpqc) => request({ url: '/mes/qc/ipqc', method: 'put', data })
export const delIpqc = (ipqcIds: number | number[]) => request({ url: '/mes/qc/ipqc/' + ipqcIds, method: 'delete' })
/** 执行判定；FAIL 时带 concessionReason 升级为让步接收 */
export const judgeIpqc = (ipqcId: number, concessionReason?: string) =>
  request({ url: `/mes/qc/ipqc/judge/${ipqcId}`, method: 'put', data: { concessionReason } })
/** 关闭（作废）检验单，仅 PENDING/INSPECTING 可关 */
export const closeIpqc = (ipqcId: number) => request({ url: `/mes/qc/ipqc/close/${ipqcId}`, method: 'put' })
/** 按来源单据查检验单（下游单据页查检验状态用） */
export const listIpqcBySource = (sourceDocType: string, sourceDocId: number) =>
  request({ url: '/mes/qc/ipqc/listBySource', method: 'get', params: { sourceDocType, sourceDocId } })
