<template>
  <el-dialog :title="title" v-model="show" width="1000px" append-to-body @close="handleClose">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="基本信息" name="header">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" :disabled="readonly">
          <el-row>
            <el-col :span="12">
              <el-form-item label="出库单号" prop="salesCode">
                <el-input v-model="form.salesCode" placeholder="自动生成" :disabled="true" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label-width="80">
                <el-switch v-model="autoGenFlag" active-text="自动生成" @change="handleAutoGen" v-if="!form.salesId" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="12">
              <el-form-item label="出库单名称" prop="salesName">
                <el-input v-model="form.salesName" placeholder="请输入" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="客户" prop="clientId">
                <el-input v-model="form.clientName" placeholder="请选择客户" readonly>
                  <template #append><el-button icon="Search" @click="handleClientSelect" /></template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="12"><el-form-item label="客户订单号"><el-input v-model="form.clientOrderCode" placeholder="PO号" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="业务员"><el-input v-model="form.salesperson" placeholder="请输入" /></el-form-item></el-col>
          </el-row>
          <el-row>
            <el-col :span="12">
              <el-form-item label="出货仓库">
                <el-input v-model="form.warehouseName" placeholder="可选（批量改仓）" readonly>
                  <template #append><el-button icon="Search" @click="handleWarehouseSelect" /></template>
                </el-input>
                <div style="font-size:12px;color:#909399;line-height:1.4;margin-top:2px">
                  仓库已按库存自动分配到各行；此处选择可批量覆盖所有行仓库
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="出库日期">
                <el-date-picker v-model="form.salesDate" type="datetime" placeholder="选择日期"
                                value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="12">
              <el-form-item label="销售类型">
                <el-select v-model="form.salesType" placeholder="请选择" style="width:100%">
                  <el-option v-for="d in sales_type_dict" :key="d.value" :label="d.label" :value="d.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="打托盘">
                <el-switch v-model="form.palletFlag" active-value="1" inactive-value="0" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="12"><el-form-item label="箱唛/唛头"><el-input v-model="form.boxLabel" placeholder="请输入" /></el-form-item></el-col>
            <el-col :span="12" v-if="form.salesOrderCode">
              <el-form-item label="来源销售订单"><el-input v-model="form.salesOrderCode" disabled /></el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item></el-col>
          </el-row>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="出库明细" name="lines">
        <el-row class="mb8" v-if="!readonly">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="Plus" size="small" @click="handleAddLine">添加物料</el-button>
          </el-col>
        </el-row>
        <el-table :data="lineList" size="small" border>
          <el-table-column label="物料编码" prop="itemCode" width="130" />
          <el-table-column label="物料名称" prop="itemName" :show-overflow-tooltip="true" min-width="140" />
          <el-table-column label="规格" prop="specification" width="120" :show-overflow-tooltip="true" />
          <el-table-column label="仓库" width="130">
            <template #default="scope">
              <span v-if="scope.row.warehouseName">{{ scope.row.warehouseName }}</span>
              <span v-else style="color:#F56C6C">⚠ 无库存</span>
            </template>
          </el-table-column>
          <el-table-column label="本仓可用量" width="110" align="center">
            <template #default="scope">
              <span :style="{ color: availColor(scope.row), fontWeight: isAvailShort(scope.row) ? 'bold' : 'normal' }">
                {{ scope.row.availableQty == null ? '-' : scope.row.availableQty }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="出库数量" width="120" align="center">
            <template #default="scope">
              <el-input-number v-if="!readonly" v-model="scope.row.quantitySales" :min="0" :precision="2"
                               size="small" controls-position="right" style="width:110px" />
              <span v-else>{{ scope.row.quantitySales }}</span>
            </template>
          </el-table-column>
          <el-table-column label="已出库" width="80" align="center">
            <template #default="scope">{{ scope.row.quantityPosted || 0 }}</template>
          </el-table-column>
          <el-table-column label="单位" prop="unitName" width="60" align="center" />
          <el-table-column label="操作" width="70" align="center" v-if="!readonly">
            <template #default="scope">
              <el-button link type="danger" icon="Delete" @click="handleDelLine(scope.$index)" />
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <el-button type="primary" @click="handleSubmit" v-if="!readonly">确 定</el-button>
      <el-button @click="show = false">{{ readonly ? '关 闭' : '取 消' }}</el-button>
    </template>

    <ClientSelect ref="clientSelectRef" @onSelected="onClientSelected" />
    <WarehouseSelect ref="warehouseSelectRef" @onSelected="onWarehouseSelected" />
    <ItemSelect ref="itemSelectRef" @onSelected="onItemSelected" />
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, getCurrentInstance } from 'vue'
import { addWmProductSales, updateWmProductSales } from '@/api/mes/wm/product_sales'
import { genSerialCode } from '@/api/mes/sys/autocoderule'
import ClientSelect from '@/components/clientSelect/single.vue'
import WarehouseSelect from '@/components/warehouseSelect/single.vue'
import ItemSelect from '@/components/itemSelect/single.vue'
import type { WmProductSales, WmProductSalesLine } from '@/types'

