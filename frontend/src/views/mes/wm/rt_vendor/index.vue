<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="退货单编码" prop="rtCode">
        <el-input v-model="queryParams.rtCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="退货单名称" prop="rtName">
        <el-input v-model="queryParams.rtName" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="原入库单编码" prop="recptCode">
        <el-input v-model="queryParams.recptCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:wm:rt_vendor:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:wm:rt_vendor:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:wm:rt_vendor:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:wm:rt_vendor:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="rtvendorList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="退货单ID" align="center" prop="rtId" width="100" :show-overflow-tooltip="true" />
      <el-table-column label="退货单编码" align="center" prop="rtCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="退货单名称" align="center" prop="rtName" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="原入库单编码" align="center" prop="recptCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="供应商编码" align="center" prop="vendorCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="供应商名称" align="center" prop="vendorName" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:rt_vendor:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:wm:rt_vendor:remove']">删除</el-button>
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
        <el-form-item label="原入库单ID" prop="recptId">
          <el-input v-model="form.recptId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="原入库单编码" prop="recptCode">
          <el-input v-model="form.recptCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="供应商ID" prop="vendorId">
          <el-input v-model="form.vendorId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="供应商编码" prop="vendorCode">
          <el-input v-model="form.vendorCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="供应商名称" prop="vendorName">
          <el-input v-model="form.vendorName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="退货仓库ID" prop="warehouseId">
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

<script setup lang="ts" name="WmRtVendor">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import type { WmRtVendorQueryParams, WmRtVendor } from '@/types/api/mes/wm/rt_vendor'
import { listWmRtVendor, getWmRtVendor, delWmRtVendor, addWmRtVendor, updateWmRtVendor } from '@/api/mes/wm/rt_vendor'
import { genSerialCode } from '@/api/mes/sys/autocoderule'

const { proxy } = getCurrentInstance() as any
const dicts = proxy.useDict('sys_yes_no')

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

const data = reactive({
  form: {} as WmRtVendor,
  queryParams: { pageNum: 1, pageSize: 10 } as WmRtVendorQueryParams,
  rules: {}
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
function reset() { autoGenFlag.value = false; form.value = {} as WmRtVendor; proxy.resetForm('formRef') }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: any[]) { ids.value = s.map(i => i.rtId); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); open.value = true; title.value = '新增采购退货单表' }
function handleUpdate(row?: WmRtVendor) {
  reset()
  const _id = row?.rtId || ids.value[0]
  getWmRtVendor(_id).then(r => { form.value = r.data; open.value = true; title.value = '修改采购退货单表' })
}
function submitForm() {
  proxy.$refs['formRef'].validate((v: boolean) => {
    if (v) {
      const fn = form.value.rtId ? updateWmRtVendor : addWmRtVendor
      fn(form.value).then(() => { proxy.$modal.msgSuccess('操作成功'); open.value = false; getList() })
    }
  })
}
function handleDelete(row?: WmRtVendor) {
  const _ids = row?.rtId ? [row.rtId] : ids.value
  proxy.$modal.confirm('是否确认删除？').then(() => delWmRtVendor(_ids)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}
function handleExport() { proxy.download('/mes/wm/rt_vendor/export', { ...queryParams.value }, `rtvendor_${Date.now()}.xlsx`) }

getList()
</script>