<template>
  <div>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['mes:wm:transfer:add']">新增行</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:wm:transfer:remove']">删除</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="lineList" @selection-change="handleSelectionChange" border>
      <el-table-column type="selection" width="45" align="center" />
      <el-table-column label="物料编码" align="center" prop="itemCode" width="120" />
      <el-table-column label="物料名称" align="center" prop="itemName" width="150" />
      <el-table-column label="规格型号" align="center" prop="specification" width="120" />
      <el-table-column label="单位" align="center" prop="unitName" width="70" />
      <el-table-column label="调拨数量" align="center" prop="quantityTransfer" width="100" />
      <el-table-column label="批次号" align="center" prop="batchCode" width="100" />
      <el-table-column label="操作" align="center" width="100">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" size="small" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button link type="primary" icon="Delete" size="small" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="700px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="物料编码" prop="itemCode">
              <el-input v-model="form.itemCode" placeholder="请输入物料编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物料名称" prop="itemName">
              <el-input v-model="form.itemName" placeholder="请输入物料名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="规格型号" prop="specification">
              <el-input v-model="form.specification" placeholder="规格型号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位" prop="unitName">
              <el-input v-model="form.unitName" placeholder="单位" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="调拨数量" prop="quantityTransfer">
              <el-input-number v-model="form.quantityTransfer" :min="0" :precision="4" style="width:100%" placeholder="调拨数量" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="批次号" prop="batchCode">
              <el-input v-model="form.batchCode" placeholder="批次号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="源库区" prop="sourceLocationId">
              <el-input v-model="form.sourceLocationId" placeholder="源库区" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="源库位" prop="sourceAreaId">
              <el-input v-model="form.sourceAreaId" placeholder="源库位" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="目标库区" prop="targetLocationId">
              <el-input v-model="form.targetLocationId" placeholder="目标库区" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="目标库位" prop="targetAreaId">
              <el-input v-model="form.targetAreaId" placeholder="目标库位" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" placeholder="备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="WmTransferLine">
import { ref, reactive, toRefs, getCurrentInstance, watch } from 'vue'
import type { WmTransferLineQueryParams, WmTransferLine } from '@/types/api/mes/wm/transfer_line'
import { listWmTransferLine, getWmTransferLine, delWmTransferLine, addWmTransferLine, updateWmTransferLine } from '@/api/mes/wm/transfer_line'

const { proxy } = getCurrentInstance() as any

const props = defineProps<{ transferId: number }>()

const lineList = ref<WmTransferLine[]>([])
const open = ref(false)
const loading = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

const data = reactive({
  form: {} as WmTransferLine,
  queryParams: { pageNum: 1, pageSize: 100 } as WmTransferLineQueryParams,
  rules: {
    itemCode: [{ required: true, message: '物料编码不能为空', trigger: 'blur' }],
    quantityTransfer: [{ required: true, message: '调拨数量不能为空', trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

watch(() => props.transferId, (val) => {
  if (val) { queryParams.value.transferId = val; getList() }
}, { immediate: true })

function getList() {
  if (!props.transferId) return
  loading.value = true
  listWmTransferLine(queryParams.value).then(r => {
    lineList.value = r.rows; total.value = r.total; loading.value = false
  })
}
function cancel() { open.value = false; reset() }
function reset() { form.value = { transferId: props.transferId } as WmTransferLine; proxy.resetForm('formRef') }
function handleSelectionChange(s: any[]) { ids.value = s.map(i => i.lineId); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); open.value = true; title.value = '新增行' }
function handleUpdate(row: WmTransferLine) {
  reset()
  getWmTransferLine(row.lineId).then(r => { form.value = r.data; open.value = true; title.value = '修改行' })
}
function submitForm() {
  proxy.$refs['formRef'].validate((v: boolean) => {
    if (v) {
      const fn = form.value.lineId ? updateWmTransferLine : addWmTransferLine
      fn(form.value).then(() => { proxy.$modal.msgSuccess('操作成功'); open.value = false; getList() })
    }
  })
}
function handleDelete(row?: WmTransferLine) {
  const _ids = row?.lineId ? [row.lineId] : ids.value
  proxy.$modal.confirm('是否确认删除？').then(() => delWmTransferLine(_ids)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}
</script>

<style scoped>
:deep(.el-form-item__label) { padding-right: 16px !important; }
</style>
