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
      <button class="scan-btn" @click="handleScan" size="mini">
        <uni-icons type="scan" size="20"></uni-icons>
      </button>
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
      <button v-if="!isVendorRole" class="filter-tag action-btn cu-btn bg-blue sm" @click="goCreate">＋ 发料</button>
      <button v-if="!isVendorRole" class="filter-tag action-btn cu-btn sm" @click="goExecute">草稿执行</button>
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
            <text class="value bold">{{ item.issueTotalQty || 0 }}吨</text>
          </view>
          <view class="item-row" v-if="item.recptTotalQty > 0">
            <text class="label">收货总量</text>
            <text class="value">{{ item.recptTotalQty }}吨</text>
          </view>
        </view>
        <view class="item-footer">
          <text class="text-grey">{{ item.operator || '-' }} · {{ formatTime(item.issueTime || item.createTime) }}</text>
        </view>
        <!-- 厂商视角：已发料→签收；已签收/加工中→录入结果；加工中→完成；已完成→发货 -->
        <view v-if="isVendorRole && item.status === 'ISSUED'" class="item-actions">
          <button class="cu-btn bg-blue sm" @click.stop="goVendorReceive(item)">签收</button>
        </view>
        <view v-if="isVendorRole && (item.status === 'VENDOR_RCVD' || item.status === 'PROCESSING')" class="item-actions">
          <button class="cu-btn bg-blue sm" @click.stop="goResult(item)">{{ item.status === 'VENDOR_RCVD' ? '录入结果' : '补录' }}</button>
          <button v-if="item.status === 'PROCESSING'" class="cu-btn bg-cyan sm" @click.stop="goComplete(item)">完成</button>
        </view>
        <view v-if="isVendorRole && item.status === 'FINISHED'" class="item-actions">
          <button class="cu-btn bg-green sm" @click.stop="goShip(item)">发货回厂</button>
        </view>
        <!-- 我方视角：仅 SHIPPED（厂商已发货）可收货 -->
        <view v-if="!isVendorRole && item.status === 'SHIPPED'" class="item-actions">
          <button class="cu-btn bg-green sm" @click.stop="goReceive(item)">收货入库</button>
        </view>
      </view>

      <uni-load-more :status="loadMoreStatus" />
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, getCurrentInstance } from 'vue'
import { onLoad, onShow, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { useUserStore } from '@/store'
import { listOutsource, vendorReceiveOutsource, completeOutsource, shipOutsource } from '@/api/mes/wm/outsource'
import { getCardScanResult } from '@/api/mes/pro/procard'
import { parseQrPayload } from '@/utils/qrPayload'

const { proxy } = getCurrentInstance()
const userStore = useUserStore()
const isVendorRole = computed(() => !!userStore.vendorId)

const list = ref([])
const loading = ref(false)
const loadMoreStatus = ref('more')
const queryParams = reactive({
  pageNum: 1, pageSize: 10,
  orderCode: '', status: '',
  workorderCode: '' // 扫流转卡/工单码定位用（搜索框不展示，手动搜索时清空）
})

const statusFilters = [
  { label: '全部', value: '' },
  { label: '草稿', value: 'DRAFT' },
  { label: '已发料', value: 'ISSUED' },
  { label: '已签收', value: 'VENDOR_RCVD' },
  { label: '加工中', value: 'PROCESSING' },
  { label: '已完成', value: 'FINISHED' },
  { label: '已发货', value: 'SHIPPED' },
  { label: '已收货', value: 'RECEIVED' }
]

const STATUS_MAP = { DRAFT: '草稿', ISSUED: '已发料', VENDOR_RCVD: '已签收', PROCESSING: '加工中', FINISHED: '已完成', SHIPPED: '已发货', RECEIVED: '已收货', CLOSED: '已关闭' }
function statusText(s) { return STATUS_MAP[s] || s || '' }
function statusTagType(s) {
  const m = { DRAFT: 'default', ISSUED: 'warning', VENDOR_RCVD: 'warning', PROCESSING: 'primary', FINISHED: 'primary', SHIPPED: 'warning', RECEIVED: 'success', CLOSED: 'default' }
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
  // 手动按外协单号搜索：清空扫卡定位的工单过滤，避免两个条件 AND 搜不到
  queryParams.workorderCode = ''
  queryParams.pageNum = 1
  loadData()
}

function loadData() {
  loading.value = true
  return listOutsource(queryParams).then(res => {
    list.value = res.rows || []
    loading.value = false
    loadMoreStatus.value = (res.rows && res.rows.length >= queryParams.pageSize) ? 'more' : 'noMore'
  }).catch(() => { loading.value = false })
}

// 统一扫码结果入口：识别 QXX|TYPE|CODE 载荷分发（CARD/WO → 反查工单号过滤外协单，其他按原文搜单号）
function handleCode(code) {
  const payload = parseQrPayload(code)
  if (payload && (payload.type === 'CARD' || payload.type === 'WO')) {
    lookupWorkorderByCard(payload.code)
    return
  }
  // 裸条码（非 QXX 载荷）：维持旧行为，orderCode=原文按外协单号搜索
  queryParams.orderCode = code
  handleQuery()
}

// 扫流转卡/工单码 → 反查工单号并过滤该工单的外协单（失败由 request 拦截器 toast；无卡这里兜底提示）
async function lookupWorkorderByCard(cardCode) {
  let data = null
  try {
    const res = await getCardScanResult(cardCode)
    data = res.data || {}
  } catch (e) { return }
  const woCode = data.card && data.card.workorderCode
  if (!woCode) {
    proxy.$modal.msgError('未找到流转卡：' + cardCode)
    return
  }
  queryParams.orderCode = ''
  queryParams.workorderCode = woCode
  queryParams.pageNum = 1
  proxy.$modal.msg('已按工单 ' + woCode + ' 过滤')
  loadData()
}

// 扫码：H5 跳统一相机扫码页（html5-qrcode，回调取结果）；App/小程序用原生 uni.scanCode
function handleScan() {
  // #ifdef H5
  uni.navigateTo({
    url: '/pages/mes/pro/scan?callback=1',
    events: { scanResult: (code) => handleCode(code) }
  })
  // #endif
  // #ifndef H5
  uni.scanCode({
    onlyFromCamera: false,
    scanType: ['barCode', 'qrCode'],
    success: (res) => { handleCode(res.result) },
    fail: (err) => { console.log('扫码取消:', err) }
  })
  // #endif
}

onPullDownRefresh(async () => {
  queryParams.pageNum = 1
  await loadData()
  uni.stopPullDownRefresh()
})
onReachBottom(() => {
  if (loading.value || loadMoreStatus.value === 'loading') return
  if (loadMoreStatus.value === 'noMore') {
    uni.showToast({ title: '没有更多了', icon: 'none' })
    return
  }
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
  // 分切来源走专用收货页（子卷入库+母卷消耗+报工）；通用外协走通用收货页
  if (item.sourceType === 'SLITTING' && item.sourceRefId) {
    proxy.$tab.navigateTo('/pages/mes/pro/slitting-receive?slitId=' + item.sourceRefId)
  } else {
    proxy.$tab.navigateTo('/pages/mes/wm/outsource-receive?orderId=' + item.orderId)
  }
}
function goCreate() {
  proxy.$tab.navigateTo('/pages/mes/wm/outsource-create')
}
function goExecute() {
  proxy.$tab.navigateTo('/pages/mes/wm/outsource-execute')
}
function goVendorReceive(item) {
  uni.showModal({
    title: '确认签收', content: '确认已收到该外协发料？签收后可录入加工结果。',
    success: (r) => {
      if (!r.confirm) return
      uni.showLoading({ title: '签收中...' })
      vendorReceiveOutsource(item.orderId).then(() => {
        uni.hideLoading()
        uni.showToast({ title: '已签收', icon: 'success' })
        loadData()
      }).catch(() => uni.hideLoading())
    }
  })
}
function goComplete(item) {
  uni.showModal({
    title: '确认完成', content: '确认完成加工？完成后可发货回厂。',
    success: (r) => {
      if (!r.confirm) return
      uni.showLoading({ title: '提交中...' })
      completeOutsource(item.orderId).then(() => {
        uni.hideLoading()
        uni.showToast({ title: '已完成，可发货', icon: 'success' })
        loadData()
      }).catch(() => uni.hideLoading())
    }
  })
}
function goShip(item) {
  uni.showModal({
    title: '确认发货', content: '确认已将加工完成的物料发回工厂？发货后不可修改。',
    success: (r) => {
      if (!r.confirm) return
      uni.showLoading({ title: '发货中...' })
      shipOutsource(item.orderId).then(() => {
        uni.hideLoading()
        uni.showToast({ title: '已发货', icon: 'success' })
        loadData()
      }).catch(() => uni.hideLoading())
    }
  })
}

// 仅在 onShow 加载（onLoad 与 onShow 首次都会触发，避免双加载）
onLoad(() => {})
// 从详情/录结果页返回时自动刷新列表；重置到第 1 页避免分页状态残留
onShow(() => {
  queryParams.pageNum = 1
  list.value = []
  loadData()
})
</script>

<style lang="scss" scoped>
page { background-color: #f5f6f7; min-height: 100%; }
.search-bar { display: flex; align-items: center; padding: 20rpx 24rpx; gap: 16rpx; background: #fff; }
.search-input { flex: 1; }
.scan-btn { width: 72rpx; height: 72rpx; display: flex; align-items: center; justify-content: center; border: 1px solid #e5e5e5; border-radius: 12rpx; background: #fff; padding: 0; margin: 0; }
.scan-btn::after { border: none; }
.search-btn { margin: 0; font-size: 26rpx; height: 64rpx; line-height: 64rpx; }
.filter-row { display: flex; flex-wrap: wrap; gap: 16rpx; padding: 16rpx 24rpx; background: #fff; border-bottom: 1px solid #f0f0f0; }
.filter-tag { padding: 8rpx 24rpx; border-radius: 32rpx; font-size: 24rpx; color: #666; background: #f5f5f5; }
.filter-tag.active { color: #fff; background: #007aff; }
.action-btn { color: #fff; border: none; margin-left: auto; }
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
