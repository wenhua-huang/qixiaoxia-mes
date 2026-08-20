<template>
  <view class="create-page">
    <view class="form-card">
      <view class="form-row">
        <text class="label">检验类型</text>
        <view class="pick" @click="pickType">{{ ipqcTypeText(form.ipqcType) || '请选择' }}</view>
      </view>
      <view class="form-row">
        <text class="label">流转卡</text>
        <uni-easyinput v-model="cardCodeInput" placeholder="扫码或输入流转卡号" :inputBorder="false" @confirm="onScanCard" />
        <uni-icons type="scan" size="24" color="#409eff" @click="scanCard" />
      </view>
      <view v-if="card" class="card-info">
        <view class="ci-row"><text class="muted">工单</text><text>{{ card.workorderCode }}</text></view>
        <view class="ci-row"><text class="muted">产品</text><text>{{ card.itemName }}</text></view>
        <view class="ci-row"><text class="muted">当前工序</text><text>{{ card.currentProcessName }}</text></view>
      </view>
      <view class="form-row" v-if="ipqcTemplates.length">
        <text class="label">检验模板</text>
        <picker :range="ipqcTemplateNames" :value="tplIndex" @change="(e) => form.templateId = ipqcTemplates[e.detail.value].templateId">
          <view class="pick">{{ tplName || '请选择模板' }}</view>
        </picker>
      </view>
    </view>
    <view style="height: 160rpx"></view>
    <view class="footer-bar">
      <button type="primary" class="btn-submit" :disabled="submitting" @click="submit">{{ submitting ? '提交中…' : '确认建单' }}</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, getCurrentInstance } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import uniIcons from '@/uni_modules/uni-icons/components/uni-icons/uni-icons.vue'
import uniEasyinput from '@/uni_modules/uni-easyinput/components/uni-easyinput/uni-easyinput.vue'
import { getCardScanResult } from '@/api/mes/pro/procard'
import { listTemplate, addIpqc } from '@/api/mes/qc'
import { ipqcTypeText, IPQC_TYPE_MAP } from '@/utils/qc'
import { parseQrPayload } from '@/utils/qrPayload'

const { proxy } = getCurrentInstance()
const cardCodeInput = ref('')
const card = ref(null)
const templates = ref([])
const form = ref({ ipqcType: 'TOUR_CHECK', templateId: null })
const submitting = ref(false)

const ipqcTypes = Object.keys(IPQC_TYPE_MAP)
const ipqcTypeNames = computed(() => ipqcTypes.map(ipqcTypeText))
function pickType() {
  uni.showActionSheet({ itemList: ipqcTypeNames.value, success: (res) => { form.value.ipqcType = ipqcTypes[res.tapIndex] } })
}

const ipqcTemplates = computed(() =>
  templates.value.filter(t => (t.qcTypes || '').split(',').map(s => s.trim()).includes('IPQC')))
const ipqcTemplateNames = computed(() => ipqcTemplates.value.map(t => t.templateName))
const tplIndex = computed(() => ipqcTemplates.value.findIndex(t => t.templateId === form.value.templateId))
const tplName = computed(() => { const t = ipqcTemplates.value[tplIndex.value]; return t ? t.templateName : '' })

onLoad((opt) => {
  listTemplate({ enableFlag: '1', pageNum: 1, pageSize: 500 }).then(r => {
    templates.value = r.rows || []
    const hasIpqcTpl = templates.value.some(t =>
      (t.qcTypes || '').split(',').map(s => s.trim()).includes('IPQC'))
    if (!hasIpqcTpl) proxy.$modal.msgWarning('未配置启用的 IPQC 检验模板，请到 PC 端维护后再建单')
  }).catch(() => proxy.$modal.msgError('检验模板加载失败'))
  if (opt.cardCode) { cardCodeInput.value = decodeURIComponent(opt.cardCode); onScanCard() }
})

function scanCard() {
  // #ifdef H5
  uni.navigateTo({ url: '/pages/mes/pro/scan?callback=1', events: { scanResult: (c) => resolveCard(c) } })
  // #endif
  // #ifndef H5
  uni.scanCode({ success: (res) => resolveCard(res.result), fail: () => {} })
  // #endif
}
function resolveCard(raw) {
  let code = (raw || '').trim()
  const p = parseQrPayload(code)
  if (p && p.code) code = p.code
  cardCodeInput.value = code
  onScanCard()
}
function onScanCard() {
  const code = cardCodeInput.value.trim()
  if (!code) return
  uni.showLoading({ title: '查卡中…' })
  getCardScanResult(code).then(res => {
    uni.hideLoading()
    const d = res.data
    if (!d || !d.card) { proxy.$modal.msgError('流转卡不存在或不可用：' + (d?.reason || code)); return }
    card.value = d.card
    // 自动选中第一个可报工任务的工序/工位
    const task = (d.reportableTasks || [])[0]
    form.value.workorderId = card.value.workorderId
    form.value.itemId = card.value.itemId
    form.value.cardId = card.value.cardId
    form.value.processId = task?.processId || card.value.currentProcessId
    form.value.workstationId = task?.workstationId || null
  }).catch(() => { uni.hideLoading(); proxy.$modal.msgError('查卡失败') })
}

function submit() {
  if (!card.value) { proxy.$modal.msgWarning('请先扫描流转卡'); return }
  if (!form.value.templateId) { proxy.$modal.msgWarning('请选择检验模板'); return }
  submitting.value = true
  const body = {
    ipqcType: form.value.ipqcType,
    workorderId: form.value.workorderId,
    workorderCode: card.value.workorderCode,
    workorderName: card.value.workorderName,
    cardId: form.value.cardId,
    cardCode: card.value.cardCode,
    processId: form.value.processId,
    processName: card.value.currentProcessName,
    workstationId: form.value.workstationId,
    itemId: form.value.itemId,
    itemCode: card.value.itemCode,
    itemName: card.value.itemName,
    specification: card.value.specification,
    unitOfMeasure: card.value.unitOfMeasure,
    templateId: form.value.templateId,
    quantityCheck: 1
  }
  addIpqc(body).then(res => {
    proxy.$modal.msgSuccess('建单成功')
    const newId = res.data
    setTimeout(() => proxy.$tab.redirectTo(`/pages/mes/qc/inspect?type=IPQC&id=${newId}`), 800)
  }).catch((e) => { proxy.$modal.msgError(e?.msg || '建单失败') })
    .finally(() => { submitting.value = false })
}
</script>

<style lang="scss" scoped>
page { background: #f5f6f7; }
.create-page { padding: 20rpx 24rpx; }
.form-card { background: #fff; border-radius: 12rpx; padding: 8rpx 24rpx; }
.form-row { display: flex; align-items: center; gap: 16rpx; padding: 24rpx 0; border-bottom: 2rpx solid #f5f5f5; }
.label { font-size: 28rpx; color: #606266; min-width: 140rpx; }
.pick { flex: 1; font-size: 28rpx; color: #303133; }
.card-info { background: #f8f9fa; border-radius: 8rpx; padding: 16rpx 20rpx; margin: 16rpx 0; }
.ci-row { display: flex; justify-content: space-between; font-size: 26rpx; padding: 6rpx 0; }
.muted { color: #909399; }
.footer-bar { position: fixed; left: 0; right: 0; bottom: 0; background: #fff; padding: 16rpx 24rpx; box-shadow: 0 -2rpx 12rpx rgba(0,0,0,.06); }
.btn-submit { border-radius: 44rpx; font-size: 30rpx; }
</style>
