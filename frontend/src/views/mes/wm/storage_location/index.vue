<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="库区编码" prop="locationCode">
        <el-input v-model="queryParams.locationCode" placeholder="请输入" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="库区名称" prop="locationName">
        <el-input v-model="queryParams.locationName" placeholder="请输入" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="仓库编码" prop="warehouseCode">
        <el-input v-model="queryParams.warehouseCode" placeholder="请输入" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['mes:wm:location:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:wm:location:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:wm:location:remove']">删除</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="locationList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="45" align="center" />
      <el-table-column label="库区编码" align="center" prop="locationCode" width="140" />
      <el-table-column label="库区名称" align="center" prop="locationName" :show-overflow-tooltip="true" />
      <el-table-column label="仓库编码" align="center" prop="warehouseCode" width="120" />
      <el-table-column label="仓库名称" align="center" prop="warehouseName" width="150" />
      <el-table-column label="面积(㎡)" align="center" prop="area" width="90" />
      <el-table-column label="启用" align="center" width="80">
        <template #default="scope">
          {{ scope.row.enableFlag === '1' ? '是' : '否' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="220" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" size="small" @click="handleArea(scope.row.locationId)" v-hasPermi="['mes:wm:area:list']">库位</el-button>
          <el-button link type="primary" size="small" @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:location:edit']">修改</el-button>
          <el-button link type="primary" size="small" @click="handleDelete(scope.row)" v-hasPermi="['mes:wm:location:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row>
          <el-col :span="16">
            <el-form-item label="库区编码" prop="locationCode">
              <el-input v-model="form.locationCode" placeholder="请输入库区编码" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label-width="70" v-if="!form.locationId">
              <el-switch v-model="autoGenFlag" active-color="#13ce66" @change="handleAutoGenChange" /><span style="margin-left:6px;font-size:12px;color:#13ce66">自动生成</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="库区名称" prop="locationName">
              <el-input v-model="form.locationName" placeholder="请输入库区名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="仓库" prop="warehouseId">
              <el-input v-model="form.warehouseId" placeholder="仓库ID" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="仓库名称" prop="warehouseName">
              <el-input v-model="form.warehouseName" placeholder="仓库名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="面积(㎡)" prop="area">
              <el-input-number v-model="form.area" :min="0" :precision="2" style="width:100%" placeholder="面积" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启用" prop="enableFlag">
              <el-radio-group v-model="form.enableFlag">
                <el-radio v-for="d in dicts.sys_yes_no" :key="d.value" :value="d.value">{{ d.label }}</el-radio>
              </el-radio-group>
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
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="WmStorageLocation">
import { ref, reactive, toRefs, getCurrentInstance, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { WmStorageLocationQueryParams, WmStorageLocation } from '@/types/api/mes/wm/storage_location'
import { listWmStorageLocation, getWmStorageLocation, delWmStorageLocation, addWmStorageLocation, updateWmStorageLocation } from '@/api/mes/wm/storage_location'
import { genSerialCode } from '@/api/mes/sys/autocoderule'

const { proxy } = getCurrentInstance() as any
const route = useRoute()
const router = useRouter()
const dicts = proxy.useDict('sys_yes_no')

const locationList = ref<WmStorageLocation[]>([])
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
  form: {} as WmStorageLocation,
  queryParams: { pageNum: 1, pageSize: 10 } as WmStorageLocationQueryParams,
  rules: {
    locationCode: [{ required: true, message: '库区编码不能为空', trigger: 'blur' }],
    locationName: [{ required: true, message: '库区名称不能为空', trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listWmStorageLocation(queryParams.value).then(r => { locationList.value = r.rows; total.value = r.total; loading.value = false })
}
function cancel() { open.value = false; reset() }
function reset() { autoGenFlag.value = false; form.value = {} as WmStorageLocation; proxy.resetForm('formRef') }
function handleAutoGenChange(flag: boolean) {
  if (flag) genSerialCode('LOCATION_CODE').then((r: any) => { form.value.locationCode = r.data })
  else form.value.locationCode = ''
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: any[]) { ids.value = s.map(i => i.locationId); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); open.value = true; title.value = '新增库区' }
function handleUpdate(row?: WmStorageLocation) {
  reset()
  const id = row?.locationId || ids.value[0]
  getWmStorageLocation(id).then(r => { form.value = r.data; open.value = true; title.value = '修改库区' })
}
function submitForm() {
  proxy.$refs['formRef'].validate((v: boolean) => {
    if (v) {
      const fn = form.value.locationId ? updateWmStorageLocation : addWmStorageLocation
      fn(form.value).then(() => { proxy.$modal.msgSuccess('操作成功'); open.value = false; getList() })
    }
  })
}
function handleDelete(row?: WmStorageLocation) {
  const _ids = row?.locationId ? [row.locationId] : ids.value
  proxy.$modal.confirm('是否确认删除？').then(() => delWmStorageLocation(_ids)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}
function handleArea(locationId: number) {
  router.push({ path: '/mes/wm/storage_area', query: { locationId } })
}

onMounted(() => {
  if (route.query.warehouseId) {
    queryParams.value.warehouseId = Number(route.query.warehouseId)
  }
  getList()
})
</script>

<style scoped>
:deep(.el-form-item__label) { padding-right: 16px !important; }
</style>
