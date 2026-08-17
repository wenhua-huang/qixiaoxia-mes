<template>
  <view class="container">
    <view v-if="loading" class="loading-box">
      <uni-load-more status="loading" />
    </view>

    <template v-else-if="record">
      <!-- 发料信息（只读） -->
      <uni-section title="发料信息" type="line"></uni-section>
      <view class="info-card">
        <view class="info-row">
          <text class="label">外协单号</text>
          <text class="value bold">{{ record.orderCode }}</text>
        </view>
        <view class="info-row" v-for="(line, idx) in (record.issueLines || [])" :key="idx">
          <text class="label">发料物料{{ (record.issueLines || []).length > 1 ? (idx+1) : '' }}</text>
          <text class="value">{{ line.itemName }} · {{ line.quantity }}{{ line.unitName }}</text>
        </view>
        <view class="info-row" v-if="firstIssue">
          <text class="label">发料仓库</text>
          <text class="value">{{ firstIssue.warehouseName }}</text>
        </view>
      </view>

      <!-- 加工中：已录进度 + 历史明细只读展示（不可重复提交） -->
      <template v-if="record.status === 'PROCESSING' || record.status === 'FINISHED'">
        <uni-section title="加工进度" type="line"></uni-section>
        <view class="info-card">
          <view class="info-row">
            <text class="label">已录 / 应录</text>
            <text class="value bold">{{ record.recptTotalQty || 0 }} / {{ record.issueTotalQty || 0 }} {{ unitName }}</text>
          </view>
        </view>
        <uni-section title="已录明细" type="line" v-if="(record.recptLines || []).length > 0"></uni-section>
        <view class="info-card" v-if="(record.recptLines || []).length > 0">
          <view v-for="(line, idx) in record.recptLines" :key="line.lineId || idx" class="info-row">
            <text class="label">产出 {{ idx + 1 }}</text>
            <text class="value">{{ formatRecptLine(line) }}</text>
          </view>
        </view>
      </template>

      <!-- 批量模板（仅分切来源，可录入时显示） -->
      <uni-section v-if="isSlitting && canAddResult" title="批量添加（同规格）" type="line"></uni-section>
      <view class="info-card" v-if="isSlitting && canAddResult">
        <view class="template-row">
          <view class="template-item">
            <text class="label">门幅(mm)</text>
            <uni-easyinput v-model="tpl.width" type="number" placeholder="如400" :inputBorder="true" />
          </view>
          <view class="template-item">
            <text class="label">克重(g)</text>
            <uni-easyinput v-model="tpl.gsm" type="number" placeholder="如80" :inputBorder="true" />
          </view>
          <view class="template-item">
            <text class="label">条数</text>
            <uni-easyinput v-model="tpl.count" type="number" placeholder="如2" :inputBorder="true" />
          </view>
        </view>
        <button class="cu-btn bg-blue sm full-btn" @click="batchAdd">生成</button>
      </view>

      <!-- 产出明细列表（本次新录入） -->
      <uni-section :title="(record.status === 'PROCESSING' ? '本次补录（' : '产出明细（') + resultLines.length + '）'" type="line"></uni-section>
      <view class="info-card">
        <view v-if="resultLines.length === 0 && !canAddResult" class="empty-hint">
          <text class="text-grey">已录满，可发货回厂</text>
        </view>
        <view v-else-if="resultLines.length === 0" class="empty-hint">
          <text class="text-grey">请添加产出明细</text>
        </view>
        <view v-else>
          <view v-for="(line, idx) in resultLines" :key="idx" class="roll-edit-row">
            <view class="roll-edit-top">
              <text class="roll-no">产出 {{ idx + 1 }}</text>
              <uni-icons v-if="canAddResult" type="close" size="18" color="#f56c6c" @click="removeLine(idx)"></uni-icons>
            </view>
            <view class="roll-edit-fields">
              <view class="field" v-if="isSlitting">
                <text class="field-label">门幅(mm)</text>
                <uni-easyinput v-model="line.width" type="number" placeholder="门幅" :inputBorder="true" :disabled="!canAddResult" />
              </view>
              <view class="field" v-if="isSlitting">
                <text class="field-label">克重(g)</text>
                <uni-easyinput v-model="line.gsm" type="number" placeholder="克重" :inputBorder="true" :disabled="!canAddResult" />
              </view>
              <view class="field">
                <text class="field-label">数量({{ unitName }})</text>
                <uni-easyinput v-model="line.quantity" type="digit" placeholder="数量" :inputBorder="true" :disabled="!canAddResult" />
              </view>
            </view>
            <!-- 批次属性（可选，不填后端用收货日期兜底） -->
            <view class="batch-toggle" v-if="canAddResult" @click="line.showBatch = !line.showBatch">
              <text class="batch-toggle-text">{{ line.showBatch ? '收起批次属性 ▲' : '展开批次属性（选填） ▼' }}</text>
            </view>
            <view class="roll-edit-fields" v-if="canAddResult && line.showBatch">
              <view class="field">
                <text class="field-label">生产批号</text>
                <uni-easyinput v-model="line.lotNumber" placeholder="批号" :inputBorder="true" />
              </view>
              <view class="field">
                <text class="field-label">生产日期</text>
                <UniDatetimePicker type="date" v-model="line.produceDate">
                  <view class="date-picker-box" :class="{ 'is-placeholder': !line.produceDate }">{{ line.produceDate || '请选择' }}</view>
                </UniDatetimePicker>
              </view>
              <view class="field">
                <text class="field-label">有效期</text>
                <UniDatetimePicker type="date" v-model="line.expireDate">
                  <view class="date-picker-box" :class="{ 'is-placeholder': !line.expireDate }">{{ line.expireDate || '请选择' }}</view>
                </UniDatetimePicker>
              </view>
            </view>
          </view>
        </view>
        <button v-if="canAddResult" class="cu-btn line-blue sm full-btn" @click="addLine">+ 添加一行</button>
      </view>

      <!-- 超量警告 -->
      <view v-if="overReceipt" class="over-warn">
        <text class="warn-text">⚠ 本次录入累计量将超过发料量，提交将被拒绝</text>
      </view>

      <!-- 底部操作（可录入/可完成/可发货时显示） -->
      <view class="footer-bar" v-if="canAddResult || record.status === 'FINISHED'">
        <button v-if="canAddResult" class="cu-btn bg-blue lg" style="flex:1;margin-right:16rpx" :disabled="!canSubmit || submitting" @click="submit">
          {{ submitting ? '提交中...' : (record.status === 'PROCESSING' ? '提交补录' : '提交加工结果') }}
        </button>
        <button v-if="record.status === 'PROCESSING'" class="cu-btn bg-cyan lg" style="flex:1;margin-right:16rpx" :disabled="submitting" @click="complete">
          手动完成
        </button>
        <button v-if="record.status === 'FINISHED'" class="cu-btn bg-green lg" style="flex:1" :disabled="submitting" @click="ship">
          发货回厂
        </button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { ref, reactive, computed, getCurrentInstance } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { getOutsource, recordOutsourceResult, completeOutsource, shipOutsource } from '@/api/mes/wm/outsource'
