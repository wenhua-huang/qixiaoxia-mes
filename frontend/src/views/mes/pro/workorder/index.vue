<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-row>
        <el-col :span="8"><el-form-item label="工单编码" prop="workorderCode"><el-input v-model="queryParams.workorderCode" placeholder="请输入" clearable @keyup.enter="handleQuery" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="产品名称" prop="productName"><el-input v-model="queryParams.productName" placeholder="请输入" clearable @keyup.enter="handleQuery" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="工单状态" prop="status"><el-select v-model="queryParams.status" placeholder="请选择" clearable><el-option label="待生产" value="PREPARE" /><el-option label="生产中" value="PRODUCING" /><el-option label="已完成" value="COMPLETED" /><el-option label="已取消" value="CANCEL" /><el-option label="已关闭" value="CLOSED" /></el-select></el-form-item></el-col>
      </el-row>
      <el-row>
        <el-col :span="8"><el-form-item><el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button><el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button></el-form-item></el-col>
      </el-row>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['mes:pro:workorder:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate" v-hasPermi="['mes:pro:workorder:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:pro:workorder:remove']">删除</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="workorderList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="工单编码" align="center" prop="workorderCode" width="130" />
      <el-table-column label="工单名称" align="center" prop="workorderName" :show-overflow-tooltip="true"><template #default="scope"><el-button link @click="handleView(scope.row)" v-hasPermi="['mes:pro:workorder:query']">{{ scope.row.workorderName }}</el-button></template></el-table-column>
      <el-table-column label="产品" align="center" prop="productName" :show-overflow-tooltip="true" />
      <el-table-column label="计划数量" align="center" prop="quantity" width="90" />
      <el-table-column label="已生产" align="center" prop="quantityProduced" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="80"><template #default="scope"><span :style="{color: statusColor[scope.row.status]}">{{ statusMap[scope.row.status] || scope.row.status }}</span></template></el-table-column>
      <el-table-column label="需求日期" align="center" prop="requestDate" width="100"><template #default="scope"><span>{{ parseTime(scope.row.requestDate, '{y}-{m}-{d}') }}</span></template></el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="150"><template #default="scope"><span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span></template></el-table-column>
      <el-table-column label="操作" align="center" width="185" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tooltip content="排产" placement="top" v-if="scope.row.status==='PREPARE' || scope.row.status==='PRODUCING'"><el-button link type="success" icon="Calendar" @click="handleSchedule(scope.row)" v-hasPermi="['mes:pro:task:add']"></el-button></el-tooltip>
          <el-tooltip content="开工" placement="top" v-if="scope.row.status==='PREPARE'"><el-button link type="primary" icon="VideoPlay" @click="handleStart(scope.row)" v-hasPermi="['mes:pro:workorder:edit']"></el-button></el-tooltip>
          <el-tooltip content="齐套检查" placement="top" v-if="scope.row.status==='PREPARE'"><el-button link type="warning" icon="List" @click="handleCheckMaterial(scope.row)"></el-button></el-tooltip>
          <el-tooltip content="修改" placement="top" v-if="scope.row.status==='PREPARE'"><el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:pro:workorder:edit']"></el-button></el-tooltip>
          <el-tooltip content="删除" placement="top" v-if="scope.row.status==='PREPARE'"><el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:pro:workorder:remove']"></el-button></el-tooltip>
          <el-tooltip content="查看" placement="top"><el-button link type="primary" icon="View" @click="handleView(scope.row)"></el-button></el-tooltip>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑弹窗 — 2 Step 向导 -->
    <el-dialog :title="title" v-model="open" width="1000px" append-to-body @close="cancel">
      <el-steps :active="step" finish-status="success" simple style="margin-bottom:20px" v-if="optType==='add' || optType==='edit'">
        <el-step title="基本信息" /><el-step title="工序明细" />
      </el-steps>

      <!-- Step 1: 基本信息 -->
      <el-form ref="form" :model="form" :rules="rules" label-width="110px" v-show="step===1">
        <el-row>
          <el-col :span="16">
            <el-form-item label="工单编码" prop="workorderCode">
              <el-input v-model="form.workorderCode" placeholder="请输入" :disabled="optType!=='add'" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label-width="80">
              <el-switch v-model="autoGenFlag" @change="handleAutoGenChange" v-if="optType==='add'" />
              <span style="margin-left:6px;font-size:12px;color:#909399">自动生成</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="工单名称" prop="workorderName">
              <el-input v-model="form.workorderName" placeholder="请输入" :disabled="optType==='view'" maxlength="255" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工单类型" prop="workorderType">
              <el-radio-group v-model="form.workorderType" :disabled="optType==='view'">
                <el-radio label="SELF">自制</el-radio>
                <el-radio label="OUTSOURCE">外协</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
                <el-row>
          <el-col :span="12">
            <el-form-item label="产品物料" prop="productId">
              <el-row :gutter="8" v-if="optType!=='view'">
                <el-col :span="18"><el-input :model-value="productDisplayName" placeholder="请选择产品" disabled /></el-col>
                <el-col :span="6"><el-button type="primary" icon="Search" size="small" @click="$refs.itemSelectRef.open()">选择</el-button></el-col>
              </el-row>
              <el-input v-else :model-value="productDisplayName" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工艺路线" prop="prorouteId">
              <el-select v-model="prorouteId" placeholder="请先选择产品" style="width:100%" @change="onRouteChange">
                <el-option v-for="r in routeOptions" :key="r.recordId" :label="(r._routeName || '路线#'+r.routeId) + ' — ' + r.itemName" :value="r.recordId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row><el-col :span="12"><el-form-item label="计划数量" prop="quantity"><el-input-number v-model="form.quantity" :min="1" style="width:100%" :disabled="optType==='view'" /></el-form-item></el-col><el-col :span="12"><el-form-item label="需求日期" prop="requestDate"><el-date-picker v-model="form.requestDate" type="date" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择" style="width:100%" :disabled="optType==='view'" /></el-form-item></el-col></el-row>
        <el-row><el-col :span="12"><el-form-item label="客户订单号"><el-input v-model="form.clientOrderCode" placeholder="客户PO号" :disabled="optType==='view'" /></el-form-item></el-col><el-col :span="12"><el-form-item label="订单类型"><el-select v-model="form.orderType" style="width:100%" :disabled="optType==='view'"><el-option label="新单" value="NEW" /><el-option label="返单" value="REPEAT" /></el-select></el-form-item></el-col></el-row>
        <el-row><el-col :span="12"><el-form-item label="产品尺寸"><el-input v-model="form.productSize" placeholder="如254*127*330mm" :disabled="optType==='view'" /></el-form-item></el-col><el-col :span="12"><el-form-item label="绳料规格"><el-input v-model="form.ropeSpec" placeholder="纸袋专用" :disabled="optType==='view'" /></el-form-item></el-col></el-row>
        <el-row><el-col :span="24"><el-form-item label="印刷要求"><el-input v-model="form.printingReq" placeholder="如1色满版黑印刷" :disabled="optType==='view'" /></el-form-item></el-col></el-row>
        <el-row><el-col :span="24"><el-form-item label="包装要求"><el-input v-model="form.packageReq" placeholder="如250个/箱,贴唛头" :disabled="optType==='view'" /></el-form-item></el-col></el-row>
      </el-form>

      <!-- Step 2: 工序明细（按工序分组显示BOM + 参数） -->
      <div v-show="step===2 || optType==='view'" v-if="form.workorderId || optType==='add'">
        <el-alert title="从工艺路线加载，可按工单调整物料或参数" type="info" :closable="false" show-icon style="margin-bottom:12px" />

        <el-empty v-if="processGroupList.length===0" description="暂无工序数据，请先选择工艺路线" :image-size="80" />

        <el-row class="mb8" v-if="optType!=='view'">
          <el-col :span="1.5"><el-button type="success" plain icon="Plus" size="small" @click="showProcessSelector=true">添加工序</el-button></el-col>
        </el-row>
        <el-collapse v-if="processGroupList.length>0" v-model="activeProcesses">
          <el-collapse-item v-for="(proc, idx) in processGroupList" :key="proc._key" :name="proc._key">
            <template #title>
              <span style="font-weight:bold;font-size:14px">{{ proc.processName || '未分配工序' }}</span>
              <el-tag size="small" style="margin-left:10px">{{ proc.bomItems.length }} 物料</el-tag>
              <el-tag size="small" type="warning" style="margin-left:4px">{{ proc.paramItems.length }} 参数</el-tag>
              <span style="margin-left:auto" v-if="optType!=='view'">
                <el-button link type="primary" size="small" icon="Top" @click.stop="moveRouteProcess(idx, -1)" :disabled="idx===0" title="上移" />
                <el-button link type="primary" size="small" icon="Bottom" @click.stop="moveRouteProcess(idx, 1)" :disabled="idx===processGroupList.length-1" title="下移" />
                <el-button link type="danger" size="small" icon="Delete" @click.stop="removeRouteProcess(proc.processId)" title="移除此工序" />
              </span>
            </template>

            <!-- BOM 物料清单 -->
            <el-divider content-position="left">BOM 物料清单</el-divider>
            <el-row class="mb8" v-if="optType!=='view'">
              <el-col :span="1.5"><el-button type="primary" plain icon="Plus" size="small" @click="handleAddBom(proc.processId)">新增物料行</el-button></el-col>
            </el-row>
            <el-table :data="proc.bomItems" size="small" v-if="proc.bomItems.length>0">
              <el-table-column label="物料编码" align="center" prop="itemCode" width="120" />
              <el-table-column label="物料名称" align="center" prop="itemName" />
              <el-table-column label="类型" align="center" width="80"><template #default="scope">{{ itemOrProductMap[scope.row.itemOrProduct] || scope.row.itemOrProduct || '-' }}</template></el-table-column>
              <el-table-column label="单位用量" align="center" prop="quantity" width="100"><template #default="scope"><el-input-number v-if="optType!=='view'" v-model="scope.row.quantity" :min="0" :precision="4" size="small" controls-position="right" style="width:90px" /><span v-else>{{ scope.row.quantity }}</span></template></el-table-column>
              <el-table-column label="预计总用量" align="center" width="110"><template #default="scope">{{ (scope.row.quantity * (form.quantity||1)).toFixed(2) }} {{ scope.row.unitName }}</template></el-table-column>
              <el-table-column label="操作" align="center" width="80" v-if="optType!=='view'" class-name="small-padding fixed-width"><template #default="scope"><el-tooltip content="编辑" placement="top"><el-button link type="primary" icon="Edit" @click="handleEditBom(scope.row)"></el-button></el-tooltip><el-tooltip content="删除" placement="top"><el-button link type="primary" icon="Delete" @click="handleDelBom(scope.row)"></el-button></el-tooltip></template></el-table-column>
            </el-table>
            <el-empty v-else description="暂无物料" :image-size="40" />

            <!-- 工序参数 -->
            <el-divider content-position="left">工序参数</el-divider>
            <el-table :data="proc.paramItems" size="small" v-if="proc.paramItems.length>0">
              <el-table-column label="参数名称" align="center" prop="_paramName" width="120" />
              <el-table-column label="标准值" align="center" prop="standardValue" width="120" />
              <el-table-column label="调整值" align="center" prop="adjustedValue" width="120"><template #default="scope"><el-input v-if="optType!=='view'" v-model="scope.row.adjustedValue" size="small" /><span v-else>{{ scope.row.adjustedValue || scope.row.standardValue }}</span></template></el-table-column>
              <el-table-column label="调整原因" align="center" min-width="150"><template #default="scope"><el-input v-if="optType!=='view'" v-model="scope.row.remark" size="small" placeholder="调整原因" /><span v-else>{{ scope.row.remark }}</span></template></el-table-column>
            </el-table>
            <el-empty v-else description="暂无参数" :image-size="40" />
          </el-collapse-item>
        </el-collapse>
      </div>

      <template #footer>
        <div class="dialog-footer" v-if="optType!=='view'">
          <el-button @click="step>1?step--:cancel()" v-if="optType==='add' || optType==='edit'">{{ step===1?'取消':'上一步' }}</el-button>
          <el-button type="primary" @click="nextStep" v-if="step<2&&(optType==='add'||optType==='edit')">下一步</el-button>
          <el-button type="primary" @click="submitForm" v-if="step===2||optType==='edit'">确 定</el-button>
        </div>
        <div class="dialog-footer" v-else><el-button @click="cancel">关 闭</el-button></div>
      </template>
    </el-dialog>

    <!-- SKU变体创建询问弹窗（BOM + 工序参数偏离检测） -->
    <el-dialog title="检测到 BOM/参数偏离标准" v-model="skuDialogOpen" width="750px" append-to-body :close-on-click-modal="false">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom:16px">
        <template #title>以下物料或工序参数与标准不一致，可能产生新产品变体</template>
      </el-alert>
      <el-table :data="skuDeviationList" size="small" max-height="220">
        <el-table-column label="来源" align="center" width="65"><template #default="s"><el-tag size="small" :type="s.row.source==='BOM'?'':'info'">{{ s.row.source }}</el-tag></template></el-table-column>
        <el-table-column label="名称" align="center" prop="itemName" min-width="120" />
        <el-table-column label="编码" align="center" prop="itemCode" width="120" />
        <el-table-column label="标准值" align="center" width="90"><template #default="s">{{ s.row.standardVal }}<span v-if="s.row.unitName" style="font-size:11px;color:#909399"> {{ s.row.unitName }}</span></template></el-table-column>
        <el-table-column label="实际值" align="center" width="90"><template #default="s">{{ s.row.actualVal }}<span v-if="s.row.unitName" style="font-size:11px;color:#909399"> {{ s.row.unitName }}</span></template></el-table-column>
        <el-table-column label="差异" align="center" width="80"><template #default="s"><el-tag size="small" :type="s.row.diffType">{{ s.row.diffLabel }}</el-tag></template></el-table-column>
      </el-table>
      <p style="margin-top:16px;color:#606266">当前产品：<strong>{{ form.productName }} ({{ form.productCode }})</strong></p>
      <p style="color:#909399;font-size:13px">是否将此定制规格保存为新产品变体（SKU）？</p>
      <el-radio-group v-model="skuChoice" style="margin:12px 0;display:flex;flex-direction:column;gap:8px">
        <el-radio value="yes">
          <span style="font-weight:bold">是，创建新变体</span>
          <span style="color:#909399;font-size:12px;margin-left:8px">下次返单可直接选此变体，无需重新调整 BOM 和参数</span>
        </el-radio>
        <el-radio value="no">
          <span style="font-weight:bold">否，沿用原物料编码 {{ form.productCode }}</span>
          <span style="color:#909399;font-size:12px;margin-left:8px">本次仅做微调，不创建新品种</span>
        </el-radio>
      </el-radio-group>
      <el-row :gutter="12" v-if="skuChoice==='yes'" style="margin-top:12px">
        <el-col :span="12"><el-form-item label="变体编码"><el-input v-model="skuCode" placeholder="如 ZD-01-V1" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="变体名称"><el-input v-model="skuName" placeholder="如 奔趣纸袋-彩印红绳" /></el-form-item></el-col>
      </el-row>
      <template #footer>
        <el-button type="primary" @click="confirmSkuDialog" :disabled="skuChoice==='yes' && (!skuCode || !skuName)">确认，继续</el-button>
        <el-button @click="cancelSkuDialog">取消</el-button>
      </template>
    </el-dialog>

    <!-- BOM物料编辑弹窗 -->
    <el-dialog :title="bomEditTitle" v-model="bomEditOpen" width="500px" append-to-body>
      <el-form ref="bomEditForm" :model="bomEditForm" label-width="100px">
        <el-form-item label="所属工序"><el-select v-model="bomEditForm._processId" style="width:100%" placeholder="请选择"><el-option v-for="p in (routeProcesses.length ? routeProcesses : processOptions)" :key="p.processId" :label="p.processName" :value="p.processId" /></el-select></el-form-item>
        <el-form-item label="物料"><el-row :gutter="8"><el-col :span="18"><el-input v-model="bomEditForm.itemName" placeholder="请选择物料" disabled /></el-col><el-col :span="6"><el-button type="primary" icon="Search" size="small" @click="$refs.bomItemSelectRef.open()">选择</el-button></el-col></el-row></el-form-item>
        <el-form-item label="物料类型"><el-select v-model="bomEditForm.itemOrProduct" style="width:100%"><el-option label="原料" value="RAW" /><el-option label="半成品" value="SEMI" /><el-option label="成品" value="FINISHED" /><el-option label="辅料" value="AUXILIARY" /><el-option label="包材" value="PACK" /></el-select></el-form-item>
        <el-form-item label="单位用量"><el-input-number v-model="bomEditForm.quantity" :min="0" :precision="4" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="confirmBomEdit">确 定</el-button><el-button @click="bomEditOpen=false">取 消</el-button></template>
    </el-dialog>

    <!-- 排产弹窗：工单 → 工序步骤 → 每步骤管理任务 -->
    <el-dialog :title="'排产 — ' + scheduleWorkorderName" v-model="scheduleOpen" width="1100px" append-to-body @close="scheduleOpen=false">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-descriptions title="工单信息" :column="1" size="small" border>
            <el-descriptions-item label="工单编码">{{ scheduleWorkorderCode }}</el-descriptions-item>
            <el-descriptions-item label="产品">{{ scheduleProductName }}</el-descriptions-item>
            <el-descriptions-item label="计划数量">{{ scheduleQuantity }} {{ scheduleUnitName }}</el-descriptions-item>
            <el-descriptions-item label="已排产">{{ scheduleQuantityScheduled }}</el-descriptions-item>
            <el-descriptions-item label="已生产">{{ scheduleQuantityProduced }}</el-descriptions-item>
          </el-descriptions>
        </el-col>
        <el-col :span="16">
          <el-steps :active="scheduleActiveStep" align-center simple style="margin-bottom: 12px; cursor: pointer">
            <el-step v-for="(item, idx) in scheduleSteps" :key="item.processId" :title="item.processName" @click="handleScheduleStepClick(idx)" />
          </el-steps>
          <div v-if="scheduleSteps.length>0" v-for="(step, idx) in scheduleSteps" :key="step.processId" v-show="scheduleActiveStep===idx">
            <el-row class="mb8">
              <el-col :span="1.5"><el-button type="primary" plain icon="Plus" size="small" @click="handleScheduleAddTask(step)" v-hasPermi="['mes:pro:task:add']">新增任务</el-button></el-col>
            </el-row>
            <el-table :data="scheduleTasksByProcess[step.processId] || []" size="small">
              <el-table-column label="任务编码" prop="taskCode" width="130" :show-overflow-tooltip="true" />
              <el-table-column label="任务名称" prop="taskName" :show-overflow-tooltip="true" min-width="150" />
              <el-table-column label="工作站" prop="workstationName" width="100" />
              <el-table-column label="排产数量" prop="quantity" width="90" />
              <el-table-column label="已生产" prop="quantityProduced" width="80" />
              <el-table-column label="开始时间" width="100"><template #default="s">{{ parseTime(s.row.startTime, '{y}-{m}-{d}') }}</template></el-table-column>
              <el-table-column label="时长(h)" prop="duration" width="70" />
              <el-table-column label="状态" width="70"><template #default="s"><span :style="{color:scheduleStatusColor[s.row.status]}">{{ scheduleStatusMap[s.row.status]||s.row.status }}</span></template></el-table-column>
              <el-table-column label="操作" width="80" class-name="small-padding fixed-width">
                <template #default="s">
                  <el-tooltip content="编辑"><el-button link type="primary" icon="Edit" size="small" @click="handleScheduleEditTask(s.row)"></el-button></el-tooltip>
                  <el-tooltip content="删除"><el-button link type="primary" icon="Delete" size="small" @click="handleScheduleDelTask(s.row)"></el-button></el-tooltip>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!(scheduleTasksByProcess[step.processId]||[]).length" description="该工序暂无排产任务" :image-size="60" />
          </div>
          <el-empty v-if="scheduleSteps.length===0" description="该工单产品未配置工艺路线，无法排产" :image-size="80" />
        </el-col>
      </el-row>
      <template #footer><el-button @click="scheduleOpen=false">关 闭</el-button></template>
    </el-dialog>

    <!-- 排产任务编辑弹窗（在排产对话框内） -->
    <el-dialog :title="scheduleTaskTitle" v-model="scheduleTaskOpen" width="600px" append-to-body>
      <el-form ref="scheduleTaskForm" :model="scheduleTaskForm" label-width="100px">
        <el-row><el-col :span="18"><el-form-item label="工作站" prop="workstationName"><el-input v-model="scheduleTaskForm.workstationName" placeholder="请选择工作站" readonly><template #append><el-button icon="Search" @click="handleOpenWorkstationSelect" /></template></el-input></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="排产数量" prop="quantity"><el-input-number v-model="scheduleTaskForm.quantity" :min="1" style="width:100%" /></el-form-item></el-col></el-row>
        <el-row><el-col :span="12"><el-form-item label="开始时间"><el-date-picker v-model="scheduleTaskForm.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="时长(小时)" prop="duration"><el-input-number v-model="scheduleTaskForm.duration" :min="1" style="width:100%" /></el-form-item></el-col></el-row>
        <el-row><el-col :span="12"><el-form-item label="机台号"><el-input v-model="scheduleTaskForm.machineCode" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="结束时间"><el-date-picker v-model="scheduleTaskForm.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item></el-col></el-row>
        <el-row><el-col :span="24"><el-form-item label="备注"><el-input v-model="scheduleTaskForm.remark" /></el-form-item></el-col></el-row>
      </el-form>
      <template #footer><el-button type="primary" @click="submitScheduleTask">确 定</el-button><el-button @click="scheduleTaskOpen=false">取 消</el-button></template>
    </el-dialog>

    <!-- 物料齐套检查弹窗 -->
    <el-dialog :title="'物料齐套检查 — ' + materialCheckWorkorderName" v-model="materialCheckOpen" width="800px" append-to-body>
      <el-alert :title="materialCheckPassed ? '所有物料齐套，可以开工！' : '以下物料缺料，暂不可开工'" :type="materialCheckPassed ? 'success' : 'warning'" :closable="false" show-icon style="margin-bottom:12px" />
      <el-table :data="materialCheckList" size="small">
        <el-table-column label="物料编码" align="center" prop="itemCode" width="120" />
        <el-table-column label="物料名称" align="center" prop="itemName" :show-overflow-tooltip="true" />
        <el-table-column label="需求数量" align="center" width="100"><template #default="scope">{{ scope.row.requiredQty }} {{ scope.row.unitName }}</template></el-table-column>
        <el-table-column label="可用库存" align="center" width="100"><template #default="scope">{{ scope.row.availableQty }} {{ scope.row.unitName }}</template></el-table-column>
        <el-table-column label="状态" align="center" width="80"><template #default="scope"><el-tag :type="scope.row.sufficient ? 'success' : 'danger'" size="small">{{ scope.row.sufficient ? '充足' : '缺料' }}</el-tag></template></el-table-column>
        <el-table-column label="缺口" align="center" width="100"><template #default="scope"><span :style="{color:scope.row.sufficient?'#67C23A':'#F56C6C'}">{{ scope.row.shortageQty }} {{ scope.row.unitName }}</span></template></el-table-column>
      </el-table>
      <template #footer>
        <el-button type="primary" @click="confirmStart" :disabled="!materialCheckPassed" v-if="materialCheckWorkorderStatus==='PREPARE'">确认开工</el-button>
        <el-button @click="materialCheckOpen=false">关 闭</el-button>
      </template>
    </el-dialog>

    <!-- 开工检查流程弹窗 -->
    <el-dialog :title="'开工检查 — ' + startCheckWorkorderName" v-model="startCheckOpen" width="850px" append-to-body @close="startCheckOpen=false" :close-on-click-modal="false">
      <!-- 步骤条 -->
      <el-steps :active="startCheckActiveStep" finish-status="success" align-center style="margin-bottom:20px">
        <el-step v-for="(s, i) in startCheckSteps" :key="i" :title="s.name" :status="stepIconStatus(s.status)" />
      </el-steps>

      <!-- 加载提示 -->
      <div v-if="startCheckRunning" style="text-align:center;padding:30px">
        <i class="el-icon-loading" style="font-size:36px;color:#409EFF" />
        <p style="margin-top:12px;color:#909399;font-size:14px">正在执行开工检查，请稍候...</p>
      </div>

      <!-- 当前步骤结果 -->
      <div v-if="!startCheckRunning && startCheckActiveStep < 4">
        <div v-for="(s, i) in startCheckSteps" :key="i">
          <div v-if="i === startCheckActiveStep || s.status === 'error' || s.status === 'success'">
            <el-alert :title="s.message" :type="s.status==='success'?'success':(s.status==='error'?'error':'info')" :closable="false" show-icon style="margin-bottom:12px" />

            <!-- Step 1: 物料齐套明细 -->
            <el-table v-if="i===0 && s.details && s.details.length>0" :data="s.details" size="small" max-height="300">
              <el-table-column label="物料编码" align="center" prop="itemCode" width="120" />
              <el-table-column label="物料名称" align="center" prop="itemName" :show-overflow-tooltip="true" />
              <el-table-column label="需求数量" align="center" width="100"><template #default="scope">{{ scope.row.requiredQty }} {{ scope.row.unitName }}</template></el-table-column>
              <el-table-column label="可用库存" align="center" width="100"><template #default="scope">{{ scope.row.availableQty }} {{ scope.row.unitName }}</template></el-table-column>
              <el-table-column label="状态" align="center" width="80"><template #default="scope"><el-tag :type="scope.row.sufficient ? 'success' : 'danger'" size="small">{{ scope.row.sufficient ? '充足' : '缺料' }}</el-tag></template></el-table-column>
              <el-table-column label="缺口" align="center" width="100"><template #default="scope"><span :style="{color:scope.row.sufficient?'#67C23A':'#F56C6C'}">{{ scope.row.shortageQty }} {{ scope.row.unitName }}</span></template></el-table-column>
            </el-table>

            <!-- Step 2: 缺排产工序列表 -->
            <el-table v-if="i===1 && s.details && s.details.length>0 && s.status==='error'" :data="s.details" size="small">
              <el-table-column label="工序编码" align="center" prop="processCode" width="130" />
              <el-table-column label="工序名称" align="center" prop="processName" />
              <el-table-column label="工序序号" align="center" prop="orderNum" width="90" />
            </el-table>

            <!-- Step 3: 生成的领料单列表 -->
            <el-table v-if="i===2 && s.details && s.details.length>0" :data="s.details" size="small">
              <el-table-column label="领料单编码" align="center" prop="issueCode" width="180" />
              <el-table-column label="工序名称" align="center" prop="processName" width="130" />
              <el-table-column label="物料行数" align="center" prop="lineCount" width="90" />
              <el-table-column label="关联排产任务" align="center" prop="taskId" width="120"><template #default="scope">{{ scope.row.taskId || '-' }}</template></el-table-column>
            </el-table>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button type="primary" @click="startCheckOpen=false">{{ startCheckAllPassed ? '完 成' : '关 闭' }}</el-button>
      </template>
    </el-dialog>

    <ItemSelect ref="itemSelectRef" @onSelected="onProductSelected" />
    <ItemSelect ref="bomItemSelectRef" @onSelected="onBomItemSelected" />
    <WorkstationSelect ref="wsSelectRef" :processId="scheduleTaskForm.processId" @onSelected="onScheduleWorkstationSelected" />

    <!-- 添加工序选择弹窗 -->
    <el-dialog title="添加工序" v-model="showProcessSelector" width="600px" append-to-body>
      <el-table :data="availableProcesses" highlight-current-row @row-dblclick="addRouteProcessByRow">
        <el-table-column label="工序编码" prop="processCode" width="130" />
        <el-table-column label="工序名称" prop="processName" />
        <el-table-column label="工序类型" width="80"><template #default="s">{{ processTypeMap[s.row.processType]||s.row.processType }}</template></el-table-column>
      </el-table>
      <template #footer><el-button type="primary" @click="showProcessSelector=false">关 闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script>
import { listWorkorder, getWorkorderDetail, delWorkorder, createWorkorderWithBom, updateWorkorderWithBom, startWorkorder, checkWorkorderMaterial, startWorkorderWithCheck, checkDeviation } from '@/api/mes/pro/workorder'
import { listRouteProduct } from '@/api/mes/pro/routeproduct'
import { listRouteProcessByRouteId } from '@/api/mes/pro/routeprocess'
import { listRoute } from '@/api/mes/pro/proroute'
import { genSerialCode } from '@/api/mes/sys/autocoderule'
import ItemSelect from '@/components/itemSelect/single.vue'
import WorkstationSelect from '@/components/workstationSelect/single.vue'
import { listAllProcess } from '@/api/mes/pro/process'

export default {
  name: 'Workorder',
  components: { ItemSelect, WorkstationSelect },
  data() {
    return {
      autoGenFlag: false, optType: undefined, step: 1, prorouteId: null,
      loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0,
      workorderList: [], routeOptions: [], bomList: [], paramList: [], processOptions: [], routeProcesses: [],
      activeProcesses: [], showProcessSelector: false,
      title: '', open: false,
      bomEditOpen: false, bomEditTitle: '', bomEditForm: {},
      // 排产对话框
      scheduleOpen: false, scheduleActiveStep: 0, scheduleSteps: [], scheduleTasksByProcess: {},
      scheduleWorkorderId: null, scheduleWorkorderCode: '', scheduleWorkorderName: '',
      scheduleProductName: '', scheduleQuantity: 0, scheduleUnitName: '',
      scheduleQuantityProduced: 0, scheduleQuantityScheduled: 0,
      scheduleTaskOpen: false, scheduleTaskTitle: '', scheduleTaskForm: {}, scheduleEditTaskId: null,
      scheduleStatusMap: { PREPARE:'待排产',NORMAL:'正常',PRODUCING:'生产中',COMPLETED:'已完成',PAUSED:'暂停',CANCEL:'取消' },
      scheduleStatusColor: { PREPARE:'#E6A23C',NORMAL:'#409EFF',PRODUCING:'#67C23A',COMPLETED:'#909399',PAUSED:'#E6A23C',CANCEL:'#F56C6C' },
      // 物料齐套检查
      materialCheckOpen: false, materialCheckWorkorderId: null, materialCheckWorkorderName: '', materialCheckWorkorderStatus: '',
      materialCheckList: [], materialCheckPassed: false,
      // 开工检查流程
      startCheckOpen: false, startCheckWorkorderId: null, startCheckWorkorderName: '',
      startCheckSteps: [
        { name: '物料齐套检查', status: 'wait', message: '', details: [] },
        { name: '排产检查', status: 'wait', message: '', details: [] },
        { name: '生成领料单', status: 'wait', message: '', details: [] },
        { name: '确认开工', status: 'wait', message: '', details: [] },
      ],
      startCheckRunning: false, startCheckAllPassed: false, startCheckActiveStep: 0,
      statusMap: { PREPARE: '待生产', PRODUCING: '生产中', COMPLETED: '已完成', CANCEL: '已取消', CLOSED: '已关闭' },
      statusColor: { PREPARE: '#E6A23C', PRODUCING: '#409EFF', COMPLETED: '#67C23A', CANCEL: '#909399', CLOSED: '#909399' },
      itemOrProductMap: { RAW: '原料', SEMI: '半成品', FINISHED: '成品', AUXILIARY: '辅料', PACK: '包材' },
      processTypeMap: { INTERNAL: '自制', OUTSOURCE: '外发', SLITTING: '分切' },
      // SKU变体对话框
      skuDialogOpen: false, skuChoice: '', skuCode: '', skuName: '', skuDeviationList: [],
      queryParams: { pageNum: 1, pageSize: 10, workorderCode: null, productName: null, status: null },
      form: {},
      rules: {
        workorderCode: [{ required: true, message: '编码不能为空', trigger: 'blur' }],
        workorderName: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
        productId: [{ required: true, message: '请选择产品', trigger: 'change' }],
        quantity: [{ required: true, message: '数量不能为空', trigger: 'blur' }],
      },
    }
  },
  computed: {
    productDisplayName() { const f=this.form; return f.productName? `${f.productName} (${f.productCode||''})` : '' },
    /** 可选工序 = 全部工序 - 已在路线中的工序 */
    availableProcesses() {
      const inRoute = new Set(this.routeProcesses.map(p=>p.processId))
      return (this.processOptions||[]).filter(p=>!inRoute.has(p.processId))
    },
    processGroupList() {
      const map = {}
      const ensure = (pid, pname) => {
        const key = pid || '__none__'
        if (!map[key]) map[key] = { _key: key, processId: pid, processName: pname || '未分配工序', bomItems: [], paramItems: [] }
        return map[key]
      }
      // 以工艺路线工序为骨架，BOM/参数只能挂到路线内的工序上
      const routePids = new Set(this.routeProcesses.map(p => p.processId))
      this.routeProcesses.forEach(p => {
        ensure(p.processId, p.processName)
      })
      // 附着 BOM（只挂路线内工序）
      this.bomList.forEach(b => {
        const pid = b.processId || b._processId
        if (routePids.has(pid)) {
          ensure(pid, b.processName || b._processName || this.getProcessName(pid)).bomItems.push(b)
        }
      })
      // 附着参数（只挂路线内工序）
      this.paramList.forEach(p => {
        const pid = p.processId
        if (routePids.has(pid)) {
          ensure(pid, p._processName || p.processName || this.getProcessName(pid)).paramItems.push(p)
        }
      })
      // 按路线工序的 order_num 排序
      const orderMap = {}
      this.routeProcesses.forEach(p => { orderMap[p.processId] = p.orderNum || 99 })
      return Object.values(map).sort((a, b) => (orderMap[a.processId] || 99) - (orderMap[b.processId] || 99))
    },
  },
  created() { this.getList(); listAllProcess().then(r=>{ this.processOptions=r.data||[] }) },
  methods: {
    getList() { this.loading=true; listWorkorder(this.queryParams).then(r=>{ this.workorderList=r.rows; this.total=r.total; this.loading=false }) },
    cancel() { this.open=false; this.reset() },
    reset() { this.form={ workorderId:null, workorderCode:null, workorderName:null, workorderType:'SELF', orderSource:'MANUAL', productId:null, productCode:null, productName:null, productSpc:null, unitOfMeasure:'PCS', unitName:'个', quantity:1, status:'PREPARE', clientOrderCode:null, orderType:'NEW', productSize:null, ropeSpec:null, printingReq:null, packageReq:null, requestDate:null, remark:null }; this.autoGenFlag=false; this.step=1; this.prorouteId=null; this.bomList=[]; this.paramList=[]; this.routeProcesses=[]; this.routeOptions=[]; this.showProcessSelector=false },
    handleQuery() { this.queryParams.pageNum=1; this.getList() },
    resetQuery() { this.$refs.queryForm?.resetFields(); this.handleQuery() },
    handleSelectionChange(sel) { this.ids=sel.map(i=>i.workorderId); this.single=sel.length!==1; this.multiple=!sel.length },
    handleAdd() { this.reset(); this.open=true; this.title='新增生产工单'; this.optType='add'; this.step=1 },
    // 查看：后端一次返回全部数据，前端直接展示
    handleView(row) { this.reset(); this.loadDetail(row.workorderId).then(() => { this.open=true; this.title='查看生产工单'; this.optType='view' }) },
    // 修改：后端一次返回全部数据，前端直接展示（step 由 reset 初始化为 1）
    handleUpdate(row) { this.reset(); const id=row.workorderId||this.ids[0]; this.loadDetail(id).then(() => { this.open=true; this.optType = this.form.status==='PREPARE' ? 'edit' : 'view'; this.title = this.optType==='edit' ? '修改生产工单' : '查看生产工单' }) },
    // 合并查询：一次请求获取工单头+BOM+参数+路线工序+路线选项（后端已全部组装）
    loadDetail(workorderId) {
      return getWorkorderDetail(workorderId).then(r => {
        const { workorder, bomList, paramList, routeProcesses, routeOptions } = r.data
        this.form = workorder
        this.bomList = (bomList || []).map(b => ({ ...b, _processId: b.processId, _processName: b.processName }))
        this.paramList = paramList || []   // 后端已富化
        this.routeProcesses = routeProcesses || []
        this.routeOptions = routeOptions || []
        this.prorouteId = workorder.routeProductId
        return r.data
      })
    },
    handleAutoGenChange(f) {
      if (f) {
        genSerialCode('WORKORDER_NO').then(r => { this.form.workorderCode = r.data })
      } else {
        this.form.workorderCode = null
      }
    },
    // Product selection
    onProductSelected(row) {
      this.form.productId=row.itemId; this.form.productCode=row.itemCode; this.form.productName=row.itemName
      this.form.productSpc=row.specification; this.form.unitOfMeasure=row.unitOfMeasure; this.form.unitName=row.unitName
      this.prorouteId=null; this.routeOptions=[]
      // Load route products for this item, then enrich with route names
      listRouteProduct({ itemId: row.itemId, pageSize: 100 }).then(r=>{
        if(r.rows&&r.rows.length>0){
          const rows = r.rows
          // 批量加载路线名，用于下拉区分同产品不同路线
          listRoute({ pageSize: 1000 }).then(routeRes => {
            const routeMap = {}
            ;(routeRes.rows || []).forEach(rt => { routeMap[rt.routeId] = rt.routeName || rt.routeCode || '路线#' + rt.routeId })
            this.routeOptions = rows.map(rp => ({ ...rp, _routeName: routeMap[rp.routeId] || '路线#' + rp.routeId }))
          })
        }
      })
    },
    onRouteChange(recordId) {
      const rp = this.routeOptions.find(r=>r.recordId===recordId)
      if(!rp) return
      // 加载路线工序（编辑和新增都需要，用于 Step 2 的 accordion 骨架）
      listRouteProcessByRouteId(rp.routeId).then(res=>{
        this.routeProcesses = res.data||[]
        const routePids = new Set(this.routeProcesses.map(p=>p.processId))

        // 新增模式：从工艺路线模板加载 BOM 和参数
        if (this.optType === 'add') {
          import('@/api/mes/pro/routeproductbom').then(m=>{
            m.listRouteProductBomByRouteId(rp.routeId).then(r=>{
              this.bomList = (r.data||[]).filter(b=>b.productId===rp.itemId && routePids.has(b.processId)).map(b=>({...b, _processId:b.processId, _processName:this.getProcessName(b.processId)}))
            })
          })
          import('@/api/mes/pro/routeprocessparam').then(m=>{
            m.listRouteProcessParamByRouteProductId(recordId).then(r=>{
              const l2Params = r.data || []
              import('@/api/mes/pro/paramtemplate').then(tm=>{
                tm.listParamTemplate({ pageSize: 1000 }).then(tmplRes => {
                  const allTemplates = tmplRes.rows || []
                  const routeTemplates = allTemplates.filter(t=>routePids.has(t.processId))
                  const templateMap = {}
                  allTemplates.forEach(t => { templateMap[t.templateId] = t })
                  const genParams = (l2Params.length > 0)
                    ? l2Params.map(p => {
                        const tmpl = templateMap[p.templateId] || {}
                        const stdVal = p.paramValue || tmpl.defaultValue || ''
                        return { ...p, _processName: this.getProcessName(p.processId), _paramName: tmpl.paramName || '', standardValue: stdVal, adjustedValue: '', paramName: tmpl.paramName || '' }
                      })
                    : routeTemplates.map(t => ({
                        routeProductId: recordId, processId: t.processId, templateId: t.templateId,
                        _processName: this.getProcessName(t.processId), _paramName: t.paramName || '',
                        standardValue: t.defaultValue || '', adjustedValue: '', paramName: t.paramName || '',
                      }))
                  // 合并已有 recordId
                  if (this.paramList.length > 0) {
                    const oldMap = {}; this.paramList.forEach(p => { if (p.recordId) oldMap[p.templateId] = p.recordId })
                    genParams.forEach(p => { if (oldMap[p.templateId]) p.recordId = oldMap[p.templateId] })
                  }
                  this.paramList = genParams
                })
              })
            })
          })
        }
        // 编辑/查看：BOM/参数已由 loadDetail 加载，processGroupList 自动按路线工序过滤显示
      })
    },
    getProcessName(pid) { const p=this.processOptions.find(i=>i.processId===pid) || this.routeProcesses.find(i=>i.processId===pid); return p?p.processName:pid },
    // BOM CRUD
    handleAddBom(processId) { this._bomEditIdx=-1; const proc=this.processGroupList.find(p=>p.processId===processId); this.bomEditForm={ _processId:processId||null, _processName:proc?proc.processName:'', itemId:null, itemCode:null, itemName:null, itemOrProduct:'RAW', quantity:0, unitOfMeasure:null, unitName:null }; this.bomEditTitle='新增物料行'; this.bomEditOpen=true },
    handleEditBom(row) { this._bomEditIdx=this.bomList.indexOf(row); this.bomEditForm={...row}; this.bomEditTitle='编辑物料行'; this.bomEditOpen=true },
    handleDelBom(row) { const idx=this.bomList.indexOf(row); if(idx>=0) this.bomList.splice(idx,1) },
    // ── 工序管理：增加/删除/排序 ──
    addRouteProcessByRow(row) {
      if (!row || !row.processId) return
      if (this.routeProcesses.find(p => p.processId === row.processId)) return
      this.routeProcesses.push({ processId: row.processId, processName: row.processName, processCode: row.processCode, processType: row.processType, orderNum: this.routeProcesses.length + 1 })
      this.showProcessSelector = false
    },
    removeRouteProcess(processId) {
      this.routeProcesses = this.routeProcesses.filter(p => p.processId !== processId)
      // 同时移除该工序下的 BOM 和参数
      this.bomList = this.bomList.filter(b => (b.processId||b._processId) !== processId)
      this.paramList = this.paramList.filter(p => p.processId !== processId)
    },
    moveRouteProcess(idx, direction) {
      const arr = this.routeProcesses
      const newIdx = idx + direction
      if (newIdx < 0 || newIdx >= arr.length) return
      const item = arr.splice(idx, 1)[0]
      arr.splice(newIdx, 0, item)
      // 更新 orderNum
      arr.forEach((p, i) => { p.orderNum = i + 1 })
    },
    onBomItemSelected(row) { this.bomEditForm.itemId=row.itemId; this.bomEditForm.itemCode=row.itemCode; this.bomEditForm.itemName=row.itemName; this.bomEditForm.unitOfMeasure=row.unitOfMeasure; this.bomEditForm.unitName=row.unitName },
    confirmBomEdit() {
      const f=this.bomEditForm; f._processName=this.getProcessName(f._processId)
      if(this._bomEditIdx>=0) this.bomList.splice(this._bomEditIdx,1,f); else this.bomList.push({...f})
      this.bomEditOpen=false
    },
    // Step navigation
    nextStep() {
      if(this.step===1) { this.$refs.form.validate(v=>{ if(v){ if(!this.prorouteId&&!this.form.workorderId){ this.$modal.msgWarning('请选择工艺路线'); return } this.step=2 } }) }
    },
    submitForm() {
      // 两次请求：① 后端偏离检测 → ② 弹窗确认 → ③ 提交
      if (this.optType === 'add' || this.optType === 'edit') {
        const payload = this.buildPayload()
        checkDeviation(payload).then(res => {
          const result = res.data
          if (result.hasDeviation && result.deviations && result.deviations.length > 0) {
            this.skuDeviationList = result.deviations
            this.skuChoice = ''
            // 自动生成递增的变体编码（调用后端 genSerialCode, FIN-BENQU-001-V1 → FIN-BENQU-001-V2...）
            genSerialCode('SKU_CODE', this.form.productCode + '-V').then(r => {
              this.skuCode = (r.data || '').trim()
            })
            this.skuName = ''
            this.skuDialogOpen = true
          } else {
            this.doSubmitForm()
          }
        }).catch((e) => {
          // API 异常时兜底：提示用户后直接提交
          console.error('checkDeviation failed:', e)
          this.$modal.msgWarning('偏离检测服务暂不可用，将直接提交工单')
          this.doSubmitForm()
        })
        return
      }
      this.doSubmitForm()
    },
    /** 构建向后端提交的 payload（BOM + params 标准化） */
    buildPayload() {
      return {
        workorder: { ...this.form, routeProductId: this.prorouteId || this.form.routeProductId },
        bomList: this.bomList.map(bom => ({
          processId: bom.processId || bom._processId || null,
          processName: bom.processName || bom._processName || '',
          itemId: bom.itemId, itemCode: bom.itemCode || '', itemName: bom.itemName || '',
          itemSpc: bom.itemSpc || '', unitOfMeasure: bom.unitOfMeasure || '',
          unitName: bom.unitName || '', itemOrProduct: bom.itemOrProduct || 'RAW',
          quantity: bom.quantity || 0
        })),
        paramList: this.paramList.map(p => ({
          recordId: p.recordId || null,
          routeProductId: p.routeProductId || null,
          templateId: p.templateId || null,
          standardValue: p.standardValue || '',
          adjustedValue: p.adjustedValue || '',
          remark: p.remark || ''
        }))
      }
    },
    /** SKU对话框——确认 */
    confirmSkuDialog() {
      if (this.skuChoice === 'yes') {
        if (!this.skuCode) { this.$modal.msgWarning('请输入变体编码'); return }
        if (!this.skuName) { this.$modal.msgWarning('请输入变体名称'); return }
      }
      this.skuDialogOpen = false
      if (this.skuChoice === 'yes') {
        this.form.createSkuVariant = true
        this.form.skuCode = this.skuCode
        this.form.skuName = this.skuName
      } else {
        this.form.createSkuVariant = false
      }
      this.doSubmitForm()
    },
    /** SKU对话框——取消（关闭弹窗，回到Step2） */
    cancelSkuDialog() {
      this.skuDialogOpen = false
    },
    /** 实际执行工单提交 */
    doSubmitForm() {
      const payload = this.buildPayload()
      const apiCall = this.optType === 'add'
        ? createWorkorderWithBom(payload)
        : updateWorkorderWithBom(payload)
      apiCall.then(() => {
        this.$modal.msgSuccess((this.optType === 'add' ? '新增' : '修改') + '成功')
        this.open = false; this.getList()
      }).catch(() => {})
    },
    handleDelete(row) {
      const ids=row.workorderId||this.ids.join(',')
      this.$modal.confirm('确认删除工单"'+ids+'"？').then(()=>delWorkorder(ids)).then(()=>{ this.getList(); this.$modal.msgSuccess('删除成功') }).catch(()=>{})
    },
    // 物料齐套检查
    handleCheckMaterial(row) {
      this.materialCheckWorkorderId = row.workorderId
      this.materialCheckWorkorderName = row.workorderName
      this.materialCheckWorkorderStatus = row.status
      this.materialCheckList = []
      this.materialCheckPassed = false
      checkWorkorderMaterial(row.workorderId).then(r => {
        this.materialCheckList = r.data || []
        this.materialCheckPassed = this.materialCheckList.every(item => item.sufficient)
        this.materialCheckOpen = true
      }).catch(() => {})
    },
    // 开工 — 打开分步检查弹窗
    handleStart(row) {
      this.startCheckWorkorderId = row.workorderId
      this.startCheckWorkorderName = row.workorderName
      // 重置步骤状态
      this.startCheckSteps = [
        { name: '物料齐套检查', status: 'wait', message: '', details: [] },
        { name: '排产检查', status: 'wait', message: '', details: [] },
        { name: '生成领料单', status: 'wait', message: '', details: [] },
        { name: '确认开工', status: 'wait', message: '', details: [] },
      ]
      this.startCheckRunning = true
      this.startCheckAllPassed = false
      this.startCheckActiveStep = 0
      this.startCheckOpen = true
      // 调用后端一键执行全流程
      startWorkorderWithCheck(row.workorderId).then(r => {
        const steps = r.data || []
        this.startCheckActiveStep = 0
        // 逐步骤更新状态
        steps.forEach((s, i) => {
          if (i < 4) {
            this.startCheckSteps[i].status = s.status === 'PASS' ? 'success' : (s.status === 'FAIL' ? 'error' : 'wait')
            this.startCheckSteps[i].message = s.message || ''
            this.startCheckSteps[i].details = s.details || []
          }
        })
        // 找到第一个失败的步骤作为 active step，全通过则显示最后一步
        const failIdx = steps.findIndex(s => s.status === 'FAIL')
        this.startCheckActiveStep = failIdx >= 0 ? failIdx : Math.min(steps.length, 3)
        this.startCheckAllPassed = steps.length === 4 && steps.every(s => s.status === 'PASS')
      }).catch(() => {
        // 网络异常等
        this.startCheckSteps.forEach(s => { if (s.status === 'wait') s.status = 'error'; s.message = s.message || '执行异常，请稍后重试' })
        this.startCheckActiveStep = 3
      }).finally(() => {
        this.startCheckRunning = false
        if (this.startCheckAllPassed) {
          this.getList()
        }
      })
    },
    // 步骤图标状态映射
    stepIconStatus(status) {
      return status === 'success' ? 'success' : (status === 'error' ? 'error' : status === 'wait' ? 'wait' : 'finish')
    },
    // 排产步骤切换
    handleScheduleStepClick(idx) {
      this.scheduleActiveStep = idx
    },
    // 排产：打开排产对话框，按工序步骤管理任务
    handleSchedule(row) {
      this.scheduleWorkorderId = row.workorderId
      this.scheduleWorkorderCode = row.workorderCode
      this.scheduleWorkorderName = row.workorderName
      this.scheduleProductName = row.productName
      this.scheduleQuantity = row.quantity
      this.scheduleUnitName = row.unitName
      this.scheduleQuantityProduced = row.quantityProduced || 0
      this.scheduleQuantityScheduled = row.quantityScheduled || 0
      this.scheduleSteps = []
      this.scheduleTasksByProcess = {}
      this.scheduleActiveStep = 0
      this.scheduleOpen = true
      this.loadScheduleData(row)
    },
    // 加载排产数据：工序步骤 + 已有任务
    loadScheduleData(row) {
      // 加载已有任务（不与路线工序耦合）
      this.loadScheduleTasks()
      // 加载路线工序（用于步骤导航）
      if (row.routeProductId) {
        listRouteProduct({ pageSize: 100 }).then(rpRes => {
          const rpList = rpRes.rows || []
          const rp = rpList.find(p => p.recordId === row.routeProductId)
          if (rp && rp.routeId) {
            listRouteProcessByRouteId(rp.routeId).then(procRes => {
              this.scheduleSteps = (procRes.data || []).map(p => ({
                processId: p.processId, processName: p.processName || p.processCode,
                processCode: p.processCode, processType: p.processType, routeId: rp.routeId, colorCode: p.colorCode || '#00AEF3'
              }))
            })
          }
        })
      }
    },
    loadScheduleTasks() {
      if (!this.scheduleWorkorderId) return
      import('@/api/mes/pro/task').then(m => {
        m.listTask({ workorderId: this.scheduleWorkorderId, pageSize: 1000 }).then(res => {
          const tasks = res.rows || []
          const byProcess = {}
          tasks.forEach(t => {
            const pid = t.processId || '__none__'
            if (!byProcess[pid]) byProcess[pid] = []
            byProcess[pid].push(t)
          })
          this.scheduleTasksByProcess = byProcess
        })
      })
    },
    // 打开工作站选择器，带入当前工序筛选条件
    handleOpenWorkstationSelect() {
      this.$refs.wsSelectRef.open(this.scheduleTaskForm.processId)
    },
    // 工作站选择回调
    onScheduleWorkstationSelected(row) {
      this.scheduleTaskForm.workstationId = row.workstationId
      this.scheduleTaskForm.workstationCode = row.workstationCode
      this.scheduleTaskForm.workstationName = row.workstationName
    },
    // 排产任务 CRUD
    handleScheduleAddTask(step) {
      this.scheduleTaskForm = {
        workorderId: this.scheduleWorkorderId, workorderCode: this.scheduleWorkorderCode, workorderName: this.scheduleWorkorderName,
        processId: step.processId, processCode: step.processCode, processName: step.processName, processType: step.processType,
        routeId: step.routeId, quantity: 1, duration: 1, setupDuration: 0, unitDuration: 0, offlineQty: 0
      }
      this.scheduleEditTaskId = null
      this.scheduleTaskTitle = '新增排产任务 — ' + step.processName
      this.scheduleTaskOpen = true
    },
    handleScheduleEditTask(row) {
      this.scheduleTaskForm = { ...row }
      this.scheduleEditTaskId = row.taskId
      this.scheduleTaskTitle = '修改排产任务'
      this.scheduleTaskOpen = true
    },
    handleScheduleDelTask(row) {
      this.$modal.confirm('确认删除任务"' + (row.taskName || row.taskCode) + '"？').then(() => {
        import('@/api/mes/pro/task').then(m => m.delTask(row.taskId)).then(() => {
          this.$modal.msgSuccess('删除成功')
          this.loadScheduleTasks()
        })
      }).catch(() => {})
    },
    submitScheduleTask() {
      import('@/api/mes/pro/task').then(m => {
        const action = this.scheduleEditTaskId ? m.updateTask(this.scheduleTaskForm) : m.addTask(this.scheduleTaskForm)
        action.then(() => {
          this.$modal.msgSuccess(this.scheduleEditTaskId ? '修改成功' : '新增成功')
          this.scheduleTaskOpen = false
          this.loadScheduleTasks()
        }).catch(() => {})
      })
    },
    // 齐套检查通过后确认开工
    confirmStart() {
      if (!this.materialCheckPassed) return
      startWorkorder(this.materialCheckWorkorderId).then(() => {
        this.$modal.msgSuccess('开工成功')
        this.materialCheckOpen = false
        this.getList()
      }).catch(() => {})
    },
  },
}
</script>
