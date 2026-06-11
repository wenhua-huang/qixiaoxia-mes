import type { PageDomain, BaseEntity } from "../../common";

export interface ClientQueryParams extends PageDomain {
  clientCode?: string; clientName?: string; clientType?: string; salesperson?: string; enableFlag?: string;
}

export interface MdClient extends BaseEntity {
  clientId?: number; factoryId?: number; clientCode?: string; clientName?: string;
  clientNick?: string; clientEn?: string; clientType?: string; salesperson?: string;
  creditCode?: string; address?: string; website?: string; email?: string; tel?: string;
  contact1?: string; contact1Tel?: string; contact1Email?: string;
  contact2?: string; contact2Tel?: string; contact2Email?: string;
  shippingAddress?: string; enableFlag?: string;
}
