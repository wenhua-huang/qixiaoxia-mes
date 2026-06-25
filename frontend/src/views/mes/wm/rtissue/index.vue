<template>
  <div class="app-container">
    <!-- 搜索区域 -->
    <el-form :model="queryParams" ref="queryRef" size="small" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="退料单编码" prop="rtCode">
        <el-input v-model="queryParams.rtCode" placeholder="请输入退料单编码" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="工单名称" prop="workorderName">
        <el-input v-model="queryParams.workorderName" placeholder="请输入工单名称" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width:120px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['mes:wm:rtissue:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:wm:rtissue:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:wm:rtissue:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" size="small" @click="handleExport" v-hasPermi="['mes:wm:rtissue:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="dataList" size="small" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="45" align="center" />
      <el-table-column label="退料单编码" align="center" prop="rtCode" width="150">
        <template #default="scope">
          <el-button link type="primary" size="small" @click="handleView(scope.row)">{{ scope.row.rtCode }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="退料单名称" align="center" prop="rtName" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="关联工单" align="center" prop="workorderName" width="140" :show-overflow-tooltip="true" />
      <el-table-column label="原领料单" align="center" prop="issueCode" width="140" :show-overflow-tooltip="true" />
      <el-table-column label="退料日期" align="center" width="110">
        <template #default="scope">
          <span>{{ parseTime(scope.row.rtDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="退料总数" align="center" prop="quantityTotal" width="90" />
      <el-table-column label="单位" align="center" prop="unitName" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <el-tag :type="statusTag[scope.row.status] || 'info'" size="small">
            {{ statusMap[scope.row.status] || scope.row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" size="small" @click="handleView(scope.row)" v-hasPermi="['mes:wm:rtissue:query']">查看</el-button>
          <el-button link type="primary" icon="Edit" size="small" v-if="scope.row.status === 'DRAFT'" @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:rtissue:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" size="small" v-if="scope.row.status === 'DRAFT'" @click="handleDelete(scope.row)" v-hasPermi="['mes:wm:rtissue:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/修改/查看弹窗（含Tabs） -->
    <el-dialog :title="title" v-model="open" width="1000px" append-to-body :close-on-click-modal="false" @close="cancel">
      <el-tabs v-model="activeTab">
        <!-- Tab1: 基本信息 -->
        <el-tab-pane label="基本信息" name="header">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" :disabled="isView">
            <el-row>
              <el-col :span="16">
                <el-form-item label="退料单编码" prop="rtCode">
                  <el-input v-model="form.rtCode" placeholder="请输入退料单编码" maxlength="64" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label-width="70" v-if="isAdd">
                  <el-switch v-model="autoGenFlag" active-color="#13ce66" active-text="自动生成" @change="handleAutoGenChange" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item label="退料单名称" prop="rtName">
                  <el-input v-model="form.rtName" placeholder="请输入退料单名称" maxlength="128" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="生产工单" prop="workorderId">
                  <el-input-number v-model="form.workorderId" :min="1" style="width:100%" placeholder="请输入工单ID" controls-position="right" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item label="原领料单" prop="issueId">
                  <el-input-number v-model="form.issueId" :min="1" style="width:100%" placeholder="请输入原领料单ID" controls-position="right" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="仓库" prop="warehouseId">
                  <el-input-number v-model="form.warehouseId" :min="1" style="width:100%" placeholder="请输入仓库ID" controls-position="right" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item label="退料日期" prop="rtDate">
                  <el-date-picker v-model="form.rtDate" type="date" placeholder="请选择退料日期" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="退料总数" prop="quantityTotal">
                  <el-input-number v-model="form.quantityTotal" :min="0" :precision="2" style="width:100%" placeholder="退料总数量" controls-position="right" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="24">
                <el-form-item label="备注" prop="remark">
                  <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注信息" maxlength="500" show-word-limit />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <!-- Tab2: 退料行明细 -->
        <el-tab-pane label="退料行明细" name="lines" :disabled="!form.rtId">
          <template v-if="form.rtId">
            <!-- 行工具栏 -->
            <el-row :gutter="10" class="mb8" v-if="isEdit && form.status === 'DRAFT'">
              <el-col :span="1.5">
                <el-button type="primary" plain icon="Plus" size="small" @click="handleAddLine">新增退料行</el-button>
              </el-col>
            </el-row>
            <!-- 行表格 -->
            <el-table :data="lineList" size="small" v-loading="lineLoading">
              <el-table-column label="物料ID" align="center" prop="itemId" width="90" />
              <el-table-column label="物料编码" align="center" prop="itemCode" width="130" :show-overflow-tooltip="true" />
              <el-table-column label="物料名称" align="center" prop="itemName" min-width="140" :show-overflow-tooltip="true" />
              <el-table-column label="退料数量" align="center" prop="quantityRt" width="110">
                <template #default="scope">
                  <el-input-number v-if="isEdit && form.status === 'DRAFT'" v-model="scope.row.quantityRt" :min="0" :precision="2" size="small" controls-position="right" style="width:95px" @change="handleLineQuantityChange(scope.row)" />
                  <span v-else>{{ scope.row.quantityRt }}</span>
                </template>
              </el-table-column>
              <el-table-column label="已退数量" align="center" prop="quantityRted" width="100">
                <template #default="scope">
                  <span>{{ scope.row.quantityRted || 0 }}</span>
                </template>
              </el-table-column>
              <el-table-column label="批次号" align="center" prop="batchCode" width="130">
                <template #default="scope">
                  <el-input v-if="isEdit && form.status === 'DRAFT'" v-model="scope.row.batchCode" size="small" placeholder="批次号" style="width:110px" />
                  <span v-else>{{ scope.row.batchCode }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" align="center" width="110" v-if="isEdit && form.status === 'DRAFT'" class-name="small-padding fixed-width">
                <template #default="scope">
                  <el-button link type="primary" icon="Edit" size="small" @click="handleEditLine(scope.row)">编辑</el-button>
                  <el-button link type="primary" icon="Delete" size="small" @click="handleDelLine(scope.row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </template>
          <el-empty v-else description="请先保存单据头后再管理退料行明细" :image-size="80" />
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="submitLoading" @click="submitForm" v-if="!isView">保 存</el-button>
          <el-button @click="cancel">关 闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 退料行编辑弹窗 -->
    <el-dialog :title="lineEditTitle" v-model="lineEditOpen" width="500px" append-to-body :close-on-click-modal="false">
      <el-form ref="lineFormRef" :model="lineForm" :rules="lineRules" label-width="100px">
        <el-form-item label="物料ID" prop="itemId">
          <el-input-number v-model="lineForm.itemId" :min="1" style="width:100%" placeholder="请输入物料ID" controls-position="right" />
        </el-form-item>
        <el-form-item label="物料编码" prop="itemCode">
          <el-input v-model="lineForm.itemCode" placeholder="选择物料后自动回填" disabled />
        </el-form-item>
        <el-form-item label="物料名称" prop="itemName">
          <el-input v-model="lineForm.itemName" placeholder="选择物料后自动回填" disabled />
        </el-form-item>
        <el-form-item label="退料数量" prop="quantityRt">
          <el-input-number v-model="lineForm.quantityRt" :min="0" :precision="2" style="width:100%" placeholder="请输入退料数量" controls-position="right" />
        </el-form-item>
        <el-form-item label="批次号" prop="batchCode">
          <el-input v-model="lineForm.batchCode" placeholder="请输入批次号" maxlength="64" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="confirmLineEdit">确 定</el-button>
          <el-button @click="lineEditOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="WmRtIssue">
import { ref, reactive, toRefs, getCurrentInstance, computed, onMounted } from 'vue'
import { listRtIssue, getRtIssue, addRtIssue, updateRtIssue, delRtIssue } from '@/api/mes/wm/rtissue'
import { listRtIssueLineByRtId, addRtIssueLine, updateRtIssueLine, delRtIssueLine } from '@/api/mes/wm/rtissueline'
import { genSerialCode } from '@/api/mes/sys/autocoderule'

const { proxy } = getCurrentInstance() as any

// -------------------- 常量 --------------------
const statusMap: Record<string, string> = {
  DRAFT: '草稿',
  CONFIRMED: '已确认'
}
const statusTag: Record<string, string> = {
  DRAFT: 'warning',
  CONFIRMED: 'success'
}
const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已确认', value: 'CONFIRMED' }
]

// -------------------- 状态定义 --------------------
const loading = ref(true)
const submitLoading = ref(false)
const lineLoading = ref(false)
const open = ref(false)
const showSearch = ref(true)
const title = ref('')
const autoGenFlag = ref(false)
const activeTab = ref('header')
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const dataList = ref<any[]>([])
const lineList = ref<any[]>([])

// 弹窗模式判断
const isAdd = computed(() => !form.value.rtId)
const isEdit = computed(() => !!form.value.rtId && !isView.value)
const isView = ref(false)

// 退料行编辑弹窗状态
const lineEditOpen = ref(false)
const lineEditTitle = ref('')
const lineEditIndex = ref(-1)
const lineForm = reactive<any>({
  itemId: undefined,
  itemCode: '',
  itemName: '',
  quantityRt: 0,
  batchCode: ''
})
const lineRules = {
  itemId: [{ required: true, message: '物料ID不能为空', trigger: 'blur' }],
  quantityRt: [{ required: true, message: '退料数量不能为空', trigger: 'blur' }]
}

const data = reactive({
  form: {} as any,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    rtCode: undefined,
    workorderName: undefined,
    status: undefined
  } as any,
  rules: {
    rtCode: [{ required: true, message: '退料单编码不能为空', trigger: 'blur' }],
    rtName: [{ required: true, message: '退料单名称不能为空', trigger: 'blur' }],
    workorderId: [{ required: true, message: '生产工单不能为空', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

// -------------------- 生命周期 --------------------
onMounted(() => getList())

// -------------------- 列表查询 --------------------
function getList() {
  loading.value = true
  listRtIssue(queryParams.value)
    .then((r: any) => {
      dataList.value = r.rows
      total.value = r.total
    })
    .catch(() => { proxy.$modal.msgError('查询退料单列表失败') })
    .finally(() => { loading.value = false })
}

// -------------------- 搜索 & 重置 --------------------
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

// -------------------- 行选 --------------------
function handleSelectionChange(selection: any[]) {
  ids.value = selection.map(item => item.rtId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

// -------------------- 弹窗开关 --------------------
function cancel() {
  open.value = false
  reset()
}

function reset() {
  isView.value = false
  submitLoading.value = false
  autoGenFlag.value = false
  activeTab.value = 'header'
  lineList.value = []
  lineEditIndex.value = -1
  form.value = {} as any
  proxy.resetForm('formRef')
}

// -------------------- 自动生成编码 --------------------
function handleAutoGenChange(flag: boolean) {
  if (flag) {
    genSerialCode('RT_ISSUE_CODE').then((r: any) => {
      form.value.rtCode = r.data
    }).catch(() => { proxy.$modal.msgError('生成编码失败') })
  } else {
    form.value.rtCode = ''
  }
}

// -------------------- 新增 --------------------
function handleAdd() {
  reset()
  open.value = true
  title.value = '新增生产退料单'
}

// -------------------- 查看 --------------------
function handleView(row: any) {
  reset()
  getRtIssue(row.rtId)
    .then((r: any) => {
      form.value = r.data
      open.value = true
      title.value = '查看生产退料单'
      isView.value = true
      activeTab.value = 'header'
      loadLines(form.value.rtId)
    })
    .catch(() => { proxy.$modal.msgError('获取退料单详情失败') })
}

// -------------------- 修改 --------------------
function handleUpdate(row?: any) {
  reset()
  const id = row?.rtId || ids.value[0]
  getRtIssue(id)
    .then((r: any) => {
      form.value = r.data
      open.value = true
      title.value = '修改生产退料单'
      activeTab.value = 'header'
      loadLines(form.value.rtId)
    })
    .catch(() => { proxy.$modal.msgError('获取退料单详情失败') })
}

// -------------------- 删除 --------------------
function handleDelete(row?: any) {
  const _ids = row?.rtId ? String(row.rtId) : ids.value.join(',')
  proxy.$modal
    .confirm('是否确认删除该退料单？删除后关联的退料行也将一并删除。')
    .then(() => delRtIssue(_ids))
    .then(() => {
      getList()
      proxy.$modal.msgSuccess('删除成功')
    })
    .catch(() => {})
}

// -------------------- 导出 --------------------
function handleExport() {
  proxy.download('/mes/wm/rtissue/export', { ...queryParams.value }, `生产退料单_${new Date().getTime()}.xlsx`)
}

// -------------------- 退料行管理 --------------------
function loadLines(rtId: number) {
  if (!rtId) return
  lineLoading.value = true
  listRtIssueLineByRtId(rtId)
    .then((r: any) => {
      lineList.value = r.data || r.rows || []
    })
    .catch(() => { proxy.$modal.msgError('加载退料行明细失败') })
    .finally(() => { lineLoading.value = false })
}

function handleAddLine() {
  lineEditIndex.value = -1
  lineForm.itemId = undefined
  lineForm.itemCode = ''
  lineForm.itemName = ''
  lineForm.quantityRt = 0
  lineForm.batchCode = ''
  lineEditTitle.value = '新增退料行'
  lineEditOpen.value = true
}

function handleEditLine(row: any) {
  lineEditIndex.value = lineList.value.indexOf(row)
  lineForm.itemId = row.itemId
  lineForm.itemCode = row.itemCode || ''
  lineForm.itemName = row.itemName || ''
  lineForm.quantityRt = row.quantityRt || 0
  lineForm.batchCode = row.batchCode || ''
  lineEditTitle.value = '编辑退料行'
  lineEditOpen.value = true
}

function handleDelLine(row: any) {
  const idx = lineList.value.indexOf(row)
  if (idx < 0) return
  if (row.lineId) {
    // 已持久化的行，调用后端删除
    delRtIssueLine(row.lineId).then(() => {
      lineList.value.splice(idx, 1)
      proxy.$modal.msgSuccess('删除退料行成功')
    }).catch(() => { proxy.$modal.msgError('删除退料行失败') })
  } else {
    // 未持久化的行，仅从本地移除
    lineList.value.splice(idx, 1)
    proxy.$modal.msgSuccess('已移除')
  }
}

function handleLineQuantityChange(row: any) {
  // quantityRt 通过 v-model 已直接绑定，此处可用于联动逻辑（如需）
}

function confirmLineEdit() {
  proxy.$refs['lineFormRef'].validate((valid: boolean) => {
    if (!valid) return

    const lineData = {
      rtId: form.value.rtId,
      itemId: lineForm.itemId,
      itemCode: lineForm.itemCode,
      itemName: lineForm.itemName,
      quantityRt: lineForm.quantityRt,
      batchCode: lineForm.batchCode
    }

    if (lineEditIndex.value >= 0) {
      // 编辑已有行
      const existing = lineList.value[lineEditIndex.value]
      lineData.lineId = existing.lineId
      if (existing.lineId) {
        // 已持久化：调用更新API
        updateRtIssueLine(lineData).then(() => {
          lineList.value.splice(lineEditIndex.value, 1, { ...existing, ...lineData })
          proxy.$modal.msgSuccess('更新退料行成功')
          lineEditOpen.value = false
        }).catch(() => { proxy.$modal.msgError('更新退料行失败') })
      } else {
        // 本地行：直接替换
        lineList.value.splice(lineEditIndex.value, 1, { ...existing, ...lineData })
        lineEditOpen.value = false
      }
    } else {
      // 新增行
      if (form.value.rtId) {
        // 已有单据头ID，直接调用新增API
        addRtIssueLine(lineData).then((r: any) => {
          lineList.value.push(r.data || lineData)
          proxy.$modal.msgSuccess('新增退料行成功')
          lineEditOpen.value = false
        }).catch(() => { proxy.$modal.msgError('新增退料行失败') })
      } else {
        // 尚未保存单据头，暂存到本地
        lineList.value.push(lineData)
        lineEditOpen.value = false
      }
    }
  })
}

// -------------------- 提交保存 --------------------
function submitForm() {
  proxy.$refs['formRef'].validate((valid: boolean) => {
    if (!valid) return
    submitLoading.value = true

    const action = form.value.rtId ? updateRtIssue(form.value) : addRtIssue(form.value)
    action
      .then((r: any) => {
        // 新增时获取返回的 rtId
        if (!form.value.rtId && r.data?.rtId) {
          form.value.rtId = r.data.rtId
        }
        // 保存未持久化的本地行
        const unsavedLines = lineList.value.filter((l: any) => !l.lineId && l.itemId)
        if (unsavedLines.length > 0 && form.value.rtId) {
          const savePromises = unsavedLines.map((l: any) => {
            l.rtId = form.value.rtId
            return addRtIssueLine(l)
          })
          return Promise.all(savePromises).then(() => undefined)
        }
      })
      .then(() => {
        proxy.$modal.msgSuccess(form.value.rtId ? '修改成功' : '新增成功')
        open.value = false
        getList()
      })
      .catch(() => { proxy.$modal.msgError('保存失败') })
      .finally(() => { submitLoading.value = false })
  })
}
</script>

<style scoped>
:deep(.el-form-item__label) {
  padding-right: 16px !important;
}
:deep(.el-tabs__header) {
  margin-bottom: 16px;
}
</style>
