<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-row>
        <el-col :span="8"><el-form-item label="领料单编码" prop="issueCode"><el-input v-model="queryParams.issueCode" placeholder="请输入" clearable @keyup.enter="handleQuery" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="工单名称" prop="workorderName"><el-input v-model="queryParams.workorderName" placeholder="请输入" clearable @keyup.enter="handleQuery" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择" clearable><el-option label="草稿" value="DRAFT" /><el-option label="已确认" value="CONFIRMED" /><el-option label="已出库" value="ISSUED" /></el-select></el-form-item></el-col>
      </el-row>
      <el-row><el-col :span="8"><el-form-item><el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button><el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button></el-form-item></el-col></el-row>
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
      <el-table-column label="领料单编码" align="center" prop="issueCode" width="140" />
      <el-table-column label="领料单名称" align="center" prop="issueName" :show-overflow-tooltip="true" min-width="150" />
      <el-table-column label="关联工单" align="center" prop="workorderName" width="130" :show-overflow-tooltip="true" />
      <el-table-column label="领料日期" align="center" width="100"><template #default="scope">{{ parseTime(scope.row.issueDate, '{y}-{m}-{d}') }}</template></el-table-column>
      <el-table-column label="总数" align="center" prop="quantityTotal" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="80"><template #default="scope"><el-tag :type="statusTag[scope.row.status]||'info'" size="small">{{ statusMap[scope.row.status] || scope.row.status }}</el-tag></template></el-table-column>
      <el-table-column label="操作" align="center" width="120" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tooltip content="查看" placement="top"><el-button link type="primary" icon="View" @click="handleView(scope.row)" v-hasPermi="['mes:wm:issue:query']"></el-button></el-tooltip>
          <el-tooltip content="修改" placement="top" v-if="scope.row.status==='DRAFT'"><el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:issue:edit']"></el-button></el-tooltip>
          <el-tooltip content="删除" placement="top" v-if="scope.row.status==='DRAFT'"><el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:wm:issue:remove']"></el-button></el-tooltip>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="1000px" append-to-body @close="cancel">
      <el-tabs v-model="activeTab" v-if="optType!=='view' || form.issueId">
        <el-tab-pane label="基本信息" name="header">
          <el-form ref="form" :model="form" :rules="rules" label-width="100px">
            <el-row>
              <el-col :span="16"><el-form-item label="领料单编码" prop="issueCode"><el-input v-model="form.issueCode" placeholder="请输入" :disabled="optType!=='add'" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label-width="80"><el-switch v-model="autoGenFlag" @change="handleAutoGen" active-text="自动生成" v-if="optType==='add'" /></el-form-item></el-col>
            </el-row>
            <el-row><el-col :span="12"><el-form-item label="领料单名称" prop="issueName"><el-input v-model="form.issueName" placeholder="请输入" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="领料类型"><el-select v-model="form.issueType" disabled><el-option label="生产领料" value="PRODUCE" /></el-select></el-form-item></el-col></el-row>
            <el-row><el-col :span="12"><el-form-item label="生产工单" prop="workorderId"><el-input-number v-model="form.workorderId" :min="1" style="width:100%" :disabled="optType==='view'" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="仓库" prop="warehouseId"><el-input-number v-model="form.warehouseId" :min="1" style="width:100%" :disabled="optType==='view'" /></el-form-item></el-col></el-row>
            <el-row><el-col :span="12"><el-form-item label="库区ID"><el-input-number v-model="form.locationId" :min="1" style="width:100%" :disabled="optType==='view'" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="领料日期"><el-date-picker v-model="form.issueDate" type="date" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" :disabled="optType==='view'" /></el-form-item></el-col></el-row>
            <el-row><el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" :disabled="optType==='view'" /></el-form-item></el-col></el-row>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="领料明细" name="lines" v-if="form.issueId">
          <el-row class="mb8" v-if="optType!=='view'"><el-col :span="1.5"><el-button type="primary" plain icon="Plus" size="small" @click="handleAddLine">新增物料行</el-button></el-col></el-row>
          <el-table :data="lineList" size="small">
            <el-table-column label="物料编码" align="center" prop="itemCode" width="120" />
            <el-table-column label="物料名称" align="center" prop="itemName" :show-overflow-tooltip="true" />
            <el-table-column label="申请数量" align="center" width="100"><template #default="scope"><el-input-number v-if="optType!=='view'" v-model="scope.row.quantityIssue" :min="0" :precision="2" size="small" controls-position="right" style="width:90px" /><span v-else>{{ scope.row.quantityIssue }}</span></template></el-table-column>
            <el-table-column label="已领数量" align="center" prop="quantityIssued" width="80"><template #default="scope"><span>{{ scope.row.quantityIssued || 0 }}</span></template></el-table-column>
            <el-table-column label="单位" align="center" prop="unitName" width="60" />
            <el-table-column label="工序" align="center" prop="processName" width="90" />
            <el-table-column label="操作" align="center" width="80" v-if="optType!=='view'" class-name="small-padding fixed-width">
              <template #default="scope">
                <el-tooltip content="编辑" placement="top"><el-button link type="primary" icon="Edit" @click="handleEditLine(scope.row)"></el-button></el-tooltip>
                <el-tooltip content="删除" placement="top"><el-button link type="primary" icon="Delete" @click="handleDelLine(scope.row)"></el-button></el-tooltip>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button type="primary" @click="submitForm" v-if="optType!=='view'">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 物料行编辑弹窗 -->
    <el-dialog :title="lineEditTitle" v-model="lineEditOpen" width="500px" append-to-body>
      <el-form ref="lineForm" :model="lineForm" label-width="100px">
        <el-form-item label="物料ID" prop="itemId"><el-input-number v-model="lineForm.itemId" :min="1" style="width:100%" placeholder="请输入物料ID" /></el-form-item>
        <el-form-item label="物料编码"><el-input v-model="lineForm.itemCode" placeholder="自动回填" disabled /></el-form-item>
        <el-form-item label="物料名称"><el-input v-model="lineForm.itemName" placeholder="自动回填" disabled /></el-form-item>
        <el-form-item label="申请数量" prop="quantityIssue"><el-input-number v-model="lineForm.quantityIssue" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="lineForm.unitName" placeholder="自动回填" disabled /></el-form-item>
        <el-form-item label="工序ID"><el-input-number v-model="lineForm.processId" :min="1" style="width:100%" placeholder="关联工序" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="confirmLineEdit">确 定</el-button><el-button @click="lineEditOpen=false">取 消</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, getCurrentInstance } from 'vue'
