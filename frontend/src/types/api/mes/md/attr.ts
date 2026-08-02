/**
 * 物料分类动态扩展属性 - 类型定义
 */

/** 属性类型 */
export type AttrType = 'TEXT' | 'NUMBER' | 'SELECT' | 'BOOL' | 'DATE'

/** 属性字典（全局定义） */
export interface MdAttrDef {
  attrId?: number
  factoryId?: number
  attrCode: string
  attrName: string
  attrType: AttrType
  attrUnit?: string
  /** SELECT 类型的可选值（JSON 字符串，如 '["锯齿口","平口"]'） */
  optionsJson?: string
  sortOrder?: number
  enableFlag?: string
  remark?: string
  /** 前端解析后的 SELECT 选项 */
  options?: string[]
}

/** 分类-属性绑定（含继承查询时的关联字段） */
export interface MdItemTypeAttr {
  id?: number
  factoryId?: number
  itemTypeId?: number
  attrId: number
  required?: string
  sortOrder?: number
  enableFlag?: string
  // 关联 attr_def 带出
  attrCode?: string
  attrName?: string
  attrType?: AttrType
  attrUnit?: string
  optionsJson?: string
  /** 继承深度：0=本类绑定，>0=继承自祖先 */
  inheritDepth?: number
}

/** 保存绑定的请求体 */
export interface SaveAttrBindRequest {
  typeId: number
  binds: Array<Pick<MdItemTypeAttr, 'attrId' | 'required' | 'sortOrder' | 'enableFlag'>>
}
