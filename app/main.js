import { createSSRApp } from 'vue'
import App from './App'
import store from './store' // store
import { install } from './plugins' // plugins
import './permission' // permission
import { useDict } from './utils/dict'
import { registerUniUI } from './utils/uni-ui-register'

export function createApp() {
  const app = createSSRApp(App)
  app.use(store)
  app.config.globalProperties.useDict = useDict
  install(app)
  registerUniUI(app) // 全局注册 uni-ui，兜底 easycom 失效（含组件内部依赖）
  return {
    app
  }
}
