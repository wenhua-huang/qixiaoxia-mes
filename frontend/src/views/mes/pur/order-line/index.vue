<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">      <el-form-item label="订单ID" prop="orderId">
        <el-input
          v-model="queryParams.orderId"
          placeholder="请输入采购订单ID(关联qxx_pur_order)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="物料ID" prop="itemId">
        <el-input
          v-model="queryParams.itemId"
          placeholder="请输入物料ID(关联qxx_md_item)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="物料编码" prop="itemCode">
        <el-input
          v-model="queryParams.itemCode"
          placeholder="请输入物料编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="物料名称" prop="itemName">
        <el-input
          v-model="queryParams.itemName"
          placeholder="请输入物料名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="主单位" prop="unitOfMeasure">
        <el-input
          v-model="queryParams.unitOfMeasure"
          placeholder="请输入主单位编码(如TON-吨,ROLL-卷,PCS-个,KG-千克)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="主单位名" prop="unitName">
        <el-input
          v-model="queryParams.unitName"
          placeholder="请输入主单位名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="辅助单位" prop="unit2">
        <el-input
          v-model="queryParams.unit2"
          placeholder="请输入辅助单位编码(如ROLL-卷,与主单位联动)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="辅助单位名" prop="unit2Name">
        <el-input
          v-model="queryParams.unit2Name"
          placeholder="请输入辅助单位名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="换算率" prop="conversionRate">
        <el-input
          v-model="queryParams.conversionRate"
          placeholder="请输入主单位→辅助单位换算率"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="订购数量" prop="quantityOrdered">
        <el-input
          v-model="queryParams.quantityOrdered"
          placeholder="请输入订购数量(主单位)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="辅助数量" prop="quantityOrdered2">
        <el-input
          v-model="queryParams.quantityOrdered2"
          placeholder="请输入订购数量(辅助单位,如卷数)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="已收货数" prop="quantityReceived">
        <el-input
          v-model="queryParams.quantityReceived"
          placeholder="请输入已收货数量(主单位)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="辅助已收" prop="quantityReceived2">
        <el-input
          v-model="queryParams.quantityReceived2"
          placeholder="请输入已收货数量(辅助单位)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="单价" prop="unitPrice">
        <el-input
          v-model="queryParams.unitPrice"
          placeholder="请输入单价(元/主单位,如元/吨)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="行金额" prop="amount">
        <el-input
          v-model="queryParams.amount"
          placeholder="请输入行金额(不含税)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="税率" prop="taxRate">
        <el-input
          v-model="queryParams.taxRate"
          placeholder="请输入税率(%)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="门幅(mm)" prop="paperWidth">
        <el-input
          v-model="queryParams.paperWidth"
          placeholder="请输入门幅要求(mm),如925mm"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="克重(g)" prop="paperWeight">
        <el-input
          v-model="queryParams.paperWeight"
          placeholder="请输入克重要求(g),如120g"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="卷数" prop="rollCount">
        <el-input
          v-model="queryParams.rollCount"
          placeholder="请输入预计卷数(纸张行业用，其他行业=0)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="客户订单号" prop="sourceOrderCode">
        <el-input
          v-model="queryParams.sourceOrderCode"
          placeholder="请输入关联客户订单号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="预计到货日期" prop="expectedDate">
        <el-date-picker clearable
          v-model="queryParams.expectedDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="请选择预计到货日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="到货通知" prop="arrivalNoticeId">
        <el-input
          v-model="queryParams.arrivalNoticeId"
          placeholder="请输入到货通知单ID(关联qxx_wm_arrival_notice,收货后回写)"
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
          v-hasPermi="['mes:pur:order-line:add']"
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
          v-hasPermi="['mes:pur:order-line:edit']"
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
          v-hasPermi="['mes:pur:order-line:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['mes:pur:order-line:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="lineList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="lineId" />      <el-table-column label="订单ID" align="center" prop="orderId" />
      <el-table-column label="物料ID" align="center" prop="itemId" />
      <el-table-column label="物料编码" align="center" prop="itemCode" />
      <el-table-column label="物料名称" align="center" prop="itemName" />
      <el-table-column label="规格" align="center" prop="specification" />
      <el-table-column label="主单位" align="center" prop="unitOfMeasure" />
      <el-table-column label="主单位名" align="center" prop="unitName" />
      <el-table-column label="辅助单位" align="center" prop="unit2" />
      <el-table-column label="辅助单位名" align="center" prop="unit2Name" />
      <el-table-column label="换算率" align="center" prop="conversionRate" />
      <el-table-column label="订购数量" align="center" prop="quantityOrdered" />
      <el-table-column label="辅助数量" align="center" prop="quantityOrdered2" />
      <el-table-column label="已收货数" align="center" prop="quantityReceived" />
      <el-table-column label="辅助已收" align="center" prop="quantityReceived2" />
      <el-table-column label="单价" align="center" prop="unitPrice" />
      <el-table-column label="行金额" align="center" prop="amount" />
      <el-table-column label="税率" align="center" prop="taxRate" />
      <el-table-column label="门幅(mm)" align="center" prop="paperWidth" />
      <el-table-column label="克重(g)" align="center" prop="paperWeight" />
      <el-table-column label="纸张种类" align="center" prop="paperType" />
      <el-table-column label="卷数" align="center" prop="rollCount" />
      <el-table-column label="客户订单号" align="center" prop="sourceOrderCode" />
      <el-table-column label="预计到货日期" align="center" prop="expectedDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.expectedDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="到货通知" align="center" prop="arrivalNoticeId" />
      <el-table-column label="状态" align="center" prop="status" width="90">
          <template #default="scope">
            <span>{{ statusMap[scope.row.status] || scope.row.status }}</span>
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
            v-hasPermi="['mes:pur:order-line:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['mes:pur:order-line:remove']"
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

    <!-- 添加或修改采购订单行对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">          </el-col>
          <el-col :span="24">
            <el-form-item label="订单ID" prop="orderId">
              <el-input v-model="form.orderId" placeholder="请输入采购订单ID(关联qxx_pur_order)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="物料ID" prop="itemId">
              <el-input v-model="form.itemId" placeholder="请输入物料ID(关联qxx_md_item)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="物料编码" prop="itemCode">
              <el-input v-model="form.itemCode" placeholder="请输入物料编码" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="物料名称" prop="itemName">
              <el-input v-model="form.itemName" placeholder="请输入物料名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="规格" prop="specification">
              <el-input v-model="form.specification" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="主单位" prop="unitOfMeasure">
              <el-input v-model="form.unitOfMeasure" placeholder="请输入主单位编码(如TON-吨,ROLL-卷,PCS-个,KG-千克)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="主单位名" prop="unitName">
              <el-input v-model="form.unitName" placeholder="请输入主单位名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="辅助单位" prop="unit2">
              <el-input v-model="form.unit2" placeholder="请输入辅助单位编码(如ROLL-卷,与主单位联动)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="辅助单位名" prop="unit2Name">
              <el-input v-model="form.unit2Name" placeholder="请输入辅助单位名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="换算率" prop="conversionRate">
              <el-input v-model="form.conversionRate" placeholder="请输入主单位→辅助单位换算率" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="订购数量" prop="quantityOrdered">
              <el-input v-model="form.quantityOrdered" placeholder="请输入订购数量(主单位)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="辅助数量" prop="quantityOrdered2">
              <el-input v-model="form.quantityOrdered2" placeholder="请输入订购数量(辅助单位,如卷数)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="已收货数" prop="quantityReceived">
              <el-input v-model="form.quantityReceived" placeholder="请输入已收货数量(主单位)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="辅助已收" prop="quantityReceived2">
              <el-input v-model="form.quantityReceived2" placeholder="请输入已收货数量(辅助单位)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="单价" prop="unitPrice">
              <el-input v-model="form.unitPrice" placeholder="请输入单价(元/主单位,如元/吨)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="行金额" prop="amount">
              <el-input v-model="form.amount" placeholder="请输入行金额(不含税)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="税率" prop="taxRate">
              <el-input v-model="form.taxRate" placeholder="请输入税率(%)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="门幅(mm)" prop="paperWidth">
              <el-input v-model="form.paperWidth" placeholder="请输入门幅要求(mm),如925mm" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="克重(g)" prop="paperWeight">
              <el-input v-model="form.paperWeight" placeholder="请输入克重要求(g),如120g" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="卷数" prop="rollCount">
              <el-input v-model="form.rollCount" placeholder="请输入预计卷数(纸张行业用，其他行业=0)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="客户订单号" prop="sourceOrderCode">
              <el-input v-model="form.sourceOrderCode" placeholder="请输入关联客户订单号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="预计到货日期" prop="expectedDate">
              <el-date-picker clearable
                v-model="form.expectedDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择预计到货日期">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="到货通知" prop="arrivalNoticeId">
              <el-input v-model="form.arrivalNoticeId" placeholder="请输入到货通知单ID(关联qxx_wm_arrival_notice,收货后回写)" />
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
import { listLine, getLine, delLine, addLine, updateLine } from "@/api/mes/pur/order-line"