import { listIssueHeader, getIssueHeader, addIssueHeader, updateIssueHeader, delIssueHeader } from '@/api/mes/wm/issueheader'
import { listIssueLineByIssueId, addIssueLine, updateIssueLine, delIssueLine } from '@/api/mes/wm/issueline'
import { genSerialCode } from '@/api/mes/sys/autocoderule'

const proxy = getCurrentInstance()?.proxy as any
const loading = ref(true); const ids = ref<number[]>([]); const single = ref(true); const multiple = ref(true); const showSearch = ref(true); const total = ref(0)
const title = ref(''); const open = ref(false); const optType = ref(''); const activeTab = ref('header'); const autoGenFlag = ref(false)
const dataList = ref<any[]>([]); const lineList = ref<any[]>([])
const form = reactive<any>({ issueType: 'PRODUCE' })
const queryParams = reactive<any>({ pageNum: 1, pageSize: 10, issueCode: null, workorderName: null, status: null })
const statusMap: Record<string,string> = { DRAFT: '草稿', CONFIRMED: '已确认', ISSUED: '已出库' }
const statusTag: Record<string,string> = { DRAFT: 'warning', CONFIRMED: 'success', ISSUED: '' }
const rules = { issueCode: [{ required: true, message: '编码不能为空' }], issueName: [{ required: true, message: '名称不能为空' }], workorderId: [{ required: true, message: '工单不能为空' }] }

