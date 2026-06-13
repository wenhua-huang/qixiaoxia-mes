import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmMiscRecptQueryParams extends PageDomain {
  recptCode?: string
  recptName?: string
  recptType?: string
  warehouseName?: string
  status?: string
}

export interface WmMiscRecpt extends BaseEntity {
  recptId: number
  factoryId?: number
  recptCode?: string
  recptName?: string
  recptType?: string
  warehouseId?: number
  warehouseName?: string
  recptDate?: string
  totalQuantity?: number
  status?: string
}