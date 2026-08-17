import { parseQrPayload } from '@/utils/qrPayload'

/**
 * 把扫到/手输的原始码解析成跳转目标。
 * 纯函数，H5 输入页与 APP/小程序扫码共用，保证分发逻辑一致。
 *
 * @param {string} raw 原始码串
 * @returns {{url: string|null, payload: object|null, toast: string|null}}
 *   url    — 可跳转的报工页地址（含 query）；不支持的类型为 null
 *   payload— parseQrPayload 结果；非系统码为 null
 *   toast  — 不支持时需要提示用户的文案；url 非空时为 null
 */
export function resolveTarget(raw) {
  const payload = parseQrPayload(raw)
  // 非本系统码：当作裸工单/卡号，交报工页处理
  if (!payload) {
    return {
      url: '/pages/mes/pro/report?rawCode=' + encodeURIComponent(raw),
      payload: null,
      toast: null
    }
  }
  switch (payload.type) {
    case 'CARD':
      return { url: '/pages/mes/pro/report?cardCode=' + encodeURIComponent(payload.code), payload, toast: null }
    case 'WO':
      return { url: '/pages/mes/pro/report?workorderCode=' + encodeURIComponent(payload.code), payload, toast: null }
    case 'MAT':
      return { url: '/pages/mes/wm/issue/scan-query?batchCode=' + encodeURIComponent(payload.code), payload, toast: null }
    case 'ROLL':
      return { url: '/pages/mes/wm/issue/scan-query?rollCode=' + encodeURIComponent(payload.code), payload, toast: null }
    default:
      return { url: null, payload, toast: '暂不支持该类型码' }
  }
}

// #ifdef H5
/**
 * H5 端：uni.scanCode 在浏览器里不被支持（返回 method not supported），
 * 直接跳到手动输入页；由输入页调 resolveTarget 后再导航到报工页。
 */
export function scanAndDispatch() {
  uni.navigateTo({ url: '/pages/mes/pro/scan' })
  return Promise.resolve(null)
}
// #endif

// #ifndef H5
/**
 * APP / 小程序端：调起原生扫码，按 payload 前缀分发。
 * 区分"用户取消"与"真失败"，真失败给可见提示，不再静默吞错。
 *
 * @returns {Promise<{type:string,code:string}|null>} 解析后的 payload；
 *          用户取消返回 null；非系统码返回 {type:'RAW', code:raw}。
 */
export function scanAndDispatch() {
  return new Promise((resolve) => {
    uni.scanCode({
      onlyFromCamera: false,
      scanType: ['barCode', 'qrCode'],
      success: (res) => {
        const target = resolveTarget(res.result)
        if (target.url) {
          uni.navigateTo({ url: target.url })
        } else if (target.toast) {
          uni.showToast({ icon: 'none', title: target.toast })
        }
        resolve(target.payload || (res.result ? { type: 'RAW', code: res.result } : null))
      },
      fail: (err) => {
        const msg = (err && err.errMsg) || ''
        // 用户主动取消（各端文案略有差异），静默
        if (/cancel/i.test(msg)) {
          resolve(null)
          return
        }
        uni.showToast({ icon: 'none', title: '扫码失败：' + msg })
        resolve(null)
      }
    })
  })
}
// #endif
