<template>
  <view class="container">
    <view v-if="loading" class="loading-box">
      <uni-load-more status="loading" />
    </view>

    <template v-else-if="record">
      <!-- 母卷信息 -->
      <uni-section title="母卷信息" type="line"></uni-section>
      <view class="info-card">
        <view class="info-row">
          <text class="label">批次号</text>
          <text class="value bold">{{ record.slitBatchNo }}</text>
        </view>
        <view class="info-row">
          <text class="label">母卷号</text>
          <text class="value">{{ record.parentRollCode }}</text>
        </view>
        <view class="info-row">
          <text class="label">母卷重量</text>
          <text class="value bold">{{ record.parentWeight }}吨</text>
        </view>
      </view>

      <!-- 子卷列表（厂商录入的结果） -->
      <uni-section :title="'收货子卷（' + childRolls.length + '）'" type="line"></uni-section>
      <view class="info-card">
        <view v-if="childRolls.length === 0" class="empty-hint">
          <text class="text-grey">厂商尚未录入分切结果</text>
        </view>
        <view v-else>
          <view v-for="roll in childRolls" :key="roll.rollId" class="roll-row">
            <view class="roll-main">
              <text class="roll-code">{{ roll.rollCode }}</text>
              <text class="roll-meta">{{ roll.actualWidth }}mm · {{ roll.actualWeightGsm || '-' }}g · {{ roll.actualWeight }}吨</text>
            </view>
          </view>
          <view class="summary-row">
            <text>子卷总重：{{ record.childTotalWeight }}吨</text>
          </view>
        </view>
      </view>

      <!-- 收货配置 -->
      <uni-section title="收货入库" type="line"></uni-section>
      <view class="info-card">
        <view class="form-row" @click="showWarehousePicker = true">
          <text class="label">收货仓库</text>
          <view class="picker-value">
            <text :class="form.receiveWarehouseName ? 'value' : 'placeholder'">
              {{ form.receiveWarehouseName || '请选择仓库' }}
            </text>
            <uni-icons type="right" size="16" color="#999"></uni-icons>
          </view>
        </view>

        <view class="form-row">
          <text class="label">纸边重量(kg)</text>
          <view class="num-input-wrap">
            <uni-easyinput v-model="form.edgeWeight" type="digit" placeholder="可选" :inputBorder="false" />
          </view>
        </view>
      </view>

      <!-- 重量校验 -->
      <view class="weight-bar" :class="weightValid ? 'ok' : 'err'">
        <text>子卷+纸边：{{ totalInput }}吨 / 母卷 {{ record.parentWeight }}吨</text>
        <text class="weight-loss">损耗率：{{ lossRate }}%</text>
      </view>

      <!-- 仓库选择弹窗 -->
      <uni-popup ref="whPopup" type="bottom" :is-mask-click="true">
        <view class="popup-content">
          <view class="popup-header">
            <text class="popup-title">选择收货仓库</text>
            <text class="popup-close" @click="showWarehousePicker = false">关闭</text>
          </view>
          <scroll-view scroll-y class="popup-scroll">
            <view
              v-for="w in warehouseOptions"
              :key="w.warehouseId"
              :class="['popup-item', form.receiveWarehouseId === w.warehouseId ? 'selected' : '']"
              @click="pickWarehouse(w)"
            >
              <view>
                <text class="roll-code">{{ w.warehouseName }}</text>
                <text class="roll-meta">{{ w.warehouseCode }}</text>
              </view>
              <uni-icons :type="form.receiveWarehouseId === w.warehouseId ? 'checkbox-filled' : 'circle'" size="22" :color="form.receiveWarehouseId === w.warehouseId ? '#007aff' : '#ccc'"></uni-icons>
            </view>
          </scroll-view>
        </view>
      </uni-popup>

      <!-- 底部提交 -->
      <view class="footer-bar">
        <button class="cu-btn bg-green lg" :disabled="!canSubmit || submitting" @click="submit">
          {{ submitting ? '提交中...' : '确认收货' }}
        </button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { ref, reactive, computed, watch, getCurrentInstance } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getSlitting, receiveOutsource } from '@/api/mes/pro/slitting'
import { listAllWarehouse } from '@/api/mes/wm/warehouse'

const { proxy } = getCurrentInstance()

const record = ref(null)
const loading = ref(true)
const submitting = ref(false)
const whPopup = ref()
const showWarehousePicker = ref(false)
const warehouseOptions = ref([])

const form = reactive({
  receiveWarehouseId: null, receiveWarehouseCode: '', receiveWarehouseName: '',
  edgeItemId: null, edgeItemCode: '', edgeItemName: '',
  edgeWeight: ''
})

