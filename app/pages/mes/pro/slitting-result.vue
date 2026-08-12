<template>
  <view class="container">
    <view v-if="loading" class="loading-box">
      <uni-load-more status="loading" />
    </view>

    <template v-else-if="record">
      <!-- 母卷信息 -->
      <uni-section title="母卷信息" type="line"></uni-section>
      <view class="info-card">
        <view class="info-row">
          <text class="label">母卷号</text>
          <text class="value bold">{{ record.parentRollCode }}</text>
        </view>
        <view class="info-row">
          <text class="label">物料</text>
          <text class="value">{{ record.parentItemName }}</text>
        </view>
        <view class="info-row">
          <text class="label">重量</text>
          <text class="value bold">{{ record.parentWeight }}吨</text>
        </view>
        <view class="info-row">
          <text class="label">门幅</text>
          <text class="value">{{ record.parentWidth || '-' }}mm</text>
        </view>
      </view>

      <!-- 批量模板（同规格生成多卷） -->
      <uni-section title="批量添加（同规格）" type="line"></uni-section>
      <view class="info-card" v-if="canEdit">
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
        <button class="cu-btn bg-blue sm full-btn" @click="batchAdd">生成子卷</button>
      </view>

      <!-- 子卷列表（逐条秤重） -->
      <uni-section :title="'子卷列表（' + childRolls.length + '）'" type="line"></uni-section>
      <view class="info-card">
        <view v-if="childRolls.length === 0" class="empty-hint">
          <text class="text-grey">请先生成或添加子卷</text>
        </view>
        <view v-else>
          <view v-for="(roll, idx) in childRolls" :key="idx" class="roll-edit-row">
            <view class="roll-edit-top">
              <text class="roll-no">子卷 {{ idx + 1 }}</text>
              <uni-icons v-if="canEdit" type="close" size="18" color="#f56c6c" @click="removeChild(idx)"></uni-icons>
            </view>
            <view class="roll-edit-fields">
              <view class="field">
                <text class="field-label">门幅(mm)</text>
                <uni-easyinput v-model="roll.actualWidth" type="number" placeholder="门幅" :inputBorder="true" :disabled="!canEdit" />
              </view>
              <view class="field">
                <text class="field-label">克重(g)</text>
                <uni-easyinput v-model="roll.actualWeightGsm" type="number" placeholder="克重" :inputBorder="true" :disabled="!canEdit" />
              </view>
              <view class="field">
                <text class="field-label">重量(吨)</text>
                <uni-easyinput v-model="roll.actualWeight" type="digit" placeholder="重量" :inputBorder="true" :disabled="!canEdit" />
              </view>
            </view>
          </view>
        </view>
        <button v-if="canEdit" class="cu-btn line-blue sm full-btn" @click="addChild">+ 单独添加</button>
      </view>

      <!-- 已提交提示 -->
      <view v-if="!canEdit" class="already-tip">
        <uni-icons type="checkmarkempty" size="16" color="#67c23a"></uni-icons>
        <text>已提交分切结果，不可修改</text>
      </view>

      <!-- 重量校验 -->
      <view class="weight-bar" :class="!weightValid ? 'err' : (lossExcessive ? 'warn' : 'ok')">
        <text>子卷总重：{{ childTotalWeight }}吨 / 母卷 {{ record.parentWeight }}吨</text>
        <text class="weight-loss">损耗率：{{ lossRate }}%</text>
      </view>
      <view v-if="lossExcessive" class="loss-tip">损耗率超过 3%，请确认边料重量；我方收货时将复核</view>

      <!-- 底部提交 -->
      <view class="footer-bar" v-if="canEdit">
        <button class="cu-btn bg-blue lg" :disabled="!canSubmit || submitting" @click="submit">
          {{ submitting ? '提交中...' : '提交分切结果' }}
        </button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { ref, reactive, computed, getCurrentInstance } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getSlitting, recordOutsourceResult } from '@/api/mes/pro/slitting'

const { proxy } = getCurrentInstance()

const record = ref(null)
const loading = ref(true)
const submitting = ref(false)
const childRolls = ref([])
const tpl = reactive({ width: '', gsm: '', count: '1' })

const childTotalWeight = computed(() => {
  return childRolls.value.reduce((sum, r) => sum + Number(r.actualWeight || 0), 0).toFixed(4)
})
const parentWeight = computed(() => Number(record.value?.parentWeight || 0))
const lossRate = computed(() => {
  const total = Number(childTotalWeight.value)
  if (parentWeight.value <= 0) return '0.00'
  return (((parentWeight.value - total) / parentWeight.value) * 100).toFixed(2)
})
const weightValid = computed(() => {
  if (childRolls.value.length === 0) return false
  const loss = parentWeight.value - Number(childTotalWeight.value)
  // 仅硬挡「子卷总重超过母卷」(loss<0)；损耗率>3% 不阻断厂商录入，
  // 3% 阈值由我方收货阶段结合边料重量校验，此处仅提示
  return loss >= 0
})
const lossExcessive = computed(() => Number(lossRate.value) > 3)
// 仅 ISSUED 状态可录入结果；已提交（SLITTING/RECEIVED）只读
const canEdit = computed(() => record.value?.status === 'ISSUED')
const canSubmit = computed(() => weightValid.value && canEdit.value)

