<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="纸卷号" prop="rollCode">
        <el-input v-model="queryParams.rollCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="物料编码" prop="itemCode">
        <el-input v-model="queryParams.itemCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="物料名称" prop="itemName">
        <el-input v-model="queryParams.itemName" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:wm:roll_detail:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:wm:roll_detail:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:wm:roll_detail:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:wm:roll_detail:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="rolldetailList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="纸卷明细ID" align="center" prop="rollId" width="100" :show-overflow-tooltip="true" />
      <el-table-column label="纸卷号" align="center" prop="rollCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="物料编码" align="center" prop="itemCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="物料名称" align="center" prop="itemName" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="批次编码" align="center" prop="batchCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="供应商编码" align="center" prop="vendorCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:roll_detail:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:wm:roll_detail:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="工厂ID" prop="factoryId">
          <el-input v-model="form.factoryId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="纸卷号" prop="rollCode">
          <el-input v-model="form.rollCode" placeholder="请输入" clearable />
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
        <el-form-item label="批次ID" prop="batchId">
          <el-input v-model="form.batchId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="批次编码" prop="batchCode">
          <el-input v-model="form.batchCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="入库单ID" prop="recptId">
          <el-input v-model="form.recptId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="入库明细ID" prop="recptDetailId">
          <el-input v-model="form.recptDetailId" placeholder="请输入" clearable />
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
        <el-form-item label="供应商原始卷号" prop="vendorRollNo">
          <el-input v-model="form.vendorRollNo" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="父卷ID" prop="parentRollId">
          <el-input v-model="form.parentRollId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="实际门幅" prop="actualWidth">
          <el-input v-model="form.actualWidth" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="实际克重" prop="actualWeightGsm">
          <el-input v-model="form.actualWeightGsm" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="实际长度" prop="actualLength">
          <el-input v-model="form.actualLength" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="实际重量" prop="actualWeight">
          <el-input v-model="form.actualWeight" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="计量单位" prop="unitOfMeasure">
          <el-input v-model="form.unitOfMeasure" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="原始数量" prop="originalQuantity">
          <el-input v-model="form.originalQuantity" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="剩余数量" prop="remainingQuantity">
          <el-input v-model="form.remainingQuantity" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="所在仓库ID" prop="warehouseId">
          <el-input v-model="form.warehouseId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="仓库编码" prop="warehouseCode">
          <el-input v-model="form.warehouseCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="仓库名称" prop="warehouseName">
          <el-input v-model="form.warehouseName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="库区ID" prop="locationId">
          <el-input v-model="form.locationId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="库位ID" prop="areaId">
          <el-input v-model="form.areaId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="库存记录ID" prop="materialStockId">
          <el-input v-model="form.materialStockId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="最近领料单ID" prop="lastIssueId">
          <el-input v-model="form.lastIssueId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="最近生产工单ID" prop="lastWorkorderId">
          <el-input v-model="form.lastWorkorderId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="最近生产工单编码" prop="lastWorkorderCode">
          <el-input v-model="form.lastWorkorderCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="纸卷状态:IN_STOCK-在库,PARTIAL-部分已用,CONSUMED-已用完,RETURNED-已退回,SCRAPPED-已报废" prop="status">
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

<script setup lang="ts" name="WmRollDetail">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import type { WmRollDetailQueryParams, WmRollDetail } from '@/types/api/mes/wm/roll_detail'
import { listWmRollDetail, getWmRollDetail, delWmRollDetail, addWmRollDetail, updateWmRollDetail } from '@/api/mes/wm/roll_detail'

const { proxy } = getCurrentInstance() as any
const dicts = proxy.useDict('sys_yes_no')

const rolldetailList = ref<WmRollDetail[]>([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

const data = reactive({
  form: {} as WmRollDetail,
  queryParams: { pageNum: 1, pageSize: 10 } as WmRollDetailQueryParams,
  rules: {}
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listWmRollDetail(queryParams.value).then(r => {
    rolldetailList.value = r.rows
    total.value = r.total
    loading.value = false
  })
}

function cancel() { open.value = false; reset() }
function reset() { form.value = {} as WmRollDetail; proxy.resetForm('formRef') }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: any[]) { ids.value = s.map(i => i.rollId); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); open.value = true; title.value = '新增纸卷明细表' }
function handleUpdate(row?: WmRollDetail) {
  reset()
  const _id = row?.rollId || ids.value[0]
  getWmRollDetail(_id).then(r => { form.value = r.data; open.value = true; title.value = '修改纸卷明细表' })
}
function submitForm() {
  proxy.$refs['formRef'].validate((v: boolean) => {
    if (v) {
      const fn = form.value.rollId ? updateWmRollDetail : addWmRollDetail
      fn(form.value).then(() => { proxy.$modal.msgSuccess('操作成功'); open.value = false; getList() })
    }
  })
}
function handleDelete(row?: WmRollDetail) {
  const _ids = row?.rollId ? [row.rollId] : ids.value
  proxy.$modal.confirm('是否确认删除？').then(() => delWmRollDetail(_ids)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}
function handleExport() { proxy.download('/mes/wm/roll_detail/export', { ...queryParams.value }, `rolldetail_${Date.now()}.xlsx`) }

getList()
</script>