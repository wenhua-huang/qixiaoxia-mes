import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmBarcodeQueryParams extends PageDomain {
  barcodeCode?: string
  barcodeType?: string
  itemCode?: string
  itemName?: string
  workorderCode?: string
}

export interface WmBarcode extends BaseEntity {
  barcodeId: number
  factoryId?: number
  barcodeCode?: string
  barcodeType?: string
  itemId?: number
  itemCode?: string
  itemName?: string
  workorderId?: number
  workorderCode?: string
  quantity?: number
  unitOfMeasure?: string
  batchCode?: string
  printCount?: number
  lastPrintTime?: string
  enableFlag?: string
}