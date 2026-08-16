<template>
  <view class="container">
    <!-- 查单区（无 salesId 直达时：扫码/输入出库单编码） -->
    <view v-if="!salesId" class="query-card">
      <view class="scan-bar">
        <view class="scan-input">
          <uni-icons type="scan" size="28" color="#409eff" @click="scanOrderCode" />
          <input
            class="code-input"
            v-model="orderCodeInput"
            placeholder="扫码或输入出库单编码（SALE 开头）"
            confirm-type="search"
            @confirm="queryByCode"
          />
        </view>
        <button class="btn-add" @click="queryByCode">查单</button>
      </view>
      <view v-if="!header.salesCode" class="empty-tip">扫描出库单编码，开始扫码出库</view>
    </view>

    <!-- 顶部出库单信息 -->
    <view v-if="header.salesId" class="header-card">
      <view class="header-row">
        <text class="code">{{ header.salesCode || '' }}</text>
        <text class="status" :class="'st-' + header.status">{{ salesStatusText(header.status) }}</text>
      </view>
      <text class="sub">{{ header.clientName || '' }} · 应出 {{ header.totalQuantity || 0 }} / 已出 {{ header.postedQuantity || 0 }} · 共 {{ (header.lines || []).length }} 行</text>
    </view>

    <!-- 状态门禁提示 -->
    <view v-if="header.salesId && !canPost(header.status)" class="block-tip">
      当前状态「{{ salesStatusText(header.status) }}」不可出库（仅草稿/部分出库可操作）
    </view>

    <!-- 待拣物料清单：点行添加，扫码场景外无需手输编码 -->
    <view v-if="header.salesId && canPost(header.status)">
      <view class="section-title">
        <text>待拣物料（点击添加）</text>
      </view>
      <view
        v-for="l in lines"
        :key="l.lineId"
        :class="['pick-line', { 'pick-done': lineRemain(l) <= 0, 'pick-in-list': inList(l.lineId) }]"
        @click="matchItem(l.itemCode)"
      >
        <view class="pl-left">
          <text class="pl-code">{{ l.itemCode }}</text>
          <text class="pl-name">{{ l.itemName }}</text>
          <text v-if="l.warehouseName" class="pl-wh">{{ l.warehouseName }}</text>
        </view>
        <text class="pl-remain">{{ lineRemain(l) <= 0 ? '已出完' : '未出 ' + lineRemain(l) + ' ' + (l.unitName || '') }}</text>
      </view>
    </view>

    <!-- 扫码/输入区（扫码枪场景；点上方清单可代替手输） -->
    <view v-if="header.salesId && canPost(header.status)" class="scan-bar">
      <view class="scan-input">
        <uni-icons type="scan" size="28" color="#409eff" @click="scanItemCode" />
        <input
          class="code-input"
          v-model="inputCode"
          placeholder="扫物料条码自动识别，或点上方物料行添加"
          confirm-type="search"
          @confirm="onInputConfirm"
        />
      </view>
      <button class="btn-add" @click="onInputConfirm">添加</button>
      <button class="btn-scan" @click="scanItemCode">扫码</button>
    </view>

    <!-- 本次出库清单 -->
    <view v-if="header.salesId && canPost(header.status)" class="section-title">
      <text>本次出库清单（{{ outList.length }} 项）</text>
      <text v-if="outList.length" class="clear-btn" @click="outList = []">清空</text>
    </view>

    <view v-for="(d, idx) in outList" :key="idx" class="out-card">
      <view class="card-top">
        <text class="item-code">{{ d.itemCode }}</text>
        <text class="del-btn" @click="outList.splice(idx, 1)">删除</text>
      </view>
      <text class="item-name">{{ d.itemName }}</text>
      <view class="card-mid">
        <text class="line-info">出库行：{{ d.quantitySales }} {{ d.unitName }}（未出 {{ d.remain }}）{{ d.warehouseName ? ' · ' + d.warehouseName : '' }}</text>
      </view>
      <view class="qty-row">
        <text class="qty-label">本次出库：</text>
        <uni-number-box v-model="d.quantity" :min="0" :max="d.remain" :step="1" />
        <text class="qty-unit">{{ d.unitName }}</text>
      </view>
      <view class="batch-row">
        <text class="batch-label">批次：</text>
        <view class="batch-pick" @click="openBatchPicker(idx)">
          <text :class="d._batchDisplay ? 'batch-val' : 'batch-ph'">{{ d._batchDisplay || 'FIFO 自动分配（点选指定批次）' }}</text>
        </view>
      </view>
    </view>
    <view v-if="header.salesId && canPost(header.status) && outList.length === 0" class="empty-tip">请扫码添加物料</view>

    <!-- 底部提交 -->
    <view v-if="header.salesId && canPost(header.status)" class="action-bar">
      <button class="btn-submit" :disabled="!canSubmit" @click="handleSubmit">提交出库</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, getCurrentInstance } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getSalesDetail, getSalesDetailByCode, postSalesOut, availableBatches } from '@/api/mes/wm/sales'
