import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmPackageLineQueryParams extends PageDomain {
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  unitName?: string
}

export interface WmPackageLine extends BaseEntity {
  lineId: number
  factoryId?: number
  packageId?: number
  itemId?: number
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  unitName?: string
  quantity?: number
  batchId?: number
  batchCode?: string
  barcodeCode?: string
}