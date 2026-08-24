<template>
  <view class="container">
    <view v-if="loading" class="loading-box">
      <uni-load-more status="loading" />
    </view>

    <template v-else-if="record">
      <!-- 基本信息 -->
      <uni-section title="外协单信息" type="line"></uni-section>
      <view class="info-card">
        <view class="info-row">
          <text class="label">单号</text>
          <text class="value bold">{{ record.orderCode }}</text>
        </view>
        <view class="info-row">
          <text class="label">状态</text>
          <uni-tag :type="statusTagType(record.status)" :text="statusText(record.status)" size="small" />
        </view>
        <view class="info-row">
          <text class="label">来源</text>
          <text class="value">{{ sourceText(record.sourceType) }}</text>
        </view>
        <view class="info-row">
          <text class="label">外协厂商</text>
          <text class="value">{{ record.vendorName || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="label">发料总量</text>
          <text class="value bold">{{ record.issueTotalQty }}{{ record.issueTotalQty ? '吨' : '' }}</text>
        </view>
        <view class="info-row" v-if="record.recptTotalQty > 0">
          <text class="label">收货总量</text>
          <text class="value">{{ record.recptTotalQty }}吨</text>
        </view>
        <view class="info-row" v-if="record.feedbackId">
          <text class="label">报工ID</text>
          <text class="value">{{ record.feedbackId }}</text>
        </view>
        <view class="info-row link-row" v-if="record.iqcId" @click="goInspect">
          <text class="label">来料检验</text>
          <view class="link-value">
            <text class="link-text">{{ record.iqcCode || '查看检验单' }}</text>
            <text class="link-arrow">›</text>
          </view>
        </view>
      </view>

      <!-- 发料明细 -->
      <uni-section title="发料明细" type="line"></uni-section>
      <view class="info-card">
        <view v-for="line in (record.issueLines || [])" :key="line.lineId" class="detail-row">
          <view class="detail-main">
            <text class="detail-code">{{ line.itemName }}</text>
            <text class="detail-meta">{{ line.quantity }}{{ line.unitName }} · {{ line.warehouseName }}</text>
          </view>
        </view>
      </view>

      <!-- 收货明细 -->
      <template v-if="(record.recptLines || []).length > 0">
        <uni-section title="收货明细" type="line"></uni-section>
        <view class="info-card">
          <view v-for="line in record.recptLines" :key="line.lineId" class="detail-row">
            <view class="detail-main">
              <text class="detail-code">{{ line.itemName }}</text>
              <text class="detail-meta">{{ line.quantity }}{{ line.unitName }} · {{ line.warehouseName }}</text>
            </view>
          </view>
        </view>
      </template>
    </template>
  </view>
</template>

<script setup>
import { ref, getCurrentInstance } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getOutsource } from '@/api/mes/wm/outsource'

const { proxy } = getCurrentInstance()
const record = ref(null)
const loading = ref(true)

const STATUS_MAP = { ISSUED: '已发料', PROCESSING: '加工中', RECEIVED: '已收货' }
function statusText(s) { return STATUS_MAP[s] || s || '' }
function statusTagType(s) {
  const m = { ISSUED: 'warning', PROCESSING: 'primary', RECEIVED: 'success' }
  return m[s] || 'default'
}
const SOURCE_MAP = { GENERIC: '通用', SLITTING: '分切', PRINTING: '印刷' }
function sourceText(s) { return SOURCE_MAP[s] || s || '-' }

onLoad((options) => {
  const orderId = options.orderId
  if (!orderId) { proxy.$modal.msgError('缺少外协单ID'); return }
  getOutsource(orderId).then(res => {
    record.value = res.data
    loading.value = false
  }).catch(() => { loading.value = false })
})

function goInspect() {
  if (!record.value || !record.value.iqcId) return
  proxy.$tab.navigateTo('/pages/mes/qc/inspect?type=IQC&id=' + record.value.iqcId)
}
</script>

<style lang="scss" scoped>
page { background-color: #f5f6f7; min-height: 100%; }
.loading-box { display: flex; justify-content: center; padding: 80rpx 0; }
.info-card { background: #fff; margin: 16rpx 24rpx; border-radius: 16rpx; padding: 8rpx 24rpx; }
.info-row { display: flex; justify-content: space-between; align-items: center; padding: 20rpx 0; border-bottom: 1px solid #f5f5f5; }
.info-row:last-child { border-bottom: none; }
.label { color: #999; font-size: 26rpx; }
.value { font-size: 28rpx; color: #333; }
.bold { font-weight: 600; }
.link-row { cursor: pointer; }
.link-value { display: flex; align-items: center; gap: 8rpx; }
.link-text { font-size: 28rpx; color: #409eff; }
.link-arrow { font-size: 32rpx; color: #c0c4cc; }
.detail-row { display: flex; justify-content: space-between; align-items: center; padding: 20rpx 0; border-bottom: 1px solid #f5f5f5; }
.detail-row:last-child { border-bottom: none; }
.detail-main { display: flex; flex-direction: column; gap: 4rpx; }
.detail-code { font-size: 28rpx; font-weight: 600; color: #333; }
.detail-meta { font-size: 24rpx; color: #999; }
</style>
