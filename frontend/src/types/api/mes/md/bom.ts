import type { BaseEntity } from "../../common";

export interface MdProductBom extends BaseEntity {
  bomId?: number; factoryId?: number; itemId?: number; bomItemId?: number;
  unitOfMeasure?: string; quantity?: number; scrapRate?: number; enableFlag?: string;
  itemName?: string; itemCode?: string; bomItemName?: string; bomItemCode?: string;
}
