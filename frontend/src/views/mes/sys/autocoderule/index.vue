<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="规则编码" prop="ruleCode">
        <el-input v-model="queryParams.ruleCode" placeholder="请输入规则编码" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="规则名称" prop="ruleName">
        <el-input v-model="queryParams.ruleName" placeholder="请输入规则名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="是否启用" prop="enableFlag">
        <el-select v-model="queryParams.enableFlag" placeholder="是否启用" clearable style="width: 200px">
          <el-option label="启用" value="1" />
          <el-option label="停用" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:sys:autocoderule:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:sys:autocoderule:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:sys:autocoderule:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:sys:autocoderule:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="ruleList" @selection-change="handleSelectionChange" @row-click="onRowClick" highlight-current-row>
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="规则编码" align="center" prop="ruleCode" />
      <el-table-column label="规则名称" align="center" prop="ruleName" />
      <el-table-column label="最大长度" align="center" prop="maxLength" width="80" />
      <el-table-column label="是否补齐" align="center" prop="isPadded" width="80">
        <template #default="scope">
          <span>{{ scope.row.isPadded === '1' ? '是' : '否' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="是否启用" align="center" prop="enableFlag" width="80">
        <template #default="scope">
          <span>{{ scope.row.enableFlag === '1' ? '启用' : '停用' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="250" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click.stop="handleUpdate(scope.row)" v-hasPermi="['mes:sys:autocoderule:edit']">修改</el-button>
          <el-button link type="primary" icon="View" @click.stop="showGenDialog(scope.row)">生成编码</el-button>
          <el-button link type="primary" icon="Delete" @click.stop="handleDelete(scope.row)" v-hasPermi="['mes:sys:autocoderule:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 分段管理（内嵌表格） -->
    <el-card v-if="selectedRule" class="mt20" header="分段配置">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>【{{ selectedRule.ruleName }}】分段配置</span>
          <el-button type="primary" size="small" icon="Plus" @click="handleAddPart" v-hasPermi="['mes:sys:autocodepart:add']">新增分段</el-button>
        </div>
      </template>
      <el-table :data="partList" v-loading="partLoading">
        <el-table-column label="序号" align="center" prop="partIndex" width="60" />
        <el-table-column label="分段类型" align="center" prop="partType" width="100">
          <template #default="scope">
            <el-tag v-if="scope.row.partType === 'FIXCHAR'" type="info">固定字符</el-tag>
            <el-tag v-else-if="scope.row.partType === 'NOWDATE'" type="success">当前日期</el-tag>
            <el-tag v-else-if="scope.row.partType === 'SERIALNO'">流水号</el-tag>
            <el-tag v-else-if="scope.row.partType === 'INPUTCHAR'" type="warning">输入字符</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分段编码" align="center" prop="partCode" />
        <el-table-column label="分段名称" align="center" prop="partName" />
        <el-table-column label="分段长度" align="center" prop="partLength" width="80" />
        <el-table-column label="操作" align="center" width="150">
          <template #default="scope">
            <el-button link type="primary" icon="Edit" @click="handleUpdatePart(scope.row)">修改</el-button>
            <el-button link type="primary" icon="Delete" @click="handleDeletePart(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 规则对话框 -->
    <el-dialog :title="title" v-model="open" width="700px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="规则编码" prop="ruleCode">
          <el-input v-model="form.ruleCode" placeholder="请输入规则编码" />
        </el-form-item>
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="form.ruleName" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="规则描述" prop="ruleDesc">
          <el-input v-model="form.ruleDesc" placeholder="请输入规则描述" type="textarea" />
        </el-form-item>
        <el-form-item label="最大长度" prop="maxLength">
          <el-input-number v-model="form.maxLength" :min="1" :max="64" style="width: 200px" />
        </el-form-item>
        <el-form-item label="是否补齐" prop="isPadded">
          <el-radio-group v-model="form.isPadded">
            <el-radio value="1">是</el-radio>
            <el-radio value="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="form.isPadded === '1'">
          <el-form-item label="补齐字符" prop="paddedChar">
            <el-input v-model="form.paddedChar" placeholder="如 0" style="width: 200px" maxlength="20" />
          </el-form-item>
          <el-form-item label="补齐方式" prop="paddedMethod">
            <el-radio-group v-model="form.paddedMethod">
              <el-radio value="L">左补齐</el-radio>
              <el-radio value="R">右补齐</el-radio>
            </el-radio-group>
          </el-form-item>
        </template>
        <el-form-item label="是否启用" prop="enableFlag">
          <el-radio-group v-model="form.enableFlag">
            <el-radio value="1">启用</el-radio>
            <el-radio value="0">停用</el-radio>
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

    <!-- 分段对话框 -->
    <el-dialog :title="partTitle" v-model="partOpen" width="650px" append-to-body>
      <el-form ref="partFormRef" :model="partForm" :rules="partRules" label-width="100px">
        <el-form-item label="分段序号" prop="partIndex">
          <el-input-number v-model="partForm.partIndex" :min="1" :max="20" style="width: 200px" />
        </el-form-item>
        <el-form-item label="分段类型" prop="partType">
          <el-select v-model="partForm.partType" placeholder="请选择分段类型" style="width: 100%" @change="onPartTypeChange">
            <el-option label="固定字符 (FIXCHAR)" value="FIXCHAR" />
            <el-option label="当前日期 (NOWDATE)" value="NOWDATE" />
            <el-option label="流水号 (SERIALNO)" value="SERIALNO" />
            <el-option label="输入字符 (INPUTCHAR)" value="INPUTCHAR" />
          </el-select>
        </el-form-item>
        <el-form-item label="分段编码" prop="partCode">
          <el-input v-model="partForm.partCode" placeholder="请输入分段编码" />
        </el-form-item>
        <el-form-item label="分段名称" prop="partName">
          <el-input v-model="partForm.partName" placeholder="请输入分段名称" />
        </el-form-item>
        <el-form-item label="分段长度" prop="partLength">
          <el-input-number v-model="partForm.partLength" :min="1" :max="64" style="width: 200px" />
        </el-form-item>
        <!-- FIXCHAR 专属 -->
        <el-form-item v-if="partForm.partType === 'FIXCHAR'" label="固定字符" prop="fixCharacter">
          <el-input v-model="partForm.fixCharacter" placeholder="如 DD、WO 等" />
        </el-form-item>
        <!-- NOWDATE 专属 -->
        <el-form-item v-if="partForm.partType === 'NOWDATE'" label="日期格式" prop="dateFormat">
          <el-input v-model="partForm.dateFormat" placeholder="如 yyyyMMdd" />
        </el-form-item>
        <!-- SERIALNO 专属 -->
        <template v-if="partForm.partType === 'SERIALNO'">
          <el-form-item label="起始值">
            <el-input-number v-model="partForm.seriaStartNo" :min="0" style="width: 200px" />
          </el-form-item>
          <el-form-item label="步长">
            <el-input-number v-model="partForm.seriaStep" :min="1" style="width: 200px" />
          </el-form-item>
          <el-form-item label="是否循环">
            <el-radio-group v-model="partForm.cycleFlag">
              <el-radio value="1">是</el-radio>
              <el-radio value="0">否</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="partForm.cycleFlag === '1'" label="循环方式">
            <el-select v-model="partForm.cycleMethod" placeholder="请选择循环方式" style="width: 100%">
              <el-option label="按年" value="YEAR" />
              <el-option label="按月" value="MONTH" />
              <el-option label="按日" value="DAY" />
              <el-option label="按小时" value="HOUR" />
              <el-option label="按分钟" value="MINUTE" />
              <el-option label="按传入字符" value="OTHER" />
            </el-select>
          </el-form-item>
        </template>
        <!-- INPUTCHAR 专属 -->
        <el-form-item v-if="partForm.partType === 'INPUTCHAR'" label="默认输入字符" prop="inputCharacter">
          <el-input v-model="partForm.inputCharacter" placeholder="默认的输入字符" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitPartForm">确 定</el-button>
          <el-button @click="cancelPart">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 生成编码测试对话框 -->
    <el-dialog title="生成编码测试" v-model="genOpen" width="500px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="规则编码">
          <el-input :model-value="selectedRule?.ruleCode" disabled />
        </el-form-item>
        <el-form-item label="输入字符" v-if="hasInputChar">
          <el-input v-model="genInputChar" placeholder="请输入编码中的输入字符" />
        </el-form-item>
        <el-form-item label="生成结果">
          <el-input :model-value="genResult" readonly style="font-weight: bold; font-size: 16px;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="doGenCode">生成编码</el-button>
          <el-button @click="genOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="AutoCodeRule">
import { ref, reactive, toRefs, computed } from 'vue'
import { getCurrentInstance } from 'vue'
import type { SysAutoCodeRule, AutoCodeRuleQueryParams } from '@/types/api/mes/sys/autocoderule'
import type { SysAutoCodePart } from '@/types/api/mes/sys/autocodepart'
import { listAutoCodeRule, getAutoCodeRule, addAutoCodeRule, updateAutoCodeRule, delAutoCodeRule, genSerialCode } from '@/api/mes/sys/autocoderule'
import { listAutoCodePartByRuleId, addAutoCodePart, updateAutoCodePart, delAutoCodePart } from '@/api/mes/sys/autocodepart'

const { proxy } = getCurrentInstance() as any

const ruleList = ref<SysAutoCodeRule[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>('')

// 分段相关
const selectedRule = ref<SysAutoCodeRule | null>(null)
const partList = ref<SysAutoCodePart[]>([])
const partLoading = ref<boolean>(false)
const partOpen = ref<boolean>(false)
const partTitle = ref<string>('')

// 生成编码
const genOpen = ref<boolean>(false)
const genInputChar = ref<string>('')
const genResult = ref<string>('')

const hasInputChar = computed(() => {
  return partList.value.some(p => p.partType === 'INPUTCHAR' || (p.partType === 'SERIALNO' && p.cycleFlag === '1' && p.cycleMethod === 'OTHER'))
})

const data = reactive({
  form: {} as SysAutoCodeRule,
  queryParams: {
    pageNum: 1, pageSize: 10,
    ruleCode: undefined, ruleName: undefined, enableFlag: undefined
  } as AutoCodeRuleQueryParams,
  rules: {
    ruleCode: [{ required: true, message: '规则编码不能为空', trigger: 'blur' }],
    ruleName: [{ required: true, message: '规则名称不能为空', trigger: 'blur' }],
    enableFlag: [{ required: true, message: '是否启用不能为空', trigger: 'blur' }]
  },
  partForm: { partIndex: 1, partType: 'FIXCHAR', partLength: 4, cycleFlag: '0' } as SysAutoCodePart,
  partRules: {
    partType: [{ required: true, message: '分段类型不能为空', trigger: 'change' }],
    partLength: [{ required: true, message: '分段长度不能为空', trigger: 'blur' }]
  }
})

const { queryParams, form, rules, partForm, partRules } = toRefs(data)

function getList() {
  loading.value = true
  listAutoCodeRule(queryParams.value).then(response => {
    ruleList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function cancel() { open.value = false; reset() }
function reset() {
  form.value = { ruleId: undefined, ruleCode: undefined, ruleName: undefined, ruleDesc: undefined, maxLength: undefined, isPadded: '0', paddedChar: '0', paddedMethod: 'L', enableFlag: '1', remark: undefined }
  proxy.resetForm('formRef')
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(selection: SysAutoCodeRule[]) {
  ids.value = selection.map(item => item.ruleId!)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function onRowClick(row: SysAutoCodeRule) { selectedRule.value = row; loadParts(row.ruleId!) }
function loadParts(ruleId: number) {
  partLoading.value = true
  listAutoCodePartByRuleId(ruleId).then(response => { partList.value = response.data || []; partLoading.value = false })
}

function handleAdd() { reset(); open.value = true; title.value = '添加编码规则' }
function handleUpdate(row?: SysAutoCodeRule) {
  reset()
  const ruleId = row?.ruleId || ids.value[0]
  getAutoCodeRule(ruleId).then(response => { form.value = response.data; open.value = true; title.value = '修改编码规则' })
}
function submitForm() {
  proxy.$refs['formRef'].validate((valid: boolean) => {
    if (valid) {
      if (form.value.ruleId != undefined) {
        updateAutoCodeRule(form.value).then(() => { proxy.$modal.msgSuccess('修改成功'); open.value = false; getList() })
      } else {
        addAutoCodeRule(form.value).then(() => { proxy.$modal.msgSuccess('新增成功'); open.value = false; getList() })
      }
    }
  })
}
function handleDelete(row?: SysAutoCodeRule) {
  const ruleIds = row?.ruleId || ids.value
  proxy.$modal.confirm('是否确认删除该编码规则？').then(function () { return delAutoCodeRule(ruleIds) }).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}
function handleExport() { proxy.download('mes/sys/autocoderule/export', { ...queryParams.value }, `autocoderule_${new Date().getTime()}.xlsx`) }

// 分段操作
function cancelPart() { partOpen.value = false; resetPart() }
function resetPart() {
  partForm.value = { partId: undefined, ruleId: selectedRule.value?.ruleId, partIndex: partList.value.length + 1, partType: 'FIXCHAR', partLength: 4, cycleFlag: '0' } as SysAutoCodePart
  proxy.resetForm('partFormRef')
}
function onPartTypeChange() { /* reset type-specific fields */ }
function handleAddPart() { resetPart(); partOpen.value = true; partTitle.value = '添加分段' }
function handleUpdatePart(row: SysAutoCodePart) {
  resetPart()
  partForm.value = { ...row }
  partOpen.value = true; partTitle.value = '修改分段'
}
function submitPartForm() {
  proxy.$refs['partFormRef'].validate((valid: boolean) => {
    if (valid) {
      partForm.value.ruleId = selectedRule.value?.ruleId
      if (partForm.value.partId != undefined) {
        updateAutoCodePart(partForm.value).then(() => { proxy.$modal.msgSuccess('修改成功'); partOpen.value = false; loadParts(selectedRule.value!.ruleId!) })
      } else {
        addAutoCodePart(partForm.value).then(() => { proxy.$modal.msgSuccess('新增成功'); partOpen.value = false; loadParts(selectedRule.value!.ruleId!) })
      }
    }
  })
}
function handleDeletePart(row: SysAutoCodePart) {
  proxy.$modal.confirm('是否确认删除该分段？').then(function () { return delAutoCodePart(row.partId!) }).then(() => { loadParts(selectedRule.value!.ruleId!); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}

// 生成编码
function showGenDialog(row: SysAutoCodeRule) {
  selectedRule.value = row
  loadParts(row.ruleId!)
  genInputChar.value = ''
  genResult.value = ''
  genOpen.value = true
}
function doGenCode() {
  genSerialCode(selectedRule.value!.ruleCode!, genInputChar.value || undefined).then(response => {
    genResult.value = response.data || response.msg
  }).catch(() => { proxy.$modal.msgError('编码生成失败') })
}

getList()
</script>
