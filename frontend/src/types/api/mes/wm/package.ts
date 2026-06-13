import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmPackageQueryParams extends PageDomain {
  packageCode?: string
  packageName?: string
  ancestors?: string
  packageType?: string
  workorderCode?: string
}

export interface WmPackage extends BaseEntity {
  packageId: number
  factoryId?: number
  packageCode?: string
  packageName?: string
  parentId?: number
  ancestors?: string
  packageType?: string
  workorderId?: number
  workorderCode?: string
  quantityPerBox?: number
  boxCount?: number
  totalQuantity?: number
  weight?: number
  length?: number
  width?: number
  height?: number
  volume?: number
  boxLabel?: string
  status?: string
}