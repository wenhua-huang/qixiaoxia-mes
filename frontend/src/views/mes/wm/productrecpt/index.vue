<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="入库单号" prop="recptCode">
        <el-input v-model="queryParams.recptCode" placeholder="请输入入库单号" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="工单号" prop="workorderCode">
        <el-input v-model="queryParams.workorderCode" placeholder="请输入工单号" clearable @keyup.enter="handleQuery" />
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
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['mes:wm:product_recpt:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:wm:product_recpt:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:wm:product_recpt:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" size="small" @click="handleExport" v-hasPermi="['mes:wm:product_recpt:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="recptList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="入库单号" align="center" prop="recptCode" width="170">
        <template #default="scope">
          <el-button link type="primary" size="small" @click="handleView(scope.row)">
            {{ scope.row.recptCode }}
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="入库名称" align="center" prop="recptName" :show-overflow-tooltip="true" width="180" />
      <el-table-column label="工单号" align="center" prop="workorderCode" width="160" />
      <el-table-column label="产品编码" align="center" prop="produceCode" width="140" />
      <el-table-column label="仓库" align="center" prop="warehouseName" width="120" />
      <el-table-column label="入库数量" align="center" prop="totalQuantity" width="100" />
      <el-table-column label="入库日期" align="center" prop="recptDate" width="160" />
      <el-table-column label="状态" align="center" width="90">
        <template #default="scope">
          <dict-tag :options="mes_itemrecpt_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tooltip content="修改" placement="top" v-if="isEditable(scope.row)"><el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:product_recpt:edit']"></el-button></el-tooltip>
          <el-tooltip content="删除" placement="top" v-if="isEditable(scope.row)"><el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:wm:product_recpt:remove']"></el-button></el-tooltip>
          <el-tooltip content="确认收货" placement="top" v-if="scope.row.status === 'DRAFT'"><el-button link type="success" icon="Check" @click="handleConfirm(scope.row)"></el-button></el-tooltip>
          <el-button v-if="scope.row.status === 'CONFIRMED'" link type="warning" size="small" @click="handlePost(scope.row)" v-hasPermi="['mes:wm:product_recpt:edit']">过账</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/修改对话框 -->
    <el-dialog :title="title" v-model="open" width="900px" append-to-body :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="入库单号" prop="recptCode">
              <el-input v-model="form.recptCode" placeholder="入库单号（工单完工自动生成）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入库名称" prop="recptName">
              <el-input v-model="form.recptName" placeholder="入库名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="工单号" prop="workorderCode">
              <el-input v-model="form.workorderCode" placeholder="工单号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产品编码" prop="produceCode">
              <el-input v-model="form.produceCode" placeholder="产品编码" />
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
            <el-form-item label="仓库" prop="warehouseName">
              <el-input v-model="form.warehouseName" readonly placeholder="请选择仓库">
                <template #append><el-button icon="Search" @click="handleSelectWarehouse" /></template>
              </el-input>
            </el-form-item>
            <WarehouseSelect ref="warehouseSelectRef" @onSelected="onWarehouseSelected" />
          </el-col>
          <el-col :span="8">
            <el-form-item label="总箱数" prop="totalBox">
              <el-input-number v-model="form.totalBox" :min="0" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" placeholder="备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <el-divider v-if="form.recptId" content-position="center">入库行</el-divider>
      <el-table v-if="form.recptId && form.lines" :data="form.lines" border size="small">
        <el-table-column label="物料编码" align="center" prop="itemCode" />
        <el-table-column label="物料名称" align="center" prop="itemName" :show-overflow-tooltip="true" />
        <el-table-column label="规格" align="center" prop="specification" :show-overflow-tooltip="true" />
        <el-table-column label="单位" align="center" prop="unitName" width="80" />
        <el-table-column label="入库数量" align="center" prop="quantityRecpt" width="100" />
        <el-table-column label="箱数" align="center" prop="quantityBox" width="80" />
        <el-table-column label="批次" align="center" prop="batchCode" width="120" />
      </el-table>

      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">保存单据</el-button>
          <el-button v-if="form.recptId && form.status === 'DRAFT'" type="warning" @click="handleConfirm(form as WmProductRecpt)">提交入库</el-button>
          <el-button @click="cancel">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="WmProductRecpt">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import type { WmProductRecptQueryParams, WmProductRecpt } from '@/api/mes/wm/product_recpt'
import { listWmProductRecpt, getWmProductRecpt, delWmProductRecpt, addWmProductRecpt, updateWmProductRecpt } from '@/api/mes/wm/product_recpt'
import request from '@/utils/request'
import WarehouseSelect from '@/components/warehouseSelect/single.vue'

const { proxy } = getCurrentInstance() as any
const { mes_itemrecpt_status } = useDict('mes_itemrecpt_status')
const warehouseSelectRef = ref()

const recptList = ref<WmProductRecpt[]>([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

const data = reactive({
  form: {} as WmProductRecpt,
  queryParams: { pageNum: 1, pageSize: 10 } as WmProductRecptQueryParams,
  rules: {
    recptCode: [{ required: true, message: '入库单号不能为空', trigger: 'blur' }],
    warehouseId: [{ required: true, message: '仓库不能为空', trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listWmProductRecpt(queryParams.value).then(r => { recptList.value = r.rows; total.value = r.total; loading.value = false })
}
function cancel() { open.value = false; reset() }
function reset() { form.value = {} as WmProductRecpt; proxy.resetForm('formRef') }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: any[]) { ids.value = s.map(i => i.recptId); single.value = s.length !== 1; multiple.value = !s.length }
function isEditable(row: WmProductRecpt) { return row.status === 'DRAFT' }

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增产品入库单'
}
function handleView(row: WmProductRecpt) {
  reset()
  getWmProductRecpt(row.recptId!).then(r => { form.value = r.data; open.value = true; title.value = '查看产品入库单' })
}
function handleUpdate(row?: WmProductRecpt) {
  reset()
  const id = row?.recptId || ids.value[0]
  getWmProductRecpt(id).then(r => { form.value = r.data; open.value = true; title.value = '修改产品入库单' })
}
function submitForm() {
  proxy.$refs['formRef'].validate((v: boolean) => {
    if (!v) return
    const isNew = !form.value.recptId
    const fn = isNew ? addWmProductRecpt : updateWmProductRecpt
    fn(form.value).then((response: any) => {
      proxy.$modal.msgSuccess('操作成功')
      if (isNew) {
        const newRecptId = response?.data?.recptId
        if (newRecptId) form.value.recptId = newRecptId
      }
      getList()
    })
  })
}
function handleDelete(row?: WmProductRecpt) {
  const _ids = row?.recptId ? [row.recptId] : ids.value
  proxy.$modal.confirm('是否确认删除？').then(() => delWmProductRecpt(_ids)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}
function handleSelectWarehouse() { warehouseSelectRef.value.open() }
function onWarehouseSelected(row: any) {
  form.value.warehouseId = row.warehouseId
  form.value.warehouseName = row.warehouseName
  form.value.warehouseCode = row.warehouseCode
}
function handleExport() { proxy.download('/mes/wm/product_recpt/export', { ...queryParams.value }, `productrecpt_${Date.now()}.xlsx`) }
function handleConfirm(row?: WmProductRecpt) {
  const target = row || form.value
  if (!target?.recptId || !target?.recptCode) return
  proxy.$modal.confirm(`确认收货 "${target.recptCode}"？系统将更新库存。`).then(() => {
    request({ url: `/mes/wm/product_recpt/confirm/${target.recptId}`, method: 'put' }).then(() => {
      proxy.$modal.msgSuccess('收货确认成功，库存已更新')
      open.value = false
      getList()
    })
  }).catch(() => {})
}
function handlePost(row: WmProductRecpt) {
  proxy.$modal.confirm(`确认过账入库单 "${row.recptCode}"？`).then(() => {
    request({ url: `/mes/wm/product_recpt/post/${row.recptId}`, method: 'put' }).then(() => {
      proxy.$modal.msgSuccess('过账成功')
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
