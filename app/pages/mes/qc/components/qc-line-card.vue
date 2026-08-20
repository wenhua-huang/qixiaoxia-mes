<template>
  <view class="line-card" :class="{ 'is-error': required && !line.checkValText && showError }">
    <view class="line-header">
      <text class="line-name">{{ line.indexName }}</text>
      <text v-if="required" class="required-tag">必检</text>
      <text v-if="previewTag" class="preview-tag" :class="previewTag === 'PASS' ? 'ok' : 'bad'">
        {{ previewTag === 'PASS' ? '正常' : '超差' }}
      </text>
    </view>
    <view class="line-meta" v-if="line.qcTool || line.checkMethod">
      <text v-if="line.qcTool">工具：{{ line.qcTool }}</text>
      <text v-if="line.checkMethod" class="meta-method">方法：{{ line.checkMethod }}</text>
    </view>

    <!-- NUMBER / COUNT -->
    <view v-if="line.qcResultType === 'NUMBER' || line.qcResultType === 'COUNT'" class="field-row">
      <uni-easyinput
        v-model="line.checkValText"
        type="number"
        :inputBorder="true"
        :disabled="readonly"
        :placeholder="rangeHint"
        @blur="onPreview"
      />
      <text v-if="line.unitOfMeasure" class="unit">{{ line.unitOfMeasure }}</text>
    </view>

    <!-- DICT：PASS/FAIL 按钮组 -->
    <view v-else-if="line.qcResultType === 'DICT'" class="dict-row">
      <view
        v-for="opt in dictOptions"
        :key="opt.value"
        class="dict-btn"
        :class="{ active: line.checkValText === opt.value, pass: opt.value === 'PASS', fail: opt.value === 'FAIL' }"
        @click="!readonly && (line.checkValText = opt.value)"
      >{{ opt.label }}</view>
    </view>

    <!-- TEXT -->
    <view v-else-if="line.qcResultType === 'TEXT'">
      <uni-easyinput v-model="line.checkValText" type="textarea" :maxlength="500"
        :inputBorder="true" :disabled="readonly" placeholder="请输入检验描述" />
    </view>

    <!-- FILE：图片 -->
    <view v-else-if="line.qcResultType === 'FILE'" class="photo-row">
      <view v-for="(img, idx) in imageList" :key="idx" class="photo-item">
        <image :src="img" mode="aspectFill" class="photo-img" @click="previewImg(idx)" />
        <uni-icons v-if="!readonly" type="closeempty" size="18" class="photo-del" @click="removeImg(idx)" />
      </view>
      <view v-if="!readonly && imageList.length < 9" class="photo-add" @click="takePhoto">
        <uni-icons type="camera-filled" size="28" color="#999" />
        <text class="text-grey">{{ uploading ? '上传中…' : '拍照' }}</text>
      </view>
    </view>

    <!-- 三档缺陷数 -->
    <view v-if="!readonly" class="defect-row">
      <view class="defect-cell">
        <text class="defect-label cr">致命</text>
        <uni-number-box v-model="line.crQuantity" :min="0" :step="1" :disabled="readonly" />
      </view>
      <view class="defect-cell">
        <text class="defect-label maj">严重</text>
        <uni-number-box v-model="line.majQuantity" :min="0" :step="1" :disabled="readonly" />
      </view>
      <view class="defect-cell">
        <text class="defect-label min">轻微</text>
        <uni-number-box v-model="line.minQuantity" :min="0" :step="1" :disabled="readonly" />
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import uniIcons from '@/uni_modules/uni-icons/components/uni-icons/uni-icons.vue'
import uniEasyinput from '@/uni_modules/uni-easyinput/components/uni-easyinput/uni-easyinput.vue'
import uniNumberBox from '@/uni_modules/uni-number-box/components/uni-number-box/uni-number-box.vue'
import { uploadQcImage } from '@/api/mes/qc/upload'
import { judgeLine } from '@/utils/qc'

const props = defineProps({
  line: { type: Object, required: true },
  readonly: { type: Boolean, default: false },
  required: { type: Boolean, default: false },
  showError: { type: Boolean, default: false }
})

