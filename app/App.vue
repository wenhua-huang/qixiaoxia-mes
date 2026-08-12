<script setup>
  import config from './config'
  import { getToken } from '@/utils/auth'
  import { useConfigStore } from '@/store'
  import { getCurrentInstance } from "vue"
  import { onLaunch } from '@dcloudio/uni-app'

  const { proxy } = getCurrentInstance()

  onLaunch(() => {
    // 清除可能残留的全局 loading（uni.showLoading 是全局模态，
    // 跨页面不自动消失；上次会话若因异常未 hideLoading 会一直盖在页面上）
    uni.hideLoading()
    initApp()
  })

  // 初始化应用
  function initApp() {
    // 初始化应用配置
    initConfig()
    // 检查用户登录状态
    //#ifdef H5
    checkLogin()
    //#endif
  }

  function initConfig() {
    useConfigStore().setConfig(config)
  }

  function checkLogin() {
    if (!getToken()) {
      proxy.$tab.reLaunch('/pages/login') 
    }
  }
</script>

<style lang="scss">
  @import '@/static/scss/index.scss'
</style>
