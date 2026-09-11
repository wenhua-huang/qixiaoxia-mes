<template>
  <view class="qc-block-bar">
    <view class="block-head">
      <uni-icons type="locked" size="18" color="#f56c6c" />
      <text class="block-title">不可开工</text>
    </view>
    <text class="block-reason">{{ reason }}</text>
    <button v-if="canRelease" class="release-btn" :disabled="releasing" @click="onRelease">
      {{ releasing ? '放行中...' : '放行（需授权）' }}
    </button>
  </view>
</template>

<script>
import UniIcons from '@/uni_modules/uni-icons/components/uni-icons/uni-icons.vue'
import { releaseQcBlock } from '@/api/mes/pro/task'

/**
 * 跟单质检不合格阻塞条：
 * - 红条展示拦截原因，无放行权限的工人只看到原因；
 * - 具备 mes:pro:task:release 权限时显示放行按钮，理由（≥2 字）随接口留痕；
 * - 放行成功后 emit('released', taskId)，由父页面重拉入口数据解除锁态。
 */
export default {
  name: 'QcBlockBar',
  components: { UniIcons },
  props: {
    task: { type: Object, required: true }
  },
  emits: ['released'],
  data() {
    return {
      releasing: false
    }
  },
  computed: {
    reason() {
      return this.task.qcBlockReason || '上道检验工序判定不合格，本工序暂不可报工，请联系质检或有权限人员放行'
    },
    canRelease() {
      return this.$auth.hasPermi('mes:pro:task:release')
    }
  },
  methods: {
    onRelease() {
      uni.showModal({
        title: '质检不合格放行',
        editable: true,
        placeholderText: '请填写放行理由（必填，至少2字）',
        success: (res) => {
          if (!res.confirm) return
          const reason = (res.content || '').trim()
          if (reason.length < 2) {
            uni.showToast({ title: '请填写放行理由（至少2字）', icon: 'none' })
            return
          }
          this.doRelease(reason)
        }
      })
    },
    doRelease(reason) {
      this.releasing = true
      releaseQcBlock(this.task.taskId, reason).then(() => {
        uni.showToast({ title: '已放行', icon: 'success' })
        this.$emit('released', this.task.taskId)
      }).catch(() => {
        // request.js 全局拦截器已 toast 后端错误信息
      }).finally(() => {
        this.releasing = false
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.qc-block-bar {
  margin: 16rpx 24rpx 0;
  padding: 24rpx;
  background: #fef0f0;
  border: 1rpx solid #f56c6c;
  border-radius: 12rpx;
}
.block-head {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-bottom: 12rpx;
}
.block-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #f56c6c;
}
.block-reason {
  font-size: 26rpx;
  color: #f56c6c;
  line-height: 1.5;
}
.release-btn {
  margin-top: 20rpx;
  background: #f56c6c;
  color: #fff;
  font-size: 28rpx;
  border-radius: 40rpx;
  height: 72rpx;
  line-height: 72rpx;
}
.release-btn::after { border: none; }
</style>
