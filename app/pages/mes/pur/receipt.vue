<template>
  <view class="container">
    <!-- 步骤 1：扫码/输入PO单号 -->
    <view class="section">
      <uni-section title="采购订单" type="line">
        <template #right>
          <text class="scan-tip">扫码或输入单号</text>
        </template>
      </uni-section>
      <view class="search-row">
        <uni-easyinput
          v-model="orderCode"
          placeholder="输入PO单号或扫码"
          :inputBorder="false"
          class="search-input"
          @confirm="searchOrder"
        />
        <button class="scan-btn" @click="handleScan" size="mini">
          <uni-icons type="scan" size="20"></uni-icons>
        </button>
        <button class="search-btn cu-btn bg-blue sm" @click="searchOrder">查询</button>
      </view>
    </view>

    <!-- 步骤 2：PO详情展示 -->
    <view v-if="order" class="section">
      <uni-section title="订单详情" type="line"></uni-section>
      <view class="order-info">
        <view class="info-row">
          <text class="label">订单编码</text>
          <text class="value bold">{{ order.orderCode }}</text>
        </view>
        <view class="info-row">
          <text class="label">供应商</text>
          <text class="value">{{ order.vendorName }}</text>
        </view>
        <view class="info-row">
          <text class="label">采购类型</text>
          <text class="value">{{ purchaseTypeText(order.purchaseType) }}</text>
        </view>
        <view class="info-row">
          <text class="label">状态</text>
          <uni-tag :type="statusTagType(order.status)" :text="statusText(order.status)" size="small" />
        </view>
      </view>

      <!-- 物料行 — 填入实收数量 -->
      <view class="line-header">
        <text class="bold">物料明细</text>
        <text class="text-grey">填写实收数量</text>
      </view>
      <view v-for="(line, idx) in lines" :key="idx" class="line-item">
        <view class="line-info">
          <text class="bold">{{ line.itemName }}</text>
          <text class="text-grey">{{ line.itemCode }} / {{ line.specification || '-' }}</text>
        </view>
        <view class="line-qty">
          <text class="label">订购: {{ line.quantityOrdered }} {{ line.unitName }}</text>
          <view class="qty-input">
            <text class="label">实收:</text>
            <uni-easyinput
              v-model="line.receiptQty"
              type="number"
              placeholder="0"
              :inputBorder="true"
              class="qty-field"
            />
            <text class="unit">{{ line.unitName }}</text>
          </view>
          <text v-if="line.quantityReceived" class="text-green">
            已收: {{ line.quantityReceived }}
          </text>
        </view>
      </view>
    </view>

    <!-- 步骤 3：拍照留证 -->
    <view v-if="order" class="section">
      <uni-section title="拍照留证" type="line">
        <template #right>
          <text class="text-grey">最多3张</text>
        </template>
      </uni-section>
      <view class="photo-row">
        <view v-for="(photo, idx) in photos" :key="idx" class="photo-item">
          <image :src="photo" mode="aspectFill" class="photo-img" @click="previewPhoto(idx)" />
          <uni-icons type="closeempty" size="18" class="photo-del" @click="removePhoto(idx)" />
        </view>
        <view v-if="photos.length < 3" class="photo-add" @click="takePhoto">
          <uni-icons type="camera-filled" size="28" color="#999" />
          <text class="text-grey">拍照</text>
        </view>
      </view>
    </view>

    <!-- 步骤 4：到货信息 -->
    <view v-if="order" class="section">
      <uni-section title="到货信息" type="line"></uni-section>
      <view class="form-box">
        <uni-forms :model="arrivalInfo" labelWidth="80px">
          <uni-forms-item label="物流单号" name="logisticsNo">
            <uni-easyinput v-model="arrivalInfo.logisticsNo" placeholder="选填" />
          </uni-forms-item>
          <uni-forms-item label="车牌号" name="vehiclePlate">
            <uni-easyinput v-model="arrivalInfo.vehiclePlate" placeholder="选填" />
          </uni-forms-item>
          <uni-forms-item label="送货单号" name="vendorDeliveryNo">
            <uni-easyinput v-model="arrivalInfo.vendorDeliveryNo" placeholder="选填" />
          </uni-forms-item>
        </uni-forms>
      </view>
    </view>

    <!-- 底部确认按钮 -->
    <view v-if="order" class="footer-bar">
      <button type="primary" class="confirm-btn" @click="submitReceipt" :disabled="submitting">
        {{ submitting ? '提交中...' : '确认收货' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, getCurrentInstance } from 'vue'
import { listOrder, listOrderLine, receiveItemRecpt } from '@/api/mes/pur/order'
import { isValidReceiptQty, genRecptCode } from '@/utils/pur.js'

const { proxy } = getCurrentInstance()
const orderCode = ref('')
const order = ref(null)
const lines = ref([])
const photos = ref([])
const submitting = ref(false)
const warehouseId = ref(1) // 默认仓库，后续可从用户配置获取
const arrivalInfo = reactive({
  logisticsNo: '',
  vehiclePlate: '',
  vendorDeliveryNo: ''
})

// 订单类型文本
const purchaseTypeMap = { PAPER: '纸张', AUX: '辅料', PACK: '包材', OTHER: '其他' }
function purchaseTypeText(type) { return purchaseTypeMap[type] || type }

// 状态标签
function statusTagType(status) {
  const map = { DRAFT: 'default', APPROVED: 'primary', ORDERED: 'warning', RECEIVING: 'warning', RECEIVED: 'success', CLOSED: 'info', CANCEL: 'danger' }
  return map[status] || 'default'
}
function statusText(status) {
  const map = { DRAFT: '草稿', APPROVED: '已审批', ORDERED: '已下单', RECEIVING: '收货中', RECEIVED: '已收货', CLOSED: '已关闭', CANCEL: '已取消' }
  return map[status] || status
}

// 扫码
function handleScan() {
  // #ifdef APP-PLUS || H5
  uni.scanCode({
    onlyFromCamera: false,
    scanType: ['barCode', 'qrCode'],
    success: (res) => {
      orderCode.value = res.result
      searchOrder()
    },
    fail: (err) => {
      console.log('扫码取消:', err)
    }
  })
  // #endif
  // #ifdef MP-WEIXIN
  uni.scanCode({
    success: (res) => {
      orderCode.value = res.result
      searchOrder()
    }
  })
  // #endif
}

// 搜索PO（通过列表API按编码搜索）
function searchOrder() {
  if (!orderCode.value.trim()) {
    proxy.$modal.msgError('请输入PO单号')
    return
  }
  proxy.$modal.loading('查询中...')
  // 使用列表API按订单编码搜索（getOrder 需要数字ID）
  listOrder({ orderCode: orderCode.value.trim() }).then(res => {
    proxy.$modal.closeLoading()
    const rows = res.rows || []
    if (rows.length === 0) {
      proxy.$modal.msgError('未找到该采购订单')
      return
    }
    const found = rows[0]
    if (found.status !== 'ORDERED' && found.status !== 'RECEIVING') {
      proxy.$modal.msgError('该订单状态为"' + statusText(found.status) + '"，仅已下单/收货中的订单可收货')
      return
    }
    // 存储原始数据，模板中用 purchaseTypeText() 显示中文
    order.value = found
    loadLines(order.value.orderId)
  }).catch(() => {
    proxy.$modal.closeLoading()
    proxy.$modal.msgError('查询失败，请检查单号')
  })
}

// 加载物料行
function loadLines(orderId) {
  listOrderLine({ orderId }).then(res => {
    lines.value = (res.rows || []).map(l => ({
      ...l,
      receiptQty: '',
      quantityReceived: l.quantityReceived || 0
    }))
  })
}

// 拍照
function takePhoto() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['camera'],
    success: (res) => {
      photos.value.push(res.tempFilePaths[0])
    }
  })
}

