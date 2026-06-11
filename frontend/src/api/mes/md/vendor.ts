import request from '@/utils/request'
import type { VendorQueryParams, MdVendor, AjaxResult, TableDataInfo } from '@/types'

export function listVendor(q: VendorQueryParams): Promise<TableDataInfo<MdVendor[]>> { return request({ url: '/mes/md/vendor/list', method: 'get', params: q }) }
export function listAllVendor(): Promise<AjaxResult<MdVendor[]>> { return request({ url: '/mes/md/vendor/listAll', method: 'get' }) }
export function getVendor(id: number): Promise<AjaxResult<MdVendor>> { return request({ url: '/mes/md/vendor/' + id, method: 'get' }) }
export function addVendor(d: MdVendor): Promise<AjaxResult> { return request({ url: '/mes/md/vendor', method: 'post', data: d }) }
export function updateVendor(d: MdVendor): Promise<AjaxResult> { return request({ url: '/mes/md/vendor', method: 'put', data: d }) }
export function delVendor(id: number | number[]): Promise<AjaxResult> { return request({ url: '/mes/md/vendor/' + id, method: 'delete' }) }
