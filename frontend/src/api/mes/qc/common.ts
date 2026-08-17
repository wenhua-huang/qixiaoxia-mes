/** 检验域共用类型（IQC/IPQC/OQC/RQC 各检验单 API 共用） */

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
