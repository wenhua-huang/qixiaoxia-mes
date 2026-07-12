<template>
  <view class="container">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <uni-easyinput
        v-model="queryParams.workstationName"
        placeholder="工位名称"
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

    <!-- 打卡记录列表 -->
    <view v-if="loading" class="loading-box">
      <uni-load-more status="loading" />
    </view>

    <view v-else-if="list.length === 0" class="empty-box">
      <text class="text-grey">暂无打卡记录</text>
    </view>

    <view v-else class="list-box">
      <view v-for="item in list" :key="item.recordId" class="list-item">
        <view class="item-header">
          <text class="bold">{{ item.workstationName || '-' }}</text>
          <uni-tag :type="statusTagType(item.status)" :text="statusText(item.status)" size="small" />
        </view>
        <view class="item-body">
          <view class="item-row" v-if="item.processName">
            <text class="label">工序</text>
            <text class="value">{{ item.processName }}</text>
          </view>
          <view class="item-row" v-if="item.workorderCode">
            <text class="label">工单</text>
            <text class="value">{{ item.workorderCode }}</text>
          </view>
          <view class="item-row">
            <text class="label">上工</text>
            <text class="value">{{ formatTime(item.clockInTime) }}</text>
          </view>
          <view class="item-row">
            <text class="label">下工</text>
            <text class="value">{{ item.clockOutTime ? formatTime(item.clockOutTime) : '—' }}</text>
          </view>
          <view class="item-row">
            <text class="label">工时</text>
            <text class="value bold duration">{{ durationText(item) }}</text>
          </view>
          <view class="item-row" v-if="item.remark">
            <text class="label">备注</text>
            <text class="value">{{ item.remark }}</text>
          </view>
        </view>
      </view>

      <uni-load-more :status="loadMoreStatus" />
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { listMyHistory } from '@/api/mes/pro/workrecord'

const list = ref([])
const loading = ref(false)
const loadMoreStatus = ref('more')
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  workstationName: '',
  status: ''
})

const statusFilters = [
  { label: '全部', value: '' },
  { label: '在岗中', value: 'ACTIVE' },
  { label: '已下工', value: 'CLOSED' }
]

const STATUS_MAP = {
  ACTIVE: '在岗中', CLOSED: '已下工'
}
function statusText(s) { return STATUS_MAP[s] || s || '' }
function statusTagType(s) {
  return s === 'ACTIVE' ? 'success' : 'default'
}

/** 格式化时间：2026-07-13 14:52:17 → 07-13 14:52 */
function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').substring(5, 16)
}

/** 工时文案：优先用后端算好的 workDuration（分钟），否则按上工时间估算 */
function durationText(item) {
  if (item.workDuration != null && item.workDuration >= 0) {
    return humanDuration(item.workDuration)
  }
  if (!item.clockInTime) return '-'
  const end = item.clockOutTime ? new Date(item.clockOutTime).getTime() : Date.now()
  const m = Math.max(0, Math.floor((end - new Date(item.clockInTime).getTime()) / 60000))
  return humanDuration(m) + (item.status === 'ACTIVE' ? '+' : '')
}

/** 分钟 → "x 小时 y 分钟" */
function humanDuration(m) {
  const h = Math.floor(m / 60)
  return h > 0 ? `${h} 小时 ${m % 60} 分钟` : `${m} 分钟`
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
  listMyHistory(queryParams).then(res => {
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
  listMyHistory(queryParams).then(res => {
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
.duration { color: #409eff; }
.text-grey { color: #999; font-size: 24rpx; }
</style>
