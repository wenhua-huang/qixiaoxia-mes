<template>
  <div class="app-container">
    <el-row :gutter="10">
      <!-- 左侧分类树 -->
      <el-col :span="4" :xs="24">
        <div class="head-container">
          <el-input v-model="filterText" placeholder="输入分类名称过滤" clearable style="margin-bottom: 10px" />
          <el-tree ref="treeRef" :data="treeData" :props="{ children: 'children', label: 'label' }"
            :filter-node-method="filterNode" node-key="id" highlight-current
            @node-click="handleNodeClick" default-expand-all />
        </div>
      </el-col>
      <!-- 右侧表格 -->
      <el-col :span="20" :xs="24">
        <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
          <el-form-item label="物料编码" prop="itemCode">
            <el-input v-model="queryParams.itemCode" placeholder="请输入物料编码" clearable style="width: 180px" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="物料名称" prop="itemName">
            <el-input v-model="queryParams.itemName" placeholder="请输入物料名称" clearable style="width: 180px" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="规格型号" prop="specification">
            <el-input v-model="queryParams.specification" placeholder="请输入规格" clearable style="width: 180px" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:md:item:add']">新增</el-button></el-col>
          <el-col :span="1.5"><el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:md:item:edit']">修改</el-button></el-col>
          <el-col :span="1.5"><el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:md:item:remove']">删除</el-button></el-col>
          <el-col :span="1.5"><el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:md:item:export']">导出</el-button></el-col>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="itemList" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column label="物料编码" align="center" prop="itemCode" width="130" />
          <el-table-column label="物料名称" align="center" prop="itemName" min-width="120" :show-overflow-tooltip="true" />
          <el-table-column label="规格型号" align="center" prop="specification" width="150" :show-overflow-tooltip="true" />
          <el-table-column label="分类" align="center" prop="itemTypeName" width="100" />
          <el-table-column label="主单位" align="center" prop="unitName" width="80" />
          <el-table-column label="是否启用" align="center" prop="enableFlag" width="80">
            <template #default="scope"><dict-tag :options="sys_yes_no" :value="scope.row.enableFlag" /></template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="110" class-name="small-padding fixed-width">
            <template #default="scope">
              <el-button link type="primary" size="small" @click="handleUpdate(scope.row)" v-hasPermi="['mes:md:item:edit']">修改</el-button>
              <el-button link type="primary" size="small" @click="handleDelete(scope.row)" v-hasPermi="['mes:md:item:remove']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
      </el-col>
    </el-row>

    <!-- 添加/修改物料对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-tabs v-model="activeTab">
        <!-- 基本信息 tab -->
        <el-tab-pane label="基本信息" name="basic">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" size="default">
            <el-row>
              <el-col :span="12">
                <el-form-item label="物料编码" prop="itemCode">
                  <el-input v-model="form.itemCode" placeholder="请输入物料编码" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="物料名称" prop="itemName">
                  <el-input v-model="form.itemName" placeholder="请输入物料名称" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item label="分类" prop="itemTypeId">
                  <el-tree-select v-model="form.itemTypeId" :data="itemTypeTree" check-strictly node-key="id"
                    :props="{ value: 'id', label: 'label', children: 'children' }"
                    placeholder="请选择分类" clearable style="width: 100%" @change="syncItemTypeName" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="规格型号">
                  <el-input v-model="form.specification" placeholder="请输入规格型号" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item label="主单位" prop="unitOfMeasure">
                  <el-select v-model="form.unitOfMeasure" placeholder="请选择主单位" style="width: 100%" @change="onUnitChange">
                    <el-option v-for="u in unitOptions" :key="u.unitCode" :label="u.unitName + ' (' + u.unitCode + ')'" :value="u.unitCode" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="辅助单位">
                  <el-select v-model="form.unit2" placeholder="请选择辅助单位" style="width: 100%" clearable @change="onUnit2Change">
                    <el-option v-for="u in unitOptions" :key="u.unitCode" :label="u.unitName + ' (' + u.unitCode + ')'" :value="u.unitCode" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row v-if="form.unit2">
              <el-col :span="12">
                <el-form-item label="换算率">
                  <el-input-number v-model="form.conversionRate" :precision="4" :step="1" :min="0" style="width: 100%" placeholder="1本=多少辅" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item label="产品尺寸">
                  <el-input v-model="form.productSize" placeholder="如254*127*330mm" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="装箱规格">
                  <el-input v-model="form.packageSpec" placeholder="如250个/箱" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="印刷要求" prop="printingReq">
              <el-input v-model="form.printingReq" type="textarea" placeholder="印刷要求描述" />
            </el-form-item>
            <el-row>
              <el-col :span="12"><el-form-item label="启用批次管理"><el-switch v-model="form.batchFlag" active-value="1" inactive-value="0" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="高价值物资"><el-switch v-model="form.highValue" active-value="1" inactive-value="0" /></el-form-item></el-col>
            </el-row>
            <el-row>
              <el-col :span="12"><el-form-item label="安全库存"><el-switch v-model="form.safeStockFlag" active-value="1" inactive-value="0" /></el-form-item></el-col>
              <el-col :span="12">
                <el-form-item label="是否启用">
                  <el-radio-group v-model="form.enableFlag">
                    <el-radio value="1">是</el-radio>
                    <el-radio value="0">否</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row v-if="form.safeStockFlag === '1'">
              <el-col :span="8"><el-form-item label="最低库存"><el-input-number v-model="form.minStock" :precision="4" style="width:100%" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="最高库存"><el-input-number v-model="form.maxStock" :precision="4" style="width:100%" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="预警库存"><el-input-number v-model="form.alertStock" :precision="4" style="width:100%" /></el-form-item></el-col>
            </el-row>
            <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 纸张属性 tab (条件显示) -->
        <el-tab-pane label="纸张属性" name="paper" v-if="showPaperTab">
          <el-form :model="form.attrPaper" label-width="100px">
            <el-row>
              <el-col :span="12"><el-form-item label="门幅"><el-input v-model="form.attrPaper!.paperWidth" placeholder="如925mm" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="克重"><el-input v-model="form.attrPaper!.paperWeight" placeholder="如120g" /></el-form-item></el-col>
            </el-row>
            <el-row>
              <el-col :span="12"><el-form-item label="来源/品牌"><el-input v-model="form.attrPaper!.paperSource" placeholder="如联盛A2" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="种类"><el-input v-model="form.attrPaper!.paperType" placeholder="如箱板纸" /></el-form-item></el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <!-- 纸袋属性 tab -->
        <el-tab-pane label="纸袋属性" name="paperBag" v-if="showPaperBagTab">
          <el-form :model="form.attrPaperBag" label-width="100px">
            <el-row>
              <el-col :span="12"><el-form-item label="绳料规格"><el-input v-model="form.attrPaperBag!.ropeSpec" placeholder="如7.5cm黄牛皮纸绳" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="口部提拔"><el-input v-model="form.attrPaperBag!.mouthType" placeholder="锯齿口/平口/翻口" /></el-form-item></el-col>
            </el-row>
            <el-form-item label="底板类型"><el-input v-model="form.attrPaperBag!.bottomType" placeholder="无/灰底白板/加强底板" /></el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- BOM组成 tab (编辑时显示) -->
        <el-tab-pane label="BOM组成" name="bom" v-if="form.itemId != null">
          <ItemBom ref="itemBomRef" :itemId="form.itemId" />
        </el-tab-pane>
        <!-- 批次配置 tab (编辑时显示) -->
        <el-tab-pane label="批次配置" name="batch" v-if="form.itemId != null">
          <BatchConfig ref="batchConfigRef" :itemId="form.itemId" />
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

