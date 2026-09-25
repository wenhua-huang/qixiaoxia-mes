<template>
  <view class="container">
    <!-- 关联对象（只读快照） -->
    <uni-section title="关联工序任务" type="line"></uni-section>
    <view class="form-card">
      <view class="ctx-row">
        <text class="label">工单</text>
        <text class="ctx-val">{{ ctx.workorderCode || '加载中…' }}{{ ctx.workorderName ? ' · ' + ctx.workorderName : '' }}</text>
      </view>
      <view class="ctx-row">
        <text class="label">工序</text>
        <text class="ctx-val">{{ ctx.processName || '—' }}</text>
      </view>
      <view class="ctx-row">
        <text class="label">任务</text>
        <text class="ctx-val">{{ ctx.taskCode || '—' }}</text>
      </view>
    </view>

    <!-- 异常类型（必选） -->
    <uni-section title="异常类型" type="line"></uni-section>
    <view class="type-grid">
      <view v-for="t in TYPE_OPTIONS" :key="t.code"
        :class="['type-card', form.exceptionType === t.code ? 'active' : '']"
        @click="form.exceptionType = t.code">
        <text class="type-name">{{ t.name }}</text>
        <text class="type-hint">{{ t.hint }}</text>
      </view>
    </view>

    <!-- 影响数量与说明 -->
    <uni-section title="异常详情" type="line"></uni-section>
    <view class="form-card">
      <view class="form-row">
        <text class="label">影响数量</text>
        <uni-easyinput v-model="form.impactQuantity" type="digit" placeholder="选填，按产品计量单位"
          :inputBorder="false" class="qty-input" />
      </view>
      <view class="desc-box">
        <uni-easyinput v-model="form.description" type="textarea" placeholder="请描述异常情况（必填，如：做坏数量、缺料物料、延迟原因等）"
          :maxlength="1000" :inputBorder="false" />
      </view>
    </view>

    <!-- 现场照片 -->
    <uni-section title="现场照片（选填）" type="line"></uni-section>
    <view class="form-card">
      <view class="photo-row">
        <view v-for="(img, i) in imageList" :key="i" class="photo-item">
          <image :src="img" mode="aspectFill" class="photo-img" @click="previewImg(i)" />
          <uni-icons type="closeempty" size="18" class="photo-del" @click="removeImg(i)" />
        </view>
        <view v-if="imageList.length + uploading < MAX_SCENE_IMAGES" class="photo-add" @click="takePhoto">
          <uni-icons type="camera-filled" size="26" :color="uploading ? '#409eff' : '#999'" />
          <text v-if="uploading" class="uploading-text">上传中 {{ uploading }}</text>
        </view>
      </view>
    </view>

    <!-- 底部提交 -->
    <view class="footer-bar">
      <button class="cu-btn lg" :class="canSubmit ? 'bg-red' : 'bg-disabled'"
        :disabled="!canSubmit || submitting" @click="submit">
        {{ submitting ? '提交中...' : (uploading ? '图片上传中，请稍候' : (contextReady ? '提交异常单' : '任务信息加载中…')) }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, getCurrentInstance } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import uniIcons from '@/uni_modules/uni-icons/components/uni-icons/uni-icons.vue'
import uniEasyinput from '@/uni_modules/uni-easyinput/components/uni-easyinput/uni-easyinput.vue'
import { addException, getReportContext, uploadExceptionImage } from '@/api/mes/pro/exception'
import { chooseImageAsync } from '@/utils/chooseImage'
import { normalizeImageUrl } from '@/utils/image'

const { proxy } = getCurrentInstance()

const TYPE_OPTIONS = [
  { code: 'QUALITY', name: '质量数量', hint: '做坏 / 做少等' },
  { code: 'MATERIAL', name: '缺料', hint: '物料未到 / 不够' },
  { code: 'DELAY', name: '进度延迟', hint: '无法按计划完成' },
  { code: 'RETURN', name: '客户退货', hint: '客户退回批次' }
]
// 现场照片上限（与 scene_images 单列逗号串、九宫格 UI 对齐）
const MAX_SCENE_IMAGES = 9
const MAX_IMAGE_MB = 10
const ALLOWED_IMAGE_EXT = ['jpg', 'jpeg', 'png', 'bmp', 'gif', 'webp']
// 提交兜底：9 张完整 URL 逗号串不超过列宽（V159 varchar(2000)）
const SCENE_IMAGES_MAX_LEN = 1900
// 上传兜底：网关 502/204 空体时 uni.uploadFile 也走 success，JSON.parse 抛错会让 Promise 永挂
const UPLOAD_TIMEOUT_MS = 30000
// 提示后延迟返回的时长（非法入口/加载失败/提交成功）
const BACK_DELAY_MS = 800
const SUCCESS_BACK_DELAY_MS = 1200

const taskId = ref(null)
const ctx = ref({})
const contextReady = ref(false)
const form = reactive({ exceptionType: '', impactQuantity: '', description: '' })
// 库内原始形态：MinIO 返回的 URL 逗号串（展示一律走 normalizeImageUrl，避免相对路径回写污染）
const sceneImages = ref('')
const uploading = ref(0)
const submitting = ref(false)

