<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="退货单编码" prop="rtCode">
        <el-input v-model="queryParams.rtCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="退货单名称" prop="rtName">
        <el-input v-model="queryParams.rtName" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="供应商" prop="vendorName">
        <el-input v-model="queryParams.vendorName" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width:140px">
          <el-option v-for="d in mes_rt_status" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="success" plain icon="Connection" @click="handleFromPo" v-hasPermi="['mes:wm:rt_vendor:fromPurOrder']">从采购订单生成</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:wm:rt_vendor:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="rtvendorList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="退货单编码" align="center" prop="rtCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="退货单名称" align="center" prop="rtName" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="采购订单" align="center" prop="purOrderCode" width="160" :show-overflow-tooltip="true" />
      <el-table-column label="供应商" align="center" prop="vendorName" width="160" :show-overflow-tooltip="true" />
      <el-table-column label="退货仓库" align="center" prop="warehouseName" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="退货日期" align="center" prop="rtDate" width="150" />
      <el-table-column label="退货数量" align="center" prop="totalQuantity" width="100" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <dict-tag :options="mes_rt_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="240">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:rt_vendor:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:wm:rt_vendor:remove']">删除</el-button>
          <el-button v-if="scope.row.status === 'DRAFT'" link type="warning" @click="handleConfirm(scope.row)" v-hasPermi="['mes:wm:rt_vendor:confirm']">确认</el-button>
          <el-button v-if="scope.row.status === 'CONFIRMED'" link type="success" @click="handlePost(scope.row)" v-hasPermi="['mes:wm:rt_vendor:post']">过账</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="1000px" append-to-body :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="退货单号" prop="rtCode">
              <el-input v-model="form.rtCode" placeholder="请输入或自动生成" />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label-width="40px">
              <el-switch v-model="autoGenFlag" active-color="#13ce66" active-text="自动" @change="handleAutoGenChange" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="退货单名称" prop="rtName">
              <el-input v-model="form.rtName" placeholder="请输入" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="供应商" prop="vendorName">
              <el-input v-model="form.vendorName" readonly placeholder="请选择供应商">
                <template #append><el-button icon="Search" @click="handleSelectVendor" /></template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="退货仓库" prop="warehouseName">
              <el-input v-model="form.warehouseName" readonly placeholder="请选择仓库">
                <template #append><el-button icon="Search" @click="handleSelectWarehouse" /></template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="采购订单" prop="purOrderCode">
              <el-input v-model="form.purOrderCode" readonly placeholder="可选,用于回写退货量">
                <template #append><el-button icon="Search" @click="handleSelectPurOrder" /></template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="退货日期" prop="rtDate">
              <el-date-picker v-model="form.rtDate" type="datetime" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" placeholder="备注" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider v-if="form.rtId" content-position="center">退货行明细</el-divider>
        <el-card shadow="always" v-if="form.rtId" class="box-card">
          <WmRtVendorLine :rtId="form.rtId" :warehouseId="form.warehouseId" :warehouseCode="form.warehouseCode" :warehouseName="form.warehouseName" />
        </el-card>
        <el-alert v-else type="info" :closable="false" show-icon title="先保存退货单头信息，保存后即可添加退货行明细。" style="margin-top:12px" />
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <VendorSelect ref="vendorSelectRef" @onSelected="onVendorSelected" />
    <WarehouseSelect ref="warehouseSelectRef" @onSelected="onWarehouseSelected" />
    <PurOrderSelect ref="purOrderSelectRef" @onSelected="onPurOrderSelected" />

    <PoWizard v-model="poWizardOpen" @success="onPoWizardSuccess" />
  </div>
</template>

<script setup lang="ts" name="WmRtVendor">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import type { WmRtVendorQueryParams, WmRtVendor, PurOrder } from '@/types'
import { listWmRtVendor, getWmRtVendor, delWmRtVendor, addWmRtVendor, updateWmRtVendor, confirmRtVendor, postRtVendor } from '@/api/mes/wm/rt_vendor'
import { genSerialCode } from '@/api/mes/sys/autocoderule'
import WmRtVendorLine from './line.vue'
import PoWizard from '@/components/rtVendorWizard/index.vue'
import VendorSelect from '@/components/vendorSelect/single.vue'
import WarehouseSelect from '@/components/warehouseSelect/single.vue'
import PurOrderSelect from '@/components/purOrderSelect/single.vue'

