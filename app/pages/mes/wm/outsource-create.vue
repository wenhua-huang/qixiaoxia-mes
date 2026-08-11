<template>
  <view class="container">
    <!-- 选工单：扫码为主 + 输入兜底 -->
    <uni-section title="发料对象（生产工单）" type="line"></uni-section>
    <view class="form-card">
      <view class="scan-row">
        <button class="cu-btn bg-blue sm" @click="handleScan">扫码</button>
        <input class="card-input" v-model="workorderCode" placeholder="扫或输入工单号" confirm-type="search" @confirm="searchWorkorder" />
        <button class="cu-btn sm" :disabled="loading" @click="searchWorkorder">{{ loading ? '搜索中...' : '搜索' }}</button>
      </view>
    </view>

    <template v-if="info">
      <!-- 工单信息 -->
      <uni-section title="工单信息" type="line"></uni-section>
      <view class="form-card">
        <view class="info-line"><text class="code">{{ info.workorderCode }}</text></view>
        <view class="info-line"><text class="meta">{{ info.workorderName || '-' }}</text></view>
        <view class="info-line"><text class="meta">产品：{{ info.productName || '-' }}</text></view>
      </view>

      <!-- 外协工序列表 -->
      <uni-section :title="'外协工序（' + (info.outsourceProcesses || []).length + '）'" type="line"></uni-section>

      <view v-if="(info.outsourceProcesses || []).length === 0" class="form-card">
        <view class="empty-hint"><text class="text-grey">该工单工艺路线无外协工序</text></view>
      </view>

      <view v-for="p in (info.outsourceProcesses || [])" :key="p.processId" :class="['form-card', 'process-card', highlightId === p.processId ? 'just-issued' : '']">
        <view class="proc-head" @click="toggleProcess(p)">
          <view>
            <text class="proc-name">{{ p.processName }}</text>
            <text class="proc-code">{{ p.processCode }}</text>
          </view>
          <view v-if="p.existingOrderId" class="proc-right">
            <uni-tag :text="statusText(p.existingStatus)" :type="statusTagType(p.existingStatus)" size="small" />
          </view>
          <uni-icons v-else :type="expandedId === p.processId ? 'up' : 'down'" size="18" color="#999" />
        </view>

        <!-- 已有外协单：防重分流 -->
        <view v-if="p.existingOrderId" class="proc-body">
          <view class="info-line"><text class="meta">外协单：{{ p.existingOrderCode }}</text></view>
          <button v-if="p.existingStatus === 'DRAFT'" class="cu-btn bg-orange sm full-btn" @click="goExecute(p.existingOrderId)">执行草稿发料</button>
          <view v-else class="exist-tip"><text class="text-grey">该工序已发料/加工中，不能重复创建</text></view>
        </view>

        <!-- 无外协单：新建发料表单 -->
        <view v-else-if="expandedId === p.processId" class="proc-body">
          <view class="form-row" @click="openVendorPicker(p)">
            <text class="label">厂商</text>
            <view class="picker-value">
              <text :class="formOf(p).vendorName ? 'value' : 'placeholder'">{{ formOf(p).vendorName || '请选择外协厂商' }}</text>
              <uni-icons type="right" size="16" color="#999"></uni-icons>
            </view>
          </view>
          <view class="form-row">
            <text class="label">备注</text>
            <input class="remark-input" v-model="formOf(p).remark" placeholder="选填" />
          </view>

          <view v-if="formOf(p).issueLines.length === 0" class="empty-hint"><text class="text-grey">该工序无 BOM 发料行</text></view>
          <view v-for="(line, idx) in formOf(p).issueLines" :key="idx" class="line-item">
            <view class="line-head">
              <text class="line-name">{{ line.itemName }}</text>
              <text class="line-spec">{{ line.itemCode }}{{ line.specification ? ' · ' + line.specification : '' }}</text>
            </view>
            <view class="line-body">
              <view class="line-row" @click="openWarehousePicker(p.processId, idx)">
                <text class="line-label">仓库</text>
                <view class="picker-value">
                  <text :class="line.warehouseName ? 'value' : 'placeholder'">{{ line.warehouseName || '请选择' }}</text>
                  <uni-icons type="right" size="14" color="#999"></uni-icons>
                </view>
              </view>
              <view class="line-row" v-if="line.batchCode">
                <text class="line-label">批次</text>
                <text class="value">{{ line.batchCode }}</text>
              </view>
              <view class="line-row">
                <text class="line-label">数量</text>
                <input class="qty-input" type="digit" v-model="line.quantity" />
                <text class="unit">{{ line.unitName || '吨' }}</text>
              </view>
            </view>
          </view>

          <button class="cu-btn bg-blue lg footer-submit" :disabled="!canSubmit(p) || formOf(p).submitting" @click="submit(p)">
            {{ formOf(p).submitting ? '提交中...' : '确认发料' }}
          </button>
        </view>
      </view>
    </template>

    <!-- 厂商选择弹窗 -->
    <uni-popup ref="vendorPopup" type="bottom" :is-mask-click="true">
      <view class="popup-content">
        <view class="popup-header"><text class="popup-title">选择外协厂商</text><text class="popup-close" @click="vendorPopup.close()">关闭</text></view>
        <scroll-view scroll-y class="popup-scroll">
          <view v-for="v in vendorOptions" :key="v.vendorId" :class="['popup-item', pickerProcess && formOf(pickerProcess).vendorId === v.vendorId ? 'selected' : '']" @click="pickVendor(v)">
            <view><text class="line-name">{{ v.vendorName }}</text><text class="line-spec">{{ v.vendorCode }}</text></view>
            <uni-icons :type="pickerProcess && formOf(pickerProcess).vendorId === v.vendorId ? 'checkbox-filled' : 'circle'" size="22" :color="pickerProcess && formOf(pickerProcess).vendorId === v.vendorId ? '#007aff' : '#ccc'"></uni-icons>
          </view>
        </scroll-view>
      </view>
    </uni-popup>

    <!-- 仓库选择弹窗 -->
    <uni-popup ref="warehousePopup" type="bottom" :is-mask-click="true">
      <view class="popup-content">
        <view class="popup-header"><text class="popup-title">选择仓库（可用批次）</text><text class="popup-close" @click="warehousePopup.close()">关闭</text></view>
        <scroll-view scroll-y class="popup-scroll">
          <view v-if="batchOptions.length === 0" class="empty-hint"><text class="text-grey">暂无可用库存</text></view>
          <view v-for="b in batchOptions" :key="(b.warehouseId || '') + '-' + (b.batchId || '')" class="popup-item" @click="pickWarehouse(b)">
            <view><text class="line-name">{{ b.warehouseName }}</text><text class="line-spec">可用 {{ b.quantityAvailable }} · 批次 {{ b.batchCode || '-' }}</text></view>
            <uni-icons type="right" size="16" color="#ccc"></uni-icons>
          </view>
        </scroll-view>
      </view>
    </uni-popup>
  </view>
