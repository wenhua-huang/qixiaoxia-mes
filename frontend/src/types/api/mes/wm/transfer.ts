import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmTransferQueryParams extends PageDomain {
  transferCode?: string
  transferName?: string
  sourceWarehouseCode?: string
  sourceWarehouseName?: string
  targetWarehouseCode?: string
}

export interface WmTransfer extends BaseEntity {
  transferId: number
  factoryId?: number
  transferCode?: string
  transferName?: string
  sourceWarehouseId?: number
  sourceWarehouseCode?: string
  sourceWarehouseName?: string
  targetWarehouseId?: number
  targetWarehouseCode?: string
  targetWarehouseName?: string
  transferDate?: string
  totalQuantity?: number
  status?: string
}