import UniDatetimePicker from '@/uni_modules/uni-datetime-picker/components/uni-datetime-picker/uni-datetime-picker.vue'

const { proxy } = getCurrentInstance()

const record = ref(null)
const loading = ref(true)
const submitting = ref(false)
const resultLines = ref([])
const tpl = reactive({ width: '', gsm: '', count: '1' })

const isSlitting = computed(() => record.value?.sourceType === 'SLITTING')
const firstIssue = computed(() => (record.value?.issueLines || [])[0])
const unitName = computed(() => firstIssue.value?.unitName || '吨')
// 仅在已签收/加工中可录入；已发料需先签收；已完成/已发货/已收货不可再追加
const canAddResult = computed(() => {
  const s = record.value?.status
  return s === 'VENDOR_RCVD' || s === 'PROCESSING'
})

const canSubmit = computed(() => {
  if (resultLines.value.length === 0) return false
  if (overReceipt.value) return false // 超量时禁用提交（后端硬拒绝，前端提前拦截）
  return resultLines.value.every(l => {
    if (!l.quantity || isNaN(Number(l.quantity)) || Number(l.quantity) <= 0) return false
    // 分切每行必须有有效的门幅/克重
    if (isSlitting.value) {
      if (!l.width || isNaN(Number(l.width)) || Number(l.width) <= 0) return false
      if (!l.gsm || isNaN(Number(l.gsm)) || Number(l.gsm) <= 0) return false
    }
    return true
  })
})

