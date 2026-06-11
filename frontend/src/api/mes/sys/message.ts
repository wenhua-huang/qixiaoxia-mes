import request from '@/utils/request'
import type { MessageQueryParams, SysMessage, AjaxResult, TableDataInfo } from '@/types'

// 查询系统消息列表
export function listMessage(query: MessageQueryParams): Promise<TableDataInfo<SysMessage[]>> {
  return request({ url: '/mes/sys/message/list', method: 'get', params: query })
}

// 查询系统消息详细
export function getMessage(messageId: number): Promise<AjaxResult<SysMessage>> {
  return request({ url: '/mes/sys/message/' + messageId, method: 'get' })
}

// 新增系统消息
export function addMessage(data: SysMessage): Promise<AjaxResult> {
  return request({ url: '/mes/sys/message', method: 'post', data: data })
}

// 删除系统消息
export function delMessage(messageId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/sys/message/' + messageId, method: 'delete' })
}

// 批量标记已读
export function markAsRead(messageIds: number | number[]): Promise<AjaxResult> {
  const ids = Array.isArray(messageIds) ? messageIds.join(',') : messageIds
  return request({ url: '/mes/sys/message/read/' + ids, method: 'put' })
}

// 获取未读消息数
export function getUnreadCount(): Promise<AjaxResult<number>> {
  return request({ url: '/mes/sys/message/unreadCount', method: 'get' })
}
