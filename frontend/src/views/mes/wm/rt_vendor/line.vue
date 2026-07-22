<template>
  <div>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['mes:wm:rt_vendor:add']">新增行</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:wm:rt_vendor:remove']">删除</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="lineList" @selection-change="handleSelectionChange" border>
      <el-table-column type="selection" width="45" align="center" />
      <el-table-column label="物料编码" align="center" prop="itemCode" width="120" />
      <el-table-column label="物料名称" align="center" prop="itemName" width="150" :show-overflow-tooltip="true" />
      <el-table-column label="规格型号" align="center" prop="specification" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="单位" align="center" prop="unitName" width="70" />
      <el-table-column label="退货数量" align="center" prop="quantityRt" width="100" />
      <el-table-column label="批次号" align="center" prop="batchCode" width="110" :show-overflow-tooltip="true" />
      <el-table-column label="仓库" align="center" prop="warehouseName" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="120">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" size="small" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button link type="primary" icon="Delete" size="small" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="800px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="物料" prop="itemCode">
              <el-input v-model="form.itemCode" readonly placeholder="请选择物料">
                <template #append><el-button icon="Search" @click="handleSelectItem" /></template>
              </el-input>
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
              <el-input v-model="form.specification" readonly />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位" prop="unitName">
              <el-input v-model="form.unitName" readonly />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="退货数量" prop="quantityRt">
              <el-input-number v-model="form.quantityRt" :min="0" :precision="4" style="width:100%" placeholder="退货数量" />
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
            <el-form-item label="仓库" prop="warehouseName">
              <el-input v-model="form.warehouseName" readonly placeholder="请选择仓库">
                <template #append><el-button icon="Search" @click="handleSelectWarehouse" /></template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="库区" prop="locationName">
              <el-input v-model="form.locationName" readonly placeholder="请选择库区">
                <template #append><el-button icon="Search" @click="handleSelectLocation" :disabled="!form.warehouseId" /></template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12" v-if="form.purOrderLineId">
            <el-form-item label="原采购量">
              <el-input :model-value="form.quantityOrdered" readonly />
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

    <ItemSelect ref="itemSelectRef" @onSelected="onItemSelected" />
    <WarehouseSelect ref="warehouseSelectRef" @onSelected="onWarehouseSelected" />
    <LocationSelect ref="locationSelectRef" @onSelected="onLocationSelected" />
  </div>
</template>

<script setup lang="ts" name="WmRtVendorLine">
import { ref, reactive, toRefs, getCurrentInstance, watch } from 'vue'
import type { WmRtVendorLineQueryParams, WmRtVendorLine } from '@/types/api/mes/wm/rt_vendor_line'
import { listWmRtVendorLine, getWmRtVendorLine, delWmRtVendorLine, addWmRtVendorLine, updateWmRtVendorLine } from '@/api/mes/wm/rt_vendor_line'
import ItemSelect from '@/components/itemSelect/single.vue'
import WarehouseSelect from '@/components/warehouseSelect/single.vue'
import LocationSelect from '@/components/locationSelect/single.vue'

const { proxy } = getCurrentInstance() as any

const props = defineProps<{ rtId: number; warehouseId?: number; warehouseCode?: string; warehouseName?: string }>()

const lineList = ref<WmRtVendorLine[]>([])
const open = ref(false)
const loading = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')
const itemSelectRef = ref()
const warehouseSelectRef = ref()
const locationSelectRef = ref()

const data = reactive({
  form: {} as WmRtVendorLine,
  queryParams: { pageNum: 1, pageSize: 100 } as WmRtVendorLineQueryParams,
  rules: {
    itemCode: [{ required: true, message: '请选择物料', trigger: 'change' }],
    quantityRt: [{ required: true, message: '请输入退货数量', trigger: 'blur' }]
  } as Record<string, any>
})
const { queryParams, form, rules } = toRefs(data)

watch(() => props.rtId, (val) => {
  if (val) { queryParams.value.rtId = val; getList() }
}, { immediate: true })

function getList() {
  if (!props.rtId) return
  loading.value = true
  listWmRtVendorLine(queryParams.value).then(r => {
    lineList.value = r.rows; total.value = r.total; loading.value = false
  })
}
function cancel() { open.value = false; reset() }
function reset() {
  form.value = {
    rtId: props.rtId,
    warehouseId: props.warehouseId,
    warehouseCode: props.warehouseCode,
    warehouseName: props.warehouseName
  } as WmRtVendorLine
  proxy.resetForm('formRef')
}
function handleSelectionChange(s: any[]) { ids.value = s.map(i => i.lineId); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); open.value = true; title.value = '新增行' }
function handleUpdate(row: WmRtVendorLine) {
  reset()
  getWmRtVendorLine(row.lineId).then(r => { form.value = r.data; open.value = true; title.value = '修改行' })
}
function submitForm() {
  proxy.$refs['formRef'].validate((v: boolean) => {
    if (v) {
      const fn = form.value.lineId ? updateWmRtVendorLine : addWmRtVendorLine
      fn(form.value).then(() => { proxy.$modal.msgSuccess('操作成功'); open.value = false; getList() })
    }
  })
}
function handleDelete(row?: WmRtVendorLine) {
  const _ids = row?.lineId ? [row.lineId] : ids.value
  proxy.$modal.confirm('是否确认删除？').then(() => delWmRtVendorLine(_ids)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}

// 选择器回调
function handleSelectItem() { itemSelectRef.value.open() }
function onItemSelected(row: any) {
  form.value.itemId = row.itemId
  form.value.itemCode = row.itemCode
  form.value.itemName = row.itemName
  form.value.specification = row.specification
  form.value.unitOfMeasure = row.unitOfMeasure
  form.value.unitName = row.unitName
}
function handleSelectWarehouse() { warehouseSelectRef.value.open() }
function onWarehouseSelected(row: any) {
  form.value.warehouseId = row.warehouseId
  form.value.warehouseCode = row.warehouseCode
  form.value.warehouseName = row.warehouseName
}
function handleSelectLocation() { locationSelectRef.value.open() }
function onLocationSelected(row: any) {
  form.value.locationId = row.locationId
  form.value.locationName = row.locationName
}
</script>
