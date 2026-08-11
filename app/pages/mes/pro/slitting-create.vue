<template>
  <view class="container">
    <!-- 选择外协厂商 -->
    <uni-section title="外协厂商" type="line"></uni-section>
    <view class="form-card">
      <view class="form-row" @click="showVendorPicker = true">
        <text class="label">厂商</text>
        <view class="picker-value">
          <text :class="form.vendorName ? 'value' : 'placeholder'">
            {{ form.vendorName || '请选择外协厂商' }}
          </text>
          <uni-icons type="right" size="16" color="#999"></uni-icons>
        </view>
      </view>
    </view>

    <!-- 选择母卷 -->
    <uni-section title="发料母卷" type="line"></uni-section>
    <view class="form-card">
      <button class="cu-btn bg-blue sm full-btn" @click="loadParentRolls">+ 选择母卷</button>
      <view v-if="selectedRolls.length === 0" class="empty-hint">
        <text class="text-grey">点击上方按钮添加发料母卷</text>
      </view>
      <view v-else class="roll-list">
        <view v-for="(roll, idx) in selectedRolls" :key="roll.rollId" class="roll-item">
          <view class="roll-info">
            <text class="roll-code">{{ roll.rollCode }}</text>
            <text class="roll-detail">{{ roll.itemName }}</text>
            <text class="roll-meta">{{ roll.actualWidth }}mm · {{ roll.actualWeight }}吨</text>
          </view>
          <uni-icons type="close" size="18" color="#f56c6c" @click="removeRoll(idx)"></uni-icons>
        </view>
      </view>
      <view v-if="selectedRolls.length > 0" class="total-bar">
        <text>共 {{ selectedRolls.length }} 个母卷 · {{ totalWeight }} 吨</text>
      </view>
    </view>

    <!-- 母卷选择弹窗 -->
    <uni-popup ref="rollPopup" type="bottom" :is-mask-click="true">
      <view class="popup-content">
        <view class="popup-header">
          <text class="popup-title">选择发料母卷（在库）</text>
          <text class="popup-close" @click="closeRollPopup">关闭</text>
        </view>
        <scroll-view scroll-y class="popup-scroll">
          <view v-if="rollOptions.length === 0" class="empty-hint">
            <text class="text-grey">暂无可发料母卷</text>
          </view>
          <view
            v-for="roll in rollOptions"
            :key="roll.rollId"
            :class="['popup-item', isSelected(roll.rollId) ? 'selected' : '']"
            @click="toggleRoll(roll)"
          >
            <view class="roll-info">
              <text class="roll-code">{{ roll.rollCode }}</text>
              <text class="roll-detail">{{ roll.itemName }}</text>
              <text class="roll-meta">{{ roll.actualWidth }}mm · {{ roll.actualWeight }}吨 · {{ roll.warehouseName }}</text>
            </view>
            <uni-icons :type="isSelected(roll.rollId) ? 'checkbox-filled' : 'circle'" size="22" :color="isSelected(roll.rollId) ? '#007aff' : '#ccc'"></uni-icons>
          </view>
        </scroll-view>
        <button class="cu-btn bg-blue" @click="confirmRolls">确认添加({{ tempSelected.length }})</button>
      </view>
    </uni-popup>

    <!-- 厂商选择弹窗 -->
    <uni-popup ref="vendorPopup" type="bottom" :is-mask-click="true">
      <view class="popup-content">
        <view class="popup-header">
          <text class="popup-title">选择外协厂商</text>
          <text class="popup-close" @click="showVendorPicker = false">关闭</text>
        </view>
        <scroll-view scroll-y class="popup-scroll">
          <view
            v-for="v in vendorOptions"
            :key="v.vendorId"
            :class="['popup-item', form.vendorId === v.vendorId ? 'selected' : '']"
            @click="pickVendor(v)"
          >
            <view>
              <text class="roll-code">{{ v.vendorName }}</text>
              <text class="roll-meta">{{ v.vendorCode }}</text>
            </view>
            <uni-icons :type="form.vendorId === v.vendorId ? 'checkbox-filled' : 'circle'" size="22" :color="form.vendorId === v.vendorId ? '#007aff' : '#ccc'"></uni-icons>
          </view>
        </scroll-view>
      </view>
    </uni-popup>

    <!-- 底部提交 -->
    <view class="footer-bar">
      <button class="cu-btn bg-blue lg" :disabled="!canSubmit || submitting" @click="submit">
        {{ submitting ? '提交中...' : '确认发料' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, watch, getCurrentInstance } from 'vue'
import { executeSlitting, listAvailableParentRolls, listOutsourceVendor } from '@/api/mes/pro/slitting'

const { proxy } = getCurrentInstance()

const form = reactive({
  vendorId: null, vendorCode: '', vendorName: ''
})
const selectedRolls = ref([])
const submitting = ref(false)

// 弹窗状态
const showVendorPicker = ref(false)
const rollPopup = ref()
const vendorPopup = ref()
const rollOptions = ref([])
const vendorOptions = ref([])
const tempSelected = ref([])

const totalWeight = computed(() => {
  return selectedRolls.value.reduce((sum, r) => sum + Number(r.actualWeight || 0), 0).toFixed(4)
})

const canSubmit = computed(() => form.vendorId != null && selectedRolls.value.length > 0)

// 预加载厂商
async function loadVendors() {
  try {
    const res = await listOutsourceVendor()
    vendorOptions.value = (res.data || []).filter(v => v.vendorType === 'OUTSOURCE' || v.vendorType === 'BOTH')
  } catch (e) {}
}
loadVendors()

async function loadParentRolls() {
  try {
    const res = await listAvailableParentRolls()
    rollOptions.value = res.data || []
    tempSelected.value = [...selectedRolls.value]
    rollPopup.value.open()
  } catch (e) {
    proxy.$modal.msgError('加载母卷失败')
  }
}

function closeRollPopup() {
  rollPopup.value.close()
}

function isSelected(rollId) {
  return tempSelected.value.some(r => r.rollId === rollId)
}

function toggleRoll(roll) {
  const idx = tempSelected.value.findIndex(r => r.rollId === roll.rollId)
  if (idx >= 0) {
    tempSelected.value.splice(idx, 1)
  } else {
    tempSelected.value.push(roll)
  }
}

function confirmRolls() {
  selectedRolls.value = [...tempSelected.value]
  rollPopup.value.close()
}

function removeRoll(idx) {
  selectedRolls.value.splice(idx, 1)
}

function pickVendor(v) {
  form.vendorId = v.vendorId
  form.vendorCode = v.vendorCode
  form.vendorName = v.vendorName
  showVendorPicker.value = false
  vendorPopup.value.close()
}

// 监听 vendorPopup 弹窗开关
watch(showVendorPicker, (val) => {
  if (val) vendorPopup.value.open()
})

async function submit() {
  if (!form.vendorId) { proxy.$modal.msg('请选择外协厂商'); return }
  if (selectedRolls.value.length === 0) { proxy.$modal.msg('请选择发料母卷'); return }
  submitting.value = true
  try {
    await executeSlitting({
      slitMode: 'OUTSOURCE',
      vendorId: form.vendorId,
      vendorCode: form.vendorCode,
      vendorName: form.vendorName,
      parentRollIds: selectedRolls.value.map(r => r.rollId)
    })
    proxy.$modal.msgSuccess('已发出 ' + selectedRolls.value.length + ' 个母卷')
    setTimeout(() => proxy.$tab.navigateBack(), 1500)
  } catch (e) {} finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
page { background-color: #f5f6f7; min-height: 100%; padding-bottom: 160rpx; }
.container { padding: 0 0 160rpx; }

.form-card {
  background: #fff; margin: 16rpx 24rpx; border-radius: 16rpx; padding: 16rpx 24rpx;
}
.form-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20rpx 0;
}
.label { color: #333; font-size: 28rpx; font-weight: 500; }
.picker-value { display: flex; align-items: center; gap: 8rpx; }
.value { color: #333; font-size: 28rpx; }
.placeholder { color: #ccc; font-size: 28rpx; }

.full-btn { width: 100%; margin-bottom: 16rpx; }
.empty-hint { padding: 40rpx 0; text-align: center; }
.text-grey { color: #999; font-size: 26rpx; }

.roll-list { }
.roll-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20rpx 0; border-bottom: 1px solid #f5f5f5;
}
.roll-info { display: flex; flex-direction: column; gap: 4rpx; }
.roll-code { font-size: 28rpx; font-weight: 600; color: #333; }
.roll-detail { font-size: 26rpx; color: #666; }
.roll-meta { font-size: 24rpx; color: #999; }
.total-bar {
  padding: 20rpx 0; text-align: right;
  font-size: 26rpx; color: #007aff; font-weight: 600;
}

.popup-content {
  background: #fff; border-radius: 24rpx 24rpx 0 0;
  max-height: 80vh; display: flex; flex-direction: column;
}
.popup-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 24rpx; border-bottom: 1px solid #f0f0f0;
}
.popup-title { font-size: 30rpx; font-weight: 600; }
.popup-close { color: #999; font-size: 28rpx; }
.popup-scroll { max-height: 60vh; }
.popup-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 24rpx; border-bottom: 1px solid #f5f5f5;
}
.popup-item.selected { background: #f0f7ff; }

.footer-bar {
  position: fixed; left: 0; right: 0; bottom: 0;
  padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));
  background: #fff; border-top: 1px solid #eee;
}
.cu-btn { border-radius: 12rpx; border: none; }
.cu-btn.sm { font-size: 26rpx; height: 64rpx; line-height: 64rpx; }
.cu-btn.lg { width: 100%; font-size: 30rpx; height: 88rpx; line-height: 88rpx; }
.bg-blue { background: #007aff; color: #fff; }
.cu-btn[disabled] { opacity: 0.5; }
</style>
