import config from '@/config'
import { getToken } from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import { useUserStore } from '@/store/modules/user'
import { toast, showConfirm, tansParams } from '@/utils/common'

let timeout = 10000
const baseUrl = config.baseUrl

const request = config => {
  // 是否需要设置 token
  const isToken = (config.headers || {}).isToken === false
  config.header = config.header || {}
  if (getToken() && !isToken) {
    config.header['Authorization'] = 'Bearer ' + getToken()
  }
  // get请求映射params参数
  if (config.params) {
    let url = config.url + '?' + tansParams(config.params)
    url = url.slice(0, -1)
    config.url = url
  }
  return new Promise((resolve, reject) => {
    uni.request({
        method: config.method || 'get',
        timeout: config.timeout ||  timeout,
        url: config.baseUrl || baseUrl + config.url,
        data: config.data,
        header: config.header,
        dataType: 'json'
      }).then(response => {
        const res = response
        // HTTP 状态码非 200（如 502/504/404 返回 HTML，或 204 空体）直接按网络错误处理，
        // 否则 res.data.code 为 undefined 会被默认成 200，把错误响应当成成功返回。
        if (res.statusCode !== 200) {
          const httpMsg = '请求失败（HTTP ' + res.statusCode + '）'
          toast(httpMsg)
          reject(httpMsg)
          return
        }
        if (!res.data || typeof res.data !== 'object') {
          toast('服务端返回数据格式异常')
          reject('服务端返回数据格式异常')
          return
        }
        const code = res.data.code || 200
        const msg = errorCode[code] || res.data.msg || errorCode['default']
        if (code === 401) {
          showConfirm('登录状态已过期，您可以继续留在该页面，或者重新登录?').then(res => {
            if (res.confirm) {
              useUserStore().logOut().then(res => {
                uni.reLaunch({ url: '/pages/login' })
              })
            }
          })
          reject('无效的会话，或者会话已过期，请重新登录。')
        } else if (code === 500) {
          toast(msg)
          reject(msg)
        } else if (code !== 200) {
          toast(msg)
          reject(msg)
        } else {
          resolve(res.data)
        }
      })
      .catch(error => {
        // uni.request 网络层失败 reject 的是 { errMsg, errCode } 或字符串，不是 axios 的 Error。
        // 若按 axios 风格读 error.message 会得到 undefined，再 .includes() 抛 TypeError，
        // 导致外层 Promise 永 pending、uni.showLoading 跨页面残留（"登录中"卡住）。
        let message = ''
        if (typeof error === 'string') {
          message = error
        } else if (error && error.errMsg) {
          message = error.errMsg            // "request:fail timeout" / "request:fail abort" 等
        } else if (error && error.message) {
          message = error.message           // 兼容 axios/H5 风格
        }
        if (message.indexOf('timeout') !== -1) {
          message = '系统接口请求超时'
        } else if (message.indexOf('abort') !== -1) {
          message = '请求已取消'
        } else if (message) {
          message = '网络异常，请检查网络连接（' + message + '）'
        } else {
          message = '网络异常，请检查网络连接'
        }
        toast(message)
        reject(error)
      })
  })
}

export default request
