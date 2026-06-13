import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmStockTakingPlanQueryParams extends PageDomain {
  planCode?: string
  planName?: string
  planType?: string
  warehouseName?: string
  status?: string
}

export interface WmStockTakingPlan extends BaseEntity {
  planId: number
  factoryId?: number
  planCode?: string
  planName?: string
  planType?: string
  warehouseId?: number
  warehouseName?: string
  planDate?: string
  status?: string
}