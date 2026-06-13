<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="装箱单编码" prop="packageCode">
        <el-input v-model="queryParams.packageCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="装箱单名称" prop="packageName">
        <el-input v-model="queryParams.packageName" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="生产工单编码" prop="workorderCode">
        <el-input v-model="queryParams.workorderCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:wm:package:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:wm:package:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:wm:package:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:wm:package:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="packageList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="装箱单ID" align="center" prop="packageId" width="100" :show-overflow-tooltip="true" />
      <el-table-column label="装箱单编码" align="center" prop="packageCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="装箱单名称" align="center" prop="packageName" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="生产工单编码" align="center" prop="workorderCode" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:package:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:wm:package:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="工厂ID" prop="factoryId">
          <el-input v-model="form.factoryId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="装箱单编码" prop="packageCode">
          <el-input v-model="form.packageCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="装箱单名称" prop="packageName">
          <el-input v-model="form.packageName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="父装箱ID" prop="parentId">
          <el-input v-model="form.parentId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="所有父节点ID" prop="ancestors">
          <el-input v-model="form.ancestors" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="装箱类型:CARTON-纸箱,PALLET-托盘,CONTAINER-集装箱" prop="packageType">
          <el-input v-model="form.packageType" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="生产工单ID" prop="workorderId">
          <el-input v-model="form.workorderId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="生产工单编码" prop="workorderCode">
          <el-input v-model="form.workorderCode" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="每箱标准数量" prop="quantityPerBox">
          <el-input v-model="form.quantityPerBox" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="箱数" prop="boxCount">
          <el-input v-model="form.boxCount" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="总数量" prop="totalQuantity">
          <el-input v-model="form.totalQuantity" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="重量" prop="weight">
          <el-input v-model="form.weight" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="长" prop="length">
          <el-input v-model="form.length" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="宽" prop="width">
          <el-input v-model="form.width" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="高" prop="height">
          <el-input v-model="form.height" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="体积" prop="volume">
          <el-input v-model="form.volume" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="箱唛/唛头" prop="boxLabel">
          <el-input v-model="form.boxLabel" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="状态:OPEN-开放,CLOSED-关闭,SHIPPED-已发货" prop="status">
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

<script setup lang="ts" name="WmPackage">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import type { WmPackageQueryParams, WmPackage } from '@/types/api/mes/wm/package'
import { listWmPackage, getWmPackage, delWmPackage, addWmPackage, updateWmPackage } from '@/api/mes/wm/package'

const { proxy } = getCurrentInstance() as any
const dicts = proxy.useDict('sys_yes_no')

const packageList = ref<WmPackage[]>([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

const data = reactive({
  form: {} as WmPackage,
  queryParams: { pageNum: 1, pageSize: 10 } as WmPackageQueryParams,
  rules: {}
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listWmPackage(queryParams.value).then(r => {
    packageList.value = r.rows
    total.value = r.total
    loading.value = false
  })
}

function cancel() { open.value = false; reset() }
function reset() { form.value = {} as WmPackage; proxy.resetForm('formRef') }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: any[]) { ids.value = s.map(i => i.packageId); single.value = s.length !== 1; multiple.value = !s.length }
function handleAdd() { reset(); open.value = true; title.value = '新增装箱单表' }
function handleUpdate(row?: WmPackage) {
  reset()
  const _id = row?.packageId || ids.value[0]
  getWmPackage(_id).then(r => { form.value = r.data; open.value = true; title.value = '修改装箱单表' })
}
function submitForm() {
  proxy.$refs['formRef'].validate((v: boolean) => {
    if (v) {
      const fn = form.value.packageId ? updateWmPackage : addWmPackage
      fn(form.value).then(() => { proxy.$modal.msgSuccess('操作成功'); open.value = false; getList() })
    }
  })
}
function handleDelete(row?: WmPackage) {
  const _ids = row?.packageId ? [row.packageId] : ids.value
  proxy.$modal.confirm('是否确认删除？').then(() => delWmPackage(_ids)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') })
}
function handleExport() { proxy.download('/mes/wm/package/export', { ...queryParams.value }, `package_${Date.now()}.xlsx`) }

getList()
</script>