</template>

<script setup>
import { ref, reactive, getCurrentInstance } from 'vue'
import { getWorkorderOutsourceInfo } from '@/api/mes/pro/workorder'
import { listOutsourceVendor } from '@/api/mes/pro/slitting'
import { createOutsource } from '@/api/mes/wm/outsource'
import { availableBatches } from '@/api/mes/wm/issue'

const { proxy } = getCurrentInstance()
const vendorPopup = ref()
const warehousePopup = ref()

const workorderCode = ref('')
const loading = ref(false)
const info = ref(null)
const expandedId = ref(null)
const pickerProcess = ref(null)
const vendorOptions = ref([])
const batchOptions = ref([])
const activeProcessId = ref(null)
const activeLineIdx = ref(null)
// 刚发料的工序ID，用于高亮反馈
const highlightId = ref(null)
// 每个外协工序独立的发料表单，key=processId
const forms = reactive({})

const STATUS_TEXT = { DRAFT: '草稿', ISSUED: '已发料', VENDOR_RCVD: '厂商已签收', PROCESSING: '加工中', FINISHED: '已完成', SHIPPED: '已发货', RECEIVED: '已收货' }
function statusText(s) { return STATUS_TEXT[s] || s || '' }
function statusTagType(s) {
  return { DRAFT: 'warning', ISSUED: 'warning', VENDOR_RCVD: 'info', PROCESSING: 'primary', FINISHED: 'primary', SHIPPED: 'warning', RECEIVED: 'success' }[s] || 'default'
}

function formOf(p) { return forms[p.processId] }

function handleScan() {
  // #ifdef APP-PLUS || H5
  uni.scanCode({
    onlyFromCamera: false, scanType: ['barCode', 'qrCode'],
    success: (res) => { workorderCode.value = res.result; searchWorkorder() },
    fail: () => {}
  })
  // #endif
  // #ifdef MP-WEIXIN
  uni.scanCode({ success: (res) => { workorderCode.value = res.result; searchWorkorder() } })
  // #endif
}

async function searchWorkorder() {
  const code = workorderCode.value.trim()
  if (!code) { proxy.$modal.msgError('请输入工单号'); return }
  loading.value = true
  info.value = null
  Object.keys(forms).forEach(k => delete forms[k])
  try {
    const res = await getWorkorderOutsourceInfo(code)
    info.value = res.data
    if (!info.value.outsourceProcesses || info.value.outsourceProcesses.length === 0) {
      proxy.$modal.msgWarning('该工单无外协工序')
    } else {
      // 预载厂商
      if (vendorOptions.value.length === 0) {
        const vres = await listOutsourceVendor()
        vendorOptions.value = (vres.data || []).filter(v => v.vendorType === 'OUTSOURCE' || v.vendorType === 'BOTH')
      }
      // 为无外协单的工序初始化表单；只有一道时自动展开
      const creatable = info.value.outsourceProcesses.filter(p => !p.existingOrderId)
      for (const p of creatable) await initForm(p)
      if (creatable.length === 1) expandedId.value = creatable[0].processId
    }
  } catch (e) {
    // request.js 已提示
  } finally { loading.value = false }
}