// 本次录入量 + 已录量 超过发料量时警告（后端会硬拒绝，前端提前提示）
const overReceipt = computed(() => {
  if (!record.value || resultLines.value.length === 0) return false
  const issueQty = Number(record.value.issueTotalQty) || 0
  const already = Number(record.value.recptTotalQty) || 0
  const adding = resultLines.value.reduce((sum, l) => sum + (Number(l.quantity) || 0), 0)
  return already + adding > issueQty
})

// 已录明细只读展示文本（分切带门幅/克重）
function formatRecptLine(line) {
  let ext = {}
  try { ext = JSON.parse(line.extAttrs || '{}') } catch (e) {}
  const parts = []
  if (ext.width) parts.push(ext.width + 'mm')
  if (ext.gsm) parts.push(ext.gsm + 'g')
  parts.push(line.quantity + (line.unitName || unitName.value))
  return parts.join(' · ')
}

function batchAdd() {
  if (!tpl.count) { proxy.$modal.msg('请填写条数'); return }
  const count = parseInt(tpl.count)
  if (isNaN(count) || count <= 0 || count > 50) { proxy.$modal.msg('条数需在 1~50 之间'); return }
  // 分切场景校验门幅/克重为有效数字
  if (isSlitting.value) {
    if (!tpl.width || isNaN(Number(tpl.width)) || Number(tpl.width) <= 0) {
      proxy.$modal.msg('请填写有效的门幅(mm)'); return
    }
    if (!tpl.gsm || isNaN(Number(tpl.gsm)) || Number(tpl.gsm) <= 0) {
      proxy.$modal.msg('请填写有效的克重(g)'); return
    }
  }
  for (let i = 0; i < count; i++) {
    resultLines.value.push({ width: tpl.width, gsm: tpl.gsm, quantity: '', lotNumber: '', produceDate: '', expireDate: '', showBatch: false })
  }
  proxy.$modal.msgSuccess('已生成 ' + count + ' 条')
}

function addLine() {
  resultLines.value.push({ width: '', gsm: '', quantity: '', lotNumber: '', produceDate: '', expireDate: '', showBatch: false })
}

function removeLine(idx) {
  resultLines.value.splice(idx, 1)
}

async function submit() {
  if (!canAddResult.value) { proxy.$modal.msgError('当前状态不可录入'); return }
  if (resultLines.value.length === 0) { proxy.$modal.msg('请添加产出明细'); return }
  submitting.value = true
  try {
    // 构造收货行数组：quantity + extAttrs(分切门幅/克重) + 批次属性(选填)，后端自动继承物料/仓库
    const data = resultLines.value.map(l => {
      const line = { quantity: Number(l.quantity) }
      if (isSlitting.value && (l.width || l.gsm)) {
        line.extAttrs = JSON.stringify({ width: Number(l.width) || 0, gsm: Number(l.gsm) || 0 })
      }
      if (l.lotNumber) line.lotNumber = l.lotNumber
      if (l.produceDate) line.produceDate = l.produceDate
      if (l.expireDate) line.expireDate = l.expireDate
      return line
    })
    const res = await recordOutsourceResult(record.value.orderId, data)
    // 用返回值刷新状态：可能自动 FINISHED，也可能仍 PROCESSING 等待补录
    record.value = res.data || record.value
    resultLines.value = []
    tpl.width = ''; tpl.gsm = ''; tpl.count = '1'
    if (record.value.status === 'FINISHED') {
      proxy.$modal.msgSuccess('已录满，加工自动完成，可发货回厂')
    } else {
      proxy.$modal.msgSuccess('加工结果已提交，可继续补录或手动完成')
    }
  } catch (e) {} finally { submitting.value = false }
}

