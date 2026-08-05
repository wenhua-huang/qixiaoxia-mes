<template>
  <view class="container">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <uni-easyinput
        v-model="queryParams.orderCode"
        placeholder="外协单号"
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

    <!-- 列表 -->
    <view v-if="loading" class="loading-box">
      <uni-load-more status="loading" />
    </view>

    <view v-else-if="list.length === 0" class="empty-box">
      <text class="text-grey">暂无外协任务</text>
    </view>

    <view v-else class="list-box">
      <view v-for="item in list" :key="item.orderId" class="list-item" @click="goDetail(item)">
        <view class="item-header">
          <text class="bold">{{ item.orderCode }}</text>
          <uni-tag :type="statusTagType(item.status)" :text="statusText(item.status)" size="small" />
        </view>
        <view class="item-body">
          <view class="item-row">
            <text class="label">来源</text>
            <text class="value">{{ sourceText(item.sourceType) }}</text>
          </view>
          <view class="item-row">
            <text class="label">外协厂商</text>
            <text class="value">{{ item.vendorName || '-' }}</text>
          </view>
          <view class="item-row">
            <text class="label">发料总量</text>
            <text class="value bold">{{ item.issueTotalQty || 0 }} {{ item.issueTotalQty ? '吨' : '' }}</text>
          </view>
          <view class="item-row" v-if="item.recptTotalQty > 0">
            <text class="label">收货总量</text>
            <text class="value">{{ item.recptTotalQty }}吨</text>
          </view>
        </view>
        <view class="item-footer">
          <text class="text-grey">{{ item.operator || '-' }} · {{ formatTime(item.issueTime || item.createTime) }}</text>
        </view>
        <!-- 厂商视角且状态=已发料 → 录入结果 -->
        <view v-if="isVendorRole && item.status === 'ISSUED'" class="item-actions">
          <button class="cu-btn bg-blue sm" @click.stop="goResult(item)">录入结果</button>
        </view>
        <!-- 我方视角且状态=加工中 → 收货 -->
        <view v-if="!isVendorRole && item.status === 'PROCESSING'" class="item-actions">
          <button class="cu-btn bg-green sm" @click.stop="goReceive(item)">收货入库</button>
        </view>
      </view>

      <uni-load-more :status="loadMoreStatus" />
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, getCurrentInstance } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { useUserStore } from '@/store'
import { listOutsource } from '@/api/mes/wm/outsource'

const { proxy } = getCurrentInstance()
const userStore = useUserStore()
const isVendorRole = computed(() => !!userStore.vendorId)

const list = ref([])
const loading = ref(false)
const loadMoreStatus = ref('more')
const queryParams = reactive({
  pageNum: 1, pageSize: 10,
  orderCode: '', status: ''
})

const statusFilters = [
  { label: '全部', value: '' },
  { label: '已发料', value: 'ISSUED' },
  { label: '加工中', value: 'PROCESSING' },
  { label: '已收货', value: 'RECEIVED' }
]

const STATUS_MAP = { ISSUED: '已发料', PROCESSING: '加工中', RECEIVED: '已收货' }
function statusText(s) { return STATUS_MAP[s] || s || '' }
function statusTagType(s) {
  const m = { ISSUED: 'warning', PROCESSING: 'primary', RECEIVED: 'success' }
  return m[s] || 'default'
}
const SOURCE_MAP = { GENERIC: '通用', SLITTING: '分切', PRINTING: '印刷' }
function sourceText(s) { return SOURCE_MAP[s] || s || '-' }
function formatTime(t) {
  if (!t) return ''
  return t.substring(5, 16)
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
  listOutsource(queryParams).then(res => {
    list.value = res.rows || []
    loading.value = false
    loadMoreStatus.value = (res.rows && res.rows.length >= queryParams.pageSize) ? 'more' : 'noMore'
  }).catch(() => { loading.value = false })
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
  listOutsource(queryParams).then(res => {
    const rows = res.rows || []
    list.value = list.value.concat(rows)
    loadMoreStatus.value = rows.length >= queryParams.pageSize ? 'more' : 'noMore'
  }).catch(() => { queryParams.pageNum--; loadMoreStatus.value = 'more' })
})

function goDetail(item) {
  proxy.$tab.navigateTo('/pages/mes/wm/outsource-detail?orderId=' + item.orderId)
}
function goResult(item) {
  proxy.$tab.navigateTo('/pages/mes/wm/outsource-result?orderId=' + item.orderId)
}
function goReceive(item) {
  proxy.$tab.navigateTo('/pages/mes/wm/outsource-receive?orderId=' + item.orderId)
}

onLoad(() => { loadData() })
</script>

<style lang="scss" scoped>
page { background-color: #f5f6f7; min-height: 100%; }
.search-bar { display: flex; align-items: center; padding: 20rpx 24rpx; gap: 16rpx; background: #fff; }
.search-input { flex: 1; }
.search-btn { margin: 0; font-size: 26rpx; height: 64rpx; line-height: 64rpx; }
.filter-row { display: flex; flex-wrap: wrap; gap: 16rpx; padding: 16rpx 24rpx; background: #fff; border-bottom: 1px solid #f0f0f0; }
.filter-tag { padding: 8rpx 24rpx; border-radius: 32rpx; font-size: 24rpx; color: #666; background: #f5f5f5; }
.filter-tag.active { color: #fff; background: #007aff; }
.loading-box, .empty-box { display: flex; justify-content: center; padding: 80rpx 0; }
.list-box { padding: 16rpx 24rpx; }
.list-item { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 20rpx; }
.item-header { display: flex; justify-content: space-between; align-items: center; padding-bottom: 16rpx; border-bottom: 1px solid #f5f5f5; }
.item-body { padding: 16rpx 0; }
.item-row { display: flex; justify-content: space-between; padding: 6rpx 0; }
.label { color: #999; font-size: 26rpx; }
.value { font-size: 26rpx; color: #333; }
.bold { font-weight: 600; }
.item-footer { display: flex; justify-content: space-between; align-items: center; padding-top: 12rpx; }
.text-grey { color: #999; font-size: 24rpx; }
.item-actions { display: flex; justify-content: flex-end; padding-top: 16rpx; gap: 16rpx; }
.cu-btn.sm { font-size: 24rpx; height: 56rpx; line-height: 56rpx; }
.bg-blue { background: #007aff; color: #fff; }
.bg-green { background: #67c23a; color: #fff; }
</style>
