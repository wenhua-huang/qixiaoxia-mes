<template>
  <div class="app-container">
    <!-- 顶部：返回 + 单号 + 状态 + 导出 -->
    <el-page-header @back="goBack" class="mb16">
      <template #content>
        <span class="header-title">销售订单详情</span>
        <el-tag v-if="order.orderCode" type="info" class="ml8">{{ order.orderCode }}</el-tag>
        <el-tag v-if="order.status" :type="statusTagType" class="ml8">{{ statusLabel }}</el-tag>
      </template>
      <template #extra>
        <el-button type="primary" plain icon="Printer" @click="exportPdf" v-hasPermi="['mes:sal:order:exportDetail']">导出 PDF</el-button>
        <el-button type="success" plain icon="Document" @click="exportExcel" v-hasPermi="['mes:sal:order:exportDetail']">导出 Excel</el-button>
      </template>
    </el-page-header>

    <div v-loading="loading">
      <!-- 驳回原因提醒（退回待提交时展示最近一次驳回意见） -->
      <el-alert v-if="order.status === 'PREPARE' && order.approveRemark" :title="'驳回原因：' + order.approveRemark"
        type="warning" :closable="false" show-icon class="mb16" />

      <el-card shadow="never" class="mb16">
        <template #header><span>订单信息</span></template>
        <el-descriptions :column="3" size="small" border>
          <el-descriptions-item label="订单名称">{{ order.orderName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="订单类型">{{ orderTypeText }}</el-descriptions-item>
          <el-descriptions-item label="订单来源">{{ sourceText }}</el-descriptions-item>
          <el-descriptions-item label="客户编码">{{ order.clientCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="客户名称">{{ order.clientName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="客户PO号">{{ order.clientOrderCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="业务员">{{ order.salesperson || '-' }}</el-descriptions-item>
          <el-descriptions-item label="业务线">{{ businessLineText }}</el-descriptions-item>
          <el-descriptions-item label="是否有样品">{{ order.sampleFlag === 'Y' ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="订单日期">{{ fmtDate(order.orderDate) }}</el-descriptions-item>
          <el-descriptions-item label="需求交期">{{ fmtDate(order.requestDate) }}</el-descriptions-item>
          <el-descriptions-item label="总金额">
            <span style="font-weight:bold;color:#f56c6c">{{ order.totalAmount ?? '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="付款方式">{{ order.paymentMethod || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ order.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 审核信息：审批人/审批时间/审批意见 -->
      <el-card shadow="never" class="mb16">
        <template #header><span>审核信息</span></template>
        <el-descriptions :column="3" size="small" border>
          <el-descriptions-item label="审核人">
            <span v-if="order.approveBy">{{ order.approveBy }}</span>
            <span v-else style="color:#909399">{{ approveHint }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="审核时间">
            <span v-if="order.approveTime">{{ fmtDateTime(order.approveTime) }}</span>
            <span v-else style="color:#909399">{{ approveHint }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="审核意见">{{ order.approveRemark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 明细行 -->
      <el-card shadow="never">
        <template #header><span>明细行（共 {{ lines.length }} 行）</span></template>
        <el-table :data="lines" size="small" border>
          <el-table-column label="行号" align="center" prop="lineNo" width="60" />
          <el-table-column label="产品编码" align="center" prop="productCode" width="140" />
          <el-table-column label="产品名称" align="center" prop="productName" :show-overflow-tooltip="true" />
          <el-table-column label="规格" align="center" prop="productSpc" width="120" :show-overflow-tooltip="true" />
          <el-table-column label="数量" align="center" prop="quantity" width="90" />
          <el-table-column label="单位" align="center" prop="unitName" width="70" />
          <el-table-column label="单价" align="center" prop="unitPrice" width="90" />
          <el-table-column label="行金额" align="center" prop="lineAmount" width="100" />
          <el-table-column label="已转工单" align="center" prop="quantityProduced" width="90">
            <template #default="s">{{ s.row.quantityProduced || 0 }}</template>
          </el-table-column>
          <el-table-column label="可转数量" align="center" prop="quantityConvertible" width="90" />
          <el-table-column label="需求交期" align="center" width="110">
            <template #default="s">{{ fmtDate(s.row.requestDate) }}</template>
          </el-table-column>
          <el-table-column label="尺寸" align="center" prop="productSize" width="130" :show-overflow-tooltip="true" />
          <el-table-column label="印刷要求" align="center" prop="printingReq" min-width="120" :show-overflow-tooltip="true" />
          <el-table-column label="包装要求" align="center" prop="packageReq" min-width="120" :show-overflow-tooltip="true" />
        </el-table>
      </el-card>

      <!-- 创建/更新审计 -->
      <div class="audit-footer">
        创建人：{{ order.createBy || '-' }}　创建时间：{{ fmtDateTime(order.createTime) }}
        <span v-if="order.updateBy">　|　更新人：{{ order.updateBy }}　更新时间：{{ fmtDateTime(order.updateTime) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail } from '@/api/mes/sal/order'
import type { SalOrder, SalOrderLine } from '@/types'

const { proxy } = getCurrentInstance() as any
const { mes_sal_order_status: statusDict } = proxy.useDict('mes_sal_order_status')

const route = useRoute()
const router = useRouter()
const orderId = computed(() => Number(route.query.orderId))
const loading = ref(false)
const order = ref<SalOrder>({} as SalOrder)
const lines = ref<SalOrderLine[]>([])

const statusLabel = computed(() => {
  const v = order.value.status
  const d = statusDict.value?.find((x: any) => x.value === v || x.dictValue === v)
  return d ? (d.label || d.dictLabel) : (v || '')
})
const statusTagType = computed(() => {
  const map: Record<string, string> = { PREPARE: 'info', PENDING: 'warning', CONFIRMED: 'success', CLOSED: 'primary', CANCEL: 'danger' }
  return map[order.value.status || ''] || 'info'
})
/** 审核人/时间为空时的占位提示 */
const approveHint = computed(() => {
  const s = order.value.status
  if (s === 'PENDING') return '待审核'
  if (s === 'PREPARE') return '未提交'
  return '-'
})
const orderTypeText = computed(() => ({ NEW: '新单', REPEAT: '返单' }[order.value.orderType || ''] || order.value.orderType || '-'))
const businessLineText = computed(() => ({ DOMESTIC: '内贸', FOREIGN: '外贸', SPOT: '现货' }[order.value.businessLine || ''] || order.value.businessLine || '-'))
const sourceText = computed(() => ({ 1: '直接新增', 2: 'CRM系统' }[String(order.value.source)] || '-'))

function fmtDate(v?: string): string {
  return v ? proxy.parseTime(v, '{y}-{m}-{d}') : '-'
}
function fmtDateTime(v?: string): string {
  return v ? proxy.parseTime(v, '{y}-{m}-{d} {h}:{i}:{s}') : '-'
}

function loadDetail() {
  if (!orderId.value) return
  loading.value = true
  getOrderDetail(orderId.value).then(r => {
    if (r.data) {
      order.value = r.data
      lines.value = r.data.lines || []
    }
  }).finally(() => { loading.value = false })
}

function exportPdf() {
  proxy.download('mes/sal/order/exportPdf/' + orderId.value, {}, `sal_order_${order.value.orderCode}.pdf`)
}
function exportExcel() {
  proxy.download('mes/sal/order/exportExcel/' + orderId.value, {}, `sal_order_${order.value.orderCode}.xlsx`)
}
function goBack() {
  router.push('/mes/sal/order')
}

loadDetail()
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.ml8 { margin-left: 8px; }
.header-title { font-weight: bold; font-size: 16px; margin-right: 8px; }
.audit-footer { color: #909399; font-size: 12px; margin-top: 12px; text-align: right; }
</style>
