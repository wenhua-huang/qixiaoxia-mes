<template>
  <view class="qc-page">
    <view class="top-bar">
      <view class="tabs">
        <view class="tab" :class="{ active: tab === 'IPQC' }" @click="switchTab('IPQC')">过程检</view>
        <view class="tab" :class="{ active: tab === 'IQC' }" @click="switchTab('IQC')">来料检</view>
      </view>
    </view>
    <scroll-view scroll-y class="list-wrap" @scrolltolower="loadMore" refresher-enabled :refresher-triggered="refresherTriggered" @refresherrefresh="onRefresh">
      <view v-if="!list.length && !loading" class="empty">暂无历史记录</view>
      <view v-for="item in list" :key="item.iqcId || item.ipqcId" class="card" @click="openItem(item)">
        <view class="card-head">
          <text class="code">{{ item.iqcCode || item.ipqcCode }}</text>
          <uni-tag :type="qcResultTagType(item.checkResult)" :text="qcResultText(item.checkResult)" size="small" />
        </view>
        <view class="card-line"><text>{{ item.itemName }}</text><text class="muted">{{ item.inspector || '—' }}</text></view>
        <view class="card-line sub"><text class="muted">{{ item.sourceDocCode || '—' }}</text><text class="muted time">{{ fmtTime(item.inspectDate) }}</text></view>
      </view>
      <view v-if="loading" class="loading-tip">加载中…</view>
      <view v-else-if="noMore && list.length" class="loading-tip">没有更多了</view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, getCurrentInstance } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import uniTag from '@/uni_modules/uni-tag/components/uni-tag/uni-tag.vue'
import { listIqc, listIpqc } from '@/api/mes/qc'
import { qcResultText, qcResultTagType } from '@/utils/qc'

const { proxy } = getCurrentInstance()
const tab = ref('IPQC')
const list = ref([])
const loading = ref(false)
const noMore = ref(false)
const refresherTriggered = ref(false)
const pageNum = ref(1)
const pageSize = 10
let loadSeq = 0

function switchTab(t) {
  if (tab.value === t) return
  tab.value = t
  reset()
  load()
}
function reset() { list.value = []; pageNum.value = 1; noMore.value = false }
function load() {
  const seq = ++loadSeq
  loading.value = true
  // status 仅支持精确匹配，分别查 COMPLETED/CLOSED 后合并，按检验时间倒序
  const api = tab.value === 'IPQC' ? listIpqc : listIqc
  const baseQ = { pageNum: pageNum.value, pageSize }
  Promise.all([
    api({ ...baseQ, status: 'COMPLETED' }),
    api({ ...baseQ, status: 'CLOSED' })
  ]).then(([r1, r2]) => {
    if (seq !== loadSeq) return
    const rows = [...(r1.rows || []), ...(r2.rows || [])]
    rows.sort((a, b) => String(b.inspectDate || '').localeCompare(String(a.inspectDate || '')))
    list.value = pageNum.value === 1 ? rows : list.value.concat(rows)
    noMore.value = rows.length < pageSize
  }).catch(() => {
    proxy.$modal.msgError('查询失败，请重试')
  }).finally(() => {
    if (seq !== loadSeq) return
    loading.value = false
    refresherTriggered.value = false
  })
}
function loadMore() {
  if (noMore.value || loading.value) return
  pageNum.value++
  load()
}
function onRefresh() { refresherTriggered.value = true; reset(); load() }
onShow(() => { reset(); load() })
function openItem(item) {
  const id = item.iqcId || item.ipqcId
  proxy.$tab.navigateTo(`/pages/mes/qc/inspect?type=${tab.value}&id=${id}`)
}
function fmtTime(t) { return t ? String(t).replace('T', ' ').substring(5, 16) : '' }
</script>

<style lang="scss" scoped>
page { background: #f5f6f7; min-height: 100%; }
.qc-page { display: flex; flex-direction: column; height: 100vh; }
.top-bar { background: #fff; padding: 16rpx 24rpx; }
.tabs { display: flex; gap: 24rpx; }
.tab { font-size: 28rpx; color: #606266; padding: 12rpx 0; position: relative;
  &.active { color: #409eff; font-weight: 600; &::after { content:''; position:absolute; bottom:0; left:20%; right:20%; height:4rpx; background:#409eff; border-radius:2rpx; } } }
.list-wrap { flex: 1; padding: 20rpx 24rpx; }
.empty { text-align: center; color: #999; padding: 120rpx 0; font-size: 28rpx; }
.card { background: #fff; border-radius: 12rpx; padding: 24rpx; margin-bottom: 20rpx; }
.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.code { font-size: 30rpx; font-weight: 600; color: #303133; }
.card-line { font-size: 28rpx; color: #303133; display: flex; justify-content: space-between; margin-bottom: 8rpx; }
.muted { color: #909399; font-size: 26rpx; }
.time { font-size: 24rpx; }
.loading-tip { text-align: center; color: #999; padding: 20rpx; font-size: 26rpx; }
</style>
