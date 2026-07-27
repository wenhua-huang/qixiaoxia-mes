<template>
  <div>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['mes:wm:sales:edit']">新增行</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:wm:sales:edit']">删除</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="lineList" @selection-change="handleSelectionChange" border>
      <el-table-column type="selection" width="45" align="center" />
      <el-table-column label="物料编码" align="center" prop="itemCode" width="120" />
      <el-table-column label="物料名称" align="center" prop="itemName" width="150" />
      <el-table-column label="规格型号" align="center" prop="specification" width="120" />
      <el-table-column label="单位" align="center" prop="unitName" width="70" />
      <el-table-column label="出库数量" align="center" prop="quantitySales" width="100" />
      <el-table-column label="箱数" align="center" prop="quantityBox" width="70" />
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
              <el-input v-model="form.itemCode" readonly />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物料名称" prop="itemName">
              <el-input v-model="form.itemName" readonly />
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
              <el-input v-model="form.unitName" readonly />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="出库数量" prop="quantitySales">
              <el-input-number v-model="form.quantitySales" :min="0" :precision="4" style="width:100%" placeholder="出库数量" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="箱数" prop="quantityBox">
              <el-input v-model="form.quantityBox" placeholder="箱数" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="批次号" prop="batchCode">
              <el-input v-model="form.batchCode" placeholder="批次号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="仓库" prop="warehouseId">
              <el-input v-model="form.warehouseId" placeholder="仓库" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="库区" prop="locationId">
              <el-input v-model="form.locationId" placeholder="库区" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="库位" prop="areaId">
              <el-input v-model="form.areaId" placeholder="库位" />
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

<script setup lang="ts" name="WmProductSalesLine">
import { ref, reactive, toRefs, getCurrentInstance, watch } from 'vue'
import type { WmProductSalesLineQueryParams, WmProductSalesLine } from '@/types/api/mes/wm/product_sales_line'
import { listWmProductSalesLine, getWmProductSalesLine, delWmProductSalesLine, addWmProductSalesLine, updateWmProductSalesLine } from '@/api/mes/wm/product_sales_line'

const { proxy } = getCurrentInstance() as any

const props = defineProps<{ salesId: number }>()

const lineList = ref<WmProductSalesLine[]>([])
const open = ref(false)
const loading = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

const data = reactive({
  form: {} as WmProductSalesLine,
  queryParams: { pageNum: 1, pageSize: 100 } as WmProductSalesLineQueryParams,
  rules: {}
})
const { queryParams, form, rules } = toRefs(data)

watch(() => props.salesId, (val: number | undefined) => {
  if (val) { queryParams.value.salesId = val; getList() }
}, { immediate: true })

function getList() {
  if (!props.salesId) return
  loading.value = true
  listWmProductSalesLine(queryParams.value).then(r => {
    lineList.value = r.rows; total.value = r.total; loading.value = false
  })
}
function cancel() { open.value = false; reset() }
function reset() { form.value = { salesId: props.salesId } as WmProductSalesLine; proxy.resetForm('formRef') }
function handleSelectionChange(s: any[]) { ids.value = s.map(i => i.lineId); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); open.value = true; title.value = '新增行' }
function handleUpdate(row: WmProductSalesLine) {
  reset()
  getWmProductSalesLine(row.lineId).then(r => { form.value = r.data; open.value = true; title.value = '修改行' })
}
function submitForm() {
  proxy.$refs['formRef'].validate((v: boolean) => {
    if (v) {
      const fn = form.value.lineId ? updateWmProductSalesLine : addWmProductSalesLine
      fn(form.value).then(() => { proxy.$modal.msgSuccess('操作成功'); open.value = false; getList() })
    }
  })
}
function handleDelete(row?: WmProductSalesLine) {
  const _ids = row?.lineId ? [row.lineId] : ids.value
  proxy.$modal.confirm('是否确认删除？').then(() => delWmProductSalesLine(_ids)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}
</script>
