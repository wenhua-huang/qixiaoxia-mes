<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-row>
        <el-col :span="6"><el-form-item label="领料单编码" prop="issueCode"><el-input v-model="queryParams.issueCode" placeholder="请输入" clearable @keyup.enter="handleQuery" /></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="工单名称" prop="workorderName"><el-input v-model="queryParams.workorderName" placeholder="请输入" clearable @keyup.enter="handleQuery" /></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="请选择" clearable>
            <el-option v-for="d in issue_status" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item></el-col>
        <el-col :span="6"><el-form-item><el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button><el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button></el-form-item></el-col>
      </el-row>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Link" size="small" @click="handleFromWorkorder" v-hasPermi="['mes:wm:issue:add']">从工单生成</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate" v-hasPermi="['mes:wm:issue:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:wm:issue:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="primary" plain icon="Promotion" size="small" :disabled="multiple" @click="handleBatchSubmit" v-hasPermi="['mes:wm:issue:submit']">批量提交</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Check" size="small" :disabled="multiple" @click="handleBatchApprove" v-hasPermi="['mes:wm:issue:approve']">批量审核</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Lock" size="small" :disabled="multiple" @click="handleBatchConfirm" v-hasPermi="['mes:wm:issue:edit']">批量预占</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" size="small" @click="handleExport" v-hasPermi="['mes:wm:issue:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="领料单编码" align="center" prop="issueCode" width="150" />
      <el-table-column label="领料单名称" align="center" prop="issueName" :show-overflow-tooltip="true" min-width="150" />
      <el-table-column label="关联工单" align="center" prop="workorderName" width="130" :show-overflow-tooltip="true" />
      <el-table-column label="领料日期" align="center" width="100"><template #default="scope">{{ parseTime(scope.row.issueDate, '{y}-{m}-{d}') }}</template></el-table-column>
      <el-table-column label="申请/已发" align="center" width="100">
        <template #default="scope"><span>{{ scope.row.quantityTotal || 0 }} / {{ scope.row.quantityIssuedTotal || 0 }}</span></template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope"><dict-tag :options="issue_status" :value="scope.row.status" /></template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="260" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tooltip content="查看" placement="top"><el-button link type="primary" icon="View" @click="handleView(scope.row)" v-hasPermi="['mes:wm:issue:query']"></el-button></el-tooltip>
          <el-tooltip content="提交审核" placement="top" v-if="scope.row.status==='DRAFT'"><el-button link type="primary" icon="Promotion" @click="handleSubmit(scope.row)" v-hasPermi="['mes:wm:issue:submit']"></el-button></el-tooltip>
          <el-tooltip content="审核通过" placement="top" v-if="scope.row.status==='PENDING'"><el-button link type="success" icon="Check" @click="handleApprove(scope.row)" v-hasPermi="['mes:wm:issue:approve']"></el-button></el-tooltip>
          <el-tooltip content="审核退回" placement="top" v-if="scope.row.status==='PENDING'"><el-button link type="warning" icon="Back" @click="handleReject(scope.row)" v-hasPermi="['mes:wm:issue:approve']"></el-button></el-tooltip>
          <el-tooltip content="预占库存" placement="top" v-if="scope.row.status==='APPROVED'"><el-button link type="success" icon="Lock" @click="handleConfirm(scope.row)" v-hasPermi="['mes:wm:issue:edit']"></el-button></el-tooltip>
          <el-tooltip content="释放预占" placement="top" v-if="scope.row.status==='ALLOCATED'"><el-button link type="info" icon="Unlock" @click="handleRelease(scope.row)" v-hasPermi="['mes:wm:issue:edit']"></el-button></el-tooltip>
          <el-tooltip content="发料出库" placement="top" v-if="scope.row.status==='ALLOCATED' || scope.row.status==='PARTIAL_ISSUED'"><el-button link type="warning" icon="Upload" @click="handleIssueOut(scope.row)" v-hasPermi="['mes:wm:issue:issueOut']"></el-button></el-tooltip>
          <el-tooltip content="关闭" placement="top" v-if="scope.row.status==='ISSUED' || scope.row.status==='PARTIAL_ISSUED'"><el-button link type="primary" icon="CircleClose" @click="handleClose2(scope.row)" v-hasPermi="['mes:wm:issue:close']"></el-button></el-tooltip>
          <el-tooltip content="修改" placement="top" v-if="scope.row.status==='DRAFT'"><el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:issue:edit']"></el-button></el-tooltip>
          <el-tooltip content="作废" placement="top" v-if="!isTerminal(scope.row.status)"><el-button link type="danger" icon="Close" @click="handleCancel(scope.row)" v-hasPermi="['mes:wm:issue:cancel']"></el-button></el-tooltip>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑/查看弹窗 -->
    <el-dialog :title="title" v-model="open" width="1000px" append-to-body @close="cancel">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="header">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
            <el-row>
              <el-col :span="16"><el-form-item label="领料单编码" prop="issueCode"><el-input v-model="form.issueCode" :placeholder="optType==='add' ? '保存时自动生成' : '请输入'" :disabled="true" /></el-form-item></el-col>
            </el-row>
            <el-row><el-col :span="12"><el-form-item label="领料单名称" prop="issueName"><el-input v-model="form.issueName" placeholder="请输入" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="领料类型"><el-select v-model="form.issueType" disabled><el-option label="生产领料" value="PRODUCE" /></el-select></el-form-item></el-col></el-row>
            <el-row><el-col :span="12"><el-form-item label="生产工单" prop="workorderId"><template v-if="optType!=='edit'"><el-input v-model="form.workorderCode" :disabled="true" /></template><template v-else><el-input v-model="form.workorderName" :placeholder="form.workorderCode || '请选择生产工单'" readonly><template #append><el-button icon="Search" @click="handleWorkorderSelect" /></template></el-input><workorderSelect ref="woSelectRef" @onSelected="onWorkorderSelected" /></template></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="仓库" prop="warehouseId"><template v-if="optType==='view'"><el-input v-model="form.warehouseName" :disabled="true" /></template><template v-else><el-input v-model="form.warehouseName" placeholder="请选择仓库" readonly><template #append><el-button icon="Search" @click="handleWarehouseSelect" /></template></el-input><WarehouseSelect ref="warehouseSelectRef" @onSelected="onWarehouseSelected" /></template></el-form-item></el-col></el-row>
            <el-row><el-col :span="12"><el-form-item label="库区"><template v-if="optType==='view'"><el-input v-model="form.locationName" :disabled="true" /></template><template v-else><el-input v-model="form.locationName" placeholder="请选择库区" readonly><template #append><el-button icon="Search" @click="handleLocationSelect" /></template></el-input><LocationSelect ref="locationSelectRef" @onSelected="onLocationSelected" /></template></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="领料日期"><el-date-picker v-model="form.issueDate" type="date" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" :disabled="optType==='view'" /></el-form-item></el-col></el-row>
            <el-row><el-col :span="12"><el-form-item label="流转卡"><el-select v-model="form.cardId" placeholder="不选则自动取工单默认卡" clearable :disabled="optType==='view'" style="width:100%" @focus="loadCardOptions"><el-option v-for="c in cardOptions" :key="c.cardId" :label="c.cardCode" :value="c.cardId" /></el-select></el-form-item></el-col></el-row>
            <el-row><el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" :disabled="optType==='view'" /></el-form-item></el-col></el-row>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="领料明细" name="lines" v-if="form.issueId || optType==='add'">
          <el-row class="mb8" v-if="optType==='add' || optType==='edit'">
            <el-col :span="1.5" v-if="form.workorderId"><el-button type="success" plain icon="MagicStick" size="small" @click="handleLoadBom">重新从BOM拉取</el-button></el-col>
          </el-row>
          <el-table :data="lineList" size="small">
            <el-table-column label="物料编码" align="center" prop="itemCode" width="120" />
            <el-table-column label="物料名称" align="center" prop="itemName" :show-overflow-tooltip="true" />
            <el-table-column label="规格" align="center" prop="itemSpc" width="120" :show-overflow-tooltip="true" />
            <el-table-column label="申请数量" align="center" width="110"><template #default="scope"><el-input-number v-if="optType!=='view'" v-model="scope.row.quantityIssue" :min="0" :precision="2" size="small" controls-position="right" style="width:100px" /><span v-else>{{ scope.row.quantityIssue }}</span></template></el-table-column>
            <el-table-column label="已发料" align="center" width="80"><template #default="scope">{{ scope.row.quantityIssued || 0 }}</template></el-table-column>
            <el-table-column label="单位" align="center" prop="unitName" width="60" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button type="primary" @click="submitForm" v-if="optType!=='view'">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 发料弹窗 -->
    <IssueOutDialog ref="issueOutRef" @success="getList" />
    <!-- 从工单生成：工单选择器（固定只列待生产/生产中工单） -->
    <workorderSelect ref="genWoSelectRef" :status-list="['PREPARE','PRODUCING']" @onSelected="onGenWorkorderSelected" />

    <!-- 作废原因弹窗 -->
    <el-dialog title="作废领料单" v-model="cancelOpen" width="450px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="作废原因"><el-input v-model="cancelReason" type="textarea" :rows="3" placeholder="请输入作废原因（可选）" /></el-form-item>
      </el-form>
      <template #footer><el-button type="danger" @click="confirmCancel">确认作废</el-button><el-button @click="cancelOpen=false">取 消</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, getCurrentInstance, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { listIssueHeader, getIssueDetail, addIssueHeader, updateIssueHeader, delIssueHeader, confirmIssue, releaseAllocation, submitForApprove, approveIssue, rejectIssue, closeIssue, cancelIssue, buildFromWorkorder, batchSubmitForApprove, batchApprove, batchConfirmIssue } from '@/api/mes/wm/issueheader'