// Line edit
const lineEditOpen = ref(false); const lineEditTitle = ref(''); const lineEditIdx = ref(-1)
const lineForm = reactive<any>({ quantityIssue: 0 })

onMounted(() => getList())
function getList() { loading.value = true; listIssueHeader(queryParams).then((r:any) => { dataList.value = r.rows; total.value = r.total; loading.value = false }).catch(() => { loading.value = false }) }
function cancel() { open.value = false; reset() }
function reset() { Object.keys(form).forEach(k => delete (form as any)[k]); form.issueType = 'PRODUCE'; lineList.value = []; activeTab.value = 'header'; autoGenFlag.value = false }
function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() { Object.keys(queryParams).forEach(k => { if (k !== 'pageNum' && k !== 'pageSize') (queryParams as any)[k] = null }); handleQuery() }
function handleSelectionChange(sel: any[]) { ids.value = sel.map(i => i.issueId); single.value = sel.length !== 1; multiple.value = !sel.length }
function handleAutoGen(f: boolean) { if(f) genSerialCode('ISSUE_CODE').then((r:any) => { form.issueCode = r.data }); else form.issueCode = null }
function handleAdd() { reset(); open.value = true; title.value = '新增生产领料单'; optType.value = 'add'; activeTab.value = 'header'; autoGenFlag.value = true }
function handleView(row: any) { reset(); getIssueHeader(row.issueId).then((r:any) => { Object.assign(form, r.data); open.value = true; title.value = '查看领料单'; optType.value = 'view'; activeTab.value = 'header'; loadLines(r.data.issueId) }) }
function handleUpdate(row: any) { reset(); getIssueHeader(row.issueId).then((r:any) => { Object.assign(form, r.data); open.value = true; title.value = '修改领料单'; optType.value = 'edit'; activeTab.value = 'header'; loadLines(r.data.issueId) }) }
function handleDelete(row: any) { const idStr = ids.value.join(',') || row.issueId; proxy.$modal.confirm('确认删除？').then(() => delIssueHeader(idStr)).then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {}) }
function handleExport() { proxy.download('/mes/wm/issueheader/export', { ...queryParams }, 'issue.xlsx') }

function loadLines(issueId: number) { if(issueId) listIssueLineByIssueId(issueId).then((r:any) => { lineList.value = r.data || [] }) }
function handleAddLine() { lineForm.issueId = form.issueId; lineForm.itemId = null; lineForm.itemCode = ''; lineForm.itemName = ''; lineForm.quantityIssue = 0; lineForm.unitName = ''; lineForm.processId = null; lineEditIdx.value = -1; lineEditTitle.value = '新增物料行'; lineEditOpen.value = true }
function handleEditLine(row: any) { lineEditIdx.value = lineList.value.indexOf(row); Object.assign(lineForm, row); lineEditTitle.value = '编辑物料行'; lineEditOpen.value = true }
function handleDelLine(row: any) { const idx = lineList.value.indexOf(row); if(idx>=0) lineList.value.splice(idx,1) }
function confirmLineEdit() {
  if (lineEditIdx.value >= 0) lineList.value.splice(lineEditIdx.value, 1, { ...lineForm })
  else lineList.value.push({ ...lineForm })
  lineEditOpen.value = false
}

function submitForm() {
  (proxy.$refs.form as any).validate((v: boolean) => {
    if(!v) return
    const action = form.issueId ? updateIssueHeader(form) : addIssueHeader(form)
    action.then((r:any) => {
      const headerId = form.issueId || r.data?.issueId
      if (!headerId) { proxy.$modal.msgSuccess('保存成功'); open.value = false; getList(); return }
      // Save line items
      Promise.all(lineList.value.map((l:any) => {
        l.issueId = headerId
        return l.lineId ? updateIssueLine(l) : addIssueLine(l)
      })).then(() => {
        proxy.$modal.msgSuccess('保存成功'); open.value = false; getList()
      }).catch(() => {})
    })
  })
}
</script>
