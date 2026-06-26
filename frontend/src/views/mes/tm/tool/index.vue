<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-row>
        <el-col :span="8">
          <el-form-item label="夹具编码" prop="toolCode">
            <el-input v-model="queryParams.toolCode" placeholder="请输入工装夹具编码" clearable @keyup.enter="handleQuery" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="夹具名称" prop="toolName">
            <el-input v-model="queryParams.toolName" placeholder="请输入工装夹具名称" clearable @keyup.enter="handleQuery" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="夹具类型" prop="toolTypeId">
            <el-select v-model="queryParams.toolTypeId" placeholder="请选择类型" clearable>
              <el-option v-for="dict in toolTypeOptions" :key="dict.toolTypeId" :label="dict.toolTypeName" :value="dict.toolTypeId" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="6">
          <el-form-item label="品牌" prop="brand" label-width="60px">
            <el-input v-model="queryParams.brand" placeholder="请输入品牌" clearable @keyup.enter="handleQuery" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="规格" prop="spec" label-width="60px">
            <el-input v-model="queryParams.spec" placeholder="请输入规格" clearable @keyup.enter="handleQuery" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="状态" prop="status" label-width="60px">
            <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
              <el-option v-for="dict in statusOptions" :key="dict.value" :label="dict.label" :value="dict.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:tm:tool:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['mes:tm:tool:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <!-- 删除按钮已移除：用启停用(enableFlag)替代物理删除 -->
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:tm:tool:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="toolList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="编码" align="center" prop="toolCode" />
      <el-table-column label="名称" align="center" prop="toolName" :show-overflow-tooltip="true">
        <template #default="scope">
          <el-button type="text" @click="handleView(scope.row)" v-hasPermi="['mes:tm:tool:query']">{{ scope.row.toolName }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="品牌" align="center" prop="brand" :show-overflow-tooltip="true" />
      <el-table-column label="规格" align="center" prop="spec" :show-overflow-tooltip="true" />
      <el-table-column label="类型" align="center" prop="toolTypeName" />
      <el-table-column label="总数量" align="center" prop="quantity" width="80" />
      <el-table-column label="可用数量" align="center" prop="availableQuantity" width="80" />
      <el-table-column label="保养类型" align="center" prop="maintenType" width="120">
        <template #default="scope">
          <span v-if="scope.row.maintenType">{{ maintenTypeMap[scope.row.maintenType] || scope.row.maintenType }}</span>
          <span v-else>无</span>
        </template>
      </el-table-column>
      <el-table-column label="下次保养日期" align="center" prop="nextMaintenDate" width="120">
        <template #default="scope">
          <span>{{ parseTime(scope.row.nextMaintenDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <dict-tag :options="statusOptions" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template #default="scope">
          <el-tooltip content="修改" placement="top"><el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:tm:tool:edit']"></el-button></el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改工装夹具清单对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="16">
            <el-form-item label="工装夹具编码" prop="toolCode">
              <el-input v-model="form.toolCode" placeholder="请输入工装夹具编码" :disabled="optType === 'edit' || optType === 'view'" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label-width="80">
              <el-switch v-model="autoGenFlag" active-color="#13ce66" active-text="自动生成" @change="handleAutoGenChange" v-if="optType === 'add'" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="工装夹具名称" prop="toolName">
              <el-input v-model="form.toolName" placeholder="请输入工装夹具名称" :disabled="optType === 'view'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工装夹具类型" prop="toolTypeId">
              <el-select v-model="form.toolTypeId" style="width:100%" placeholder="请选择类型" @change="onToolTypeChanged" :disabled="optType === 'view'">
                <el-option v-for="dict in toolTypeOptions" :key="dict.toolTypeId" :label="dict.toolTypeName" :value="dict.toolTypeId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="品牌" prop="brand">
              <el-input v-model="form.brand" placeholder="请输入品牌" :disabled="optType === 'view'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规格型号" prop="spec">
              <el-input v-model="form.spec" placeholder="请输入规格型号" :disabled="optType === 'view'" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="总数量" prop="quantity">
              <el-input-number v-model="form.quantity" :min="0" :max="99999999" :disabled="form._codeFlag === '1' || optType === 'view'" @change="onQuantityChanged" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="可用数量" prop="availableQuantity">
              <el-input-number v-model="form.availableQuantity" :min="0" :max="99999999" :disabled="form._codeFlag === '1' || optType === 'view'" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="保养类型" prop="maintenType">
              <el-select v-model="form.maintenType" placeholder="请选择保养类型" :disabled="optType === 'view'" style="width:100%">
                <el-option v-for="dict in maintenTypeOptions" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="下次保养日期" prop="nextMaintenDate">
              <el-date-picker v-model="form.nextMaintenDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择下次保养日期" :disabled="optType === 'view'" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" placeholder="请选择状态" :disabled="optType === 'view'" style="width:100%">
                <el-option v-for="dict in statusOptions" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否启用" prop="enableFlag">
              <el-radio-group v-model="form.enableFlag" :disabled="optType === 'view'">
                <el-radio v-for="dict in sys_yes_no_options" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" :disabled="optType === 'view'" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm" v-if="optType !== 'view'">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { listTool, getTool, delTool, addTool, updateTool } from '@/api/mes/tm/tool'
import { listAllType } from '@/api/mes/tm/tooltype'
import { genSerialCode } from '@/api/mes/sys/autocoderule'

export default {
  name: 'Tool',
  data() {
    return {
      // 保养类型中文映射
      maintenTypeOptions: [
        { label: '每天', value: 'DAY' },
        { label: '每周', value: 'WEEK' },
        { label: '每月', value: 'MONTH' },
        { label: '每季', value: 'QUARTER' },
        { label: '每半年', value: 'HALFYEAR' },
        { label: '每年', value: 'YEAR' },
        { label: '按使用次数', value: 'USAGE' },
      ],
      maintenTypeMap: {
        DAY: '每天', WEEK: '每周', MONTH: '每月', QUARTER: '每季',
        HALFYEAR: '每半年', YEAR: '每年', USAGE: '按使用次数',
      },
      statusOptions: [
        { label: '在库', value: 'STORE' },
        { label: '使用中', value: 'USING' },
        { label: '保养中', value: 'MAINTENANCE' },
        { label: '报废', value: 'SCRAPPED' },
      ],
      sys_yes_no_options: [
        { label: '是', value: '1' },
        { label: '否', value: '0' },
      ],
      // 自动生成编码
      autoGenFlag: false,
      optType: undefined,
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
      // 类型下拉数据
      toolTypeOptions: [],
      // 弹出层标题
      title: '',
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        toolCode: null,
        toolName: null,
        brand: null,
        spec: null,
        toolTypeId: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        toolCode: [
          { required: true, message: '工装夹具编码不能为空', trigger: 'blur' },
          { max: 64, message: '字段过长', trigger: 'blur' },
        ],
        toolName: [
          { required: true, message: '工装夹具名称不能为空', trigger: 'blur' },
          { max: 100, message: '字段过长', trigger: 'blur' },
        ],
        toolTypeId: [{ required: true, message: '工装夹具类型不能为空', trigger: 'blur' }],
        quantity: [{ required: true, message: '总数量不能为空', trigger: 'blur' }],
        spec: [{ max: 100, message: '字段过长', trigger: 'blur' }],
        brand: [{ max: 100, message: '字段过长', trigger: 'blur' }],
      },
    }
  },
  created() {
    this.getList()
    this.getTypeList()
  },
  methods: {
    getList() {
      this.loading = true
      listTool(this.queryParams).then(response => {
        this.toolList = response.rows || []
        this.total = response.total
        this.loading = false
      })
    },
    getTypeList() {
      listAllType().then(response => {
        this.toolTypeOptions = response.data || []
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        toolId: null,
        toolCode: null,
        toolName: null,
        brand: null,
        spec: null,
        toolTypeId: null,
        toolTypeCode: null,
        toolTypeName: null,
        quantity: 0,
        availableQuantity: 0,
        maintenType: null,
        nextMaintenDate: null,
        status: 'STORE',
        enableFlag: '1',
        remark: null,
        _codeFlag: '0',
      }
      this.autoGenFlag = false
      this.$refs.form?.resetFields()
    },
    onToolTypeChanged() {
      const types = this.toolTypeOptions.filter(item => item.toolTypeId === this.form.toolTypeId)
      if (types && types.length > 0) {
        const t = types[0]
        this.form.toolTypeCode = t.toolTypeCode
        this.form.toolTypeName = t.toolTypeName
        this.form.maintenType = t.maintenType
        if (t.needCodeFlag === '1') {
          // 独立编码管理：数量固定为1
          this.form.quantity = 1
          this.form.availableQuantity = 1
          this.form._codeFlag = '1'
        } else {
          this.form._codeFlag = '0'
        }
      }
    },
    onQuantityChanged() {
      if (this.form._codeFlag !== '1') {
        this.form.availableQuantity = this.form.quantity
      }
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.$refs.queryForm?.resetFields()
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.toolId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '添加工装夹具清单'
      this.optType = 'add'
    },
    handleView(row) {
      this.reset()
      const toolId = row.toolId || this.ids[0]
      getTool(toolId).then(response => {
        this.form = response.data
        this.open = true
        this.title = '查看工装夹具信息'
        this.optType = 'view'
      })
    },
    handleUpdate(row) {
      this.reset()
      const toolId = row.toolId || this.ids[0]
      getTool(toolId).then(response => {
        this.form = response.data
        this.open = true
        this.title = '修改工装夹具清单'
        this.optType = 'edit'
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          if (this.form.toolId != null) {
            updateTool(this.form).then(() => {
              this.$modal.msgSuccess('修改成功')
              this.open = false
              this.getList()
            })
          } else {
            addTool(this.form).then(() => {
              this.$modal.msgSuccess('新增成功')
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const toolIds = row.toolId || this.ids.join(',')
      this.$modal.confirm('是否确认删除工装夹具清单编号为"' + toolIds + '"的数据项？').then(() => {
        return delTool(toolIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleExport() {
      this.download('mes/tm/tool/export', { ...this.queryParams }, `tool_${new Date().getTime()}.xlsx`)
    },
    handleAutoGenChange(autoGenFlag) {
      if (autoGenFlag) {
        genSerialCode('TOOL_CODE').then(response => {
          this.form.toolCode = response.data
        })
      } else {
        this.form.toolCode = null
      }
    },
  },
}
</script>
