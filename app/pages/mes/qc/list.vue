<template>
  <view class="qc-page">
    <view class="top-bar">
      <view class="tabs">
        <view class="tab" :class="{ active: tab === 'IPQC' }" @click="switchTab('IPQC')">过程检 IPQC</view>
        <view class="tab" :class="{ active: tab === 'IQC' }" @click="switchTab('IQC')">来料检 IQC</view>
      </view>
      <view class="top-actions">
        <uni-icons type="scan" size="26" color="#409eff" @click="goScan" />
        <view class="history-btn" @click="goHistory">
          <uni-icons type="clock" size="18" color="#409eff" />
          <text>检验历史</text>
        </view>
      </view>
    </view>

    <scroll-view scroll-y class="list-wrap" @scrolltolower="loadMore" refresher-enabled :refresher-triggered="refresherTriggered" @refresherrefresh="onRefresh">
      <view v-if="!list.length && !loading" class="empty">暂无待检单</view>
      <view v-for="item in list" :key="item.iqcId || item.ipqcId" class="card" @click="openItem(item)">
        <view class="card-head">
          <text class="code">{{ item.iqcCode || item.ipqcCode }}</text>
          <uni-tag :type="statusTagType(item.status)" :text="statusText(item.status)" size="small" />
        </view>
        <view class="card-line" v-if="tab === 'IPQC'">
          <text>{{ item.itemName }}</text>
          <text v-if="item.processName" class="muted"> · {{ item.processName }}</text>
        </view>
        <view class="card-line" v-else>
          <text>{{ item.itemName }}</text>
          <text v-if="item.vendorName" class="muted"> · {{ item.vendorName }}</text>
        </view>
        <view class="card-line sub">
          <text class="muted">来源：{{ item.sourceDocCode || '—' }}</text>
          <text class="muted time">{{ fmtTime(item.createTime) }}</text>
        </view>
      </view>
      <view v-if="loading" class="loading-tip">加载中…</view>
      <view v-else-if="noMore && list.length" class="loading-tip">没有更多了</view>
    </scroll-view>

    <view v-if="tab === 'IPQC'" class="fab" @click="goCreate">
      <uni-icons type="plusempty" size="28" color="#fff" />
      <text>手工建单</text>
    </view>
  </view>
</template>

<script setup>
import { ref, getCurrentInstance } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import uniIcons from '@/uni_modules/uni-icons/components/uni-icons/uni-icons.vue'
import uniTag from '@/uni_modules/uni-tag/components/uni-tag/uni-tag.vue'
import { listIqc, listIpqc, scanIqc } from '@/api/mes/qc'
import { getCardScanResult } from '@/api/mes/pro/procard'
import { parseQrPayload } from '@/utils/qrPayload'
import { qcStatusText, qcStatusTagType } from '@/utils/qc'

const { proxy } = getCurrentInstance()

const tab = ref('IPQC')
const list = ref([])
const loading = ref(false)
const noMore = ref(false)
const refresherTriggered = ref(false)
const pageNum = ref(1)
const pageSize = 10
let loadSeq = 0

