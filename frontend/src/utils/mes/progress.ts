/** 工单/任务状态文案与 el-tag 类型映射 */
const STATUS_TEXT: Record<string, string> = {
  PREPARE: '待生产', NORMAL: '待开工', PRODUCING: '生产中',
  PAUSED: '暂停', COMPLETED: '已完工', CANCEL: '已取消'
}
const STATUS_TYPE: Record<string, string> = {
  PREPARE: 'info', NORMAL: 'info', PRODUCING: 'warning',
  PAUSED: 'warning', COMPLETED: 'success', CANCEL: 'danger'
}

/** 延期等级文案与 el-tag 类型映射 */
const DELAY_TEXT: Record<string, string> = {
  NORMAL: '正常', WARNING: '临期', DELAY: '延期',
  FINISHED_DELAY: '完工延期', BEHIND: '进度滞后'
}
const DELAY_TYPE: Record<string, string> = {
  NORMAL: 'info', WARNING: 'warning', DELAY: 'danger',
  FINISHED_DELAY: 'danger', BEHIND: 'warning'
}

export function statusText(s: string) { return STATUS_TEXT[s] || s }
export function workorderStatusType(s: string) { return STATUS_TYPE[s] || 'info' }
export function delayText(level: string) { return DELAY_TEXT[level] || level }
export function delayType(level: string) { return DELAY_TYPE[level] || 'info' }
