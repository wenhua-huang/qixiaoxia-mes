import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()],
  server: {
    // 绑定 0.0.0.0，允许真机通过局域网 IP 访问 H5 dev server
    host: true,
    port: 9090,
    proxy: {
      '/dev-api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/dev-api/, '')
      }
    }
  }
})