async function initForm(p) {
  const f = reactive({
    vendorId: p.vendorId || null, vendorCode: p.vendorCode || '', vendorName: p.vendorName || '',
    remark: '', submitting: false,
    issueLines: (p.bomLines || []).map(b => ({
      itemId: b.itemId, itemCode: b.itemCode, itemName: b.itemName, specification: b.specification || '',
      quantity: b.quantity, unitOfMeasure: b.unitOfMeasure || 'TON', unitName: b.unitName || '吨',
      batchId: 0, warehouseId: null, warehouseCode: '', warehouseName: '', batchCode: '',
      quantityAvailable: null
    }))
  })
  forms[p.processId] = f
  // FIFO 预填仓库/批次
  await Promise.all(f.issueLines.map(async (line) => {
    try {
      const bres = await availableBatches(line.itemId)
      const avail = (bres.data || []).find(b => b.quantityAvailable > 0)
      if (avail) {
        line.warehouseId = avail.warehouseId; line.warehouseCode = avail.warehouseCode
        line.warehouseName = avail.warehouseName; line.batchId = avail.batchId || 0; line.batchCode = avail.batchCode || ''
        line.quantityAvailable = avail.quantityAvailable
      }
    } catch (_) {}
  }))
}

function toggleProcess(p) {
  if (p.existingOrderId) return
  expandedId.value = expandedId.value === p.processId ? null : p.processId
}

function canSubmit(p) {
  const f = formOf(p)
  if (!f || !f.vendorId) return false
  return f.issueLines.length > 0 && f.issueLines.every(l => l.warehouseId != null && Number(l.quantity) > 0)
}

function openVendorPicker(p) { pickerProcess.value = p; vendorPopup.value.open() }
function pickVendor(v) {
  if (pickerProcess.value) {
    const f = formOf(pickerProcess.value)
    f.vendorId = v.vendorId; f.vendorCode = v.vendorCode; f.vendorName = v.vendorName
  }
  vendorPopup.value.close()
}

async function openWarehousePicker(processId, idx) {
  activeProcessId.value = processId; activeLineIdx.value = idx
  try { const res = await availableBatches(forms[processId].issueLines[idx].itemId); batchOptions.value = res.data || [] }
  catch (e) { batchOptions.value = [] }
  warehousePopup.value.open()
}
function pickWarehouse(b) {
  const line = forms[activeProcessId.value].issueLines[activeLineIdx.value]
  line.warehouseId = b.warehouseId; line.warehouseCode = b.warehouseCode; line.warehouseName = b.warehouseName
  line.batchId = b.batchId || 0; line.batchCode = b.batchCode || ''
  line.quantityAvailable = b.quantityAvailable
  warehousePopup.value.close()
}

function goExecute(orderId) {
  proxy.$tab.navigateTo('/pages/mes/wm/outsource-execute?orderId=' + orderId)
}

async function submit(p) {
  const f = formOf(p)
  if (!canSubmit(p)) { proxy.$modal.msg('请补全厂商和每行仓库/数量'); return }
  // FIFO 库存不足时提示（不阻断，用户可能改用其他批次或手工调整）
  const insufficient = f.issueLines.filter(l => l.quantityAvailable != null && Number(l.quantity) > Number(l.quantityAvailable))
  if (insufficient.length > 0) {
    proxy.$modal.msgWarning(`有 ${insufficient.length} 行数量超过当前可用库存，请确认`)
  }
  const confirmed = await new Promise(resolve => {
    uni.showModal({
      title: '确认发料',
      content: `将扣减库存并创建外协发料单（${f.issueLines.length} 行），确认？`,
      success: r => resolve(r.confirm)
    })
  })
  if (!confirmed) return
  f.submitting = true
  try {
    const res = await createOutsource({
      vendorId: f.vendorId, vendorCode: f.vendorCode, vendorName: f.vendorName,
      workorderId: info.value.workorderId, workorderCode: info.value.workorderCode,
      cardId: info.value.activeCardId, routeId: info.value.routeId,
      processId: p.processId, processCode: p.processCode, processName: p.processName,
      sourceType: p.processCode === 'PRC-SLIT' ? 'SLITTING' : 'GENERIC',
      remark: f.remark,
      // <input type="digit"> 的 v-model 产出字符串，后端/BigDecimal 反序列化需显式转数字
      issueLines: f.issueLines.map(l => ({ ...l, quantity: Number(l.quantity) }))
    })
    proxy.$modal.msgSuccess('已发料 ' + f.issueLines.length + ' 行')
    // 本地把该工序置为已发料，避免整页 info=null 重渲染闪烁
    const order = res.data || {}
    const proc = (info.value.outsourceProcesses || []).find(x => x.processId === p.processId)
    if (proc) {
      proc.existingOrderId = order.orderId
      proc.existingOrderCode = order.orderCode
      proc.existingStatus = order.status || 'ISSUED'
    }
    delete forms[p.processId]
    if (expandedId.value === p.processId) expandedId.value = null
    highlightId.value = p.processId
    setTimeout(() => { highlightId.value = null }, 2000)
  } catch (e) {} finally { f.submitting = false }
}
</script>

