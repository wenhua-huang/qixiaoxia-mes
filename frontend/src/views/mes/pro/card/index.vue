<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryFormRef" size="small" :inline="true" v-show="showSearch" label-width="90px">
      <el-row>
        <el-col :span="8">
          <el-form-item label="流转卡编码" prop="cardCode">
            <el-input v-model="queryParams.cardCode" placeholder="请输入流转卡编码" clearable @keyup.enter="handleQuery" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="工单名称" prop="workorderName">
            <el-input v-model="queryParams.workorderName" placeholder="请输入工单名称" clearable @keyup.enter="handleQuery" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="产品名称" prop="itemName">
            <el-input v-model="queryParams.itemName" placeholder="请输入产品名称" clearable @keyup.enter="handleQuery" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
              <el-option
                v-for="item in statusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item>
            <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['mes:pro:card:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:pro:card:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:pro:card:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" size="small" @click="handleExport" v-hasPermi="['mes:pro:card:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="流转卡编码" align="center" prop="cardCode" width="150">
        <template #default="scope">
          <el-button link type="primary" size="small" @click="handleView(scope.row)" v-hasPermi="['mes:pro:card:query']">{{ scope.row.cardCode }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="工单" align="center" prop="workorderName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="产品名称" align="center" prop="itemName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="批次号" align="center" prop="batchCode" width="120" />
      <el-table-column label="流转数量" align="center" prop="quantityTransfered" width="100" />
      <el-table-column label="当前工序" align="center" prop="currentProcessName" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <el-tag :type="statusTagMap[scope.row.status] || 'info'" size="small">{{ statusLabelMap[scope.row.status] || scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" size="small" icon="View" @click="handleView(scope.row)" v-hasPermi="['mes:pro:card:query']">查看</el-button>
          <el-button link type="primary" size="small" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:pro:card:edit']">修改</el-button>
          <el-button link type="primary" size="small" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:pro:card:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      v-model:current-page="queryParams.pageNum"
      v-model:page-size="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 新增/修改/查看对话框 -->
    <el-dialog :title="title" v-model="open" width="850px" append-to-body :close-on-click-modal="false" @close="cancel">
      <el-tabs v-model="activeTab">
        <!-- Tab1: 基本信息 -->
        <el-tab-pane label="基本信息" name="header">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
            <el-row>
              <el-col :span="16">
                <el-form-item label="流转卡编码" prop="cardCode">
                  <el-input v-model="form.cardCode" placeholder="请输入流转卡编码" :disabled="optType !== 'add'" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label-width="0" v-if="optType === 'add'">
                  <el-switch v-model="autoGenFlag" size="small" @change="handleAutoGenChange" />
                  <span>&nbsp;自动生成</span>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="16">
                <el-form-item label="生产工单" prop="workorderName">
                  <el-input v-model="form.workorderName" placeholder="请选择生产工单" readonly :disabled="optType === 'view'">
                    <template #append v-if="optType !== 'view'">
                      <el-button icon="Search" @click="handleWorkorderSelect" />
                    </template>
                  </el-input>
                  <workorderSelect ref="woSelectRef" @onSelected="onWorkorderSelected" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item label="批次号" prop="batchCode">
                  <el-input v-model="form.batchCode" placeholder="请输入批次号" :disabled="optType === 'view'">
                    <template #append v-if="optType === 'add'">
                      <el-button @click="handleGenBatchCode">生成</el-button>
                    </template>
                  </el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="流转数量" prop="quantityTransfered">
                  <el-input-number v-model="form.quantityTransfered" :min="1" :precision="2" style="width: 100%" :disabled="optType === 'view'" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item label="状态" prop="status">
                  <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
                    <el-option
                      v-for="item in statusOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="24">
                <el-form-item label="备注" prop="remark">
                  <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <!-- Tab2: 工序流转详情（仅查看/修改已有记录时显示） -->
        <el-tab-pane label="工序流转详情" name="process" v-if="form.cardId != null">
          <el-table v-loading="processLoading" :data="processList" size="small" border>
            <el-table-column label="序号" type="index" width="55" align="center" />
            <el-table-column label="工序名称" align="center" prop="processName" min-width="120" :show-overflow-tooltip="true" />
            <el-table-column label="工序编码" align="center" prop="processCode" width="120" />
            <el-table-column label="顺序号" align="center" prop="orderNum" width="80" />
            <el-table-column label="状态" align="center" prop="status" width="90">
              <template #default="scope">
                <el-tag :type="processStatusTagMap[scope.row.status] || 'info'" size="small">{{ processStatusLabelMap[scope.row.status] || scope.row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="开始时间" align="center" prop="startTime" width="160">
              <template #default="scope">
                <span>{{ parseTime(scope.row.startTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="结束时间" align="center" prop="endTime" width="160">
              <template #default="scope">
                <span>{{ parseTime(scope.row.endTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="加工数量" align="center" prop="quantityTransfered" width="100" />
            <el-table-column label="合格数" align="center" prop="quantityQualified" width="90" />
            <el-table-column label="不合格数" align="center" prop="quantityUnqualified" width="90" />
            <el-table-column label="备注" align="center" prop="remark" min-width="120" :show-overflow-tooltip="true" />
          </el-table>
          <div v-if="!processLoading && processList.length === 0" style="text-align: center; padding: 40px 0; color: #909399;">
            暂无工序流转记录
          </div>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm" v-if="optType !== 'view'">确 定</el-button>
          <el-button @click="cancel">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, getCurrentInstance } from 'vue'
import { listProcard, getProcard, addProcard, updateProcard, delProcard } from '@/api/mes/pro/procard'
import { listCardProcessByCardId } from '@/api/mes/pro/cardprocess'
import { genSerialCode } from '@/api/mes/sys/autocoderule'
import workorderSelect from '@/components/workorderSelect/single.vue'

const { proxy } = getCurrentInstance() as any

// 状态常量映射
const statusOptions = [
  { label: '流转中', value: 'ACTIVE' },
  { label: '已完工', value: 'COMPLETED' },
  { label: '已报废', value: 'SCRAPPED' },
]
const statusLabelMap: Record<string, string> = { ACTIVE: '流转中', COMPLETED: '已完工', SCRAPPED: '已报废' }
const statusTagMap: Record<string, string> = { ACTIVE: '', COMPLETED: 'success', SCRAPPED: 'danger' }

const processStatusLabelMap: Record<string, string> = { PENDING: '待加工', PROCESSING: '加工中', COMPLETED: '已完成', SKIP: '已跳过' }
const processStatusTagMap: Record<string, string> = { PENDING: 'info', PROCESSING: 'warning', COMPLETED: 'success', SKIP: '' }

// 响应式状态
const loading = ref(true)
const open = ref(false)
const showSearch = ref(true)
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')
const optType = ref('')
const autoGenFlag = ref(false)
const activeTab = ref('header')
const ids = ref<number[]>([])
const dataList = ref<any[]>([])
const processLoading = ref(false)
const processList = ref<any[]>([])

const queryFormRef = ref()
const formRef = ref()
const woSelectRef = ref()

const queryParams = reactive<any>({
  pageNum: 1,
  pageSize: 10,
  cardCode: null,
  workorderName: null,
  itemName: null,
  status: null,
})

const form = reactive<any>({
  cardId: null,
  cardCode: null,
  workorderId: null,
  taskId: null,
  batchCode: null,
  itemId: null,
  itemCode: null,
  itemName: null,
  unitOfMeasure: null,
  currentProcessId: null,
  currentProcessName: null,
  quantityTransfered: 1,
  status: 'ACTIVE',
  remark: null,
})

const rules = {
  cardCode: [
    { required: true, message: '流转卡编码不能为空', trigger: 'blur' },
  ],
  workorderId: [
    { required: true, message: '生产工单不能为空', trigger: 'blur' },
  ],
  itemId: [
    { required: true, message: '产品物料不能为空', trigger: 'blur' },
  ],
  quantityTransfered: [
    { required: true, message: '流转数量不能为空', trigger: 'blur' },
  ],
}

// ==================== 列表查询 ====================
function getList() {
  loading.value = true
  listProcard(queryParams).then((response: any) => {
    dataList.value = response.rows
    total.value = response.total
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryFormRef')
  handleQuery()
}

function handleSelectionChange(selection: any[]) {
  ids.value = selection.map((item) => item.cardId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

// ==================== 弹窗操作 ====================
function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.cardId = null
  form.cardCode = null
  form.workorderId = null
  form.taskId = null
  form.batchCode = null
  form.itemId = null
  form.itemCode = null
  form.itemName = null
  form.currentProcessId = null
  form.currentProcessName = null
  form.unitOfMeasure = null
  form.quantityTransfered = 1
  form.status = 'ACTIVE'
  form.remark = null
  autoGenFlag.value = false
  activeTab.value = 'header'
  processList.value = []
  proxy.resetForm('formRef')
}

function handleWorkorderSelect() { woSelectRef.value?.open() }
function onWorkorderSelected(row: any) {
  if (!row) return
  form.workorderId = row.workorderId
  form.workorderName = row.workorderName
  form.itemId = row.productId || form.itemId
  form.itemCode = row.productCode || form.itemCode
  form.itemName = row.productName || form.itemName
  form.unitOfMeasure = row.unitOfMeasure || form.unitOfMeasure
}
function handleGenBatchCode() {
  const now = new Date()
  const date = `${now.getFullYear()}${String(now.getMonth()+1).padStart(2,'0')}${String(now.getDate()).padStart(2,'0')}`
  const rand = String(Math.floor(Math.random()*900+100))
  form.batchCode = `B${date}${rand}`
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增流转卡'
  optType.value = 'add'
}

function handleView(row: any) {
  reset()
  const cardId = row.cardId || ids.value[0]
  getProcard(cardId).then((response: any) => {
    Object.assign(form, response.data)
    open.value = true
    title.value = '查看流转卡'
    optType.value = 'view'
    // 加载工序流转记录
    loadCardProcess(cardId)
  })
}

function handleUpdate(row?: any) {
  reset()
  const cardId = row ? row.cardId : ids.value[0]
  getProcard(cardId).then((response: any) => {
    Object.assign(form, response.data)
    open.value = true
    title.value = '修改流转卡'
    optType.value = 'edit'
    // 加载工序流转记录
    loadCardProcess(cardId)
  })
}

function loadCardProcess(cardId: number) {
  processLoading.value = true
  listCardProcessByCardId(cardId).then((response: any) => {
    processList.value = response.rows || response.data || []
    processLoading.value = false
  }).catch(() => {
    processLoading.value = false
  })
}

function submitForm() {
  formRef.value?.validate((valid: boolean) => {
    if (valid) {
      if (form.cardId != null) {
        updateProcard(form).then(() => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addProcard(form).then(() => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

// ==================== 删除 / 导出 ====================
function handleDelete(row?: any) {
  const cardIds = row ? row.cardId : ids.value.join(',')
  proxy.$modal.confirm('是否确认删除流转卡编号为 "' + cardIds + '" 的数据项？').then(() => {
    return delProcard(cardIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handleExport() {
  proxy.download('/mes/pro/procard/export', {
    ...queryParams,
  }, `procard_${new Date().getTime()}.xlsx`)
}

// ==================== 自动编码 ====================
function handleAutoGenChange(flag: boolean) {
  if (flag) {
    genSerialCode('PRO_CARD_CODE').then((response: any) => {
      form.cardCode = response.data
    })
  } else {
    form.cardCode = null
  }
}

// 初始化
getList()
</script>
