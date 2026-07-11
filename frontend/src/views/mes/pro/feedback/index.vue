<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-row>
        <el-col :span="8">
          <el-form-item label="报工编码" prop="feedbackCode">
            <el-input v-model="queryParams.feedbackCode" placeholder="请输入报工编码" clearable @keyup.enter="handleQuery" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="工单名称" prop="workorderName">
            <el-input v-model="queryParams.workorderName" placeholder="请输入工单名称" clearable @keyup.enter="handleQuery" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="工作站名称" prop="workstationName">
            <el-input v-model="queryParams.workstationName" placeholder="请输入工作站名称" clearable @keyup.enter="handleQuery" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 100%; min-width: 160px" size="default">
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
          <el-form-item label="报工类型" prop="feedbackType">
            <el-select v-model="queryParams.feedbackType" placeholder="请选择报工类型" clearable style="width: 100%; min-width: 160px" size="default">
              <el-option
                v-for="item in feedbackTypeOptions"
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
        <el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['mes:pro:feedback:add']">新增报工</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:pro:feedback:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:pro:feedback:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" size="small" @click="handleExport" v-hasPermi="['mes:pro:feedback:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="报工编码" align="center" prop="feedbackCode" width="150">
        <template #default="scope">
          <el-button link type="primary" size="small" @click="handleView(scope.row)" v-hasPermi="['mes:pro:feedback:query']">{{ scope.row.feedbackCode }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="工单名称" align="center" prop="workorderName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="工序名称" align="center" prop="processName" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="工作站" align="center" prop="workstationName" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="报工类型" align="center" prop="feedbackType" width="110">
        <template #default="scope">
          <span>{{ feedbackTypeLabelMap[scope.row.feedbackType] || scope.row.feedbackType }}</span>
        </template>
      </el-table-column>
      <el-table-column label="合格数" align="center" prop="quantityQualified" width="80" />
      <el-table-column label="不合格数" align="center" prop="quantityUnqualified" width="90" />
      <el-table-column label="状态" align="center" prop="status" width="85">
        <template #default="scope">
          <el-tag :type="statusTagMap[scope.row.status] || 'info'" size="small">{{ statusLabelMap[scope.row.status] || scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="报工人" align="center" prop="nickName" width="90" :show-overflow-tooltip="true">
        <template #default="scope">
          <span>{{ scope.row.nickName || scope.row.userName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="报工时间" align="center" prop="feedbackTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.feedbackTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="260" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="success" size="small" icon="CircleCheck" @click="handleConfirm(scope.row)" v-if="scope.row.status==='PREPARE'" v-hasPermi="['mes:pro:feedback:edit']">确认</el-button>
          <el-button link type="primary" size="small" icon="Check" @click="handleAudit(scope.row)" v-if="scope.row.status==='CONFIRMED'" v-hasPermi="['mes:pro:feedback:edit']">审核</el-button>
          <el-button link type="primary" size="small" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:pro:feedback:edit']">修改</el-button>
          <el-button link type="primary" size="small" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:pro:feedback:remove']">删除</el-button>
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
    <el-dialog :title="title" v-model="open" width="860px" append-to-body :close-on-click-modal="false" @close="cancel">
      <el-tabs v-model="activeTab" type="border-card">
        <!-- Tab1: 报工信息 -->
        <el-tab-pane label="报工信息" name="feedback">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
            <!-- 第1行：生产任务 + 填单联动提示 -->
            <el-row>
              <el-col :span="18">
                <el-form-item label="生产任务" prop="taskName">
                  <el-input v-model="form.taskName" placeholder="请选择生产任务（自动填充工单/工序/产品信息）" readonly>
                    <template #append v-if="optType !== 'view'">
                      <el-button icon="Search" @click="handleTaskSelect" />
                    </template>
                  </el-input>
                </el-form-item>
              </el-col>
            </el-row>

            <!-- 第2行：报工编码 + 报工类型 -->
            <el-row>
              <el-col :span="12">
                <el-form-item label="报工编码" prop="feedbackCode">
                  <el-input v-model="form.feedbackCode" placeholder="自动生成" :disabled="optType !== 'add'">
                    <template #append v-if="optType === 'add'">
                      <el-button icon="Refresh" @click="generateFeedbackCode" :loading="genLoading">生成</el-button>
                    </template>
                  </el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="报工类型" prop="feedbackType">
                  <el-select v-model="form.feedbackType" placeholder="请选择报工类型" style="width: 100%">
                    <el-option
                      v-for="item in feedbackTypeOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <!-- 第3行：工单 + 产品物料 -->
            <el-row>
              <el-col :span="16">
                <el-form-item label="生产工单" prop="workorderName">
                  <el-input v-model="form.workorderName" placeholder="请选择生产工单" readonly>
                    <template #append v-if="optType !== 'view'">
                      <el-button icon="Search" @click="handleWorkorderSelect" />
                    </template>
                  </el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="产品物料">
                  <el-input v-model="form.itemName" placeholder="自动填充" disabled />
                </el-form-item>
              </el-col>
            </el-row>

            <!-- 第4行：工序 + 工作站 -->
            <el-row>
              <el-col :span="12">
                <el-form-item label="工序">
                  <el-input v-model="form.processName" placeholder="自动填充" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="工作站名称">
                  <el-input v-model="form.workstationName" placeholder="点击右侧按钮选择工作站" readonly>
                    <template #append>
                      <el-button icon="Search" @click="handleWorkstationSelect" />
                    </template>
                  </el-input>
                </el-form-item>
              </el-col>
            </el-row>

            <!-- 第7行：报工数量 + 合格 + 不合格 -->
            <el-row>
              <el-col :span="8">
                <el-form-item label="报工数量" prop="quantityFeedback">
                  <el-input-number v-model="form.quantityFeedback" :min="0" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="合格数" prop="quantityQualified">
                  <el-input-number v-model="form.quantityQualified" :min="0" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="不合格数" prop="quantityUnqualified">
                  <el-input-number v-model="form.quantityUnqualified" :min="0" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>

            <!-- 第8行：工废 + 料废 + 待检 -->
            <el-row>
              <el-col :span="8">
                <el-form-item label="工废" prop="quantityLaborScrap">
                  <el-input-number v-model="form.quantityLaborScrap" :min="0" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="料废" prop="quantityMaterialScrap">
                  <el-input-number v-model="form.quantityMaterialScrap" :min="0" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="待检测" prop="quantityUncheck">
                  <el-input-number v-model="form.quantityUncheck" :min="0" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>

            <!-- 第9行：报工渠道 + 操作工 + 生产批号 -->
            <el-row>
              <el-col :span="8">
                <el-form-item label="报工渠道" prop="feedbackChannel">
                  <el-select v-model="form.feedbackChannel" placeholder="请选择" style="width: 100%">
                    <el-option label="PC端" value="PC" />
                    <el-option label="Pad端" value="PAD" />
                    <el-option label="扫码" value="SCAN" />
                    <el-option label="微信" value="WECHAT" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="操作工" prop="userName">
                  <el-input v-model="form.userName" placeholder="请输入操作工" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="生产批号" prop="lotNumber">
                  <el-input v-model="form.lotNumber" placeholder="请输入生产批号" />
                </el-form-item>
              </el-col>
            </el-row>

            <!-- 第10行：报工时间 + 备注 -->
            <el-row>
              <el-col :span="12">
                <el-form-item label="报工时间" prop="feedbackTime">
                  <el-date-picker
                    v-model="form.feedbackTime"
                    type="datetime"
                    placeholder="请选择报工时间"
                    style="width: 100%"
                    value-format="YYYY-MM-DD HH:mm:ss"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <!-- 第11行：备注 -->
            <el-row>
              <el-col :span="24">
                <el-form-item label="备注" prop="remark">
                  <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <!-- Tab2: 物料消耗 -->
        <el-tab-pane label="物料消耗" name="consume">
          <div style="margin-bottom: 10px">
            <el-button type="primary" size="small" icon="Plus" @click="handleAddConsume" v-hasPermi="['mes:pro:feedback:add']">新增物料</el-button>
          </div>
          <el-table :data="consumeList" border size="small" max-height="300">
            <el-table-column label="物料编码" align="center" prop="itemCode" width="130">
              <template #default="scope">
                <el-input v-model="scope.row.itemCode" size="small" placeholder="请输入" />
              </template>
            </el-table-column>
            <el-table-column label="物料名称" align="center" prop="itemName" width="150">
              <template #default="scope">
                <el-input v-model="scope.row.itemName" size="small" placeholder="请输入" />
              </template>
            </el-table-column>
            <el-table-column label="消耗数量" align="center" prop="quantity" width="120">
              <template #default="scope">
                <el-input-number v-model="scope.row.quantity" :min="0" size="small" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column label="批次号" align="center" prop="batchCode" min-width="130">
              <template #default="scope">
                <el-input v-model="scope.row.batchCode" size="small" placeholder="请输入" />
              </template>
            </el-table-column>
            <el-table-column label="操作" align="center" width="80">
              <template #default="scope">
                <el-button link type="danger" size="small" icon="Delete" @click="handleDeleteConsume(scope.$index)" v-hasPermi="['mes:pro:feedback:remove']" />
              </template>
            </el-table-column>
          </el-table>
          <div v-if="consumeList.length === 0" style="text-align: center; padding: 20px; color: #909399;">暂无物料消耗数据</div>
        </el-tab-pane>

        <!-- Tab3: 工序参数 -->
        <el-tab-pane label="工序参数" name="param">
          <el-table :data="paramList" border size="small" max-height="320">
            <el-table-column label="参数名称" align="center" prop="paramName" min-width="130" />
            <el-table-column label="单位" align="center" prop="unit" width="80" />
            <el-table-column label="标准范围" align="center" min-width="140">
              <template #default="scope">
                <span>{{ formatRange(scope.row) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="实际值" align="center" prop="actualValue" min-width="150">
              <template #default="scope">
                <el-input v-model="scope.row.actualValue" size="small" placeholder="请输入" :disabled="optType === 'view'" />
              </template>
            </el-table-column>
            <el-table-column label="偏差" align="center" width="80">
              <template #default="scope">
                <el-tag v-if="scope.row.isDeviation === 'Y'" type="danger" size="small">偏差</el-tag>
                <el-tag v-else-if="scope.row.isDeviation === 'N'" type="success" size="small">正常</el-tag>
                <span v-else style="color: #909399">-</span>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="paramList.length === 0" style="text-align: center; padding: 20px; color: #909399;">
            暂无工序参数{{ form.processId ? '' : '（请先选择工序）' }}
          </div>
          <div v-if="paramList.some(p => p.isDeviation === 'Y')" style="margin-top: 10px; color: #f56c6c; font-size: 12px;">
            <el-icon><Warning /></el-icon> 存在偏差参数，请核对实际值是否符合工艺要求
          </div>
        </el-tab-pane>
      </el-tabs>

      <!-- 工作站选择弹窗 -->
      <WorkstationSelect ref="wsSelectRef" @onSelected="onWorkstationSelected" />
      <taskSelect ref="taskSelectRef" @onSelected="onTaskSelected" />
      <workorderSelect ref="woSelectRef" @onSelected="onWorkorderSelected" />

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
import { Warning } from '@element-plus/icons-vue'
import { listFeedback, getFeedback, addFeedback, updateFeedback, delFeedback, confirmFeedback, auditFeedback, getConsumeDefaults } from '@/api/mes/pro/feedback'
import { getTask } from '@/api/mes/pro/task'
import { listParamTemplateByProcessId } from '@/api/mes/pro/paramtemplate'
import { genSerialCode } from '@/api/mes/sys/autocoderule'
import WorkstationSelect from '@/components/workstationSelect/single.vue'
import taskSelect from '@/components/taskSelect/single.vue'
import workorderSelect from '@/components/workorderSelect/single.vue'

const { proxy } = getCurrentInstance() as any

// ==================== 常量映射 ====================
const statusOptions = [
  { label: '待确认', value: 'PREPARE' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '已审核', value: 'AUDITED' },
]
const statusLabelMap: Record<string, string> = { PREPARE: '待确认', CONFIRMED: '已确认', AUDITED: '已审核' }
const statusTagMap: Record<string, string> = { PREPARE: 'warning', CONFIRMED: 'success', AUDITED: '' }

const feedbackTypeOptions = [
  { label: '自制报工', value: 'INTERNAL' },
  { label: '外发代填', value: 'OUTSOURCE_AGENT' },
  { label: '外发直填', value: 'OUTSOURCE_VENDOR' },
]
const feedbackTypeLabelMap: Record<string, string> = { INTERNAL: '自制报工', OUTSOURCE_AGENT: '外发代填', OUTSOURCE_VENDOR: '外发直填' }

// ==================== 响应式状态 ====================
const loading = ref(true)
const open = ref(false)
const showSearch = ref(true)
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')
const optType = ref('')
const activeTab = ref('feedback')
const genLoading = ref(false)
const ids = ref<number[]>([])
const dataList = ref<any[]>([])

const queryFormRef = ref()
const formRef = ref()
const wsSelectRef = ref()
const taskSelectRef = ref()
const woSelectRef = ref()

// 物料消耗列表（Tab2 内联编辑表格数据）
interface ConsumeItem {
  id?: number
  itemId: number | null
  itemCode: string
  itemName: string
  quantity: number
  batchCode: string
}
const consumeList = ref<ConsumeItem[]>([])

// 工序参数列表（Tab3，新增时从参数模板加载，提交时回传后端做偏差判定）
interface ParamItem {
  templateId: number | null
  workorderParamId: number | null
  paramName: string
  unit: string
  minValue: number | null
  maxValue: number | null
  actualValue: string
  isDeviation: string | null
}
const paramList = ref<ParamItem[]>([])

/** 格式化参数标准范围显示 */
function formatRange(row: ParamItem): string {
  const hasMin = row.minValue != null
  const hasMax = row.maxValue != null
  if (hasMin && hasMax) return `${row.minValue} ~ ${row.maxValue}`
  if (hasMin) return `≥ ${row.minValue}`
  if (hasMax) return `≤ ${row.maxValue}`
  return '-'
}

const queryParams = reactive<any>({
  pageNum: 1,
  pageSize: 10,
  feedbackCode: null,
  workorderName: null,
  workstationName: null,
  status: null,
  feedbackType: null,
})

const form = reactive<any>({
  recordId: null,
  feedbackCode: null,
  feedbackType: 'INTERNAL',
  taskId: null,
  workorderId: null,
  workorderName: null,
  processId: null,
  processName: null,
  workstationId: null,
  workstationName: null,
  itemId: null,
  itemName: null,
  routeId: null,
  quantityFeedback: 0,
  quantityQualified: 0,
  quantityUnqualified: 0,
  quantityLaborScrap: 0,
  quantityMaterialScrap: 0,
  quantityUncheck: 0,
  feedbackChannel: 'PC',
  userName: null,
  lotNumber: null,
  feedbackTime: null,
  remark: null,
})

const rules = {
  feedbackCode: [
    { required: true, message: '报工编码不能为空，请点击生成按钮自动生成', trigger: 'blur' },
  ],
  feedbackType: [
    { required: true, message: '报工类型不能为空', trigger: 'blur' },
  ],
  taskId: [
    { required: true, message: '生产任务不能为空', trigger: 'blur' },
  ],
  workorderId: [
    { required: true, message: '生产工单不能为空', trigger: 'blur' },
  ],
  quantityFeedback: [
    { required: true, message: '报工数量不能为空', trigger: 'blur' },
  ],
}

// ==================== 列表查询 ====================
function getList() {
  loading.value = true
  listFeedback(queryParams).then((response: any) => {
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
  ids.value = selection.map((item) => item.recordId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

// ==================== 自动生成报工编码 ====================
function generateFeedbackCode() {
  genLoading.value = true
  genSerialCode('FEEDBACK_CODE').then((r: any) => {
    form.feedbackCode = r.data || r
    genLoading.value = false
  }).catch(() => {
    genLoading.value = false
  })
}

// ==================== 任务变更 → 自动填充工单信息 ====================
function handleTaskChange(taskId: number | null | undefined) {
  if (!taskId || taskId <= 0) {
    return
  }
  getTask(taskId).then((response: any) => {
    const task = response.data
    if (task) {
      form.workorderId = task.workorderId
      form.workorderName = task.workorderName || null
      form.processId = task.processId
      form.processName = task.processName || null
      form.workstationId = task.workstationId
      form.workstationName = task.workstationName || null
      form.itemId = task.itemId
      form.itemName = task.itemName || null
      form.routeId = task.routeId || null
    }
  }).catch(() => {
    // 任务查询失败不做处理，用户可手动填写
  })
}

// ==================== 任务选择 ====================
function handleTaskSelect() {
  taskSelectRef.value?.open()
}

function onTaskSelected(row: any) {
  if (row) {
    form.taskId = row.taskId
    form.taskName = row.taskName
    form.workorderId = row.workorderId
    form.workorderName = row.workorderName
    form.processId = row.processId
    form.processName = row.processName
    form.workstationId = row.workstationId
    form.workstationName = row.workstationName
    form.itemId = row.itemId
    form.itemName = row.itemName
    form.routeId = row.routeId
    fetchConsumeDefaults(row.workorderId)
    if (row.processId) loadParamTemplates(row.processId)
  }
}

/** 加载工序参数模板（只加载报工可见 isReportVisible=Y 的），填充到 paramList 供操作工填报 */
function loadParamTemplates(processId: number) {
  listParamTemplateByProcessId(processId).then((res: any) => {
    const templates = (res.data || []).filter((t: any) => t.isReportVisible === 'Y' && t.enableFlag === '1')
    paramList.value = templates.map((t: any) => ({
      templateId: t.templateId,
      workorderParamId: null,
      paramName: t.paramName,
      unit: t.unit || '',
      minValue: t.minValue,
      maxValue: t.maxValue,
      actualValue: '',
      isDeviation: null,
    }))
  })
}

// ==================== 工单选择 ====================
function handleWorkorderSelect() {
  woSelectRef.value?.open()
}

function onWorkorderSelected(row: any) {
  if (row) {
    form.workorderId = row.workorderId
    form.workorderName = row.workorderName
    form.itemId = row.productId
    form.itemName = row.productName
    fetchConsumeDefaults(row.workorderId)
  }
}

// ==================== 工作站选择 ====================
function handleWorkstationSelect() {
  wsSelectRef.value?.open()
}

function onWorkstationSelected(row: any) {
  if (row) {
    form.workstationId = row.workstationId
    form.workstationName = row.workstationName
  }
}

// ==================== 物料消耗管理（Tab2） ====================
function handleAddConsume() {
  consumeList.value.push({
    itemId: null,
    itemCode: '',
    itemName: '',
    quantity: 0,
    batchCode: '',
  })
}

function handleDeleteConsume(index: number) {
  consumeList.value.splice(index, 1)
}

/** 根据工单ID从后端获取默认物料消耗（新增报工时预填） */
async function fetchConsumeDefaults(workorderId: number | null) {
  if (!workorderId) return
  try {
    const response: any = await getConsumeDefaults(workorderId)
    const data = response.data || response || []
    if (Array.isArray(data) && data.length > 0) {
      consumeList.value = data.map((item: any) => ({
        consumeId: item.consumeId,
        feedbackId: item.feedbackId,
        itemId: item.itemId ?? null,
        itemCode: item.itemCode ?? '',
        itemName: item.itemName ?? '',
        quantity: Number(item.quantity ?? 0),
        batchCode: item.batchCode ?? '',
        workorderId: item.workorderId ?? workorderId,
      }))
    }
  } catch {
    // 物料消耗默认查询失败，保持现有列表
  }
}

// ==================== 弹窗操作 ====================
function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.recordId = null
  form.feedbackCode = null
  form.feedbackType = 'INTERNAL'
  form.taskId = null
  form.workorderId = null
  form.workorderName = null
  form.processId = null
  form.processName = null
  form.workstationId = null
  form.workstationName = null
  form.itemId = null
  form.itemName = null
  form.routeId = null
  form.quantityFeedback = 0
  form.quantityQualified = 0
  form.quantityUnqualified = 0
  form.quantityLaborScrap = 0
  form.quantityMaterialScrap = 0
  form.quantityUncheck = 0
  form.feedbackChannel = 'PC'
  form.userName = null
  form.lotNumber = null
  form.feedbackTime = null
  form.remark = null
  consumeList.value = []
  paramList.value = []
  activeTab.value = 'feedback'
  proxy.resetForm('formRef')
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增报工'
  optType.value = 'add'
  // 自动生成报工编码，避免用户忘记点击生成按钮
  generateFeedbackCode()
}

function handleView(row: any) {
  reset()
  const recordId = row.recordId || ids.value[0]
  getFeedback(recordId).then((response: any) => {
    Object.assign(form, response.data)
    // 回填物料消耗列表（如果后端返回了 consumeList）
    if (response.data.consumeList) {
      consumeList.value = response.data.consumeList
    }
    // 回填工序参数列表（如果后端返回了 paramList）
    if (response.data.paramList) {
      paramList.value = response.data.paramList.map((p: any) => ({
        templateId: p.templateId,
        workorderParamId: p.workorderParamId,
        paramName: p.paramName || '',
        unit: p.unit || '',
        minValue: p.minValue,
        maxValue: p.maxValue,
        actualValue: p.actualValue || '',
        isDeviation: p.isDeviation,
      }))
    }
    open.value = true
    title.value = '查看报工'
    optType.value = 'view'
  })
}

function handleUpdate(row?: any) {
  reset()
  const recordId = row ? row.recordId : ids.value[0]
  getFeedback(recordId).then((response: any) => {
    Object.assign(form, response.data)
    // 回填物料消耗列表（如果后端返回了 consumeList）
    if (response.data.consumeList) {
      consumeList.value = response.data.consumeList
    }
    // 回填工序参数列表（如果后端返回了 paramList）
    if (response.data.paramList) {
      paramList.value = response.data.paramList.map((p: any) => ({
        templateId: p.templateId,
        workorderParamId: p.workorderParamId,
        paramName: p.paramName || '',
        unit: p.unit || '',
        minValue: p.minValue,
        maxValue: p.maxValue,
        actualValue: p.actualValue || '',
        isDeviation: p.isDeviation,
      }))
    }
    open.value = true
    title.value = '修改报工'
    optType.value = 'edit'
  })
}

function submitForm() {
  formRef.value?.validate((valid: boolean) => {
    if (valid) {
      // 将物料消耗列表和工序参数列表合并到表单数据
      const submitData = { ...form, consumeList: consumeList.value, paramList: paramList.value }
      if (form.recordId != null) {
        updateFeedback(submitData).then(() => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addFeedback(submitData).then(() => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

// ==================== 确认 / 审核 ====================
function handleConfirm(row: any) {
  proxy.$modal.confirm(`确认报工"${row.feedbackCode}"？确认后不可随意修改。`).then(() => {
    confirmFeedback(row.recordId).then(() => { proxy.$modal.msgSuccess('已确认'); getList() }).catch(() => {})
  }).catch(() => {})
}
function handleAudit(row: any) {
  proxy.$modal.confirm(`审核通过报工"${row.feedbackCode}"？`).then(() => {
    auditFeedback(row.recordId).then(() => { proxy.$modal.msgSuccess('已审核'); getList() }).catch(() => {})
  }).catch(() => {})
}

// ==================== 删除 / 导出 ====================
function handleDelete(row?: any) {
  const recordIds = row ? row.recordId : ids.value.join(',')
  proxy.$modal.confirm('是否确认删除报工记录编号为 "' + recordIds + '" 的数据项？').then(() => {
    return delFeedback(recordIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handleExport() {
  proxy.download('/mes/pro/feedback/export', {
    ...queryParams,
  }, `feedback_${new Date().getTime()}.xlsx`)
}

// ==================== 初始化 ====================
getList()
</script>
