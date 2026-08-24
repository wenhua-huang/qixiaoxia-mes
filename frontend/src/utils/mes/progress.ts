/**
 * 工单/任务状态与延期等级的文案、el-tag 类型。
 *
 * 任务状态走后端字典 mes_pro_task_status（组件内 useDict + dict-tag 渲染），
 * 不再在此硬编码；工单状态暂无字典，沿用工单列表页 workorder/index.vue 的
 * 权威 statusMap/statusColor，集中维护于此供进度/报表/预警复用。
 */

/** 工单状态 → 文案（与 workorder/index.vue statusMap 保持一致） */
const WORKORDER_STATUS_LABEL: Record<string, string> = {
  PREPARE: '待生产',
  PRODUCING: '生产中',
  COMPLETED: '已完成',
  CANCEL: '已取消',
  CLOSED: '已关闭'
}

/** 工单状态 → el-tag 类型（与 workorder/index.vue statusColor 对齐） */
const WORKORDER_STATUS_TAG: Record<string, string> = {
  PREPARE: 'warning',
  PRODUCING: 'primary',
  COMPLETED: 'success',
  CANCEL: 'info',
  CLOSED: 'info'
}

/** 工单状态选项，供 dict-tag 直接消费 */
export const WORKORDER_STATUS_OPTIONS = Object.entries(WORKORDER_STATUS_LABEL).map(([value, label]) => ({
  value,
  label,
  elTagType: WORKORDER_STATUS_TAG[value] || 'info'
}))

export function workorderStatusText(s: string) { return WORKORDER_STATUS_LABEL[s] || s }
export function workorderStatusType(s: string) { return WORKORDER_STATUS_TAG[s] || 'info' }

/** 延期等级文案与 el-tag 类型映射 */
const DELAY_TEXT: Record<string, string> = {
  NORMAL: '正常', WARNING: '临期', DELAY: '延期',
  FINISHED_DELAY: '完工延期', BEHIND: '进度滞后'
}
const DELAY_TYPE: Record<string, string> = {
  NORMAL: 'info', WARNING: 'warning', DELAY: 'danger',
  FINISHED_DELAY: 'danger', BEHIND: 'warning'
}

export function delayText(level: string) { return DELAY_TEXT[level] || level }
export function delayType(level: string) { return DELAY_TYPE[level] || 'info' }