<script setup lang="ts" name="Item">
import { ref, reactive, toRefs, computed, watch, nextTick } from 'vue'
import { getCurrentInstance } from 'vue'
import type { MdItem, ItemQueryParams, MdItemAttrPaper, MdItemAttrPaperBag } from '@/types/api/mes/md/item'
import type { TreeSelect } from '@/types/api/common'
import { listItem, getItem, delItem, addItem, updateItem } from '@/api/mes/md/item'
import { treeselect } from '@/api/mes/md/itemtype'
import { listAllUnitmeasure } from '@/api/mes/md/unitmeasure'
import BatchConfig from './components/BatchConfig.vue'
import ItemBom from './components/ItemBom.vue'

const { proxy } = getCurrentInstance() as any
const { sys_yes_no } = useDict('sys_yes_no')

const itemList = ref<MdItem[]>([])
const open = ref<boolean>(false)
const loading = ref<boolean>(true)
const showSearch = ref<boolean>(true)
const ids = ref<number[]>([])
const single = ref<boolean>(true)
const multiple = ref<boolean>(true)
const total = ref<number>(0)
const title = ref<string>('')
const activeTab = ref<string>('basic')
const treeData = ref<TreeSelect[]>([])
const itemTypeTree = ref<TreeSelect[]>([])
const unitOptions = ref<any[]>([])
const filterText = ref<string>('')
const treeRef = ref<any>(null)
const batchConfigRef = ref<any>(null)
const itemBomRef = ref<any>(null)