import { salesStatusText, canPost, lineRemain } from '@/utils/wm-sales.js'
// 显式引入 uni-ui 组件（绕过 HBuilderX 发行 H5 时 easycom 失效）
import UniIcons from '@/uni_modules/uni-icons/components/uni-icons/uni-icons.vue'
import UniNumberBox from '@/uni_modules/uni-number-box/components/uni-number-box/uni-number-box.vue'

const { proxy } = getCurrentInstance()
const salesId = ref(null)
const header = ref({})
const lines = ref([])
const outList = ref([])  // 本次出库清单
const inputCode = ref('')      // 手动输入的物料编码
const orderCodeInput = ref('') // 手动输入的出库单编码

const canSubmit = computed(() => outList.value.some(d => d.quantity > 0))

onLoad((options) => {
  if (options?.salesId) {
    salesId.value = options.salesId
    loadData()
  }
  // 无 salesId：停留在查单区，扫/输单编码后进入拣货
})

async function loadData() {
  try {
    const res = await getSalesDetail(salesId.value)
    header.value = res.data || {}
    lines.value = header.value.lines || []
  } catch (e) {}
}

// ── 查单（无参直达）──
function queryByCode() {
  const code = (orderCodeInput.value || '').trim()
  if (!code) { proxy.$modal.msgError('请输入出库单编码'); return }
  resolveOrder(code)
}

function scanOrderCode() {
  uni.scanCode({
    onlyFromCamera: false,
    scanType: ['barCode', 'qrCode'],
    success: (res) => resolveOrder(res.result),
    fail: () => {}
  })
}

function resolveOrder(code) {
  if (!code) return
  uni.showLoading({ title: '查单中...' })
  getSalesDetailByCode(code.trim()).then(res => {
    uni.hideLoading()
    const d = res.data
    if (!d) { proxy.$modal.msgError('出库单不存在：' + code); return }
    header.value = d
    lines.value = d.lines || []
    salesId.value = d.salesId
    outList.value = []
  }).catch(() => { uni.hideLoading() })
}

// ── 扫物料码拣货 ──
function onInputConfirm() {
  const code = (inputCode.value || '').trim()
  if (!code) { proxy.$modal.msgError('请输入物料编码'); return }
  matchItem(code)
  inputCode.value = ''
}

function scanItemCode() {
  uni.scanCode({
    onlyFromCamera: false,
    scanType: ['barCode', 'qrCode'],
    success: (res) => matchItem(res.result),
    fail: () => { proxy.$modal.msgError('扫码取消或失败') }
  })
}

/** 出库行是否已加入清单 */
const inList = (lineId) => outList.value.some(d => d.lineId === lineId)

