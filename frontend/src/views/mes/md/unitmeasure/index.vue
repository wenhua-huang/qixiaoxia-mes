<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="单位编码" prop="unitCode">
        <el-input
          v-model="queryParams.unitCode"
          placeholder="请输入单位编码"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="单位名称" prop="unitName">
        <el-input
          v-model="queryParams.unitName"
          placeholder="请输入单位名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否启用" prop="enableFlag">
        <el-select v-model="queryParams.enableFlag" placeholder="是否启用" clearable style="width: 200px">
          <el-option label="是" value="Y" />
          <el-option label="否" value="N" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['mes:md:unitmeasure:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate()"
          v-hasPermi="['mes:md:unitmeasure:edit']"
        >修改</el-button>
      </el-col>
      <!-- 删除按钮已移除：用启停用(enableFlag)替代物理删除 -->
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['mes:md:unitmeasure:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="unitmeasureList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="单位编码" align="center" prop="unitCode" />
      <el-table-column label="单位名称" align="center" prop="unitName" />
      <el-table-column label="主单位编码" align="center" prop="primaryUnit">
        <template #default="scope">
          <span>{{ scope.row.primaryUnit || '主单位' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="换算率" align="center" prop="conversionRate" />
      <el-table-column label="是否启用" align="center" prop="enableFlag">
        <template #default="scope">
          <dict-tag :options="sys_yes_no" :value="scope.row.enableFlag" />
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:md:unitmeasure:edit']">修改</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改单位对话框 -->
    <el-dialog :title="title" v-model="open" width="700px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="130px">
        <el-form-item label="单位编码" prop="unitCode">
          <el-input v-model="form.unitCode" placeholder="请输入单位编码" :disabled="optType === 'edit' || optType === 'view'" />
        </el-form-item>
        <el-form-item label="单位名称" prop="unitName">
          <el-input v-model="form.unitName" placeholder="请输入单位名称" />
        </el-form-item>
        <el-form-item label="主单位" prop="primaryUnit">
          <el-select v-model="form.primaryUnit" placeholder="请选择主单位（留空则本单位为主单位）" clearable style="width: 100%">
            <el-option
              v-for="item in primaryUnitOptions"
              :key="item.unitCode"
              :label="item.unitName + ' (' + item.unitCode + ')'"
              :value="item.unitCode"
              :disabled="item.enableFlag === 'N'"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="换算率" prop="conversionRate" v-if="form.primaryUnit">
          <el-input-number v-model="form.conversionRate" :precision="6" :step="1" :min="0" style="width: 200px" placeholder="请输入与主单位的换算率" />
          <span style="margin-left: 8px; color: #999; font-size: 12px;">表示 1 本单位 = 多少主单位</span>
        </el-form-item>
        <el-form-item label="是否启用" prop="enableFlag">
          <el-radio-group v-model="form.enableFlag">
            <el-radio value="Y">是</el-radio>
            <el-radio value="N">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
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

<script setup lang="ts" name="Unitmeasure">
import { ref, reactive, toRefs } from 'vue'
import { getCurrentInstance } from 'vue'
import type { MdUnitMeasure, UnitMeasureQueryParams } from '@/types/api/mes/md/unitmeasure'
import { listUnitmeasure, listPrimaryUnitmeasure, getUnitmeasure, delUnitmeasure, addUnitmeasure, updateUnitmeasure } from '@/api/mes/md/unitmeasure'

const { proxy } = getCurrentInstance() as any
const { sys_yes_no } = useDict('sys_yes_no')

const unitmeasureList = ref<MdUnitMeasure[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>('')
const primaryUnitOptions = ref<MdUnitMeasure[]>([])
const optType = ref<string | undefined>(undefined)

const data = reactive({
  form: {} as MdUnitMeasure,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    unitCode: undefined,
    unitName: undefined,
    enableFlag: undefined
  } as UnitMeasureQueryParams,
  rules: {
    unitCode: [
      { required: true, message: '单位编码不能为空', trigger: 'blur' },
      { max: 64, message: '单位编码长度不能超过64个字符', trigger: 'blur' }
    ],
    unitName: [
      { required: true, message: '单位名称不能为空', trigger: 'blur' },
      { max: 64, message: '单位名称长度不能超过64个字符', trigger: 'blur' }
    ],
    enableFlag: [
      { required: true, message: '是否启用不能为空', trigger: 'blur' }
    ]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询单位列表 */
function getList() {
  loading.value = true
  listUnitmeasure(queryParams.value).then(response => {
    unitmeasureList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  optType.value = undefined
  form.value = {
    unitId: undefined,
    factoryId: undefined,
    unitCode: undefined,
    unitName: undefined,
    primaryUnit: undefined,
    conversionRate: undefined,
    enableFlag: 'Y',
    remark: undefined
  }
  proxy.resetForm('formRef')
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection: MdUnitMeasure[]) {
  ids.value = selection.map(item => item.unitId!)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/** 加载主单位选项 */
function loadPrimaryUnitOptions() {
  listPrimaryUnitmeasure().then(response => {
    primaryUnitOptions.value = response.data || []
  })
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  optType.value = 'add'
  loadPrimaryUnitOptions()
  open.value = true
  title.value = '添加单位'
}

/** 修改按钮操作 */
function handleUpdate(row?: MdUnitMeasure) {
  reset()
  optType.value = 'edit'
  loadPrimaryUnitOptions()
  const unitId = row?.unitId || ids.value[0]
  getUnitmeasure(unitId).then(response => {
    form.value = response.data
    open.value = true
    title.value = '修改单位'
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs['formRef'].validate((valid: boolean) => {
    if (valid) {
      if (form.value.unitId != undefined) {
        updateUnitmeasure(form.value).then(() => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addUnitmeasure(form.value).then(() => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row?: MdUnitMeasure) {
  const unitIds = row?.unitId || ids.value
  proxy.$modal.confirm('是否确认删除单位编号为"' + unitIds + '"的数据项？').then(function () {
    return delUnitmeasure(unitIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('mes/md/unitmeasure/export', {
    ...queryParams.value
  }, `unitmeasure_${new Date().getTime()}.xlsx`)
}

getList()
</script>

<style scoped>
:deep(.el-form-item__label) { padding-right: 16px !important; }
</style>