const { proxy } = getCurrentInstance() as any
const { mes_rt_status } = proxy.useDict('mes_rt_status')

const rtvendorList = ref<WmRtVendor[]>([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')
const autoGenFlag = ref(false)
const poWizardOpen = ref(false)
const vendorSelectRef = ref()
const warehouseSelectRef = ref()
const purOrderSelectRef = ref()

const data = reactive({
  form: {} as WmRtVendor,
  queryParams: { pageNum: 1, pageSize: 10 } as WmRtVendorQueryParams,
  rules: {
    rtCode: [{ required: true, message: '请输入或生成退货单号', trigger: 'blur' }],
    warehouseName: [{ required: true, message: '请选择退货仓库', trigger: 'change' }]
  } as Record<string, any>
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listWmRtVendor(queryParams.value).then(r => {
    rtvendorList.value = r.rows
    total.value = r.total
    loading.value = false
  })
}

function cancel() { open.value = false; reset() }
function handleAutoGenChange(flag: boolean) {
  if (flag) genSerialCode('RT_VENDOR_NO').then((r: any) => { form.value.rtCode = r.data })
  else form.value.rtCode = ''
}
function reset() {
  autoGenFlag.value = false
  form.value = { status: 'DRAFT' } as WmRtVendor
  proxy.resetForm('formRef')
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: any[]) { ids.value = s.map(i => i.rtId); single.value = s.length !== 1; multiple.value = !s.length }
function handleFromPo() { poWizardOpen.value = true }
function onPoWizardSuccess() { getList() }
function handleUpdate(row?: WmRtVendor) {
  reset()
  const _id = row?.rtId || ids.value[0]
  getWmRtVendor(_id).then(r => { form.value = r.data; open.value = true; title.value = '修改采购退货单' })
}
function submitForm() {
  proxy.$refs['formRef'].validate((v: boolean) => {
    if (v) {
      const isNew = !form.value.rtId
      const fn = isNew ? addWmRtVendor : updateWmRtVendor
      fn(form.value).then((response: any) => {
        proxy.$modal.msgSuccess('操作成功')
        if (isNew && response?.data?.rtId) {
          form.value.rtId = response.data.rtId
        } else {
          open.value = false
        }
        getList()
      })
    }
  })
}
function handleDelete(row?: WmRtVendor) {
  const _ids = row?.rtId ? [row.rtId] : ids.value
  proxy.$modal.confirm('是否确认删除？').then(() => delWmRtVendor(_ids)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}
function handleConfirm(row: WmRtVendor) {
  proxy.$modal.confirm('确认退货单 "' + row.rtCode + '" 吗？确认后将扣减库存且不可修改。').then(() => {
    return confirmRtVendor(row.rtId)
  }).then(() => { getList(); proxy.$modal.msgSuccess('确认成功') }).catch(() => {})
}
function handlePost(row: WmRtVendor) {
  proxy.$modal.confirm('过账退货单 "' + row.rtCode + '" 吗？过账后将回写采购订单已退货数量。').then(() => {
    return postRtVendor(row.rtId)
  }).then(() => { getList(); proxy.$modal.msgSuccess('过账成功') }).catch(() => {})
}
function handleExport() { proxy.download('/mes/wm/rt_vendor/export', { ...queryParams.value }, `rtvendor_${Date.now()}.xlsx`) }

// 选择器回调
function handleSelectVendor() { vendorSelectRef.value.open() }
function onVendorSelected(row: any) {
  form.value.vendorId = row.vendorId
  form.value.vendorCode = row.vendorCode
  form.value.vendorName = row.vendorName
}
function handleSelectWarehouse() { warehouseSelectRef.value.open() }
function onWarehouseSelected(row: any) {
  form.value.warehouseId = row.warehouseId
  form.value.warehouseCode = row.warehouseCode
  form.value.warehouseName = row.warehouseName
}
function handleSelectPurOrder() { purOrderSelectRef.value.open() }
function onPurOrderSelected(row: PurOrder) {
  form.value.purOrderId = row.orderId
  form.value.purOrderCode = row.orderCode
  if (row.vendorId) {
    onVendorSelected({ vendorId: row.vendorId, vendorCode: row.vendorCode, vendorName: row.vendorName })
  }
}

getList()
</script>
