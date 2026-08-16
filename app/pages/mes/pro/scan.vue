<template>
  <view class="scan-page">
    <!-- 相机扫码模式 -->
    <view v-if="mode === 'camera'" class="camera-wrap">
      <view id="qr-reader" class="qr-reader"></view>
      <view class="scan-status" :class="{ error: !!errorMsg, success: scanned }">
        <uni-icons v-if="scanned" type="checkmarkempty" size="18" color="#67c23a" />
        <uni-icons v-else-if="errorMsg" type="info" size="18" color="#f56c6c" />
        <uni-icons v-else type="scan" size="18" color="#409eff" />
        <text class="status-text">{{ statusText }}</text>
      </view>
      <view v-if="errorMsg" class="error-actions">
        <button class="cu-btn bg-blue sm" @click="startCamera">重试</button>
        <button class="cu-btn line-blue sm" @click="switchToManual">手动输入</button>
      </view>
      <view v-else class="scan-tip-bar">
        <text>将二维码/条码对准框内，自动识别</text>
        <text class="manual-link" @click="switchToManual">手动输入 ›</text>
      </view>
    </view>

    <!-- 手动输入模式（相机不可用或用户主动切换） -->
    <view v-else class="manual-wrap">
      <!-- 相机不可用的原因，帮助自助排查 -->
      <view v-if="errorMsg" class="error-card">
        <uni-icons type="info" size="18" color="#f56c6c" />
        <view class="error-body">
          <text class="error-title">相机扫码不可用</text>
          <text class="error-msg">{{ errorMsg }}</text>
          <text v-if="!windowIsSecure" class="error-hint">
            请用 Chrome 浏览器打开，地址栏输入 chrome://flags，
            搜索「Insecure origins treated as secure」，启用并填入
            {{ currentOrigin }} 后重启浏览器。
          </text>
        </view>
      </view>
      <view class="tip-card">
        <uni-icons type="info" size="18" color="#e6a23c" />
        <text class="tip-text">手动输入或粘贴编码</text>
      </view>
      <view class="section">
        <uni-section title="扫码/输入" type="line" />
        <view class="input-row">
          <uni-easyinput
            v-model="code"
            placeholder="扫码内容 / 编码"
            :inputBorder="false"
            clearable
            class="code-input"
            @confirm="submitManual"
          />
        </view>
        <button class="btn-confirm cu-btn bg-blue" @click="submitManual" :disabled="!code.trim()">
          确定
        </button>
        <button v-if="cameraAvailable" class="btn-back cu-btn line-blue" @click="switchToCamera">
          返回相机扫码
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, onUnmounted, getCurrentInstance } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { resolveTarget } from '@/utils/scanDispatch'
import UniEasyInput from '@/uni_modules/uni-easyinput/components/uni-easyinput/uni-easyinput.vue'
import UniIcons from '@/uni_modules/uni-icons/components/uni-icons/uni-icons.vue'
import UniSection from '@/components/uni-section/uni-section.vue'

const mode = ref('camera')          // 'camera' | 'manual'
const statusText = ref('正在启动相机…')
const errorMsg = ref('')
const scanned = ref(false)
const code = ref('')
const cameraAvailable = ref(false)  // 非安全上下文/无设备时为 false，隐藏"返回相机"
const currentOrigin = ref('')       // 当前访问来源，用于提示用户填入 chrome://flags
const windowIsSecure = ref(true)    // 是否安全上下文

let scanner = null                  // Html5Qrcode 实例
let starting = false                // 防止重复启动
let navigating = false              // 防止扫码成功后重复跳转
const callbackMode = ref(false)     // true=扫完把结果回传给来源页（eventChannel），不做全局分发

const { proxy: _proxy } = getCurrentInstance() || {}

// callback=1 时由业务页（发料/查库存等）navigateTo 进入，扫完 emit('scanResult', code) 返回
onLoad((options) => {
  callbackMode.value = !!(options && options.callback === '1')
})

onMounted(() => {
  // #ifdef H5
  currentOrigin.value = window.location.origin
  windowIsSecure.value = window.isSecureContext
  if (!window.isSecureContext) {
    errorMsg.value = '非安全上下文：浏览器禁止在 HTTP 局域网下开启相机。'
    statusText.value = errorMsg.value
    mode.value = 'manual'
    cameraAvailable.value = false
    return
  }
  startCamera()
  // #endif
  // #ifndef H5
  errorMsg.value = '此页面仅用于 H5 浏览器扫码'
  mode.value = 'manual'
  // #endif
})

onUnmounted(() => {
  stopCamera()
})

// #ifdef H5
async function startCamera() {
  if (starting || scanner) return
  starting = true
  errorMsg.value = ''
  scanned.value = false
  statusText.value = '正在启动相机…'
  mode.value = 'camera'
  try {
    const { Html5Qrcode } = await import('html5-qrcode')
    scanner = new Html5Qrcode('qr-reader')
    const config = {
      fps: 10,
      qrbox: (vw, vh) => {
        const size = Math.floor(Math.min(vw, vh) * 0.7)
        return { width: size, height: size }
      },
      aspectRatio: 1.0
    }
    // 部分环境（无相机/权限弹窗无响应）start() 可能既不 resolve 也不 reject，加 8s 超时兜底
    await Promise.race([
      scanner.start(
        { facingMode: 'environment' },
        config,
        onDecodeSuccess,
        () => { /* 逐帧解码失败是正常的，忽略 */ }
      ),
      new Promise((_, reject) => setTimeout(() => reject(new Error('CameraStartTimeout')), 8000))
    ])
    cameraAvailable.value = true
    statusText.value = '将二维码/条码对准框内'
  } catch (err) {
    // start() 可能在超时 reject 后才真正启动，先尽力停掉释放摄像头
    if (scanner) {
      try { await scanner.stop() } catch (e) { /* 未真正启动，忽略 */ }
      try { scanner.clear() } catch (e) { /* ignore */ }
    }
    cameraAvailable.value = false
    errorMsg.value = describeCameraError(err)
    statusText.value = errorMsg.value
    mode.value = 'manual'
    scanner = null
  } finally {
    starting = false
  }
}