function batchAdd() {
  if (!tpl.width || !tpl.count) {
    proxy.$modal.msg('请填写门幅和条数')
    return
  }
  const count = parseInt(tpl.count)
  if (isNaN(count) || count <= 0 || count > 50) {
    proxy.$modal.msg('条数需在 1~50 之间')
    return
  }
  for (let i = 0; i < count; i++) {
    childRolls.value.push({
      actualWidth: tpl.width,
      actualWeightGsm: tpl.gsm || '',
      actualWeight: ''
    })
  }
  proxy.$modal.msgSuccess('已生成 ' + count + ' 条子卷')
}

function addChild() {
  childRolls.value.push({ actualWidth: '', actualWeightGsm: '', actualWeight: '' })
}

function removeChild(idx) {
  childRolls.value.splice(idx, 1)
}

async function submit() {
  if (!canEdit.value) { proxy.$modal.msgError('当前状态不可修改'); return }
  if (childRolls.value.length === 0) { proxy.$modal.msg('请添加子卷'); return }
  // 校验每条都有重量
  const missing = childRolls.value.some(r => !r.actualWeight || !r.actualWidth)
  if (missing) { proxy.$modal.msg('请填写所有子卷的门幅和重量'); return }
  if (!weightValid.value) {
    proxy.$modal.alert('重量校验未通过：子卷总重超过母卷或损耗率超过3%，请检查录入数据')
    return
  }
  submitting.value = true
  try {
    await recordOutsourceResult(record.value.slitId, {
      childRolls: childRolls.value.map(r => ({
        actualWidth: r.actualWidth,
        actualWeightGsm: r.actualWeightGsm,
        actualWeight: r.actualWeight
      }))
    })
    proxy.$modal.msgSuccess('分切结果已提交')
    setTimeout(() => proxy.$tab.navigateBack(), 1500)
  } catch (e) {} finally {
    submitting.value = false
  }
}

onLoad((options) => {
  const slitId = options.slitId
  if (!slitId) { proxy.$modal.msgError('缺少分切单ID'); return }
  getSlitting(slitId).then(res => {
    record.value = res.data
    loading.value = false
    // 若已录过结果（SLITTING/RECEIVED），回填已有子卷
    if (res.data.childRolls && res.data.childRolls.length > 0) {
      childRolls.value = res.data.childRolls.map(r => ({
        actualWidth: r.actualWidth, actualWeightGsm: r.actualWeightGsm, actualWeight: r.actualWeight
      }))
    }
  }).catch(() => { loading.value = false })
})
</script>

<style lang="scss" scoped>
page { background-color: #f5f6f7; min-height: 100%; padding-bottom: 200rpx; }
.container { padding: 0 0 200rpx; }

.loading-box { display: flex; justify-content: center; padding: 80rpx 0; }

.info-card {
  background: #fff; margin: 16rpx 24rpx; border-radius: 16rpx; padding: 8rpx 24rpx;
}
.info-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20rpx 0; border-bottom: 1px solid #f5f5f5;
}
.info-row:last-child { border-bottom: none; }
.label { color: #999; font-size: 26rpx; }
.value { font-size: 28rpx; color: #333; }
.bold { font-weight: 600; }

.template-row {
  display: flex; gap: 16rpx; padding: 16rpx 0;
}
.template-item { flex: 1; display: flex; flex-direction: column; gap: 8rpx; }
.full-btn { width: 100%; margin: 16rpx 0; }

.empty-hint { padding: 40rpx 0; text-align: center; }
.text-grey { color: #999; font-size: 26rpx; }

.roll-edit-row {
  padding: 20rpx 0; border-bottom: 1px solid #f5f5f5;
}
.roll-edit-top {
  display: flex; justify-content: space-between; align-items: center;
  padding-bottom: 12rpx;
}
.roll-no { font-size: 28rpx; font-weight: 600; color: #333; }
.roll-edit-fields {
  display: flex; gap: 12rpx;
}
.field { flex: 1; display: flex; flex-direction: column; gap: 6rpx; }
.field-label { font-size: 22rpx; color: #999; }

.weight-bar {
  margin: 16rpx 24rpx; padding: 20rpx 24rpx; border-radius: 12rpx;
  display: flex; justify-content: space-between; font-size: 26rpx;
}
.weight-bar.ok { background: #f0f9eb; color: #67c23a; }
.weight-bar.err { background: #fef0f0; color: #f56c6c; }
.weight-bar.warn { background: #fdf6ec; color: #e6a23c; }
.weight-loss { font-weight: 600; }
.loss-tip { margin: 0 24rpx 16rpx; font-size: 22rpx; color: #e6a23c; }
.already-tip { display: flex; align-items: center; justify-content: center; gap: 8rpx; margin: 16rpx 24rpx; padding: 24rpx; background: #f0f9eb; border-radius: 12rpx; font-size: 26rpx; color: #67c23a; }

.footer-bar {
  position: fixed; left: 0; right: 0; bottom: 0;
  padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));
  background: #fff; border-top: 1px solid #eee;
}
.cu-btn { border-radius: 12rpx; border: none; }
.cu-btn.sm { font-size: 26rpx; height: 64rpx; line-height: 64rpx; }
.cu-btn.lg { width: 100%; font-size: 30rpx; height: 88rpx; line-height: 88rpx; }
.bg-blue { background: #007aff; color: #fff; }
.line-blue { background: #fff; color: #007aff; border: 1px solid #007aff; }
.cu-btn[disabled] { opacity: 0.5; }
</style>
