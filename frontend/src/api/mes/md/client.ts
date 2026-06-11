import request from '@/utils/request'
import type { ClientQueryParams, MdClient, AjaxResult, TableDataInfo } from '@/types'

export function listClient(q: ClientQueryParams): Promise<TableDataInfo<MdClient[]>> { return request({ url: '/mes/md/client/list', method: 'get', params: q }) }
export function listAllClient(): Promise<AjaxResult<MdClient[]>> { return request({ url: '/mes/md/client/listAll', method: 'get' }) }
export function getClient(id: number): Promise<AjaxResult<MdClient>> { return request({ url: '/mes/md/client/' + id, method: 'get' }) }
export function addClient(d: MdClient): Promise<AjaxResult> { return request({ url: '/mes/md/client', method: 'post', data: d }) }
export function updateClient(d: MdClient): Promise<AjaxResult> { return request({ url: '/mes/md/client', method: 'put', data: d }) }
export function delClient(id: number | number[]): Promise<AjaxResult> { return request({ url: '/mes/md/client/' + id, method: 'delete' }) }
