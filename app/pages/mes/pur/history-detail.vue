<template>
  <view class="container">
    <view v-if="loading" class="loading-box">
      <uni-load-more status="loading" />
    </view>

    <template v-else>
      <!-- 头部信息卡片 -->
      <view class="card">
        <view class="card-header">
          <text class="recpt-code">{{ header.recptCode || '-' }}</text>
          <uni-tag :type="tagType(header.status)" :text="statusMap[header.status] || header.status" size="small" />
        </view>
        <view class="card-body">
          <view class="info-row">
            <text class="label">PO单号</text>
            <text class="value">{{ header.purOrderCode || '-' }}</text>
          </view>
          <view class="info-row" v-if="header.workorderCode">
            <text class="label">工单号</text>
            <text class="value">{{ header.workorderCode }}</text>
          </view>
          <view class="info-row">
            <text class="label">供应商</text>
            <text class="value">{{ header.vendorName || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="label">入库仓库</text>
            <text class="value">{{ header.warehouseName || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="label">入库日期</text>
            <text class="value">{{ header.recptDate || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="label">入库数量</text>
            <text class="value bold">{{ header.totalQuantity || 0 }}</text>
          </view>
          <view class="info-row">
            <text class="label">入库类型</text>
            <text class="value">{{ recptTypeMap[header.recptType] || header.recptType || '-' }}</text>
          </view>
          <view class="info-row" v-if="header.remark">
            <text class="label">备注</text>
            <text class="value remark-text">{{ header.remark }}</text>
          </view>
        </view>
      </view>

      <!-- 行项目列表 -->
      <view class="card">
        <view class="card-title">入库明细</view>
        <view v-if="lines.length === 0" class="empty-line">
          <text class="text-grey">暂无明细数据</text>
        </view>
        <view v-for="(line, idx) in lines" :key="line.lineId" class="line-item">
          <view class="line-header">
            <text class="line-num">#{{ idx + 1 }}</text>
            <text class="line-code">{{ line.itemCode }}</text>
          </view>
          <text class="line-name">{{ line.itemName || '-' }}</text>
          <view class="line-row" v-if="line.specification">
            <text class="label">规格</text>
            <text class="value">{{ line.specification }}</text>
          </view>
          <view class="line-row">
            <text class="label">数量</text>
            <text class="value bold">{{ line.quantityRecpt || 0 }} {{ line.unitName || '' }}</text>
          </view>
          <view class="line-row" v-if="line.warehouseName">
            <text class="label">入库仓库</text>
            <text class="value">{{ line.warehouseName }}</text>
          </view>
          <view class="line-row" v-if="line.batchCode">
            <text class="label">系统批号</text>
            <text class="value bold">{{ line.batchCode }}</text>
          </view>
          <view class="line-row" v-if="line.lotNumber">
            <text class="label">生产批号</text>
            <text class="value">{{ line.lotNumber }}</text>
          </view>
          <view class="line-row" v-if="line.produceDate">
            <text class="label">生产日期</text>
            <text class="value">{{ line.produceDate }}</text>
          </view>
          <view class="line-row" v-if="line.expireDate">
            <text class="label">有效期至</text>
            <text class="value">{{ line.expireDate }}</text>
          </view>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup>
import { ref, getCurrentInstance } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getItemRecpt, listItemRecptLines } from '@/api/mes/pur/order'

const header = ref({})
const lines = ref([])
const loading = ref(true)

const statusMap = { DRAFT: '草稿', CONFIRMED: '已确认', POSTED: '已过账', CANCEL: '已取消' }
const recptTypeMap = { PURCHASE: '采购入库', RETURN: '退货入库', MISC: '杂项入库' }

function tagType(status) {
  const map = { DRAFT: 'default', CONFIRMED: 'primary', POSTED: 'success', CANCEL: 'danger' }
  return map[status] || 'default'
}

onLoad((options) => {
  const recptId = options.recptId
  if (!recptId) {
    loading.value = false
    return
  }
  Promise.all([
    getItemRecpt(recptId),
    listItemRecptLines({ recptId: Number(recptId) })
  ]).then(([res1, res2]) => {
    header.value = res1.data || {}
    lines.value = res2.rows || []
  }).catch(() => {
    // ignore
  }).finally(() => {
    loading.value = false
  })
})
</script>

<style lang="scss" scoped>
page { background-color: #f5f6f7; min-height: 100%; }

.loading-box { display: flex; justify-content: center; padding: 160rpx 0; }

.card {
  background: #fff; border-radius: 16rpx;
  padding: 24rpx; margin: 20rpx 24rpx;
}

.card-header {
  display: flex; justify-content: space-between; align-items: center;
  padding-bottom: 16rpx; border-bottom: 1px solid #f5f5f5;
}
.recpt-code { font-size: 32rpx; font-weight: 600; color: #333; }

.card-body { padding: 16rpx 0 0; }

.info-row {
  display: flex; justify-content: space-between;
  padding: 8rpx 0;
}
.label { color: #999; font-size: 26rpx; }
.value { font-size: 26rpx; color: #333; }
.bold { font-weight: 600; }

.card-title {
  font-size: 28rpx; font-weight: 600; color: #333;
  padding-bottom: 16rpx; border-bottom: 1px solid #f5f5f5;
}

.empty-line { text-align: center; padding: 40rpx 0; }

.line-item {
  padding: 20rpx 0;
  border-bottom: 1px solid #f5f5f5;
  &:last-child { border-bottom: none; }
}
.line-header { display: flex; align-items: center; gap: 12rpx; margin-bottom: 4rpx; }
.line-num { color: #999; font-size: 24rpx; }
.line-code { font-size: 26rpx; font-weight: 600; color: #333; }
.line-name { font-size: 26rpx; color: #666; display: block; margin-bottom: 8rpx; }
.line-row { display: flex; justify-content: space-between; padding: 4rpx 0; }

.text-grey { color: #999; font-size: 24rpx; }
.remark-text { font-size: 22rpx; color: #999; text-align: right; max-width: 400rpx; }
</style>
