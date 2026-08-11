<template>
  <view class="container">
    <view v-if="loading" class="loading-box">
      <uni-load-more status="loading" />
    </view>

    <template v-else-if="record">
      <!-- 外协单信息 -->
      <uni-section title="外协单信息" type="line"></uni-section>
      <view class="info-card">
        <view class="info-row">
          <text class="label">单号</text>
          <text class="value bold">{{ record.orderCode }}</text>
        </view>
        <view class="info-row">
          <text class="label">外协厂商</text>
          <text class="value">{{ record.vendorName }}</text>
        </view>
        <view class="info-row">
          <text class="label">状态</text>
          <uni-tag :type="statusTagType(record.status)" :text="statusText(record.status)" size="small" />
        </view>
      </view>

      <!-- 收货明细（厂商已录） -->
      <uni-section :title="'收货明细（' + (record.recptLines || []).length + '）'" type="line"></uni-section>
      <view class="info-card">
        <view v-if="!(record.recptLines || []).length" class="empty-hint">
          <text class="text-grey">厂商尚未录入加工结果</text>
        </view>
        <view v-else>
          <view v-for="line in record.recptLines" :key="line.lineId" class="roll-row">
            <view class="roll-main">
              <text class="roll-code">{{ line.itemName }}</text>
              <text class="roll-meta">{{ line.quantity }}{{ line.unitName }} · {{ line.warehouseName }}</text>
            </view>
          </view>
          <view class="summary-row">
            <text>总收货量：{{ record.recptTotalQty }}{{ firstUnit }}</text>
          </view>
        </view>
      </view>

      <!-- 底部提交 -->
      <view v-if="canReceive" class="footer-bar">
        <button class="cu-btn bg-green lg" :disabled="submitting" @click="submit">
          {{ submitting ? '提交中...' : '确认收货' }}
        </button>
      </view>
      <view v-else class="footer-bar readonly-tip">
        <text>当前状态（{{ statusText(record.status) }}）不可收货，需厂商发货后才能操作</text>
      </view>
    </template>
  </view>
</template>

<script setup>
import { ref, computed, getCurrentInstance } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getOutsource, receiveOutsource } from '@/api/mes/wm/outsource'

const { proxy } = getCurrentInstance()
const record = ref(null)
const loading = ref(true)
const submitting = ref(false)

const firstUnit = computed(() => (record.value?.recptLines || [])[0]?.unitName || '吨')
const canReceive = computed(() => record.value?.status === 'SHIPPED')

const STATUS_MAP = { DRAFT: '草稿', ISSUED: '已发料', VENDOR_RCVD: '厂商已收', PROCESSING: '加工中', FINISHED: '加工完成', SHIPPED: '已发货', RECEIVED: '已收货', CLOSED: '已关闭' }
function statusText(s) { return STATUS_MAP[s] || s || '' }
function statusTagType(s) {
  const m = { DRAFT: 'info', ISSUED: 'warning', VENDOR_RCVD: 'warning', PROCESSING: 'primary', FINISHED: 'primary', SHIPPED: 'primary', RECEIVED: 'success', CLOSED: 'success' }
  return m[s] || 'default'
}

async function submit() {
  if (!canReceive.value) { proxy.$modal.msgError('当前状态不可收货'); return }
  const confirmed = await new Promise(resolve => {
    uni.showModal({
      title: '确认收货',
      content: '确认将该外协物料收货入库？收货后单据将关闭，不可修改。',
      success: r => resolve(r.confirm)
    })
  })
  if (!confirmed) return
  submitting.value = true
  try {
    await receiveOutsource(record.value.orderId)
    proxy.$modal.msgSuccess('收货成功')
    setTimeout(() => proxy.$tab.navigateBack(), 1500)
  } catch (e) {} finally { submitting.value = false }
}

onLoad((options) => {
  const orderId = options.orderId
  if (!orderId) { proxy.$modal.msgError('缺少外协单ID'); return }
  getOutsource(orderId).then(res => {
    record.value = res.data
    loading.value = false
  }).catch(() => { loading.value = false })
})
</script>

<style lang="scss" scoped>
page { background-color: #f5f6f7; min-height: 100%; padding-bottom: 200rpx; }
.container { padding: 0 0 200rpx; }
.loading-box { display: flex; justify-content: center; padding: 80rpx 0; }
.info-card { background: #fff; margin: 16rpx 24rpx; border-radius: 16rpx; padding: 8rpx 24rpx; }
.info-row { display: flex; justify-content: space-between; align-items: center; padding: 20rpx 0; border-bottom: 1px solid #f5f5f5; }
.info-row:last-child { border-bottom: none; }
.label { color: #999; font-size: 26rpx; }
.value { font-size: 28rpx; color: #333; }
.bold { font-weight: 600; }
.empty-hint { padding: 40rpx 0; text-align: center; }
.text-grey { color: #999; font-size: 26rpx; }
.roll-row { display: flex; justify-content: space-between; align-items: center; padding: 20rpx 0; border-bottom: 1px solid #f5f5f5; }
.roll-main { display: flex; flex-direction: column; gap: 4rpx; }
.roll-code { font-size: 28rpx; font-weight: 600; color: #333; }
.roll-meta { font-size: 24rpx; color: #999; }
.summary-row { padding: 20rpx 0; font-size: 26rpx; color: #333; font-weight: 600; }
.footer-bar { position: fixed; left: 0; right: 0; bottom: 0; padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom)); background: #fff; border-top: 1px solid #eee; }
.cu-btn { border-radius: 12rpx; border: none; }
.cu-btn.lg { width: 100%; font-size: 30rpx; height: 88rpx; line-height: 88rpx; }
.bg-green { background: #67c23a; color: #fff; }
.cu-btn[disabled] { opacity: 0.5; }
.readonly-tip { text-align: center; color: #999; font-size: 26rpx; padding: 24rpx; }
</style>
