import request from '@/utils/request'
import type { MdProductBom, AjaxResult, TableDataInfo } from '@/types'

export function listBom(q: any): Promise<TableDataInfo<MdProductBom[]>> {
  return request({ url: '/mes/md/bom/list', method: 'get', params: q })
}
export function addBom(d: MdProductBom): Promise<AjaxResult> {
  return request({ url: '/mes/md/bom', method: 'post', data: d })
}
export function updateBom(d: MdProductBom): Promise<AjaxResult> {
  return request({ url: '/mes/md/bom', method: 'put', data: d })
}
export function delBom(id: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/md/bom/' + id, method: 'delete' })
}
