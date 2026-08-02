<template>
  <div class="app-container">
    <!-- 搜索 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="分类名称" prop="itemTypeName">
        <el-input v-model="queryParams.itemTypeName" placeholder="请输入分类名称" clearable style="width:200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="是否启用" prop="enableFlag">
        <el-select v-model="queryParams.enableFlag" placeholder="选择" clearable style="width:200px">
          <el-option label="是" value="1" /><el-option label="否" value="0" />
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
        <el-button type="info" plain icon="Sort" @click="toggleExpandAll">展开/折叠</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 树形表格 -->
    <el-table
      v-if="refreshTable"
      v-loading="loading"
      :data="itemTypeList"
      row-key="itemTypeId"
      :default-expand-all="isExpandAll"
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
    >
      <el-table-column prop="itemTypeName" label="分类" width="260" />
      <el-table-column prop="orderNum" label="排序" width="100" align="center" />
      <el-table-column prop="itemOrProduct" label="物料/产品" width="120" align="center">
        <template #default="scope">
          <dict-tag :options="mes_item_type" :value="scope.row.itemOrProduct" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ scope.row.createTime }}</span>
        </template>
      </el-table-column>
      <el-table-column label="启用" align="center" width="70">
        <template #default="scope">
          <el-switch v-model="scope.row.enableFlag" active-value="1" inactive-value="0" @change="handleEnableChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="280" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:md:itemtype:edit']">修改</el-button>
          <el-button v-if="scope.row.parentTypeId" link type="primary" icon="Plus" @click="handleAdd(scope.row)" v-hasPermi="['mes:md:itemtype:add']">新增</el-button>
          <el-button link type="primary" icon="Setting" @click="handleAttrBind(scope.row)" v-hasPermi="['mes:md:itemtype:attr']">扩展属性</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item v-if="form.parentTypeId !== 0" label="父分类" prop="parentTypeId">
          <el-tree-select v-model="form.parentTypeId" :data="itemTypeTree" check-strictly
            :props="{ value: 'id', label: 'label', children: 'children' }"
            placeholder="请选择上级分类" disabled style="width:100%" />
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="分类编码" prop="itemTypeCode">
              <el-input v-model="form.itemTypeCode" placeholder="请输入分类编码" :disabled="optType === 'edit' || optType === 'view'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类名称" prop="itemTypeName">
              <el-input v-model="form.itemTypeName" placeholder="请输入分类名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="排序" prop="orderNum">
              <el-input-number v-model="form.orderNum" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物料/产品">
              <span style="color:#409eff;font-weight:500">{{ itemOrProductLabel }}</span>
              <span style="color:#999;font-size:12px;margin-left:8px">（继承自父分类）</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="启用状态">
              <el-radio-group v-model="form.enableFlag">
                <el-radio value="1">是</el-radio>
                <el-radio value="0">否</el-radio>
              </el-radio-group>
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

    <!-- 扩展属性配置对话框 -->
    <el-dialog :title="'扩展属性配置 - ' + (attrBindRow?.itemTypeName || '')" v-model="attrBindOpen" width="680px" append-to-body>
      <el-alert type="info" :closable="false" style="margin-bottom: 12px">
        为本分类勾选需要填写的扩展属性。父分类已绑定的属性子类会自动继承，通常只需在大类配置。
      </el-alert>
      <el-table :data="attrBindList" v-loading="attrBindLoading" border size="small">
        <el-table-column label="属性编码" prop="attrCode" width="160" />
        <el-table-column label="属性名称" prop="attrName" width="140" />
        <el-table-column label="类型" prop="attrType" width="90" align="center" />
        <el-table-column label="必填" width="70" align="center">
          <template #default="scope">
            <el-checkbox v-model="scope.row.checked" true-value="1" false-value="0" :disabled="scope.row.inheritFromParent" />
          </template>
        </el-table-column>
        <el-table-column label="启用" width="70" align="center">
          <template #default="scope">
            <el-checkbox v-model="scope.row.enableFlag" true-value="1" false-value="0" :disabled="scope.row.inheritFromParent" />
          </template>
        </el-table-column>
        <el-table-column label="来源" width="90" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.inheritFromParent" type="warning" size="small">继承</el-tag>
            <el-tag v-else type="success" size="small">本类</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="scope">
            <el-button v-if="!scope.row.inheritFromParent" link type="danger" size="small" @click="removeAttrFromBind(scope.$index)">移除</el-button>
            <span v-else style="color:#bbb;font-size:12px">—</span>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:12px">
        <span style="font-size:13px;color:#606266">添加已有属性：</span>
        <el-select v-model="addAttrId" placeholder="选择后自动加入列表（记得点保存）" size="small" style="width:300px" filterable @change="addAttrToBind">
          <el-option v-for="a in attrDefOptions" :key="a.attrId" :label="`${a.attrName}(${a.attrCode})`" :value="a.attrId" />
        </el-select>
        <el-button type="success" plain size="small" style="margin-left:16px" @click="toggleNewAttrForm">{{ newAttrOpen ? '收起新建' : '+ 新建属性' }}</el-button>
      </div>

      <!-- 新建属性内联表单 -->
      <el-card v-if="newAttrOpen" shadow="never" style="margin-top:12px">
        <template #header><span style="font-weight:600">新建属性（提交后自动绑定到当前分类）</span></template>
        <el-form :model="newAttrForm" label-width="90px" size="small">
          <el-row :gutter="12">
            <el-col :span="10">
              <el-form-item label="属性编码" required>
                <el-input v-model="newAttrForm.attrCode" placeholder="大写+下划线，如 PRESSURE" />
              </el-form-item>
            </el-col>
            <el-col :span="10">
              <el-form-item label="属性名称" required>
                <el-input v-model="newAttrForm.attrName" placeholder="如 抗压强度" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="12">
            <el-col :span="6">
              <el-form-item label="类型">
                <el-select v-model="newAttrForm.attrType" style="width:100%">
                  <el-option label="文本" value="TEXT" />
                  <el-option label="数字" value="NUMBER" />
                  <el-option label="下拉" value="SELECT" />
                  <el-option label="开关" value="BOOL" />
                  <el-option label="日期" value="DATE" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="单位"><el-input v-model="newAttrForm.attrUnit" placeholder="如 MPa" /></el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="必填"><el-switch v-model="newAttrForm.required" /></el-form-item>
            </el-col>
          </el-row>
          <el-form-item v-if="newAttrForm.attrType === 'SELECT'" label="下拉选项">
            <el-input v-model="newAttrForm.optionsInput" type="textarea" :rows="2" placeholder="逗号分隔，如 优,良,差" />
          </el-form-item>
          <div style="text-align:right">
            <el-button size="small" @click="toggleNewAttrForm">取消</el-button>
            <el-button type="primary" size="small" :loading="newAttrSubmitting" @click="submitNewAttr">创建并绑定</el-button>
          </div>
        </el-form>
      </el-card>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="saveAttrBindForm">保 存</el-button>
          <el-button @click="attrBindOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="Itemtype">
