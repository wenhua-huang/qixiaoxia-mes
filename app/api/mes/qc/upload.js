import upload from '@/utils/upload'

// 质检图片上传 MinIO，返回 AjaxResult（url 在顶层）
export function uploadQcImage(filePath) {
  return upload({ url: '/common/uploadMinio', name: 'file', filePath })
}