export default {
  name: "Line",
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
      // 采购订单行表格数据
      lineList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,        orderId: null,
        itemId: null,
        itemCode: null,
        itemName: null,
        specification: null,
        unitOfMeasure: null,
        unitName: null,
        unit2: null,
        unit2Name: null,
        conversionRate: null,
        quantityOrdered: null,
        quantityOrdered2: null,
        quantityReceived: null,
        quantityReceived2: null,
        unitPrice: null,
        amount: null,
        taxRate: null,
        paperWidth: null,
        paperWeight: null,
        paperType: null,
        rollCount: null,
        sourceOrderCode: null,
        expectedDate: null,
        arrivalNoticeId: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {        orderId: [
          { required: true, message: "采购订单ID(关联qxx_pur_order)不能为空", trigger: "blur" }
        ],
        itemId: [
          { required: true, message: "物料ID(关联qxx_md_item)不能为空", trigger: "blur" }
        ],
        itemCode: [
          { required: true, message: "物料编码不能为空", trigger: "blur" }
        ],
        itemName: [
          { required: true, message: "物料名称不能为空", trigger: "blur" }
        ],
        unitOfMeasure: [
          { required: true, message: "主单位编码(如TON-吨,ROLL-卷,PCS-个,KG-千克)不能为空", trigger: "blur" }
        ],
        unitName: [
          { required: true, message: "主单位名称不能为空", trigger: "blur" }
        ],
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询采购订单行列表 */
    getList() {
      this.loading = true
      listLine(this.queryParams).then(response => {
        this.lineList = response.rows
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
        lineId: null,        orderId: null,
        itemId: null,
        itemCode: null,
        itemName: null,
        specification: null,
        unitOfMeasure: null,
        unitName: null,
        unit2: null,
        unit2Name: null,
        conversionRate: null,
        quantityOrdered: null,
        quantityOrdered2: null,
        quantityReceived: null,
        quantityReceived2: null,
        unitPrice: null,
        amount: null,
        taxRate: null,
        paperWidth: null,
        paperWeight: null,
        paperType: null,
        rollCount: null,
        sourceOrderCode: null,
        expectedDate: null,
        arrivalNoticeId: null,
        status: null,
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
      this.ids = selection.map(item => item.lineId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加采购订单行"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const lineId = row.lineId || this.ids
      getLine(lineId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改采购订单行"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.lineId != null) {
            updateLine(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addLine(this.form).then(response => {
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
      const lineIds = row.lineId || this.ids
      this.$modal.confirm('是否确认删除采购订单行编号为"' + lineIds + '"的数据项？').then(function() {
        return delLine(lineIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('mes/pur/order-line/export', {
        ...this.queryParams
      }, `line_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
