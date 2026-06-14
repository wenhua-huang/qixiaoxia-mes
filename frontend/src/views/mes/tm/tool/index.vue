<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="工厂" prop="factoryId">
        <el-input
          v-model="queryParams.factoryId"
          placeholder="请输入工厂ID(关联qxx_md_factory)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="编码" prop="toolCode">
        <el-input
          v-model="queryParams.toolCode"
          placeholder="请输入工装夹具编码(唯一)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="名称" prop="toolName">
        <el-input
          v-model="queryParams.toolName"
          placeholder="请输入工装夹具名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="品牌" prop="brand">
        <el-input
          v-model="queryParams.brand"
          placeholder="请输入品牌"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="类型ID" prop="toolTypeId">
        <el-input
          v-model="queryParams.toolTypeId"
          placeholder="请输入工装夹具类型ID(关联qxx_tm_tool_type)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="类型编码" prop="toolTypeCode">
        <el-input
          v-model="queryParams.toolTypeCode"
          placeholder="请输入工装夹具类型编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="类型名称" prop="toolTypeName">
        <el-input
          v-model="queryParams.toolTypeName"
          placeholder="请输入工装夹具类型名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="总数量" prop="quantity">
        <el-input
          v-model="queryParams.quantity"
          placeholder="请输入总数量"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="可用数量" prop="availableQuantity">
        <el-input
          v-model="queryParams.availableQuantity"
          placeholder="请输入可用数量(总数量减去使用中和保养中的数量)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="下次保养" prop="nextMaintenDate">
        <el-date-picker clearable
          v-model="queryParams.nextMaintenDate"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择下次保养日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="启用" prop="enableFlag">
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
          v-hasPermi="['mes:tm:tool:add']"
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
          v-hasPermi="['mes:tm:tool:edit']"
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
          v-hasPermi="['mes:tm:tool:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['mes:tm:tool:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="toolList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="toolId" />
      <el-table-column label="工厂" align="center" prop="factoryId" />
      <el-table-column label="编码" align="center" prop="toolCode" />
      <el-table-column label="名称" align="center" prop="toolName" />
      <el-table-column label="品牌" align="center" prop="brand" />
      <el-table-column label="规格" align="center" prop="spec" />
      <el-table-column label="类型ID" align="center" prop="toolTypeId" />
      <el-table-column label="类型编码" align="center" prop="toolTypeCode" />
      <el-table-column label="类型名称" align="center" prop="toolTypeName" />
      <el-table-column label="总数量" align="center" prop="quantity" />
      <el-table-column label="可用数量" align="center" prop="availableQuantity" />
      <el-table-column label="保养类型" align="center" prop="maintenType" width="120">
          <template #default="scope">
            <dict-tag :options="maintenTypeOptions" :value="scope.row.maintenType" />
          </template>
        </el-table-column>
      <el-table-column label="下次保养" align="center" prop="nextMaintenDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.nextMaintenDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
          <template #default="scope">
            <dict-tag :options="statusOptions" :value="scope.row.status" />
          </template>
        </el-table-column>
      <el-table-column label="启用" align="center" prop="enableFlag" width="80">
          <template #default="scope">
            <dict-tag :options="sys_yes_no" :value="scope.row.enableFlag" />
          </template>
        </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['mes:tm:tool:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['mes:tm:tool:remove']"
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

    <!-- 添加或修改工装夹具清单对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="工厂" prop="factoryId">
              <el-input v-model="form.factoryId" placeholder="请输入工厂ID(关联qxx_md_factory)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="编码" prop="toolCode">
              <el-input v-model="form.toolCode" placeholder="请输入工装夹具编码(唯一)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="名称" prop="toolName">
              <el-input v-model="form.toolName" placeholder="请输入工装夹具名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="品牌" prop="brand">
              <el-input v-model="form.brand" placeholder="请输入品牌" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="规格" prop="spec">
              <el-input v-model="form.spec" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="类型ID" prop="toolTypeId">
              <el-input v-model="form.toolTypeId" placeholder="请输入工装夹具类型ID(关联qxx_tm_tool_type)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="类型编码" prop="toolTypeCode">
              <el-input v-model="form.toolTypeCode" placeholder="请输入工装夹具类型编码" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="类型名称" prop="toolTypeName">
              <el-input v-model="form.toolTypeName" placeholder="请输入工装夹具类型名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="总数量" prop="quantity">
              <el-input v-model="form.quantity" placeholder="请输入总数量" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="可用数量" prop="availableQuantity">
              <el-input v-model="form.availableQuantity" placeholder="请输入可用数量(总数量减去使用中和保养中的数量)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="下次保养" prop="nextMaintenDate">
              <el-date-picker clearable
                v-model="form.nextMaintenDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择下次保养日期">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="启用" prop="enableFlag">
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
import { listTool, getTool, delTool, addTool, updateTool } from "@/api/mes/tm/tool"

export default {
  name: "Tool",
  data() {
    return {
      maintenTypeMap: { DAY: "每天", WEEK: "每周", MONTH: "每月", QUARTER: "每季", HALFYEAR: "每半年", YEAR: "每年", USAGE: "按使用次数" },
      statusMap: { STORE: "在库", USING: "使用中", MAINTENANCE: "保养中", SCRAPPED: "报废" },
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
      // 工装夹具清单表格数据
      toolList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        factoryId: null,
        toolCode: null,
        toolName: null,
        brand: null,
        spec: null,
        toolTypeId: null,
        toolTypeCode: null,
        toolTypeName: null,
        quantity: null,
        availableQuantity: null,
        maintenType: null,
        nextMaintenDate: null,
        status: null,
        enableFlag: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        factoryId: [
          { required: true, message: "工厂ID(关联qxx_md_factory)不能为空", trigger: "blur" }
        ],
        toolCode: [
          { required: true, message: "工装夹具编码(唯一)不能为空", trigger: "blur" }
        ],
        toolName: [
          { required: true, message: "工装夹具名称不能为空", trigger: "blur" }
        ],
        toolTypeId: [
          { required: true, message: "工装夹具类型ID(关联qxx_tm_tool_type)不能为空", trigger: "blur" }
        ],
        status: [
          { required: true, message: "状态:STORE-在库,USING-使用中,MAINTENANCE-保养中,SCRAPPED-报废不能为空", trigger: "change" }
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
    /** 查询工装夹具清单列表 */
    getList() {
      this.loading = true
      listTool(this.queryParams).then(response => {
        this.toolList = response.rows
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
        toolId: null,
        factoryId: null,
        toolCode: null,
        toolName: null,
        brand: null,
        spec: null,
        toolTypeId: null,
        toolTypeCode: null,
        toolTypeName: null,
        quantity: null,
        availableQuantity: null,
        maintenType: null,
        nextMaintenDate: null,
        status: null,
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
      this.ids = selection.map(item => item.toolId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加工装夹具清单"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const toolId = row.toolId || this.ids
      getTool(toolId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改工装夹具清单"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.toolId != null) {
            updateTool(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addTool(this.form).then(response => {
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
      const toolIds = row.toolId || this.ids
      this.$modal.confirm('是否确认删除工装夹具清单编号为"' + toolIds + '"的数据项？').then(function() {
        return delTool(toolIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('mes/tm/tool/export', {
        ...this.queryParams
      }, `tool_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
