<template>
  <view class="container">
    <view class="tip-card">
      <text class="tip-text">扫描物料条码，查询该物料的实时库存</text>
    </view>

    <!-- 扫码区 -->
    <view class="scan-bar">
      <view class="scan-input" @click="scanCode">
        <uni-icons type="scan" size="28" color="#409eff" />
        <text class="scan-tip">{{ itemCode || '点击扫码查询库存' }}</text>
      </view>
      <button class="btn-scan" @click="scanCode">扫码</button>
    </view>

    <!-- 手动输入 -->
    <view class="manual-row">
      <uni-easyinput v-model="itemCode" placeholder="或手动输入物料编码" clearable />
      <button class="btn-search" @click="queryStock">查询</button>
    </view>

    <!-- 物料信息 -->
    <view v-if="itemInfo" class="info-card">
      <view class="card-top">
        <text class="code">{{ itemInfo.itemCode }}</text>
        <text class="unit">{{ itemInfo.unitOfMeasure || '' }}</text>
      </view>
      <text class="name">{{ itemInfo.itemName }}</text>
      <view v-if="itemInfo.specification" class="sub">规格：{{ itemInfo.specification }}</view>
    </view>

    <!-- 库存列表 -->
    <view v-if="stockList.length > 0" class="section-title">库存明细（{{ stockList.length }} 条）</view>
    <view v-for="(s, idx) in stockList" :key="idx" class="stock-card">
      <view class="stock-row"><text class="label">仓库</text><text class="value">{{ s.warehouseCode || '-' }}</text></view>
      <view class="stock-row"><text class="label">批次</text><text class="value">{{ s.batchCode || '无批次' }}</text></view>
      <view class="stock-row">
        <text class="label">现有量</text>
        <text class="qty onhand">{{ s.quantityOnhand || 0 }}</text>
      </view>
      <view class="stock-row">
        <text class="label">可用量</text>
        <text class="qty avail">{{ s.quantityAvailable || 0 }}</text>
        <text class="qty-tip">（已扣除预占）</text>
      </view>
    </view>
    <view v-if="queried && stockList.length === 0" class="empty-tip">该物料暂无库存记录</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { getItemStock } from '@/api/mes/wm/issue'

const { proxy } = getCurrentInstance()
const itemCode = ref('')
const itemInfo = ref(null)
const stockList = ref([])
const queried = ref(false)

function scanCode() {
  uni.scanCode({
    onlyFromCamera: false,
    scanType: ['barCode', 'qrCode'],
    success: (res) => {
      itemCode.value = res.result
      queryStock()
    },
    fail: () => { proxy.$modal.msgError('扫码取消或失败') }
  })
}

async function queryStock() {
  if (!itemCode.value) { proxy.$modal.msgError('请输入或扫描物料编码'); return }
  queried.value = true
  try {
    const res = await getItemStock(itemCode.value)
    const rows = res.rows || []
    stockList.value = rows
    // 取第一条作为物料信息展示
    if (rows.length > 0) {
      itemInfo.value = {
        itemCode: rows[0].itemCode,
        itemName: rows[0].itemName,
        specification: rows[0].specification,
        unitOfMeasure: rows[0].unitOfMeasure
      }
    } else {
      itemInfo.value = null
    }
  } catch (e) {}
}
</script>

<style lang="scss" scoped>
.container { min-height: 100vh; background: #f5f6f7; padding-bottom: 40rpx; }
.tip-card {
  background: #ecf5ff; margin: 16rpx; padding: 20rpx 24rpx; border-radius: 12rpx;
  .tip-text { font-size: 26rpx; color: #409eff; }
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
.manual-row {
  display: flex; gap: 16rpx; padding: 0 16rpx; margin-bottom: 16rpx; align-items: center;
  .btn-search {
    background: #67c23a; color: #fff; border-radius: 8rpx; font-size: 28rpx; padding: 0 32rpx; border: none;
    &::after { border: none; }
  }
}
.info-card {
  background: #fff; border-radius: 12rpx; padding: 24rpx; margin: 0 16rpx 16rpx;
  .card-top { display: flex; justify-content: space-between; margin-bottom: 8rpx;
    .code { font-size: 32rpx; font-weight: bold; color: #303133; }
    .unit { font-size: 24rpx; color: #909399; }
  }
  .name { font-size: 28rpx; color: #606266; display: block; }
  .sub { font-size: 24rpx; color: #909399; display: block; margin-top: 8rpx; }
}
.section-title { font-size: 28rpx; font-weight: bold; color: #303133; padding: 16rpx 24rpx; }
.stock-card {
  background: #fff; border-radius: 12rpx; padding: 24rpx; margin: 0 16rpx 16rpx;
  .stock-row { display: flex; align-items: center; font-size: 28rpx; margin-bottom: 12rpx;
    .label { color: #909399; width: 140rpx; flex-shrink: 0; }
    .value { color: #303133; flex: 1; }
    .qty { font-size: 32rpx; font-weight: bold; }
    .qty.onhand { color: #409eff; }
    .qty.avail { color: #67c23a; }
    .qty-tip { font-size: 22rpx; color: #c0c4cc; margin-left: 8rpx; }
  }
}
.empty-tip { text-align: center; color: #909399; font-size: 26rpx; padding: 60rpx 0; }
</style>
