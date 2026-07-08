<template>
  <view class="container">
    <!-- 状态筛选 Tab -->
    <view class="tab-bar">
      <view v-for="t in tabs" :key="t.value" class="tab-item" :class="{ active: activeTab === t.value }" @click="switchTab(t.value)">
        <text>{{ t.label }}</text>
      </view>
    </view>

    <!-- 列表 -->
    <view class="list-wrap">
      <view v-if="list.length === 0 && !loading" class="empty-tip">暂无领料单</view>
      <view v-for="item in list" :key="item.issueId" class="issue-card" @click="goDetail(item)">
        <view class="card-header">
          <text class="issue-code">{{ item.issueCode }}</text>
          <text class="issue-status" :class="'st-' + item.status">{{ statusText(item.status) }}</text>
        </view>
        <view class="card-row">
          <text class="label">名称：</text>
          <text class="value">{{ item.issueName || '-' }}</text>
        </view>
        <view class="card-row">
          <text class="label">工单：</text>
          <text class="value">{{ item.workorderName || '-' }}</text>
        </view>
        <view class="card-row">
          <text class="label">数量：</text>
          <text class="value">{{ item.quantityTotal || 0 }} / {{ item.quantityIssuedTotal || 0 }}（申请/已发）</text>
        </view>
        <view class="card-row">
          <text class="label">日期：</text>
          <text class="value">{{ formatDate(item.issueDate) }}</text>
        </view>
      </view>
      <view v-if="loading" class="loading-tip">加载中...</view>
      <view v-if="finished && list.length > 0" class="loading-tip">没有更多了</view>
    </view>
  </view>
</template>

<script setup>
import { ref, getCurrentInstance } from 'vue'
import { onShow, onReachBottom, onPullDownRefresh } from '@dcloudio/uni-app'
import { listIssue } from '@/api/mes/wm/issue'

const { proxy } = getCurrentInstance()

// 状态 Tab（按业务场景分组）
const tabs = [
  { label: '全部', value: '' },
  { label: '待发料', value: 'ALLOCATED' },
  { label: '发料中', value: 'PARTIAL_ISSUED' },
  { label: '已完成', value: 'ISSUED' }
]
const activeTab = ref('')
const list = ref([])
const loading = ref(false)
const finished = ref(false)
const queryParams = ref({ pageNum: 1, pageSize: 10 })

// 状态文字映射
const STATUS_MAP = {
  DRAFT: '草稿', PENDING: '待审核', APPROVED: '已下达', ALLOCATED: '已预占',
  PARTIAL_ISSUED: '部分发料', ISSUED: '已发料', CLOSED: '已关闭', CANCELED: '已作废'
}
function statusText(s) { return STATUS_MAP[s] || s }
function formatDate(d) { return d ? d.substring(0, 10) : '-' }

function switchTab(v) {
  activeTab.value = v
  resetList()
}

function resetList() {
  list.value = []
  finished.value = false
  queryParams.value.pageNum = 1
  loadList()
}

async function loadList() {
  if (loading.value || finished.value) return
  loading.value = true
  try {
    const params = { ...queryParams.value }
    if (activeTab.value) params.status = activeTab.value
    const res = await listIssue(params)
    const rows = res.rows || []
    list.value = [...list.value, ...rows]
    if (rows.length < queryParams.value.pageSize) finished.value = true
    else queryParams.value.pageNum++
  } catch (e) {
    // request.js 已处理错误提示
  } finally {
    loading.value = false
  }
}

function goDetail(item) {
  proxy.$tab.navigateTo('/pages/mes/wm/issue/detail?issueId=' + item.issueId)
}

onShow(() => { resetList() })
onReachBottom(() => { loadList() })
onPullDownRefresh(() => { resetList(); uni.stopPullDownRefresh() })
</script>

<style lang="scss" scoped>
.container { min-height: 100vh; background: #f5f6f7; }
.tab-bar {
  display: flex; background: #fff; position: sticky; top: 0; z-index: 10;
  border-bottom: 1rpx solid #eee;
  .tab-item {
    flex: 1; text-align: center; padding: 24rpx 0; font-size: 28rpx; color: #666;
    &.active { color: #409eff; font-weight: bold; border-bottom: 4rpx solid #409eff; }
  }
}
.list-wrap { padding: 16rpx; }
.issue-card {
  background: #fff; border-radius: 12rpx; padding: 24rpx; margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04);
  .card-header {
    display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx;
    .issue-code { font-size: 30rpx; font-weight: bold; color: #303133; }
    .issue-status { font-size: 24rpx; padding: 4rpx 16rpx; border-radius: 20rpx; }
  }
  .card-row { display: flex; font-size: 26rpx; margin-bottom: 8rpx;
    .label { color: #909399; width: 110rpx; flex-shrink: 0; }
    .value { color: #303133; flex: 1; }
  }
}
// 状态颜色
.st-DRAFT, .st-PENDING { background: #fdf6ec; color: #e6a23c; }
.st-APPROVED { background: #ecf5ff; color: #409eff; }
.st-ALLOCATED, .st-PARTIAL_ISSUED { background: #f0f9eb; color: #67c23a; }
.st-ISSUED, .st-CLOSED { background: #f4f4f5; color: #909399; }
.st-CANCELED { background: #fef0f0; color: #f56c6c; }
.empty-tip, .loading-tip { text-align: center; color: #909399; font-size: 26rpx; padding: 40rpx 0; }
</style>
