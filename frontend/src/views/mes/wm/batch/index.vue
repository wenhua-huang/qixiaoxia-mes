<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="批次编码" prop="batchCode">
        <el-input v-model="queryParams.batchCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="批次名称" prop="batchName">
        <el-input v-model="queryParams.batchName" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="物料编码" prop="itemCode">
        <el-input v-model="queryParams.itemCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:wm:batch:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:wm:batch:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:wm:batch:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:wm:batch:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="batchList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="批次ID" align="center" prop="batchId" width="100" :show-overflow-tooltip="true" />
      <el-table-column label="批次编码" align="center" prop="batchCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="批次名称" align="center" prop="batchName" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="物料编码" align="center" prop="itemCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="物料名称" align="center" prop="itemName" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="供应商编码" align="center" prop="vendorCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:batch:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:wm:batch:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="工厂ID" prop="factoryId">
          <el-input v-model="form.factoryId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="批次编码" prop="batchCode">
          <el-input v-model="form.batchCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="批次名称" prop="batchName">
          <el-input v-model="form.batchName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="物料ID" prop="itemId">
          <el-input v-model="form.itemId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="物料编码" prop="itemCode">
          <el-input v-model="form.itemCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="物料名称" prop="itemName">
          <el-input v-model="form.itemName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="规格型号" prop="specification">
          <el-input v-model="form.specification" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="生产日期" prop="produceDate">
          <el-date-picker v-model="form.produceDate" type="datetime" placeholder="选择日期" clearable style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="有效期至" prop="expireDate">
          <el-date-picker v-model="form.expireDate" type="datetime" placeholder="选择日期" clearable style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="入库日期" prop="recptDate">
          <el-date-picker v-model="form.recptDate" type="datetime" placeholder="选择日期" clearable style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
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
        <el-form-item label="生产工单ID" prop="workorderId">
          <el-input v-model="form.workorderId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="生产工单编码" prop="workorderCode">
          <el-input v-model="form.workorderCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="质量状态:NORMAL-正常,HOLD-冻结,REJECT-不合格" prop="qualityStatus">
          <el-input v-model="form.qualityStatus" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="批次状态:ACTIVE-活跃,CLOSED-关闭,EXPIRED-过期" prop="status">
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

<script setup lang="ts" name="WmBatch">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import type { WmBatchQueryParams, WmBatch } from '@/types/api/mes/wm/batch'
import { listWmBatch, getWmBatch, delWmBatch, addWmBatch, updateWmBatch } from '@/api/mes/wm/batch'

const { proxy } = getCurrentInstance() as any
const dicts = proxy.useDict('sys_yes_no')

const batchList = ref<WmBatch[]>([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

const data = reactive({
  form: {} as WmBatch,
  queryParams: { pageNum: 1, pageSize: 10 } as WmBatchQueryParams,
  rules: {}
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listWmBatch(queryParams.value).then(r => {
    batchList.value = r.rows
    total.value = r.total
    loading.value = false
  })
}

function cancel() { open.value = false; reset() }
function reset() { form.value = {} as WmBatch; proxy.resetForm('formRef') }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: any[]) { ids.value = s.map(i => i.batchId); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); open.value = true; title.value = '新增批次记录表' }
function handleUpdate(row?: WmBatch) {
  reset()
  const _id = row?.batchId || ids.value[0]
  getWmBatch(_id).then(r => { form.value = r.data; open.value = true; title.value = '修改批次记录表' })
}
function submitForm() {
  proxy.$refs['formRef'].validate((v: boolean) => {
    if (v) {
      const fn = form.value.batchId ? updateWmBatch : addWmBatch
      fn(form.value).then(() => { proxy.$modal.msgSuccess('操作成功'); open.value = false; getList() })
    }
  })
}
function handleDelete(row?: WmBatch) {
  const _ids = row?.batchId ? [row.batchId] : ids.value
  proxy.$modal.confirm('是否确认删除？').then(() => delWmBatch(_ids)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}
function handleExport() { proxy.download('/mes/wm/batch/export', { ...queryParams.value }, `batch_${Date.now()}.xlsx`) }

getList()
</script>