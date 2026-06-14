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
      <el-form-item label="排班日期" prop="shiftDate">
        <el-date-picker clearable
          v-model="queryParams.shiftDate"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择排班日期">
        </el-date-picker>
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
      <el-form-item label="班次ID(关联qxx_cal_shift)" prop="shiftId">
        <el-input
          v-model="queryParams.shiftId"
          placeholder="请输入班次ID(关联qxx_cal_shift)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="班次名称" prop="shiftName">
        <el-input
          v-model="queryParams.shiftName"
          placeholder="请输入班次名称"
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
          v-hasPermi="['mes:cal:teamshift:add']"
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
          v-hasPermi="['mes:cal:teamshift:edit']"
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
          v-hasPermi="['mes:cal:teamshift:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['mes:cal:teamshift:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="teamshiftList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="排班明细ID" align="center" prop="teamshiftId" />
      <el-table-column label="工厂ID(关联qxx_md_factory)" align="center" prop="factoryId" />
      <el-table-column label="排班日期" align="center" prop="shiftDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.shiftDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="班组ID(关联qxx_cal_team)" align="center" prop="teamId" />
      <el-table-column label="班组编码" align="center" prop="teamCode" />
      <el-table-column label="班组名称" align="center" prop="teamName" />
      <el-table-column label="班次ID(关联qxx_cal_shift)" align="center" prop="shiftId" />
      <el-table-column label="班次名称" align="center" prop="shiftName" />
      <el-table-column label="排班计划ID(关联qxx_cal_plan)" align="center" prop="planId" />
      <el-table-column label="排班计划编码" align="center" prop="planCode" />
      <el-table-column label="排班计划名称" align="center" prop="planName" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['mes:cal:teamshift:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['mes:cal:teamshift:remove']"
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

    <!-- 添加或修改班组排班明细对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="工厂ID(关联qxx_md_factory)" prop="factoryId">
              <el-input v-model="form.factoryId" placeholder="请输入工厂ID(关联qxx_md_factory)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="排班日期" prop="shiftDate">
              <el-date-picker clearable
                v-model="form.shiftDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择排班日期">
              </el-date-picker>
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
            <el-form-item label="班次ID(关联qxx_cal_shift)" prop="shiftId">
              <el-input v-model="form.shiftId" placeholder="请输入班次ID(关联qxx_cal_shift)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="班次名称" prop="shiftName">
              <el-input v-model="form.shiftName" placeholder="请输入班次名称" />
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
import { listTeamshift, getTeamshift, delTeamshift, addTeamshift, updateTeamshift } from "@/api/mes/cal/teamshift"

export default {
  name: "Teamshift",
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
      // 班组排班明细表格数据
      teamshiftList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        factoryId: null,
        shiftDate: null,
        teamId: null,
        teamCode: null,
        teamName: null,
        shiftId: null,
        shiftName: null,
        planId: null,
        planCode: null,
        planName: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        factoryId: [
          { required: true, message: "工厂ID(关联qxx_md_factory)不能为空", trigger: "blur" }
        ],
        shiftDate: [
          { required: true, message: "排班日期不能为空", trigger: "blur" }
        ],
        teamId: [
          { required: true, message: "班组ID(关联qxx_cal_team)不能为空", trigger: "blur" }
        ],
        shiftId: [
          { required: true, message: "班次ID(关联qxx_cal_shift)不能为空", trigger: "blur" }
        ],
        planId: [
          { required: true, message: "排班计划ID(关联qxx_cal_plan)不能为空", trigger: "blur" }
        ],
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询班组排班明细列表 */
    getList() {
      this.loading = true
      listTeamshift(this.queryParams).then(response => {
        this.teamshiftList = response.rows
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
        teamshiftId: null,
        factoryId: null,
        shiftDate: null,
        teamId: null,
        teamCode: null,
        teamName: null,
        shiftId: null,
        shiftName: null,
        planId: null,
        planCode: null,
        planName: null,
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
      this.ids = selection.map(item => item.teamshiftId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加班组排班明细"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const teamshiftId = row.teamshiftId || this.ids
      getTeamshift(teamshiftId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改班组排班明细"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.teamshiftId != null) {
            updateTeamshift(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addTeamshift(this.form).then(response => {
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
      const teamshiftIds = row.teamshiftId || this.ids
      this.$modal.confirm('是否确认删除班组排班明细编号为"' + teamshiftIds + '"的数据项？').then(function() {
        return delTeamshift(teamshiftIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('mes/cal/teamshift/export', {
        ...this.queryParams
      }, `teamshift_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
