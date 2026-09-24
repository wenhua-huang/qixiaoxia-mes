import request from '@/utils/request'
import upload from '@/utils/upload'

// 手机端上报异常（POST /mes/pro/exception）
export function addException(data) {
  return request({ url: '/mes/pro/exception', method: 'post', data: data })
}

// 上报上下文：按任务带出工单/工序只读快照
export function getReportContext(taskId) {
  return request({ url: '/mes/pro/exception/reportContext/' + taskId, method: 'get' })
}

// 异常现场照片上传（与质检缺陷图同走 MinIO，返回 { url }）
export function uploadExceptionImage(filePath) {
  return upload({ url: '/common/uploadMinio', name: 'file', filePath })
}
