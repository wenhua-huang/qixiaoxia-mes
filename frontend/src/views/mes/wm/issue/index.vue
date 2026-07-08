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
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['mes:wm:issue:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate" v-hasPermi="['mes:wm:issue:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:wm:issue:remove']">删除</el-button></el-col>
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
              <el-col :span="16"><el-form-item label="领料单编码" prop="issueCode"><el-input v-model="form.issueCode" placeholder="请输入" :disabled="optType!=='add'" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label-width="80"><el-switch v-model="autoGenFlag" @change="handleAutoGen" active-text="自动生成" v-if="optType==='add'" /></el-form-item></el-col>
            </el-row>
            <el-row><el-col :span="12"><el-form-item label="领料单名称" prop="issueName"><el-input v-model="form.issueName" placeholder="请输入" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="领料类型"><el-select v-model="form.issueType" disabled><el-option label="生产领料" value="PRODUCE" /></el-select></el-form-item></el-col></el-row>
            <el-row><el-col :span="12"><el-form-item label="生产工单" prop="workorderId"><template v-if="optType==='view'"><el-input v-model="form.workorderCode" :disabled="true" /></template><template v-else><el-input v-model="form.workorderName" :placeholder="form.workorderCode || '请选择生产工单'" readonly><template #append><el-button icon="Search" @click="handleWorkorderSelect" /></template></el-input><workorderSelect ref="woSelectRef" @onSelected="onWorkorderSelected" /></template></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="仓库" prop="warehouseId"><template v-if="optType==='view'"><el-input v-model="form.warehouseName" :disabled="true" /></template><template v-else><el-input v-model="form.warehouseName" placeholder="请选择仓库" readonly><template #append><el-button icon="Search" @click="handleWarehouseSelect" /></template></el-input><WarehouseSelect ref="warehouseSelectRef" @onSelected="onWarehouseSelected" /></template></el-form-item></el-col></el-row>
            <el-row><el-col :span="12"><el-form-item label="库区"><template v-if="optType==='view'"><el-input v-model="form.locationName" :disabled="true" /></template><template v-else><el-input v-model="form.locationName" placeholder="请选择库区" readonly><template #append><el-button icon="Search" @click="handleLocationSelect" /></template></el-input><LocationSelect ref="locationSelectRef" @onSelected="onLocationSelected" /></template></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="领料日期"><el-date-picker v-model="form.issueDate" type="date" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" :disabled="optType==='view'" /></el-form-item></el-col></el-row>
            <el-row><el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" :disabled="optType==='view'" /></el-form-item></el-col></el-row>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="领料明细" name="lines" v-if="form.issueId">
          <el-row class="mb8" v-if="optType==='add' || optType==='edit'">
            <el-col :span="1.5"><el-button type="primary" plain icon="Plus" size="small" @click="handleAddLine">添加物料</el-button></el-col>
            <el-col :span="1.5" v-if="form.workorderId"><el-button type="success" plain icon="MagicStick" size="small" @click="handleLoadBom">从BOM导入</el-button></el-col>
          </el-row>
          <el-table :data="lineList" size="small">
            <el-table-column label="物料编码" align="center" prop="itemCode" width="120" />
            <el-table-column label="物料名称" align="center" prop="itemName" :show-overflow-tooltip="true" />
            <el-table-column label="规格" align="center" prop="itemSpc" width="120" :show-overflow-tooltip="true" />
            <el-table-column label="申请数量" align="center" width="110"><template #default="scope"><el-input-number v-if="optType!=='view'" v-model="scope.row.quantityIssue" :min="0" :precision="2" size="small" controls-position="right" style="width:100px" /><span v-else>{{ scope.row.quantityIssue }}</span></template></el-table-column>
            <el-table-column label="已发料" align="center" width="80"><template #default="scope">{{ scope.row.quantityIssued || 0 }}</template></el-table-column>
            <el-table-column label="单位" align="center" prop="unitName" width="60" />
            <el-table-column label="操作" align="center" width="70" v-if="optType!=='view'">
              <template #default="scope"><el-button link type="danger" icon="Delete" @click="handleDelLine(scope.row)"></el-button></template>
            </el-table-column>
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
    <!-- 物料选择器（用于添加领料行） -->
    <ItemSelect ref="itemSelectRef" @onSelected="onItemSelected" />

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
import { listIssueHeader, getIssueDetail, addIssueHeader, updateIssueHeader, delIssueHeader, confirmIssue, releaseAllocation, submitForApprove, approveIssue, rejectIssue, closeIssue, cancelIssue } from '@/api/mes/wm/issueheader'
import { addIssueLine, updateIssueLine, delIssueLine } from '@/api/mes/wm/issueline'
import { loadBomLines } from '@/api/mes/wm/issueheader'
import { genSerialCode } from '@/api/mes/sys/autocoderule'
import workorderSelect from '@/components/workorderSelect/single.vue'
import WarehouseSelect from '@/components/warehouseSelect/single.vue'
import LocationSelect from '@/components/locationSelect/single.vue'
import ItemSelect from '@/components/itemSelect/single.vue'
import IssueOutDialog from './components/IssueOutDialog.vue'

const { proxy } = getCurrentInstance() as any
const { issue_status } = proxy.useDict('mes_wm_issue_status')

const loading = ref(true); const ids = ref<number[]>([]); const single = ref(true); const multiple = ref(true); const showSearch = ref(true); const total = ref(0)
const title = ref(''); const open = ref(false); const optType = ref(''); const activeTab = ref('header'); const autoGenFlag = ref(false)
const dataList = ref<any[]>([]); const lineList = ref<any[]>([])
const form = reactive<any>({ issueType: 'PRODUCE' })
const queryParams = reactive<any>({ pageNum: 1, pageSize: 10, issueCode: null, workorderName: null, status: null })
const rules = { issueCode: [{ required: true, message: '编码不能为空' }], issueName: [{ required: true, message: '名称不能为空' }], workorderId: [{ required: true, message: '工单不能为空' }] }
const TERMINAL = ['CLOSED', 'CANCELED']
const isTerminal = (s: string) => TERMINAL.includes(s)

// selector refs
const woSelectRef = ref(); const warehouseSelectRef = ref(); const locationSelectRef = ref(); const itemSelectRef = ref()
// 发料弹窗 ref
const issueOutRef = ref()
// 作废弹窗
const cancelOpen = ref(false); const cancelReason = ref(''); const cancelTargetId = ref<number | null>(null)
// 物料选择回调目标行（新增时存当前编辑行索引）
const lineSelectIdx = ref(-1)

onMounted(() => getList())
function getList() { loading.value = true; listIssueHeader(queryParams).then((r: any) => { dataList.value = r.rows; total.value = r.total; loading.value = false }).catch(() => { loading.value = false }) }
function cancel() { open.value = false; reset() }
function reset() { Object.keys(form).forEach(k => delete (form as any)[k]); form.issueType = 'PRODUCE'; lineList.value = []; activeTab.value = 'header'; autoGenFlag.value = false }
function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() { Object.keys(queryParams).forEach(k => { if (k !== 'pageNum' && k !== 'pageSize') (queryParams as any)[k] = null }); handleQuery() }
function handleSelectionChange(sel: any[]) { ids.value = sel.map(i => i.issueId); single.value = sel.length !== 1; multiple.value = !sel.length }
function handleAutoGen(f: boolean) { if (f) genSerialCode('ISSUE_CODE').then((r: any) => { form.issueCode = r.data }); else form.issueCode = null }

function handleAdd() { reset(); open.value = true; title.value = '新增生产领料单'; optType.value = 'add'; activeTab.value = 'header'; autoGenFlag.value = true }
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

// 发料出库（打开弹窗）
async function handleIssueOut(row: any) {
  const r: any = await getIssueDetail(row.issueId)
  const h = r.data?.header || r.data
  const lines = r.data?.lines || []
  issueOutRef.value?.open(h, lines)
}

// 选择器 handlers
function handleWorkorderSelect() { woSelectRef.value?.open() }
function onWorkorderSelected(row: any) { if (!row) return; form.workorderId = row.workorderId; form.workorderCode = row.workorderCode; form.workorderName = row.workorderName }
function handleWarehouseSelect() { warehouseSelectRef.value?.open() }
function onWarehouseSelected(row: any) { if (!row) return; form.warehouseId = row.warehouseId; form.warehouseName = row.warehouseName; form.warehouseCode = row.warehouseCode }
function handleLocationSelect() { locationSelectRef.value?.open() }
function onLocationSelected(row: any) { if (!row) return; form.locationId = row.locationId; form.locationName = row.locationName; form.locationCode = row.locationCode }

// 物料行
function handleAddLine() {
  lineSelectIdx.value = -1
  itemSelectRef.value?.open()
}
function onItemSelected(row: any) {
  // 新增一行领料明细（默认带入物料信息）
  lineList.value.push({
    itemId: row.itemId, itemCode: row.itemCode, itemName: row.itemName,
    itemSpc: row.specification, unitOfMeasure: row.unitOfMeasure, unitName: row.unitOfMeasure,
    quantityIssue: 1, warehouseId: form.warehouseId
  })
}
function handleLoadBom() {
  if (!form.workorderId) { proxy.$modal.msgError('请先选择工单'); return }
  loadBomLines(form.issueId, form.workorderId).then(() => {
    proxy.$modal.msgSuccess('BOM 导入成功')
    // 重新加载明细
    getIssueDetail(form.issueId).then((r: any) => { lineList.value = r.data?.lines || [] })
  })
}
function handleDelLine(row: any) { const idx = lineList.value.indexOf(row); if (idx >= 0) lineList.value.splice(idx, 1) }

function submitForm() {
  (proxy.$refs.formRef as any).validate((v: boolean) => {
    if (!v) return
    const action = form.issueId ? updateIssueHeader(form) : addIssueHeader(form)
    action.then((r: any) => {
      const headerId = form.issueId || r.data?.issueId
      if (!headerId) { proxy.$modal.msgSuccess('保存成功'); open.value = false; getList(); return }
      Promise.all(lineList.value.map((l: any) => {
        l.issueId = headerId
        return l.lineId ? updateIssueLine(l) : addIssueLine(l)
      })).then(() => { proxy.$modal.msgSuccess('保存成功'); open.value = false; getList() }).catch(() => {})
    })
  })
}
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
</style>