function statusText(s) { return qcStatusText(s) }
function statusTagType(s) { return qcStatusTagType(s) }

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
  // list 端点 status 仅支持精确匹配，分别查 PENDING/INSPECTING 后合并，按创建时间倒序
  const api = tab.value === 'IPQC' ? listIpqc : listIqc
  const baseQ = { pageNum: pageNum.value, pageSize }
  Promise.all([
    api({ ...baseQ, status: 'PENDING' }),
    api({ ...baseQ, status: 'INSPECTING' })
  ]).then(([r1, r2]) => {
    if (seq !== loadSeq) return
    const rows = [...(r1.rows || []), ...(r2.rows || [])]
    rows.sort((a, b) => String(b.createTime || '').localeCompare(String(a.createTime || '')))
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
function goHistory() { proxy.$tab.navigateTo('/pages/mes/qc/history') }
function goCreate() { proxy.$tab.navigateTo('/pages/mes/qc/ipqc-create') }

function fmtTime(t) { return t ? String(t).replace('T', ' ').substring(5, 16) : '' }

// ===== 扫码路由 =====
function goScan() {
  // #ifdef H5
  uni.navigateTo({ url: '/pages/mes/pro/scan?callback=1', events: { scanResult: (code) => resolveCode(code) } })
  // #endif
  // #ifndef H5
  uni.scanCode({
    onlyFromCamera: false, scanType: ['barCode', 'qrCode'],
    success: (res) => resolveCode(res.result),
    fail: () => {}
  })
  // #endif
}

function resolveCode(raw) {
  const code = (raw || '').trim()
  if (!code) return
  // 解析 QXX|<TYPE>|<CODE> 结构，按类型分流避免无谓的错误提示
  const payload = parseQrPayload(code)
  const realCode = payload ? payload.code : code
  const qrType = payload ? payload.type : null

  uni.showLoading({ title: '查单中…' })
  if (qrType === 'CARD' || qrType === 'WO') {
    resolveCard(realCode)
  } else {
    // 未知类型或 MAT/PKG/ROLL：先当收货单试，失败再试流转卡
    scanIqc(realCode).then(res => {
      uni.hideLoading()
      const iqcList = res.data?.iqcList || []
      if (iqcList.length) {
        const target = iqcList.find(i => i.status !== 'CLOSED') || iqcList[0]
        proxy.$tab.navigateTo(`/pages/mes/qc/inspect?type=IQC&id=${target.iqcId}`)
      } else {
        proxy.$modal.msgWarning('该收货单未生成检验单或已免检')
      }
    }).catch(() => resolveCard(realCode))
  }
}

function resolveCard(realCode) {
  getCardScanResult(realCode).then(res => {
    const d = res.data
    if (!d || !d.card) {
      uni.hideLoading()
      proxy.$modal.msgError('未找到对应检验单或流转卡：' + realCode); return
    }
    const card = d.card
    // 查该卡是否已有进行中（PENDING/INSPECTING）IPQC
    return listIpqc({ cardCode: card.cardCode, pageNum: 1, pageSize: 20 })
      .then(r => {
        uni.hideLoading()
        const rows = (r.rows || []).filter(x => x.status === 'PENDING' || x.status === 'INSPECTING')
        if (rows.length) {
          proxy.$tab.navigateTo(`/pages/mes/qc/inspect?type=IPQC&id=${rows[0].ipqcId}`)
        } else {
          proxy.$tab.navigateTo(`/pages/mes/qc/ipqc-create?cardCode=${encodeURIComponent(card.cardCode)}`)
        }
      })
  }).catch(() => { uni.hideLoading(); proxy.$modal.msgError('查单失败') })
}
</script>

<style lang="scss" scoped>
page { background: #f5f6f7; min-height: 100%; }
.qc-page { display: flex; flex-direction: column; height: 100vh; }
.top-bar { display: flex; align-items: center; justify-content: space-between; padding: 16rpx 24rpx; background: #fff; }
.tabs { display: flex; gap: 24rpx; }
.tab { font-size: 28rpx; color: #606266; padding: 12rpx 0; position: relative;
  &.active { color: #409eff; font-weight: 600; &::after { content:''; position:absolute; bottom:0; left:20%; right:20%; height:4rpx; background:#409eff; border-radius:2rpx; } } }
.top-actions { display: flex; gap: 28rpx; align-items: center; }
.history-btn { display: flex; align-items: center; gap: 6rpx; font-size: 24rpx; color: #409eff;
  border: 2rpx solid #b3d8ff; background: #ecf5ff; border-radius: 24rpx; padding: 6rpx 18rpx; }
.list-wrap { flex: 1; padding: 20rpx 24rpx 140rpx; }
.empty { text-align: center; color: #999; padding: 120rpx 0; font-size: 28rpx; }
.card { background: #fff; border-radius: 12rpx; padding: 24rpx; margin-bottom: 20rpx; }
.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.code { font-size: 30rpx; font-weight: 600; color: #303133; }
.card-line { font-size: 28rpx; color: #303133; margin-bottom: 8rpx; display: flex; justify-content: space-between; }
.muted { color: #909399; font-size: 26rpx; }
.time { font-size: 24rpx; }
.loading-tip { text-align: center; color: #999; padding: 20rpx; font-size: 26rpx; }
.fab { position: fixed; right: 32rpx; bottom: 60rpx; background: #409eff; color: #fff;
  display: flex; align-items: center; gap: 8rpx; padding: 20rpx 28rpx; border-radius: 44rpx;
  font-size: 28rpx; box-shadow: 0 6rpx 20rpx rgba(64,158,255,.4); }
</style>
