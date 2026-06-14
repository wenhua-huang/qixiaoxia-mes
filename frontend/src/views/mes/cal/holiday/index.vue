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
      <el-form-item label="日期" prop="holidayDate">
        <el-date-picker clearable
          v-model="queryParams.holidayDate"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="节假日名称" prop="holidayName">
        <el-input
          v-model="queryParams.holidayName"
          placeholder="请输入节假日名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否启用(1-是,0-否)" prop="enableFlag">
        <el-input
          v-model="queryParams.enableFlag"
          placeholder="请输入是否启用(1-是,0-否)"
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
          v-hasPermi="['mes:cal:holiday:add']"
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
          v-hasPermi="['mes:cal:holiday:edit']"
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
          v-hasPermi="['mes:cal:holiday:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['mes:cal:holiday:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="holidayList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="节假日设置ID" align="center" prop="holidayId" />
      <el-table-column label="工厂ID(关联qxx_md_factory)" align="center" prop="factoryId" />
      <el-table-column label="日期" align="center" prop="holidayDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.holidayDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="节假日名称" align="center" prop="holidayName" />
      <el-table-column label="类型:HOLIDAY-节假日,WORKDAY-调休工作日" align="center" prop="holidayType" />
      <el-table-column label="是否启用(1-是,0-否)" align="center" prop="enableFlag" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['mes:cal:holiday:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['mes:cal:holiday:remove']"
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

    <!-- 添加或修改节假日设置对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="工厂ID(关联qxx_md_factory)" prop="factoryId">
              <el-input v-model="form.factoryId" placeholder="请输入工厂ID(关联qxx_md_factory)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="日期" prop="holidayDate">
              <el-date-picker clearable
                v-model="form.holidayDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择日期">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="节假日名称" prop="holidayName">
              <el-input v-model="form.holidayName" placeholder="请输入节假日名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="是否启用(1-是,0-否)" prop="enableFlag">
              <el-input v-model="form.enableFlag" placeholder="请输入是否启用(1-是,0-否)" />
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
import { listHoliday, getHoliday, delHoliday, addHoliday, updateHoliday } from "@/api/mes/cal/holiday"

export default {
  name: "Holiday",
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
      // 节假日设置表格数据
      holidayList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        factoryId: null,
        holidayDate: null,
        holidayName: null,
        holidayType: null,
        enableFlag: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        factoryId: [
          { required: true, message: "工厂ID(关联qxx_md_factory)不能为空", trigger: "blur" }
        ],
        holidayDate: [
          { required: true, message: "日期不能为空", trigger: "blur" }
        ],
        holidayName: [
          { required: true, message: "节假日名称不能为空", trigger: "blur" }
        ],
        holidayType: [
          { required: true, message: "类型:HOLIDAY-节假日,WORKDAY-调休工作日不能为空", trigger: "change" }
        ],
        enableFlag: [
          { required: true, message: "是否启用(1-是,0-否)不能为空", trigger: "blur" }
        ],
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询节假日设置列表 */
    getList() {
      this.loading = true
      listHoliday(this.queryParams).then(response => {
        this.holidayList = response.rows
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
        holidayId: null,
        factoryId: null,
        holidayDate: null,
        holidayName: null,
        holidayType: null,
        enableFlag: null,
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
      this.ids = selection.map(item => item.holidayId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加节假日设置"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const holidayId = row.holidayId || this.ids
      getHoliday(holidayId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改节假日设置"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.holidayId != null) {
            updateHoliday(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addHoliday(this.form).then(response => {
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
      const holidayIds = row.holidayId || this.ids
      this.$modal.confirm('是否确认删除节假日设置编号为"' + holidayIds + '"的数据项？').then(function() {
        return delHoliday(holidayIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('mes/cal/holiday/export', {
        ...this.queryParams
      }, `holiday_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
