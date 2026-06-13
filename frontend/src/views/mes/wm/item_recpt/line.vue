<template>
  <div>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['mes:wm:itemrecpt:add']">新增行</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:wm:itemrecpt:remove']">删除</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="lineList" @selection-change="handleSelectionChange" border>
      <el-table-column type="selection" width="45" align="center" />
      <el-table-column label="物料编码" align="center" prop="itemCode" width="120" />
      <el-table-column label="物料名称" align="center" prop="itemName" :show-overflow-tooltip="true" width="150" />
      <el-table-column label="规格型号" align="center" prop="specification" :show-overflow-tooltip="true" width="120" />
      <el-table-column label="单位" align="center" prop="unitName" width="70" />
      <el-table-column label="入库数量" align="center" prop="quantityRecpt" width="100" />
      <el-table-column label="批次号" align="center" prop="batchCode" width="100" />
      <el-table-column label="库区" align="center" prop="locationId" width="80" />
      <el-table-column label="操作" align="center" width="100">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" size="small" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button link type="primary" icon="Delete" size="small" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 行编辑对话框 -->
    <el-dialog :title="title" v-model="open" width="750px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="物料编码" prop="itemCode">
              <el-input v-model="form.itemCode" readonly placeholder="请选择物料">
                <template #append><el-button icon="Search" @click="handleSelectItem" /></template>
              </el-input>
              <ItemSelect ref="itemSelectRef" @onSelected="onItemSelected" />
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
            <el-form-item label="入库数量" prop="quantityRecpt">
              <el-input-number v-model="form.quantityRecpt" :min="0" :precision="4" style="width:100%" placeholder="数量" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="仓库" prop="warehouseName">
              <el-input v-model="form.warehouseName" readonly />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="库区" prop="locationId" label-width="60px">
              <el-input v-model="form.locationId" placeholder="库区ID" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="库位" prop="areaId" label-width="60px">
              <el-input v-model="form.areaId" placeholder="库位ID" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="生产日期" prop="produceDate">
              <el-date-picker v-model="form.produceDate" type="date" placeholder="生产日期" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="有效期" prop="expireDate">
              <el-date-picker v-model="form.expireDate" type="date" placeholder="有效期" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="供应商批号" prop="lotNumber">
              <el-input v-model="form.lotNumber" placeholder="供应商批号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="内部批次号" prop="batchCode">
              <el-input v-model="form.batchCode" readonly placeholder="保存后自动生成" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
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

<script setup lang="ts" name="WmItemRecptLine">
import { ref, reactive, toRefs, getCurrentInstance, watch } from 'vue'
import type { WmItemRecptLine, WmItemRecptLineQueryParams } from '@/types/api/mes/wm/item_recpt_line'
import { listWmItemRecptLine, getWmItemRecptLine, delWmItemRecptLine, addWmItemRecptLine, updateWmItemRecptLine } from '@/api/mes/wm/item_recpt_line'
import ItemSelect from '@/components/itemSelect/single.vue'
// item selector — 后续集成 MdItem 选择组件

const { proxy } = getCurrentInstance() as any
const itemSelectRef = ref()

const props = defineProps<{ recptId: number; warehouseId?: number; warehouseName?: string; warehouseCode?: string }>()

const lineList = ref<WmItemRecptLine[]>([])
const open = ref(false)
const loading = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

const data = reactive({
  form: {} as WmItemRecptLine,
  queryParams: { pageNum: 1, pageSize: 100 } as WmItemRecptLineQueryParams,
  rules: {
    itemCode: [{ required: true, message: '物料编码不能为空', trigger: 'blur' }],
    quantityRecpt: [{ required: true, message: '入库数量不能为空', trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

watch(() => props.recptId, (val) => {
  if (val) { queryParams.value.recptId = val; getList() }
}, { immediate: true })

function getList() {
  if (!props.recptId) return
  loading.value = true
  listWmItemRecptLine(queryParams.value).then(r => {
    lineList.value = r.rows; total.value = r.total; loading.value = false
  })
}
function handleSelectItem() { itemSelectRef.value.open() }
function onItemSelected(row: any) {
  form.value.itemId = row.itemId
  form.value.itemCode = row.itemCode
  form.value.itemName = row.itemName
  form.value.specification = row.specification
  form.value.unitOfMeasure = row.unitOfMeasure
  form.value.unitName = row.unitName || row.unitOfMeasure
}
function cancel() { open.value = false; reset() }
function reset() { form.value = { recptId: props.recptId, warehouseId: props.warehouseId, warehouseName: props.warehouseName, warehouseCode: props.warehouseCode } as WmItemRecptLine; proxy.resetForm('formRef') }
function handleSelectionChange(s: any[]) { ids.value = s.map(i => i.lineId); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); open.value = true; title.value = '新增行' }
function handleUpdate(row: WmItemRecptLine) {
  reset()
  getWmItemRecptLine(row.lineId).then(r => { form.value = r.data; open.value = true; title.value = '修改行' })
}
function submitForm() {
  proxy.$refs['formRef'].validate((v: boolean) => {
    if (v) {
      const fn = form.value.lineId ? updateWmItemRecptLine : addWmItemRecptLine
      fn(form.value).then(() => { proxy.$modal.msgSuccess('操作成功'); open.value = false; getList() })
    }
  })
}
function handleDelete(row?: WmItemRecptLine) {
  const _ids = row?.lineId ? [row.lineId] : ids.value
  proxy.$modal.confirm('是否确认删除？').then(() => delWmItemRecptLine(_ids)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}

</script>

<style scoped>
:deep(.el-form-item__label) { padding-right: 16px !important; }
</style>
