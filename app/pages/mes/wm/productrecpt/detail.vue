<template>
  <view class="container">
    <!-- 加载中 -->
    <view v-if="loading" class="loading-box">
      <uni-load-more status="loading" />
    </view>

    <template v-else>
      <!-- 入库单信息 -->
      <view class="section">
        <uni-section title="入库单信息" type="line">
          <template #right>
            <uni-tag :type="recptStatusTagType(header.status)" :text="recptStatusText(header.status)" size="small" />
          </template>
        </uni-section>
        <view class="info-card">
          <view class="info-row">
            <text class="label">入库单号</text>
            <text class="value bold">{{ header.recptCode }}</text>
          </view>
          <view class="info-row">
            <text class="label">入库名称</text>
            <text class="value">{{ header.recptName || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="label">工单号</text>
            <text class="value">{{ header.workorderCode || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="label">产出编号</text>
            <text class="value">{{ header.produceCode || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="label">入库日期</text>
            <text class="value">{{ header.recptDate || '-' }}</text>
          </view>
        </view>
      </view>

      <!-- 物料明细 — 可编辑（仅草稿状态） -->
      <view class="section">
        <uni-section title="物料明细" type="line">
          <template v-if="isEditable" #right>
            <text class="scan-tip">填写实收数量</text>
          </template>
        </uni-section>
        <view v-for="(line, idx) in lines" :key="idx" class="line-item">
          <view class="line-info">
            <text class="bold">{{ line.itemName }}</text>
            <text class="text-grey">{{ line.itemCode }}</text>
          </view>
          <view v-if="line.specification" class="line-spec">
            <text class="label">规格：{{ line.specification }}</text>
            <text class="unit-text">{{ line.unitName }}</text>
          </view>

          <!-- 实收件数 -->
          <view class="line-qty">
            <text class="label">实收件数</text>
            <view class="qty-input">
              <uni-easyinput
                v-model="line.quantityRecpt"
                type="number"
                placeholder="0"
                :inputBorder="true"
                :disabled="!isEditable"
                class="qty-field"
              />
              <text class="unit">{{ line.unitName }}</text>
            </view>
          </view>

          <!-- 实收箱数 -->
          <view class="line-qty">
            <text class="label">实收箱数</text>
            <view class="qty-input">
              <uni-easyinput
                v-model="line.quantityBox"
                type="number"
                placeholder="0"
                :inputBorder="true"
                :disabled="!isEditable"
                class="qty-field"
              />
              <text class="unit">箱</text>
            </view>
          </view>

          <!-- 入库仓库（行级） -->
          <view v-if="isEditable" class="line-warehouse">
            <text class="label">入库仓库</text>
            <picker :value="warehouseIndex(line.warehouseId)"
              :range="warehouseList" range-key="warehouseName"
              @change="(e) => line.warehouseId = warehouseList[e.detail.value].warehouseId">
              <view class="picker-value-sm">
                {{ warehouseNameOf(line.warehouseId) || '默认' }}
                <uni-icons type="right" size="12" color="#999" />
              </view>
            </picker>
          </view>

          <!-- 批次号 -->
          <view v-if="line.batchCode" class="line-extra">
            <text class="label">批次：{{ line.batchCode }}</text>
          </view>
        </view>

        <!-- 汇总 -->
        <view class="summary-row">
          <text class="label">总件数</text>
          <text class="total-val">{{ totalQty }} PCS</text>
          <text class="label" style="margin-left: 32rpx;">总箱数</text>
          <text class="total-val">{{ totalBox }} 箱</text>
        </view>
      </view>

      <!-- 拍照留证（仅草稿，选填） -->
      <view v-if="isEditable" class="section">
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

      <!-- 备注 -->
      <view v-if="isEditable" class="section">
        <uni-section title="备注（选填）" type="line"></uni-section>
        <view class="form-box">
          <uni-easyinput
            type="textarea"
            v-model="remark"
            placeholder="可填写备注信息"
            :maxlength="200"
          />
        </view>
      </view>

      <!-- 底部确认按钮（仅草稿状态） -->
      <view v-if="isEditable" class="footer-bar">
        <button type="primary" class="confirm-btn" @click="handleConfirm" :disabled="submitting">
          {{ submitting ? '提交中...' : '确认入库' }}
        </button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { ref, reactive, computed, getCurrentInstance } from 'vue'
// 显式引入 uni-ui 组件（绕过 HBuilderX 发行 H5 时 easycom 失效）
import UniEasyInput from '@/uni_modules/uni-easyinput/components/uni-easyinput/uni-easyinput.vue'
import UniIcons from '@/uni_modules/uni-icons/components/uni-icons/uni-icons.vue'
import UniLoadMore from '@/uni_modules/uni-load-more/components/uni-load-more/uni-load-more.vue'
import UniSection from '@/components/uni-section/uni-section.vue'
import UniTag from '@/uni_modules/uni-tag/components/uni-tag/uni-tag.vue'
import { onLoad } from '@dcloudio/uni-app'
import { getProductRecptDetail, mobileConfirmProductRecpt, listWarehouseAll } from '@/api/mes/wm/productrecpt'
import { recptStatusText, recptStatusTagType, isValidRecptQty } from '@/utils/wm-productrecpt.js'

const { proxy } = getCurrentInstance()
const header = ref({})
const lines = ref([])
const loading = ref(true)
const submitting = ref(false)
const photos = ref([])
const remark = ref('')
const warehouseList = ref([])

const isEditable = computed(() => header.value.status === 'DRAFT')

const totalQty = computed(() => {
  return lines.value.reduce((sum, l) => sum + (Number(l.quantityRecpt) || 0), 0)
})

const totalBox = computed(() => {
  return lines.value.reduce((sum, l) => sum + (Number(l.quantityBox) || 0), 0)
})

function warehouseNameOf(id) {
  const found = warehouseList.value.find(w => w.warehouseId === id)
  return found ? found.warehouseName : ''
}

function warehouseIndex(id) {
  const idx = warehouseList.value.findIndex(w => w.warehouseId === id)
  return idx >= 0 ? idx : 0
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

function previewPhoto(idx) {
  uni.previewImage({
    current: idx,
    urls: photos.value
  })
}

function removePhoto(idx) {
  photos.value.splice(idx, 1)
}

// 确认入库
function handleConfirm() {
  // 发送全部行（后端 require all lines），校验至少一行有数量
  if (totalQty.value <= 0) {
    proxy.$modal.msgError('请至少填写一行的实收数量')
    return
  }

  proxy.$modal.confirm('确认入库？确认后将更新库存，不可撤销。').then(() => {
    submitting.value = true
    const body = {
      warehouseId: header.value.warehouseId,
      remark: remark.value || null,
      lines: lines.value.map(l => ({
        lineId: l.lineId,
        quantityRecpt: parseFloat(l.quantityRecpt),
        quantityBox: Number(l.quantityBox) || 0,
        warehouseId: l.warehouseId || header.value.warehouseId,
        batchCode: l.batchCode || null
      }))
    }

    mobileConfirmProductRecpt(header.value.recptId, body).then(() => {
      proxy.$modal.msgSuccess('入库确认成功！库存已更新')
      setTimeout(() => { proxy.$tab.navigateBack() }, 1500)
    }).catch(e => {
      // 注意：request.js 拦截器已 toast 过具体错误（如"仅草稿状态可确认收货"），
      // 这里只做兜底提示，避免覆盖拦截器的具体消息。
      const detail = typeof e === 'string' ? e : (e && (e.msg || e.message)) ? (e.msg || e.message) : ''
      if (detail) {
        proxy.$modal.msgError('入库失败：' + detail)
      }
    }).finally(() => {
      submitting.value = false
    })
  }).catch(() => {})
}

onLoad((options) => {
  const recptId = options.recptId
  if (!recptId) {
    proxy.$modal.msgError('参数错误')
    setTimeout(() => { proxy.$tab.navigateBack() }, 1500)
    return
  }

  // 并行加载入库单详情和仓库列表
  Promise.all([
    getProductRecptDetail(recptId),
    listWarehouseAll()
  ]).then(([recptRes, warehouseRes]) => {
    const data = recptRes.data || {}
    header.value = data
    lines.value = (data.lines || []).map(l => ({
      ...l,
      quantityRecpt: l.quantityRecpt != null ? l.quantityRecpt : '',
      quantityBox: l.quantityBox != null ? l.quantityBox : ''
    }))
    warehouseList.value = warehouseRes.data || []
    loading.value = false
  }).catch(() => {
    proxy.$modal.msgError('加载失败，请重试')
    loading.value = false
  })
})
</script>

<style lang="scss" scoped>
page { background-color: #f5f6f7; min-height: 100%; }

.container { padding-bottom: 120rpx; }

.section {
  margin: 20rpx 0;
  background: #fff;
}

.loading-box {
  display: flex; justify-content: center; padding: 80rpx 0;
}

.scan-tip { color: #999; font-size: 24rpx; }

.info-card {
  padding: 16rpx 24rpx 24rpx;
}
.info-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8rpx 0;
}
.label { color: #999; font-size: 26rpx; }
.value { font-size: 28rpx; color: #333; }
.bold { font-weight: 600; }

.line-item {
  padding: 20rpx 24rpx;
  border-bottom: 1px solid #f5f5f5;
}
.line-info {
  display: flex; justify-content: space-between;
  margin-bottom: 8rpx;
}
.line-spec {
  display: flex; justify-content: space-between;
  margin-bottom: 12rpx;
}
.unit-text { color: #666; font-size: 24rpx; }

.line-qty {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 8rpx;
}
.qty-input {
  display: flex; align-items: center;
  gap: 8rpx;
}
.qty-field { width: 180rpx; }
.unit { color: #666; font-size: 24rpx; }

.line-warehouse {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 12rpx; padding-top: 12rpx;
  border-top: 1px dashed #e5e5e5;
}
.picker-value-sm {
  display: flex; align-items: center; gap: 6rpx;
  color: #333; font-size: 26rpx;
}

.line-extra {
  margin-top: 8rpx; padding-top: 8rpx;
  border-top: 1px dashed #e5e5e5;
}

.summary-row {
  display: flex; align-items: center;
  padding: 20rpx 24rpx;
}
.total-val {
  font-size: 32rpx; font-weight: 700; color: #409eff;
}

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

.text-grey { color: #999; font-size: 24rpx; }

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
