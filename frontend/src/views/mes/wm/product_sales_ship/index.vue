<template>
  <div class="app-container">
    <!-- 顶部：返回 + 单号 + 打印 -->
    <el-page-header @back="goBack" class="mb16">
      <template #content>
        <span class="header-title">发货工作台</span>
        <el-tag v-if="header.salesCode" type="info" class="ml8">{{ header.salesCode }}</el-tag>
        <el-tag v-if="header.shipStatus" :type="shipStatusTag(header.shipStatus)" class="ml8">
          {{ shipStatusLabel(header.shipStatus) }}
        </el-tag>
      </template>
      <template #extra>
        <el-button type="primary" plain icon="Printer" @click="openPrint('delivery')">打印送货单</el-button>
        <el-button type="success" plain icon="Printer" @click="openPrint('packing')">打印装箱单</el-button>
      </template>
    </el-page-header>

    <!-- 出库单信息 + 发运汇总 -->
    <el-row :gutter="12" class="mb16">
      <el-col :span="14">
        <el-card shadow="never" header="出库单信息">
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item label="客户">{{ header.clientName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="客户订单号">{{ header.clientOrderCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="业务员">{{ header.salesperson || '-' }}</el-descriptions-item>
            <el-descriptions-item label="销售订单">{{ header.salesOrderCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="出库日期">{{ parseTime(header.salesDate, '{y}-{m}-{d}') }}</el-descriptions-item>
            <el-descriptions-item label="销售类型">
              <dict-tag :options="sales_type" :value="header.salesType" />
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never" header="发运汇总">
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item label="应出库量">{{ header.totalQuantity || 0 }}</el-descriptions-item>
            <el-descriptions-item label="已过账量">{{ header.postedQuantity || 0 }}</el-descriptions-item>
            <el-descriptions-item label="已发运量">
              <span style="color:#409EFF;font-weight:bold">{{ header.shippedQuantity || 0 }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="未发运量">
              <span :style="{ color: remainShip() > 0 ? '#E6A23C' : '#67C23A', fontWeight:'bold' }">
                {{ remainShip() }}
              </span>
            </el-descriptions-item>
          </el-descriptions>
          <el-progress v-if="header.totalQuantity > 0"
            :percentage="Math.round(((header.shippedQuantity || 0) / header.totalQuantity) * 100)"
            :stroke-width="10" class="mt8" />
        </el-card>
      </el-col>
    </el-row>

    <!-- Tabs -->
    <el-tabs v-model="activeTab">
      <el-tab-pane label="装箱明细" name="boxes">
        <BoxList :sales-id="salesId" :lines="header.lines || []" :boxes="header.boxes || []"
                 :readonly="!canEdit" @refresh="loadDetail" />
      </el-tab-pane>
      <el-tab-pane label="发运记录" name="shipments">
        <ShipmentList :sales-id="salesId" :header="header" :shipments="header.shipments || []"
                      :boxes="header.boxes || []" :readonly="!canEdit" @refresh="loadDetail" />
      </el-tab-pane>
    </el-tabs>

    <!-- 打印弹窗 -->
    <el-dialog v-model="printVisible" :title="printTitle" width="900px" append-to-body :fullscreen="true">
      <DeliveryPrint v-if="printType==='delivery'" :header="header" />
      <PackingPrint v-else-if="printType==='packing'" :header="header" :boxes="header.boxes || []" />
      <template #footer>
        <el-button type="primary" icon="Printer" @click="doPrint">打印</el-button>
        <el-button @click="printVisible=false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSalesDetail } from '@/api/mes/wm/product_sales'
import type { WmProductSales } from '@/types'
import BoxList from './components/BoxList.vue'
import ShipmentList from './components/ShipmentList.vue'
import DeliveryPrint from './components/print/DeliveryPrint.vue'
import PackingPrint from './components/print/PackingPrint.vue'

const { proxy } = getCurrentInstance() as any
const { mes_product_sales_type: sales_type, mes_wm_ship_status: ship_status_dict } = proxy.useDict(
  'mes_product_sales_type', 'mes_wm_ship_status')

const route = useRoute()
const router = useRouter()
const salesId = computed(() => Number(route.query.salesId))
const activeTab = ref('boxes')
const header = ref<WmProductSales>({} as WmProductSales)
const printVisible = ref(false)
const printType = ref<'delivery' | 'packing'>('delivery')

const printTitle = computed(() => printType.value === 'delivery' ? '送货单打印' : '装箱单打印')

/** 是否可操作装箱/发运（已关闭/作废不可操作） */
const canEdit = computed(() => {
  const s = header.value.status
  return s && !['CLOSED', 'CANCELED'].includes(s)
})

function loadDetail() {
  if (!salesId.value) return
  getSalesDetail(salesId.value).then(r => {
    if (r.data) header.value = r.data
  })
}

function remainShip(): number {
  const total = Number(header.value.totalQuantity || 0)
  const shipped = Number(header.value.shippedQuantity || 0)
  return Math.max(0, total - shipped)
}

function shipStatusLabel(v?: string): string {
  if (!v) return '-'
  const d = ship_status_dict.value?.find((x: any) => x.value === v)
  return d?.label || v
}
function shipStatusTag(v?: string): string {
  const map: Record<string, string> = {
    UN_SHIPPED: 'info', PARTIAL_SHIPPED: 'warning', SHIPPED: 'primary', RECEIVED: 'success'
  }
  return map[v || ''] || 'info'
}

function openPrint(type: 'delivery' | 'packing') {
  printType.value = type
  printVisible.value = true
}
function doPrint() {
  window.print()
}

function goBack() {
  router.push('/mes/wm/product_sales')
}

loadDetail()
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.ml8 { margin-left: 8px; }
.mt8 { margin-top: 8px; }
.header-title { font-weight: bold; font-size: 16px; margin-right: 8px; }
</style>
