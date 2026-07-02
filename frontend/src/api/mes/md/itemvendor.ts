import request from '@/utils/request'

export function listItemVendor(query: any) { return request({ url: '/mes/md/itemvendor/list', method: 'get', params: query }) }
export function listByVendorId(vendorId: number) { return request({ url: '/mes/md/itemvendor/listByVendorId/' + vendorId, method: 'get' }) }
export function getItemVendor(recordId: number) { return request({ url: '/mes/md/itemvendor/' + recordId, method: 'get' }) }
export function addItemVendor(data: any) { return request({ url: '/mes/md/itemvendor', method: 'post', data }) }
export function updateItemVendor(data: any) { return request({ url: '/mes/md/itemvendor', method: 'put', data }) }
export function delItemVendor(recordIds: string) { return request({ url: '/mes/md/itemvendor/' + recordIds, method: 'delete' }) }
