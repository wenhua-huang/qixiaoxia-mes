// 应用全局配置
// 生产构建(uni build)用 /prod-api 走 Nginx 代理；
// 本地开发(uni)用相对路径 /dev-api，由 H5 dev server(Vite) 转发到后端，
// 这样真机(手机)通过 192.168.x.x:9090 访问时也能通，避免写死 localhost 指向手机自身。
const isProd = process.env.NODE_ENV === 'production'
export default {
  baseUrl: isProd ? '/prod-api' : '/dev-api',
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
