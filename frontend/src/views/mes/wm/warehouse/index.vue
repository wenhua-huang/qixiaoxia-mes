<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="仓库编码" prop="warehouseCode">
        <el-input v-model="queryParams.warehouseCode" placeholder="请输入仓库编码" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="仓库名称" prop="warehouseName">
        <el-input v-model="queryParams.warehouseName" placeholder="请输入仓库名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['mes:wm:warehouse:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:wm:warehouse:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:wm:warehouse:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" size="small" @click="handleExport" v-hasPermi="['mes:wm:warehouse:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="warehouseList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="45" align="center" />
      <el-table-column label="仓库编码" align="center" prop="warehouseCode" width="140">
        <template #default="scope">
          <el-button link type="primary" size="small" @click="handleView(scope.row)">
            {{ scope.row.warehouseCode }}
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="仓库名称" align="center" prop="warehouseName" :show-overflow-tooltip="true" />
      <el-table-column label="仓库类型" align="center" prop="warehouseType" width="90">
        <template #default="scope">
          <dict-tag :options="mes_warehouse_type" :value="scope.row.warehouseType" />
        </template>
      </el-table-column>
      <el-table-column label="地址" align="center" prop="address" :show-overflow-tooltip="true" width="200" />
      <el-table-column label="面积(㎡)" align="center" prop="area" width="90" />
      <el-table-column label="负责人" align="center" prop="charge" width="90" />
      <el-table-column label="启用" align="center" width="80">
        <template #default="scope">
          <el-switch v-model="scope.row.enableFlag" active-value="1" inactive-value="0" @change="handleEnableChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="220" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" size="small" @click="handleLocation(scope.row.warehouseId)" v-hasPermi="['mes:wm:location:list']">库区</el-button>
          <el-button link type="primary" size="small" @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:warehouse:edit']">修改</el-button>
          <el-button link type="primary" size="small" @click="handleDelete(scope.row)" v-hasPermi="['mes:wm:warehouse:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="680px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-row>
          <el-col :span="16">
            <el-form-item label="仓库编码" prop="warehouseCode">
              <el-input v-model="form.warehouseCode" placeholder="请输入仓库编码" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label-width="70" v-if="!form.warehouseId">
              <el-switch v-model="autoGenFlag" active-color="#13ce66" @change="handleAutoGenChange" /><span style="margin-left:6px;font-size:12px;color:#13ce66">自动生成</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="仓库名称" prop="warehouseName">
              <el-input v-model="form.warehouseName" placeholder="请输入仓库名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="仓库类型" prop="warehouseType">
              <el-select v-model="form.warehouseType" placeholder="请选择" clearable style="width:100%">
                <el-option v-for="d in mes_warehouse_type" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人" prop="charge">
              <el-input v-model="form.charge" placeholder="请输入负责人" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="面积(㎡)" prop="area">
              <el-input-number v-model="form.area" :min="0" :precision="2" style="width:100%" placeholder="请输入面积" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否启用" prop="enableFlag">
              <el-radio-group v-model="form.enableFlag">
                <el-radio value="1">是</el-radio>
                <el-radio value="0">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="地址" prop="address">
              <el-input v-model="form.address" type="textarea" placeholder="请输入仓库地址" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
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

<script setup lang="ts" name="WmWarehouse">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import type { WmWarehouseQueryParams, WmWarehouse } from '@/types/api/mes/wm/warehouse'
import { listWmWarehouse, getWmWarehouse, delWmWarehouse, addWmWarehouse, updateWmWarehouse } from '@/api/mes/wm/warehouse'
import { genSerialCode } from '@/api/mes/sys/autocoderule'

const { proxy } = getCurrentInstance() as any
const router = useRouter()
const { mes_warehouse_type } = useDict('mes_warehouse_type')

const warehouseList = ref<WmWarehouse[]>([])
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
  form: {} as WmWarehouse,
  queryParams: { pageNum: 1, pageSize: 10 } as WmWarehouseQueryParams,
  rules: {
    warehouseCode: [{ required: true, message: '仓库编码不能为空', trigger: 'blur' }],
    warehouseName: [{ required: true, message: '仓库名称不能为空', trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listWmWarehouse(queryParams.value).then(r => { warehouseList.value = r.rows; total.value = r.total; loading.value = false })
}
function cancel() { open.value = false; reset() }
function reset() { form.value = {} as WmWarehouse; autoGenFlag.value = false; proxy.resetForm('formRef') }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: any[]) { ids.value = s.map(i => i.warehouseId); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); open.value = true; title.value = '新增仓库' }
function handleView(row: WmWarehouse) { reset(); form.value = { ...row }; open.value = true; title.value = '查看仓库' }
function handleUpdate(row?: WmWarehouse) {
  reset()
  const id = row?.warehouseId || ids.value[0]
  getWmWarehouse(id).then(r => { form.value = r.data; open.value = true; title.value = '修改仓库' })
}
function submitForm() {
  proxy.$refs['formRef'].validate((v: boolean) => {
    if (v) {
      const fn = form.value.warehouseId ? updateWmWarehouse : addWmWarehouse
      fn(form.value).then(() => { proxy.$modal.msgSuccess('操作成功'); open.value = false; getList() })
    }
  })
}
function handleDelete(row?: WmWarehouse) {
  const _ids = row?.warehouseId ? [row.warehouseId] : ids.value
  proxy.$modal.confirm('是否确认删除？').then(() => delWmWarehouse(_ids)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}
function handleLocation(warehouseId: number) {
  router.push({ path: '/mes/wm/storage_location', query: { warehouseId } })
}
function handleAutoGenChange(flag: boolean) {
  if (flag) {
    genSerialCode('WAREHOUSE_CODE').then((r: any) => { form.value.warehouseCode = r.data })
  } else {
    form.value.warehouseCode = ''
  }
}
function handleExport() { proxy.download('/mes/wm/warehouse/export', { ...queryParams.value }, `warehouse_${Date.now()}.xlsx`) }
function handleEnableChange(row: WmWarehouse) {
  const newVal = row.enableFlag
  const text = newVal === '1' ? '启用' : '停用'
  proxy.$modal.confirm(`确认要${text}"${row.warehouseName}"吗？`).then(() => {
    updateWmWarehouse(row).then(() => proxy.$modal.msgSuccess(`${text}成功`))
  }).catch(() => {
    row.enableFlag = newVal === '1' ? '0' : '1'  // 取消 → 回退
    getList()
  })
}
getList()
</script>

<style scoped>
:deep(.el-form-item__label) { padding-right: 16px !important; }
</style>
