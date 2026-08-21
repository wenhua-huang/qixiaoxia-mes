// 图片 URL 规范化：后端上传 MinIO 返回 http://localhost:9010/qxx-mes/...，
// 真机/其他设备访问 H5 时 localhost 指向设备自身，图片加载失败。
// H5 下把任意 host 的 9010 地址改写成同源相对路径 /qxx-mes/...，由 Vite/Nginx 代理；
// 非 H5（App/小程序）保留原始 URL（生产 endpoint 为公网地址，可直接访问）。
const MINIO_ORIGIN_RE = /^https?:\/\/[^/]+:9010(?=\/)/i

export function normalizeImageUrl(url) {
  if (!url) return ''
  if (url.startsWith('blob:') || url.startsWith('data:') || url.startsWith('/')) return url
  // #ifdef H5
  if (MINIO_ORIGIN_RE.test(url)) return url.replace(MINIO_ORIGIN_RE, '')
  // #endif
  return url
}

export function normalizeImageUrls(urls) {
  return (urls || []).map(normalizeImageUrl)
}
