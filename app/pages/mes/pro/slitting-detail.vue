<template>
  <view class="container">
    <view v-if="loading" class="loading-box">
      <uni-load-more status="loading" />
    </view>

    <template v-else-if="record">
      <!-- 基本信息 -->
      <uni-section title="分切单信息" type="line"></uni-section>
      <view class="info-card">
        <view class="info-row">
          <text class="label">批次号</text>
          <text class="value bold">{{ record.slitBatchNo }}</text>
        </view>
        <view class="info-row">
          <text class="label">状态</text>
          <uni-tag :type="statusTagType(record.status)" :text="statusText(record.status)" size="small" />
        </view>
        <view class="info-row">
          <text class="label">外协厂商</text>
          <text class="value">{{ record.vendorName || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="label">操作人</text>
          <text class="value">{{ record.operator || '-' }}</text>
        </view>
        <view class="info-row" v-if="record.slitTime">
          <text class="label">分切时间</text>
          <text class="value">{{ record.slitTime }}</text>
        </view>
      </view>

      <!-- 母卷信息 -->
      <uni-section title="母卷信息" type="line"></uni-section>
      <view class="info-card">
        <view class="info-row">
          <text class="label">母卷号</text>
          <text class="value bold">{{ record.parentRollCode }}</text>
        </view>
        <view class="info-row">
          <text class="label">物料</text>
          <text class="value">{{ record.parentItemName }}</text>
        </view>
        <view class="info-row">
          <text class="label">门幅</text>
          <text class="value">{{ record.parentWidth || '-' }}mm</text>
        </view>
        <view class="info-row">
          <text class="label">重量</text>
          <text class="value bold">{{ record.parentWeight }}吨</text>
        </view>
      </view>

      <!-- 子卷列表 -->
      <uni-section :title="'子卷明细（' + (childRolls.length) + '）'" type="line"></uni-section>
      <view class="info-card">
        <view v-if="childRolls.length === 0" class="empty-hint">
          <text class="text-grey">暂无子卷（厂商未录结果）</text>
        </view>
        <view v-else>
          <view v-for="roll in childRolls" :key="roll.rollId" class="roll-row">
            <view class="roll-main">
              <text class="roll-code">{{ roll.rollCode }}</text>
              <text class="roll-meta">{{ roll.actualWidth }}mm · {{ roll.actualWeightGsm || '-' }}g · {{ roll.actualWeight }}吨</text>
            </view>
            <uni-tag :type="rollStatusType(roll.status)" :text="rollStatusText(roll.status)" size="mini"></uni-tag>
          </view>
          <view class="summary-row">
            <text>子卷总重：{{ record.childTotalWeight }}吨</text>
            <text class="text-grey">损耗率：{{ record.lossRate }}%</text>
          </view>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup>
import { ref, computed, getCurrentInstance } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getSlitting } from '@/api/mes/pro/slitting'

const { proxy } = getCurrentInstance()

const record = ref(null)
const loading = ref(true)

const childRolls = computed(() => record.value?.childRolls || [])

const STATUS_MAP = {
  ISSUED: '已发料', SLITTING: '分切中', RECEIVED: '已收货', EXECUTED: '已执行'
}
function statusText(s) { return STATUS_MAP[s] || s || '' }
function statusTagType(s) {
  const m = { ISSUED: 'warning', SLITTING: 'primary', RECEIVED: 'success', EXECUTED: 'success' }
  return m[s] || 'default'
}
const ROLL_STATUS_MAP = { IN_STOCK: '在库', OUTSOURCED: '外协中', CONSUMED: '已消耗' }
function rollStatusText(s) { return ROLL_STATUS_MAP[s] || s || '' }
function rollStatusType(s) {
  const m = { IN_STOCK: 'success', OUTSOURCED: 'warning', CONSUMED: 'error' }
  return m[s] || 'default'
}

onLoad((options) => {
  const slitId = options.slitId
  if (!slitId) { proxy.$modal.msgError('缺少分切单ID'); return }
  getSlitting(slitId).then(res => {
    record.value = res.data
    loading.value = false
  }).catch(() => { loading.value = false })
})
</script>

<style lang="scss" scoped>
page { background-color: #f5f6f7; min-height: 100%; }

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
  display: flex; justify-content: space-between;
  padding: 20rpx 0; font-size: 26rpx; color: #333; font-weight: 600;
}
</style>
