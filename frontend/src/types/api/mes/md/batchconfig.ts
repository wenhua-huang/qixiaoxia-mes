import type { PageDomain, BaseEntity } from "../../common";

export interface BatchConfigQueryParams extends PageDomain {
  itemId?: number;
}

export interface MdItemBatchConfig extends BaseEntity {
  configId?: number;
  factoryId?: number;
  itemId?: number;
  produceDateFlag?: string;
  expireDateFlag?: string;
  recptDateFlag?: string;
  vendorFlag?: string;
  clientFlag?: string;
  coCodeFlag?: string;
  poCodeFlag?: string;
  workorderFlag?: string;
  taskFlag?: string;
  workstationFlag?: string;
  toolFlag?: string;
  moldFlag?: string;
  lotNumberFlag?: string;
  qualityStatusFlag?: string;
  paperRollFlag?: string;
  paperWidthFlag?: string;
  enableFlag?: string;
}
