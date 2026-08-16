<template>
  <div class="app-container">
    <!-- 搜索区 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="检测项编码" prop="indexCode">
        <el-input v-model="queryParams.indexCode" placeholder="请输入检测项编码" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="检测项名称" prop="indexName">
        <el-input v-model="queryParams.indexName" placeholder="请输入检测项名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="检验类型" prop="indexType">
        <el-select v-model="queryParams.indexType" placeholder="检验类型" clearable style="width: 200px">
          <el-option v-for="d in mes_qc_type" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="值类型" prop="qcResultType">
        <el-select v-model="queryParams.qcResultType" placeholder="值类型" clearable style="width: 200px">
          <el-option v-for="d in mes_qc_result_type" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="是否启用" prop="enableFlag">
        <el-select v-model="queryParams.enableFlag" placeholder="是否启用" clearable style="width: 200px">
          <el-option label="是" value="1" />
          <el-option label="否" value="0" />
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
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:qc:index:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:qc:index:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:qc:index:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:qc:index:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="indexList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="检测项编码" align="center" prop="indexCode" width="150" />
      <el-table-column label="检测项名称" align="center" prop="indexName" :show-overflow-tooltip="true" />
      <el-table-column label="检验类型" align="center" prop="indexType" width="100">
        <template #default="scope">
          <dict-tag v-if="mes_qc_type" :options="mes_qc_type" :value="scope.row.indexType" />
        </template>
      </el-table-column>
      <el-table-column label="值类型" align="center" prop="qcResultType" width="90">
        <template #default="scope">
          <dict-tag v-if="mes_qc_result_type" :options="mes_qc_result_type" :value="scope.row.qcResultType" />
        </template>
      </el-table-column>
      <el-table-column label="检测工具" align="center" prop="qcTool" width="100" />
      <el-table-column label="值属性" align="center" prop="qcResultSpc" width="90" />
      <el-table-column label="启用" align="center" width="70">
        <template #default="scope">
          <span :style="{ color: scope.row.enableFlag === '1' ? '#67c23a' : '#909399' }">{{ scope.row.enableFlag === '1' ? '是' : '否' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:qc:index:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:qc:index:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="title" v-model="open" width="680px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="检测项编码" prop="indexCode">
              <el-input v-model="form.indexCode" placeholder="请输入检测项编码" :disabled="optType === 'edit'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="检测项名称" prop="indexName">
              <el-input v-model="form.indexName" placeholder="请输入检测项名称" />
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
            <el-form-item label="值类型" prop="qcResultType">
              <el-select v-model="form.qcResultType" placeholder="请选择值类型" style="width: 100%">
                <el-option v-for="d in mes_qc_result_type" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="检测工具" prop="qcTool">
              <el-input v-model="form.qcTool" placeholder="请输入检测工具" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="值属性" prop="qcResultSpc">
              <el-input v-model="form.qcResultSpc" placeholder="如 长度mm / 色差ΔE" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="form.qcResultType === 'DICT'">
          <el-col :span="12">
            <el-form-item label="关联字典" prop="dictType">
              <el-input v-model="form.dictType" placeholder="sys_dict_type 的 type，如 mes_qc_check_result" />
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

<script setup lang="ts" name="QcIndex">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import type { QcIndex } from '@/api/mes/qc/index'
import { listIndex, getIndex, addIndex, updateIndex, delIndex } from '@/api/mes/qc/index'

const { proxy } = getCurrentInstance() as any
const { mes_qc_type, mes_qc_result_type } = useDict('mes_qc_type', 'mes_qc_result_type')

const indexList = ref<QcIndex[]>([])
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
  form: {} as QcIndex,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    indexCode: undefined,
    indexName: undefined,
    indexType: undefined,
    qcResultType: undefined,
    enableFlag: undefined
  } as any,
  rules: {
    indexCode: [{ required: true, message: '检测项编码不能为空', trigger: 'blur' }],
    indexName: [{ required: true, message: '检测项名称不能为空', trigger: 'blur' }],
    indexType: [{ required: true, message: '检验类型不能为空', trigger: 'change' }],
    qcResultType: [{ required: true, message: '值类型不能为空', trigger: 'change' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

/** 查询检测项列表 */
function getList() {
  loading.value = true
  listIndex(queryParams.value).then((response: any) => {
    indexList.value = response.rows
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
    indexId: undefined, indexCode: undefined, indexName: undefined,
    indexType: undefined, qcTool: undefined, qcResultType: undefined,
    dictType: undefined, qcResultSpc: undefined, enableFlag: '1', remark: undefined
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

function handleSelectionChange(selection: QcIndex[]) {
  ids.value = selection.map(item => item.indexId!)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleAdd() {
  reset()
  optType.value = 'add'
  open.value = true
  title.value = '添加检测项'
}

function handleUpdate(row?: QcIndex) {
  reset()
  optType.value = 'edit'
  const indexId = row?.indexId || ids.value[0]
  getIndex(indexId).then((response: any) => {
    form.value = response.data
    open.value = true
    title.value = '修改检测项'
  })
}

function submitForm() {
  proxy.$refs['formRef'].validate((valid: boolean) => {
    if (valid) {
      if (form.value.indexId != undefined) {
        updateIndex(form.value).then(() => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addIndex(form.value).then(() => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

function handleDelete(row?: QcIndex) {
  const indexIds = row?.indexId || ids.value
  proxy.$modal.confirm('是否确认删除检测项编号为"' + indexIds + '"的数据项？').then(function () {
    return delIndex(indexIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handleExport() {
  proxy.download('mes/qc/index/export', { ...queryParams.value }, `qc_index_${new Date().getTime()}.xlsx`)
}

getList()
</script>

<style scoped>
:deep(.el-form-item__label) { padding-right: 16px !important; }
</style>
