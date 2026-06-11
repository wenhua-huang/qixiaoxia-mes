import type { PageDomain, BaseEntity } from "../../common";

export interface WorkstationQueryParams extends PageDomain {
  workstationCode?: string; workstationName?: string; workshopId?: number; enableFlag?: string;
}

export interface MdWorkstation extends BaseEntity {
  workstationId?: number; factoryId?: number; workstationCode?: string; workstationName?: string;
  workshopId?: number; workstationType?: string; processType?: string;
  capacity?: number; status?: string; enableFlag?: string;
}

export interface MdWorkstationMachine extends BaseEntity {
  recordId?: number; factoryId?: number; workstationId?: number;
  machineryId?: number; machineryCode?: string; machineryName?: string;
}

export interface MdWorkstationWorker extends BaseEntity {
  recordId?: number; factoryId?: number; workstationId?: number;
  userId?: number; userName?: string; roleType?: string;
}
