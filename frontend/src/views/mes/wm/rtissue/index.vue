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
      <el-form-item label="原领料单" prop="issueCode">
        <el-input v-model="queryParams.issueCode" placeholder="请输入原领料单编码" clearable style="width:180px" @keyup.enter="handleQuery" />
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
        <el-button type="primary" plain icon="Link" size="small" @click="handleFromIssue" v-hasPermi="['mes:wm:rtissue:add']">从领料单生成</el-button>
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
      <el-table-column label="退料单编码" align="center" prop="rtCode" width="160">
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
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <el-tag :type="statusTag[scope.row.status] || 'info'" size="small">
            {{ statusMap[scope.row.status] || scope.row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="230" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" size="small" @click="handleView(scope.row)" v-hasPermi="['mes:wm:rtissue:query']">查看</el-button>
          <el-button link type="primary" icon="Edit" size="small" v-if="scope.row.status === 'DRAFT'" @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:rtissue:edit']">修改</el-button>
          <el-button link type="success" icon="Upload" size="small" v-if="scope.row.status === 'DRAFT'" @click="handleExecute(scope.row)" v-hasPermi="['mes:wm:rtissue:edit']">执行退库</el-button>
          <el-button link type="danger" icon="Delete" size="small" v-if="scope.row.status === 'DRAFT'" @click="handleDelete(scope.row)" v-hasPermi="['mes:wm:rtissue:remove']">删除</el-button>
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
                  <el-input v-model="form.rtCode" :placeholder="isAdd ? '保存时自动生成' : '请输入退料单编码'" disabled maxlength="64" />
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
                  <el-input v-model="form.workorderCode" disabled placeholder="来源于领料单" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item label="原领料单" prop="issueId">
                  <el-input v-model="form.issueCode" disabled placeholder="来源于领料单" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="仓库" prop="warehouseId">
                  <el-input v-model="form.warehouseName" disabled placeholder="来源于领料单" />
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
                  <el-input v-model="form.quantityTotal" disabled placeholder="按明细自动汇总" />
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
        <el-tab-pane label="退料行明细" name="lines">
          <!-- 行工具栏：草稿态可重新从领料单拉取 -->
          <el-row :gutter="10" class="mb8" v-if="!isView && form.status === 'DRAFT' && form.issueId">
            <el-col :span="1.5">
              <el-button type="success" plain icon="MagicStick" size="small" @click="handleReloadDraft">重新从领料单拉取</el-button>
            </el-col>
          </el-row>
          <!-- 行表格 -->
          <el-table :data="lineList" size="small" v-loading="lineLoading">
            <el-table-column label="物料编码" align="center" prop="itemCode" width="130" :show-overflow-tooltip="true" />
            <el-table-column label="物料名称" align="center" prop="itemName" min-width="140" :show-overflow-tooltip="true" />
            <el-table-column label="规格" align="center" prop="itemSpc" width="120" :show-overflow-tooltip="true" />
            <el-table-column label="退料数量" align="center" prop="quantityRt" width="120">
              <template #default="scope">
                <el-input-number v-if="!isView && form.status === 'DRAFT'" v-model="scope.row.quantityRt" :min="0" :precision="2" size="small" controls-position="right" style="width:105px" />
                <span v-else>{{ scope.row.quantityRt }}</span>
              </template>
            </el-table-column>
            <el-table-column label="已退数量" align="center" prop="quantityRted" width="100">
              <template #default="scope">
                <span>{{ scope.row.quantityRted || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="单位" align="center" prop="unitName" width="70" />
            <el-table-column label="批次号" align="center" prop="batchCode" width="130">
              <template #default="scope">
                <el-input v-if="!isView && form.status === 'DRAFT'" v-model="scope.row.batchCode" size="small" placeholder="批次号" style="width:110px" />
                <span v-else>{{ scope.row.batchCode }}</span>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!lineList.length" description="暂无退料明细" :image-size="80" />
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="submitLoading" @click="submitForm" v-if="!isView">保 存</el-button>
          <el-button @click="cancel">关 闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 领料单选择弹窗（从领料单生成退料单入口） -->
    <el-dialog title="选择领料单（仅已发料可退料）" v-model="issueSelectOpen" width="900px" append-to-body :close-on-click-modal="false">
      <el-form :inline="true" size="small" :model="issueQueryParams">
        <el-form-item label="领料单编码">
          <el-input v-model="issueQueryParams.issueCode" placeholder="请输入" clearable style="width:160px" @keyup.enter="loadIssueList" />
        </el-form-item>
        <el-form-item label="工单名称">
          <el-input v-model="issueQueryParams.workorderName" placeholder="请输入" clearable style="width:160px" @keyup.enter="loadIssueList" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" size="small" @click="loadIssueList">搜索</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="issueLoading" :data="issueList" size="small" highlight-current-row :row-class-name="issueRowClass" @row-dblclick="onIssueRowDblClick">
        <el-table-column label="领料单编码" align="center" prop="issueCode" width="160" />
        <el-table-column label="工单" align="center" prop="workorderName" min-width="140" :show-overflow-tooltip="true" />
        <el-table-column label="状态" align="center" prop="status" width="90">
          <template #default="scope">
            <el-tag size="small">{{ issueStatusMap[scope.row.status] || scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请/已发" align="center" width="110">
          <template #default="scope">
            <span>{{ scope.row.quantityTotal || 0 }} / {{ scope.row.quantityIssuedTotal || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="可退量" align="center" width="100">
          <template #default="scope">
            <el-tag v-if="(scope.row.returnableQty || 0) > 0" type="success" size="small">{{ scope.row.returnableQty }}</el-tag>
            <el-tag v-else type="danger" size="small">无可退</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="领料日期" align="center" width="110">
          <template #default="scope">
            <span>{{ parseTime(scope.row.issueDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="90" fixed="right">
          <template #default="scope">
            <el-button link type="primary" size="small" :disabled="(scope.row.returnableQty || 0) <= 0" @click="onIssueSelected(scope.row)">选择</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="issueTotal>0" :total="issueTotal" v-model:page="issueQueryParams.pageNum" v-model:limit="issueQueryParams.pageSize" @pagination="loadIssueList" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="WmRTIssue">
import { ref, reactive, toRefs, getCurrentInstance, computed, onMounted } from 'vue'
import { listRtIssue, getRtIssue, addRtIssue, updateRtIssue, delRtIssue, buildFromIssue, executeReturn, returnablePreview } from '@/api/mes/wm/rtissue'
import { listRtIssueLineByRtId } from '@/api/mes/wm/rtissueline'

const { proxy } = getCurrentInstance() as any

// -------------------- 常量 --------------------
// 状态机：DRAFT → POSTED（与后端 doExecuteReturn 一致）
const statusMap: Record<string, string> = {
  DRAFT: '草稿',
  POSTED: '已退库'
}
const statusTag: Record<string, string> = {
  DRAFT: 'warning',
  POSTED: 'success'
}
const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已退库', value: 'POSTED' }
]
// 领料单状态映射（选择弹窗展示用）
const issueStatusMap: Record<string, string> = {
  DRAFT: '草稿', PENDING: '待审核', APPROVED: '已审核', ALLOCATED: '已预占',
  ISSUED: '已发料', PARTIAL_ISSUED: '部分发料', CLOSED: '已关闭', CANCELED: '已作废'
}

// -------------------- 状态定义 --------------------
const loading = ref(true)
const submitLoading = ref(false)
const lineLoading = ref(false)
const open = ref(false)
const showSearch = ref(true)
const title = ref('')
const activeTab = ref('header')
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const dataList = ref<any[]>([])
const lineList = ref<any[]>([])

// 弹窗模式判断（草稿态=新增，有 rtId 且非查看=编辑）
const isView = ref(false)
const isAdd = computed(() => !form.value.rtId)
const isEdit = computed(() => !!form.value.rtId && !isView.value)

// 领料单选择弹窗状态
const issueSelectOpen = ref(false)
const issueLoading = ref(false)
const issueList = ref<any[]>([])
const issueTotal = ref(0)
const issueQueryParams = reactive<any>({
  pageNum: 1, pageSize: 10, issueCode: undefined, workorderName: undefined, status: 'ISSUED'
})

const data = reactive({
  form: {} as any,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    rtCode: undefined,
    workorderName: undefined,
    issueCode: undefined,
    status: undefined
  } as any,
  rules: {
    rtName: [{ required: true, message: '退料单名称不能为空', trigger: 'blur' }]
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
  activeTab.value = 'header'
  lineList.value = []
  form.value = {} as any
  proxy.resetForm('formRef')
}

// -------------------- 从领料单生成（主入口）--------------------
function handleFromIssue() {
  issueQueryParams.pageNum = 1
  issueSelectOpen.value = true
  loadIssueList()
}

// 可退量=0 的行置灰
function issueRowClass({ row }: { row: any }) {
  return (row.returnableQty || 0) <= 0 ? 'disabled-row' : ''
}

function loadIssueList() {
  issueLoading.value = true
  returnablePreview(issueQueryParams)
    .then((r: any) => {
      issueList.value = r.rows
      issueTotal.value = r.total
    })
    .catch(() => { proxy.$modal.msgError('加载领料单列表失败') })
    .finally(() => { issueLoading.value = false })
}

async function onIssueSelected(row: any) {
  if (!row || !row.issueId) return
  try {
    const r: any = await buildFromIssue(row.issueId)
    issueSelectOpen.value = false
    openAddDraft(r.data)
  } catch (e) { /* 错误已由 request 拦截器提示 */ }
}

// 双击领料单行直接选中
function onIssueRowDblClick(row: any) {
  onIssueSelected(row)
}

// 用草稿打开编辑弹窗（领料单驱动，关联字段锁定只读，可调退料量/批次/日期/备注）
function openAddDraft(draft: any) {
  reset()
  const { lines, ...header } = draft
  Object.assign(form.value, header)
  form.value.rtCode = '' // 草稿态无编码，保存时后端自动生成
  lineList.value = (lines || []).map((l: any) => ({ ...l }))
  activeTab.value = 'header'
  title.value = '从领料单生成退料单'
  open.value = true
}

// 草稿态重新从领料单拉取明细（覆盖当前明细）
function handleReloadDraft() {
  if (!form.value.issueId) { proxy.$modal.msgError('领料单缺失，无法拉取'); return }
  buildFromIssue(form.value.issueId).then((r: any) => {
    const draft = r.data
    if (draft && draft.lines) {
      lineList.value = draft.lines.map((l: any) => ({ ...l }))
      proxy.$modal.msgSuccess('已从领料单重新拉取明细')
    }
  })
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

// -------------------- 执行退库（DRAFT→POSTED，加库存）--------------------
function handleExecute(row: any) {
  proxy.$modal.confirm(`确认对退料单【${row.rtCode}】执行退库？将增加库存且不可撤销。`)
    .then(() => executeReturn(row.rtId))
    .then(() => { proxy.$modal.msgSuccess('退库成功'); getList() })
    .catch(() => {})
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

// -------------------- 退料行加载（查看/修改已有单时用）--------------------
function loadLines(rtId: number) {
  if (!rtId) return
  lineLoading.value = true
  listRtIssueLineByRtId(rtId)
    .then((r: any) => { lineList.value = r.data || r.rows || [] })
    .catch(() => { proxy.$modal.msgError('加载退料行明细失败') })
    .finally(() => { lineLoading.value = false })
}

// -------------------- 提交保存（头+明细一次性提交）--------------------
function submitForm() {
  proxy.$refs['formRef'].validate((valid: boolean) => {
    if (!valid) return
    submitLoading.value = true
    // 头+明细一次性提交（后端原子事务落库，修复旧版脏单/删行不生效问题）
    form.value.lines = lineList.value.map((l: any) => ({ ...l, rtId: form.value.rtId || undefined }))
    const action = form.value.rtId ? updateRtIssue(form.value) : addRtIssue(form.value)
    action
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
/* 可退量=0 的领料单行置灰 */
:deep(.el-table .disabled-row) {
  color: #c0c4cc;
  background-color: #fafafa;
}
</style>