const childRolls = computed(() => record.value?.childRolls || [])
const childTotalWeight = computed(() => Number(record.value?.childTotalWeight || 0))
const edgeWeightTon = computed(() => Number(form.edgeWeight || 0) / 1000)
const totalInput = computed(() => (childTotalWeight.value + edgeWeightTon.value).toFixed(4))
const parentWeight = computed(() => Number(record.value?.parentWeight || 0))
const lossRate = computed(() => {
  if (parentWeight.value <= 0) return '0.00'
  const loss = parentWeight.value - childTotalWeight.value - edgeWeightTon.value
  return ((loss / parentWeight.value) * 100).toFixed(2)
})
const weightValid = computed(() => {
  const loss = parentWeight.value - childTotalWeight.value - edgeWeightTon.value
  return loss >= 0 && Number(lossRate.value) <= 3
})
const canSubmit = computed(() => form.receiveWarehouseId != null && weightValid.value)

async function loadWarehouses() {
  try {
    const res = await listAllWarehouse()
    warehouseOptions.value = res.data || []
  } catch (e) {}
}

function pickWarehouse(w) {
  form.receiveWarehouseId = w.warehouseId
  form.receiveWarehouseCode = w.warehouseCode
  form.receiveWarehouseName = w.warehouseName
  showWarehousePicker.value = false
  whPopup.value.close()
}

watch(showWarehousePicker, (val) => {
  if (val) whPopup.value.open()
})

async function submit() {
  if (!form.receiveWarehouseId) { proxy.$modal.msg('请选择收货仓库'); return }
  if (!weightValid.value) {
    proxy.$modal.alert('重量校验未通过，请检查数据')
    return
  }
  submitting.value = true
  try {
    await receiveOutsource(record.value.slitId, {
      receiveWarehouseId: form.receiveWarehouseId,
      receiveWarehouseCode: form.receiveWarehouseCode,
      receiveWarehouseName: form.receiveWarehouseName,
      edgeWeight: form.edgeWeight ? Number(form.edgeWeight) : 0
    })
    proxy.$modal.msgSuccess('收货成功，子卷已入库')
    setTimeout(() => proxy.$tab.navigateBack(), 1500)
  } catch (e) {} finally {
    submitting.value = false
  }
}

onLoad((options) => {
  const slitId = options.slitId
  if (!slitId) { proxy.$modal.msgError('缺少分切单ID'); return }
  loadWarehouses()
  getSlitting(slitId).then(res => {
    record.value = res.data
    loading.value = false
  }).catch(() => { loading.value = false })
})
</script>

<style lang="scss" scoped>
page { background-color: #f5f6f7; min-height: 100%; padding-bottom: 200rpx; }
.container { padding: 0 0 200rpx; }

.loading-box { display: flex; justify-content: center; padding: 80rpx 0; }

.info-card {
  background: #fff; margin: 16rpx 24rpx; border-radius: 16rpx; padding: 8rpx 24rpx;
}
.info-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20rpx 0; border-bottom: 1px solid #f5f5f5;
}
.info-row:last-child { border-bottom: none; }
.label { color: #999; font-size: 26rpx; }
.value { font-size: 28rpx; color: #333; }
.bold { font-weight: 600; }
.picker-value { display: flex; align-items: center; gap: 8rpx; }
.placeholder { color: #ccc; font-size: 28rpx; }

.form-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20rpx 0; border-bottom: 1px solid #f5f5f5;
}
.form-row:last-child { border-bottom: none; }
.num-input-wrap { width: 200rpx; }

.empty-hint { padding: 40rpx 0; text-align: center; }
.text-grey { color: #999; font-size: 26rpx; }

.roll-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20rpx 0; border-bottom: 1px solid #f5f5f5;
}
.roll-main { display: flex; flex-direction: column; gap: 4rpx; }
.roll-code { font-size: 28rpx; font-weight: 600; color: #333; }
.roll-meta { font-size: 24rpx; color: #999; }
.summary-row {
  padding: 20rpx 0; font-size: 26rpx; color: #333; font-weight: 600;
}

.weight-bar {
  margin: 16rpx 24rpx; padding: 20rpx 24rpx; border-radius: 12rpx;
  display: flex; justify-content: space-between; font-size: 26rpx;
}
.weight-bar.ok { background: #f0f9eb; color: #67c23a; }
.weight-bar.err { background: #fef0f0; color: #f56c6c; }
.weight-loss { font-weight: 600; }

.popup-content {
  background: #fff; border-radius: 24rpx 24rpx 0 0;
  max-height: 70vh; display: flex; flex-direction: column;
}
.popup-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 24rpx; border-bottom: 1px solid #f0f0f0;
}
.popup-title { font-size: 30rpx; font-weight: 600; }
.popup-close { color: #999; font-size: 28rpx; }
.popup-scroll { max-height: 56vh; }
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
.cu-btn.lg { width: 100%; font-size: 30rpx; height: 88rpx; line-height: 88rpx; }
.bg-green { background: #67c23a; color: #fff; }
.cu-btn[disabled] { opacity: 0.5; }
</style>
