<template>
  <view class="container">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <uni-easyinput
        v-model="queryParams.orderCode"
        placeholder="输入外协单号"
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
        :class="['filter-tag', activeStatus === s.value ? 'active' : '']"
        @click="filterStatus(s.value)"
      >{{ s.label }}</text>
    </view>

    <!-- 加载中 -->
    <view v-if="loading" class="loading-box">
      <uni-load-more status="loading" />
    </view>

    <!-- 空状态 -->
    <view v-else-if="list.length === 0" class="empty-box">
      <text class="text-grey">暂无待收货外协单</text>
    </view>

    <!-- 列表 -->
    <view v-else class="list-box">
      <view v-for="item in list" :key="item.orderId" class="list-item" @click="goReceive(item)">
        <view class="item-header">
          <text class="bold">{{ item.orderCode }}</text>
          <uni-tag :type="outsourceStatusTagType(item.status)" :text="outsourceStatusText(item.status)" size="small" />
        </view>
        <view class="item-body">
          <view class="item-row">
            <text class="label">来源</text>
            <text class="value">{{ outsourceSourceText(item.sourceType) }}</text>
          </view>
          <view class="item-row">
            <text class="label">外协厂商</text>
            <text class="value">{{ item.vendorName || '-' }}</text>
          </view>
          <view class="item-row" v-if="item.workorderCode">
            <text class="label">工单号</text>
            <text class="value">{{ item.workorderCode }}</text>
          </view>
          <view class="item-row">
            <text class="label">发料 / 收货</text>
            <text class="value bold">{{ item.issueTotalQty || 0 }} / {{ item.recptTotalQty || 0 }} 吨</text>
          </view>
          <view class="item-row" v-if="item.shipTime">
            <text class="label">发货时间</text>
            <text class="value">{{ formatTime(item.shipTime) }}</text>
          </view>
        </view>
        <view class="item-footer">
          <text class="text-grey">{{ item.shipBy || '-' }} · {{ formatTime(item.shipTime || item.createTime) }}</text>
          <text v-if="item.status === 'SHIPPED'" class="go-text">去收货 ›</text>
          <uni-icons v-else type="right" size="16" color="#ccc" />
        </view>
      </view>

      <uni-load-more :status="loadMoreStatus" />
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, getCurrentInstance } from 'vue'
import UniEasyInput from '@/uni_modules/uni-easyinput/components/uni-easyinput/uni-easyinput.vue'
import UniIcons from '@/uni_modules/uni-icons/components/uni-icons/uni-icons.vue'
import UniLoadMore from '@/uni_modules/uni-load-more/components/uni-load-more/uni-load-more.vue'
import UniTag from '@/uni_modules/uni-tag/components/uni-tag/uni-tag.vue'
import { onShow, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { listOutsource } from '@/api/mes/wm/outsource'
import { outsourceStatusText, outsourceStatusTagType, outsourceSourceText } from '@/utils/outsource.js'

const { proxy } = getCurrentInstance()
const list = ref([])
const loading = ref(false)
const loadMoreStatus = ref('more')
// activeStatus: __PENDING__ = 待收货(SHIPPED)；'' = 全部
const activeStatus = ref('__PENDING__')
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  orderCode: '',
  status: '',
  statusList: null
})

const statusFilters = [
  { label: '待收货', value: '__PENDING__' },
  { label: '全部', value: '' }
]

function formatTime(t) {
  if (!t) return ''
  return String(t).substring(5, 16)
}

function filterStatus(val) {
  activeStatus.value = val
  queryParams.pageNum = 1
  loadData(false)
}

function handleQuery() {
  queryParams.pageNum = 1
  loadData(false)
}

function applyStatusFilter() {
  if (activeStatus.value === '__PENDING__') {
    queryParams.status = ''
    queryParams.statusList = ['SHIPPED']
  } else {
    queryParams.status = ''
    queryParams.statusList = null
  }
}

function buildQuery() {
  const q = {
    pageNum: queryParams.pageNum,
    pageSize: queryParams.pageSize,
    orderCode: queryParams.orderCode
  }
  if (queryParams.statusList && queryParams.statusList.length) {
    q.statusList = queryParams.statusList
  }
  return q
}

function loadData(append = false) {
  loading.value = true
  applyStatusFilter()
  return listOutsource(buildQuery()).then(res => {
    const rows = res.rows || []
    list.value = append ? list.value.concat(rows) : rows
    loading.value = false
    loadMoreStatus.value = rows.length >= queryParams.pageSize ? 'more' : 'noMore'
  }).catch(() => {
    loading.value = false
    loadMoreStatus.value = 'more'
  })
}

function goReceive(item) {
  if (item.status !== 'SHIPPED') {
    proxy.$modal.msgError('仅"已发货"状态的外协单可收货')
    return
  }
  // 分切来源走专用收货页（子卷入库+母卷消耗+报工）；通用外协走通用收货页
  if (item.sourceType === 'SLITTING' && item.sourceRefId) {
    proxy.$tab.navigateTo('/pages/mes/pro/slitting-receive?slitId=' + item.sourceRefId)
  } else {
    proxy.$tab.navigateTo('/pages/mes/wm/outsource-receive?orderId=' + item.orderId)
  }
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
.go-text { color: #007aff; font-size: 26rpx; }
.text-grey { color: #999; font-size: 24rpx; }
</style>