const dictOptions = [
  { label: '合格', value: 'PASS' },
  { label: '不合格', value: 'FAIL' }
]
const uploading = ref(false)

const imageList = computed(() => {
  const v = props.line.checkValText
  return v ? v.split(',').filter(Boolean) : []
})

const rangeHint = computed(() => {
  const std = props.line.standerVal
  if (std != null) {
    const lo = props.line.thresholdMin != null ? Number(std) + Number(props.line.thresholdMin) : null
    const hi = props.line.thresholdMax != null ? Number(std) + Number(props.line.thresholdMax) : null
    if (lo != null && hi != null) return `标准 ${std}（${lo}~${hi}）`
    if (lo != null) return `≥ ${lo}`
    if (hi != null) return `≤ ${hi}`
    return `标准 ${std}`
  }
  return '0'
})

const previewTag = computed(() => {
  if (props.line.qcResultType !== 'NUMBER') return null
  const r = judgeLine(props.line)
  return r
})

function onPreview() { /* trigger 重算 previewTag */ }

function takePhoto() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['camera', 'album'],
    success: (res) => {
      const path = res.tempFilePaths[0]
      uploading.value = true
      uploadQcImage(path).then((r) => {
        const url = r.url
        const cur = props.line.checkValText ? props.line.checkValText + ',' : ''
        props.line.checkValText = cur + url
      }).catch(() => {
        uni.showToast({ title: '图片上传失败', icon: 'none' })
      }).finally(() => { uploading.value = false })
    }
  })
}
function removeImg(idx) {
  const arr = imageList.value.slice()
  arr.splice(idx, 1)
  props.line.checkValText = arr.join(',')
}
function previewImg(idx) {
  uni.previewImage({ current: idx, urls: imageList.value })
}
</script>

<style lang="scss" scoped>
.line-card {
  background: #fff;
  border-radius: 12rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  border-left: 6rpx solid #409eff;
  &.is-error { border-left-color: #f56c6c; }
}
.line-header { display: flex; align-items: center; gap: 12rpx; margin-bottom: 12rpx; }
.line-name { font-size: 30rpx; font-weight: 600; color: #303133; }
.required-tag { font-size: 22rpx; color: #f56c6c; }
.preview-tag { font-size: 22rpx; padding: 2rpx 12rpx; border-radius: 6rpx; margin-left: auto;
  &.ok { color: #67c23a; background: #f0f9eb; }
  &.bad { color: #f56c6c; background: #fef0f0; } }
.line-meta { font-size: 24rpx; color: #909399; margin-bottom: 12rpx; display: flex; gap: 20rpx; }
.field-row { display: flex; align-items: center; gap: 12rpx; }
.unit { font-size: 26rpx; color: #606266; min-width: 60rpx; }
.dict-row { display: flex; gap: 20rpx; }
.dict-btn { flex: 1; text-align: center; padding: 18rpx 0; border-radius: 8rpx; border: 2rpx solid #dcdfe6;
  font-size: 28rpx; color: #606266;
  &.active.pass { background: #f0f9eb; border-color: #67c23a; color: #67c23a; }
  &.active.fail { background: #fef0f0; border-color: #f56c6c; color: #f56c6c; } }
.photo-row { display: flex; flex-wrap: wrap; gap: 16rpx; }
.photo-item { position: relative; width: 160rpx; height: 160rpx; }
.photo-img { width: 100%; height: 100%; border-radius: 8rpx; }
.photo-del { position: absolute; top: -10rpx; right: -10rpx; background: #fff; border-radius: 50%; }
.photo-add { width: 160rpx; height: 160rpx; border: 2rpx dashed #dcdfe6; border-radius: 8rpx;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6rpx; }
.text-grey { font-size: 22rpx; color: #999; }
.defect-row { display: flex; gap: 16rpx; margin-top: 20rpx; }
.defect-cell { flex: 1; display: flex; align-items: center; justify-content: space-between; }
.defect-label { font-size: 24rpx; color: #606266;
  &.cr { color: #f56c6c; } &.maj { color: #e6a23c; } &.min { color: #909399; } }
</style>
