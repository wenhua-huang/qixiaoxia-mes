/**
 * 工序任务状态文案与配色（qxx_pro_task.status）。
 * 任务状态无后端字典，前端多处共用本映射；新增消费方直接引用，勿再拷贝。
 */
export const TASK_STATUS_LABEL: Record<string, string> = {
  PREPARE: '待排产',
  NORMAL: '正常',
  PRODUCING: '生产中',
  COMPLETED: '已完成',
  PAUSED: '暂停',
  CANCEL: '取消'
}

/** el-tag type（与任务列表内联配色保持同一语义） */
export const TASK_STATUS_TAG_TYPE: Record<string, '' | 'success' | 'info' | 'warning' | 'danger' | 'primary'> = {
  PREPARE: 'warning',
  NORMAL: 'primary',
  PRODUCING: 'success',
  COMPLETED: 'info',
  PAUSED: 'warning',
  CANCEL: 'danger'
}

export const TASK_STATUS_COLOR: Record<string, string> = {
  PREPARE: '#E6A23C',
  NORMAL: '#409EFF',
  PRODUCING: '#67C23A',
  COMPLETED: '#909399',
  PAUSED: '#E6A23C',
  CANCEL: '#F56C6C'
}
