<template>
  <div class="app-container">
    <!-- 搜索区 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="缺陷编码" prop="defectCode">
        <el-input v-model="queryParams.defectCode" placeholder="请输入缺陷编码" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="缺陷描述" prop="defectName">
        <el-input v-model="queryParams.defectName" placeholder="请输入缺陷描述" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="检验类型" prop="indexType">
        <el-select v-model="queryParams.indexType" placeholder="检验类型" clearable style="width: 200px">
          <el-option v-for="d in mes_qc_type" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="缺陷等级" prop="defectLevel">
        <el-select v-model="queryParams.defectLevel" placeholder="缺陷等级" clearable style="width: 200px">
          <el-option v-for="d in mes_qc_defect_level" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:qc:defect:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:qc:defect:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:qc:defect:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:qc:defect:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="defectList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="缺陷编码" align="center" prop="defectCode" width="130" />
      <el-table-column label="缺陷描述" align="center" prop="defectName" :show-overflow-tooltip="true" />
      <el-table-column label="检验类型" align="center" prop="indexType" width="100">
        <template #default="scope">
          <dict-tag v-if="mes_qc_type" :options="mes_qc_type" :value="scope.row.indexType" />
        </template>
      </el-table-column>
      <el-table-column label="缺陷等级" align="center" prop="defectLevel" width="110">
        <template #default="scope">
          <dict-tag v-if="mes_qc_defect_level" :options="mes_qc_defect_level" :value="scope.row.defectLevel" />
        </template>
      </el-table-column>
      <el-table-column label="处置方法" align="center" prop="processMethod" width="110" />
      <el-table-column label="启用" align="center" width="70">
        <template #default="scope">
          <span :style="{ color: scope.row.enableFlag === '1' ? '#67c23a' : '#909399' }">{{ scope.row.enableFlag === '1' ? '是' : '否' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:qc:defect:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:qc:defect:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="title" v-model="open" width="680px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="缺陷编码" prop="defectCode">
              <el-input v-model="form.defectCode" placeholder="请输入缺陷编码" :disabled="optType === 'edit'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="缺陷描述" prop="defectName">
              <el-input v-model="form.defectName" placeholder="请输入缺陷描述" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="检验类型" prop="indexType">
              <el-select v-model="form.indexType" placeholder="请选择检验类型" style="width: 100%">
                <el-option v-for="d in mes_qc_type" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="缺陷等级" prop="defectLevel">
              <el-select v-model="form.defectLevel" placeholder="请选择缺陷等级" style="width: 100%">
                <el-option v-for="d in mes_qc_defect_level" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="处置方法" prop="processMethod">
              <el-input v-model="form.processMethod" placeholder="如 返工 / 让步接收 / 退货 / 报废" />
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

<script setup lang="ts" name="QcDefect">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import type { QcDefect } from '@/api/mes/qc/defect'
import { listDefect, getDefect, addDefect, updateDefect, delDefect } from '@/api/mes/qc/defect'

const { proxy } = getCurrentInstance() as any
const { mes_qc_type, mes_qc_defect_level } = useDict('mes_qc_type', 'mes_qc_defect_level')

const defectList = ref<QcDefect[]>([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')
const optType = ref('')

const data = reactive({
  form: {} as QcDefect,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    defectCode: undefined,
    defectName: undefined,
    indexType: undefined,
    defectLevel: undefined
  } as any,
  rules: {
    defectCode: [{ required: true, message: '缺陷编码不能为空', trigger: 'blur' }],
    defectName: [{ required: true, message: '缺陷描述不能为空', trigger: 'blur' }],
    indexType: [{ required: true, message: '检验类型不能为空', trigger: 'change' }],
    defectLevel: [{ required: true, message: '缺陷等级不能为空', trigger: 'change' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

/** 查询缺陷列表 */
function getList() {
  loading.value = true
  listDefect(queryParams.value).then((response: any) => {
    defectList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  optType.value = ''
  form.value = {
    defectId: undefined, defectCode: undefined, defectName: undefined,
    indexType: undefined, defectLevel: undefined, processMethod: undefined,
    enableFlag: '1', remark: undefined
  }
  proxy.resetForm('formRef')
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleSelectionChange(selection: QcDefect[]) {
  ids.value = selection.map(item => item.defectId!)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleAdd() {
  reset()
  optType.value = 'add'
  open.value = true
  title.value = '添加缺陷'
}

function handleUpdate(row?: QcDefect) {
  reset()
  optType.value = 'edit'
  const defectId = row?.defectId || ids.value[0]
  getDefect(defectId).then((response: any) => {
    form.value = response.data
    open.value = true
    title.value = '修改缺陷'
  })
}

function submitForm() {
  proxy.$refs['formRef'].validate((valid: boolean) => {
    if (valid) {
      if (form.value.defectId != undefined) {
        updateDefect(form.value).then(() => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addDefect(form.value).then(() => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

function handleDelete(row?: QcDefect) {
  const defectIds = row?.defectId || ids.value
  proxy.$modal.confirm('是否确认删除缺陷编号为"' + defectIds + '"的数据项？').then(function () {
    return delDefect(defectIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handleExport() {
  proxy.download('mes/qc/defect/export', { ...queryParams.value }, `qc_defect_${new Date().getTime()}.xlsx`)
}

getList()
</script>

<style scoped>
:deep(.el-form-item__label) { padding-right: 16px !important; }
</style>
