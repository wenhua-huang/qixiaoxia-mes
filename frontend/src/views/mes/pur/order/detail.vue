<template>
  <div class="app-container">
    <!-- 顶部：返回 + 单号 + 状态 + 导出 -->
    <el-page-header @back="goBack" class="mb16">
      <template #content>
        <span class="header-title">采购订单详情</span>
        <el-tag v-if="order.orderCode" type="info" class="ml8">{{ order.orderCode }}</el-tag>
        <el-tag v-if="order.status" :type="statusMeta.type" class="ml8">{{ statusMeta.label }}</el-tag>
      </template>
      <template #extra>
        <el-button type="primary" plain icon="Printer" @click="exportPdf" v-hasPermi="['mes:pur:order:exportDetail']">导出 PDF</el-button>
        <el-button type="success" plain icon="Document" @click="exportExcel" v-hasPermi="['mes:pur:order:exportDetail']">导出 Excel</el-button>
      </template>
    </el-page-header>

    <div v-loading="loading">
      <!-- 订单信息 -->
      <el-card shadow="never" class="mb16">
        <template #header><span>订单信息</span></template>
        <el-descriptions :column="3" size="small" border>
          <el-descriptions-item label="订单名称">{{ order.orderName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="供应商编码">{{ order.vendorCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="供应商名称">{{ order.vendorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="采购员">{{ order.purchaser || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审批人">{{ order.approver || '-' }}</el-descriptions-item>
          <el-descriptions-item label="币种">{{ order.currency || '-' }}</el-descriptions-item>
          <el-descriptions-item label="订单日期">{{ fmtDate(order.orderDate) }}</el-descriptions-item>
          <el-descriptions-item label="交货日期">{{ fmtDate(order.expectedDate) }}</el-descriptions-item>
          <el-descriptions-item label="关联客户订单">{{ order.sourceOrderCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="采购数量">{{ order.totalQuantity ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="已收数量">{{ order.receivedQuantity ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="采购金额">
            <span style="font-weight:bold;color:#f56c6c">{{ order.totalAmount ?? '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="3">{{ order.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 明细行 -->
      <el-card shadow="never">
        <template #header><span>明细行（共 {{ lines.length }} 行）</span></template>
        <el-table :data="lines" size="small" border>
          <el-table-column label="序号" align="center" type="index" width="60" />
          <el-table-column label="物料编码" align="center" prop="itemCode" width="140" />
          <el-table-column label="物料名称" align="center" prop="itemName" :show-overflow-tooltip="true" />
          <el-table-column label="规格" align="center" prop="specification" width="120" :show-overflow-tooltip="true" />
          <el-table-column label="单位" align="center" prop="unitName" width="70" />
          <el-table-column label="数量" align="center" prop="quantityOrdered" width="90" />
          <el-table-column label="单价" align="center" prop="unitPrice" width="90" />
          <el-table-column label="不含税金额" align="center" prop="amount" width="110" />
          <el-table-column label="税率" align="center" width="80">
            <template #default="s">{{ s.row.taxRate != null ? s.row.taxRate + '%' : '-' }}</template>
          </el-table-column>
          <el-table-column label="已收量" align="center" prop="quantityReceived" width="90">
            <template #default="s">{{ s.row.quantityReceived || 0 }}</template>
          </el-table-column>
          <el-table-column label="行交期" align="center" width="110">
            <template #default="s">{{ fmtDate(s.row.expectedDate) }}</template>
          </el-table-column>
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
import { getOrderDetail } from '@/api/mes/pur/order'
import type { PurOrder, PurOrderLine } from '@/types/api/mes/pur/order'

const { proxy } = getCurrentInstance() as any

const route = useRoute()
const router = useRouter()
const orderId = computed(() => Number(route.query.orderId))
const loading = ref(false)
const order = ref<PurOrder>({} as PurOrder)
const lines = ref<PurOrderLine[]>([])

/** 采购订单状态 → 标签（硬编码，不依赖字典加载） */
const STATUS_MAP: Record<string, { label: string; type: string }> = {
  DRAFT: { label: '草稿', type: 'info' },
  APPROVED: { label: '已审批', type: 'warning' },
  ORDERED: { label: '已下单', type: '' },
  RECEIVING: { label: '收货中', type: 'warning' },
  RECEIVED: { label: '已收货', type: 'success' },
  CLOSED: { label: '已关闭', type: 'primary' },
  CANCEL: { label: '已取消', type: 'danger' }
}
const statusMeta = computed(() => STATUS_MAP[order.value.status || ''] || { label: order.value.status || '-', type: 'info' })

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
      // 采购单 detail 返回 { order, lines }（PurOrderDetailVO）
      order.value = r.data.order || {}
      lines.value = r.data.lines || []
    }
  }).finally(() => { loading.value = false })
}

function exportPdf() {
  proxy.download('mes/pur/order/exportPdf/' + orderId.value, {}, `pur_order_${order.value.orderCode}.pdf`)
}
function exportExcel() {
  proxy.download('mes/pur/order/exportExcel/' + orderId.value, {}, `pur_order_${order.value.orderCode}.xlsx`)
}
function goBack() {
  router.push('/mes/pur/order')
}

loadDetail()
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.ml8 { margin-left: 8px; }
.header-title { font-weight: bold; font-size: 16px; margin-right: 8px; }
.audit-footer { color: #909399; font-size: 12px; margin-top: 12px; text-align: right; }
</style>
