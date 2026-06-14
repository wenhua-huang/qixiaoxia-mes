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
      <el-form-item label="采购订单编码(唯一)" prop="orderCode">
        <el-input
          v-model="queryParams.orderCode"
          placeholder="请输入采购订单编码(唯一)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="采购订单名称" prop="orderName">
        <el-input
          v-model="queryParams.orderName"
          placeholder="请输入采购订单名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="供应商ID(关联qxx_md_vendor,vendor_type=MATERIAL)" prop="vendorId">
        <el-input
          v-model="queryParams.vendorId"
          placeholder="请输入供应商ID(关联qxx_md_vendor,vendor_type=MATERIAL)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="供应商编码" prop="vendorCode">
        <el-input
          v-model="queryParams.vendorCode"
          placeholder="请输入供应商编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="供应商名称" prop="vendorName">
        <el-input
          v-model="queryParams.vendorName"
          placeholder="请输入供应商名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="下单日期" prop="orderDate">
        <el-date-picker clearable
          v-model="queryParams.orderDate"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择下单日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="预计到货日期" prop="expectedDate">
        <el-date-picker clearable
          v-model="queryParams.expectedDate"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择预计到货日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="采购员(申购人)" prop="purchaser">
        <el-input
          v-model="queryParams.purchaser"
          placeholder="请输入采购员(申购人)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="审批人" prop="approver">
        <el-input
          v-model="queryParams.approver"
          placeholder="请输入审批人"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="采购总数量(主单位)" prop="totalQuantity">
        <el-input
          v-model="queryParams.totalQuantity"
          placeholder="请输入采购总数量(主单位)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="采购总金额(元)" prop="totalAmount">
        <el-input
          v-model="queryParams.totalAmount"
          placeholder="请输入采购总金额(元)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="币种:CNY-人民币,USD-美元" prop="currency">
        <el-input
          v-model="queryParams.currency"
          placeholder="请输入币种:CNY-人民币,USD-美元"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="关联客户订单号(如PO#ORD66003MT)" prop="sourceOrderCode">
        <el-input
          v-model="queryParams.sourceOrderCode"
          placeholder="请输入关联客户订单号(如PO#ORD66003MT)"
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
          v-hasPermi="['mes:pur:order:add']"
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
          v-hasPermi="['mes:pur:order:edit']"
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
          v-hasPermi="['mes:pur:order:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['mes:pur:order:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="orderList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="采购订单ID" align="center" prop="orderId" />
      <el-table-column label="工厂ID(关联qxx_md_factory)" align="center" prop="factoryId" />
      <el-table-column label="采购订单编码(唯一)" align="center" prop="orderCode" />
      <el-table-column label="采购订单名称" align="center" prop="orderName" />
      <el-table-column label="供应商ID(关联qxx_md_vendor,vendor_type=MATERIAL)" align="center" prop="vendorId" />
      <el-table-column label="供应商编码" align="center" prop="vendorCode" />
      <el-table-column label="供应商名称" align="center" prop="vendorName" />
      <el-table-column label="采购类型:PAPER-纸张,AUX-辅料(绳子/胶水/油墨),PACK-包材(纸箱),OTHER-其他" align="center" prop="purchaseType" />
      <el-table-column label="下单日期" align="center" prop="orderDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.orderDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="预计到货日期" align="center" prop="expectedDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.expectedDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="采购员(申购人)" align="center" prop="purchaser" />
      <el-table-column label="审批人" align="center" prop="approver" />
      <el-table-column label="采购总数量(主单位)" align="center" prop="totalQuantity" />
      <el-table-column label="采购总金额(元)" align="center" prop="totalAmount" />
      <el-table-column label="币种:CNY-人民币,USD-美元" align="center" prop="currency" />
      <el-table-column label="关联客户订单号(如PO#ORD66003MT)" align="center" prop="sourceOrderCode" />
      <el-table-column label="状态:DRAFT-草稿,APPROVED-已审批,ORDERED-已下单,RECEIVING-收货中,RECEIVED-已收货,CLOSED-已关闭,CANCEL-已取消" align="center" prop="status" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['mes:pur:order:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['mes:pur:order:remove']"
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

    <!-- 添加或修改采购订单头对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="工厂ID(关联qxx_md_factory)" prop="factoryId">
              <el-input v-model="form.factoryId" placeholder="请输入工厂ID(关联qxx_md_factory)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="采购订单编码(唯一)" prop="orderCode">
              <el-input v-model="form.orderCode" placeholder="请输入采购订单编码(唯一)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="采购订单名称" prop="orderName">
              <el-input v-model="form.orderName" placeholder="请输入采购订单名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="供应商ID(关联qxx_md_vendor,vendor_type=MATERIAL)" prop="vendorId">
              <el-input v-model="form.vendorId" placeholder="请输入供应商ID(关联qxx_md_vendor,vendor_type=MATERIAL)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="供应商编码" prop="vendorCode">
              <el-input v-model="form.vendorCode" placeholder="请输入供应商编码" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="供应商名称" prop="vendorName">
              <el-input v-model="form.vendorName" placeholder="请输入供应商名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="下单日期" prop="orderDate">
              <el-date-picker clearable
                v-model="form.orderDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择下单日期">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="预计到货日期" prop="expectedDate">
              <el-date-picker clearable
                v-model="form.expectedDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择预计到货日期">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="采购员(申购人)" prop="purchaser">
              <el-input v-model="form.purchaser" placeholder="请输入采购员(申购人)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="审批人" prop="approver">
              <el-input v-model="form.approver" placeholder="请输入审批人" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="采购总数量(主单位)" prop="totalQuantity">
              <el-input v-model="form.totalQuantity" placeholder="请输入采购总数量(主单位)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="采购总金额(元)" prop="totalAmount">
              <el-input v-model="form.totalAmount" placeholder="请输入采购总金额(元)" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="币种:CNY-人民币,USD-美元" prop="currency">
              <el-input v-model="form.currency" placeholder="请输入币种:CNY-人民币,USD-美元" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="关联客户订单号(如PO#ORD66003MT)" prop="sourceOrderCode">
              <el-input v-model="form.sourceOrderCode" placeholder="请输入关联客户订单号(如PO#ORD66003MT)" />
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
import { listOrder, getOrder, delOrder, addOrder, updateOrder } from "@/api/mes/pur/order"

export default {
  name: "Order",
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
      // 采购订单头表格数据
      orderList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        factoryId: null,
        orderCode: null,
        orderName: null,
        vendorId: null,
        vendorCode: null,
        vendorName: null,
        purchaseType: null,
        orderDate: null,
        expectedDate: null,
        purchaser: null,
        approver: null,
        totalQuantity: null,
        totalAmount: null,
        currency: null,
        sourceOrderCode: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        factoryId: [
          { required: true, message: "工厂ID(关联qxx_md_factory)不能为空", trigger: "blur" }
        ],
        orderCode: [
          { required: true, message: "采购订单编码(唯一)不能为空", trigger: "blur" }
        ],
        vendorId: [
          { required: true, message: "供应商ID(关联qxx_md_vendor,vendor_type=MATERIAL)不能为空", trigger: "blur" }
        ],
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询采购订单头列表 */
    getList() {
      this.loading = true
      listOrder(this.queryParams).then(response => {
        this.orderList = response.rows
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
        orderId: null,
        factoryId: null,
        orderCode: null,
        orderName: null,
        vendorId: null,
        vendorCode: null,
        vendorName: null,
        purchaseType: null,
        orderDate: null,
        expectedDate: null,
        purchaser: null,
        approver: null,
        totalQuantity: null,
        totalAmount: null,
        currency: null,
        sourceOrderCode: null,
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
      this.ids = selection.map(item => item.orderId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加采购订单头"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const orderId = row.orderId || this.ids
      getOrder(orderId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改采购订单头"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.orderId != null) {
            updateOrder(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addOrder(this.form).then(response => {
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
      const orderIds = row.orderId || this.ids
      this.$modal.confirm('是否确认删除采购订单头编号为"' + orderIds + '"的数据项？').then(function() {
        return delOrder(orderIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('mes/pur/order/export', {
        ...this.queryParams
      }, `order_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
