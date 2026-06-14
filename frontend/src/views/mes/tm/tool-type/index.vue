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
      <el-form-item label="类型编码(唯一)" prop="toolTypeCode">
        <el-input
          v-model="queryParams.toolTypeCode"
          placeholder="请输入类型编码(唯一)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="类型名称" prop="toolTypeName">
        <el-input
          v-model="queryParams.toolTypeName"
          placeholder="请输入类型名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否需要编码(1-需要,0-不需要)" prop="needCodeFlag">
        <el-input
          v-model="queryParams.needCodeFlag"
          placeholder="请输入是否需要编码(1-需要,0-不需要)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="保养周期(与保养类型配合,如:月+3=每3个月保养一次)" prop="maintenCycle">
        <el-input
          v-model="queryParams.maintenCycle"
          placeholder="请输入保养周期(与保养类型配合,如:月+3=每3个月保养一次)"
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
          v-hasPermi="['mes:tm:tool-type:add']"
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
          v-hasPermi="['mes:tm:tool-type:edit']"
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
          v-hasPermi="['mes:tm:tool-type:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['mes:tm:tool-type:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="typeList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="工装夹具类型ID" align="center" prop="toolTypeId" />
      <el-table-column label="工厂ID(关联qxx_md_factory)" align="center" prop="factoryId" />
      <el-table-column label="类型编码(唯一)" align="center" prop="toolTypeCode" />
      <el-table-column label="类型名称" align="center" prop="toolTypeName" />
      <el-table-column label="是否需要编码(1-需要,0-不需要)" align="center" prop="needCodeFlag" />
      <el-table-column label="保养类型:DAY-每天,WEEK-每周,MONTH-每月,QUARTER-每季,HALFYEAR-每半年,YEAR-每年,USAGE-按使用次数" align="center" prop="maintenType" />
      <el-table-column label="保养周期(与保养类型配合,如:月+3=每3个月保养一次)" align="center" prop="maintenCycle" />
      <el-table-column label="是否启用(1-是,0-否)" align="center" prop="enableFlag" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['mes:tm:tool-type:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['mes:tm:tool-type:remove']"
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

    <!-- 添加或修改工装夹具类型对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="工厂ID(关联qxx_md_factory)" prop="factoryId">
              <el-input v-model="form.factoryId" placeholder="请输入工厂ID(关联qxx_md_factory)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="类型编码(唯一)" prop="toolTypeCode">
              <el-input v-model="form.toolTypeCode" placeholder="请输入类型编码(唯一)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="类型名称" prop="toolTypeName">
              <el-input v-model="form.toolTypeName" placeholder="请输入类型名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="是否需要编码(1-需要,0-不需要)" prop="needCodeFlag">
              <el-input v-model="form.needCodeFlag" placeholder="请输入是否需要编码(1-需要,0-不需要)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="保养周期(与保养类型配合,如:月+3=每3个月保养一次)" prop="maintenCycle">
              <el-input v-model="form.maintenCycle" placeholder="请输入保养周期(与保养类型配合,如:月+3=每3个月保养一次)" />
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
import { listType, getType, delType, addType, updateType } from "@/api/mes/tm/tool-type"

export default {
  name: "Type",
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
      // 工装夹具类型表格数据
      typeList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        factoryId: null,
        toolTypeCode: null,
        toolTypeName: null,
        needCodeFlag: null,
        maintenType: null,
        maintenCycle: null,
        enableFlag: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        factoryId: [
          { required: true, message: "工厂ID(关联qxx_md_factory)不能为空", trigger: "blur" }
        ],
        toolTypeCode: [
          { required: true, message: "类型编码(唯一)不能为空", trigger: "blur" }
        ],
        toolTypeName: [
          { required: true, message: "类型名称不能为空", trigger: "blur" }
        ],
        needCodeFlag: [
          { required: true, message: "是否需要编码(1-需要,0-不需要)不能为空", trigger: "blur" }
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
    /** 查询工装夹具类型列表 */
    getList() {
      this.loading = true
      listType(this.queryParams).then(response => {
        this.typeList = response.rows
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
        toolTypeId: null,
        factoryId: null,
        toolTypeCode: null,
        toolTypeName: null,
        needCodeFlag: null,
        maintenType: null,
        maintenCycle: null,
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
      this.ids = selection.map(item => item.toolTypeId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加工装夹具类型"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const toolTypeId = row.toolTypeId || this.ids
      getType(toolTypeId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改工装夹具类型"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.toolTypeId != null) {
            updateType(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addType(this.form).then(response => {
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
      const toolTypeIds = row.toolTypeId || this.ids
      this.$modal.confirm('是否确认删除工装夹具类型编号为"' + toolTypeIds + '"的数据项？').then(function() {
        return delType(toolTypeIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('mes/tm/tool-type/export', {
        ...this.queryParams
      }, `type_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
