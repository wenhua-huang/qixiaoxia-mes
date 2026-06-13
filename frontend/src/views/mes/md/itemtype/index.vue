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
          <span>{{ itemOrProductMap[scope.row.itemOrProduct] || scope.row.itemOrProduct }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="enableFlag" label="是否启用" width="100" align="center">
        <template #default="scope">
          <dict-tag :options="sys_yes_no" :value="scope.row.enableFlag" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ scope.row.createTime }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:md:itemtype:edit']">修改</el-button>
          <el-button v-if="scope.row.parentTypeId" link type="primary" icon="Plus" @click="handleAdd(scope.row)" v-hasPermi="['mes:md:itemtype:add']">新增</el-button>
          <el-button v-if="scope.row.parentTypeId != 0" link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:md:itemtype:remove']">删除</el-button>
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
              <el-input v-model="form.itemTypeCode" placeholder="请输入分类编码" />
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
  </div>
</template>

<script setup lang="ts" name="Itemtype">
import { ref, reactive, toRefs, computed } from 'vue'
import { getCurrentInstance } from 'vue'
import type { MdItemType, ItemTypeQueryParams } from '@/types/api/mes/md/itemtype'
import type { TreeSelect } from '@/types/api/common'
import { listItemtype, treeselect, listExcludeChild, getItemtype, delItemtype, addItemtype, updateItemtype } from '@/api/mes/md/itemtype'
import { handleTree } from '@/utils/ruoyi'

const { proxy } = getCurrentInstance() as any
const { sys_yes_no } = useDict('sys_yes_no')

const itemOrProductMap: Record<string, string> = { RAW: '原料', SEMI: '半成品', FINISHED: '成品', AUXILIARY: '辅料', PACK: '包材' }
const itemOrProductLabel = computed(() => itemOrProductMap[form.value.itemOrProduct || ''] || '—')

const loading = ref(true)
const showSearch = ref(true)
const itemTypeList = ref<MdItemType[]>([])
const itemTypeTree = ref<TreeSelect[]>([])
const open = ref(false)
const title = ref('')
const isExpandAll = ref(true)
const refreshTable = ref(true)

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

function handleDelete(row: MdItemType) {
  proxy.$modal.confirm('是否确认删除名称为"' + row.itemTypeName + '"的分类？').then(() => delItemtype(row.itemTypeId!)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}

getList()
</script>

<style scoped>
:deep(.el-form-item__label) { padding-right: 16px !important; }
</style>