// 扫码后匹配出库行（按物料编码；同物料多行时自动匹配下一个未出完的行）
function matchItem(code) {
  const matches = (l) => l.itemCode === code || code.includes(l.itemCode) || l.itemCode.includes(code)
  // 优先：未在清单中的行
  let matched = lines.value.find(l => matches(l) && !inList(l.lineId))
  if (!matched) {
    const anyMatch = lines.value.find(l => matches(l))
    if (anyMatch && inList(anyMatch.lineId)) {
      proxy.$modal.msgSuccess('已在清单中：' + anyMatch.itemCode)
    } else {
      proxy.$modal.msgError('未匹配到出库行：' + code)
    }
    return
  }
  const remain = lineRemain(matched)
  if (remain <= 0) {
    proxy.$modal.msgError('该物料已出库完成：' + matched.itemCode)
    return
  }
  outList.value.push({
    lineId: matched.lineId,
    itemId: matched.itemId,
    itemCode: matched.itemCode,
    itemName: matched.itemName,
    unitOfMeasure: matched.unitOfMeasure,
    unitName: matched.unitName,
    quantitySales: matched.quantitySales,
    warehouseName: matched.warehouseName,
    remain,
    quantity: remain,  // 默认出库全部未出量
    batchId: null,     // null=FIFO 自动分配；选批次=精确批次扣减
    batchCode: '',
    warehouseId: matched.warehouseId,
    _batchDisplay: '',
    _batchOptions: []
  })
  loadBatchOptions(outList.value.length - 1)
  proxy.$modal.msgSuccess('已添加：' + matched.itemName)
}

// 加载可选批次列表：不传 warehouseId，返回该物料所有仓所有批次
async function loadBatchOptions(idx) {
  const d = outList.value[idx]
  if (!d?.itemId) return
  try {
    const res = await availableBatches(d.itemId, null)
    if (outList.value[idx]) outList.value[idx]._batchOptions = res.data || []
  } catch (e) {
    if (outList.value[idx]) outList.value[idx]._batchOptions = []
  }
}

// 打开批次选择（action sheet）：第 0 项 FIFO 自动分配
function openBatchPicker(idx) {
  const d = outList.value[idx]
  if (!d) return
  const opts = d._batchOptions || []
  if (!opts.length) {
    proxy.$modal.msgError('该物料暂无可用批次，保持 FIFO 自动分配')
    return
  }
  const items = [
    'FIFO 自动分配（系统按先进先出扣批次）',
    ...opts.map(b => {
      const bc = b.batchCode || '无批次'
      const wh = b.warehouseName || `仓${b.warehouseId}`
      return `${bc} · ${wh} (可用${b.quantityAvailable}/${b.quantityOnhand})`
    })
  ]
  uni.showActionSheet({
    itemList: items,
    success: (r) => {
      const row = outList.value[idx]
      if (!row) return
      if (r.tapIndex === 0) {
        // FIFO：batchId 置空，后端按 create_time 升序跨批次扣减
        row.batchId = null
        row.batchCode = ''
        row.warehouseId = lines.value.find(l => l.lineId === row.lineId)?.warehouseId || null
        row._batchDisplay = ''
      } else {
        const b = opts[r.tapIndex - 1]
        row.batchId = b.batchId
        // batchCode 存原始值（可能为空字符串），"无批次" 只用于展示
        row.batchCode = b.batchCode || ''
        if (b.warehouseId) row.warehouseId = b.warehouseId
        const batchLabel = b.batchCode || '无批次'
        row._batchDisplay = `${batchLabel} · ${b.warehouseName || ('仓' + b.warehouseId)}`
      }
      // 强制触发数组更新（uni-app H5 端响应式兜底）
      outList.value.splice(idx, 1, { ...row })
    },
    fail: (err) => {
      console.log('[batch picker] cancelled or failed:', err)
    }
  })
}

