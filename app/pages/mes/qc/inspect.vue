<template>
  <view class="inspect-page">
    <!-- 单据头 -->
    <view class="header-card">
      <view class="h-row">
        <text class="h-code">{{ form.iqcCode || form.ipqcCode }}</text>
        <uni-tag :type="qcStatusTagType(form.status)" :text="qcStatusText(form.status)" size="small" />
      </view>
      <view class="h-row sub" v-if="type === 'IPQC'">
        <text>{{ form.itemName }}</text>
        <text class="muted">{{ form.processName }} · {{ ipqcTypeText(form.ipqcType) }}</text>
      </view>
      <view class="h-row sub" v-else>
        <text>{{ form.itemName }}</text>
        <text class="muted">{{ form.vendorName }}</text>
      </view>
      <view class="h-row qty-row">
        <text class="muted">本次检测数量</text>
        <uni-number-box v-if="!readonly" v-model="form.quantityCheck" :min="1" :step="1" />
        <text v-else>{{ form.quantityCheck }}</text>
      </view>
    </view>

    <!-- 判定结果横幅 -->
    <view v-if="form.checkResult" class="result-banner" :class="form.checkResult">
      <uni-icons :type="bannerIcon" size="22" color="#fff" />
      <text>{{ qcResultText(form.checkResult) }}</text>
      <text v-if="form.checkResult === 'FAIL'" class="rb-sub">下游业务将被拦截</text>
      <text v-if="form.checkResult === 'CONCESSION'" class="rb-sub">让步接收</text>
    </view>

    <view v-if="loadError" class="error-box">检验单加载失败：{{ loadError }}</view>

    <!-- 检测项 -->
    <view v-if="lines.length" class="section-title">检测项（{{ lines.length }}）</view>
    <view v-for="(line, idx) in lines" :id="'qc-line-' + idx" :key="line.lineId || idx">
      <qc-line-card
        :line="line" :readonly="readonly" :required="true" :show-error="showLineError"
        @uploading-change="onUploadingChange"
      />
    </view>

    <!-- 缺陷记录 -->
    <qc-defect-editor
      v-model="defectRecords" :readonly="readonly" :defect-options="defectOptions"
      @uploading-change="onUploadingChange"
    />

    <!-- 让步理由（COMPLETED 后回显） -->
    <view v-if="form.concessionReason" class="concession-box">
      <text class="muted">让步理由：</text>
      <text>{{ form.concessionReason }}</text>
    </view>

    <view style="height: 180rpx"></view>

    <!-- 底部操作栏 -->
    <view v-if="!readonly" class="footer-bar">
      <button class="btn-save" @click="save" :disabled="submitting || uploading">暂存</button>
      <button class="btn-judge" type="primary" @click="onJudge" :disabled="submitting || uploading">
        {{ uploading ? '图片上传中…' : (submitting ? '提交中…' : '提交判定') }}
      </button>
    </view>

    <!-- 判定确认弹窗（FAIL 时让步） -->
    <uni-popup ref="judgePopup" type="center" v-if="predictResult">
      <view class="judge-dialog">
        <view class="jd-title">预判结果</view>
        <uni-tag :type="predictResult === 'PASS' ? 'success' : 'error'"
          :text="predictResult === 'PASS' ? 'PASS 合格' : 'FAIL 不合格'" size="default" />
        <view class="jd-reason" v-for="(r, i) in predictReasons" :key="i">{{ r }}</view>
        <view v-if="predictResult === 'FAIL'" class="jd-concession">
          <text class="muted">让步理由（填了则升级让步接收）</text>
          <uni-easyinput v-model="concessionInput" type="textarea" :maxlength="200" placeholder="如选择让步接收则必填" />
        </view>
        <view class="jd-actions">
          <button v-if="predictResult === 'FAIL'" class="btn-fail" @click="doJudge(false)">按不合格提交</button>
          <button v-if="predictResult === 'FAIL'" class="btn-concession" :disabled="!concessionInput.trim()" @click="doJudge(true)">让步接收</button>
          <button v-else type="primary" @click="doJudge(false)">确认判定</button>
          <button class="btn-cancel" @click="closeDialog">取消</button>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script setup>
import { ref, computed, getCurrentInstance } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import uniTag from '@/uni_modules/uni-tag/components/uni-tag/uni-tag.vue'
import uniIcons from '@/uni_modules/uni-icons/components/uni-icons/uni-icons.vue'
import uniNumberBox from '@/uni_modules/uni-number-box/components/uni-number-box/uni-number-box.vue'
import uniEasyinput from '@/uni_modules/uni-easyinput/components/uni-easyinput/uni-easyinput.vue'
import uniPopup from '@/uni_modules/uni-popup/components/uni-popup/uni-popup.vue'
import qcLineCard from './components/qc-line-card.vue'
import qcDefectEditor from './components/qc-defect-editor.vue'
import { getIqc, updateIqc, judgeIqc, getIpqc, updateIpqc, judgeIpqc, listDefect } from '@/api/mes/qc'
import { qcStatusText, qcStatusTagType, qcResultText, ipqcTypeText, predictOrder } from '@/utils/qc'

