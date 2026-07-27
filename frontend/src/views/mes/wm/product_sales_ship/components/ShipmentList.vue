<template>
  <div>
    <el-row class="mb8" v-if="!readonly">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['mes:wm:sales:ship']">新增发运</el-button></el-col>
      <el-col :span="20" class="hint">一张出库单可多次发运；勾选已装箱的箱，登记物流/车辆信息</el-col>
    </el-row>

    <el-table :data="shipments" size="small" border>
      <el-table-column label="发运单号" prop="shipmentCode" width="150" />
      <el-table-column label="发运日期" width="110">
        <template #default="s">{{ parseTime(s.row.actualShipDate, '{y}-{m}-{d}') }}</template>
      </el-table-column>
      <el-table-column label="发货方式" width="80" align="center">
        <template #default="s"><dict-tag :options="ship_method" :value="s.row.shipMethod" /></template>
      </el-table-column>
      <el-table-column label="物流/承运" prop="logisticsCompany" width="110" :show-overflow-tooltip="true" />
      <el-table-column label="运单号" prop="trackingNo" width="130" :show-overflow-tooltip="true" />
      <el-table-column label="车牌/司机" width="110">
        <template #default="s">
          <span v-if="s.row.vehicleNo">{{ s.row.vehicleNo }} / {{ s.row.driverName }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="发运量" prop="shippedQuantity" width="80" align="center" />
      <el-table-column label="箱数" prop="boxCount" width="60" align="center" />
      <el-table-column label="状态" width="80" align="center">
        <template #default="s"><dict-tag :options="shipment_status" :value="s.row.status" /></template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="s">
          <el-button link type="primary" icon="View" @click="handleView(s.row)">明细</el-button>
          <el-button v-if="s.row.status==='IN_TRANSIT' || s.row.status==='SHIPPING'" link type="success" icon="Check"
                     @click="handleReceive(s.row)" v-hasPermi="['mes:wm:sales:receive']">签收</el-button>
          <el-button v-if="s.row.status==='SHIPPING'" link type="warning" icon="Close"
                     @click="handleCancel(s.row)" v-hasPermi="['mes:wm:sales:ship']">取消</el-button>
          <el-button v-if="s.row.status!=='RECEIVED' && s.row.status!=='CANCELED'" link type="danger" icon="Delete"
                     @click="handleDel(s.row)" v-hasPermi="['mes:wm:sales:ship']">删</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增发运表单弹窗 -->
    <ShipmentForm ref="formRef" :sales-id="salesId" :header="header" :boxes="boxes" @success="emitRefresh" />
    <!-- 签收弹窗 -->
    <ReceiveDialog ref="receiveRef" @success="emitRefresh" />
    <!-- 明细查看 -->
    <el-dialog title="发运明细" v-model="viewShow" width="800px" append-to-body>
      <el-descriptions :column="2" border size="small" v-if="viewData">
        <el-descriptions-item label="发运单号">{{ viewData.shipmentCode }}</el-descriptions-item>
        <el-descriptions-item label="状态"><dict-tag :options="shipment_status" :value="viewData.status" /></el-descriptions-item>
        <el-descriptions-item label="发货方式"><dict-tag :options="ship_method" :value="viewData.shipMethod" /></el-descriptions-item>
        <el-descriptions-item label="发运日期">{{ parseTime(viewData.actualShipDate) }}</el-descriptions-item>
        <el-descriptions-item label="物流公司">{{ viewData.logisticsCompany || '-' }}</el-descriptions-item>
        <el-descriptions-item label="运单号">{{ viewData.trackingNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="车牌">{{ viewData.vehicleNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="司机">{{ viewData.driverName }} {{ viewData.driverTel }}</el-descriptions-item>
        <el-descriptions-item label="收货人">{{ viewData.receiverName }} {{ viewData.receiverTel }}</el-descriptions-item>
        <el-descriptions-item label="收货地址">{{ viewData.shippingAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="签收时间" v-if="viewData.receivedTime">{{ parseTime(viewData.receivedTime) }}</el-descriptions-item>
        <el-descriptions-item label="签收人" v-if="viewData.receivedBy">{{ viewData.receivedBy }}</el-descriptions-item>
        <el-descriptions-item label="回单附件" :span="2" v-if="attachments.length">
          <div class="att-list">
            <div v-for="(att, i) in attachments" :key="i" class="att-item" @click="handlePreview(att, i)">
              <img v-if="att.isImage" :src="att.fullUrl" class="att-thumb" />
              <div v-else class="att-file">
                <el-icon :size="28"><Document /></el-icon>
                <span class="att-name">{{ att.name }}</span>
              </div>
            </div>
          </div>
          <el-image-viewer v-if="previewShow" :url-list="imageUrls" :initial-index="previewImg" @close="previewShow=false" />
        </el-descriptions-item>
      </el-descriptions>
      <div v-if="viewData?.boxes?.length" class="mt8">
        <div class="sub-title">本次发运箱清单</div>
        <el-table :data="viewData.boxes" size="small" border>
          <el-table-column label="箱号" prop="boxNo" width="100" />
          <el-table-column label="物料编码" prop="itemCode" width="120" />
          <el-table-column label="物料名称" prop="itemName" min-width="140" />
          <el-table-column label="数量" prop="quantity" width="80" align="center" />
          <el-table-column label="箱规" prop="boxSpec" width="120" />
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, getCurrentInstance } from 'vue'
import { Document } from '@element-plus/icons-vue'
import { getShipment, delShipment, cancelShipment } from '@/api/mes/wm/product_sales_shipment'
import type { WmProductSales, WmProductSalesShipment, WmProductSalesBox } from '@/types'
import ShipmentForm from './ShipmentForm.vue'
import ReceiveDialog from './ReceiveDialog.vue'

const props = defineProps<{
  salesId: number
  header: WmProductSales
  shipments: WmProductSalesShipment[]
  boxes: WmProductSalesBox[]
  readonly: boolean
}>()
const emit = defineEmits<{ refresh: [] }>()

const { proxy } = getCurrentInstance() as any
const { mes_wm_ship_method: ship_method, mes_wm_shipment_status: shipment_status } = proxy.useDict(
  'mes_wm_ship_method', 'mes_wm_shipment_status')
const baseUrl = import.meta.env.VITE_APP_BASE_API

const formRef = ref()
const receiveRef = ref()
const viewShow = ref(false)
const viewData = ref<WmProductSalesShipment | null>(null)
const previewShow = ref(false)
const previewImg = ref(0)

// 附件解析：把 attachmentUrl(逗号分隔) 拆成结构化列表；区分图片(可预览)与其他文件(点击下载)
interface Attachment { raw: string; fullUrl: string; name: string; isImage: boolean }
const attachments = computed<Attachment[]>(() => {
  const s = viewData.value?.attachmentUrl
  if (!s) return []
  return s.split(',').map(u => u.trim()).filter(Boolean).map(u => {
    const name = u.split('/').pop() || u
    const ext = name.split('.').pop()?.toLowerCase() || ''
    return { raw: u, fullUrl: baseUrl + u, name, isImage: ['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext) }
  })
})
const imageUrls = computed(() => attachments.value.filter(a => a.isImage).map(a => a.fullUrl))

function handlePreview(att: Attachment, i: number) {
  if (att.isImage) {
    // 大图预览：以图片列表中的位置为准，非图片不参与
    previewImg.value = attachments.value.slice(0, i).filter(a => a.isImage).length
    previewShow.value = true
  } else {
    // 非图片(pdf 等) 新 tab 打开
    window.open(att.fullUrl, '_blank')
  }
}

function handleAdd() { formRef.value?.open() }
function emitRefresh() { emit('refresh') }

function handleView(row: WmProductSalesShipment) {
  getShipment(row.shipmentId).then(r => {
    viewData.value = r.data
    viewShow.value = true
  })
}
function handleReceive(row: WmProductSalesShipment) { receiveRef.value?.open(row) }

function handleCancel(row: WmProductSalesShipment) {
  proxy.$modal.confirm(`取消发运单[${row.shipmentCode}]？仅待发运状态可取消。`).then(() => cancelShipment(row.shipmentId))
    .then(() => { proxy.$modal.msgSuccess('已取消'); emit('refresh') }).catch(() => {})
}
function handleDel(row: WmProductSalesShipment) {
  const msg = row.status === 'IN_TRANSIT'
    ? `删除发运单[${row.shipmentCode}]？将回滚关联箱状态和头表发运量。`
    : `删除发运单[${row.shipmentCode}]？`
  proxy.$modal.confirm(msg).then(() => delShipment(row.shipmentId))
    .then(() => { proxy.$modal.msgSuccess('已删除'); emit('refresh') }).catch(() => {})
}
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.mt8 { margin-top: 8px; }
.mr8 { margin-right: 8px; }
.hint { color: #909399; font-size: 12px; padding-left: 12px; line-height: 32px; }
.sub-title { font-size: 13px; font-weight: bold; margin-bottom: 6px; }
.att-list { display: flex; gap: 8px; flex-wrap: wrap; }
.att-item { width: 100px; height: 100px; border-radius: 4px; overflow: hidden; border: 1px solid #dcdfe6; cursor: pointer; }
.att-thumb { width: 100%; height: 100%; object-fit: cover; }
.att-file { width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; background: #f5f7fa; }
.att-name { font-size: 11px; color: #606266; margin-top: 4px; max-width: 90px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
