import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmMiscIssueQueryParams extends PageDomain {
  issueCode?: string
  issueName?: string
  issueType?: string
  warehouseName?: string
  status?: string
}

export interface WmMiscIssue extends BaseEntity {
  issueId: number
  factoryId?: number
  issueCode?: string
  issueName?: string
  issueType?: string
  warehouseId?: number
  warehouseName?: string
  issueDate?: string
  totalQuantity?: number
  status?: string
}