// 查看照片
function previewPhoto(idx) {
  uni.previewImage({
    current: idx,
    urls: photos.value
  })
}

// 删除照片
function removePhoto(idx) {
  photos.value.splice(idx, 1)
}

// 提交收货 — 单接口完成（头+行+确认，后端事务保证原子性）
function submitReceipt() {
  const hasQty = lines.value.some(l => isValidReceiptQty(l.receiptQty))
  if (!hasQty) {
    proxy.$modal.msgError('请至少填写一行的实收数量')
    return
  }

  proxy.$modal.confirm('确认提交收货？确认后将更新库存。').then(() => {
    submitting.value = true
    const body = {
      header: {
        recptCode: genRecptCode(),
        recptName: '移动端收货-' + (order.value.orderCode || ''),
        purOrderId: order.value.orderId,
        purOrderCode: order.value.orderCode,
        vendorId: order.value.vendorId,
        vendorCode: order.value.vendorCode,
        vendorName: order.value.vendorName,
        warehouseId: warehouseId.value,
        recptType: 'PURCHASE',
        status: 'DRAFT',
        remark: [
          arrivalInfo.logisticsNo && '物流:' + arrivalInfo.logisticsNo,
          arrivalInfo.vehiclePlate && '车牌:' + arrivalInfo.vehiclePlate,
          arrivalInfo.vendorDeliveryNo && '送货单:' + arrivalInfo.vendorDeliveryNo
        ].filter(Boolean).join('; ')
      },
      lines: lines.value.filter(l => isValidReceiptQty(l.receiptQty)).map(l => ({
        itemId: l.itemId, itemCode: l.itemCode, itemName: l.itemName,
        specification: l.specification,
        unitOfMeasure: l.unitOfMeasure, unitName: l.unitName,
        quantityRecpt: parseFloat(l.receiptQty)
      }))
    }

    // 单接口调用，后端原子完成：创建头 → 创建行 → 确认收货 → 回写PO
    receiveItemRecpt(body).then(() => {
      proxy.$modal.msgSuccess('收货确认成功！库存已更新')
      setTimeout(() => { proxy.$tab.navigateBack() }, 1500)
    }).catch(e => {
      proxy.$modal.msgError('收货失败：' + (e.msg || '未知错误'))
    }).finally(() => {
      submitting.value = false
    })
  }).catch(() => {})
}
</script>

