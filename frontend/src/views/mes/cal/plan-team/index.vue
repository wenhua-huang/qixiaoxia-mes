<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="工厂ID(关联qxx_md_factory)" prop="factoryId">
        <el-input
          v-model="queryParams.factoryId"
          placeholder="请输入工厂ID(关联qxx_md_factory)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="排班计划ID(关联qxx_cal_plan)" prop="planId">
        <el-input
          v-model="queryParams.planId"
          placeholder="请输入排班计划ID(关联qxx_cal_plan)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="排班计划编码" prop="planCode">
        <el-input
          v-model="queryParams.planCode"
          placeholder="请输入排班计划编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="排班计划名称" prop="planName">
        <el-input
          v-model="queryParams.planName"
          placeholder="请输入排班计划名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="班组ID(关联qxx_cal_team)" prop="teamId">
        <el-input
          v-model="queryParams.teamId"
          placeholder="请输入班组ID(关联qxx_cal_team)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="班组编码" prop="teamCode">
        <el-input
          v-model="queryParams.teamCode"
          placeholder="请输入班组编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="班组名称" prop="teamName">
        <el-input
          v-model="queryParams.teamName"
          placeholder="请输入班组名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['mes:cal:team:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mes:cal:team:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mes:cal:team:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['mes:cal:team:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="teamList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="关联记录ID" align="center" prop="recordId" />
      <el-table-column label="工厂ID(关联qxx_md_factory)" align="center" prop="factoryId" />
      <el-table-column label="排班计划ID(关联qxx_cal_plan)" align="center" prop="planId" />
      <el-table-column label="排班计划编码" align="center" prop="planCode" />
      <el-table-column label="排班计划名称" align="center" prop="planName" />
      <el-table-column label="班组ID(关联qxx_cal_team)" align="center" prop="teamId" />
      <el-table-column label="班组编码" align="center" prop="teamCode" />
      <el-table-column label="班组名称" align="center" prop="teamName" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['mes:cal:team:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['mes:cal:team:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      v-model:current-page="queryParams.pageNum"
      v-model:page-size="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改计划班组关联对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="工厂ID(关联qxx_md_factory)" prop="factoryId">
              <el-input v-model="form.factoryId" placeholder="请输入工厂ID(关联qxx_md_factory)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="排班计划ID(关联qxx_cal_plan)" prop="planId">
              <el-input v-model="form.planId" placeholder="请输入排班计划ID(关联qxx_cal_plan)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="排班计划编码" prop="planCode">
              <el-input v-model="form.planCode" placeholder="请输入排班计划编码" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="排班计划名称" prop="planName">
              <el-input v-model="form.planName" placeholder="请输入排班计划名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="班组ID(关联qxx_cal_team)" prop="teamId">
              <el-input v-model="form.teamId" placeholder="请输入班组ID(关联qxx_cal_team)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="班组编码" prop="teamCode">
              <el-input v-model="form.teamCode" placeholder="请输入班组编码" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="班组名称" prop="teamName">
              <el-input v-model="form.teamName" placeholder="请输入班组名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listTeam, getTeam, delTeam, addTeam, updateTeam } from "@/api/mes/cal/plan-team"

export default {
  name: "Team",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 计划班组关联表格数据
      teamList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        factoryId: null,
        planId: null,
        planCode: null,
        planName: null,
        teamId: null,
        teamCode: null,
        teamName: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        factoryId: [
          { required: true, message: "工厂ID(关联qxx_md_factory)不能为空", trigger: "blur" }
        ],
        planId: [
          { required: true, message: "排班计划ID(关联qxx_cal_plan)不能为空", trigger: "blur" }
        ],
        teamId: [
          { required: true, message: "班组ID(关联qxx_cal_team)不能为空", trigger: "blur" }
        ],
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询计划班组关联列表 */
    getList() {
      this.loading = true
      listTeam(this.queryParams).then(response => {
        this.teamList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置
    reset() {
      this.form = {
        recordId: null,
        factoryId: null,
        planId: null,
        planCode: null,
        planName: null,
        teamId: null,
        teamCode: null,
        teamName: null,
        remark: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null
      }
      this.resetForm("form")
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.recordId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加计划班组关联"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const recordId = row.recordId || this.ids
      getTeam(recordId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改计划班组关联"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.recordId != null) {
            updateTeam(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addTeam(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const recordIds = row.recordId || this.ids
      this.$modal.confirm('是否确认删除计划班组关联编号为"' + recordIds + '"的数据项？').then(function() {
        return delTeam(recordIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('mes/cal/team/export', {
        ...this.queryParams
      }, `team_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
