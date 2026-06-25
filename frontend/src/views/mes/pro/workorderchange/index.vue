<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-row>
        <el-col :span="8">
          <el-form-item label="工单ID" prop="workorderId">
            <el-input v-model="queryParams.workorderId" placeholder="请输入工单ID" clearable @keyup.enter="handleQuery" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="变更类型" prop="changeType">
            <el-select v-model="queryParams.changeType" placeholder="请选择" clearable>
              <el-option v-for="d in changeTypeOptions" :key="d.value" :label="d.label" :value="d.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="请选择" clearable>
              <el-option v-for="d in statusOptions" :key="d.value" :label="d.label" :value="d.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <el-form-item>
            <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['mes:pro:workorder:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate" v-hasPermi="['mes:pro:workorder:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:pro:workorder:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="changeList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="变更ID" align="center" prop="changeId" width="80" />
      <el-table-column label="工单ID" align="center" prop="workorderId" width="80" />
      <el-table-column label="变更类型" align="center" prop="changeType" width="100">
        <template #default="scope">
          <span>{{ changeTypeMap[scope.row.changeType] || scope.row.changeType }}</span>
        </template>
      </el-table-column>
      <el-table-column label="变更字段" align="center" prop="fieldName" :show-overflow-tooltip="true" />
      <el-table-column label="原值" align="center" prop="oldValue" :show-overflow-tooltip="true" />
      <el-table-column label="新值" align="center" prop="newValue" :show-overflow-tooltip="true" />
      <el-table-column label="变更原因" align="center" prop="changeReason" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'APPROVED' ? 'success' : scope.row.status === 'PENDING' ? 'warning' : 'info'" size="small">{{ statusMap[scope.row.status] || scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="审批人" align="center" prop="approver" width="80" />
      <el-table-column label="审批时间" align="center" prop="approveTime" width="150">
        <template #default="scope">
          <span v-if="scope.row.approveTime">{{ parseTime(scope.row.approveTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="125" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tooltip content="审批" placement="top" v-if="scope.row.status === 'PENDING'">
            <el-button link type="success" icon="Select" @click="handleApprove(scope.row)" v-hasPermi="['mes:pro:workorder:edit']"></el-button>
          </el-tooltip>
          <el-tooltip content="修改" placement="top" v-if="scope.row.status === 'PENDING'">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:pro:workorder:edit']"></el-button>
          </el-tooltip>
          <el-tooltip content="删除" placement="top" v-if="scope.row.status === 'PENDING'">
            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:pro:workorder:remove']"></el-button>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body @close="cancel">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="工单ID" prop="workorderId">
          <el-input-number v-model="form.workorderId" :min="1" style="width:100%" :disabled="optType === 'edit'" />
        </el-form-item>
        <el-form-item label="变更类型" prop="changeType">
          <el-select v-model="form.changeType" placeholder="请选择" style="width:100%">
            <el-option v-for="d in changeTypeOptions" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="变更字段" prop="fieldName">
          <el-input v-model="form.fieldName" placeholder="如：quantity、productId、requestDate" maxlength="64" />
        </el-form-item>
        <el-form-item label="原值" prop="oldValue">
          <el-input v-model="form.oldValue" placeholder="变更前的值" maxlength="255" />
        </el-form-item>
        <el-form-item label="新值" prop="newValue">
          <el-input v-model="form.newValue" placeholder="变更后的值" maxlength="255" />
        </el-form-item>
        <el-form-item label="变更原因" prop="changeReason">
          <el-input v-model="form.changeReason" type="textarea" :rows="2" placeholder="请输入变更原因" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { listWorkorderChange, getWorkorderChange, addWorkorderChange, updateWorkorderChange, delWorkorderChange, approveWorkorderChange } from '@/api/mes/pro/workorderchange'

export default {
  name: 'WorkorderChange',
  data() {
    return {
      optType: undefined,
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      changeList: [],
      title: '',
      open: false,
      // 状态映射
      statusMap: { PENDING: '待审批', APPROVED: '已审批', REJECTED: '已驳回' },
      statusOptions: [
        { label: '待审批', value: 'PENDING' },
        { label: '已审批', value: 'APPROVED' },
        { label: '已驳回', value: 'REJECTED' }
      ],
      changeTypeOptions: [
        { label: '数量变更', value: 'QUANTITY' },
        { label: 'BOM变更', value: 'BOM' },
        { label: '日期变更', value: 'DATE' },
        { label: '产品变更', value: 'PRODUCT' },
        { label: '工艺变更', value: 'ROUTE' },
        { label: '其他', value: 'OTHER' }
      ],
      changeTypeMap: {
        QUANTITY: '数量变更', BOM: 'BOM变更', DATE: '日期变更',
        PRODUCT: '产品变更', ROUTE: '工艺变更', OTHER: '其他'
      },
      queryParams: { pageNum: 1, pageSize: 10, workorderId: null, changeType: null, status: null },
      form: {},
      rules: {
        workorderId: [{ required: true, message: '工单ID不能为空', trigger: 'blur' }],
        changeType: [{ required: true, message: '请选择变更类型', trigger: 'change' }],
        fieldName: [{ required: true, message: '变更字段不能为空', trigger: 'blur' }],
        changeReason: [{ required: true, message: '变更原因不能为空', trigger: 'blur' }]
      }
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      listWorkorderChange(this.queryParams).then(r => {
        this.changeList = r.rows
        this.total = r.total
        this.loading = false
      })
    },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = {
        changeId: null, workorderId: null, changeType: null,
        fieldName: null, oldValue: null, newValue: null, changeReason: null
      }
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.$refs.queryForm?.resetFields(); this.handleQuery() },
    handleSelectionChange(sel) {
      this.ids = sel.map(i => i.changeId)
      this.single = sel.length !== 1
      this.multiple = !sel.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增变更申请'
      this.optType = 'add'
    },
    handleUpdate(row) {
      this.reset()
      const id = row.changeId || this.ids[0]
      getWorkorderChange(id).then(r => {
        this.form = r.data
        this.open = true
        this.title = '修改变更申请'
        this.optType = 'edit'
      })
    },
    handleDelete(row) {
      const ids = row.changeId || this.ids.join(',')
      this.$modal.confirm('确认删除变更记录"' + ids + '"？').then(() => {
        return delWorkorderChange(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    // 审批
    handleApprove(row) {
      this.$modal.confirm('确认审批通过变更"' + row.changeId + '"？').then(() => {
        return approveWorkorderChange(row.changeId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('审批成功')
      }).catch(() => {})
    },
    submitForm() {
      this.$refs.form.validate(v => {
        if (!v) return
        if (this.form.changeId) {
          updateWorkorderChange(this.form).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.open = false
            this.getList()
          }).catch(() => {})
        } else {
          addWorkorderChange(this.form).then(() => {
            this.$modal.msgSuccess('新增成功')
            this.open = false
            this.getList()
          }).catch(() => {})
        }
      })
    }
  }
}
</script>
