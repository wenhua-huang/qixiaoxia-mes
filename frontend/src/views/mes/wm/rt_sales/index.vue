<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="退货单编码" prop="rtCode">
        <el-input v-model="queryParams.rtCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="退货单名称" prop="rtName">
        <el-input v-model="queryParams.rtName" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="原销售出库单编码" prop="salesCode">
        <el-input v-model="queryParams.salesCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:wm:rt_sales:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:wm:rt_sales:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:wm:rt_sales:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:wm:rt_sales:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="rtsalesList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="退货单ID" align="center" prop="rtId" width="100" :show-overflow-tooltip="true" />
      <el-table-column label="退货单编码" align="center" prop="rtCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="退货单名称" align="center" prop="rtName" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="原销售出库单编码" align="center" prop="salesCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="客户编码" align="center" prop="clientCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="客户名称" align="center" prop="clientName" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:rt_sales:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:wm:rt_sales:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="工厂ID" prop="factoryId">
          <el-input v-model="form.factoryId" placeholder="请输入" clearable />
        </el-form-item>
        <el-row>
          <el-col :span="16">
            <el-form-item label="退货单号" prop="rtCode">
              <el-input v-model="form.rtCode" placeholder="请输入" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label-width="70">
              <el-switch v-model="autoGenFlag" active-color="#13ce66" active-text="自动生成" @change="handleAutoGenChange" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="退货单名称" prop="rtName">
          <el-input v-model="form.rtName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="原销售出库单ID" prop="salesId">
          <el-input v-model="form.salesId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="原销售出库单编码" prop="salesCode">
          <el-input v-model="form.salesCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="客户ID" prop="clientId">
          <el-input v-model="form.clientId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="客户编码" prop="clientCode">
          <el-input v-model="form.clientCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="客户名称" prop="clientName">
          <el-input v-model="form.clientName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="退货入库仓库ID" prop="warehouseId">
          <el-input v-model="form.warehouseId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="仓库编码" prop="warehouseCode">
          <el-input v-model="form.warehouseCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="仓库名称" prop="warehouseName">
          <el-input v-model="form.warehouseName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="退货日期" prop="rtDate">
          <el-date-picker v-model="form.rtDate" type="datetime" placeholder="选择日期" clearable style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="退货总数量" prop="totalQuantity">
          <el-input v-model="form.totalQuantity" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="退料质检单ID" prop="rqcId">
          <el-input v-model="form.rqcId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="退料质检单编码" prop="rqcCode">
          <el-input v-model="form.rqcCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账" prop="status">
          <el-input v-model="form.status" placeholder="请输入" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="WmRtSales">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import type { WmRtSalesQueryParams, WmRtSales } from '@/types/api/mes/wm/rt_sales'
import { listWmRtSales, getWmRtSales, delWmRtSales, addWmRtSales, updateWmRtSales } from '@/api/mes/wm/rt_sales'
import { genSerialCode } from '@/api/mes/sys/autocoderule'

const { proxy } = getCurrentInstance() as any
const dicts = proxy.useDict('sys_yes_no')

const rtsalesList = ref<WmRtSales[]>([])
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
  form: {} as WmRtSales,
  queryParams: { pageNum: 1, pageSize: 10 } as WmRtSalesQueryParams,
  rules: {}
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listWmRtSales(queryParams.value).then(r => {
    rtsalesList.value = r.rows
    total.value = r.total
    loading.value = false
  })
}

function cancel() { open.value = false; reset() }
function handleAutoGenChange(flag: boolean) {
  if (flag) genSerialCode('RT_SALES_NO').then((r: any) => { form.value.rtCode = r.data })
  else form.value.rtCode = ''
}
function reset() { autoGenFlag.value = false; form.value = {} as WmRtSales; proxy.resetForm('formRef') }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: any[]) { ids.value = s.map(i => i.rtId); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); open.value = true; title.value = '新增销售退货单表' }
function handleUpdate(row?: WmRtSales) {
  reset()
  const _id = row?.rtId || ids.value[0]
  getWmRtSales(_id).then(r => { form.value = r.data; open.value = true; title.value = '修改销售退货单表' })
}
function submitForm() {
  proxy.$refs['formRef'].validate((v: boolean) => {
    if (v) {
      const fn = form.value.rtId ? updateWmRtSales : addWmRtSales
      fn(form.value).then(() => { proxy.$modal.msgSuccess('操作成功'); open.value = false; getList() })
    }
  })
}
function handleDelete(row?: WmRtSales) {
  const _ids = row?.rtId ? [row.rtId] : ids.value
  proxy.$modal.confirm('是否确认删除？').then(() => delWmRtSales(_ids)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}
function handleExport() { proxy.download('/mes/wm/rt_sales/export', { ...queryParams.value }, `rtsales_${Date.now()}.xlsx`) }

getList()
</script>