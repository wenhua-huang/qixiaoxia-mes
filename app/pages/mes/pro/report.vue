<template>
  <view class="container">
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
          <uni-tag v-if="selectedTaskId === task.taskId" type="primary" text="已选" size="small" />
        </view>
        <view class="task-sub">
          <text class="text-grey">工作站：{{ task.workstationName || '-' }}</text>
        </view>
      </view>
      <view v-if="taskList.length === 0" class="empty-tip">该工单暂无可报工的工序任务</view>
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
import { ref, reactive, computed, getCurrentInstance } from 'vue'
import { addFeedback, getFeedbackEntry } from '@/api/mes/pro/feedback'

const { proxy } = getCurrentInstance()
const workorderCode = ref('')
const workorder = ref(null)
const taskList = ref([])
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
  selectedTaskId.value = null
  selectedTask.value = null
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
    if (taskList.value.length === 0) {
      proxy.$modal.msgError('该工单暂无可报工的工序任务')
    }
  }).catch(() => {
    proxy.$modal.closeLoading()
    proxy.$modal.msgError('查询失败，请检查工单号')
  })
}

// 选择工序任务
function selectTask(task) {
  selectedTaskId.value = task.taskId
  selectedTask.value = task
}

// 提交报工
function submitReport() {
  if (totalQuantity.value <= 0) {
    proxy.$modal.msgError('请至少填写一项报工数量')
    return
  }
  proxy.$modal.confirm('确认提交报工？合格' + form.quantityQualified + '，不合格' + form.quantityUnqualified + '。').then(() => {
    submitting.value = true
    const t = selectedTask.value
    const w = workorder.value
    const body = {
      feedbackType: 'INTERNAL',
      feedbackCode: 'FB' + Date.now(),
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
      feedbackChannel: 'PAD',
      feedbackTime: null,  // 后端 autoFillCodes 不自动填，这里留 null 让后端处理
      userName: null,
      remark: form.remark || null,
      status: 'PREPARE'
    }
    addFeedback(body).then(() => {
      proxy.$modal.msgSuccess('报工提交成功！')
      setTimeout(() => { proxy.$tab.navigateBack() }, 1500)
    }).catch(e => {
      proxy.$modal.msgError('报工失败：' + (typeof e === 'string' ? e : (e.msg || e.message || '未知错误')))
    }).finally(() => {
      submitting.value = false
    })
  }).catch(() => {})
}
</script>

<style lang="scss" scoped>
page { background-color: #f5f6f7; min-height: 100%; }

.container { padding-bottom: 120rpx; }

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
.task-name { font-size: 30rpx; font-weight: 600; color: #333; }
.task-sub { margin-top: 4rpx; }
.text-grey { color: #999; font-size: 24rpx; }
.empty-tip {
  text-align: center; padding: 40rpx;
  color: #999; font-size: 26rpx;
}

.form-box { padding: 16rpx 24rpx 24rpx; }

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
