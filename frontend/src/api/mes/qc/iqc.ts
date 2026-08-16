import request from '@/utils/request'

/** 检验单行（IQC/IPQC/OQC/RQC 共用，字段对齐后端 QcOrderLine 实体） */
export interface QcOrderLine {
  lineId?: number
  qcType?: string
  qcId?: number
  indexId?: number
  indexCode?: string
  indexName?: string
  indexType?: string
  qcTool?: string
  /** 值类型: NUMBER/TEXT/DICT/FILE/COUNT */
  qcResultType?: string
  checkMethod?: string
  standerVal?: number
  unitOfMeasure?: string
  thresholdMin?: number
  thresholdMax?: number
  /** 实测值（数值型存数字文本，行结果 NUMBER 型由服务端判定） */
  checkValText?: string
  crQuantity?: number
  majQuantity?: number
  minQuantity?: number
  /** PASS/FAIL */
  lineResult?: string
  orderNum?: number
  remark?: string
}

/** 检验缺陷记录（字段对齐后端 QcDefectRecord 实体） */
export interface QcDefectRecord {
  recordId?: number
  qcType?: string
  qcId?: number
  lineId?: number
  defectId?: number
  defectCode?: string
  defectName?: string
  /** CRITICAL/MAJOR/MINOR */
  defectLevel?: string
  defectQuantity?: number
  processMethod?: string
  defectImage?: string
  remark?: string
}

/** 来料检验单（字段对齐后端 QcIqc 实体） */
export interface QcIqc {
  iqcId?: number
  iqcCode?: string
  iqcName?: string
  templateId?: number
  sourceDocId?: number
  sourceDocType?: string
  sourceDocCode?: string
  sourceLineId?: number
  vendorId?: number
  vendorCode?: string
  vendorName?: string
  vendorBatch?: string
  itemId?: number
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  quantityReceived?: number
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
  receiveDate?: string
  inspectDate?: string
  inspector?: string
  /** PENDING/INSPECTING/COMPLETED/CLOSED */
  status?: string
  remark?: string
  lines?: QcOrderLine[]
  defectRecords?: QcDefectRecord[]
}

export const listIqc = (query?: any) => request({ url: '/mes/qc/iqc/list', method: 'get', params: query })
export const getIqc = (iqcId: number) => request({ url: '/mes/qc/iqc/' + iqcId, method: 'get' })
export const addIqc = (data: QcIqc) => request({ url: '/mes/qc/iqc', method: 'post', data })
/** 整头编辑：lines/defectRecords null=不动、[]=清空、非空=全删全插 */
export const updateIqc = (data: QcIqc) => request({ url: '/mes/qc/iqc', method: 'put', data })
export const delIqc = (iqcIds: number | number[]) => request({ url: '/mes/qc/iqc/' + iqcIds, method: 'delete' })
/** 执行判定；FAIL 时带 concessionReason 升级为让步接收 */
export const judgeIqc = (iqcId: number, concessionReason?: string) =>
  request({ url: `/mes/qc/iqc/judge/${iqcId}`, method: 'put', data: { concessionReason } })
/** 关闭（作废）检验单，仅 PENDING/INSPECTING 可关 */
export const closeIqc = (iqcId: number) => request({ url: `/mes/qc/iqc/close/${iqcId}`, method: 'put' })
/** 按来源单据查检验单（下游单据页查检验状态用） */
export const listIqcBySource = (sourceDocType: string, sourceDocId: number) =>
  request({ url: '/mes/qc/iqc/listBySource', method: 'get', params: { sourceDocType, sourceDocId } })
