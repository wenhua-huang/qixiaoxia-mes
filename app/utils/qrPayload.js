/**
 * 二维码 payload 构造与解析工具。格式：QXX|TYPE|CODE
 * 与后端 com.ruoyi.common.utils.qr.QrPayloadUtil 保持一致（设计文档 §3.1）。
 *
 * 注意解析语义：后端用 Java split("\\|", 3)，limit=3 会把第 3 段及以后
 * 全部保留到最后一段；而 JS 的 split('|', 3) 会【截断】丢弃第 3 段之后内容。
 * 为保持含管道符的 CODE 不被截断，这里先无限制 split，再 slice(2).join('|')
 * 重新拼回，等价于后端的 limit=3 行为。
 */
const PREFIX = 'QXX'
const TYPES = ['CARD', 'MAT', 'ROLL', 'WO', 'PKG']

export function buildQrPayload(type, code) {
  return PREFIX + '|' + type + '|' + code
}

export function parseQrPayload(raw) {
  if (!raw) return null
  const parts = raw.split('|')
  if (parts.length < 3 || parts[0] !== PREFIX) return null
  if (TYPES.indexOf(parts[1]) < 0) return null
  // 第 3 段及以后合并为 code，保留可能包含管道符的编码
  const code = parts.slice(2).join('|')
  if (!code) return null
  return { type: parts[1], code }
}
