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
        {{ submitting ? '提交中...' : (uploading ? '图片上传中，请稍候' : '提交异常单') }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, getCurrentInstance } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
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

const taskId = ref(null)
const ctx = ref({})
const form = reactive({ exceptionType: '', impactQuantity: '', description: '' })
// 库内原始形态：MinIO 返回的 URL 逗号串（展示一律走 normalizeImageUrl，避免相对路径回写污染）
const sceneImages = ref('')
const uploading = ref(0)
const submitting = ref(false)

const imageList = computed(() =>
  sceneImages.value ? sceneImages.value.split(',').filter(Boolean).map(normalizeImageUrl) : [])
const canSubmit = computed(() =>
  !!form.exceptionType && form.description.trim().length >= 2 && uploading.value === 0)

onLoad((options) => {
  if (!options || !options.taskId) {
    proxy.$modal.msgError('缺少关联任务')
    setTimeout(() => uni.navigateBack(), 800)
    return
  }
  taskId.value = Number(options.taskId)
  loadContext()
})

async function loadContext() {
  try {
    const res = await getReportContext(taskId.value)
    ctx.value = res.data || {}
  } catch (e) {
    proxy.$modal.msgError('任务信息加载失败')
  }
}

function takePhoto() {
  const remain = MAX_SCENE_IMAGES - imageList.value.length - uploading.value
  if (remain <= 0) return
  chooseImageAsync({ count: remain, sizeType: ['compressed'] }).then((res) => {
    // H5 可选多张；逐张上传，失败提示并保留已成功部分
    res.tempFilePaths.forEach((path) => {
      uploading.value++
      uploadExceptionImage(path).then((r) => {
        sceneImages.value = sceneImages.value ? sceneImages.value + ',' + r.url : r.url
      }).catch(() => uni.showToast({ title: '图片上传失败', icon: 'none' }))
        .finally(() => { uploading.value = Math.max(0, uploading.value - 1) })
    })
  }).catch(() => {})
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
  if (!form.exceptionType) { proxy.$modal.msg('请选择异常类型'); return }
  if (form.description.trim().length < 2) { proxy.$modal.msg('请填写异常说明（至少 2 个字）'); return }
  if (uploading.value > 0) { proxy.$modal.msg('图片仍在上传，请稍候'); return }
  const qty = Number(form.impactQuantity)
  const data = {
    taskId: taskId.value,
    exceptionType: form.exceptionType,
    description: form.description.trim(),
    sceneImages: sceneImages.value || undefined
  }
  if (form.impactQuantity !== '' && !Number.isNaN(qty) && qty > 0) {
    data.impactQuantity = qty
  }
  submitting.value = true
  try {
    await addException(data)
    proxy.$modal.msgSuccess('异常单已提交，待主管处理')
    // 成功后保持提交态直到页面返回，避免延迟返回窗口内重复提单
    setTimeout(() => proxy.$tab.navigateBack(), 1200)
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
