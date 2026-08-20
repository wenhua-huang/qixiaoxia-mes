<template>
  <view class="container">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <uni-easyinput
        v-model="queryParams.orderCode"
        placeholder="输入PO单号"
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
      <text class="text-grey">暂无待收货订单</text>
    </view>

    <!-- 列表 -->
    <view v-else class="list-box">
      <view v-for="item in list" :key="item.orderId" class="list-item" @click="goReceipt(item)">
        <view class="item-header">
          <text class="bold">{{ item.orderCode }}</text>
          <uni-tag :type="orderStatusTagType(item.status)" :text="orderStatusText(item.status)" size="small" />
        </view>
        <view class="item-body">
          <view class="item-row">
            <text class="label">供应商</text>
            <text class="value">{{ item.vendorName || '-' }}</text>
          </view>
          <view class="item-row">
            <text class="label">采购类型</text>
            <text class="value">{{ purchaseTypeText(item.purchaseType) }}</text>
          </view>
          <view class="item-row">
            <text class="label">订购 / 已收</text>
            <text class="value bold">{{ item.totalQuantity || 0 }} / {{ item.receivedQuantity || 0 }}</text>
          </view>
          <view class="item-row">
            <text class="label">预计到货</text>
            <text class="value">{{ formatDate(item.expectedDate) || '-' }}</text>
          </view>
        </view>
        <view class="item-footer">
          <text class="text-grey">{{ formatDate(item.orderDate) || item.createTime }}</text>
          <text v-if="canReceive(item.status)" class="go-text">去收货 ›</text>
          <uni-icons v-else type="right" size="16" color="#ccc" />
        </view>
      </view>

      <uni-load-more :status="loadMoreStatus" />
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, getCurrentInstance } from 'vue'
// 显式引入 uni-ui 组件（绕过 HBuilderX 发行 H5 时 easycom 失效）
import UniEasyInput from '@/uni_modules/uni-easyinput/components/uni-easyinput/uni-easyinput.vue'
import UniIcons from '@/uni_modules/uni-icons/components/uni-icons/uni-icons.vue'
import UniLoadMore from '@/uni_modules/uni-load-more/components/uni-load-more/uni-load-more.vue'
import UniTag from '@/uni_modules/uni-tag/components/uni-tag/uni-tag.vue'
import { onShow, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { listOrder } from '@/api/mes/pur/order'
import { purchaseTypeText, orderStatusText, orderStatusTagType, canReceive } from '@/utils/pur.js'

const { proxy } = getCurrentInstance()
const list = ref([])
const loading = ref(false)
const loadMoreStatus = ref('more')
// activeStatus 是当前 tab 的逻辑值；__PENDING__ 代表待收货聚合（ORDERED+RECEIVING）
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
  { label: '全部', value: '' },
  { label: '已下单', value: 'ORDERED' },
  { label: '收货中', value: 'RECEIVING' },
  { label: '已收货', value: 'RECEIVED' }
]

function formatDate(v) {
  if (!v) return ''
  // 后端返回 2026-08-19T00:00:00 或时间戳，统一截到日期
  return String(v).substring(0, 10)
}

function filterStatus(val) {
  activeStatus.value = val
  queryParams.pageNum = 1
  applyStatusFilter()
  loadData()
}

function handleQuery() {
  queryParams.pageNum = 1
  loadData()
}

// 把 activeStatus 翻译成后端查询参数：待收货走 statusList 多值，其余走单值 status
function applyStatusFilter() {
  if (activeStatus.value === '__PENDING__') {
    queryParams.status = ''
    queryParams.statusList = ['ORDERED', 'RECEIVING']
  } else {
    queryParams.status = activeStatus.value
    queryParams.statusList = null
  }
}

function buildQuery() {
  const q = {
    pageNum: queryParams.pageNum,
    pageSize: queryParams.pageSize,
    orderCode: queryParams.orderCode
  }
  if (queryParams.statusList) {
    q.statusList = queryParams.statusList
  } else if (queryParams.status) {
    q.status = queryParams.status
  }
  return q
}

function loadData(append = false) {
  loading.value = true
  applyStatusFilter()
  return listOrder(buildQuery()).then(res => {
    const rows = res.rows || []
    list.value = append ? list.value.concat(rows) : rows
    loading.value = false
    loadMoreStatus.value = rows.length >= queryParams.pageSize ? 'more' : 'noMore'
  }).catch(() => {
    loading.value = false
    loadMoreStatus.value = 'more'
  })
}

function goReceipt(item) {
  if (!canReceive(item.status)) {
    proxy.$modal.msgError('仅"已下单/收货中"的订单可收货')
    return
  }
  proxy.$tab.navigateTo('/pages/mes/pur/receipt?orderId=' + item.orderId)
}

// 收货返回后自动刷新列表
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
