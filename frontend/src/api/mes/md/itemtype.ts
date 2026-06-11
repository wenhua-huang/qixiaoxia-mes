import request from '@/utils/request'
import type { ItemTypeQueryParams, MdItemType, AjaxResult, TreeSelect } from '@/types'

// 查询物料分类列表（全量，不分页，树形表格用）
export function listItemtype(query: ItemTypeQueryParams): Promise<AjaxResult<MdItemType[]>> {
  return request({
    url: '/mes/md/itemtype/list',
    method: 'get',
    params: query
  })
}

// 查询分类树（下拉用）
export function treeselect(): Promise<AjaxResult<TreeSelect[]>> {
  return request({
    url: '/mes/md/itemtype/treeselect',
    method: 'get'
  })
}

// 查询分类列表（排除自身及子孙节点）
export function listExcludeChild(itemTypeId: number): Promise<AjaxResult<MdItemType[]>> {
  return request({
    url: '/mes/md/itemtype/list/exclude/' + itemTypeId,
    method: 'get'
  })
}

// 查询分类详细
export function getItemtype(itemTypeId: number): Promise<AjaxResult<MdItemType>> {
  return request({
    url: '/mes/md/itemtype/' + itemTypeId,
    method: 'get'
  })
}

// 新增分类
export function addItemtype(data: MdItemType): Promise<AjaxResult> {
  return request({
    url: '/mes/md/itemtype',
    method: 'post',
    data: data
  })
}

// 修改分类
export function updateItemtype(data: MdItemType): Promise<AjaxResult> {
  return request({
    url: '/mes/md/itemtype',
    method: 'put',
    data: data
  })
}

// 删除分类
export function delItemtype(itemTypeId: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mes/md/itemtype/' + itemTypeId,
    method: 'delete'
  })
}
