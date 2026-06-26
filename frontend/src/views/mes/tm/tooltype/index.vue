<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="类型编码" prop="toolTypeCode">
        <el-input v-model="queryParams.toolTypeCode" placeholder="请输入类型编码" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="类型名称" prop="toolTypeName">
        <el-input v-model="queryParams.toolTypeName" placeholder="请输入类型名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="保养类型" prop="maintenType">
        <el-select v-model="queryParams.maintenType" placeholder="请选择保养类型" clearable>
          <el-option v-for="dict in maintenTypeOptions" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:tm:tooltype:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['mes:tm:tooltype:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:tm:tooltype:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:tm:tooltype:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="typeList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="类型编码" align="center" prop="toolTypeCode">
        <template #default="scope">
          <el-button type="text" @click="handleView(scope.row)" v-hasPermi="['mes:tm:tooltype:query']">{{ scope.row.toolTypeCode }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="类型名称" align="center" prop="toolTypeName" :show-overflow-tooltip="true" />
      <el-table-column label="是否编码管理" align="center" prop="needCodeFlag" width="110">
        <template #default="scope">
          <dict-tag :options="sys_yes_no_options" :value="scope.row.needCodeFlag" />
        </template>
      </el-table-column>
      <el-table-column label="保养类型" align="center" prop="maintenType" width="120">
        <template #default="scope">
          <span v-if="scope.row.needCodeFlag === '1'">{{ maintenTypeMap[scope.row.maintenType] || scope.row.maintenType }}</span>
          <span v-else>无</span>
        </template>
      </el-table-column>
      <el-table-column label="保养周期" align="center" prop="maintenCycle" width="110">
        <template #default="scope">
          <span v-if="scope.row.needCodeFlag === '1' && scope.row.maintenType && maintenTypePeriodUnit[scope.row.maintenType] && scope.row.maintenCycle != null && scope.row.maintenCycle > 0">
            {{ scope.row.maintenCycle + maintenTypePeriodUnit[scope.row.maintenType] }}
          </span>
          <span v-else>无</span>
        </template>
      </el-table-column>
      <el-table-column label="是否启用" align="center" prop="enableFlag" width="80">
        <template #default="scope">
          <dict-tag :options="sys_yes_no_options" :value="scope.row.enableFlag" />
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template #default="scope">
          <el-tooltip content="修改" placement="top"><el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:tm:tooltype:edit']"></el-button></el-tooltip>
          <el-tooltip content="删除" placement="top"><el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:tm:tooltype:remove']"></el-button></el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改工装夹具类型对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="8">
            <el-form-item label="类型编码" prop="toolTypeCode">
              <el-input v-model="form.toolTypeCode" placeholder="请输入类型编码" :disabled="optType === 'edit' || optType === 'view'" />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-switch v-model="autoGenFlag" active-color="#13ce66" active-text="自动生成" @change="handleAutoGenChange" v-if="optType === 'add'" />
          </el-col>
          <el-col :span="12">
            <el-form-item label="类型名称" prop="toolTypeName">
              <el-input v-model="form.toolTypeName" placeholder="请输入类型名称" :disabled="optType === 'view'" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="是否编码管理" prop="needCodeFlag">
              <el-radio-group v-model="form.needCodeFlag" :disabled="optType === 'view'">
                <el-radio v-for="dict in sys_yes_no_options" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8" v-if="form.needCodeFlag === '1'">
            <el-form-item label="保养类型" prop="maintenType">
              <el-select v-model="form.maintenType" placeholder="请选择保养类型" :disabled="optType === 'view'">
                <el-option v-for="dict in maintenTypeOptions" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8" v-if="form.needCodeFlag === '1'">
            <el-form-item label="保养周期" prop="maintenCycle">
              <el-input v-model="form.maintenCycle" placeholder="请输入保养周期" :disabled="optType === 'view'" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="是否启用" prop="enableFlag">
              <el-radio-group v-model="form.enableFlag" :disabled="optType === 'view'">
                <el-radio v-for="dict in sys_yes_no_options" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="16">
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
import { listType, getType, delType, addType, updateType } from '@/api/mes/tm/tooltype'
import { genSerialCode } from '@/api/mes/sys/autocoderule'

export default {
  name: 'ToolType',
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
      maintenTypePeriodUnit: {
        DAY: '天', WEEK: '周', MONTH: '月', QUARTER: '季',
        HALFYEAR: '半年', YEAR: '年', USAGE: '次',
      },
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
      // 工装夹具类型表格数据
      typeList: [],
      // 弹出层标题
      title: '',
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        toolTypeCode: null,
        toolTypeName: null,
        maintenType: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        toolTypeCode: [
          { required: true, message: '类型编码不能为空', trigger: 'blur' },
          { max: 64, message: '字段过长', trigger: 'blur' },
        ],
        toolTypeName: [
          { required: true, message: '类型名称不能为空', trigger: 'blur' },
          { max: 100, message: '字段过长', trigger: 'blur' },
        ],
        needCodeFlag: [{ required: true, message: '是否编码管理不能为空', trigger: 'change' }],
        enableFlag: [{ required: true, message: '是否启用不能为空', trigger: 'change' }],
        maintenCycle: [{ pattern: /^[1-9]\d*$/, message: '必须为正整数', trigger: 'blur' }],
      },
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listType(this.queryParams).then(response => {
        this.typeList = response.rows || []
        this.total = response.total
        this.loading = false
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        toolTypeId: null,
        toolTypeCode: null,
        toolTypeName: null,
        needCodeFlag: '1',
        maintenType: null,
        maintenCycle: null,
        enableFlag: '1',
        remark: null,
      }
      this.autoGenFlag = false
      this.$refs.form?.resetFields()
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
      this.ids = selection.map(item => item.toolTypeId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '添加工装夹具类型'
      this.optType = 'add'
    },
    handleView(row) {
      this.reset()
      const toolTypeId = row.toolTypeId || this.ids[0]
      getType(toolTypeId).then(response => {
        this.form = response.data
        this.open = true
        this.title = '查看工装夹具类型'
        this.optType = 'view'
      })
    },
    handleUpdate(row) {
      this.reset()
      const toolTypeId = row.toolTypeId || this.ids[0]
      getType(toolTypeId).then(response => {
        this.form = response.data
        this.open = true
        this.title = '修改工装夹具类型'
        this.optType = 'edit'
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          if (this.form.toolTypeId != null) {
            updateType(this.form).then(() => {
              this.$modal.msgSuccess('修改成功')
              this.open = false
              this.getList()
            })
          } else {
            addType(this.form).then(() => {
              this.$modal.msgSuccess('新增成功')
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const toolTypeIds = row.toolTypeId || this.ids.join(',')
      this.$modal.confirm('是否确认删除工装夹具类型编号为"' + toolTypeIds + '"的数据项？').then(() => {
        return delType(toolTypeIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleExport() {
      this.download('mes/tm/tooltype/export', { ...this.queryParams }, `tooltype_${new Date().getTime()}.xlsx`)
    },
    handleAutoGenChange(autoGenFlag) {
      if (autoGenFlag) {
        genSerialCode('TOOL_TYPE_CODE').then(response => {
          this.form.toolTypeCode = response.data
        })
      } else {
        this.form.toolTypeCode = null
      }
    },
  },
}
</script>
