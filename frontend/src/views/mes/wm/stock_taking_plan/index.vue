<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="盘点方案编码" prop="planCode">
        <el-input v-model="queryParams.planCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="盘点方案名称" prop="planName">
        <el-input v-model="queryParams.planName" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="盘点仓库名称" prop="warehouseName">
        <el-input v-model="queryParams.warehouseName" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:wm:stock_taking_plan:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:wm:stock_taking_plan:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:wm:stock_taking_plan:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:wm:stock_taking_plan:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="stocktakingplanList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="盘点方案ID" align="center" prop="planId" width="100" :show-overflow-tooltip="true" />
      <el-table-column label="盘点方案编码" align="center" prop="planCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="盘点方案名称" align="center" prop="planName" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="盘点仓库名称" align="center" prop="warehouseName" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:stock_taking_plan:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:wm:stock_taking_plan:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="工厂ID" prop="factoryId">
          <el-input v-model="form.factoryId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="盘点方案编码" prop="planCode">
          <el-input v-model="form.planCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="盘点方案名称" prop="planName">
          <el-input v-model="form.planName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="盘点类型:FULL-全盘,PARTIAL-抽盘,CYCLE-循环盘点" prop="planType">
          <el-input v-model="form.planType" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="盘点仓库ID" prop="warehouseId">
          <el-input v-model="form.warehouseId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="盘点仓库名称" prop="warehouseName">
          <el-input v-model="form.warehouseName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="计划盘点日期" prop="planDate">
          <el-date-picker v-model="form.planDate" type="datetime" placeholder="选择日期" clearable style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="状态:PREPARE-准备中,TAKING-盘点中,COMPLETED-已完成,ADJUSTED-已调整" prop="status">
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

<script setup lang="ts" name="WmStockTakingPlan">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import type { WmStockTakingPlanQueryParams, WmStockTakingPlan } from '@/types/api/mes/wm/stock_taking_plan'
import { listWmStockTakingPlan, getWmStockTakingPlan, delWmStockTakingPlan, addWmStockTakingPlan, updateWmStockTakingPlan } from '@/api/mes/wm/stock_taking_plan'

const { proxy } = getCurrentInstance() as any
const dicts = proxy.useDict('sys_yes_no')

const stocktakingplanList = ref<WmStockTakingPlan[]>([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

const data = reactive({
  form: {} as WmStockTakingPlan,
  queryParams: { pageNum: 1, pageSize: 10 } as WmStockTakingPlanQueryParams,
  rules: {}
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listWmStockTakingPlan(queryParams.value).then(r => {
    stocktakingplanList.value = r.rows
    total.value = r.total
    loading.value = false
  })
}

function cancel() { open.value = false; reset() }
function reset() { form.value = {} as WmStockTakingPlan; proxy.resetForm('formRef') }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: any[]) { ids.value = s.map(i => i.planId); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); open.value = true; title.value = '新增盘点方案表' }
function handleUpdate(row?: WmStockTakingPlan) {
  reset()
  const _id = row?.planId || ids.value[0]
  getWmStockTakingPlan(_id).then(r => { form.value = r.data; open.value = true; title.value = '修改盘点方案表' })
}
function submitForm() {
  proxy.$refs['formRef'].validate((v: boolean) => {
    if (v) {
      const fn = form.value.planId ? updateWmStockTakingPlan : addWmStockTakingPlan
      fn(form.value).then(() => { proxy.$modal.msgSuccess('操作成功'); open.value = false; getList() })
    }
  })
}
function handleDelete(row?: WmStockTakingPlan) {
  const _ids = row?.planId ? [row.planId] : ids.value
  proxy.$modal.confirm('是否确认删除？').then(() => delWmStockTakingPlan(_ids)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}
function handleExport() { proxy.download('/mes/wm/stock_taking_plan/export', { ...queryParams.value }, `stocktakingplan_${Date.now()}.xlsx`) }

getList()
</script>