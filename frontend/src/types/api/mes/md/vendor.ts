import type { PageDomain, BaseEntity } from "../../common";

export interface VendorQueryParams extends PageDomain {
  vendorCode?: string; vendorName?: string; vendorType?: string; coopStatus?: string; enableFlag?: string;
}

export interface MdVendor extends BaseEntity {
  vendorId?: number; factoryId?: number; vendorCode?: string; vendorName?: string;
  outsourceFactoryId?: number; vendorNick?: string; vendorEn?: string;
  vendorType?: string; vendorDes?: string; vendorLogo?: string; vendorLevel?: string;
  vendorScore?: number; address?: string; website?: string; email?: string; tel?: string;
  contact1?: string; contact1Tel?: string; contact1Email?: string;
  contact2?: string; contact2Tel?: string; contact2Email?: string;
  creditCode?: string; settlementType?: string; paymentTerms?: string;
  coopStatus?: string; enableFlag?: string;
}