async function stopCamera() {
  if (!scanner) return
  try {
    // stop() 会关闭视频流；忽略"未运行"等异常
    await scanner.stop()
    scanner.clear()
  } catch (e) { /* 已停止或组件卸载，忽略 */ }
  scanner = null
}

function onDecodeSuccess(decodedText) {
  if (navigating || scanned.value) return
  if (!decodedText) return
  scanned.value = true
  navigating = true
  statusText.value = '识别成功：' + decodedText
  uni.vibrateShort && uni.vibrateShort({ type: 'light' })
  stopCamera().finally(() => dispatch(decodedText))
}

function describeCameraError(err) {
  const name = (err && (err.name || '')) + ' ' + ((err && err.message) || String(err || ''))
  if (/NotAllowedError|Permission denied|permission/i.test(name)) {
    return '相机权限被拒绝，请在浏览器设置中允许相机权限后重试。'
  }
  if (/NotFoundError|Requested device not found|no camera/i.test(name)) {
    return '未检测到摄像头设备。'
  }
  if (/NotReadableError|Could not start video source|in use/i.test(name)) {
    return '摄像头被其他应用占用，请关闭后重试。'
  }
  if (/NotSupportedError|secure context|Only secure/i.test(name)) {
    return '当前环境不支持相机调用（需 HTTPS 或安全来源）。'
  }
  if (/CameraStartTimeout/.test(name)) {
    return '相机启动超时，请确认已授权相机权限或改用手动输入。'
  }
  return '相机启动失败：' + (err && err.message ? err.message : String(err))
}
// #endif

function switchToManual() {
  stopCamera()
  mode.value = 'manual'
}

function switchToCamera() {
  code.value = ''
  startCamera()
}

function submitManual() {
  const raw = code.value.trim()
  if (!raw) {
    uni.showToast({ icon: 'none', title: '请输入编码' })
    return
  }
  dispatch(raw)
}

function dispatch(raw) {
  if (callbackMode.value) {
    // 回调模式：结果传回来源页（navigateTo 的 events.scanResult），自己退栈
    try {
      const ec = _proxy && _proxy.getOpenerEventChannel && _proxy.getOpenerEventChannel()
      if (ec && ec.emit) ec.emit('scanResult', raw)
    } catch (e) { /* eventChannel 不可用时无能为力，返回即可 */ }
    uni.navigateBack()
    return
  }
  const target = resolveTarget(raw)
  if (target.url) {
    // redirectTo 替换当前页：返回键直接回工作台，不会回到扫码页
    uni.redirectTo({ url: target.url })
  } else if (target.toast) {
    navigating = false
    scanned.value = false
    statusText.value = target.toast
    uni.showToast({ icon: 'none', title: target.toast })
  }
}
</script>

<style lang="scss" scoped>
.scan-page { min-height: 100vh; background: #1a1a1a; }
.camera-wrap { position: relative; }
.qr-reader {
  width: 100%;
  // Html5Qrcode 内部 video/canvas 会铺满容器；给一个稳定的宽高比
  :deep(video) { width: 100% !important; height: auto !important; display: block; }
  :deep(canvas) { display: none; }
}
.scan-status {
  display: flex; align-items: center; gap: 12rpx; justify-content: center;
  padding: 24rpx 32rpx; color: #409eff; font-size: 28rpx;
  &.error { color: #f56c6c; }
  &.success { color: #67c23a; }
  .status-text { flex: 1; }
}
.scan-tip-bar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16rpx 32rpx 40rpx; color: #c0c4cc; font-size: 24rpx;
  .manual-link { color: #409eff; font-size: 26rpx; }
}
.error-actions {
  display: flex; gap: 24rpx; justify-content: center; padding: 24rpx 32rpx 40rpx;
}
.manual-wrap { min-height: 100vh; background: #f5f6f7; padding-bottom: 40rpx; }
.error-card {
  display: flex; gap: 16rpx;
  background: #fef0f0; margin: 16rpx; padding: 24rpx; border-radius: 12rpx;
  .error-body { flex: 1; display: flex; flex-direction: column; gap: 8rpx; }
  .error-title { font-size: 28rpx; font-weight: bold; color: #f56c6c; }
  .error-msg { font-size: 26rpx; color: #606266; }
  .error-hint { font-size: 24rpx; color: #909399; line-height: 1.6; }
}
.tip-card {
  display: flex; align-items: center; gap: 12rpx;
  background: #fdf6ec; margin: 16rpx; padding: 20rpx 24rpx; border-radius: 12rpx;
  .tip-text { font-size: 26rpx; color: #e6a23c; flex: 1; }
}
.section { padding: 0 16rpx; }
.input-row {
  background: #fff; border-radius: 8rpx; padding: 8rpx 16rpx; margin: 16rpx 0;
  .code-input { width: 100%; }
}
.btn-confirm, .btn-back {
  margin: 24rpx 0; width: 100%; height: 88rpx; line-height: 88rpx;
  border-radius: 8rpx; font-size: 30rpx; border: none;
  &::after { border: none; }
  &[disabled] { opacity: 0.5; }
}
.btn-back { margin-top: 0; background: #fff; color: #409eff; border: 1rpx solid #409eff; }
</style>
