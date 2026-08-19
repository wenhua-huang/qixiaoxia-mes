import { PageDomain, BaseEntity } from "@/types/api/common"
import type { WmItemRecptLine } from "./item_recpt_line"

export interface WmItemRecptQueryParams extends PageDomain {
  recptCode?: string
  recptName?: string
  purOrderCode?: string
  vendorCode?: string
  vendorName?: string
}

export interface WmItemRecpt extends BaseEntity {
  recptId: number
  factoryId?: number
  recptCode?: string
  recptName?: string
  purOrderId?: number
  purOrderCode?: string
  vendorId?: number
  vendorCode?: string
  vendorName?: string
  warehouseId?: number
  warehouseCode?: string
  warehouseName?: string
  recptDate?: string
  recptType?: string
  totalQuantity?: number
  iqcId?: number
  iqcCode?: string
  status?: string
  /** 检验状态汇总（列表计算列）：PASSED/CONCESSION/PENDING/FAILED/NONE，空=未启用质检 */
  qcStatus?: string
  /** 入库单行列表（新增草稿/详情返回，非持久化） */
  lines?: WmItemRecptLine[]
}