// 应用全局配置
// 生产构建(uni build)用 /prod-api 走 Nginx 代理；
// H5 开发用相对路径 /dev-api 走 Vite 代理（vite.config.js），桌面 localhost 与
// 手机 LAN IP 访问都通，无需写死 IP；APP/小程序无同源代理，需填电脑 LAN IP。
const isProd = process.env.NODE_ENV === 'production'
let baseUrl
if (isProd) {
  baseUrl = '/prod-api'
} else {
  // #ifdef H5
  baseUrl = '/dev-api'
  // #endif
  // #ifndef H5
  // APP/小程序真机调试：改成运行本 dev server 的电脑局域网 IP（后端需监听 0.0.0.0 或同机反代）
  baseUrl = 'http://192.168.1.9:8081'
  // #endif
}
export default {
  baseUrl,
  // baseUrl: 'https://vue.ruoyi.vip/prod-api',
  // 应用信息
  appInfo: {
    // 应用名称
    name: "ruoyi-app",
    // 应用版本
    version: "1.2.0",
    // 应用logo
    logo: "/static/logo.png",
    // 官方网站
    site_url: "http://ruoyi.vip",
    // 政策协议
    agreements: [{
        title: "隐私政策",
        url: "https://ruoyi.vip/protocol.html"
      },
      {
        title: "用户服务协议",
        url: "https://ruoyi.vip/protocol.html"
      }
    ]
  }
}