import { addIssueLine, updateIssueLine, delIssueLine } from '@/api/mes/wm/issueline'
import { loadBomLines } from '@/api/mes/wm/issueheader'
import { genSerialCode } from '@/api/mes/sys/autocoderule'
import { listProcard } from '@/api/mes/pro/procard'
import { getWorkorder } from '@/api/mes/pro/workorder'
import workorderSelect from '@/components/workorderSelect/single.vue'
import WarehouseSelect from '@/components/warehouseSelect/single.vue'
import LocationSelect from '@/components/locationSelect/single.vue'
import IssueOutDialog from './components/IssueOutDialog.vue'

const { proxy } = getCurrentInstance() as any
const router = useRouter()
const { issue_status } = proxy.useDict('mes_wm_issue_status')

const loading = ref(true); const ids = ref<number[]>([]); const single = ref(true); const multiple = ref(true); const showSearch = ref(true); const total = ref(0)
const title = ref(''); const open = ref(false); const optType = ref(''); const activeTab = ref('header')
const dataList = ref<any[]>([]); const lineList = ref<any[]>([])
const cardOptions = ref<any[]>([])
const form = reactive<any>({ issueType: 'PRODUCE' })
const queryParams = reactive<any>({ pageNum: 1, pageSize: 10, issueCode: null, workorderName: null, status: null })
const rules = { issueName: [{ required: true, message: '名称不能为空' }], workorderId: [{ required: true, message: '工单不能为空' }], warehouseId: [{ required: true, message: '仓库不能为空' }] }
const TERMINAL = ['CLOSED', 'CANCELED']
const isTerminal = (s: string) => TERMINAL.includes(s)

