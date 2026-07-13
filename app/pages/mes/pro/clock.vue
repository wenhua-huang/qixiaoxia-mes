<template>
  <view class="container">
    <!-- ═══ 顶部：打卡历史入口 ═══ -->
    <view class="history-entry" @click="goHistory">
      <uni-icons type="list" size="16" color="#409eff"></uni-icons>
      <text class="history-text">打卡历史</text>
      <uni-icons type="right" size="14" color="#c0c4cc"></uni-icons>
    </view>

    <!-- ═══ 在岗中（status=ACTIVE）═══ -->
    <view v-if="session" class="section active-card">
      <view class="status-bar">
        <uni-icons type="checkmark-filled" size="22" color="#67c23a"></uni-icons>
        <text class="status-text">在岗中</text>
      </view>
      <view class="info-list">
        <view class="info-row">
          <text class="label">工作站</text>
          <text class="value bold">{{ session.workstationName || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="label">上工时间</text>
          <text class="value">{{ formatTime(session.clockInTime) }}</text>
        </view>
        <view class="info-row">
          <text class="label">已工作</text>
          <text class="value duration">{{ elapsedText }}</text>
        </view>
        <view class="info-row" v-if="session.taskCode">
          <text class="label">关联任务</text>
          <text class="value">{{ session.processName || session.taskCode }}</text>
        </view>
        <view class="info-row" v-if="remark">
          <text class="label">备注</text>
          <text class="value">{{ remark }}</text>
        </view>
      </view>
      <!-- 下工前选填备注 -->
      <view class="remark-box">
        <uni-easyinput type="textarea" v-model="remark" placeholder="下工备注（选填）" :maxlength="200" />
      </view>
    </view>

    <view v-if="session" class="footer-bar">
      <button type="warn" class="action-btn" @click="doClockOut" :disabled="submitting">
        {{ submitting ? '处理中...' : '下工打卡' }}
      </button>
    </view>

    <!-- ═══ 上工表单（无在岗会话）═══ -->
    <template v-if="!session && loaded">
      <!-- 选择工位 -->
      <view class="section">
        <uni-section title="选择工作站" type="line">
          <template #right>
            <text class="scan-tip" @click="handleScanWs">扫码选择</text>
          </template>
        </uni-section>
        <view class="form-box">
          <!-- 我的绑定工位（快捷） -->
          <view v-if="myWorkstations.length > 0" class="ws-quick">
            <text class="quick-label">我的工位</text>
            <view class="ws-chips">
              <view v-for="ws in myWorkstations" :key="ws.workstationId"
                class="ws-chip" :class="{ active: form.workstationId === ws.workstationId }"
                @click="selectWorkstation(ws)">
                {{ ws.workstationName }}
              </view>
            </view>
          </view>
          <!-- 手动输入/扫码结果 -->
          <view class="input-row">
            <uni-easyinput v-model="wsCodeInput" placeholder="输入工位编码或扫码"
              :inputBorder="false" @confirm="searchWorkstation" />
            <button class="search-btn cu-btn bg-blue sm" @click="searchWorkstation">查询</button>
          </view>
          <!-- 当前选中 -->
          <view v-if="form.workstationName" class="selected-ws">
            <uni-icons type="checkmarkfilled" size="16" color="#67c23a"></uni-icons>
            <text>{{ form.workstationCode }} {{ form.workstationName }}</text>
          </view>
        </view>
      </view>

      <!-- 可选：关联任务 -->
      <view class="section">
        <uni-section title="关联任务（选填）" type="line">
          <template #right>
            <text class="scan-tip" @click="handleScanWo">扫码工单</text>
          </template>
        </uni-section>
        <view class="form-box">
          <view class="input-row">
            <uni-easyinput v-model="workorderCode" placeholder="输入工单号查询任务"
              :inputBorder="false" @confirm="searchWorkorder" />
            <button class="search-btn cu-btn bg-blue sm" @click="searchWorkorder">查询</button>
          </view>
          <view v-for="(task, idx) in taskList" :key="idx"
            class="task-item" :class="{ active: form.taskId === task.taskId }"
            @click="selectTask(task)">
            <view class="task-top">
              <text class="task-name">{{ task.processName || '工序' }}</text>
              <uni-tag v-if="form.taskId === task.taskId" type="primary" text="已选" size="small" />
            </view>
            <text class="text-grey">工作站：{{ task.workstationName || '-' }}</text>
          </view>
          <view v-if="workorderCode && taskList.length === 0" class="empty-tip">该工单暂无可选任务</view>
        </view>
      </view>

      <view class="section">
        <uni-section title="备注（选填）" type="line"></uni-section>
        <view class="form-box">
          <uni-easyinput type="textarea" v-model="remark" placeholder="如换班/异常说明等" :maxlength="200" />
        </view>
      </view>

      <view class="footer-bar">
        <button type="primary" class="action-btn" @click="doClockIn"
          :disabled="submitting || !form.workstationId">
          {{ submitting ? '处理中...' : '上工打卡' }}
        </button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { ref, reactive, computed, onUnmounted, getCurrentInstance } from 'vue'
import { clockIn, clockOut, getActiveSession, getMyWorkstations, resolveWorkstation } from '@/api/mes/pro/workrecord'
import { getFeedbackEntry } from '@/api/mes/pro/feedback'

const { proxy } = getCurrentInstance()

const loaded = ref(false)
const session = ref(null)         // 当前在岗会话（null=未上工）
const myWorkstations = ref([])    // 我的绑定工位
const taskList = ref([])          // 可选任务列表
const workorderCode = ref('')
const wsCodeInput = ref('')
const remark = ref('')
const submitting = ref(false)

// 实时时长：每分钟 tick 触发 computed 重算
const tick = ref(0)
let timer = null

const form = reactive({
  workstationId: null,
  workstationCode: '',
  workstationName: '',
  taskId: null,
  taskCode: '',
  workorderId: null,
  workorderCode: '',
  processName: ''
})

// 已工作时长文案（依赖 tick 触发响应式更新）
const elapsedText = computed(() => {
  tick.value  // 触发依赖
  if (!session.value) return '0 分钟'
  const m = Math.floor((Date.now() - new Date(session.value.clockInTime).getTime()) / 60000)
  const h = Math.floor(m / 60)
  return h > 0 ? `${h} 小时 ${m % 60} 分钟` : `${m} 分钟`
})

function formatTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').substring(0, 19)
}

// ═══ 初始化：查在岗状态 + 我的工位 ═══
function init() {
  proxy.$modal.loading('加载中...')
  Promise.all([getActiveSession(), getMyWorkstations()]).then(([sRes, wRes]) => {
    proxy.$modal.closeLoading()
    session.value = (sRes.data && sRes.data.recordId) ? sRes.data : null
    myWorkstations.value = wRes.data || []
    if (session.value) startTimer()
    loaded.value = true
  }).catch(() => {
    proxy.$modal.closeLoading()
    loaded.value = true
  })
}

function startTimer() {
  stopTimer()
  timer = setInterval(() => { tick.value++ }, 60000)  // 每分钟刷新
}
function stopTimer() {
  if (timer) { clearInterval(timer); timer = null }
}
onUnmounted(stopTimer)

// ═══ 工位选择 ═══
function selectWorkstation(ws) {
  form.workstationId = ws.workstationId
  form.workstationCode = ws.workstationCode
  form.workstationName = ws.workstationName
}

// 按编码查工位（调后端解析真实 workstationId，绑定优先不强制）
function searchWorkstation() {
  const code = wsCodeInput.value.trim()
  if (!code) {
    proxy.$modal.msgError('请输入工位编码')
    return
  }
  proxy.$modal.loading('查询工位...')
  resolveWorkstation(code).then(res => {
    proxy.$modal.closeLoading()
    const ws = res.data
    if (!ws || !ws.workstationId) {
      proxy.$modal.msgError('未找到该工位编码，请检查或扫码')
      form.workstationId = null
      form.workstationCode = ''
      form.workstationName = ''
      return
    }
    selectWorkstation(ws)
    // 绑定优先不强制：若不在绑定列表，仅提示不阻断
    const bound = myWorkstations.value.some(w => w.workstationId === ws.workstationId)
    if (!bound) {
      proxy.$modal.showToast('该工位不在您的绑定列表，仍可使用')
    }
  }).catch(() => {
    proxy.$modal.closeLoading()
    proxy.$modal.msgError('查询工位失败')
  })
}

// 扫码选工位
function handleScanWs() {
  // #ifdef APP-PLUS || H5
  uni.scanCode({
    onlyFromCamera: false, scanType: ['barCode', 'qrCode'],
    success: (res) => { wsCodeInput.value = res.result; searchWorkstation() },
    fail: () => {}
  })
  // #endif
  // #ifdef MP-WEIXIN
  uni.scanCode({ success: (res) => { wsCodeInput.value = res.result; searchWorkstation() } })
  // #endif
}

// ═══ 任务选择（复用报工的 feedbackEntry）═══
function searchWorkorder() {
  if (!workorderCode.value.trim()) return
  proxy.$modal.loading('查询中...')
  getFeedbackEntry(workorderCode.value.trim()).then(res => {
    proxy.$modal.closeLoading()
    taskList.value = (res.data && res.data.tasks) || []
    if (taskList.value.length === 0) proxy.$modal.msgError('该工单暂无可选任务')
  }).catch(() => {
    proxy.$modal.closeLoading()
    proxy.$modal.msgError('查询失败')
  })
}

function selectTask(task) {
  form.taskId = task.taskId
  form.taskCode = task.taskCode
  form.workorderId = task.workorderId
  form.workorderCode = task.workorderCode
  form.processName = task.processName
  // 若未选工位，用任务的工位
  if (!form.workstationId && task.workstationId) {
    form.workstationId = task.workstationId
    form.workstationCode = task.workstationCode
    form.workstationName = task.workstationName
  }
}

function handleScanWo() {
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

// ═══ 上工 ═══
function goHistory() {
  proxy.$tab.navigateTo('/pages/mes/pro/clock-history')
}

function doClockIn() {
  if (!form.workstationId) {
    proxy.$modal.msgError('请选择工作站')
    return
  }
  proxy.$modal.confirm('确认上工打卡？').then(() => {
    submitting.value = true
    const body = {
      workstationId: form.workstationId,
      taskId: form.taskId || null,
      workorderId: form.workorderId || null,
      remark: remark.value || null
    }
    clockIn(body).then(() => {
      proxy.$modal.msgSuccess('上工成功！')
      // 刷新在岗状态
      remark.value = ''
      Object.assign(form, { workstationId: null, workstationCode: '', workstationName: '', taskId: null, taskCode: '', workorderId: null, workorderCode: '', processName: '' })
      wsCodeInput.value = ''
      workorderCode.value = ''
      taskList.value = []
      init()
    }).catch(e => {
      proxy.$modal.msgError(typeof e === 'string' ? e : (e.msg || '上工失败'))
    }).finally(() => { submitting.value = false })
  }).catch(() => {})
}

// ═══ 下工 ═══
function doClockOut() {
  proxy.$modal.confirm('确认下工打卡？').then(() => {
    submitting.value = true
    clockOut({ remark: remark.value || null }).then(() => {
      proxy.$modal.msgSuccess('下工成功！')
      session.value = null
      stopTimer()
      remark.value = ''
      init()
    }).catch(e => {
      proxy.$modal.msgError(typeof e === 'string' ? e : (e.msg || '下工失败'))
    }).finally(() => { submitting.value = false })
  }).catch(() => {})
}

init()
</script>

<style lang="scss" scoped>
page { background-color: #f5f6f7; min-height: 100%; }
.container { padding-bottom: 140rpx; }

.history-entry {
  display: flex; align-items: center; gap: 8rpx;
  margin: 20rpx 24rpx 0; padding: 16rpx 24rpx;
  background: #fff; border-radius: 12rpx;
}
.history-text { flex: 1; font-size: 28rpx; color: #409eff; }

.section { margin: 20rpx 0; background: #fff; }
.scan-tip { color: #409eff; font-size: 24rpx; }

/* 在岗卡片 */
.active-card { padding: 0 0 24rpx; }
.status-bar {
  display: flex; align-items: center; gap: 12rpx;
  padding: 28rpx 32rpx; background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}
.status-text { color: #fff; font-size: 32rpx; font-weight: 600; }
.info-list { padding: 16rpx 32rpx; }
.info-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16rpx 0; border-bottom: 1px solid #f5f5f5;
}
.info-row:last-child { border-bottom: none; }
.label { color: #999; font-size: 26rpx; }
.value { font-size: 28rpx; color: #333; }
.bold { font-weight: 600; }
.duration { color: #409eff; font-weight: 600; }
.remark-box { padding: 16rpx 32rpx; }

/* 表单 */
.form-box { padding: 16rpx 24rpx 24rpx; }
.quick-label { font-size: 26rpx; color: #666; }
.ws-quick { margin-bottom: 20rpx; }
.ws-chips { display: flex; flex-wrap: wrap; gap: 16rpx; margin-top: 12rpx; }
.ws-chip {
  padding: 12rpx 24rpx; background: #f4f4f5; border-radius: 8rpx;
  font-size: 26rpx; color: #606266; border: 1px solid #e4e7ed;
}
.ws-chip.active { background: #ecf5ff; color: #409eff; border-color: #409eff; }
.input-row { display: flex; align-items: center; gap: 16rpx; }
.search-btn { margin: 0; font-size: 26rpx; height: 64rpx; line-height: 64rpx; flex-shrink: 0; }
.selected-ws {
  display: flex; align-items: center; gap: 8rpx;
  margin-top: 16rpx; padding: 16rpx; background: #f0f9eb; border-radius: 8rpx;
  font-size: 26rpx; color: #67c23a;
}
.task-item {
  padding: 24rpx; border-bottom: 1px solid #f5f5f5;
  border-left: 6rpx solid transparent;
}
.task-item.active { background: #ecf5ff; border-left-color: #409eff; }
.task-top {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 8rpx;
}
.task-name { font-size: 30rpx; font-weight: 600; color: #333; }
.text-grey { color: #999; font-size: 24rpx; }
.empty-tip { text-align: center; padding: 40rpx; color: #999; font-size: 26rpx; }

/* 底部按钮 */
.footer-bar {
  position: fixed; bottom: 0; left: 0; right: 0;
  padding: 20rpx 32rpx; padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff; border-top: 1px solid #f0f0f0; z-index: 100;
}
.action-btn { width: 100%; height: 88rpx; line-height: 88rpx; font-size: 32rpx; border-radius: 12rpx; }
</style>
