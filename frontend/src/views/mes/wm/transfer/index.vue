<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="调拨单号" prop="transferCode">
        <el-input v-model="queryParams.transferCode" placeholder="请输入" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width:110px">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已确认" value="CONFIRMED" />
          <el-option label="已过账" value="POSTED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['mes:wm:transfer:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:wm:transfer:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:wm:transfer:remove']">删除</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="transferList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="45" align="center" />
      <el-table-column label="调拨单号" align="center" prop="transferCode" width="150">
        <template #default="scope">
          <el-button link type="primary" size="small" @click="handleView(scope.row)">{{ scope.row.transferCode }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="调拨名称" align="center" prop="transferName" :show-overflow-tooltip="true" />
      <el-table-column label="源仓库" align="center" prop="sourceWarehouseName" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="目标仓库" align="center" prop="targetWarehouseName" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="调拨日期" align="center" prop="transferDate" width="110" />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'CONFIRMED' ? 'success' : scope.row.status === 'POSTED' ? '' : 'info'" size="small">{{ scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="140" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" size="small" v-if="scope.row.status === 'DRAFT'" @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:transfer:edit']">修改</el-button>
          <el-button link type="primary" size="small" v-if="scope.row.status === 'DRAFT'" @click="handleDelete(scope.row)" v-hasPermi="['mes:wm:transfer:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="750px" append-to-body :close-on-click-modal="false">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="单据头" name="header">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
            <el-row>
              <el-col :span="16">
                <el-form-item label="调拨单号" prop="transferCode">
                  <el-input v-model="form.transferCode" placeholder="调拨单号" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label-width="70" v-if="!form.transferId">
                  <el-switch v-model="autoGenFlag" active-color="#13ce66" active-text="自动生成" @change="handleAutoGenChange" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="24">
                <el-form-item label="调拨名称" prop="transferName">
                  <el-input v-model="form.transferName" placeholder="调拨名称" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item label="源仓库" prop="sourceWarehouseId">
                  <el-input v-model="form.sourceWarehouseId" placeholder="源仓库ID" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="目标仓库" prop="targetWarehouseId">
                  <el-input v-model="form.targetWarehouseId" placeholder="目标仓库ID" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item label="调拨日期" prop="transferDate">
                  <el-date-picker v-model="form.transferDate" type="datetime" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="24">
                <el-form-item label="备注" prop="remark">
                  <el-input v-model="form.remark" type="textarea" placeholder="备注" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="调拨明细" name="lines" v-if="form.transferId">
          <WmTransferLine :transferId="form.transferId!" />
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">保存单据</el-button>
          <el-button @click="cancel">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="WmTransfer">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import type { WmTransferQueryParams, WmTransfer } from '@/types/api/mes/wm/transfer'
import { listWmTransfer, getWmTransfer, delWmTransfer, addWmTransfer, updateWmTransfer } from '@/api/mes/wm/transfer'
import { genSerialCode } from '@/api/mes/sys/autocoderule'
import WmTransferLine from './line.vue'

const { proxy } = getCurrentInstance() as any

const transferList = ref<WmTransfer[]>([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')
const autoGenFlag = ref(false)
const activeTab = ref('header')

const data = reactive({
  form: {} as WmTransfer,
  queryParams: { pageNum: 1, pageSize: 10 } as WmTransferQueryParams,
  rules: {
    transferCode: [{ required: true, message: '调拨单号不能为空', trigger: 'blur' }],
    sourceWarehouseId: [{ required: true, message: '源仓库不能为空', trigger: 'blur' }],
    targetWarehouseId: [{ required: true, message: '目标仓库不能为空', trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listWmTransfer(queryParams.value).then(r => { transferList.value = r.rows; total.value = r.total; loading.value = false })
}
function cancel() { open.value = false; reset() }
function handleAutoGenChange(flag: boolean) {
  if (flag) genSerialCode('TRANSFER_NO').then((r: any) => { form.value.transferCode = r.data })
  else form.value.transferCode = ''
}
function reset() { autoGenFlag.value = false; form.value = {} as WmTransfer; activeTab.value = 'header'; proxy.resetForm('formRef') }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: any[]) { ids.value = s.map(i => i.transferId); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); open.value = true; title.value = '新增调拨单' }
function handleView(row: WmTransfer) {
  reset()
  getWmTransfer(row.transferId).then(r => { form.value = r.data; open.value = true; title.value = '查看调拨单'; activeTab.value = 'lines' })
}
function handleUpdate(row?: WmTransfer) {
  reset()
  const id = row?.transferId || ids.value[0]
  getWmTransfer(id).then(r => { form.value = r.data; open.value = true; title.value = '修改调拨单' })
}
function submitForm() {
  proxy.$refs['formRef'].validate((v: boolean) => {
    if (v) {
      const fn = form.value.transferId ? updateWmTransfer : addWmTransfer
      fn(form.value).then((r: any) => {
        proxy.$modal.msgSuccess('操作成功')
        if (!form.value.transferId && r.data?.transferId) form.value.transferId = r.data.transferId
        open.value = false; getList()
      })
    }
  })
}
function handleDelete(row?: WmTransfer) {
  const _ids = row?.transferId ? [row.transferId] : ids.value
  proxy.$modal.confirm('是否确认删除？').then(() => delWmTransfer(_ids)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}

getList()
</script>

<style scoped>
:deep(.el-form-item__label) { padding-right: 16px !important; }
</style>