// selector refs
const woSelectRef = ref(); const warehouseSelectRef = ref(); const locationSelectRef = ref(); const genWoSelectRef = ref()
// 发料弹窗 ref
const issueOutRef = ref()
// 作废弹窗
const cancelOpen = ref(false); const cancelReason = ref(''); const cancelTargetId = ref<number | null>(null)

onMounted(() => getList())
function getList() { loading.value = true; listIssueHeader(queryParams).then((r: any) => { dataList.value = r.rows; total.value = r.total; loading.value = false }).catch(() => { loading.value = false }) }
function cancel() { open.value = false; reset() }
function reset() { Object.keys(form).forEach(k => delete (form as any)[k]); form.issueType = 'PRODUCE'; lineList.value = []; activeTab.value = 'header' }
function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() { Object.keys(queryParams).forEach(k => { if (k !== 'pageNum' && k !== 'pageSize') (queryParams as any)[k] = null }); handleQuery() }
function handleSelectionChange(sel: any[]) { ids.value = sel.map(i => i.issueId); single.value = sel.length !== 1; multiple.value = !sel.length }
// 从工单生成：打开工单选择器（固定状态过滤 PREPARE/PRODUCING）
function handleFromWorkorder() { genWoSelectRef.value?.open() }
async function onGenWorkorderSelected(row: any) {
  if (!row) return
  try {
    const r: any = await buildFromWorkorder(row.workorderId)
    openAddDraft(r.data)
  } catch (e) { /* 错误已由 request 拦截器提示 */ }
}
// 用草稿打开编辑弹窗（工单驱动，工单/BOM 锁定不可改，仅可调领料数量/仓库/批次/日期/备注）
function openAddDraft(draft: any) {
  reset()
  const { lines, ...header } = draft
  Object.assign(form, header)
  form.issueCode = '' // 草稿态无编码，保存时后端自动生成
  lineList.value = (lines || []).map((l: any) => ({ ...l }))
  optType.value = 'add'; activeTab.value = 'header'
  title.value = '从工单生成领料单'
  open.value = true
}
async function fill(row: any, type: string, ttl: string) { reset(); const r: any = await getIssueDetail(row.issueId); const h = r.data?.header || r.data; const lines = r.data?.lines || []; Object.assign(form, h); lineList.value = lines; title.value = ttl; optType.value = type; activeTab.value = 'header'; await nextTick(); open.value = true }
function handleView(row: any) { fill(row, 'view', '查看领料单') }
function handleUpdate(row: any) { fill(row, 'edit', '修改领料单') }
function handleDelete(row: any) { const idStr = ids.value.join(',') || row.issueId; proxy.$modal.confirm('确认删除？仅草稿/待审核状态可删。').then(() => delIssueHeader(idStr)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {}) }
function handleExport() { proxy.download('/mes/wm/issueheader/export', { ...queryParams }, 'issue.xlsx') }

