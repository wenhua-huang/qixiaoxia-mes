<template>
  <div class="app-container">
    <!-- 搜索区 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="模板编码" prop="templateCode">
        <el-input v-model="queryParams.templateCode" placeholder="请输入模板编码" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="模板名称" prop="templateName">
        <el-input v-model="queryParams.templateName" placeholder="请输入模板名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="检验种类" prop="qcTypes">
        <el-select v-model="queryParams.qcTypes" placeholder="检验种类" clearable style="width: 200px">
          <el-option v-for="d in mes_qc_type" :key="d.value" :label="d.label" :value="d.value" />
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
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:qc:template:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:qc:template:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:qc:template:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="templateList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="模板编码" align="center" prop="templateCode" width="150" />
      <el-table-column label="模板名称" align="center" prop="templateName" :show-overflow-tooltip="true" />
      <el-table-column label="适用检验种类" align="center" prop="qcTypes" width="180">
        <template #default="scope">
          <dict-tag v-if="mes_qc_type" v-for="t in splitTypes(scope.row.qcTypes)" :key="t" :options="mes_qc_type" :value="t" style="margin-right: 4px" />
        </template>
      </el-table-column>
      <el-table-column label="启用" align="center" width="70">
        <template #default="scope">
          <span :style="{ color: scope.row.enableFlag === '1' ? '#67c23a' : '#909399' }">{{ scope.row.enableFlag === '1' ? '是' : '否' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:qc:template:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:qc:template:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑弹窗：头表单 + 两个子表 Tab -->
    <el-dialog :title="title" v-model="open" width="1100px" append-to-body :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="模板编码" prop="templateCode">
              <el-input v-model="form.templateCode" placeholder="请输入模板编码" :disabled="optType === 'edit'" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="模板名称" prop="templateName">
              <el-input v-model="form.templateName" placeholder="请输入模板名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否启用" prop="enableFlag">
              <el-radio-group v-model="form.enableFlag">
                <el-radio value="1">是</el-radio>
                <el-radio value="0">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="16">
            <el-form-item label="检验种类" prop="qcTypesArr">
              <el-checkbox-group v-model="qcTypesArr">
                <el-checkbox v-for="d in mes_qc_type" :key="d.value" :value="d.value">{{ d.label }}</el-checkbox>
              </el-checkbox-group>
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

      <el-tabs model-value="index">
        <el-tab-pane label="检测项" name="index">
          <IndexRowsTab ref="indexRowsTabRef" :rows="indexRows" />
        </el-tab-pane>
        <el-tab-pane label="适用物料" name="product">
          <ProductRowsTab :rows="productRows" :show-process="showProcessColumn" />
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="QcTemplate">
import { ref, reactive, toRefs, computed, getCurrentInstance } from 'vue'
import type { QcTemplate, QcTemplateIndexRow, QcTemplateProductRow } from '@/api/mes/qc/template'
import { listTemplate, getTemplate, addTemplate, updateTemplate, delTemplate } from '@/api/mes/qc/template'
import IndexRowsTab from './components/IndexRowsTab.vue'
import ProductRowsTab from './components/ProductRowsTab.vue'

const { proxy } = getCurrentInstance() as any
const { mes_qc_type } = useDict('mes_qc_type')

const templateList = ref<QcTemplate[]>([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const multiple = ref(true)
const total = ref(0)
const title = ref('')
const optType = ref('')
const indexRowsTabRef = ref()

/** 头表 qcTypes 逗号串 ↔ 勾选数组 */
const qcTypesArr = ref<string[]>([])
const indexRows = ref<QcTemplateIndexRow[]>([])
const productRows = ref<QcTemplateProductRow[]>([])

/** IPQC 类型才显示工序绑定列 */
const showProcessColumn = computed(() => qcTypesArr.value.includes('IPQC'))

const data = reactive({
  form: {} as QcTemplate,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    templateCode: undefined,
    templateName: undefined,
    qcTypes: undefined,
    enableFlag: undefined
  } as any,
  rules: {
    templateCode: [{ required: true, message: '模板编码不能为空', trigger: 'blur' }],
    templateName: [{ required: true, message: '模板名称不能为空', trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

function splitTypes(qcTypes?: string): string[] {
  return (qcTypes || '').split(',').map(s => s.trim()).filter(Boolean)
}

/** 查询模板列表 */
function getList() {
  loading.value = true
  listTemplate(queryParams.value).then((response: any) => {
    templateList.value = response.rows
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
  form.value = { templateId: undefined, templateCode: undefined, templateName: undefined, qcTypes: undefined, enableFlag: '1', remark: undefined }
  qcTypesArr.value = []
  indexRows.value = []
  productRows.value = []
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

function handleSelectionChange(selection: QcTemplate[]) {
  ids.value = selection.map(item => item.templateId!)
  multiple.value = !selection.length
}

function handleAdd() {
  reset()
  optType.value = 'add'
  open.value = true
  title.value = '添加检验模板'
}

function handleUpdate(row: QcTemplate) {
  reset()
  optType.value = 'edit'
  getTemplate(row.templateId!).then((response: any) => {
    const d = response.data
    form.value = d
    qcTypesArr.value = splitTypes(d.qcTypes)
    // 回读行数组（后端 null=不动子表，前端提交时永远带数组，防误清空）
    indexRows.value = d.indexRows || []
    productRows.value = d.productRows || []
    open.value = true
    title.value = '修改检验模板'
  })
}

/** 整头提交：头 + indexRows + productRows 一起 put/post（数组永远传，防止误清空/漏更新子表） */
function submitForm() {
  proxy.$refs['formRef'].validate((valid: boolean) => {
    if (valid) {
      if (qcTypesArr.value.length === 0) {
        proxy.$modal.msgWarning('请至少勾选一个检验种类')
        return
      }
      const indexErr = indexRowsTabRef.value?.validate?.()
      if (indexErr) {
        proxy.$modal.msgWarning(indexErr)
        return
      }
      const payload: QcTemplate = { ...form.value, qcTypes: qcTypesArr.value.join(','), indexRows: indexRows.value, productRows: productRows.value }
      const fn = payload.templateId ? updateTemplate(payload) : addTemplate(payload)
      fn.then(() => {
        proxy.$modal.msgSuccess(payload.templateId ? '修改成功' : '新增成功')
        open.value = false
        getList()
      })
    }
  })
}

function handleDelete(row?: QcTemplate) {
  const templateIds = row?.templateId || ids.value
  proxy.$modal.confirm('是否确认删除模板编号为"' + templateIds + '"的数据项？').then(function () {
    return delTemplate(templateIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handleExport() {
  proxy.download('mes/qc/template/export', { ...queryParams.value }, `qc_template_${new Date().getTime()}.xlsx`)
}

getList()
</script>

<style scoped>
:deep(.el-form-item__label) { padding-right: 16px !important; }
</style>
