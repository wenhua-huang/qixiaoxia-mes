<template>
  <view class="container">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <uni-easyinput
        v-model="queryParams.workorderName"
        placeholder="工单名称"
        :inputBorder="false"
        class="search-input"
        @confirm="handleQuery"
      />
      <button class="search-btn cu-btn bg-blue sm" @click="handleQuery">搜索</button>
    </view>

    <!-- 状态筛选 -->
    <view class="filter-row">
      <text
        v-for="s in statusFilters"
        :key="s.value"
        :class="['filter-tag', queryParams.status === s.value ? 'active' : '']"
        @click="filterStatus(s.value)"
      >{{ s.label }}</text>
    </view>

    <!-- 报工记录列表 -->
    <view v-if="loading" class="loading-box">
      <uni-load-more status="loading" />
    </view>

    <view v-else-if="list.length === 0" class="empty-box">
      <text class="text-grey">暂无报工记录</text>
    </view>

    <view v-else class="list-box">
      <view v-for="item in list" :key="item.recordId" class="list-item">
        <view class="item-header">
          <text class="bold">{{ item.feedbackCode }}</text>
          <uni-tag :type="fbStatusTagType(item.status)" :text="fbStatusText(item.status)" size="small" />
        </view>
        <view class="item-body">
          <view class="item-row">
            <text class="label">工单</text>
            <text class="value">{{ item.workorderName || '-' }}</text>
          </view>
          <view class="item-row">
            <text class="label">工序</text>
            <text class="value">{{ item.processName || '-' }}</text>
          </view>
          <view class="item-row">
            <text class="label">合格数</text>
            <text class="value bold">{{ item.quantityQualified || 0 }} {{ item.unitName || '' }}</text>
          </view>
          <view class="item-row" v-if="item.quantityUnqualified > 0">
            <text class="label">不合格数</text>
            <text class="value" style="color:#f56c6c">{{ item.quantityUnqualified }}</text>
          </view>
        </view>
        <view class="item-footer">
          <text class="text-grey">{{ item.feedbackTime || item.createTime }}</text>
        </view>
      </view>

      <uni-load-more :status="loadMoreStatus" />
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { listFeedback } from '@/api/mes/pro/feedback'

const list = ref([])
const loading = ref(false)
const loadMoreStatus = ref('more')
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  workorderName: '',
  status: ''
})

const statusFilters = [
  { label: '全部', value: '' },
  { label: '待确认', value: 'PREPARE' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '已审核', value: 'AUDITED' }
]

const STATUS_MAP = {
  PREPARE: '待确认', CONFIRMED: '已确认', AUDITED: '已审核'
}
function fbStatusText(s) { return STATUS_MAP[s] || s || '' }
function fbStatusTagType(s) {
  const m = { PREPARE: 'warning', CONFIRMED: 'primary', AUDITED: 'success' }
  return m[s] || 'default'
}

function filterStatus(val) {
  queryParams.status = val
  queryParams.pageNum = 1
  loadData()
}

function handleQuery() {
  queryParams.pageNum = 1
  loadData()
}

function loadData() {
  loading.value = true
  listFeedback(queryParams).then(res => {
    list.value = res.rows || []
    loading.value = false
    loadMoreStatus.value = (res.rows && res.rows.length >= queryParams.pageSize) ? 'more' : 'noMore'
  }).catch(() => {
    loading.value = false
  })
}

onPullDownRefresh(() => {
  queryParams.pageNum = 1
  loadData()
  uni.stopPullDownRefresh()
})

onReachBottom(() => {
  if (loadMoreStatus.value === 'noMore') return
  queryParams.pageNum++
  loadMoreStatus.value = 'loading'
  listFeedback(queryParams).then(res => {
    const rows = res.rows || []
    list.value = list.value.concat(rows)
    loadMoreStatus.value = rows.length >= queryParams.pageSize ? 'more' : 'noMore'
  }).catch(() => {
    queryParams.pageNum--
    loadMoreStatus.value = 'more'
  })
})

// 页面加载时自动查询
loadData()
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

.filter-row {
  display: flex; gap: 16rpx;
  padding: 16rpx 24rpx; background: #fff;
  border-bottom: 1px solid #f0f0f0;
}
.filter-tag {
  padding: 8rpx 24rpx; border-radius: 32rpx;
  font-size: 24rpx; color: #666; background: #f5f5f5;
}
.filter-tag.active {
  color: #fff; background: #007aff;
}

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
.value { font-size: 26rpx; color: #333; }
.bold { font-weight: 600; }
.item-footer {
  display: flex; justify-content: space-between; align-items: center;
  padding-top: 12rpx;
}
.text-grey { color: #999; font-size: 24rpx; }
</style>
