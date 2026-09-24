/**
 * MinIO 图片 URL 规范化：后端 /common/uploadMinio 返回的地址由服务端 endpoint 拼成
 * http://localhost:9010/qxx-mes/...（生产也是同机 localhost），浏览器打开时 localhost
 * 指向浏览者自身，图片会裂。这里去掉任意 host 的 9010 源，改成同源相对路径 /qxx-mes/...，
 * 本地由 vite proxy、生产由 nginx /qxx-mes/ 反代到 MinIO（与 app/utils/image.js 同策略）。
 */
const MINIO_ORIGIN_RE = /^https?:\/\/[^/]+:9010(?=\/)/i

export function normalizeImageUrl(url?: string): string {
  if (!url) return ''
  if (url.startsWith('blob:') || url.startsWith('data:') || url.startsWith('/')) return url
  return url.replace(MINIO_ORIGIN_RE, '')
}
