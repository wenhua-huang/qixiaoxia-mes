// 应用全局配置
// 生产构建(uni build)用 /prod-api 走 Nginx 代理,本地开发(uni)用 localhost
const isProd = process.env.NODE_ENV === 'production'
export default {
  baseUrl: isProd ? '/prod-api' : 'http://localhost:8081',
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
