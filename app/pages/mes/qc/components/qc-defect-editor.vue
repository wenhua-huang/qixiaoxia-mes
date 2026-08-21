<template>
  <view class="defect-editor">
    <view class="de-header">
      <text class="de-title">缺陷记录</text>
      <text v-if="!readonly" class="de-add" @click="addRecord">+ 添加缺陷</text>
    </view>
    <view v-if="!list.length" class="de-empty">暂无缺陷记录</view>
    <view v-for="(rec, idx) in list" :key="idx" class="de-card">
      <view class="de-row">
        <text class="de-label">缺陷</text>
        <picker :range="defectNames" :value="defectIndex(rec)" @change="(e) => pickDefect(rec, e.detail.value)">
          <view class="de-pick">{{ rec.defectName || '请选择缺陷' }}</view>
        </picker>
      </view>
      <view class="de-row">
        <text class="de-label">等级</text>
        <view class="de-tag" :class="rec.defectLevel" @click="!readonly && pickLevel(rec)">
          {{ levelText(rec.defectLevel) || '请选择' }}
        </view>
        <text class="de-label" style="margin-left:24rpx">数量</text>
        <uni-number-box v-model="rec.defectQuantity" :min="1" :step="1" :disabled="readonly" />
      </view>
      <view class="de-row">
        <text class="de-label">处置</text>
        <view class="de-pick" @click="!readonly && pickProcess(rec)">{{ rec.processMethod || '请选择处置方式' }}</view>
      </view>
      <view class="photo-row">
        <view v-for="(img, i) in imagesOf(rec)" :key="i" class="photo-item">
          <image :src="img" mode="aspectFill" class="photo-img" @click="previewImg(rec, i)" />
          <uni-icons v-if="!readonly" type="closeempty" size="18" class="photo-del" @click="removeImg(rec, i)" />
        </view>
        <view v-if="!readonly && imagesOf(rec).length < 9" class="photo-add" @click="takePhoto(rec)">
          <uni-icons type="camera-filled" size="24" :color="uploading ? '#409eff' : '#999'" />
        </view>
      </view>
      <view v-if="!readonly" class="de-del" @click="removeRecord(idx)">删除</view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import uniIcons from '@/uni_modules/uni-icons/components/uni-icons/uni-icons.vue'
import uniNumberBox from '@/uni_modules/uni-number-box/components/uni-number-box/uni-number-box.vue'
import { uploadQcImage } from '@/api/mes/qc/upload'
import { DEFECT_LEVEL_ORDER, defectLevelText } from '@/utils/qc'
import { chooseImageAsync } from '@/utils/chooseImage'
import { normalizeImageUrl } from '@/utils/image'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  readonly: { type: Boolean, default: false },
  defectOptions: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:modelValue', 'uploading-change'])

const list = computed(() => props.modelValue || [])
const defectNames = computed(() => props.defectOptions.map(d => d.defectName))
const uploading = ref(0)

function defectIndex(rec) {
  return props.defectOptions.findIndex(d => d.defectId === rec.defectId)
}
function levelText(l) { return defectLevelText(l) }

function pickDefect(rec, i) {
  const d = props.defectOptions[i]
  if (!d) return
  rec.defectId = d.defectId
  rec.defectCode = d.defectCode
  rec.defectName = d.defectName
  if (!rec.defectLevel) rec.defectLevel = d.defectLevel
  if (!rec.processMethod) rec.processMethod = d.processMethod
}
function pickLevel(rec) {
  uni.showActionSheet({
    itemList: DEFECT_LEVEL_ORDER.map(defectLevelText),
    success: (res) => { rec.defectLevel = DEFECT_LEVEL_ORDER[res.tapIndex] }
  })
}
function pickProcess(rec) {
  const items = ['返工', '返修', '报废', '让步接收', '退货']
  uni.showActionSheet({
    itemList: items,
    success: (res) => { rec.processMethod = items[res.tapIndex] }
  })
}
function addRecord() {
  const arr = list.value.slice()
  arr.push({ defectId: null, defectCode: '', defectName: '', defectLevel: 'MAJOR', defectQuantity: 1, processMethod: '', defectImage: '' })
  emit('update:modelValue', arr)
}
function removeRecord(idx) {
  const arr = list.value.slice()
  arr.splice(idx, 1)
  emit('update:modelValue', arr)
}
function imagesOf(rec) {
  return rec.defectImage ? rec.defectImage.split(',').filter(Boolean).map(normalizeImageUrl) : []
}
function takePhoto(rec) {
  chooseImageAsync({ count: 1, sizeType: ['compressed'] }).then((res) => {
    uploading.value++
    emit('uploading-change', true)
    uploadQcImage(res.tempFilePaths[0]).then((r) => {
      rec.defectImage = rec.defectImage ? rec.defectImage + ',' + r.url : r.url
    }).catch(() => uni.showToast({ title: '图片上传失败', icon: 'none' }))
      .finally(() => { uploading.value = Math.max(0, uploading.value - 1); if (!uploading.value) emit('uploading-change', false) })
  }).catch(() => {})
}
function removeImg(rec, i) {
  // 始终操作原始存储字符串：imagesOf 是规范化后的展示值，写回会把相对路径污染进库
  const arr = rec.defectImage ? rec.defectImage.split(',').filter(Boolean) : []
  arr.splice(i, 1)
  rec.defectImage = arr.join(',')
}
function previewImg(rec, i) {
  uni.previewImage({ current: i, urls: imagesOf(rec) })
}
</script>

<style lang="scss" scoped>
.de-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.de-title { font-size: 30rpx; font-weight: 600; }
.de-add { font-size: 26rpx; color: #409eff; }
.de-empty { font-size: 26rpx; color: #999; padding: 20rpx 0; }
.de-card { background: #fff; border-radius: 12rpx; padding: 20rpx; margin-bottom: 16rpx; }
.de-row { display: flex; align-items: center; gap: 12rpx; margin-bottom: 16rpx; flex-wrap: wrap; }
.de-label { font-size: 26rpx; color: #606266; min-width: 60rpx; }
.de-pick { flex: 1; font-size: 28rpx; color: #303133; border: 2rpx solid #dcdfe6; border-radius: 8rpx; padding: 12rpx 16rpx; min-width: 200rpx; }
.de-tag { padding: 8rpx 20rpx; border-radius: 8rpx; font-size: 26rpx; border: 2rpx solid #dcdfe6;
  &.CRITICAL { color: #f56c6c; border-color: #f56c6c; }
  &.MAJOR { color: #e6a23c; border-color: #e6a23c; }
  &.MINOR { color: #909399; border-color: #909399; } }
.de-del { text-align: right; font-size: 26rpx; color: #f56c6c; margin-top: 8rpx; }
.photo-row { display: flex; flex-wrap: wrap; gap: 12rpx; }
.photo-item { position: relative; width: 130rpx; height: 130rpx; }
.photo-img { width: 100%; height: 100%; border-radius: 8rpx; }
.photo-del { position: absolute; top: -10rpx; right: -10rpx; background: #fff; border-radius: 50%; }
.photo-add { width: 130rpx; height: 130rpx; border: 2rpx dashed #dcdfe6; border-radius: 8rpx; display: flex; align-items: center; justify-content: center; }
</style>
