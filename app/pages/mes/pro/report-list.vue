<template>
  <view class="container">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <uni-easyinput
        v-model="queryParams.workorderCode"
        placeholder="输入工单号"
        :inputBorder="false"
        class="search-input"
        @confirm="handleQuery"
      />
      <button class="search-btn cu-btn bg-blue sm" @click="handleQuery">搜索</button>
    </view>

    <!-- 加载中 -->
    <view v-if="loading" class="loading-box">
      <uni-load-more status="loading" />
    </view>

    <!-- 空状态 -->
    <view v-else-if="list.length === 0" class="empty-box">
      <text class="text-grey">暂无待报工工序</text>
    </view>

    <!-- 列表 -->
    <view v-else class="list-box">
      <view v-for="item in list" :key="item.taskId" class="list-item" @click="goReport(item)">
        <view class="item-header">
          <text class="bold">{{ item.workorderCode }}</text>
          <uni-tag type="warning" text="生产中" size="small" />
        </view>
        <view class="item-body">
          <view class="item-row">
            <text class="label">工序</text>
            <text class="value">{{ item.processCode }} {{ item.processName }}</text>
          </view>
          <view class="item-row">
            <text class="label">工作站</text>
            <text class="value">{{ item.workstationName || '-' }}</text>
          </view>
          <view class="item-row" v-if="item.itemName">
            <text class="label">产品</text>
            <text class="value">{{ item.itemName }}</text>
          </view>
          <view class="item-row">
            <text class="label">排产 / 已产</text>
            <text class="value bold">{{ num(item.quantity) }} / {{ num(item.quantityProduced) }} {{ item.unitName || '' }}</text>
          </view>
        </view>
        <view class="item-footer">
          <text class="text-grey">{{ item.workorderName || '-' }}</text>
          <text class="go-text">去报工 ›</text>
        </view>
      </view>

      <uni-load-more :status="loadMoreStatus" />
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, getCurrentInstance } from 'vue'
import UniEasyInput from '@/uni_modules/uni-easyinput/components/uni-easyinput/uni-easyinput.vue'
import UniLoadMore from '@/uni_modules/uni-load-more/components/uni-load-more/uni-load-more.vue'
import UniTag from '@/uni_modules/uni-tag/components/uni-tag/uni-tag.vue'
import { onShow, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { listReportableTask } from '@/api/mes/pro/feedback'

const { proxy } = getCurrentInstance()
const list = ref([])
const loading = ref(false)
const loadMoreStatus = ref('more')
const queryParams = reactive({
  pageNum: 1,
  pageSize: 20,
  workorderCode: ''
})

function num(v) {
  const n = Number(v)
  return isNaN(n) ? 0 : n
}

function handleQuery() {
  queryParams.pageNum = 1
  loadData(false)
}

function buildQuery() {
  return {
    pageNum: queryParams.pageNum,
    pageSize: queryParams.pageSize,
    workorderCode: queryParams.workorderCode
  }
}

function loadData(append = false) {
  loading.value = true
  return listReportableTask(buildQuery()).then(res => {
    const rows = res.rows || []
    list.value = append ? list.value.concat(rows) : rows
    loading.value = false
    loadMoreStatus.value = rows.length >= queryParams.pageSize ? 'more' : 'noMore'
  }).catch(() => {
    loading.value = false
    loadMoreStatus.value = 'more'
  })
}

function goReport(item) {
  proxy.$tab.navigateTo('/pages/mes/pro/report?workorderCode=' + encodeURIComponent(item.workorderCode) + '&taskId=' + item.taskId)
}

onShow(() => {
  queryParams.pageNum = 1
  loadData(false)
})

onPullDownRefresh(() => {
  queryParams.pageNum = 1
  loadData(false).then(() => uni.stopPullDownRefresh())
})

onReachBottom(() => {
  if (loading.value || loadMoreStatus.value === 'noMore') return
  queryParams.pageNum++
  loadData(true)
})
</script>

<style lang="scss" scoped>
page { background-color: #f5f6f7; min-height: 100%; }

.search-bar {
  display: flex; align-items: center;
  padding: 20rpx 24rpx; gap: 16rpx;
  background: #fff;
}
.search-input { flex: 1; }
.search-btn { margin: 0; font-size: 26rpx; height: 64rpx; line-height: 64rpx; }

.loading-box, .empty-box {
  display: flex; justify-content: center; padding: 80rpx 0;
}

.list-box { padding: 16rpx 24rpx; }

.list-item {
  background: #fff; border-radius: 16rpx;
  padding: 24rpx; margin-bottom: 20rpx;
}
.item-header {
  display: flex; justify-content: space-between; align-items: center;
  padding-bottom: 16rpx; border-bottom: 1px solid #f5f5f5;
}
.item-body { padding: 16rpx 0; }
.item-row {
  display: flex; justify-content: space-between;
  padding: 6rpx 0;
}
.label { color: #999; font-size: 26rpx; }
.value { font-size: 26rpx; color: #333; max-width: 60%; text-align: right; }
.bold { font-weight: 600; }
.item-footer {
  display: flex; justify-content: space-between; align-items: center;
  padding-top: 12rpx;
}
.go-text { color: #007aff; font-size: 26rpx; }
.text-grey { color: #999; font-size: 24rpx; }
</style>
