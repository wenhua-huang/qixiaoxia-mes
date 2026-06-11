import request from '@/utils/request'
import type { AttachmentQueryParams, SysAttachment, AjaxResult, TableDataInfo } from '@/types'

// 查询附件列表
export function listAttachment(query: AttachmentQueryParams): Promise<TableDataInfo<SysAttachment[]>> {
  return request({ url: '/mes/sys/attachment/list', method: 'get', params: query })
}

// 按关联单据查询附件
export function listAttachmentBySource(sourceDocId: number, sourceDocType: string): Promise<AjaxResult<SysAttachment[]>> {
  return request({ url: '/mes/sys/attachment/listBySource', method: 'get', params: { sourceDocId, sourceDocType } })
}

// 查询附件详细
export function getAttachment(attachmentId: number): Promise<AjaxResult<SysAttachment>> {
  return request({ url: '/mes/sys/attachment/' + attachmentId, method: 'get' })
}

// 新增附件
export function addAttachment(data: SysAttachment): Promise<AjaxResult> {
  return request({ url: '/mes/sys/attachment', method: 'post', data: data })
}

// 修改附件
export function updateAttachment(data: SysAttachment): Promise<AjaxResult> {
  return request({ url: '/mes/sys/attachment', method: 'put', data: data })
}

// 删除附件
export function delAttachment(attachmentId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/sys/attachment/' + attachmentId, method: 'delete' })
}
