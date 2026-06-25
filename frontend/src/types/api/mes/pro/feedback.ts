import { PageDomain, BaseEntity } from '@/types/api/common'

export interface ProFeedbackQueryParams extends PageDomain {
  feedbackCode?: string
  workorderName?: string
  workstationName?: string
  processName?: string
  status?: string
}

export interface ProFeedback extends BaseEntity {
  recordId?: number
  feedbackCode?: string
  taskId?: number
  taskCode?: string
  workorderId?: number
  workorderCode?: string
  workorderName?: string
  productId?: number
  productCode?: string
  productName?: string
  processId?: number
  processName?: string
  workstationId?: number
  workstationName?: string
  quantityFeedback?: number
  quantityQualified?: number
  quantityUnqualified?: number
  quantityLaborScrap?: number
  quantityMaterialScrap?: number
  quantityUncheck?: number
  feedbackType?: string
  feedbackChannel?: string
  userName?: string
  nickName?: string
  status?: string
  feedbackTime?: string
  consumeList?: ProFeedbackConsume[]
}

export interface ProFeedbackConsume {
  consumeId?: number
  feedbackId?: number
  workorderId?: number
  itemId?: number
  itemCode?: string
  itemName?: string
  quantity?: number
  batchCode?: string
}

export interface ProFeedbackParam {
  recordId?: number
  feedbackId?: number
  paramName?: string
  standardValue?: string
  actualValue?: string
  remark?: string
}
