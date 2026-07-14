<template>
  <view class="container">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <uni-easyinput
        v-model="queryParams.keyword"
        placeholder="工单号/入库单号"
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
    </view>

    <!-- 加载中 -->
    <view v-if="loading" class="loading-box">
      <uni-load-more status="loading" />
    </view>

    <!-- 空状态 -->
    <view v-else-if="list.length === 0" class="empty-box">
      <text class="text-grey">暂无入库记录</text>
    </view>

    <!-- 列表 -->
    <view v-else class="list-box">
      <view v-for="item in list" :key="item.recptId" class="list-item" @click="viewDetail(item)">
        <view class="item-header">
          <text class="bold">{{ item.recptCode }}</text>
          <uni-tag :type="recptStatusTagType(item.status)" :text="recptStatusText(item.status)" size="small" />
        </view>
        <view class="item-body">
          <view class="item-row">
            <text class="label">工单号</text>
            <text class="value">{{ item.workorderCode || '-' }}</text>
          </view>
          <view class="item-row">
            <text class="label">产出编号</text>
            <text class="value">{{ item.produceCode || '-' }}</text>
          </view>
          <view class="item-row">
            <text class="label">仓库</text>
            <text class="value">{{ item.warehouseName || '-' }}</text>
          </view>
          <view class="item-row">
            <text class="label">入库数量 / 箱数</text>
            <text class="value bold">{{ item.totalQuantity || 0 }} / {{ item.totalBox || 0 }}</text>
          </view>
        </view>
        <view class="item-footer">
          <text class="text-grey">{{ item.recptDate || item.createTime }}</text>
          <uni-icons type="right" size="16" color="#ccc" />
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
import { onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { listProductRecpt } from '@/api/mes/wm/productrecpt'
import { recptStatusText, recptStatusTagType } from '@/utils/wm-productrecpt.js'

const { proxy } = getCurrentInstance()
const list = ref([])
const loading = ref(false)
const loadMoreStatus = ref('more')
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: ''
})

const statusFilters = [
  { label: '全部', value: '' },
  { label: '草稿', value: 'DRAFT' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '已过账', value: 'POSTED' }
]

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
  // 产品入库按工单号搜索（产线工人扫码工单为主场景）。
  // 注意：Mapper XML 中 workorderCode 和 recptCode 为 AND 关系，
  // 同时赋值会导致两个 LIKE 条件 AND，搜不到结果。
  const params = { ...queryParams }
  if (params.keyword) {
    params.workorderCode = params.keyword
  }
  delete params.keyword
  listProductRecpt(params).then(res => {
    list.value = res.rows || []
    loading.value = false
    loadMoreStatus.value = (res.rows && res.rows.length >= queryParams.pageSize) ? 'more' : 'noMore'
  }).catch(() => {
    loading.value = false
  })
}

function viewDetail(item) {
  proxy.$tab.navigateTo('/pages/mes/wm/productrecpt/detail?recptId=' + item.recptId)
}

// 扫码
function handleScan() {
  // #ifdef APP-PLUS || H5
  uni.scanCode({
    onlyFromCamera: false,
    scanType: ['barCode', 'qrCode'],
    success: (res) => {
      queryParams.keyword = res.result
      handleQuery()
    },
    fail: (err) => { console.log('扫码取消:', err) }
  })
  // #endif
  // #ifdef MP-WEIXIN
  uni.scanCode({
    success: (res) => {
      queryParams.keyword = res.result
      handleQuery()
    }
  })
  // #endif
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
  listProductRecpt(queryParams).then(res => {
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
.scan-btn {
  width: 72rpx; height: 72rpx;
  display: flex; align-items: center; justify-content: center;
  border: 1px solid #e5e5e5; border-radius: 12rpx;
  background: #fff;
  padding: 0; margin: 0;
}
.scan-btn::after { border: none; }
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
