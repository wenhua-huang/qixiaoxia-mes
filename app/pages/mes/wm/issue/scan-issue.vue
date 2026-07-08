<template>
  <view class="container">
    <!-- 顶部领料单信息 -->
    <view class="header-card">
      <view class="header-row">
        <text class="code">{{ header.issueCode || '' }}</text>
        <text class="status" :class="'st-' + header.status">{{ statusText(header.status) }}</text>
      </view>
      <text class="sub">扫码物料条码，自动匹配领料行并发料</text>
    </view>

    <!-- 扫码区 -->
    <view class="scan-bar">
      <view class="scan-input" @click="scanCode">
        <uni-icons type="scan" size="28" color="#409eff" />
        <text class="scan-tip">{{ lastScanCode || '点击扫码（物料条码）' }}</text>
      </view>
      <button class="btn-scan" @click="scanCode">扫码</button>
    </view>

    <!-- 本次发料清单 -->
    <view class="section-title">
      <text>本次发料清单（{{ issueList.length }} 项）</text>
      <text v-if="issueList.length" class="clear-btn" @click="issueList = []">清空</text>
    </view>

    <view v-for="(d, idx) in issueList" :key="idx" class="issue-card">
      <view class="card-top">
        <text class="item-code">{{ d.itemCode }}</text>
        <text class="del-btn" @click="issueList.splice(idx, 1)">删除</text>
      </view>
      <text class="item-name">{{ d.itemName }}</text>
      <view class="card-mid">
        <text class="line-info">领料行：{{ d.quantityIssue }} {{ d.unitName }}（未发 {{ d.remain }}）</text>
      </view>
      <view class="qty-row">
        <text class="qty-label">本次发料：</text>
        <uni-number-box v-model="d.quantity" :min="0" :max="d.remain" :step="1" />
        <text class="qty-unit">{{ d.unitName }}</text>
      </view>
      <view class="batch-row">
        <text class="batch-label">批次：</text>
        <uni-easyinput v-model="d.batchCode" placeholder="可选，输入批次编码" clearable />
      </view>
    </view>
    <view v-if="issueList.length === 0" class="empty-tip">请扫码添加物料</view>

    <!-- 底部提交 -->
    <view class="action-bar">
      <button class="btn-submit" :disabled="!canSubmit" @click="handleSubmit">提交发料</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, getCurrentInstance } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getIssueDetail, issueOut } from '@/api/mes/wm/issue'

const { proxy } = getCurrentInstance()
const issueId = ref(null)
const header = ref({})
const lines = ref([])
const issueList = ref([])  // 本次发料清单
const lastScanCode = ref('')

const STATUS_MAP = {
  DRAFT: '草稿', PENDING: '待审核', APPROVED: '已下达', ALLOCATED: '已预占',
  PARTIAL_ISSUED: '部分发料', ISSUED: '已发料', CLOSED: '已关闭', CANCELED: '已作废'
}
function statusText(s) { return STATUS_MAP[s] || s || '' }

const canSubmit = computed(() => issueList.value.some(d => d.quantity > 0))

async function loadData() {
  try {
    const res = await getIssueDetail(issueId.value)
    header.value = res.data?.header || res.data || {}
    lines.value = res.data?.lines || []
  } catch (e) {}
}

// 扫码
function scanCode() {
  uni.scanCode({
    onlyFromCamera: false,
    scanType: ['barCode', 'qrCode'],
    success: (res) => {
      const code = res.result
      lastScanCode.value = code
      matchItem(code)
    },
    fail: () => { proxy.$modal.msgError('扫码取消或失败') }
  })
}

// 扫码后匹配领料行（按物料编码匹配）
function matchItem(code) {
  // 尝试精确匹配 itemCode
  let matched = lines.value.find(l => l.itemCode === code)
  // 兜底：按包含匹配（条码可能含物料编码前缀）
  if (!matched) {
    matched = lines.value.find(l => code.includes(l.itemCode) || l.itemCode.includes(code))
  }
  if (!matched) {
    proxy.$modal.msgError('未匹配到领料行：' + code)
    return
  }
  const remain = (matched.quantityIssue || 0) - (matched.quantityIssued || 0)
  if (remain <= 0) {
    proxy.$modal.msgError('该物料已发料完成：' + matched.itemCode)
    return
  }
  // 已在清单中则提示
  const exist = issueList.value.find(d => d.lineId === matched.lineId)
  if (exist) {
    proxy.$modal.msgSuccess('已在清单中：' + matched.itemCode)
    return
  }
  issueList.value.push({
    lineId: matched.lineId,
    itemId: matched.itemId,
    itemCode: matched.itemCode,
    itemName: matched.itemName,
    unitOfMeasure: matched.unitOfMeasure,
    unitName: matched.unitName,
    quantityIssue: matched.quantityIssue,
    remain: remain,
    quantity: remain,  // 默认发料全部未发量
    batchCode: ''
  })
  proxy.$modal.msgSuccess('已添加：' + matched.itemName)
}