const imageList = computed(() =>
  sceneImages.value ? sceneImages.value.split(',').filter(Boolean).map(normalizeImageUrl) : [])
const canSubmit = computed(() =>
  contextReady.value && !!form.exceptionType && form.description.trim().length >= 2 && uploading.value === 0)

// 页面卸载后忽略迟到的上传回调，避免返回后弹 toast / 写已销毁实例。
// 必须是 ref：提交成功路径靠 pageActive.value 判定，误写成普通布尔会使
// `!pageActive.value` 恒为 true，成功响应后直接 return，按钮永久停在"提交中..."
const pageActive = ref(true)
// 延迟返回定时器：用户提前手动返回时必须清掉，否则会在来源页再 pop 一次
const backTimers = []
onUnload(() => {
  pageActive.value = false
  backTimers.forEach(clearTimeout)
  backTimers.length = 0
})

/** 延迟执行（用于提示后返回）；页面已卸载则放弃，定时器在 onUnload 统一清理 */
function delayRun(fn, delay) {
  const timer = setTimeout(() => { if (pageActive.value) fn() }, delay)
  backTimers.push(timer)
}

/**
 * 返回上一页；本页为栈底（H5 直链/刷新、外部唤入等）时 navigateBack 必然 fail，
 * 改 switchTab 回首页（再 fail 用 reLaunch 兜底），否则页面会停在原地：
 * 提交成功路径刻意保持 submitting=true，navigateBack 失败即按钮永久"提交中..."
 */
function goBackOrHome() {
  if (getCurrentPages().length > 1) {
    uni.navigateBack()
    return
  }
  uni.switchTab({
    url: '/pages/index',
    fail: () => uni.reLaunch({ url: '/pages/index' })
  })
}

onLoad((options) => {
  const id = Number(options?.taskId)
  // 主键必须是正整数；0/负数/小数/空格会放行到 400 或拼错 path
  if (!options?.taskId || !Number.isInteger(id) || id <= 0) {
    proxy.$modal.msgError('缺少关联任务')
    delayRun(goBackOrHome, BACK_DELAY_MS)
    return
  }
  taskId.value = id
  loadContext()
})

async function loadContext() {
  try {
    const res = await getReportContext(taskId.value)
    ctx.value = res.data || {}
    contextReady.value = true
  } catch {
    // request 封装已 toast 具体原因；停留在此是死路，退回上一页重新进入
    if (pageActive.value) {
      proxy.$modal.msgError('任务信息加载失败')
      delayRun(goBackOrHome, BACK_DELAY_MS)
    }
  }
}

function isAcceptableImage(path, file) {
  const name = file?.name || path || ''
  const dot = String(name).lastIndexOf('.')
  const ext = dot > -1 ? String(name).slice(dot + 1).toLowerCase() : ''
  if (ext && !ALLOWED_IMAGE_EXT.includes(ext)) return false
  // size 未知（老端取不到）放行；0 字节/超限拒绝，避免白图与上传失败
  if (typeof file?.size === 'number') {
    if (file.size <= 0) return false
    if (file.size / 1024 / 1024 > MAX_IMAGE_MB) return false
  }
  return true
}

/** 上传竞速超时：超时按失败 reject，保证 pending/uploading 必然归零（共用 upload.js 不改） */
function uploadImageWithTimeout(path) {
  return Promise.race([
    uploadExceptionImage(path),
    new Promise((_, reject) => setTimeout(() => reject(new Error('upload timeout')), UPLOAD_TIMEOUT_MS))
  ])
}

async function takePhoto() {
  // 提交在途时禁止再起上传：成功路径随即返回，新传图不会进入已提交单据
  if (submitting.value) return
  const remain = MAX_SCENE_IMAGES - imageList.value.length - uploading.value
  if (remain <= 0) return
  let picked
  try {
    picked = await chooseImageAsync({ count: remain, sizeType: ['compressed'] })
  } catch (err) {
    // 用户取消保持静默（H5 reject 的是中文“取消选择”，App 端 errMsg 含 cancel）
    const msg = String(err?.errMsg || err?.message || '')
    if (pageActive.value && msg && !/cancel|取消/i.test(msg)) {
      uni.showToast({ title: '无法打开相机/相册，请检查授权', icon: 'none' })
    }
    return
  }
  // 先统一校验，非法张合并成一条提示，避免多条 toast 互相覆盖
  const accepted = []
  let rejected = 0
  picked.tempFilePaths.forEach((path, idx) => {
    if (isAcceptableImage(path, picked.tempFiles?.[idx])) accepted.push(path)
    else rejected++
  })
  if (rejected > 0 && pageActive.value) {
    uni.showToast({ title: `已跳过 ${rejected} 张（格式不符、空文件或超 ${MAX_IMAGE_MB}MB）`, icon: 'none' })
  }
  if (!accepted.length) return
  // 按选择序占位，全部完成后按序拼接，避免并发完成顺序打乱九宫格
  const slots = new Array(accepted.length).fill('')
  // pending 按本批计数：两批并发上传时各自等自己的文件全部落地再拼接，避免 URL 丢失
  let pending = accepted.length
  uploading.value += accepted.length
  accepted.forEach((path, idx) => {
    uploadImageWithTimeout(path).then((r) => { slots[idx] = r.url })
      .catch(() => {
        if (pageActive.value) uni.showToast({ title: '图片上传失败', icon: 'none' })
      })
      .finally(() => {
        uploading.value = Math.max(0, uploading.value - 1)
        pending--
        if (pending === 0 && slots.some(Boolean)) {
          const urls = slots.filter(Boolean).join(',')
          sceneImages.value = sceneImages.value ? sceneImages.value + ',' + urls : urls
        }
      })
  })
}

