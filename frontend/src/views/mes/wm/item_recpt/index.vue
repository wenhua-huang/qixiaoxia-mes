<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="入库单号" prop="recptCode">
        <el-input v-model="queryParams.recptCode" placeholder="请输入入库单号" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="供应商" prop="vendorId">
        <el-input v-model="queryParams.vendorId" placeholder="供应商ID" clearable />
      </el-form-item>
      <el-form-item label="采购订单号" prop="purOrderCode">
        <el-input v-model="queryParams.purOrderCode" placeholder="请输入采购订单号" clearable />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width:100px">
          <el-option v-for="d in mes_itemrecpt_status" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['mes:wm:itemrecpt:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:wm:itemrecpt:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:wm:itemrecpt:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" size="small" @click="handleExport" v-hasPermi="['mes:wm:itemrecpt:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="recptList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="入库单号" align="center" prop="recptCode" width="150">
        <template #default="scope">
          <el-button link type="primary" size="small" @click="handleView(scope.row)">
            {{ scope.row.recptCode }}
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="入库名称" align="center" prop="recptName" :show-overflow-tooltip="true" width="150" />
      <el-table-column label="供应商" align="center" prop="vendorName" width="120" />
      <el-table-column label="仓库" align="center" prop="warehouseName" width="120" />
      <el-table-column label="入库日期" align="center" prop="recptDate" width="110" />
      <el-table-column label="状态" align="center" width="90">
        <template #default="scope">
          <dict-tag :options="mes_itemrecpt_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="105" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tooltip content="修改" placement="top" v-if="isEditable(scope.row)"><el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:itemrecpt:edit']"></el-button></el-tooltip>
          <el-tooltip content="删除" placement="top" v-if="isEditable(scope.row)"><el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:wm:itemrecpt:remove']"></el-button></el-tooltip>
          <el-tooltip content="确认" placement="top" v-if="scope.row.status === 'DRAFT'"><el-button link type="success" icon="Check" @click="handleConfirm(scope.row)"></el-button></el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/修改对话框 — 单据头 + 物料行同一页面 -->
    <el-dialog :title="title" v-model="open" width="1000px" append-to-body :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="入库单号" prop="recptCode">
              <el-input v-model="form.recptCode" placeholder="入库单号" />
            </el-form-item>
          </el-col>
          <el-col :span="6" v-if="!form.recptId">
            <el-form-item>
              <el-switch v-model="autoGenFlag" active-color="#13ce66" size="small" @change="handleAutoGenChange" />
              <span style="margin-left:6px;font-size:12px;color:#13ce66">自动生成</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入库名称" prop="recptName">
              <el-input v-model="form.recptName" placeholder="入库名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="入库日期" prop="recptDate">
              <el-date-picker v-model="form.recptDate" type="datetime" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="供应商" prop="vendorName">
              <el-input v-model="form.vendorName" readonly placeholder="请选择供应商">
                <template #append><el-button icon="Search" @click="handleSelectVendor" /></template>
              </el-input>
            </el-form-item>
            <VendorSelect ref="vendorSelectRef" @onSelected="onVendorSelected" />
          </el-col>
          <el-col :span="8">
            <el-form-item label="仓库" prop="warehouseName">
              <el-input v-model="form.warehouseName" readonly placeholder="请选择仓库">
                <template #append><el-button icon="Search" @click="handleSelectWarehouse" /></template>
              </el-input>
            </el-form-item>
            <WarehouseSelect ref="warehouseSelectRef" @onSelected="onWarehouseSelected" />
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="采购订单号" prop="purOrderCode">
              <el-input v-model="form.purOrderCode" readonly placeholder="请选择采购订单">
                <template #append><el-button icon="Search" @click="handleSelectPurOrder" /></template>
              </el-input>
            </el-form-item>
            <PurOrderSelect ref="purOrderSelectRef" @onSelected="onPurOrderSelected" />
          </el-col>
          <el-col :span="8">
            <el-form-item label="入库类型" prop="recptType">
              <el-select v-model="form.recptType" placeholder="请选择" style="width:100%">
                <el-option v-for="d in mes_recpt_type" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" placeholder="备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <el-divider v-if="form.recptId" content-position="center">物料信息</el-divider>
      <el-card shadow="always" v-if="form.recptId" class="box-card">
        <WmItemRecptLine :recptId="form.recptId" :warehouseId="form.warehouseId" :warehouseName="form.warehouseName" :warehouseCode="form.warehouseCode" />
      </el-card>

      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">保存单据</el-button>
          <el-button v-if="form.recptId" type="warning" @click="handleConfirm(form.value as WmItemRecpt)">提交入库</el-button>
          <el-button @click="cancel">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="WmItemRecpt">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import type { WmItemRecptQueryParams, WmItemRecpt } from '@/types/api/mes/wm/item_recpt'
import { listWmItemRecpt, getWmItemRecpt, delWmItemRecpt, addWmItemRecpt, updateWmItemRecpt } from '@/api/mes/wm/item_recpt'
import request from '@/utils/request'
import { genSerialCode } from '@/api/mes/sys/autocoderule'
import WmItemRecptLine from './line.vue'
import VendorSelect from '@/components/vendorSelect/single.vue'
import WarehouseSelect from '@/components/warehouseSelect/single.vue'
import PurOrderSelect from '@/components/purOrderSelect/single.vue'
import type { PurOrder } from '@/types/api/mes/pur/order'

const { proxy } = getCurrentInstance() as any
const { mes_itemrecpt_status, mes_recpt_type } = useDict('mes_itemrecpt_status', 'mes_recpt_type')
const vendorSelectRef = ref()
const warehouseSelectRef = ref()
const purOrderSelectRef = ref()

const recptList = ref<WmItemRecpt[]>([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')
const autoGenFlag = ref(false)

const data = reactive({
  form: {} as WmItemRecpt,
  queryParams: { pageNum: 1, pageSize: 10 } as WmItemRecptQueryParams,
  rules: {
    recptCode: [{ required: true, message: '入库单号不能为空', trigger: 'blur' }],
    warehouseId: [{ required: true, message: '仓库不能为空', trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listWmItemRecpt(queryParams.value).then(r => { recptList.value = r.rows; total.value = r.total; loading.value = false })
}
function cancel() { open.value = false; reset() }
function reset() { autoGenFlag.value = false; form.value = {} as WmItemRecpt; proxy.resetForm('formRef') }
function handleAutoGenChange(flag: boolean) {
  if (flag) genSerialCode('RECEIPT_NO').then((r: any) => { form.value.recptCode = r.data })
  else form.value.recptCode = ''
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: any[]) { ids.value = s.map(i => i.recptId); single.value = s.length !== 1; multiple.value = !s.length }
function isEditable(row: WmItemRecpt) { return row.status === 'DRAFT' }

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增入库单'
}
function handleView(row: WmItemRecpt) {
  reset()
  getWmItemRecpt(row.recptId).then(r => { form.value = r.data; open.value = true; title.value = '查看入库单' })
}
function handleUpdate(row?: WmItemRecpt) {
  reset()
  const id = row?.recptId || ids.value[0]
  getWmItemRecpt(id).then(r => { form.value = r.data; open.value = true; title.value = '修改入库单' })
}
function submitForm() {
  proxy.$refs['formRef'].validate((v: boolean) => {
    if (!v) return
    const isNew = !form.value.recptId
    const fn = isNew ? addWmItemRecpt : updateWmItemRecpt
    fn(form.value).then((response: any) => {
      proxy.$modal.msgSuccess('操作成功')
      if (isNew) {
        // 后端 add 已返回完整实体（含 recptId），直接从响应中获取
        const newRecptId = response?.data?.recptId
        if (newRecptId) {
          form.value.recptId = newRecptId
        }
      }
      getList()
    })
  })
}
function handleDelete(row?: WmItemRecpt) {
  const _ids = row?.recptId ? [row.recptId] : ids.value
  proxy.$modal.confirm('是否确认删除？').then(() => delWmItemRecpt(_ids)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}
function handleSelectVendor() { vendorSelectRef.value.open() }
function onVendorSelected(row: any) {
  form.value.vendorId = row.vendorId
  form.value.vendorName = row.vendorName
  form.value.vendorCode = row.vendorCode
}
function handleSelectWarehouse() { warehouseSelectRef.value.open() }
function onWarehouseSelected(row: any) {
  form.value.warehouseId = row.warehouseId
  form.value.warehouseName = row.warehouseName
  form.value.warehouseCode = row.warehouseCode
}
function handleSelectPurOrder() { purOrderSelectRef.value.open() }
function onPurOrderSelected(row: PurOrder) {
  form.value.purOrderId = row.orderId
  form.value.purOrderCode = row.orderCode
  // 回填供应商信息（采购订单无供应商时清除旧值，避免残留）
  // 复用 onVendorSelected 的字段赋值逻辑，避免 vendor 字段映射重复
  onVendorSelected({
    vendorId: row.vendorId ?? undefined,
    vendorCode: row.vendorCode ?? undefined,
    vendorName: row.vendorName ?? undefined,
  })
}
function handleExport() { proxy.download('/mes/wm/item_recpt/export', { ...queryParams.value }, `itemrecpt_${Date.now()}.xlsx`) }
function handleConfirm(row?: WmItemRecpt) {
  const target = row || form.value
  if (!target?.recptId || !target?.recptCode) return
  proxy.$modal.confirm(`确认入库单 "${target.recptCode}" 并生成库存记录？`).then(() => {
    request({ url: `/mes/wm/item_recpt/confirm/${target.recptId}`, method: 'put' }).then(() => {
      proxy.$modal.msgSuccess('确认成功，库存已更新')
      open.value = false
      getList()
    })
  }).catch(() => {})
}

getList()
</script>

<style scoped>
:deep(.el-form-item__label) {
  padding-right: 16px !important;
}
</style>
