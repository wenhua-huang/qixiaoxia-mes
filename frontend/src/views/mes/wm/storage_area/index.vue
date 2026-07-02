<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="库位编码" prop="areaCode">
        <el-input v-model="queryParams.areaCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="库位名称" prop="areaName">
        <el-input v-model="queryParams.areaName" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="库区编码" prop="locationCode">
        <el-input v-model="queryParams.locationCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:wm:storage_area:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:wm:storage_area:edit']">修改</el-button></el-col>
      <!-- 删除按钮已移除：用启停用(enableFlag)替代物理删除 -->
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:wm:storage_area:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="storageareaList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="库位ID" align="center" prop="areaId" width="100" :show-overflow-tooltip="true" />
      <el-table-column label="库位编码" align="center" prop="areaCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="库位名称" align="center" prop="areaName" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="库区编码" align="center" prop="locationCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="库区名称" align="center" prop="locationName" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="仓库编码" align="center" prop="warehouseCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="启用" align="center" width="70">
        <template #default="scope">
          <el-switch v-model="scope.row.enableFlag" active-value="1" inactive-value="0" @change="handleEnableChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:storage_area:edit']">修改</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="16">
            <el-form-item label="库位编码" prop="areaCode">
              <el-input v-model="form.areaCode" placeholder="请输入库位编码" :disabled="optType === 'edit' || optType === 'view'" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label-width="80" v-if="optType === 'add'">
              <el-switch v-model="autoGenFlag" size="small" active-color="#13ce66" @change="handleAutoGenChange" /><span style="margin-left:6px;font-size:12px;color:#13ce66">自动生成</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="库位名称" prop="areaName">
              <el-input v-model="form.areaName" placeholder="请输入库位名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="库区编码" prop="locationCode">
              <el-input v-model="form.locationCode" placeholder="库区编码" :disabled="!!route.query.locationId" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库区名称" prop="locationName">
              <el-input v-model="form.locationName" placeholder="库区名称" :disabled="!!route.query.locationId" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="最大容积" prop="maxVolume">
              <el-input v-model="form.maxVolume" placeholder="请输入" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大承重" prop="maxWeight">
              <el-input v-model="form.maxWeight" placeholder="请输入" clearable />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
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

<script setup lang="ts" name="WmStorageArea">
import { ref, reactive, toRefs, getCurrentInstance, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import type { WmStorageAreaQueryParams, WmStorageArea } from '@/types/api/mes/wm/storage_area'
import { listWmStorageArea, getWmStorageArea, delWmStorageArea, addWmStorageArea, updateWmStorageArea } from '@/api/mes/wm/storage_area'
import { genSerialCode } from '@/api/mes/sys/autocoderule'

const { proxy } = getCurrentInstance() as any
const route = useRoute()

const storageareaList = ref<WmStorageArea[]>([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')
const autoGenFlag = ref(false)
const optType = ref<string | undefined>(undefined)

const data = reactive({
  form: {} as WmStorageArea,
  queryParams: { pageNum: 1, pageSize: 10 } as WmStorageAreaQueryParams,
  rules: {}
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listWmStorageArea(queryParams.value).then(r => {
    storageareaList.value = r.rows
    total.value = r.total
    loading.value = false
  })
}

function cancel() { open.value = false; reset() }
function reset() { optType.value = undefined; autoGenFlag.value = false; form.value = {} as WmStorageArea; proxy.resetForm('formRef') }
function handleAutoGenChange(flag: boolean) {
  if (flag) {
    genSerialCode('AREA_CODE').then((r: any) => {
      // 防止竞态：仅在开关仍为 ON 时才写入
      if (autoGenFlag.value) form.value.areaCode = r.data
    }).catch(() => {
      autoGenFlag.value = false
    })
  } else {
    form.value.areaCode = ''
  }
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: any[]) { ids.value = s.map(i => i.areaId); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); optType.value = 'add'; open.value = true; title.value = '新增库位'
  // 从库区页面进入时，自动带出库区及仓库信息
  if (route.query.locationId) {
    form.value.locationId = Number(route.query.locationId)
    form.value.locationCode = route.query.locationCode as string
    form.value.locationName = route.query.locationName as string
    if (route.query.warehouseId) form.value.warehouseId = Number(route.query.warehouseId)
  }
}
function handleUpdate(row?: WmStorageArea) {
  reset()
  optType.value = 'edit'
  const _id = row?.areaId || ids.value[0]
  getWmStorageArea(_id).then(r => { form.value = r.data; open.value = true; title.value = '修改库位' })
}
function submitForm() {
  proxy.$refs['formRef'].validate((v: boolean) => {
    if (v) {
      const fn = form.value.areaId ? updateWmStorageArea : addWmStorageArea
      fn(form.value).then(() => { proxy.$modal.msgSuccess('操作成功'); open.value = false; getList() })
    }
  })
}
function handleEnableChange(row: any) {
  const newVal = row.enableFlag
  const text = newVal === '1' ? '启用' : '停用'
  proxy.$modal.confirm(`确认要${text}"${row.areaName}"吗？`).then(() => {
    updateWmStorageArea({ areaId: row.areaId, enableFlag: newVal } as any).then(() => proxy.$modal.msgSuccess(`${text}成功`))
  }).catch(() => {
    row.enableFlag = newVal === '1' ? '0' : '1'
    getList()
  })
}

function handleDelete(row?: WmStorageArea) {
  const _ids = row?.areaId ? [row.areaId] : ids.value
  proxy.$modal.confirm('是否确认删除？').then(() => delWmStorageArea(_ids)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}
function handleExport() { proxy.download('/mes/wm/storage_area/export', { ...queryParams.value }, `storagearea_${Date.now()}.xlsx`) }

onMounted(() => {
  if (route.query.locationId) {
    queryParams.value.locationId = Number(route.query.locationId)
  }
  getList()
})
</script>