async function handleSubmit() {
  const valid = issueList.value.filter(d => d.quantity > 0)
  if (!valid.length) { proxy.$modal.msgError('请输入有效的发料数量'); return }
  // 校验不超过未发料量
  for (const d of valid) {
    if (d.quantity > d.remain) {
      proxy.$modal.msgError(`${d.itemCode} 发料量(${d.quantity})超过未发料量(${d.remain})`)
      return
    }
  }
  uni.showModal({
    title: '确认发料', content: `共 ${valid.length} 项物料，确认发料出库？`,
    success: async (r) => {
      if (!r.confirm) return
      try {
        // 组装 details（与后端 WmIssueDetail 字段对齐）
        const details = valid.map(d => ({
          lineId: d.lineId, itemId: d.itemId, itemCode: d.itemCode, itemName: d.itemName,
          unitOfMeasure: d.unitOfMeasure, unitName: d.unitName,
          quantity: d.quantity, batchCode: d.batchCode || null,
          batchId: null, warehouseId: header.value.warehouseId
        }))
        await issueOut(issueId.value, details)
        proxy.$modal.msgSuccess('发料成功')
        setTimeout(() => uni.navigateBack(), 1000)
      } catch (e) {}
    }
  })
}

onLoad((options) => {
  issueId.value = options.issueId
  loadData()
})
</script>

<style lang="scss" scoped>
.container { min-height: 100vh; background: #f5f6f7; padding-bottom: 140rpx; }
.header-card {
  background: #fff; padding: 24rpx; margin: 16rpx; border-radius: 12rpx;
  .header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8rpx;
    .code { font-size: 32rpx; font-weight: bold; color: #303133; }
    .status { font-size: 24rpx; padding: 4rpx 16rpx; border-radius: 20rpx; }
  }
  .sub { font-size: 24rpx; color: #909399; }
}
.scan-bar {
  display: flex; gap: 16rpx; padding: 0 16rpx; margin-bottom: 16rpx;
  .scan-input {
    flex: 1; background: #fff; border-radius: 8rpx; padding: 20rpx;
    display: flex; align-items: center; gap: 12rpx;
    .scan-tip { font-size: 28rpx; color: #909399; margin-left: 8rpx; }
  }
  .btn-scan {
    background: #409eff; color: #fff; border-radius: 8rpx; font-size: 28rpx; padding: 0 32rpx; border: none;
    &::after { border: none; }
  }
}
.section-title {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16rpx 24rpx; font-size: 28rpx; font-weight: bold; color: #303133;
  .clear-btn { font-size: 24rpx; color: #f56c6c; font-weight: normal; }
}
.issue-card {
  background: #fff; border-radius: 12rpx; padding: 24rpx; margin: 0 16rpx 16rpx;
  .card-top { display: flex; justify-content: space-between; margin-bottom: 8rpx;
    .item-code { font-size: 28rpx; font-weight: bold; color: #303133; }
    .del-btn { font-size: 24rpx; color: #f56c6c; }
  }
  .item-name { font-size: 26rpx; color: #606266; display: block; margin-bottom: 8rpx; }
  .card-mid { margin-bottom: 12rpx;
    .line-info { font-size: 24rpx; color: #909399; }
  }
  .qty-row, .batch-row { display: flex; align-items: center; gap: 12rpx; margin-top: 8rpx;
    .qty-label, .batch-label { font-size: 26rpx; color: #606266; width: 140rpx; flex-shrink: 0; }
    .qty-unit { font-size: 24rpx; color: #909399; }
  }
}
.st-DRAFT, .st-PENDING { background: #fdf6ec; color: #e6a23c; }
.st-ALLOCATED, .st-PARTIAL_ISSUED { background: #f0f9eb; color: #67c23a; }
.st-ISSUED, .st-CLOSED { background: #f4f4f5; color: #909399; }
.st-CANCELED { background: #fef0f0; color: #f56c6c; }
.empty-tip { text-align: center; color: #909399; font-size: 26rpx; padding: 60rpx 0; }
.action-bar {
  position: fixed; bottom: 0; left: 0; right: 0; background: #fff; padding: 20rpx 32rpx;
  border-top: 1rpx solid #eee;
  .btn-submit {
    width: 100%; background: #409eff; color: #fff; border-radius: 8rpx; font-size: 32rpx; border: none;
    &[disabled] { background: #c0c4cc; }
    &::after { border: none; }
  }
}
</style>