// 状态流转动作
const act = (row: any, api: any, okMsg: string, confirmMsg?: string) => {
  const run = () => api(row.issueId).then(() => { proxy.$modal.msgSuccess(okMsg); getList() })
  if (confirmMsg) proxy.$modal.confirm(confirmMsg).then(run).catch(() => {})
  else run()
}
function handleSubmit(row: any) { act(row, submitForApprove, '已提交审核', `提交审核【${row.issueName}】？`) }
function handleApprove(row: any) { act(row, approveIssue, '审核通过', `审核通过【${row.issueName}】？`) }
function handleReject(row: any) { act(row, rejectIssue, '已退回草稿', `审核退回【${row.issueName}】到草稿？`) }
function handleConfirm(row: any) { act(row, confirmIssue, '已预占库存', `预占库存【${row.issueName}】？将扣减可用库存。`) }
function handleRelease(row: any) { act(row, releaseAllocation, '已释放预占', `释放预占【${row.issueName}】？将恢复可用库存。`) }
function handleClose2(row: any) { act(row, closeIssue, '已关闭', `关闭领料单【${row.issueName}】？关闭后不可再操作。`) }
function handleCancel(row: any) { cancelTargetId.value = row.issueId; cancelReason.value = ''; cancelOpen.value = true }
function confirmCancel() { if (!cancelTargetId.value) return; cancelIssue(cancelTargetId.value, cancelReason.value).then(() => { proxy.$modal.msgSuccess('已作废'); cancelOpen.value = false; getList() }) }

// 批量流转动作：尽力执行，失败明细用 alert 列出
const actBatch = (api: any, verb: string) => {
  if (!ids.value.length) return
  proxy.$modal.confirm(`确认对选中的 ${ids.value.length} 张领料单执行「${verb}」？状态不符或库存不足的单据将被跳过并提示。`)
    .then(() => api([...ids.value]).then((res: any) => {
      const r = res.data || {}
      const ok = r.successCount ?? 0, fail = r.failedCount ?? 0
      // 全成功→success；部分成功→warning；全失败→error（避免"完成: 0 张成功"这种矛盾提示）
      const msg = `${verb}完成：成功 ${ok} 张` + (fail ? `，失败 ${fail} 张` : '')
      if (fail === 0) proxy.$modal.msgSuccess(msg)
      else if (ok === 0) proxy.$modal.msgError(msg)
      else proxy.$modal.msgWarning(msg)
      if (fail && r.failures?.length) {
        const lines = r.failures.map((f: any) => `· ${f.issueCode || f.issueId}：${f.reason}`).join('\n')
        proxy.$modal.alertError(`${verb}失败明细（${fail} 张）：\n\n${lines}`)
      }
      getList()
    })).catch(() => {})
}
function handleBatchSubmit()  { actBatch(batchSubmitForApprove, '提交审核') }
function handleBatchApprove() { actBatch(batchApprove, '审核') }
function handleBatchConfirm() { actBatch(batchConfirmIssue, '预占库存') }

