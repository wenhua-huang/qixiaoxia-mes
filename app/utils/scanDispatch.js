import { parseQrPayload } from '@/utils/qrPayload'

/**
 * 调起 uni 扫码，返回 Promise<rawString>。
 * 用户取消时 reject（调用方自行 catch）。
 */
function scan() {
  return new Promise((resolve, reject) => {
    uni.scanCode({
      onlyFromCamera: false,
      scanType: ['barCode', 'qrCode'],
      success: (res) => resolve(res.result),
      fail: (err) => reject(err)
    })
  })
}

/**
 * 扫码并按 payload 前缀分发到对应页面。
 * P1 仅落地 CARD/WO；MAT/ROLL 待 P2/P3。
 *
 * @returns {Promise<{type:string, code:string}|null>} 解析后的 payload；
 *          用户取消返回 null；非系统码返回 {type:'RAW', code:raw}。
 */
export async function scanAndDispatch() {
  let raw
  try {
    raw = await scan()
  } catch (e) {
    return null // 用户取消
  }
  const payload = parseQrPayload(raw)
  if (!payload) {
    // 非本系统码：当作裸工单/卡号，交报工页处理
    uni.navigateTo({ url: '/pages/mes/pro/report?rawCode=' + encodeURIComponent(raw) })
    return { type: 'RAW', code: raw }
  }
  switch (payload.type) {
    case 'CARD':
      uni.navigateTo({ url: '/pages/mes/pro/report?cardCode=' + encodeURIComponent(payload.code) })
      break
    case 'WO':
      uni.navigateTo({ url: '/pages/mes/pro/report?workorderCode=' + encodeURIComponent(payload.code) })
      break
    case 'MAT':
    case 'ROLL':
      uni.showToast({ icon: 'none', title: '该类型码待 P2/P3 支持' })
      break
    default:
      uni.showToast({ icon: 'none', title: '暂不支持该类型码' })
  }
  return payload
}
