import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmRollDetailQueryParams extends PageDomain {
  rollCode?: string
  itemCode?: string
  itemName?: string
  specification?: string
  batchCode?: string
}

export interface WmRollDetail extends BaseEntity {
  rollId: number
  factoryId?: number
  rollCode?: string
  itemId?: number
  itemCode?: string
  itemName?: string
  specification?: string
  batchId?: number
  batchCode?: string
  recptId?: number
  recptDetailId?: number
  vendorId?: number
  vendorCode?: string
  vendorName?: string
  vendorRollNo?: string
  parentRollId?: number
  actualWidth?: string
  actualWeightGsm?: string
  actualLength?: number
  actualWeight?: number
  unitOfMeasure?: string
  originalQuantity?: number
  remainingQuantity?: number
  warehouseId?: number
  warehouseCode?: string
  warehouseName?: string
  locationId?: number
  areaId?: number
  materialStockId?: number
  lastIssueId?: number
  lastWorkorderId?: number
  lastWorkorderCode?: string
  status?: string
}