// 发料出库（打开弹窗）
async function handleIssueOut(row: any) {
  // 前置校验：工单必须已开工（开工时自动建立流转卡作为物料追溯载体）
  // 未开工发料会导致物料无处挂载，追溯链断裂
  if (row.workorderId) {
    try {
      const woRes: any = await getWorkorder(row.workorderId)
      const wo = woRes?.data || {}
      if (wo.status !== 'PRODUCING') {
        proxy.$modal.confirm(
          `工单【${wo.workorderCode || row.workorderName}】尚未开工（当前状态：${wo.status || '未知'}）。\n\n` +
          `发料出库前需要先开工，开工时会自动建立流转卡作为物料追溯载体。\n\n` +
          `点击「确定」跳转到工单管理页面完成开工。`
        ).then(() => {
          router.push({ path: '/mes/pro/workorder', query: { workorderId: row.workorderId, action: 'start' } })
        }).catch(() => {})
        return
      }
    } catch (e) {
      // 工单查询失败不阻断，让后端校验兜底
      console.warn('查询工单状态失败，交由后端校验', e)
    }
  }
  const r: any = await getIssueDetail(row.issueId)
  const h = r.data?.header || r.data
  const lines = r.data?.lines || []
  issueOutRef.value?.open(h, lines)
}

// 选择器 handlers
function handleWorkorderSelect() { woSelectRef.value?.open() }
function onWorkorderSelected(row: any) { if (!row) return; form.workorderId = row.workorderId; form.workorderCode = row.workorderCode; form.workorderName = row.workorderName; form.cardId = null; cardOptions.value = [] }

// ==================== 流转卡选择 ====================
function loadCardOptions() {
  if (!form.workorderId) return
  listProcard({ workorderId: form.workorderId, status: 'ACTIVE', pageNum: 1, pageSize: 50 }).then((res: any) => {
    cardOptions.value = res.rows || []
  }).catch(() => {})
}
function handleWarehouseSelect() { warehouseSelectRef.value?.open() }
function onWarehouseSelected(row: any) { if (!row) return; form.warehouseId = row.warehouseId; form.warehouseName = row.warehouseName; form.warehouseCode = row.warehouseCode }
function handleLocationSelect() { locationSelectRef.value?.open() }
function onLocationSelected(row: any) { if (!row) return; form.locationId = row.locationId; form.locationName = row.locationName; form.locationCode = row.locationCode }

// 重新从工单 BOM 拉取领料明细（覆盖当前明细，不落库）
function handleLoadBom() {
  if (!form.workorderId) { proxy.$modal.msgError('工单缺失，无法拉取BOM'); return }
  buildFromWorkorder(form.workorderId).then((r: any) => {
    const draft = r.data
    if (draft && draft.lines) {
      lineList.value = draft.lines.map((l: any) => ({ ...l, warehouseId: form.warehouseId || l.warehouseId }))
      proxy.$modal.msgSuccess('已从BOM重新拉取明细')
    }
  })
}

function submitForm() {
  (proxy.$refs.formRef as any).validate((v: boolean) => {
    if (!v) return
    // 头+明细一次性提交（后端原子事务落库，修复旧版脏单/删行不生效问题）
    form.lines = lineList.value.map((l: any) => ({ ...l, issueId: form.issueId || undefined }))
    const action = form.issueId ? updateIssueHeader(form) : addIssueHeader(form)
    action.then(() => { proxy.$modal.msgSuccess('保存成功'); open.value = false; getList() }).catch(() => {})
  })
}
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
</style>
