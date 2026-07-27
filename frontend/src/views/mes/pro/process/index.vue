<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-row>
        <el-col :span="8">
          <el-form-item label="工序编码" prop="processCode">
            <el-input v-model="queryParams.processCode" placeholder="请输入" clearable @keyup.enter="handleQuery" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="工序名称" prop="processName">
            <el-input v-model="queryParams.processName" placeholder="请输入" clearable @keyup.enter="handleQuery" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="是否启用" prop="enableFlag">
            <el-select v-model="queryParams.enableFlag" placeholder="请选择" clearable>
              <el-option v-for="d in yesNoOptions" :key="d.value" :label="d.label" :value="d.value" />
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
        <el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['mes:pro:process:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate" v-hasPermi="['mes:pro:process:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <!-- 删除按钮已移除：用启停用(enableFlag)替代物理删除 -->
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" size="small" @click="handleExport" v-hasPermi="['mes:pro:process:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="processList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="工序编码" align="center" prop="processCode" width="130" />
      <el-table-column label="工序名称" align="center" prop="processName" :show-overflow-tooltip="true">
        <template #default="scope">
          <el-button link @click="handleView(scope.row)" v-hasPermi="['mes:pro:process:query']">{{ scope.row.processName }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="工序类型" align="center" prop="processType" width="100">
        <template #default="scope">
          <span>{{ processTypeMap[scope.row.processType] || scope.row.processType }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="启用" align="center" width="70">
        <template #default="scope">
          <el-switch v-model="scope.row.enableFlag" active-value="1" inactive-value="0" @change="handleEnableChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="80" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tooltip content="修改" placement="top"><el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:pro:process:edit']"></el-button></el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑/查看对话框 -->
    <el-dialog :title="title" v-model="open" width="960px" append-to-body @close="cancel">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="16">
            <el-form-item label="工序编码" prop="processCode">
              <el-input v-model="form.processCode" placeholder="请输入" :disabled="optType === 'edit' || optType === 'view'" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label-width="80">
              <el-switch v-model="autoGenFlag" active-color="#13ce66" @change="handleAutoGenChange" v-if="optType === 'add'" />
              <span style="margin-left:6px;font-size:12px;color:#909399">自动生成</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="工序名称" prop="processName">
              <el-input v-model="form.processName" placeholder="请输入" :disabled="optType === 'view'" maxlength="255" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工序类型" prop="processType">
              <el-select v-model="form.processType" placeholder="请选择" style="width:100%" :disabled="optType === 'view'">
                <el-option v-for="d in processTypeOptions" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="是否启用" prop="enableFlag">
              <el-radio-group v-model="form.enableFlag" :disabled="optType === 'view'">
                <el-radio v-for="d in yesNoOptions" :key="d.value" :label="d.value">{{ d.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="工序说明" prop="attention">
              <el-input v-model="form.attention" type="textarea" :rows="3" placeholder="工艺要求/注意事项" :disabled="optType === 'view'" maxlength="1000" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入" :disabled="optType === 'view'" maxlength="500" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <!-- 操作步骤子表 -->
      <el-divider content-position="center" v-if="form.processId != null">操作步骤</el-divider>
      <div v-if="form.processId != null">
        <el-row :gutter="10" class="mb8" v-if="optType !== 'view'">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="Plus" size="small" @click="handleAddContent">新增步骤</el-button>
          </el-col>
        </el-row>
        <el-table :data="contentList" v-loading="contentLoading" size="small">
          <el-table-column label="顺序号" align="center" prop="orderNum" width="70" />
          <el-table-column label="步骤说明" align="center" prop="contentText" :show-overflow-tooltip="true" min-width="200" />
          <el-table-column label="辅助设备" align="center" prop="device" width="120" :show-overflow-tooltip="true" />
          <el-table-column label="辅助材料" align="center" prop="material" width="120" :show-overflow-tooltip="true" />
          <el-table-column label="SOP文档" align="center" prop="docUrl" width="120" :show-overflow-tooltip="true" />
          <el-table-column label="备注" align="center" prop="remark" width="120" :show-overflow-tooltip="true" />
          <el-table-column label="操作" align="center" width="80" v-if="optType !== 'view'" class-name="small-padding fixed-width">
            <template #default="scope">
              <el-tooltip content="修改" placement="top"><el-button link type="primary" icon="Edit" @click="handleUpdateContent(scope.row)"></el-button></el-tooltip>
              <el-tooltip content="删除" placement="top"><el-button link type="primary" icon="Delete" @click="handleDeleteContent(scope.row)"></el-button></el-tooltip>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 参数模版子表 -->
      <el-divider content-position="center" v-if="form.processId != null">参数模版</el-divider>
      <div v-if="form.processId != null">
        <el-row :gutter="10" class="mb8" v-if="optType !== 'view'">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="Plus" size="small" @click="handleAddParam">新增参数</el-button>
          </el-col>
        </el-row>
        <el-table :data="paramTemplateList" v-loading="paramLoading" size="small">
          <el-table-column label="参数编码" align="center" prop="paramCode" width="120" :show-overflow-tooltip="true" />
          <el-table-column label="参数名称" align="center" prop="paramName" min-width="120" :show-overflow-tooltip="true" />
          <el-table-column label="参数类型" align="center" prop="paramType" width="100">
            <template #default="scope">
              <span>{{ paramTypeMap[scope.row.paramType] || scope.row.paramType }}</span>
            </template>
          </el-table-column>
          <el-table-column label="单位" align="center" prop="unit" width="80" />
          <el-table-column label="默认值" align="center" prop="defaultValue" width="100" :show-overflow-tooltip="true" />
          <el-table-column label="图样" align="center" width="70">
            <template #default="scope">
              <image-preview v-if="scope.row.imageUrl" :src="scope.row.imageUrl" :width="40" :height="40" />
              <span v-else style="color: #909399">-</span>
            </template>
          </el-table-column>
          <el-table-column label="必填" align="center" prop="isRequired" width="60">
            <template #default="scope"><span :style="{color: scope.row.isRequired === 'Y' ? '#67C23A' : '#909399'}">{{ scope.row.isRequired === 'Y' ? '是' : '否' }}</span></template>
          </el-table-column>
          <el-table-column label="启用" align="center" prop="enableFlag" width="60">
            <template #default="scope"><span :style="{color: scope.row.enableFlag === '1' ? '#67C23A' : '#909399'}">{{ scope.row.enableFlag === '1' ? '是' : '否' }}</span></template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="80" v-if="optType !== 'view'" class-name="small-padding fixed-width">
            <template #default="scope">
              <el-tooltip content="修改" placement="top"><el-button link type="primary" icon="Edit" @click="handleUpdateParam(scope.row)"></el-button></el-tooltip>
              <el-tooltip content="删除" placement="top"><el-button link type="primary" icon="Delete" @click="handleDeleteParam(scope.row)"></el-button></el-tooltip>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm" v-if="optType !== 'view'">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 操作步骤编辑弹窗 -->
    <el-dialog :title="contentTitle" v-model="contentOpen" width="600px" append-to-body>
      <el-form ref="contentForm" :model="contentForm" :rules="contentRules" label-width="100px">
        <el-form-item label="顺序编号" prop="orderNum">
          <el-input-number v-model="contentForm.orderNum" :min="1" :max="999" style="width:100%" />
        </el-form-item>
        <el-form-item label="步骤说明" prop="contentText">
          <el-input v-model="contentForm.contentText" type="textarea" :rows="3" placeholder="请输入作业内容" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="辅助设备">
          <el-input v-model="contentForm.device" placeholder="所需设备/工具" maxlength="255" />
        </el-form-item>
        <el-form-item label="辅助材料">
          <el-input v-model="contentForm.material" placeholder="所需辅料" maxlength="255" />
        </el-form-item>
        <el-form-item label="SOP文档URL">
          <el-input v-model="contentForm.docUrl" placeholder="作业指导书链接" maxlength="255" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="contentForm.remark" type="textarea" placeholder="请输入" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitContentForm">确 定</el-button>
        <el-button @click="contentOpen = false">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 参数模版编辑弹窗 -->
    <el-dialog :title="paramTitle" v-model="paramOpen" width="650px" append-to-body>
      <el-form ref="paramForm" :model="paramFormData" :rules="paramRules" label-width="110px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="参数编码" prop="paramCode">
              <el-input v-model="paramFormData.paramCode" placeholder="英文标识，如 color_count" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="参数名称" prop="paramName">
              <el-input v-model="paramFormData.paramName" placeholder="如：印刷色数" maxlength="128" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="参数类型" prop="paramType">
              <el-select v-model="paramFormData.paramType" style="width:100%">
                <el-option label="整数(INT)" value="INT" />
                <el-option label="小数(DECIMAL)" value="DECIMAL" />
                <el-option label="文本(VARCHAR)" value="VARCHAR" />
                <el-option label="枚举(ENUM)" value="ENUM" />
                <el-option label="日期(DATE)" value="DATE" />
                <el-option label="布尔(BOOLEAN)" value="BOOLEAN" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="参数分组" prop="paramGroup">
              <el-select v-model="paramFormData.paramGroup" style="width:100%">
                <el-option label="设备参数" value="MACHINE" />
                <el-option label="工艺参数" value="PROCESS" />
                <el-option label="材料参数" value="MATERIAL" />
                <el-option label="质量参数" value="QUALITY" />
                <el-option label="产品规格参数" value="PRODUCT" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="单位">
              <el-input v-model="paramFormData.unit" placeholder="如mm, ℃, m/min" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号" prop="sortOrder">
              <el-input-number v-model="paramFormData.sortOrder" :min="1" :max="999" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="默认值">
          <el-input v-model="paramFormData.defaultValue" placeholder="创建路线时自动填充的默认值" maxlength="255" />
        </el-form-item>
        <el-form-item v-if="paramFormData.paramType === 'ENUM'" label="枚举可选值">
          <el-input v-model="paramFormData.enumValues" placeholder='JSON数组格式，如["光膜","哑膜"]' maxlength="500" />
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="最小值">
              <el-input-number v-model="paramFormData.minValue" :precision="4" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大值">
              <el-input-number v-model="paramFormData.maxValue" :precision="4" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="是否必填"><el-radio-group v-model="paramFormData.isRequired"><el-radio label="Y">是</el-radio><el-radio label="N">否</el-radio></el-radio-group></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="报工可见"><el-radio-group v-model="paramFormData.isReportVisible"><el-radio label="Y">是</el-radio><el-radio label="N">否</el-radio></el-radio-group></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否启用"><el-radio-group v-model="paramFormData.enableFlag"><el-radio label="1">是</el-radio><el-radio label="0">否</el-radio></el-radio-group></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="paramFormData.remark" type="textarea" placeholder="请输入" maxlength="500" />
        </el-form-item>
        <el-form-item label="标准图样">
          <image-upload v-model="paramFormData.imageUrl" :limit="6" :file-size="10" :file-type="['png','jpg','jpeg']" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitParamForm">确 定</el-button>
        <el-button @click="paramOpen = false">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { listProcess, getProcess, delProcess, addProcess, updateProcess } from '@/api/mes/pro/process'
import { listProcessContentByProcessId, getProcessContent, addProcessContent, updateProcessContent, delProcessContent } from '@/api/mes/pro/processcontent'
import { listParamTemplateByProcessId, getParamTemplate, addParamTemplate, updateParamTemplate, delParamTemplate } from '@/api/mes/pro/paramtemplate'
import { genSerialCode } from '@/api/mes/sys/autocoderule'

export default {
  name: 'Process',
  data() {
    return {
      autoGenFlag: false,
      optType: undefined,
      loading: true,
      contentLoading: false,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      processList: [],
      contentList: [],
      title: '',
      open: false,
      contentOpen: false,
      contentTitle: '',
      contentForm: {},
      yesNoOptions: [
        { label: '是', value: '1' },
        { label: '否', value: '0' }
      ],
      processTypeOptions: [
        { label: '自制工序', value: 'INTERNAL' },
        { label: '外发工序', value: 'OUTSOURCE' },
        { label: '分切工序', value: 'SLITTING' }
      ],
      processTypeMap: { INTERNAL: '自制', OUTSOURCE: '外发', SLITTING: '分切' },
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        processCode: null,
        processName: null,
        enableFlag: null
      },
      form: {},
      rules: {
        processCode: [
          { required: true, message: '工序编码不能为空', trigger: 'blur' },
          { max: 64, message: '字段过长', trigger: 'blur' }
        ],
        processName: [
          { required: true, message: '工序名称不能为空', trigger: 'blur' },
          { max: 255, message: '字段过长', trigger: 'blur' }
        ],
        enableFlag: [
          { required: true, message: '请选择是否启用', trigger: 'change' }
        ]
      },
      contentRules: {
        orderNum: [
          { required: true, message: '顺序编号不能为空', trigger: 'blur' }
        ],
        contentText: [
          { required: true, message: '步骤说明不能为空', trigger: 'blur' }
        ]
      },
      // 参数模版
      paramTemplateList: [],
      paramLoading: false,
      paramOpen: false,
      paramTitle: '',
      paramFormData: {},
      paramTypeMap: { INT: '整数', DECIMAL: '小数', VARCHAR: '文本', ENUM: '枚举', DATE: '日期', BOOLEAN: '布尔' },
      paramRules: {
        paramCode: [{ required: true, message: '参数编码不能为空', trigger: 'blur' }, { max: 64, message: '字段过长', trigger: 'blur' }],
        paramName: [{ required: true, message: '参数名称不能为空', trigger: 'blur' }, { max: 128, message: '字段过长', trigger: 'blur' }],
        paramType: [{ required: true, message: '请选择参数类型', trigger: 'change' }],
        sortOrder: [{ required: true, message: '排序号不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listProcess(this.queryParams).then(r => {
        this.processList = r.rows
        this.total = r.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    loadContentList(processId) {
      this.contentLoading = true
      listProcessContentByProcessId(processId).then(r => {
        this.contentList = r.data || []
        this.contentLoading = false
      }).catch(() => { this.contentLoading = false })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        processId: null,
        processCode: null,
        processName: null,
        processType: 'INTERNAL',
        attention: null,
        enableFlag: '1',
        remark: null
      }
      this.autoGenFlag = false
      this.contentList = []
      this.paramTemplateList = []
      this.resetForm('form')
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.$refs.queryForm?.resetFields()
      this.handleQuery()
    },
    handleSelectionChange(sel) {
      this.ids = sel.map(i => i.processId)
      this.single = sel.length !== 1
      this.multiple = !sel.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增生产工序'
      this.optType = 'add'
    },
    handleView(row) {
      this.reset()
      const id = row.processId || this.ids[0]
      getProcess(id).then(r => {
        this.form = r.data
        this.open = true
        this.title = '查看工序信息'
        this.optType = 'view'
        this.loadContentList(id)
        this.loadParamTemplateList(id)
      })
    },
    handleUpdate(row) {
      this.reset()
      const id = row.processId || this.ids[0]
      getProcess(id).then(r => {
        this.form = r.data
        this.open = true
        this.title = '修改生产工序'
        this.optType = 'edit'
        this.loadContentList(id)
        this.loadParamTemplateList(id)
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        if (this.form.processId != null) {
          updateProcess(this.form).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.open = false
            this.getList()
          })
        } else {
          addProcess(this.form).then(r => {
            this.$modal.msgSuccess('新增成功')
            this.form.processId = r.data?.processId || r.data
            this.loadContentList(this.form.processId)
            this.getList()
          })
        }
      })
    },
    handleDelete(row) {
      const ids = row.processId || this.ids.join(',')
      this.$modal.confirm('确认删除工序编号为"' + ids + '"的数据项？').then(() => delProcess(ids)).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleAutoGenChange(f) {
      if (f) {
        genSerialCode('PROCESS_CODE').then(r => { this.form.processCode = r.data })
      } else {
        this.form.processCode = null
      }
    },
    handleExport() {
      this.download('mes/pro/process/export', { ...this.queryParams }, `process_${new Date().getTime()}.xlsx`)
    },
    // ===== 操作步骤 CRUD =====
    handleAddContent() {
      this.contentForm = {
        processId: this.form.processId,
        orderNum: this.contentList.length + 1,
        contentText: null,
        device: null,
        material: null,
        docUrl: null,
        remark: null
      }
      this.contentTitle = '新增操作步骤'
      this.contentOpen = true
    },
    handleUpdateContent(row) {
      this.contentForm = { ...row }
      this.contentTitle = '修改操作步骤'
      this.contentOpen = true
    },
    handleDeleteContent(row) {
      this.$modal.confirm('确认删除该操作步骤？').then(() => delProcessContent(row.contentId)).then(() => {
        this.loadContentList(this.form.processId)
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    submitContentForm() {
      this.$refs.contentForm.validate(valid => {
        if (!valid) return
        if (this.contentForm.contentId != null) {
          updateProcessContent(this.contentForm).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.contentOpen = false
            this.loadContentList(this.form.processId)
          })
        } else {
          addProcessContent(this.contentForm).then(() => {
            this.$modal.msgSuccess('新增成功')
            this.contentOpen = false
            this.loadContentList(this.form.processId)
          })
        }
      })
    },
    // ===== 参数模版 CRUD =====
    loadParamTemplateList(processId) {
      this.paramLoading = true
      listParamTemplateByProcessId(processId).then(r => {
        this.paramTemplateList = r.data || []
        this.paramLoading = false
      }).catch(() => { this.paramLoading = false })
    },
    handleAddParam() {
      this.paramFormData = {
        processId: this.form.processId,
        paramCode: null,
        paramName: null,
        paramGroup: 'PROCESS',
        paramType: 'VARCHAR',
        unit: null,
        enumValues: null,
        defaultValue: null,
        minValue: null,
        maxValue: null,
        sortOrder: (this.paramTemplateList.length + 1),
        isRequired: 'Y',
        isReportVisible: 'Y',
        enableFlag: '1',
        imageUrl: null,
        remark: null
      }
      this.paramTitle = '新增参数模版'
      this.paramOpen = true
    },
    handleUpdateParam(row) {
      this.paramFormData = { ...row }
      this.paramTitle = '修改参数模版'
      this.paramOpen = true
    },
    handleDeleteParam(row) {
      this.$modal.confirm('确认删除参数"' + row.paramName + '"？').then(() => delParamTemplate(row.templateId)).then(() => {
        this.loadParamTemplateList(this.form.processId)
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    submitParamForm() {
      this.$refs.paramForm.validate(valid => {
        if (!valid) return
        if (this.paramFormData.templateId != null) {
          updateParamTemplate(this.paramFormData).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.paramOpen = false
            this.loadParamTemplateList(this.form.processId)
          })
        } else {
          addParamTemplate(this.paramFormData).then(() => {
            this.$modal.msgSuccess('新增成功')
            this.paramOpen = false
            this.loadParamTemplateList(this.form.processId)
          })
        }
      })
    },
    handleEnableChange(row) {
      const newVal = row.enableFlag
      const text = newVal === '1' ? '启用' : '停用'
      this.$modal.confirm('确认要' + text + '"' + row.processName + '"吗？').then(() => {
        updateProcess({ processId: row.processId, enableFlag: newVal }).then(() => this.$modal.msgSuccess(text + '成功'))
      }).catch(() => {
        row.enableFlag = newVal === '1' ? '0' : '1'
        this.getList()
      })
    }
  }
}

</script>
