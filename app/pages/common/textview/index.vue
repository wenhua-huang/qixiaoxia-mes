<template>
  <view>
    <uni-card class="view-title" :title="title">
      <text class="uni-body view-content">{{ content }}</text>
    </uni-card>
  </view>
</template>

<script>
  // 显式引入 uni-ui 组件（绕过 HBuilderX 发行 H5 时 easycom 失效）
  import UniCard from '@/uni_modules/uni-card/components/uni-card/uni-card.vue'

  export default {
    components: { UniCard },
    data() {
      return {
        title: '',
        content: ''
      }
    },
    onLoad(options) {
      this.title = options.title
      this.content = options.content
      uni.setNavigationBarTitle({
        title: options.title
      })
    }
  }
</script>

<style scoped>
  page {
    background-color: #ffffff;
  }

  .view-title {
    font-weight: bold;
  }

  .view-content {
    font-size: 26rpx;
    padding: 12px 5px 0;
    color: #333;
    line-height: 24px;
    font-weight: normal;
  }
</style>
