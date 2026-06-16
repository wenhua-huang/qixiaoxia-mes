<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryFormRef" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="班组类型" prop="calendarType">
        <el-select v-model="queryParams.calendarType" placeholder="请选择班组类型" clearable>
          <el-option
            v-for="item in dictData.mes_calendar_type"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="计划编号" prop="planCode">
        <el-input
          v-model="queryParams.planCode"
          placeholder="请输入计划编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="计划名称" prop="planName">
        <el-input
          v-model="queryParams.planName"
          placeholder="请输入计划名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="开始日期" prop="startDate">
        <el-date-picker
          v-model="queryParams.startDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择开始日期"
          clearable
        />
      </el-form-item>
      <el-form-item label="结束日期" prop="endDate">
        <el-date-picker
          v-model="queryParams.endDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择结束日期"
          clearable
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="small"
          @click="handleAdd"
          v-hasPermi="['mes:cal:calplan:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="small"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mes:cal:calplan:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="small"
          @click="handleExport"
          v-hasPermi="['mes:cal:calplan:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="planList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="计划编号" align="center" prop="planCode">
        <template #default="scope">
          <el-button
            link
            @click="handleView(scope.row)"
            v-hasPermi="['mes:cal:calplan:query']"
          >{{ scope.row.planCode }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="计划名称" width="200" align="center" prop="planName" :show-overflow-tooltip="true" />
      <el-table-column label="班组类型" align="center" prop="calendarType">
        <template #default="scope">
          <dict-tag :options="dictData.mes_calendar_type" :value="scope.row.calendarType" />
        </template>
      </el-table-column>
      <el-table-column label="开始日期" align="center" prop="startDate" width="120">
        <template #default="scope">
          <span>{{ parseTime(scope.row.startDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结束日期" align="center" prop="endDate" width="120">
        <template #default="scope">
          <span>{{ parseTime(scope.row.endDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="轮班方式" align="center" prop="shiftType">
        <template #default="scope">
          <dict-tag :options="dictData.mes_shift_type" :value="scope.row.shiftType" />
        </template>
      </el-table-column>
      <el-table-column label="倒班方式" align="center" prop="shiftMethod">
        <template #default="scope">
          <dict-tag :options="dictData.mes_shift_method" :value="scope.row.shiftMethod" />
        </template>
      </el-table-column>
      <el-table-column label="单据状态" align="center" prop="status">
        <template #default="scope">
          <dict-tag :options="dictData.mes_order_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template #default="scope">
          <el-button
            size="small"
            link
            icon="el-icon-edit"
            v-if="scope.row.status === 'PREPARE'"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['mes:cal:calplan:edit']"
          >修改</el-button>
          <el-button
            size="small"
            link
            icon="el-icon-delete"
            v-if="scope.row.status === 'PREPARE'"
            @click="handleDelete(scope.row)"
            v-hasPermi="['mes:cal:calplan:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:current-page="queryParams.pageNum"
      v-model:page-size="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加/修改/查看排班计划对话框 -->
    <el-dialog :title="title" v-loading="formLoading" v-model="open" width="960px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="计划编号" prop="planCode">
              <el-input v-model="form.planCode" placeholder="请输入计划编号" />
            </el-form-item>
          </el-col>
          <el-col :span="5">
            <el-form-item label-width="0">
              <el-switch
                v-model="autoGenFlag"
                active-color="#13ce66"
                active-text="自动生成"
                @change="handleAutoGenChange"
                v-if="optType !== 'view'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="11">
            <el-form-item label="计划名称" prop="planName">
              <el-input v-model="form.planName" placeholder="请输入计划名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="开始日期" prop="startDate">
              <el-date-picker
                v-model="form.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择开始日期"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="结束日期" prop="endDate">
              <el-date-picker
                v-model="form.endDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择结束日期"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="班组类型" prop="calendarType">
              <el-select v-model="form.calendarType" placeholder="请选择班组类型">
                <el-option
                  v-for="item in dictData.mes_calendar_type"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="轮班方式">
              <el-radio-group v-model="form.shiftType">
                <el-radio
                  v-for="item in dictData.mes_shift_type"
                  :key="item.value"
                  :value="item.value"
                >{{ item.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col v-if="form.shiftType !== 'SINGLE'" :span="6">
            <el-form-item label="倒班方式" prop="shiftMethod">
              <el-select v-model="form.shiftMethod" placeholder="请选择倒班方式" style="width: 100px">
                <el-option
                  v-for="item in dictData.mes_shift_method"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="form.shiftMethod === 'DAY' && form.shiftType !== 'SINGLE'" :span="6">
            <el-form-item label-width="20" prop="shiftCount">
              <el-input-number :min="1" controls-position="right" v-model="form.shiftCount" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <!-- 班次/班组 Tabs — 仅编辑/查看时显示 -->
      <el-tabs type="border-card" v-if="form.planId != null">
        <el-tab-pane label="班次">
          <Shift ref="shiftTabRef" :planId="form.planId" :optType="optType" />
        </el-tab-pane>
        <el-tab-pane label="班组">
          <Team ref="teamTabRef" :planId="form.planId" :calendarType="form.calendarType" :optType="optType" />
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm" v-if="form.status === 'PREPARE' && optType !== 'view'">确 定</el-button>
          <el-button type="success" @click="handleFinish" v-if="form.status === 'PREPARE' && optType !== 'view' && form.planId != null">完成</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, getCurrentInstance } from 'vue'
import { listPlan, getPlan, delPlan, addPlan, updatePlan } from '@/api/mes/cal/plan'
import { genSerialCode } from '@/api/mes/sys/autocoderule'
import { useDict } from '@/utils/dict'
import Shift from './shift.vue'
import Team from './team.vue'

const { proxy } = getCurrentInstance() as any
const { mes_calendar_type, mes_shift_type, mes_shift_method, mes_order_status } = useDict(
  'mes_calendar_type', 'mes_shift_type', 'mes_shift_method', 'mes_order_status'
)

// 模板中需直接引用的 dict 数据
const dictData = reactive({
  mes_calendar_type,
  mes_shift_type,
  mes_shift_method,
  mes_order_status,
})

const autoGenFlag = ref(false)
const optType = ref<string | undefined>(undefined)
const loading = ref(true)
const formLoading = ref(false)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const showSearch = ref(true)
const total = ref(0)
const planList = ref<any[]>([])
const title = ref('')
const open = ref(false)
const queryFormRef = ref()
const formRef = ref()
const shiftTabRef = ref()
const teamTabRef = ref()

const queryParams = reactive<any>({
  pageNum: 1,
  pageSize: 10,
  planCode: null,
  planName: null,
  calendarType: null,
  startDate: null,
  endDate: null,
  shiftType: null,
  shiftMethod: null,
})

const form = reactive<any>({
  planId: null,
  planCode: null,
  planName: null,
  calendarType: null,
  startDate: null,
  endDate: null,
  shiftType: 'SHIFT_TWO',
  shiftMethod: 'MONTH',
  shiftCount: 1,
  status: 'PREPARE',
  remark: null,
})

const rules = {
  planCode: [
    { required: true, message: '计划编号不能为空', trigger: 'blur' },
    { max: 64, message: '字段过长', trigger: 'blur' },
  ],
  planName: [
    { required: true, message: '计划名称不能为空', trigger: 'blur' },
    { max: 100, message: '字段过长', trigger: 'blur' },
  ],
  calendarType: [
    { required: true, message: '请选择班组类型', trigger: 'blur' },
  ],
  startDate: [
    { required: true, message: '开始日期不能为空', trigger: 'blur' },
  ],
  endDate: [
    { required: true, message: '结束日期不能为空', trigger: 'blur' },
  ],
  remark: [
    { max: 250, message: '长度必须小于250个字符', trigger: 'blur' },
  ],
}

function getList() {
  loading.value = true
  listPlan(queryParams).then((response: any) => {
    planList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.planId = null
  form.planCode = null
  form.planName = null
  form.calendarType = null
  form.startDate = null
  form.endDate = null
  form.shiftType = 'SHIFT_TWO'
  form.shiftMethod = 'MONTH'
  form.shiftCount = 1
  form.status = 'PREPARE'
  form.remark = null
  autoGenFlag.value = false
  proxy.resetForm('formRef')
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
  ids.value = selection.map((item) => item.planId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '添加排班计划'
  optType.value = 'add'
}

function handleView(row: any) {
  reset()
  const planId = row.planId || ids.value
  formLoading.value = true
  getPlan(planId).then((response: any) => {
    Object.assign(form, response.data)
    open.value = true
    title.value = '查看排班计划'
    optType.value = 'view'
    formLoading.value = false
  })
}

function handleUpdate(row: any) {
  reset()
  const planId = row.planId || ids.value
  formLoading.value = true
  getPlan(planId).then((response: any) => {
    Object.assign(form, response.data)
    open.value = true
    title.value = '修改排班计划'
    optType.value = 'edit'
    formLoading.value = false
  })
}

function submitForm() {
  formRef.value?.validate((valid: boolean) => {
    if (valid) {
      if (form.planId != null) {
        updatePlan(form).then(() => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addPlan(form).then(() => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

function handleDelete(row: any) {
  const planIds = row.planId || ids.value
  proxy.$modal.confirm('是否确认删除排班计划编号为"' + planIds + '"的数据项？').then(() => {
    return delPlan(planIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handleFinish() {
  formLoading.value = true
  proxy.$modal.confirm('是否完成计划编制？【完成后将不能更改】').then(() => {
    form.status = 'CONFIRMED'
    formRef.value?.validate((valid: boolean) => {
      if (valid) {
        if (form.planId != null) {
          updatePlan(form).then(() => {
            proxy.$modal.msgSuccess('已完成')
            open.value = false
            getList()
            formLoading.value = false
          }).catch(() => {
            form.status = 'PREPARE'
            formLoading.value = false
          })
        }
      } else {
        form.status = 'PREPARE'
        formLoading.value = false
      }
    })
  }).catch(() => {
    formLoading.value = false
  })
}

function handleExport() {
  proxy.download('mes/cal/plan/export', {
    ...queryParams,
  }, `plan_${new Date().getTime()}.xlsx`)
}

function handleAutoGenChange(flag: boolean) {
  if (flag) {
    genSerialCode('CAL_PLAN_CODE').then((response: any) => {
      form.planCode = response.data
    })
  } else {
    form.planCode = null
  }
}

getList()
</script>