const { proxy } = getCurrentInstance()
const type = ref('IPQC')
const form = ref({})
const lines = ref([])
const defectRecords = ref([])
const defectOptions = ref([])
const submitting = ref(false)
const uploadingCount = ref(0)
const uploading = computed(() => uploadingCount.value > 0)
const showLineError = ref(false)
const loadError = ref('')
const judgePopup = ref(null)
const predictResult = ref(null)
const predictReasons = ref([])
const concessionInput = ref('')

const readonly = computed(() => form.value.status === 'COMPLETED' || form.value.status === 'CLOSED')
const bannerIcon = computed(() => {
  if (form.value.checkResult === 'PASS') return 'checkmarkempty'
  if (form.value.checkResult === 'CONCESSION') return 'info'
  return 'closeempty'
})
// 未选择缺陷的空行（误加）不参与预判/提交，否则默认 MAJOR qty=1 会误判
const validDefects = computed(() => defectRecords.value.filter(r => r.defectId))

// 提交前校验：有拍照但没选缺陷名称的行不能静默丢弃（否则图片丢失），必须提示补全
function validateDefects() {
  const incomplete = defectRecords.value.find(r => !r.defectId && r.defectImage)
  if (incomplete) {
    proxy.$modal.msgWarning('有缺陷已拍照但未选择缺陷名称，请补全后再提交')
    return false
  }
  return true
}

onLoad((opt) => {
  type.value = opt.type || 'IPQC'
  loadDetail(opt.id)
  loadDefects()
})

function loadDetail(id) {
  loadError.value = ''
  const api = type.value === 'IPQC' ? getIpqc : getIqc
  uni.showLoading({ title: '加载中…' })
  api(id).then(res => {
    const d = res.data || {}
    form.value = d
    lines.value = (d.lines || []).map(l => ({ ...l,
      crQuantity: l.crQuantity || 0, majQuantity: l.majQuantity || 0, minQuantity: l.minQuantity || 0 }))
    defectRecords.value = (d.defectRecords || []).map(r => ({ ...r }))
  }).catch((e) => {
    form.value = {}
    loadError.value = e?.msg || '检验单不存在或已被删除'
  }).finally(() => uni.hideLoading())
}
function loadDefects() {
  listDefect({ indexType: type.value, enableFlag: '1', pageNum: 1, pageSize: 500 })
    .then(res => { defectOptions.value = res.rows || [] }).catch(() => {})
}
function onUploadingChange(busy) {
  uploadingCount.value = Math.max(0, uploadingCount.value + (busy ? 1 : -1))
}

function buildBody() {
  // 整单提交（lines/defectRecords 全量，edit 全删全插）
  const head = type.value === 'IPQC'
    ? { ipqcId: form.value.ipqcId, ipqcCode: form.value.ipqcCode, quantityCheck: form.value.quantityCheck,
        workorderId: form.value.workorderId, cardId: form.value.cardId, processId: form.value.processId,
        itemId: form.value.itemId, templateId: form.value.templateId, status: form.value.status }
    : { iqcId: form.value.iqcId, iqcCode: form.value.iqcCode, quantityCheck: form.value.quantityCheck,
        sourceDocId: form.value.sourceDocId, sourceDocType: form.value.sourceDocType,
        itemId: form.value.itemId, templateId: form.value.templateId, status: form.value.status }
  return { ...head, lines: lines.value, defectRecords: validDefects.value }
}

function save() {
  if (uploading.value) { proxy.$modal.msgWarning('图片仍在上传，请稍候'); return }
  if (!form.value.quantityCheck || form.value.quantityCheck < 1) {
    proxy.$modal.msgWarning('请填写本次检测数量'); return
  }
  if (!validateDefects()) return
  submitting.value = true
  const api = type.value === 'IPQC' ? updateIpqc : updateIqc
  api(buildBody()).then(() => {
    proxy.$modal.msgSuccess('已保存')
  }).catch((e) => {
    proxy.$modal.msgError(e?.msg || '保存失败')
  }).finally(() => { submitting.value = false })
}

