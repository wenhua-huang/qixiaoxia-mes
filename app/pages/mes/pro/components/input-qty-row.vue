<template>
  <view class="qty-row input-qty-row">
    <text class="qty-label">上机数量</text>
    <view class="qty-right">
      <text v-if="changed" class="changed-tip">已改·默认 {{ defaultVal }}，保存后留痕</text>
      <view class="qty-input">
        <uni-number-box :model-value="boxValue" :min="0" :step="1" @update:modelValue="onBoxChange" />
        <text class="unit">{{ unit || 'PCS' }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import UniNumberBox from '@/uni_modules/uni-number-box/components/uni-number-box/uni-number-box.vue'

/**
 * 报工「本次上机数量」行：
 * - 父级未给值（modelValue 为 null/undefined）时自动带出系统默认值 defaultVal；
 * - 人工值与默认值不一致时橙色提示「已改·默认 x，保存后留痕」；
 * - 不做清空成 null 的交互（后端 update 不支持值→null）。
 * 默认值为 null（无路线/无任务排产数等异常场景）时允许 0 起步手填。
 */
export default {
  name: 'InputQtyRow',
  components: { UniNumberBox },
  props: {
    modelValue: { type: Number, default: null },
    defaultVal: { type: Number, default: null },
    unit: { type: String, default: '' }
  },
  emits: ['update:modelValue'],
  data() {
    return {
      // 只在首次拿到默认值时自动带出一次，避免覆盖用户后续手改
      autoFilled: false
    }
  },
  computed: {
    boxValue() {
      return this.modelValue == null ? 0 : this.modelValue
    },
    changed() {
      return this.defaultVal != null && Number(this.modelValue) !== Number(this.defaultVal)
    }
  },
  watch: {
    defaultVal: {
      immediate: true,
      handler(val) {
        this.tryAutoFill(val)
      }
    }
  },
  methods: {
    tryAutoFill(val) {
      if (this.autoFilled) return
      if (val == null) return
      if (this.modelValue != null) {
        this.autoFilled = true
        return
      }
      this.autoFilled = true
      this.$emit('update:modelValue', Number(val))
    },
    onBoxChange(v) {
      this.autoFilled = true
      this.$emit('update:modelValue', Number(v))
    }
  }
}
</script>

<style lang="scss" scoped>
.qty-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16rpx 0;
  border-bottom: 1px solid #f5f5f5;
}
.qty-label { font-size: 28rpx; color: #333; }
.qty-right {
  display: flex; align-items: center; gap: 16rpx;
}
.qty-input {
  display: flex; align-items: center; gap: 12rpx;
}
.unit { color: #666; font-size: 24rpx; }
.changed-tip {
  font-size: 22rpx; color: #e6a23c; max-width: 320rpx; text-align: right;
}
</style>
