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
          <uni-tag :type="orderStatusTagType(order.status)" :text="orderStatusText(order.status)" size="small" />
        </view>
      </view>

      <!-- 物料行 — 填入实收数量 + 仓库 -->
      <view class="line-header">
        <text class="bold">物料明细</text>
        <text class="text-grey">填写实收数量</text>
      </view>
      <view v-for="(line, idx) in lines" :key="idx" class="line-item">
        <view class="line-info">
          <text class="bold">{{ line.itemName }}</text>
          <text class="text-grey">{{ line.itemCode }}</text>
        </view>
        <view class="line-spec">
          <text class="label">规格型号</text>
          <uni-easyinput v-model="line.specification" placeholder="请输入" :inputBorder="true" class="spec-field" />
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
        <view class="line-warehouse">
          <text class="label">入库仓库</text>
          <picker :value="warehouseList.findIndex(w => w.warehouseId === line.warehouseId)"
            :range="warehouseList" range-key="warehouseName"
            @change="(e) => line.warehouseId = warehouseList[e.detail.value].warehouseId">
            <view class="picker-value-sm">
              {{ warehouseNameOf(line.warehouseId) || '请选择' }}
              <uni-icons type="right" size="12" color="#999" />
            </view>
          </picker>
        </view>
        <view class="line-extra">
          <view class="extra-row">
            <text class="label">生产日期</text>
            <picker mode="date" :value="line.produceDate" @change="(e) => line.produceDate = e.detail.value">
              <view class="picker-value-sm">{{ line.produceDate || '请选择' }}</view>
            </picker>
          </view>
          <view class="extra-row">
            <text class="label">有效期至</text>
            <picker mode="date" :value="line.expireDate" @change="(e) => line.expireDate = e.detail.value">
              <view class="picker-value-sm">{{ line.expireDate || '请选择' }}</view>
            </picker>
          </view>
          <view class="extra-row">
            <text class="label">生产批号</text>
            <uni-easyinput v-model="line.lotNumber" placeholder="供应商批号" :inputBorder="true" class="extra-field" />
          </view>
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
import { ref, reactive, getCurrentInstance, onMounted } from 'vue'
// 显式引入 uni-ui 组件（绕过 HBuilderX 发行 H5 时 easycom 失效）
import UniEasyInput from '@/uni_modules/uni-easyinput/components/uni-easyinput/uni-easyinput.vue'
import UniForms from '@/uni_modules/uni-forms/components/uni-forms/uni-forms.vue'
import UniFormsItem from '@/uni_modules/uni-forms/components/uni-forms-item/uni-forms-item.vue'
import UniIcons from '@/uni_modules/uni-icons/components/uni-icons/uni-icons.vue'
import UniSection from '@/components/uni-section/uni-section.vue'
import UniTag from '@/uni_modules/uni-tag/components/uni-tag/uni-tag.vue'
import { getOrderDetailByCode, receiveItemRecpt, listWarehouseAll } from '@/api/mes/pur/order'
import { isValidReceiptQty, genRecptCode, purchaseTypeText, orderStatusTagType, orderStatusText, canReceive } from '@/utils/pur.js'

const { proxy } = getCurrentInstance()
const orderCode = ref('')
const order = ref(null)
const lines = ref([])
const photos = ref([])
const submitting = ref(false)
const warehouseList = ref([])
function warehouseNameOf(id) {
  const found = warehouseList.value.find(w => w.warehouseId === id)
  return found ? found.warehouseName : ''
}
const arrivalInfo = reactive({
  logisticsNo: '',
  vehiclePlate: '',
  vendorDeliveryNo: ''
})

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

// 搜索PO（一次请求拿到头+行，避免二次调用 order-line/list）
function searchOrder() {
  if (!orderCode.value.trim()) {
    proxy.$modal.msgError('请输入PO单号')
    return
  }
  proxy.$modal.loading('查询中...')
  getOrderDetailByCode(orderCode.value.trim()).then(res => {
    proxy.$modal.closeLoading()
    const detail = res.data
    if (!detail || !detail.order) {
      // H5 下 hideLoading + 立即 showToast/showModal 有时序冲突，用 setTimeout 错开
      setTimeout(() => proxy.$modal.msgError('未找到该采购订单'), 60)
      return
    }
    const found = detail.order
    if (!canReceive(found.status)) {
      // 用 alert 弹框而不是 toast：toast 在 H5/小程序都有宽度限制，长文本会被截断
      // 延迟一帧，避免被 hideLoading 遗留的 mask 挡住
      setTimeout(() => {
        proxy.$modal.alert('该订单状态为"' + orderStatusText(found.status) + '"，仅"已下单/收货中"的订单可执行收货操作', '无法收货')
      }, 60)
      return
    }
    // 存储原始数据，模板中用 purchaseTypeText() 显示中文
    order.value = found
    lines.value = (detail.lines || []).map(l => ({
      ...l,
      receiptQty: '',
      warehouseId: null,
      produceDate: '',
      expireDate: '',
      lotNumber: l.lotNumber || '',
      quantityReceived: l.quantityReceived || 0
    }))
  }).catch(() => {
    proxy.$modal.closeLoading()
    proxy.$modal.msgError('查询失败，请检查单号')
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
  const filledLines = lines.value.filter(l => isValidReceiptQty(l.receiptQty))
  if (filledLines.some(l => !l.warehouseId)) {
    proxy.$modal.msgError('请为每个已填数量的行选择入库仓库')
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
        quantityRecpt: parseFloat(l.receiptQty),
        warehouseId: l.warehouseId,
        warehouseCode: warehouseList.value.find(w => w.warehouseId === l.warehouseId)?.warehouseCode || '',
        warehouseName: warehouseNameOf(l.warehouseId),
        produceDate: l.produceDate || null,
        expireDate: l.expireDate || null,
        lotNumber: l.lotNumber || null
      }))
    }

    // 单接口调用，后端原子完成：创建头 → 创建行 → 确认收货 → 回写PO
    receiveItemRecpt(body).then(() => {
      proxy.$modal.msgSuccess('收货确认成功！库存已更新')
      setTimeout(() => { proxy.$tab.navigateBack() }, 1500)
    }).catch(e => {
      proxy.$modal.msgError('收货失败：' + (typeof e === 'string' ? e : (e.msg || e.message || '未知错误')))
    }).finally(() => {
      submitting.value = false
    })
  }).catch(() => {})
}

onMounted(() => {
  listWarehouseAll().then(res => { warehouseList.value = res.data || [] })
})
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

.line-warehouse {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 12rpx; padding-top: 12rpx;
  border-top: 1px dashed #e5e5e5;
}
.picker-value-sm {
  display: flex; align-items: center; gap: 6rpx;
  color: #333; font-size: 26rpx;
}

.line-spec {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 8rpx;
}
.spec-field { width: 280rpx; }

.line-extra {
  margin-top: 8rpx; padding-top: 8rpx;
  border-top: 1px dashed #e5e5e5;
}
.extra-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 4rpx 0;
}
.extra-field { width: 260rpx; }

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
