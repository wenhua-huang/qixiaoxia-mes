import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'
import type { MdAttrDef, MdItemTypeAttr, SaveAttrBindRequest } from '@/types/api/mes/md/attr'

// ============ 属性字典 CRUD ============

export function listAttrDef(query: Partial<MdAttrDef>): Promise<TableDataInfo<MdAttrDef>> {
  return request({ url: '/mes/md/attrDef/list', method: 'get', params: query })
}

export function getAttrDef(attrId: number): Promise<AjaxResult<MdAttrDef>> {
  return request({ url: '/mes/md/attrDef/' + attrId, method: 'get' })
}

export function addAttrDef(data: MdAttrDef): Promise<AjaxResult> {
  return request({ url: '/mes/md/attrDef', method: 'post', data })
}

export function updateAttrDef(data: MdAttrDef): Promise<AjaxResult> {
  return request({ url: '/mes/md/attrDef', method: 'put', data })
}

export function delAttrDef(attrId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/md/attrDef/' + attrId, method: 'delete' })
}

// ============ 分类-属性绑定 ============

/** 查某分类直接绑定（配置页用，不含继承） */
export function getAttrBind(typeId: number): Promise<AjaxResult<MdItemTypeAttr[]>> {
  return request({ url: '/mes/md/itemtype/attrBind/' + typeId, method: 'get' })
}

/** 全量保存某分类的属性绑定 */
export function saveAttrBind(data: SaveAttrBindRequest): Promise<AjaxResult> {
  return request({ url: '/mes/md/itemtype/attrBind', method: 'put', data })
}

/** 查某分类有效属性 schema（含继承，动态表单渲染用） */
export function getEffAttrSchema(typeId: number): Promise<AjaxResult<MdItemTypeAttr[]>> {
  return request({ url: '/mes/md/itemtype/effAttrSchema/' + typeId, method: 'get' })
}

/** 新建属性并绑定到分类（隐式字典：attr_code 存在则复用，不重复创建） */
export function createAttrAndBind(data: {
  typeId: number
  attrDef: Partial<MdAttrDef>
  required?: boolean
}): Promise<AjaxResult<MdItemTypeAttr>> {
  return request({ url: '/mes/md/itemtype/attrBind/createAttrAndBind', method: 'post', data })
}
