<template>
  <view class="rqc-fields">
    <view class="h-row qty-row">
      <text class="muted">责任归属</text>
      <picker v-if="!readonly" class="rqc-picker" :range="respOptions" range-key="label" @change="onRespChange">
        <view class="picker-value" :class="{ placeholder: !form.responsibility }">
          {{ form.responsibility ? responsibilityText(form.responsibility) : '请选择' }}
        </view>
      </picker>
      <text v-else>{{ responsibilityText(form.responsibility) || '—' }}</text>
    </view>
    <view class="rqc-reason">
      <text class="muted">退料原因</text>
      <uni-easyinput v-if="!readonly" v-model="form.returnReason" type="textarea" :maxlength="200" placeholder="请输入退料原因" />
      <text v-else class="rqc-reason-text">{{ form.returnReason || '—' }}</text>
    </view>
  </view>
</template>

<script setup>
import uniEasyinput from '@/uni_modules/uni-easyinput/components/uni-easyinput/uni-easyinput.vue'
import { responsibilityText, RESPONSIBILITY_OPTIONS } from '@/utils/qc'

// 表单子组件：直接写回传入的 form 对象属性（项目既有模式，见 qc-line-card 改 line.checkValText）
const props = defineProps({ form: { type: Object, required: true }, readonly: Boolean })
const respOptions = RESPONSIBILITY_OPTIONS

function onRespChange(e) {
  const idx = Number(e.detail.value)
  props.form.responsibility = respOptions[idx].value
}
</script>

<style lang="scss" scoped>
.rqc-fields { border-top: 2rpx solid #f0f0f0; padding-top: 16rpx; margin-top: 8rpx; }
.rqc-picker { flex: 1; text-align: right; }
.picker-value { font-size: 28rpx; color: #303133; text-align: right;
  &.placeholder { color: #c0c4cc; } }
.rqc-reason { margin-top: 16rpx;
  .muted { display: block; margin-bottom: 8rpx; } }
.rqc-reason-text { font-size: 28rpx; color: #303133; }
</style>