function onJudge() {
  showLineError.value = false
  if (!form.value.quantityCheck || form.value.quantityCheck < 1) {
    proxy.$modal.msgWarning('请填写本次检测数量'); return
  }
  if (!validateDefects()) return
  const pred = predictOrder({
    lines: lines.value,
    defects: validDefects.value,
    quantityCheck: form.value.quantityCheck,
    acQuantity: form.value.quantityMaxUnqualified,
    crRateLimit: form.value.crRateLimit,
    majRateLimit: form.value.majRateLimit,
    minRateLimit: form.value.minRateLimit
  })
  if (pred.unentered) {
    showLineError.value = true
    proxy.$modal.msgError(`检测项[${pred.unentered.indexName}]未录入结果`)
    const idx = lines.value.indexOf(pred.unentered)
    if (idx >= 0) {
      uni.createSelectorQuery().select('#qc-line-' + idx).boundingClientRect()
        .selectViewport().scrollOffset().exec((rects) => {
          const r = rects && rects[0], v = rects && rects[1]
          if (r && v) uni.pageScrollTo({ scrollTop: r.top + v.scrollTop - 20, duration: 200 })
        })
    }
    return
  }
  predictResult.value = pred.result
  predictReasons.value = pred.reasons
  concessionInput.value = ''
  judgePopup.value.open()
}

function closeDialog() {
  judgePopup.value && judgePopup.value.close()
  predictResult.value = null
}

function doJudge(concession) {
  if (concession && !concessionInput.value.trim()) {
    proxy.$modal.msgWarning('请填写让步理由'); return
  }
  judgePopup.value.close()
  proxy.$modal.confirm('确认提交判定？判定后不可修改。').then(() => {
    submitting.value = true
    const api = type.value === 'IPQC' ? updateIpqc : updateIqc
    const judgeApi = type.value === 'IPQC' ? judgeIpqc : judgeIqc
    const id = form.value.ipqcId || form.value.iqcId
    // 先保存最新录入，再单次 judge（让步理由随本次提交）
    return api(buildBody()).then(() => judgeApi(id, concession ? concessionInput.value.trim() : null))
  }).then(() => {
    predictResult.value = null
    proxy.$modal.msgSuccess('判定完成')
    return loadDetail(form.value.iqcId || form.value.ipqcId)
  }).catch((e) => {
    if (e === 'cancel' || e === false || e?.cancel) return
    proxy.$modal.msgError(e?.msg || '判定失败')
  }).finally(() => { submitting.value = false })
}
</script>

<style lang="scss" scoped>
page { background: #f5f6f7; }
.inspect-page { padding: 20rpx 24rpx; }
.header-card { background: #fff; border-radius: 12rpx; padding: 24rpx; margin-bottom: 20rpx; }
.h-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.h-code { font-size: 32rpx; font-weight: 600; }
.sub { font-size: 28rpx; color: #303133; }
.muted { color: #909399; font-size: 26rpx; }
.qty-row { border-top: 2rpx solid #f0f0f0; padding-top: 16rpx; margin-top: 8rpx; }
.result-banner { display: flex; align-items: center; gap: 12rpx; padding: 20rpx 24rpx; border-radius: 12rpx; color: #fff; margin-bottom: 20rpx; font-size: 30rpx; font-weight: 600;
  &.PASS { background: #67c23a; }
  &.FAIL { background: #f56c6c; }
  &.CONCESSION { background: #e6a23c; }
  .rb-sub { font-size: 24rpx; font-weight: normal; margin-left: auto; } }
.section-title { font-size: 28rpx; color: #606266; margin: 12rpx 0 16rpx; }
.error-box { background: #fef0f0; color: #f56c6c; padding: 24rpx; border-radius: 12rpx; font-size: 28rpx; margin-bottom: 20rpx; }
.concession-box { background: #fdf6ec; padding: 20rpx; border-radius: 8rpx; font-size: 26rpx; margin-top: 16rpx; }
.footer-bar { position: fixed; left: 0; right: 0; bottom: 0; background: #fff; padding: 16rpx 24rpx; display: flex; gap: 20rpx; box-shadow: 0 -2rpx 12rpx rgba(0,0,0,.06); }
.btn-save { flex: 1; background: #f4f4f5; color: #606266; font-size: 30rpx; border-radius: 44rpx; }
.btn-judge { flex: 2; font-size: 30rpx; border-radius: 44rpx; }
.judge-dialog { width: 620rpx; background: #fff; border-radius: 16rpx; padding: 32rpx; }
.jd-title { font-size: 32rpx; font-weight: 600; margin-bottom: 20rpx; }
.jd-reason { font-size: 26rpx; color: #606266; margin-top: 12rpx; }
.jd-concession { margin-top: 24rpx; }
.jd-actions { display: flex; flex-wrap: wrap; gap: 16rpx; margin-top: 28rpx; }
.jd-actions button { flex: 1; min-width: 240rpx; font-size: 28rpx; border-radius: 40rpx; }
.btn-fail { background: #fef0f0; color: #f56c6c; }
.btn-concession { background: #fdf6ec; color: #e6a23c; }
.btn-cancel { background: #f4f4f5; color: #909399; }
</style>