const proxy = getCurrentInstance()?.proxy as any
const { mes_product_sales_type: sales_type_dict } = proxy.useDict('mes_product_sales_type')

const emit = defineEmits<{ success: [] }>()
const show = ref(false)
const readonly = ref(false)
const title = ref('')
const activeTab = ref('header')
const autoGenFlag = ref(false)
const form = reactive<WmProductSales>({} as WmProductSales)
const lineList = ref<WmProductSalesLine[]>([])
const rules = {
  salesName: [{ required: true, message: '请输入出库单名称' }],
  clientId: [{ required: true, message: '请选择客户' }]
}

const clientSelectRef = ref()
const warehouseSelectRef = ref()
const itemSelectRef = ref()
const formRef = ref()

/** 打开新增 */
function openAdd(prefill?: Partial<WmProductSales>) {
  reset()
  Object.assign(form, prefill || {})
  form.status = 'DRAFT'
  form.salesType = form.salesType || 'FOREIGN'
  form.palletFlag = form.palletFlag || '0'
  if (prefill?.lines) lineList.value = [...prefill.lines]
  readonly.value = false
  title.value = '新增销售出库单'
  autoGenFlag.value = true
  handleAutoGen(true)
  show.value = true
}

/** 打开编辑 */
function openEdit(data: WmProductSales, lines: WmProductSalesLine[]) {
  reset()
  Object.assign(form, data)
  lineList.value = lines || []
  readonly.value = false
  title.value = '修改销售出库单'
  show.value = true
}

/** 打开查看 */
function openView(data: WmProductSales, lines: WmProductSalesLine[]) {
  reset()
  Object.assign(form, data)
  lineList.value = lines || []
  readonly.value = true
  title.value = '查看出库单'
  show.value = true
}

/** 本仓可用量着色：不足需求红色，充足绿色，无数据灰色 */
function availColor(row: WmProductSalesLine): string {
  if (row.availableQty == null) return '#909399'
  return Number(row.availableQty) < Number(row.quantitySales || 0) ? '#F56C6C' : '#67C23A'
}
/** 本仓可用量是否不足需求（用于加粗，与 availColor 共享同一判定基准） */
function isAvailShort(row: WmProductSalesLine): boolean {
  return row.availableQty != null && Number(row.availableQty) < Number(row.quantitySales || 0)
}

function reset() {
  Object.keys(form).forEach(k => delete (form as any)[k])
  lineList.value = []
  activeTab.value = 'header'
  autoGenFlag.value = false
}

function handleClose() { reset() }

function handleAutoGen(flag: boolean) {
  if (flag) genSerialCode('SALES_NO').then((r: any) => { form.salesCode = r.data })
  else form.salesCode = ''
}

// 选择器
function handleClientSelect() { clientSelectRef.value?.open() }
function onClientSelected(row: any) {
  if (!row) return
  form.clientId = row.clientId
  form.clientCode = row.clientCode
  form.clientName = row.clientName
  form.salesperson = row.salesperson || form.salesperson
  if (row.shippingAddress) form.shippingAddress = row.shippingAddress
}
function handleWarehouseSelect() { warehouseSelectRef.value?.open() }
function onWarehouseSelected(row: any) {
  if (!row) return
  form.warehouseId = row.warehouseId
  form.warehouseCode = row.warehouseCode
  form.warehouseName = row.warehouseName
  // 批量覆盖所有行仓库（表头选仓即统一改仓）
  lineList.value.forEach((l: WmProductSalesLine) => {
    l.warehouseId = row.warehouseId
    l.warehouseCode = row.warehouseCode
    l.warehouseName = row.warehouseName
  })
}

// 物料行
function handleAddLine() { itemSelectRef.value?.open() }
function onItemSelected(row: any) {
  lineList.value.push({
    itemId: row.itemId, itemCode: row.itemCode, itemName: row.itemName,
    specification: row.specification, unitOfMeasure: row.unitOfMeasure, unitName: row.unitName,
    quantitySales: 1, quantityPosted: 0, warehouseId: form.warehouseId,
    warehouseCode: form.warehouseCode, warehouseName: form.warehouseName
  } as unknown as WmProductSalesLine)
}
function handleDelLine(idx: number) { lineList.value.splice(idx, 1) }

function handleSubmit() {
  formRef.value?.validate((v: boolean) => {
    if (!v) return
    // 校验：无仓库行（无库存物料）需用户确认保留
    const noWhLines = lineList.value.filter((l: WmProductSalesLine) => l.warehouseId == null)
    const doSave = () => {
      form.lines = lineList.value
      const action = form.salesId ? updateWmProductSales(form) : addWmProductSales(form)
      action.then(() => {
        proxy.$modal.msgSuccess('保存成功')
        show.value = false
        emit('success')
      })
    }
    if (noWhLines.length) {
      proxy.$modal.confirm(
        `${noWhLines.length} 行物料无库存未分配仓库，保存后这些行将无法过账出库。确认保留？`
      ).then(doSave).catch(() => {})
    } else {
      doSave()
    }
  })
}

defineExpose({ openAdd, openEdit, openView })
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
</style>
