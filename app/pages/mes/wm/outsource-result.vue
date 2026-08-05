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

      <!-- 批量模板（仅分切来源） -->
      <uni-section v-if="isSlitting" title="批量添加（同规格）" type="line"></uni-section>
      <view class="info-card" v-if="isSlitting">
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

      <!-- 产出明细列表 -->
      <uni-section :title="'产出明细（' + resultLines.length + '）'" type="line"></uni-section>
      <view class="info-card">
        <view v-if="resultLines.length === 0" class="empty-hint">
          <text class="text-grey">请添加产出明细</text>
        </view>
        <view v-else>
          <view v-for="(line, idx) in resultLines" :key="idx" class="roll-edit-row">
            <view class="roll-edit-top">
              <text class="roll-no">产出 {{ idx + 1 }}</text>
              <uni-icons type="close" size="18" color="#f56c6c" @click="removeLine(idx)"></uni-icons>
            </view>
            <view class="roll-edit-fields">
              <view class="field" v-if="isSlitting">
                <text class="field-label">门幅(mm)</text>
                <uni-easyinput v-model="line.width" type="number" placeholder="门幅" :inputBorder="true" />
              </view>
              <view class="field" v-if="isSlitting">
                <text class="field-label">克重(g)</text>
                <uni-easyinput v-model="line.gsm" type="number" placeholder="克重" :inputBorder="true" />
              </view>
              <view class="field">
                <text class="field-label">数量({{ unitName }})</text>
                <uni-easyinput v-model="line.quantity" type="digit" placeholder="数量" :inputBorder="true" />
              </view>
            </view>
          </view>
        </view>
        <button class="cu-btn line-blue sm full-btn" @click="addLine">+ 添加一行</button>
      </view>

      <!-- 底部提交 -->
      <view class="footer-bar">
        <button class="cu-btn bg-blue lg" :disabled="!canSubmit || submitting" @click="submit">
          {{ submitting ? '提交中...' : '提交加工结果' }}
        </button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { ref, reactive, computed, getCurrentInstance } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getOutsource, recordOutsourceResult } from '@/api/mes/wm/outsource'

const { proxy } = getCurrentInstance()

const record = ref(null)
const loading = ref(true)
const submitting = ref(false)
const resultLines = ref([])
const tpl = reactive({ width: '', gsm: '', count: '1' })

const isSlitting = computed(() => record.value?.sourceType === 'SLITTING')
const firstIssue = computed(() => (record.value?.issueLines || [])[0])
const unitName = computed(() => firstIssue.value?.unitName || '吨')

const canSubmit = computed(() => {
  return resultLines.value.length > 0 && resultLines.value.every(l => l.quantity && Number(l.quantity) > 0)
})

function batchAdd() {
  if (!tpl.count) { proxy.$modal.msg('请填写条数'); return }
  const count = parseInt(tpl.count)
  if (count <= 0 || count > 50) { proxy.$modal.msg('条数需在 1~50 之间'); return }
  for (let i = 0; i < count; i++) {
    resultLines.value.push({ width: tpl.width, gsm: tpl.gsm, quantity: '' })
  }
  proxy.$modal.msgSuccess('已生成 ' + count + ' 条')
}

function addLine() {
  resultLines.value.push({ width: '', gsm: '', quantity: '' })
}

function removeLine(idx) {
  resultLines.value.splice(idx, 1)
}

async function submit() {
  if (resultLines.value.length === 0) { proxy.$modal.msg('请添加产出明细'); return }
  submitting.value = true
  try {
    // 构造收货行数组：只填 quantity + extAttrs(分切的门幅/克重)，后端自动继承物料/仓库
    const data = resultLines.value.map(l => {
      const line = { quantity: Number(l.quantity) }
      if (isSlitting.value && (l.width || l.gsm)) {
        line.extAttrs = JSON.stringify({ width: l.width || '', gsm: l.gsm || '' })
      }
      return line
    })
    await recordOutsourceResult(record.value.orderId, data)
    proxy.$modal.msgSuccess('加工结果已提交')
    setTimeout(() => proxy.$tab.navigateBack(), 1500)
  } catch (e) {} finally { submitting.value = false }
}

onLoad((options) => {
  const orderId = options.orderId
  if (!orderId) { proxy.$modal.msgError('缺少外协单ID'); return }
  getOutsource(orderId).then(res => {
    record.value = res.data
    loading.value = false
    // 若已有收货行（PROCESSING/RECEIVED），回填
    if (res.data.recptLines && res.data.recptLines.length > 0) {
      resultLines.value = res.data.recptLines.map(l => {
        let ext = {}
        try { ext = JSON.parse(l.extAttrs || '{}') } catch (e) {}
        return { width: ext.width || '', gsm: ext.gsm || '', quantity: String(l.quantity) }
      })
    }
  }).catch(() => { loading.value = false })
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
.footer-bar { position: fixed; left: 0; right: 0; bottom: 0; padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom)); background: #fff; border-top: 1px solid #eee; }
.cu-btn { border-radius: 12rpx; border: none; }
.cu-btn.sm { font-size: 26rpx; height: 64rpx; line-height: 64rpx; }
.cu-btn.lg { width: 100%; font-size: 30rpx; height: 88rpx; line-height: 88rpx; }
.bg-blue { background: #007aff; color: #fff; }
.line-blue { background: #fff; color: #007aff; border: 1px solid #007aff; }
.cu-btn[disabled] { opacity: 0.5; }
</style>
