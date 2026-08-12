<template>
  <view class="container">
    <!-- 流转卡报工标识条（扫码进入时显示） -->
    <view class="card-banner" v-if="card">
      <uni-tag text="流转卡报工" type="primary" size="small" />
      <text class="card-banner-code">{{ card.cardCode }} · {{ card.currentProcessName }}</text>
    </view>

    <!-- 步骤 1：扫码/输入工单编码 -->
    <view class="section">
      <uni-section title="生产工单" type="line">
        <template #right>
          <text class="scan-tip">扫码或输入工单号</text>
        </template>
      </uni-section>
      <view class="search-row">
        <uni-easyinput
          v-model="workorderCode"
          placeholder="输入工单号或扫码"
          :inputBorder="false"
          class="search-input"
          @confirm="searchWorkorder"
        />
        <button class="scan-btn" @click="handleScan" size="mini">
          <uni-icons type="scan" size="20"></uni-icons>
        </button>
        <button class="search-btn cu-btn bg-blue sm" @click="searchWorkorder">查询</button>
      </view>
    </view>

    <!-- 步骤 2：选择工序任务 -->
    <view v-if="workorder" class="section">
      <uni-section title="工单详情" type="line"></uni-section>
      <view class="order-info">
        <view class="info-row">
          <text class="label">工单编码</text>
          <text class="value bold">{{ workorder.workorderCode }}</text>
        </view>
        <view class="info-row">
          <text class="label">工单名称</text>
          <text class="value">{{ workorder.workorderName }}</text>
        </view>
        <view class="info-row">
          <text class="label">产品</text>
          <text class="value">{{ workorder.productName || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="label">状态</text>
          <uni-tag :type="woStatusTagType(workorder.status)" :text="woStatusText(workorder.status)" size="small" />
        </view>
      </view>

      <view class="line-header">
        <text class="bold">选择工序（任务）</text>
      </view>
      <view v-for="(task, idx) in taskList" :key="idx"
        class="task-item" :class="{ active: selectedTaskId === task.taskId }"
        @click="selectTask(task)">
        <view class="task-top">
          <text class="task-name">{{ task.processName || '工序' }}</text>
          <view class="task-tags">
            <uni-tag v-if="(task.pendingFeedbackCount || 0) > 0" type="warning" :text="'待审核' + task.pendingFeedbackCount" size="small" />
            <uni-tag v-if="selectedTaskId === task.taskId" type="primary" text="已选" size="small" />
          </view>
        </view>
        <view class="task-sub">
          <text class="text-grey">工作站：{{ task.workstationName || '-' }} · 进度：{{ task.quantityProduced || 0 }}/{{ task.quantity || 0 }} {{ workorder.unitName || 'PCS' }}</text>
        </view>
      </view>
      <view v-if="taskList.length === 0" class="empty-tip">该工单暂无可报工的工序任务</view>

      <!-- 外协工序任务（展示用，不可点击报工，厂商手机端录结果） -->
      <view v-if="outsourceTaskList.length > 0" class="completed-section">
        <view class="line-header">
          <text class="bold completed-title">外协工序</text>
        </view>
        <view v-for="(task, idx) in outsourceTaskList" :key="'co-' + idx" class="task-item completed">
          <view class="task-top">
            <text class="task-name">{{ task.processName || '工序' }}</text>
            <uni-tag :type="taskStatusTagType(task.status)" :text="taskStatusLabel(task.status)" size="small" />
          </view>
          <view class="task-sub">
            <text class="text-grey">厂商：{{ task.workstationName || '-' }} · 进度：{{ task.quantityProduced || 0 }}/{{ task.quantity || 0 }} {{ workorder.unitName || 'PCS' }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 步骤 3：填报工数量 -->
    <view v-if="selectedTask" class="section">
      <uni-section title="报工数量" type="line"></uni-section>
      <view class="form-box">
        <view class="qty-row">
          <text class="qty-label"><text class="required">*</text>合格数</text>
          <view class="qty-input">
            <uni-number-box v-model="form.quantityQualified" :min="0" :step="1" />
            <text class="unit">{{ workorder.unitName || 'PCS' }}</text>
          </view>
        </view>
        <view class="qty-row">
          <text class="qty-label">不合格数</text>
          <view class="qty-input">
            <uni-number-box v-model="form.quantityUnqualified" :min="0" :step="1" />
            <text class="unit">{{ workorder.unitName || 'PCS' }}</text>
          </view>
        </view>
        <view class="qty-row">
          <text class="qty-label">工废</text>
          <view class="qty-input">
            <uni-number-box v-model="form.quantityLaborScrap" :min="0" :step="1" />
            <text class="unit">{{ workorder.unitName || 'PCS' }}</text>
          </view>
        </view>
        <view class="qty-row">
          <text class="qty-label">料废</text>
          <view class="qty-input">
            <uni-number-box v-model="form.quantityMaterialScrap" :min="0" :step="1" />
            <text class="unit">{{ workorder.unitName || 'PCS' }}</text>
          </view>
        </view>
        <view class="qty-row total-row">
          <text class="qty-label">本次报工</text>
          <text class="total-val">{{ totalQuantity }} {{ workorder.unitName || 'PCS' }}</text>
        </view>
      </view>
    </view>

    <!-- 步骤 3.5：工序参数填报 -->
    <view v-if="selectedTask && paramList.length > 0" class="section">
      <uni-section title="工序参数" type="line"></uni-section>
      <view class="form-box">
        <view v-for="(p, idx) in paramList" :key="idx" class="param-row">
          <view class="param-head">
            <view class="param-title">
              <text class="param-name">{{ p.paramName }}</text>
              <text v-if="p.unit" class="param-unit">{{ p.unit }}</text>
            </view>
            <!-- 标准图样缩略图，点击放大对照 -->
            <image v-if="p.imageUrl" class="param-thumb"
              :src="fullImgUrl(p.imageUrl.split(',')[0])" mode="aspectFill"
              @click="previewParamImage(p)" />
          </view>
          <text v-if="formatRange(p)" class="param-range">标准范围：{{ formatRange(p) }}</text>
          <view class="param-input">
            <uni-easyinput v-model="p.actualValue" placeholder="输入实际值" :inputBorder="false" class="param-easyinput" />
            <uni-tag v-if="p.isDeviation === 'Y'" type="error" text="偏差" size="mini" />
            <uni-tag v-else-if="p.isDeviation === 'N'" type="success" text="正常" size="mini" />
          </view>
        </view>
      </view>
    </view>

    <!-- 步骤 4：备注 -->
    <view v-if="selectedTask" class="section">
      <uni-section title="备注（选填）" type="line"></uni-section>
      <view class="form-box">
        <uni-easyinput
          type="textarea"
          v-model="form.remark"
          placeholder="可填写异常说明等"
          :maxlength="200"
        />
      </view>
    </view>

    <!-- 底部提交 -->
    <view v-if="selectedTask" class="footer-bar">
      <button type="primary" class="confirm-btn" @click="submitReport" :disabled="submitting">
        {{ submitting ? '提交中...' : '提交报工' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, watch, getCurrentInstance } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { addFeedback, getFeedbackEntry } from '@/api/mes/pro/feedback'
import { getCardScanResult } from '@/api/mes/pro/procard'
import { parseQrPayload } from '@/utils/qrPayload'
import { listParamTemplateByProcessId } from '@/api/mes/pro/paramtemplate'
import config from '@/config.js'

const { proxy } = getCurrentInstance()
const workorderCode = ref('')
const workorder = ref(null)
const taskList = ref([])
const outsourceTaskList = ref([])
const selectedTaskId = ref(null)
const selectedTask = ref(null)
const submitting = ref(false)

const form = reactive({
  quantityQualified: 0,
  quantityUnqualified: 0,
  quantityLaborScrap: 0,
  quantityMaterialScrap: 0,
  remark: ''
})
const paramList = ref([])
const card = ref(null)                       // 扫码得到的流转卡
const reportedQualifiedSum = ref(0)          // 该卡该工序已报合格数

// 本次报工总数 = 合格 + 不合格 + 工废 + 料废
const totalQuantity = computed(() => {
  return Number(form.quantityQualified || 0)
    + Number(form.quantityUnqualified || 0)
    + Number(form.quantityLaborScrap || 0)
    + Number(form.quantityMaterialScrap || 0)
})

const WO_STATUS_MAP = {
  PREPARE: '待生产', PRODUCING: '生产中', COMPLETED: '已完成', CANCEL: '已取消', CLOSED: '已关闭'
}
function woStatusText(s) { return WO_STATUS_MAP[s] || s || '' }
function woStatusTagType(s) {
  const m = { COMPLETED: 'success', CANCEL: 'error', CLOSED: 'info', PRODUCING: 'warning' }
  return m[s] || 'default'
}

// 外协任务状态标签（覆盖外协流转状态：PRODUCING/COMPLETED 等）
function taskStatusLabel(s) {
  const m = { PREPARE: '待外发', PRODUCING: '外协中', COMPLETED: '已完工', CANCEL: '已取消' }
  return m[s] || s || ''
}
function taskStatusTagType(s) {
  const m = { COMPLETED: 'success', PRODUCING: 'warning', CANCEL: 'error', PREPARE: 'default' }
  return m[s] || 'default'
}

// 页面入口：扫码分发（Task 5）带入 cardCode / workorderCode / rawCode
onLoad((options) => {
  if (!options) return
  if (options.cardCode) {
    scanByCard(options.cardCode)
  } else if (options.workorderCode) {
    workorderCode.value = options.workorderCode
    searchWorkorder()
  } else if (options.rawCode) {
    // 裸码兜底：像卡号就当卡，否则当工单
    const parsed = parseQrPayload(options.rawCode)
    if (parsed && parsed.type === 'CARD') {
      scanByCard(parsed.code)
    } else {
      workorderCode.value = options.rawCode
      searchWorkorder()
    }
  }
})

// 不可报工原因文案
function reasonText(reason) {
  const map = {
    CARD_NOT_FOUND: '未找到该流转卡',
    CARD_COMPLETED: '该流转卡已完工，无需报工',
    CARD_OUTSOURCING: '该流转卡外协中，请到外协收货页操作',
    CARD_SCRAPPED: '该流转卡已报废',
    NO_REPORTABLE_TASK: '当前工序无可报工任务'
  }
  return map[reason] || '当前不可报工'
}

// 扫流转卡码 → 后端反查报工上下文 → 单任务自动选中直进填数量步（4 步压成 2 步）
function scanByCard(cardCode) {
  proxy.$modal.loading('查询流转卡...')
  getCardScanResult(cardCode).then(res => {
    proxy.$modal.closeLoading()
    const data = res.data || {}
    if (!data.card) {
      proxy.$modal.msgError('未找到流转卡：' + cardCode)
      return
    }
    if (!data.canReport) {
      proxy.$modal.alert(reasonText(data.reason), '无法报工')
      return
    }
    // 重置已选状态（与 searchWorkorder 一致）
    workorder.value = null
    taskList.value = []
    outsourceTaskList.value = []
    selectedTaskId.value = null
    selectedTask.value = null
    paramList.value = []
    Object.assign(form, { quantityQualified: 0, quantityUnqualified: 0, quantityLaborScrap: 0, quantityMaterialScrap: 0, remark: '' })

    card.value = data.card
    // 用卡的冗余字段填充 workorder 供 doSubmit 复用（字段名与 doSubmit body 一致）
    workorder.value = {
      workorderId: data.card.workorderId,
      workorderCode: data.card.workorderCode,
      workorderName: data.card.workorderName,
      productId: data.card.itemId,
      productCode: data.card.itemCode,
      productName: data.card.itemName,
      unitOfMeasure: data.card.unitOfMeasure,
      unitName: data.card.unitName
    }
    taskList.value = data.reportableTasks || []
    outsourceTaskList.value = []
    reportedQualifiedSum.value = data.reportedQualifiedSum || 0
    // 单任务自动选中 → 直接进填数量步
    if (taskList.value.length === 1) {
      selectTask(taskList.value[0])
    }
  }).catch(() => {
    proxy.$modal.closeLoading()
    proxy.$modal.msgError('查询流转卡失败')
  })
}

// 扫码
function handleScan() {
  // #ifdef APP-PLUS || H5
  uni.scanCode({
    onlyFromCamera: false,
    scanType: ['barCode', 'qrCode'],
    success: (res) => {
      workorderCode.value = res.result
      searchWorkorder()
    },
    fail: (err) => { console.log('扫码取消:', err) }
  })
  // #endif
  // #ifdef MP-WEIXIN
  uni.scanCode({
    success: (res) => {
      workorderCode.value = res.result
      searchWorkorder()
    }
  })
  // #endif
}

// 查询工单 + 可报工任务（一次请求）
function searchWorkorder() {
  if (!workorderCode.value.trim()) {
    proxy.$modal.msgError('请输入工单号')
    return
  }
  proxy.$modal.loading('查询中...')
  // 重置已选状态
  workorder.value = null
  taskList.value = []
  outsourceTaskList.value = []
  selectedTaskId.value = null
  selectedTask.value = null
  paramList.value = []
  Object.assign(form, { quantityQualified: 0, quantityUnqualified: 0, quantityLaborScrap: 0, quantityMaterialScrap: 0, remark: '' })

  getFeedbackEntry(workorderCode.value.trim()).then(res => {
    proxy.$modal.closeLoading()
    const data = res.data || {}
    if (!data.workorder) {
      proxy.$modal.msgError(res.msg || '未找到该工单')
      return
    }
    workorder.value = data.workorder
    taskList.value = data.tasks || []
    outsourceTaskList.value = data.outsourceTasks || []
    if (taskList.value.length === 0 && outsourceTaskList.value.length > 0) {
      proxy.$modal.msg('外协工序请在厂商端录结果，无需厂内报工')
    } else if (taskList.value.length === 0) {
      proxy.$modal.msgError('该工单暂无可报工的工序任务')
    }
  }).catch((err) => {
    proxy.$modal.closeLoading()
    // request.js 已全局 toast 显示后端错误信息，这里不再覆盖
    if (!err) proxy.$modal.msgError('查询失败，请检查工单号')
  })
}

// 选择工序任务
function selectTask(task) {
  selectedTaskId.value = task.taskId
  selectedTask.value = task
  // 加载该工序报工可见的参数模板
  paramList.value = []
  if (task.processId) {
    loadParams(task.processId)
  }
}

// 加载工序参数模板（只加载报工可见 isReportVisible=Y 且启用 enableFlag=1 的）
function loadParams(processId) {
  listParamTemplateByProcessId(processId).then(res => {
    paramList.value = (res.data || [])
      .filter(t => t.isReportVisible === 'Y' && t.enableFlag === '1')
      .map(t => ({
        templateId: t.templateId,
        workorderParamId: null,
        paramName: t.paramName,
        unit: t.unit || '',
        minValue: t.minValue,
        maxValue: t.maxValue,
        imageUrl: t.imageUrl,
        actualValue: '',
        isDeviation: null
      }))
  }).catch(() => { paramList.value = [] })
}

// 图片相对路径 → 完整 URL（拼接 baseUrl）
function fullImgUrl(rel) {
  if (!rel) return ''
  if (/^https?:/.test(rel)) return rel
  return config.baseUrl + rel
}

// 点击缩略图全屏预览（支持多图逗号分隔）
function previewParamImage(p) {
  const urls = (p.imageUrl || '').split(',').filter(Boolean).map(fullImgUrl)
  if (urls.length === 0) return
  uni.previewImage({ current: urls[0], urls })
}

// 标准范围格式化
function formatRange(p) {
  if (p.minValue != null && p.maxValue != null) return `${p.minValue} ~ ${p.maxValue}`
  if (p.minValue != null) return `≥ ${p.minValue}`
  if (p.maxValue != null) return `≤ ${p.maxValue}`
  return ''
}

// 实际值变化时本地预判偏差（提交前给工人即时反馈，最终以后端判定为准）
watch(() => paramList.value.map(p => p.actualValue).join(','), () => {
  paramList.value.forEach(p => {
    if (!p.actualValue || (p.minValue == null && p.maxValue == null)) {
      p.isDeviation = null; return
    }
    const v = Number(p.actualValue)
    if (isNaN(v)) { p.isDeviation = null; return }
    p.isDeviation = ((p.minValue != null && v < p.minValue) || (p.maxValue != null && v > p.maxValue)) ? 'Y' : 'N'
  })
})

// 提交报工
function submitReport() {
  if (totalQuantity.value <= 0) {
    proxy.$modal.msgError('请至少填写一项报工数量')
    return
  }
  doSubmit()
}

function doSubmit() {
  const pending = selectedTask.value?.pendingFeedbackCount || 0
  // 第一步：有待审核报工时先提醒
  const pendingConfirm = pending > 0
    ? proxy.$modal.confirm('该工序已有 ' + pending + ' 条待审核报工，是否继续提交？', '重复报工提醒')
    : Promise.resolve()
  pendingConfirm.then(() => {
    // 第二步：正常提交确认
    return proxy.$modal.confirm('确认提交报工？合格' + form.quantityQualified + '，不合格' + form.quantityUnqualified + '。')
  }).then(() => {
    submitting.value = true
    const t = selectedTask.value
    const w = workorder.value
    const body = {
      feedbackType: 'INTERNAL',
      feedbackCode: '',
      taskId: t.taskId,
      taskCode: t.taskCode,
      workorderId: w.workorderId,
      workorderCode: w.workorderCode,
      workorderName: w.workorderName,
      processId: t.processId,
      processName: t.processName,
      workstationId: t.workstationId,
      workstationName: t.workstationName,
      itemId: w.productId || t.itemId,
      itemCode: w.productCode || t.itemCode,
      itemName: w.productName || t.itemName,
      unitOfMeasure: w.unitOfMeasure || t.unitOfMeasure,
      unitName: w.unitName || t.unitName,
      routeId: t.routeId,
      quantityFeedback: totalQuantity.value,
      quantityQualified: Number(form.quantityQualified || 0),
      quantityUnqualified: Number(form.quantityUnqualified || 0),
      quantityLaborScrap: Number(form.quantityLaborScrap || 0),
      quantityMaterialScrap: Number(form.quantityMaterialScrap || 0),
      quantityUncheck: 0,
      quantityOtherScrap: 0,
      feedbackChannel: 'APP',
      feedbackTime: null,
      userName: null,
      remark: form.remark || null,
      status: 'PREPARE',
      paramList: paramList.value
        .filter(p => p.actualValue !== null && p.actualValue !== '')
        .map(p => ({
          templateId: p.templateId,
          workorderParamId: p.workorderParamId,
          actualValue: p.actualValue
        }))
    }
    return addFeedback(body)
  }).then(() => {
    proxy.$modal.msgSuccess('报工提交成功！')
    setTimeout(() => { proxy.$tab.navigateBack() }, 1500)
  }).catch(e => {
    if (e === false || e === undefined || e === 'cancel' || e?.cancel) return // 用户取消，不提示
    const errMsg = typeof e === 'string' ? e : (e?.msg || e?.message || '未知错误')
    proxy.$modal.alert(errMsg, '报工失败')
  }).finally(() => {
    submitting.value = false
  })
}
</script>

<style lang="scss" scoped>
page { background-color: #f5f6f7; min-height: 100%; }

.container { padding-bottom: 120rpx; }

.card-banner {
  display: flex; align-items: center;
  gap: 16rpx;
  padding: 16rpx 24rpx;
  background: #ecf5ff;
  border-radius: 12rpx;
  margin: 20rpx 24rpx 0;
}
.card-banner-code { font-size: 28rpx; color: #303133; }

.section {
  margin: 20rpx 0;
  background: #fff;
}

.scan-tip { color: #999; font-size: 24rpx; }

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
.task-item {
  padding: 24rpx;
  border-bottom: 1px solid #f5f5f5;
  border-left: 6rpx solid transparent;
}
.task-item.active {
  background: #ecf5ff;
  border-left-color: #409eff;
}
.task-top {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 8rpx;
}
.task-tags {
  display: flex; align-items: center; gap: 8rpx;
}
.task-name { font-size: 30rpx; font-weight: 600; color: #333; }
.task-sub { margin-top: 4rpx; }
.text-grey { color: #999; font-size: 24rpx; }
.completed-section { margin-top: 8rpx; }
.completed-title { color: #67c23a; }
.task-item.completed { opacity: 0.75; background: #f0f9eb; border-left-color: #67c23a; }
.empty-tip {
  text-align: center; padding: 40rpx;
  color: #999; font-size: 26rpx;
}

.form-box { padding: 16rpx 24rpx 24rpx; }

.param-row {
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}
.param-row:last-child { border-bottom: none; }
.param-head {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 8rpx;
}
.param-title { display: flex; align-items: baseline; gap: 12rpx; }
.param-name { font-size: 28rpx; color: #333; font-weight: 600; }
.param-unit { font-size: 24rpx; color: #999; }
.param-thumb {
  width: 64rpx; height: 64rpx; border-radius: 8rpx;
  border: 1rpx solid #e5e5e5; background: #f9f9f9;
}
.param-range { font-size: 24rpx; color: #e6a23c; display: block; margin-bottom: 12rpx; }
.param-input {
  display: flex; align-items: center; gap: 16rpx;
}
.param-easyinput { flex: 1; }

.qty-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16rpx 0;
  border-bottom: 1px solid #f5f5f5;
}
.qty-row:last-child { border-bottom: none; }
.qty-label { font-size: 28rpx; color: #333; }
.required { color: #f56c6c; margin-right: 4rpx; }
.qty-input {
  display: flex; align-items: center; gap: 12rpx;
}
.unit { color: #666; font-size: 24rpx; }
.total-row {
  padding-top: 24rpx;
  border-top: 2rpx solid #e5e5e5;
}
.total-val { font-size: 32rpx; font-weight: 700; color: #409eff; }

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