import { ref, reactive, toRefs, computed } from 'vue'
import { getCurrentInstance } from 'vue'
import type { MdItemType, ItemTypeQueryParams } from '@/types/api/mes/md/itemtype'
import type { TreeSelect } from '@/types/api/common'
import type { MdAttrDef, MdItemTypeAttr } from '@/types/api/mes/md/attr'
import { listItemtype, treeselect, listExcludeChild, getItemtype, delItemtype, addItemtype, updateItemtype } from '@/api/mes/md/itemtype'
import { listAttrDef, getAttrBind, saveAttrBind, getEffAttrSchema, createAttrAndBind } from '@/api/mes/md/attr'
import { handleTree } from '@/utils/ruoyi'

const { proxy } = getCurrentInstance() as any
const { sys_yes_no } = useDict('sys_yes_no')
const { mes_item_type } = useDict('mes_item_type')

const itemOrProductLabel = computed(() => {
  const found = mes_item_type.value?.find((d: any) => d.value === (form.value.itemOrProduct || ''))
  return found?.label || '—'
})

const loading = ref(true)
const showSearch = ref(true)
const itemTypeList = ref<MdItemType[]>([])
const itemTypeTree = ref<TreeSelect[]>([])
const open = ref(false)
const title = ref('')
const isExpandAll = ref(true)
const refreshTable = ref(true)
const optType = ref<string | undefined>(undefined)