<style lang="scss" scoped>
page { background-color: #f5f6f7; min-height: 100%; }

.container { padding-bottom: 120rpx; }

.section {
  margin: 20rpx 0;
  background: #fff;
}

.search-row {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  gap: 16rpx;
}
.search-input { flex: 1; }
.scan-btn {
  width: 72rpx; height: 72rpx;
  display: flex; align-items: center; justify-content: center;
  border: 1px solid #e5e5e5; border-radius: 12rpx;
  background: #fff;
  padding: 0; margin: 0;
}
.scan-btn::after { border: none; }
.search-btn { margin: 0; font-size: 26rpx; height: 64rpx; line-height: 64rpx; }

.order-info {
  padding: 16rpx 24rpx 24rpx;
  border-bottom: 1px solid #f0f0f0;
}
.info-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8rpx 0;
}
.label { color: #999; font-size: 26rpx; }
.value { font-size: 28rpx; color: #333; }
.bold { font-weight: 600; }

.line-header {
  display: flex; justify-content: space-between;
  padding: 24rpx 24rpx 12rpx;
  font-size: 28rpx;
}
.line-item {
  padding: 20rpx 24rpx;
  border-bottom: 1px solid #f5f5f5;
}
.line-info { margin-bottom: 12rpx; }
.line-qty {
  display: flex; align-items: center; gap: 16rpx;
  flex-wrap: wrap;
}
.qty-input {
  display: flex; align-items: center;
  gap: 8rpx;
}
.qty-field {
  width: 160rpx;
}
.unit { color: #666; font-size: 24rpx; }
.text-grey { color: #999; font-size: 24rpx; }
.text-green { color: #4cd964; font-size: 24rpx; }

.photo-row {
  display: flex; gap: 20rpx; flex-wrap: wrap;
  padding: 16rpx 24rpx 24rpx;
}
.photo-item {
  width: 160rpx; height: 160rpx;
  border-radius: 12rpx; overflow: hidden;
  position: relative;
}
.photo-img { width: 100%; height: 100%; }
.photo-del {
  position: absolute; top: 4rpx; right: 4rpx;
  background: rgba(0,0,0,0.5); border-radius: 50%;
  padding: 4rpx;
}
.photo-add {
  width: 160rpx; height: 160rpx;
  border: 2rpx dashed #ccc; border-radius: 12rpx;
  display: flex; flex-direction: column; align-items: center;
  justify-content: center; gap: 8rpx;
  color: #999;
}

.form-box { padding: 16rpx 24rpx; }

.footer-bar {
  position: fixed; bottom: 0; left: 0; right: 0;
  padding: 20rpx 32rpx; padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff; border-top: 1px solid #f0f0f0;
  z-index: 100;
}
.confirm-btn {
  width: 100%; height: 88rpx; line-height: 88rpx;
  font-size: 32rpx; border-radius: 12rpx;
}
</style>
