<template>
  <view class="container">
    <view v-if="header" class="detail-wrap">
      <!-- 头部信息 -->
      <view class="info-card">
        <view class="card-header">
          <text class="code">{{ header.issueCode }}</text>
          <text class="status" :class="'st-' + header.status">{{ statusText(header.status) }}</text>
        </view>
        <view class="info-row"><text class="label">名称</text><text class="value">{{ header.issueName || '-' }}</text></view>
        <view class="info-row"><text class="label">工单</text><text class="value">{{ header.workorderName || '-' }}</text></view>
        <view class="info-row"><text class="label">仓库</text><text class="value">{{ header.warehouseName || '-' }}</text></view>
        <view class="info-row"><text class="label">日期</text><text class="value">{{ formatDate(header.issueDate) }}</text></view>
        <view class="info-row"><text class="label">申请/已发</text><text class="value">{{ header.quantityTotal || 0 }} / {{ header.quantityIssuedTotal || 0 }}</text></view>
      </view>

      <!-- 领料明细行 -->
      <view class="section-title">领料明细</view>
      <view v-for="line in lines" :key="line.lineId" class="line-card">
        <view class="line-header">
          <text class="item-code">{{ line.itemCode }}</text>
          <text class="item-unit">{{ line.unitName }}</text>
        </view>
        <text class="item-name">{{ line.itemName }}</text>
        <view v-if="line.itemSpc" class="line-sub">规格：{{ line.itemSpc }}</view>
        <view class="line-qty">
          <text>申请：{{ line.quantityIssue || 0 }}</text>
          <text>已发：{{ line.quantityIssued || 0 }}</text>
          <text class="remain">未发：{{ remain(line) }}</text>
        </view>
      </view>
      <view v-if="lines.length === 0" class="empty-tip">暂无明细行</view>

      <!-- 操作按钮 -->
      <view class="action-bar">
        <button v-if="canIssue" class="btn-primary" @click="goScanIssue">扫码发料</button>
        <button v-if="canConfirm" class="btn-primary" @click="handleConfirm">预占库存</button>
        <button v-if="canClose" class="btn-primary" @click="handleClose">关闭领料单</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, getCurrentInstance } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getIssueDetail, confirmIssue, closeIssue } from '@/api/mes/wm/issue'

const { proxy } = getCurrentInstance()
const issueId = ref(null)
const header = ref(null)
const lines = ref([])

const STATUS_MAP = {
  DRAFT: '草稿', PENDING: '待审核', APPROVED: '已下达', ALLOCATED: '已预占',
  PARTIAL_ISSUED: '部分发料', ISSUED: '已发料', CLOSED: '已关闭', CANCELED: '已作废'
}
function statusText(s) { return STATUS_MAP[s] || s }
function formatDate(d) { return d ? d.substring(0, 16) : '-' }
function remain(line) {
  const issue = line.quantityIssue || 0
  const issued = line.quantityIssued || 0
  return Math.max(0, issue - issued)
}

// 按钮显隐：根据状态
const canIssue = computed(() => ['ALLOCATED', 'PARTIAL_ISSUED'].includes(header.value?.status))
const canConfirm = computed(() => header.value?.status === 'APPROVED')
const canClose = computed(() => ['ISSUED', 'PARTIAL_ISSUED'].includes(header.value?.status))

async function loadDetail() {
  try {
    const res = await getIssueDetail(issueId.value)
    header.value = res.data?.header || res.data
    lines.value = res.data?.lines || []
  } catch (e) {}
}

function goScanIssue() {
  proxy.$tab.navigateTo('/pages/mes/wm/issue/scan-issue?issueId=' + issueId.value)
}

function handleConfirm() {
  uni.showModal({
    title: '确认', content: '预占库存？将扣减可用库存。',
    success: async (r) => {
      if (!r.confirm) return
      try {
        await confirmIssue(issueId.value)
        proxy.$modal.msgSuccess('预占成功')
        loadDetail()
      } catch (e) {}
    }
  })
}

function handleClose() {
  uni.showModal({
    title: '确认', content: '关闭领料单？关闭后不可再操作。',
    success: async (r) => {
      if (!r.confirm) return
      try {
        await closeIssue(issueId.value)
        proxy.$modal.msgSuccess('已关闭')
        loadDetail()
      } catch (e) {}
    }
  })
}

onLoad((options) => {
  issueId.value = options.issueId
  loadDetail()
})
</script>

<style lang="scss" scoped>
.container { min-height: 100vh; background: #f5f6f7; padding-bottom: 140rpx; }
.detail-wrap { padding: 16rpx; }
.info-card, .line-card {
  background: #fff; border-radius: 12rpx; padding: 24rpx; margin-bottom: 16rpx;
}
.card-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx;
  .code { font-size: 32rpx; font-weight: bold; color: #303133; }
  .status { font-size: 24rpx; padding: 4rpx 16rpx; border-radius: 20rpx; }
}
.info-row { display: flex; font-size: 28rpx; margin-bottom: 12rpx;
  .label { color: #909399; width: 160rpx; flex-shrink: 0; }
  .value { color: #303133; flex: 1; }
}
.section-title { font-size: 30rpx; font-weight: bold; color: #303133; margin: 16rpx 8rpx; }
.line-card {
  .line-header { display: flex; justify-content: space-between; margin-bottom: 8rpx;
    .item-code { font-size: 28rpx; font-weight: bold; color: #303133; }
    .item-unit { font-size: 24rpx; color: #909399; }
  }
  .item-name { font-size: 26rpx; color: #606266; display: block; margin-bottom: 8rpx; }
  .line-sub { font-size: 24rpx; color: #909399; margin-bottom: 8rpx; }
  .line-qty { display: flex; justify-content: space-between; font-size: 26rpx; color: #606266;
    .remain { color: #e6a23c; font-weight: bold; }
  }
}
.st-DRAFT, .st-PENDING { background: #fdf6ec; color: #e6a23c; }
.st-APPROVED { background: #ecf5ff; color: #409eff; }
.st-ALLOCATED, .st-PARTIAL_ISSUED { background: #f0f9eb; color: #67c23a; }
.st-ISSUED, .st-CLOSED { background: #f4f4f5; color: #909399; }
.st-CANCELED { background: #fef0f0; color: #f56c6c; }
.empty-tip { text-align: center; color: #909399; font-size: 26rpx; padding: 40rpx 0; }
.action-bar {
  position: fixed; bottom: 0; left: 0; right: 0; background: #fff; padding: 20rpx 32rpx;
  display: flex; gap: 20rpx; border-top: 1rpx solid #eee;
  .btn-primary {
    flex: 1; background: #409eff; color: #fff; border-radius: 8rpx; font-size: 30rpx; border: none;
    &::after { border: none; }
  }
}
</style>