async function handleSubmit() {
  const valid = outList.value.filter(d => d.quantity > 0)
  if (!valid.length) { proxy.$modal.msgError('请输入有效的出库数量'); return }
  // 校验不超过未出库量
  for (const d of valid) {
    if (d.quantity > d.remain) {
      proxy.$modal.msgError(`${d.itemCode} 出库量(${d.quantity})超过未出库量(${d.remain})`)
      return
    }
  }
  uni.showModal({
    title: '确认出库', content: `共 ${valid.length} 项物料，确认出库并扣减库存？`,
    success: async (r) => {
      if (!r.confirm) return
      try {
        // 组装 details（与后端 WmProductSalesDetail 字段对齐）
        const details = valid.map(d => ({
          lineId: d.lineId, itemId: d.itemId, itemCode: d.itemCode, itemName: d.itemName,
          unitOfMeasure: d.unitOfMeasure, unitName: d.unitName,
          quantity: d.quantity,
          batchId: d.batchId ?? null,     // null=FIFO 自动分配；真实 id=精确批次扣减
          batchCode: d.batchCode || null,
          warehouseId: d.warehouseId || null
        }))
        await postSalesOut(salesId.value, details)
        proxy.$modal.msgSuccess('出库成功')
        setTimeout(() => uni.navigateBack(), 1000)
      } catch (e) {}
    }
  })
}
</script>

<style lang="scss" scoped>
.container { min-height: 100vh; background: #f5f6f7; padding-bottom: 140rpx; }
.query-card {
  background: #fff; margin: 16rpx; border-radius: 12rpx; padding: 16rpx;
}
.header-card {
  background: #fff; padding: 24rpx; margin: 16rpx; border-radius: 12rpx;
  .header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8rpx;
    .code { font-size: 32rpx; font-weight: bold; color: #303133; }
    .status { font-size: 24rpx; padding: 4rpx 16rpx; border-radius: 20rpx; }
  }
  .sub { font-size: 24rpx; color: #909399; }
}
.block-tip {
  margin: 16rpx; padding: 24rpx; border-radius: 12rpx;
  background: #fdf6ec; color: #e6a23c; font-size: 26rpx;
}
.scan-bar {
  display: flex; gap: 16rpx; padding: 0 16rpx; margin-bottom: 16rpx;
  .scan-input {
    flex: 1; background: #fff; border-radius: 8rpx; padding: 0 20rpx;
    display: flex; align-items: center; gap: 12rpx; height: 80rpx;
  }
  .code-input {
    flex: 1; font-size: 28rpx; color: #303133; height: 80rpx;
  }
  .btn-add, .btn-scan {
    border-radius: 8rpx; font-size: 28rpx; padding: 0 28rpx; border: none; line-height: 80rpx;
    &::after { border: none; }
  }
  .btn-add { background: #67c23a; color: #fff; }
  .btn-scan { background: #409eff; color: #fff; }
}
.section-title {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16rpx 24rpx; font-size: 28rpx; font-weight: bold; color: #303133;
  .clear-btn { font-size: 24rpx; color: #f56c6c; font-weight: normal; }
}
.pick-line {
  display: flex; justify-content: space-between; align-items: center;
  background: #fff; border-radius: 12rpx; padding: 20rpx 24rpx; margin: 0 16rpx 12rpx;
  .pl-left { display: flex; flex-direction: column; gap: 4rpx; flex: 1; min-width: 0;
    .pl-code { font-size: 28rpx; font-weight: bold; color: #303133; }
    .pl-name { font-size: 24rpx; color: #606266; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .pl-wh { font-size: 22rpx; color: #909399; }
  }
  .pl-remain { font-size: 26rpx; color: #e6a23c; flex-shrink: 0; padding-left: 16rpx; }
  &.pick-done { opacity: 0.5;
    .pl-remain { color: #909399; }
  }
  &.pick-in-list { border: 2rpx solid #409eff; }
}
.out-card {
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
  .batch-pick {
    flex: 1; background: #f5f6f7; border-radius: 8rpx; padding: 12rpx 20rpx; min-height: 56rpx;
    display: flex; align-items: center;
    .batch-val { font-size: 26rpx; color: #303133; }
    .batch-ph { font-size: 24rpx; color: #909399; }
  }
}
.st-DRAFT { background: #f4f4f5; color: #909399; }
.st-PARTIAL_POSTED { background: #fdf6ec; color: #e6a23c; }
.st-POSTED { background: #ecf5ff; color: #409eff; }
.st-SHIPPED { background: #f0f9eb; color: #67c23a; }
.st-CLOSED { background: #f4f4f5; color: #909399; }
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
