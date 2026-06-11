import request from '@/utils/request'
import type { WorkstationQueryParams, MdWorkstation, MdWorkstationMachine, MdWorkstationWorker, AjaxResult, TableDataInfo } from '@/types'

export function listWorkstation(q: WorkstationQueryParams): Promise<TableDataInfo<MdWorkstation[]>> { return request({ url: '/mes/md/workstation/list', method: 'get', params: q }) }
export function getWorkstation(id: number): Promise<AjaxResult<MdWorkstation>> { return request({ url: '/mes/md/workstation/' + id, method: 'get' }) }
export function addWorkstation(d: MdWorkstation): Promise<AjaxResult> { return request({ url: '/mes/md/workstation', method: 'post', data: d }) }
export function updateWorkstation(d: MdWorkstation): Promise<AjaxResult> { return request({ url: '/mes/md/workstation', method: 'put', data: d }) }
export function delWorkstation(id: number | number[]): Promise<AjaxResult> { return request({ url: '/mes/md/workstation/' + id, method: 'delete' }) }

export function listMachines(workstationId: number): Promise<AjaxResult<MdWorkstationMachine[]>> { return request({ url: '/mes/md/workstation/' + workstationId + '/machines', method: 'get' }) }
export function addMachine(d: MdWorkstationMachine): Promise<AjaxResult> { return request({ url: '/mes/md/workstation/machine', method: 'post', data: d }) }
export function updateMachine(d: MdWorkstationMachine): Promise<AjaxResult> { return request({ url: '/mes/md/workstation/machine', method: 'put', data: d }) }
export function delMachine(id: number | number[]): Promise<AjaxResult> { return request({ url: '/mes/md/workstation/machine/' + id, method: 'delete' }) }

export function listWorkers(workstationId: number): Promise<AjaxResult<MdWorkstationWorker[]>> { return request({ url: '/mes/md/workstation/' + workstationId + '/workers', method: 'get' }) }
export function addWorker(d: MdWorkstationWorker): Promise<AjaxResult> { return request({ url: '/mes/md/workstation/worker', method: 'post', data: d }) }
export function updateWorker(d: MdWorkstationWorker): Promise<AjaxResult> { return request({ url: '/mes/md/workstation/worker', method: 'put', data: d }) }
export function delWorker(id: number | number[]): Promise<AjaxResult> { return request({ url: '/mes/md/workstation/worker/' + id, method: 'delete' }) }