<style lang="scss" scoped>
page { background-color: #f5f6f7; min-height: 100%; }
.container { padding: 0 0 40rpx; }
.form-card { background: #fff; margin: 16rpx 24rpx; border-radius: 16rpx; padding: 16rpx 24rpx; }
.form-row { display: flex; justify-content: space-between; align-items: center; padding: 20rpx 0; border-bottom: 1px solid #f5f5f5; }
.form-row:last-child, .line-item:last-child { border-bottom: none; }
.label, .value, .code { color: #333; font-size: 28rpx; }
.label { font-weight: 500; }
.code { font-size: 30rpx; font-weight: 600; }
.picker-value { display: flex; align-items: center; gap: 8rpx; }
.placeholder { color: #ccc; font-size: 28rpx; }
.remark-input { text-align: right; font-size: 28rpx; flex: 1; margin-left: 24rpx; }
.scan-row { display: flex; align-items: center; gap: 16rpx; padding: 16rpx 0; }
.card-input, .qty-input { flex: 1; font-size: 30rpx; }
.card-input { background: #f5f6f7; border-radius: 12rpx; height: 72rpx; line-height: 72rpx; padding: 0 24rpx; box-sizing: border-box; }
.qty-input { text-align: right; }
.info-line { display: flex; padding: 6rpx 0; }
.meta, .text-grey, .line-spec, .proc-code, .unit { font-size: 26rpx; color: #999; }
.meta { color: #666; font-size: 26rpx; }
.empty-hint { padding: 40rpx 0; text-align: center; }
.process-card { padding: 0; transition: box-shadow .3s ease; }
.process-card.just-issued { box-shadow: 0 0 0 4rpx rgba(0,122,255,.25); animation: flash 1.6s ease; }
@keyframes flash {
  0%, 100% { background: #fff; }
  30% { background: #eef6ff; }
}
.proc-head { display: flex; justify-content: space-between; align-items: center; padding: 24rpx; }
.proc-name, .line-name { font-size: 28rpx; font-weight: 600; color: #333; }
.proc-name { margin-right: 16rpx; }
.proc-body { padding: 0 24rpx 24rpx; border-top: 1px solid #f5f5f5; }
.proc-right { display: flex; align-items: center; }
.exist-tip { padding: 16rpx 0; }
.full-btn { width: 100%; margin-top: 16rpx; }
.line-item { padding: 20rpx 0; border-bottom: 1px solid #f5f5f5; }
.line-head { display: flex; flex-direction: column; gap: 4rpx; margin-bottom: 12rpx; }
.line-body { display: flex; flex-direction: column; gap: 8rpx; padding-left: 16rpx; }
.line-row { display: flex; align-items: center; gap: 16rpx; }
.line-label { color: #666; font-size: 26rpx; width: 80rpx; }
.popup-content { background: #fff; border-radius: 24rpx 24rpx 0 0; max-height: 80vh; display: flex; flex-direction: column; }
.popup-header { display: flex; justify-content: space-between; align-items: center; padding: 24rpx; border-bottom: 1px solid #f0f0f0; }
.popup-title { font-size: 30rpx; font-weight: 600; }
.popup-close { color: #999; font-size: 28rpx; }
.popup-scroll { max-height: 60vh; }
.popup-item { display: flex; justify-content: space-between; align-items: center; padding: 24rpx; border-bottom: 1px solid #f5f5f5; }
.popup-item.selected { background: #f0f7ff; }
.footer-submit, .cu-btn.lg { width: 100%; font-size: 30rpx; height: 88rpx; line-height: 88rpx; }
.footer-submit { margin-top: 24rpx; }
.cu-btn { border-radius: 12rpx; border: none; }
.cu-btn.sm { font-size: 28rpx; height: 72rpx; line-height: 72rpx; padding: 0 28rpx; }
.bg-blue { background: #007aff; color: #fff; }
.bg-orange { background: #ff9900; color: #fff; }
.cu-btn[disabled] { opacity: 0.5; }
</style>
