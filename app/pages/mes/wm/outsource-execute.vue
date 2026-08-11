<template>
  <view class="container">
    <!-- 直达模式：从外协发料页"执行草稿"进入，显示单头信息 -->
    <template v-if="directMode && selectedOrder">
      <uni-section title="执行草稿发料" type="line"></uni-section>
      <view class="form-card">
        <view class="draft-head">
          <text class="code">{{ selectedOrder.orderCode }}</text>
          <uni-tag :text="statusLabel(selectedOrder.status)" :type="statusTag(selectedOrder.status)" size="small" />
        </view>
        <view class="info-line"><text class="meta">工序：{{ selectedOrder.processName || '-' }}</text></view>
        <view class="info-line"><text class="meta">厂商：{{ selectedOrder.vendorName || '-' }}</text></view>
      </view>
    </template>

    <!-- 扫工单码：扫码为主 + 输入兜底 -->
    <template v-else>
    <uni-section title="扫码查草稿外协单" type="line"></uni-section>
    <view class="form-card">
      <view class="scan-row">
        <button class="cu-btn bg-blue sm" @click="scanWorkorder">扫码</button>
        <input class="card-input" v-model="workorderCode" placeholder="扫或输入工单号" confirm-type="search" @confirm="searchDrafts" />
        <button class="cu-btn sm" @click="searchDrafts">搜索</button>
      </view>
    </view>

    <!-- 草稿外协单列表 -->
    <template v-if="searched">
      <uni-section :title="'草稿外协单（' + draftList.length + '）'" type="line"></uni-section>
      <view class="form-card">
        <view v-if="draftList.length === 0" class="empty-hint"><text class="text-grey">该工单暂无草稿外协发料单（可能已执行或无外协工序）</text></view>
        <view v-for="d in draftList" :key="d.orderId" :class="['draft-item', selectedOrderId === d.orderId ? 'selected' : '']" @click="selectDraft(d)">
          <view class="draft-head">
            <text class="code">{{ d.orderCode }}</text>
            <uni-tag :text="statusLabel(d.status)" :type="statusTag(d.status)" size="small" />
          </view>
          <view class="info-line"><text class="meta">工序：{{ d.processName || '-' }}</text></view>
          <view class="info-line"><text class="meta">厂商：{{ d.vendorName || '-' }}</text></view>
          <view class="info-line"><text class="meta">总量：{{ d.issueTotalQty }}</text></view>
        </view>
      </view>
    </template>
    </template>

    <!-- 选中草稿单的发料行编辑 -->
    <template v-if="selectedOrder">
      <uni-section :title="'发料明细 - ' + (selectedOrder.processName || '')" type="line"></uni-section>
      <view class="form-card">
        <view v-if="issueLines.length === 0" class="empty-hint"><text class="text-grey">无发料行</text></view>
        <view v-for="(line, idx) in issueLines" :key="idx" class="line-item">
          <view class="line-head">
            <text class="line-name">{{ line.itemName }}</text>
            <text class="line-spec">{{ line.itemCode }}{{ line.specification ? ' · ' + line.specification : '' }}</text>
          </view>
          <view class="line-body">
            <view class="line-row" @click="openWarehousePicker(idx)">
              <text class="line-label">仓库</text>
              <view class="picker-value">
                <text :class="line.warehouseName ? 'value' : 'placeholder'">{{ line.warehouseName || '请选择' }}</text>
                <uni-icons v-if="isDraft" type="right" size="14" color="#999"></uni-icons>
              </view>
            </view>
            <view class="line-row" v-if="line.batchCode || line.warehouseName">
              <text class="line-label">批次</text>
              <text class="value">{{ line.batchCode || '-' }}</text>
            </view>
            <view class="line-row">
              <text class="line-label">数量</text>
              <input class="qty-input" type="digit" v-model="line.quantity" :disabled="!isDraft" />
              <text class="unit">{{ line.unitName }}</text>
            </view>
          </view>
        </view>
      </view>
    </template>

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

    <!-- 底部操作（非草稿单只读，不显示操作栏） -->
    <view class="footer-bar" v-if="selectedOrder && isDraft">
      <button class="cu-btn lg" style="flex:1;margin-right:16rpx" :disabled="!canExecute || submitting" @click="saveLines">保存发料行</button>
      <button class="cu-btn bg-blue lg" style="flex:1" :disabled="!canExecute || submitting" @click="execute">
        {{ submitting ? '执行中...' : '执行发料' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, getCurrentInstance } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { listOutsource, getOutsource, updateIssueLines, executeOutsource } from '@/api/mes/wm/outsource'
import { availableBatches } from '@/api/mes/wm/issue'

const { proxy } = getCurrentInstance()
const warehousePopup = ref()
const workorderCode = ref('')
const searched = ref(false)
const directMode = ref(false)
const draftList = ref([])
const selectedOrderId = ref(null)
const selectedOrder = ref(null)
const issueLines = ref([])
const batchOptions = ref([])
const activeLineIdx = ref(null)
const submitting = ref(false)

const STATUS_LABEL = { DRAFT: '草稿', ISSUED: '已发料', VENDOR_RCVD: '已签收', PROCESSING: '加工中', FINISHED: '已完成', SHIPPED: '已发货', RECEIVED: '已收货', CLOSED: '已关闭' }
const STATUS_TAG = { DRAFT: 'default', ISSUED: 'warning', VENDOR_RCVD: 'warning', PROCESSING: 'primary', FINISHED: 'primary', SHIPPED: 'warning', RECEIVED: 'success', CLOSED: 'default' }
function statusLabel(s) { return STATUS_LABEL[s] || s || '' }
function statusTag(s) { return STATUS_TAG[s] || 'default' }

// 从外协列表跳转时带 workorderCode 参数，自动查询；
// 从外协发料页"执行草稿"带 orderId 时，直接加载该草稿单
onLoad((options) => {
  if (options && options.orderId) {
    loadOrderById(options.orderId)
  } else if (options && options.workorderCode) {
    workorderCode.value = options.workorderCode
    searchDrafts()
  }
})

// 将后端发料行映射为可编辑表单行（两处复用，消除重复）
function mapIssueLines(rawLines) {
  return (rawLines || []).map(l => ({
    itemId: l.itemId, itemCode: l.itemCode, itemName: l.itemName, specification: l.specification || '',
    quantity: l.quantity, unitOfMeasure: l.unitOfMeasure || 'TON', unitName: l.unitName || '吨',
    warehouseId: l.warehouseId, warehouseCode: l.warehouseCode || '', warehouseName: l.warehouseName || '',
    batchId: l.batchId || 0, batchCode: l.batchCode || ''
  }))
}

// 提交前把表单行的 quantity（digit 输入框产出字符串）转为数字
function toNumericLines(lines) {
  return (lines || []).map(l => ({ ...l, quantity: Number(l.quantity) }))
}

// 直达：按 orderId 直接加载草稿单发料明细（不经过工单搜索）
async function loadOrderById(orderId) {
  directMode.value = true
  proxy.$modal.loading('加载发料明细...')
  selectedOrderId.value = Number(orderId)
  try {
    const res = await getOutsource(orderId)
    if (res.data.status !== 'DRAFT') {
      proxy.$modal.msgWarning('该外协单不是草稿状态，无法执行发料')
      selectedOrder.value = res.data
      issueLines.value = mapIssueLines(res.data.issueLines)
      return
    }
    selectedOrder.value = res.data
    searched.value = true
    issueLines.value = mapIssueLines(res.data.issueLines)
  } catch (e) {
    proxy.$modal.msgError('加载发料明细失败')
    selectedOrder.value = null
  } finally { proxy.$modal.closeLoading() }
}

const canExecute = computed(() => issueLines.value.length > 0
  && issueLines.value.every(l => l.warehouseId != null && Number(l.quantity) > 0))
// 非草稿单（直达模式加载到已执行的单）只读，禁止编辑/执行
const isDraft = computed(() => selectedOrder.value?.status === 'DRAFT')

function scanWorkorder() {
  // #ifdef APP-PLUS || H5
  uni.scanCode({
    onlyFromCamera: false, scanType: ['barCode', 'qrCode'],
    success: (res) => { workorderCode.value = res.result; searchDrafts() },
    fail: () => {}
  })
  // #endif
  // #ifdef MP-WEIXIN
  uni.scanCode({ success: (res) => { workorderCode.value = res.result; searchDrafts() } })
  // #endif
}

async function searchDrafts() {
  if (!workorderCode.value.trim()) { proxy.$modal.msgError('请输入工单号'); return }
  proxy.$modal.loading('查询草稿外协单...')
  selectedOrderId.value = null
  selectedOrder.value = null
  issueLines.value = []
  try {
    const res = await listOutsource({ workorderCode: workorderCode.value.trim(), status: 'DRAFT', pageSize: 100 })
    draftList.value = res.rows || []
    searched.value = true
    if (draftList.value.length === 0) {
      proxy.$modal.msg('该工单无草稿外协发料单')
    } else if (draftList.value.length === 1) {
      // 只有一张草稿单，自动选中
      await selectDraft(draftList.value[0])
    }
  } catch (e) {
    proxy.$modal.msgError('查询失败')
  } finally { proxy.$modal.closeLoading() }
}

async function selectDraft(d) {
  proxy.$modal.loading('加载发料明细...')
  selectedOrderId.value = d.orderId
  try {
    const res = await getOutsource(d.orderId)
    if (res.data.status !== 'DRAFT') {
      proxy.$modal.msgWarning('该外协单已执行，仅可查看')
    }
    selectedOrder.value = res.data
    issueLines.value = mapIssueLines(res.data.issueLines)
  } catch (e) {
    proxy.$modal.msgError('加载发料明细失败')
    selectedOrder.value = null
  } finally { proxy.$modal.closeLoading() }
}

async function openWarehousePicker(idx) {
  if (!isDraft.value) return
  activeLineIdx.value = idx
  try {
    const res = await availableBatches(issueLines.value[idx].itemId)
    batchOptions.value = res.data || []
  } catch (e) { batchOptions.value = [] }
  warehousePopup.value.open()
}

function pickWarehouse(b) {
  const line = issueLines.value[activeLineIdx.value]
  line.warehouseId = b.warehouseId; line.warehouseCode = b.warehouseCode
  line.warehouseName = b.warehouseName; line.batchId = b.batchId || 0; line.batchCode = b.batchCode || ''
  warehousePopup.value.close()
}

async function saveLines() {
  if (!canExecute.value) { proxy.$modal.msg('请补全每行仓库和数量'); return }
  submitting.value = true
  try {
    // <input type="digit"> 的 v-model 产出字符串，提交前显式转数字
    await updateIssueLines(selectedOrderId.value, toNumericLines(issueLines.value))
    proxy.$modal.msgSuccess('发料行已保存')
  } catch (e) {} finally { submitting.value = false }
}

async function execute() {
  if (!canExecute.value) { proxy.$modal.msg('请补全每行仓库和数量'); return }
  const confirm = await new Promise(resolve => {
    uni.showModal({ title: '确认执行发料', content: '将扣减库存并将单据置为「已发料」，确认？', success: r => resolve(r.confirm) })
  })
  if (!confirm) return
  submitting.value = true
  try {
    // 先保存发料行（确保修改生效），再执行扣料
    await updateIssueLines(selectedOrderId.value, toNumericLines(issueLines.value))
    await executeOutsource(selectedOrderId.value)
    proxy.$modal.msgSuccess('发料成功')
    // 从列表移除已执行的单，自动选下一张或返回搜索
    draftList.value = draftList.value.filter(d => d.orderId !== selectedOrderId.value)
    selectedOrder.value = null
    selectedOrderId.value = null
    issueLines.value = []
    if (draftList.value.length === 1) {
      await selectDraft(draftList.value[0])
    } else if (draftList.value.length === 0) {
      setTimeout(() => proxy.$tab.navigateBack(), 1500)
    }
  } catch (e) {} finally { submitting.value = false }
}
</script>

<style lang="scss" scoped>
page { background-color: #f5f6f7; min-height: 100%; padding-bottom: 160rpx; }
.container { padding: 0 0 160rpx; }
.form-card { background: #fff; margin: 16rpx 24rpx; border-radius: 16rpx; padding: 16rpx 24rpx; }
.scan-row { display: flex; align-items: center; gap: 16rpx; padding: 16rpx 0; }
.card-input { flex: 1; background: #f5f6f7; border-radius: 8rpx; padding: 12rpx 20rpx; font-size: 28rpx; }
.empty-hint { padding: 40rpx 0; text-align: center; }
.text-grey { color: #999; font-size: 26rpx; }
.draft-item { padding: 20rpx 0; border-bottom: 1px solid #f5f5f5; }
.draft-item:last-child { border-bottom: none; }
.draft-item.selected { background: #f0f7ff; border-radius: 8rpx; }
.draft-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8rpx; }
.code { font-size: 30rpx; font-weight: 600; color: #333; }
.info-line { display: flex; margin-top: 4rpx; }
.meta { font-size: 26rpx; color: #666; }
.line-item { padding: 20rpx 0; border-bottom: 1px solid #f5f5f5; }
.line-item:last-child { border-bottom: none; }
.line-head { display: flex; flex-direction: column; gap: 4rpx; margin-bottom: 12rpx; }
.line-name { font-size: 28rpx; font-weight: 600; color: #333; }
.line-spec { font-size: 24rpx; color: #999; }
.line-body { display: flex; flex-direction: column; gap: 8rpx; padding-left: 16rpx; }
.line-row { display: flex; align-items: center; gap: 16rpx; }
.line-label { color: #666; font-size: 26rpx; width: 80rpx; }
.value { color: #333; font-size: 28rpx; }
.placeholder { color: #ccc; font-size: 28rpx; }
.picker-value { display: flex; align-items: center; gap: 8rpx; flex: 1; justify-content: flex-end; }
.qty-input { flex: 1; font-size: 28rpx; text-align: right; }
.unit { font-size: 24rpx; color: #999; }
.popup-content { background: #fff; border-radius: 24rpx 24rpx 0 0; max-height: 80vh; display: flex; flex-direction: column; }
.popup-header { display: flex; justify-content: space-between; align-items: center; padding: 24rpx; border-bottom: 1px solid #f0f0f0; }
.popup-title { font-size: 30rpx; font-weight: 600; }
.popup-close { color: #999; font-size: 28rpx; }
.popup-scroll { max-height: 60vh; }
.popup-item { display: flex; justify-content: space-between; align-items: center; padding: 24rpx; border-bottom: 1px solid #f5f5f5; }
.footer-bar { position: fixed; left: 0; right: 0; bottom: 0; display: flex; padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom)); background: #fff; border-top: 1px solid #eee; }
.cu-btn { border-radius: 12rpx; border: none; }
.cu-btn.sm { font-size: 26rpx; height: 64rpx; line-height: 64rpx; padding: 0 20rpx; }
.cu-btn.lg { font-size: 30rpx; height: 88rpx; line-height: 88rpx; }
.bg-blue { background: #007aff; color: #fff; }
.cu-btn[disabled] { opacity: 0.5; }
</style>