const data = reactive({
  form: {} as MdItemType,
  queryParams: { itemTypeName: undefined, enableFlag: undefined } as ItemTypeQueryParams,
  rules: {
    parentTypeId: [{ required: true, message: '父分类不能为空', trigger: 'blur' }],
    itemTypeCode: [{ required: true, message: '分类编码不能为空', trigger: 'blur' }],
    itemTypeName: [{ required: true, message: '分类名称不能为空', trigger: 'blur' }],
    orderNum: [{ required: true, message: '排序不能为空', trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

/** 查询列表 — 转成树结构 */
function getList() {
  loading.value = true
  listItemtype(queryParams.value).then(response => {
    itemTypeList.value = handleTree(response.data || [], 'itemTypeId', 'parentTypeId')
    loading.value = false
  })
}

/** 展开/折叠 */
function toggleExpandAll() {
  refreshTable.value = false
  isExpandAll.value = !isExpandAll.value
  proxy.$nextTick(() => { refreshTable.value = true })
}

function cancel() { open.value = false; reset() }

function reset() {
  optType.value = undefined
  form.value = {
    itemTypeId: undefined, parentTypeId: undefined, itemTypeCode: '',
    itemTypeName: undefined, orderNum: 1, itemOrProduct: '', enableFlag: '1'
  }
  proxy.resetForm('formRef')
}
// enableFlag 默认值 '1' 已正确，MES 表统一使用 1/0

function handleQuery() { getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }

/** 新增 — 父行自动填入 parentTypeId，itemOrProduct 从父分类继承 */
function handleAdd(row?: MdItemType) {
  reset()
  optType.value = 'add'
  if (row) {
    form.value.parentTypeId = row.itemTypeId
    form.value.itemOrProduct = row.itemOrProduct
  }
  open.value = true
  title.value = '添加分类'
  treeselect().then(r => { itemTypeTree.value = r.data || [] })
}

/** 修改 — 排除自身及子孙作为可选的父分类 */
function handleUpdate(row: MdItemType) {
  reset()
  optType.value = 'edit'
  getItemtype(row.itemTypeId!).then(response => {
    form.value = response.data
    open.value = true
    title.value = '修改分类'
  })
  // 父分类选项排除自身及子孙（防止循环）
  listExcludeChild(row.itemTypeId!).then(response => {
    itemTypeTree.value = handleTree(response.data || [], 'itemTypeId', 'parentTypeId')
  })
}

function submitForm() {
  proxy.$refs['formRef'].validate((valid: boolean) => {
    if (valid) {
      if (form.value.itemTypeId != undefined) {
        updateItemtype(form.value).then(() => { proxy.$modal.msgSuccess('修改成功'); open.value = false; getList() })
      } else {
        addItemtype(form.value).then(() => { proxy.$modal.msgSuccess('新增成功'); open.value = false; getList() })
      }
    }
  })
}

function handleEnableChange(row: any) {
  const newVal = row.enableFlag
  const text = newVal === '1' ? '启用' : '停用'
  proxy.$modal.confirm(`确认要${text}"${row.itemTypeName}"吗？`).then(() => {
    updateItemtype({ itemTypeId: row.itemTypeId, enableFlag: newVal } as any).then(() => proxy.$modal.msgSuccess(`${text}成功`))
  }).catch(() => {
    row.enableFlag = newVal === '1' ? '0' : '1'
    getList()
  })
}

function handleDelete(row: MdItemType) {
  proxy.$modal.confirm('是否确认删除名称为"' + row.itemTypeName + '"的分类？').then(() => delItemtype(row.itemTypeId!)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}

// ==================== 扩展属性配置 ====================
const attrBindOpen = ref(false)
const attrBindRow = ref<MdItemType | null>(null)
const attrBindLoading = ref(false)
/** 绑定列表（合并本类绑定 + 继承属性，继承项标记 inheritFromParent 不可编辑） */
const attrBindList = ref<Array<MdItemTypeAttr & { checked?: string; inheritFromParent?: boolean }>>([])
const attrDefOptions = ref<MdAttrDef[]>([])
const addAttrId = ref<number | undefined>(undefined)
// 新建属性内联表单
const newAttrOpen = ref(false)
const newAttrSubmitting = ref(false)
const newAttrForm = reactive<{ attrCode: string; attrName: string; attrType: string; attrUnit: string; required: boolean; optionsInput: string }>({
  attrCode: '', attrName: '', attrType: 'TEXT', attrUnit: '', required: false, optionsInput: ''
})

function handleAttrBind(row: MdItemType) {
  attrBindRow.value = row
  attrBindOpen.value = true
  attrBindLoading.value = true
  addAttrId.value = undefined
  attrBindList.value = []
  // 加载属性字典（用于"添加属性"下拉）
  listAttrDef({ enableFlag: '1' }).then(res => {
    attrDefOptions.value = (res as any).rows || []
  })
  // 加载本类直接绑定 + 有效属性(含继承)，合并展示
  Promise.all([
    getAttrBind(row.itemTypeId!),
    getEffAttrSchema(row.itemTypeId!)
  ]).then(([bindRes, effRes]) => {
    const binds = bindRes.data || []
    const effs = effRes.data || []
    const bindCodes = new Set(binds.map(b => b.attrCode))
    const merged: Array<MdItemTypeAttr & { checked?: string; inheritFromParent?: boolean }> = []
    // 本类绑定项
    for (const b of binds) {
      merged.push({ ...b, checked: b.required || '0', enableFlag: b.enableFlag || '1', inheritFromParent: false })
    }
    // 继承项（在 effSchema 但不在本类 binds 中的）
    for (const e of effs) {
      if (!bindCodes.has(e.attrCode) && e.inheritDepth && e.inheritDepth > 0) {
        merged.push({ ...e, checked: e.required || '0', enableFlag: e.enableFlag || '1', inheritFromParent: true })
      }
    }
    attrBindList.value = merged
    attrBindLoading.value = false
  })
}

/** 从字典添加一个属性到本类绑定（立即落库，所见即所得） */
function addAttrToBind() {
  if (!addAttrId.value) return
  const def = attrDefOptions.value.find(a => a.attrId === addAttrId.value)
  if (!def) return
  if (attrBindList.value.some(b => b.attrId === def.attrId)) {
    proxy.$modal.msgWarning('该属性已在列表中')
    return
  }
  attrBindList.value.push({
    attrId: def.attrId!, attrCode: def.attrCode, attrName: def.attrName, attrType: def.attrType,
    attrUnit: def.attrUnit, optionsJson: def.optionsJson,
    checked: '0', enableFlag: '1', inheritFromParent: false
  })
  addAttrId.value = undefined
}

/** 从列表移除一个本类属性（继承项不可移除；保存时生效） */
function removeAttrFromBind(index: number) {
  attrBindList.value.splice(index, 1)
}

/** 将当前列表全量保存到后端（统一落库，避免加属性后忘点保存丢失） */
function persistBinds(msg = '保存成功') {
  if (!attrBindRow.value) return
  const binds = attrBindList.value
    .filter(b => !b.inheritFromParent && b.enableFlag === '1')
    .map(b => ({ attrId: b.attrId, required: b.checked || '0', sortOrder: b.sortOrder || 0, enableFlag: b.enableFlag || '1' }))
  return saveAttrBind({ typeId: attrBindRow.value.itemTypeId!, binds }).then(() => {
    proxy.$modal.msgSuccess(msg)
  })
}

function saveAttrBindForm() {
  persistBinds().then(() => { attrBindOpen.value = false })
}

/** 展开/收起新建属性内联表单 */
function toggleNewAttrForm() {
  newAttrOpen.value = !newAttrOpen.value
  if (newAttrOpen.value) {
    newAttrForm.attrCode = ''
    newAttrForm.attrName = ''
    newAttrForm.attrType = 'TEXT'
    newAttrForm.attrUnit = ''
    newAttrForm.required = false
    newAttrForm.optionsInput = ''
  }
}

/** 提交新建属性：调 createAttrAndBind（隐式字典，attr_code 存在则复用） */
function submitNewAttr() {
  if (!attrBindRow.value) return
  const code = newAttrForm.attrCode.trim().toUpperCase()
  const name = newAttrForm.attrName.trim()
  if (!code) { proxy.$modal.msgWarning('属性编码不能为空'); return }
  if (!/^[A-Z][A-Z0-9_]*$/.test(code)) { proxy.$modal.msgWarning('属性编码须大写字母开头，仅含大写字母/数字/下划线'); return }
  if (!name) { proxy.$modal.msgWarning('属性名称不能为空'); return }
  // 同名校验：提示已有同名属性，避免重复
  const dup = attrDefOptions.value.find(a => a.attrCode === code)
  if (dup) { proxy.$modal.msgWarning(`属性编码 ${code} 已存在（${dup.attrName}），将复用该字典记录`); }

  // SELECT 选项转 JSON 数组字符串
  let optionsJson: string | undefined
  if (newAttrForm.attrType === 'SELECT') {
    const opts = newAttrForm.optionsInput.split(/[,，]/).map(s => s.trim()).filter(Boolean)
    if (!opts.length) { proxy.$modal.msgWarning('下拉类型需填写选项'); return }
    optionsJson = JSON.stringify(opts)
  }

  newAttrSubmitting.value = true
  createAttrAndBind({
    typeId: attrBindRow.value.itemTypeId!,
    attrDef: { attrCode: code, attrName: name, attrType: newAttrForm.attrType, attrUnit: newAttrForm.attrUnit || undefined, optionsJson },
    required: newAttrForm.required
  }).then(r => {
    proxy.$modal.msgSuccess('属性已创建并绑定')
    // 追加到绑定列表
    attrBindList.value.push({
      ...(r.data as any), checked: (r.data as any)?.required || '0', enableFlag: '1', inheritFromParent: false
    })
    // 同步到字典下拉选项
    if (!attrDefOptions.value.some(a => a.attrCode === code)) {
      attrDefOptions.value.push({ attrId: (r.data as any)?.attrId, attrCode: code, attrName: name, attrType: newAttrForm.attrType, attrUnit: newAttrForm.attrUnit, optionsJson })
    }
    toggleNewAttrForm()
  }).finally(() => { newAttrSubmitting.value = false })
}

getList()
</script>

<style scoped>
:deep(.el-form-item__label) { padding-right: 16px !important; }
</style>