const data = reactive({
  form: {
    enableFlag: '1', batchFlag: '1', safeStockFlag: '0',
    highValue: '0', conversionRate: 1.0, parentId: 0
  } as MdItem,
  queryParams: { pageNum: 1, pageSize: 10 } as ItemQueryParams,
  rules: {
    itemCode: [{ required: true, message: '物料编码不能为空', trigger: 'blur' }],
    itemName: [{ required: true, message: '物料名称不能为空', trigger: 'blur' }],
    unitOfMeasure: [{ required: true, message: '主单位不能为空', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

const showPaperTab = computed(() => {
  const c = form.value.itemTypeCode?.toUpperCase() || ''
  return c.includes('PAPER') || c.includes('纸')
})
const showPaperBagTab = computed(() => {
  const c = form.value.itemTypeCode?.toUpperCase() || ''
  return c.includes('BAG') || c.includes('纸袋')
})

watch(filterText, (val) => { treeRef.value?.filter(val) })

function filterNode(value: string, data: any) {
  if (!value) return true
  return data.label?.indexOf(value) !== -1
}

function loadTree() {
  treeData.value = []
  itemTypeTree.value = []
  unitOptions.value = []
  return Promise.all([
    treeselect().then(r => { treeData.value = r.data || []; itemTypeTree.value = r.data || [] }),
    listAllUnitmeasure().then(r => { unitOptions.value = r.data || [] })
  ])
}

/** 补全分类名称（el-tree-select 只回填 id，需手动补 name） */
function syncItemTypeName() {
  if (form.value.itemTypeId) {
    const findNode = (nodes: any[], id: number): any => {
      for (const n of nodes) {
        if (n.id === id) return n
        if (n.children) { const f = findNode(n.children, id); if (f) return f }
      }
      return null
    }
    const node = findNode(itemTypeTree.value, form.value.itemTypeId)
    if (node) {
      form.value.itemTypeName = node.label
      // 同时根据分类名称推断 itemTypeCode（取节点 label 匹配？不，等后端 resolveItemTypeId 兜底）
    }
  }
}

function handleNodeClick(data: any) {
  queryParams.value.itemTypeId = data.id
  handleQuery()
}

function onUnitChange(code: string) {
  const found = unitOptions.value.find((u: any) => u.unitCode === code)
  if (found) form.value.unitName = found.unitName
}
function onUnit2Change(code: string) {
  const found = unitOptions.value.find((u: any) => u.unitCode === code)
  if (found) form.value.unit2Name = found.unitName
}

function getList() {
  loading.value = true
  listItem(queryParams.value).then(r => { itemList.value = r.rows; total.value = r.total; loading.value = false })
}

function cancel() { open.value = false; reset() }

function reset() {
  form.value = { enableFlag: '1', batchFlag: '1', safeStockFlag: '0', highValue: '0', conversionRate: 1.0, parentId: 0,
    attrPaper: {} as MdItemAttrPaper, attrPaperBag: {} as MdItemAttrPaperBag } as MdItem
  activeTab.value = 'basic'
  proxy?.resetForm('formRef')
}

function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(s: MdItem[]) { ids.value = s.map(i => i.itemId!); single.value = s.length !== 1; multiple.value = !s.length }

function handleAdd() {
  reset()
  loadTree().then(() => {
    open.value = true; title.value = '添加物料'
  })
}

function handleUpdate(row?: MdItem) {
  reset()
  const itemId = row?.itemId || ids.value[0]
  // 先加载下拉数据，再加载表单数据，确保 select/tree-select 能正确回显
  loadTree().then(() => {
    getItem(itemId).then(r => {
      form.value = { ...r.data }
      if (!form.value.attrPaper) form.value.attrPaper = {} as MdItemAttrPaper
      if (!form.value.attrPaperBag) form.value.attrPaperBag = {} as MdItemAttrPaperBag
      open.value = true; title.value = '修改物料'
      nextTick(() => { if (batchConfigRef.value) batchConfigRef.value.load(itemId); if (itemBomRef.value) itemBomRef.value.load() })
    })
  })
}

function submitForm() {
  proxy.$refs['formRef'].validate((valid: boolean) => {
    if (valid) {
      syncItemTypeName()
      if (form.value.itemId != undefined) {
        updateItem(form.value).then(() => { proxy.$modal.msgSuccess('修改成功'); open.value = false; getList() })
      } else {
        addItem(form.value).then(() => { proxy.$modal.msgSuccess('新增成功'); open.value = false; getList() })
      }
    }
  })
}

function handleDelete(row?: MdItem) {
  const itemIds = row?.itemId || ids.value
  proxy.$modal.confirm('是否确认删除物料编号为"' + itemIds + '"的数据项？').then(() => delItem(itemIds)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}

function handleExport() {
  proxy.download('mes/md/item/export', { ...queryParams.value }, `item_${new Date().getTime()}.xlsx`)
}

loadTree()
getList()
</script>