function removeImg(i) {
  const arr = sceneImages.value ? sceneImages.value.split(',').filter(Boolean) : []
  arr.splice(i, 1)
  sceneImages.value = arr.join(',')
}

function previewImg(i) {
  uni.previewImage({ current: i, urls: imageList.value })
}

async function submit() {
  if (!contextReady.value) { proxy.$modal.msg('任务信息未加载完成，请稍候'); return }
  if (!form.exceptionType) { proxy.$modal.msg('请选择异常类型'); return }
  if (form.description.trim().length < 2) { proxy.$modal.msg('请填写异常说明（至少 2 个字）'); return }
  if (uploading.value > 0) { proxy.$modal.msg('图片仍在上传，请稍候'); return }
  let impactQuantity = null
  if (form.impactQuantity !== '') {
    impactQuantity = Number(form.impactQuantity)
    if (!Number.isFinite(impactQuantity) || impactQuantity <= 0) {
      proxy.$modal.msg('影响数量需为大于 0 的数字，或清空留空')
      return
    }
  }
  if (sceneImages.value.length > SCENE_IMAGES_MAX_LEN) {
    proxy.$modal.msg(`现场照片地址过长（${sceneImages.value.length}），请减少照片后再提交`)
    return
  }
  const data = {
    taskId: taskId.value,
    exceptionType: form.exceptionType,
    description: form.description.trim(),
    sceneImages: sceneImages.value || undefined
  }
  if (impactQuantity !== null) {
    data.impactQuantity = impactQuantity
  }
  submitting.value = true
  try {
    await addException(data)
    if (!pageActive.value) return
    proxy.$modal.msgSuccess('异常单已提交，待主管处理')
    // 成功后保持提交态直到页面返回，避免延迟返回窗口内重复提单
    delayRun(goBackOrHome, SUCCESS_BACK_DELAY_MS)
  } catch (e) {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
page { background-color: #f5f6f7; min-height: 100%; }
.container { padding: 0 0 160rpx; }

.form-card {
  background: #fff; margin: 16rpx 24rpx; border-radius: 16rpx; padding: 8rpx 24rpx;
}
.ctx-row {
  display: flex; align-items: center; gap: 16rpx;
  padding: 20rpx 0; border-bottom: 1rpx solid #f5f5f5;
  &:last-child { border-bottom: none; }
}
.form-row { display: flex; align-items: center; padding: 12rpx 0; }
.label { color: #333; font-size: 28rpx; font-weight: 500; min-width: 110rpx; }
.ctx-val { flex: 1; font-size: 26rpx; color: #606266; word-break: break-all; }
.qty-input { flex: 1; }
.desc-box { padding: 8rpx 0 16rpx; }

.type-grid {
  display: flex; flex-wrap: wrap; gap: 16rpx; margin: 16rpx 24rpx;
}
.type-card {
  width: calc(50% - 8rpx); box-sizing: border-box;
  background: #fff; border: 2rpx solid #e4e7ed; border-radius: 16rpx;
  padding: 24rpx 20rpx; display: flex; flex-direction: column; gap: 8rpx;
  &.active { border-color: #f56c6c; background: #fef0f0; }
}
.type-name { font-size: 30rpx; font-weight: 600; color: #303133; }
.type-hint { font-size: 24rpx; color: #909399; }
.type-card.active .type-name { color: #f56c6c; }

.photo-row { display: flex; flex-wrap: wrap; gap: 12rpx; padding: 16rpx 0; }
.photo-item { position: relative; width: 140rpx; height: 140rpx; }
.photo-img { width: 100%; height: 100%; border-radius: 8rpx; }
.photo-del { position: absolute; top: -10rpx; right: -10rpx; background: #fff; border-radius: 50%; }
.photo-add {
  width: 140rpx; height: 140rpx; border: 2rpx dashed #dcdfe6; border-radius: 8rpx;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4rpx;
}
.uploading-text { font-size: 20rpx; color: #409eff; }

.footer-bar {
  position: fixed; left: 0; right: 0; bottom: 0;
  padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));
  background: #fff; border-top: 1px solid #eee;
}
.cu-btn { border-radius: 12rpx; border: none; }
.cu-btn.lg { width: 100%; font-size: 30rpx; height: 88rpx; line-height: 88rpx; }
.bg-red { background: #f56c6c; color: #fff; }
.bg-disabled { background: #c8c9cc; color: #fff; }
.cu-btn[disabled] { opacity: 0.8; }
</style>
