import request from '@/utils/request'

// 质检不合格放行（需 mes:pro:task:release 权限，理由必填）
export function releaseQcBlock(taskId, reason) {
  return request({
    url: '/mes/pro/task/releaseQcBlock/' + taskId,
    method: 'put',
    params: { reason }
  })
}