// 厂商手动完成（PROCESSING → FINISHED，允许短交，录不满计划量时用）
async function complete() {
  proxy.$modal.confirm('确认完成加工？完成后可发货回厂。').then(async () => {
    submitting.value = true
    try {
      const res = await completeOutsource(record.value.orderId)
      record.value = res.data || record.value
      proxy.$modal.msgSuccess('已完成，可发货')
    } catch (e) {} finally { submitting.value = false }
  }).catch(() => {})
}

// 厂商发货（FINISHED → SHIPPED，发回工厂）
async function ship() {
  const confirmed = await new Promise(resolve => {
    uni.showModal({
      title: '确认发货',
      content: '确认已将加工完成的物料发回工厂？发货后不可修改。',
      success: r => resolve(r.confirm)
    })
  })
  if (!confirmed) return
  submitting.value = true
  try {
    await shipOutsource(record.value.orderId)
    proxy.$modal.msgSuccess('已发货')
    setTimeout(() => proxy.$tab.navigateBack(), 1500)
  } catch (e) {} finally { submitting.value = false }
}

onLoad((options) => {
  const orderId = options.orderId
  if (!orderId) { proxy.$modal.msgError('缺少外协单ID'); return }
  getOutsource(orderId).then(res => {
    record.value = res.data
    loading.value = false
    // 已有收货行（PROCESSING/FINISHED）只读展示在"已录明细"中；
    // 不回填到可编辑表单，避免厂商误把旧明细重复提交
  }).catch(() => { loading.value = false })
})

onShow(() => {
  // 防御性清理：上个页面残留的全局 loading 会遮住本页 toast
  uni.hideLoading()
})
</script>

<style lang="scss" scoped>
page { background-color: #f5f6f7; min-height: 100%; padding-bottom: 200rpx; }
.container { padding: 0 0 200rpx; }
.loading-box { display: flex; justify-content: center; padding: 80rpx 0; }
.info-card { background: #fff; margin: 16rpx 24rpx; border-radius: 16rpx; padding: 8rpx 24rpx; }
.info-row { display: flex; justify-content: space-between; align-items: center; padding: 20rpx 0; border-bottom: 1px solid #f5f5f5; }
.info-row:last-child { border-bottom: none; }
.label { color: #999; font-size: 26rpx; }
.value { font-size: 28rpx; color: #333; }
.bold { font-weight: 600; }
.template-row { display: flex; gap: 16rpx; padding: 16rpx 0; }
.template-item { flex: 1; display: flex; flex-direction: column; gap: 8rpx; }
.full-btn { width: 100%; margin: 16rpx 0; }
.empty-hint { padding: 40rpx 0; text-align: center; }
.text-grey { color: #999; font-size: 26rpx; }
.roll-edit-row { padding: 20rpx 0; border-bottom: 1px solid #f5f5f5; }
.roll-edit-top { display: flex; justify-content: space-between; align-items: center; padding-bottom: 12rpx; }
.roll-no { font-size: 28rpx; font-weight: 600; color: #333; }
.roll-edit-fields { display: flex; gap: 12rpx; }
.field { flex: 1; display: flex; flex-direction: column; gap: 6rpx; }
.field-label { font-size: 22rpx; color: #999; }
.date-picker-box {
  height: 70rpx; line-height: 70rpx;
  border: 1px solid #e5e5e5; border-radius: 8rpx;
  padding: 0 16rpx; font-size: 26rpx; color: #333;
  background: #fff; box-sizing: border-box;
}
.date-picker-box.is-placeholder { color: #c0c4cc; }
.batch-toggle { padding: 10rpx 0; }
.batch-toggle-text { font-size: 24rpx; color: #007aff; }
.footer-bar { position: fixed; left: 0; right: 0; bottom: 0; padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom)); background: #fff; border-top: 1px solid #eee; }
.cu-btn { border-radius: 12rpx; border: none; }
.cu-btn.sm { font-size: 26rpx; height: 64rpx; line-height: 64rpx; }
.cu-btn.lg { width: 100%; font-size: 30rpx; height: 88rpx; line-height: 88rpx; }
.bg-blue { background: #007aff; color: #fff; }
.line-blue { background: #fff; color: #007aff; border: 1px solid #007aff; }
.over-warn { padding: 16rpx 24rpx; }
.warn-text { font-size: 24rpx; color: #f56c6c; }
.cu-btn[disabled] { opacity: 0.5; }
</style>
