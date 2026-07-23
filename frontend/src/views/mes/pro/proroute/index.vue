<template>
  <!-- ItemSelect 弹窗组件 -->
  <ItemSelect ref="itemSelectRef" @onSelected="onItemSelected" />
  <ItemSelect ref="bomItemSelectRef" @onSelected="onBomItemSelected" />

  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-row>
        <el-col :span="8">
          <el-form-item label="路线编码" prop="routeCode">
            <el-input v-model="queryParams.routeCode" placeholder="请输入路线编码" clearable @keyup.enter="handleQuery" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="路线名称" prop="routeName">
            <el-input v-model="queryParams.routeName" placeholder="请输入路线名称" clearable @keyup.enter="handleQuery" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="启用状态" prop="enableFlag">
            <el-select v-model="queryParams.enableFlag" placeholder="请选择" clearable>
              <el-option v-for="dict in sys_yes_no_options" :key="dict.value" :label="dict.label" :value="dict.value" />
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
        <el-button type="primary" plain icon="Plus" size="small" @click="handleAdd" v-hasPermi="['mes:pro:proroute:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" size="small" :disabled="single" @click="handleUpdate" v-hasPermi="['mes:pro:proroute:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <!-- 删除按钮已移除：用启停用(enableFlag)替代物理删除 -->
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="routeList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="路线编码" align="center" prop="routeCode" />
      <el-table-column label="路线名称" align="center" prop="routeName" :show-overflow-tooltip="true">
        <template #default="scope">
          <el-button link @click="handleView(scope.row)" v-hasPermi="['mes:pro:proroute:query']">{{ scope.row.routeName }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="路线说明" align="center" prop="routeDesc" :show-overflow-tooltip="true" />
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="启用" align="center" width="70">
        <template #default="scope">
          <el-switch v-model="scope.row.enableFlag" active-value="1" inactive-value="0" @change="handleEnableChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="80" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tooltip content="修改" placement="top"><el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:pro:proroute:edit']"></el-button></el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize" @pagination="getList" />

    <!-- 编辑弹窗（含 3 个 Tab） -->
    <el-dialog :title="title" v-model="open" width="1000px" append-to-body @close="cancel">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="base">
          <el-form ref="form" :model="form" :rules="rules" label-width="100px">
            <el-row>
              <el-col :span="16">
                <el-form-item label="路线编码" prop="routeCode">
                  <el-input v-model="form.routeCode" placeholder="请输入路线编码" :disabled="optType === 'edit' || optType === 'view'" maxlength="64" />
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
                <el-form-item label="路线名称" prop="routeName">
                  <el-input v-model="form.routeName" placeholder="请输入路线名称" :disabled="optType === 'view'" maxlength="255" />
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
                <el-form-item label="路线说明" prop="routeDesc">
                  <el-input v-model="form.routeDesc" type="textarea" :rows="3" placeholder="请输入路线说明" :disabled="optType === 'view'" maxlength="500" show-word-limit />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="24">
                <el-form-item label="备注" prop="remark">
                  <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" :disabled="optType === 'view'" maxlength="500" show-word-limit />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <!-- Tab2: 工序组成 -->
        <el-tab-pane label="工序组成" name="process" v-if="form.routeId != null">
          <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
              <el-button type="primary" plain icon="Plus" size="small" @click="handleAddRouteProcess" v-if="optType !== 'view'">添加工序</el-button>
            </el-col>
          </el-row>
          <el-table :data="processList" v-loading="processLoading">
            <el-table-column label="序号" align="center" prop="orderNum" width="60" />
            <el-table-column label="工序" align="center">
              <template #default="scope">{{ scope.row.processName || scope.row.processCode }}</template>
            </el-table-column>
            <el-table-column label="工序类型" align="center" prop="processType" width="100">
              <template #default="scope">{{ processTypeMap[scope.row.processType] || scope.row.processType }}</template>
            </el-table-column>
            <el-table-column label="关系" align="center" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.linkType==='SS'?'success':(scope.row.linkType==='FS'?'warning':'info')" size="small">{{ linkTypeMap[scope.row.linkType] || scope.row.linkType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="关键工序" align="center" prop="keyFlag" width="80">
              <template #default="scope">
                <span :style="{color:scope.row.keyFlag==='Y'?'#E6A23C':'#909399'}">{{ scope.row.keyFlag==='Y'?'是':'否' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="检验工序" align="center" prop="isCheck" width="80">
              <template #default="scope">
                <span :style="{color:scope.row.isCheck==='Y'?'#67C23A':'#909399'}">{{ scope.row.isCheck==='Y'?'是':'否' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" align="center" width="135" v-if="optType !== 'view'" class-name="small-padding fixed-width">
              <template #default="scope">
                <el-tooltip content="修改" placement="top"><el-button link type="primary" icon="Edit" @click="handleUpdateRouteProcess(scope.row)"></el-button></el-tooltip>
                <el-tooltip content="BOM" placement="top"><el-button link type="primary" icon="Grid" @click="handleOpenProcessBom(scope.row)"></el-button></el-tooltip>
                <el-tooltip content="参数" placement="top"><el-button link type="primary" icon="Setting" @click="handleOpenProcessParam(scope.row)"></el-button></el-tooltip>
                <el-tooltip content="删除" placement="top"><el-button link type="primary" icon="Delete" @click="handleDeleteRouteProcess(scope.row)"></el-button></el-tooltip>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab3: 关联产品 -->
        <el-tab-pane label="关联产品" name="product" v-if="form.routeId != null">
          <el-row :gutter="10" class="mb8">
            <el-col :span="4">
              <el-input v-model="productSearchKey" placeholder="搜索编码/名称" size="small" clearable />
            </el-col>
            <el-col :span="1.5">
              <el-button type="primary" plain icon="Plus" size="small" @click="handleAddProduct" v-if="optType !== 'view'">关联产品</el-button>
            </el-col>
          </el-row>
          <el-table :data="filteredProductList" v-loading="productLoading" @row-click="onProductRowClick" highlight-current-row>
            <el-table-column label="产品编码" align="center" prop="itemCode" />
            <el-table-column label="产品名称" align="center" prop="itemName" :show-overflow-tooltip="true" />
            <el-table-column label="规格型号" align="center" prop="specification" />
            <el-table-column label="基准批量" align="center" prop="quantity" width="80" />
            <el-table-column label="标准工时" align="center" width="100">
              <template #default="scope">{{ scope.row.productionTime }} {{ scope.row.timeUnitType }}</template>
            </el-table-column>
            <el-table-column label="操作" align="center" width="80" v-if="optType !== 'view'" class-name="small-padding fixed-width">
              <template #default="scope">
                <el-tooltip content="修改" placement="top"><el-button link type="primary" icon="Edit" @click.stop="handleUpdateProduct(scope.row)"></el-button></el-tooltip>
                <el-tooltip content="删除" placement="top"><el-button link type="primary" icon="Delete" @click.stop="handleDeleteProduct(scope.row)"></el-button></el-tooltip>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm" v-if="optType !== 'view'">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 路线工序编辑弹窗 -->
    <el-dialog :title="rpTitle" v-model="rpOpen" width="560px" append-to-body>
      <el-form ref="rpForm" :model="rpForm" :rules="rpRules" label-width="120px">
        <el-form-item label="工序" prop="processId">
          <el-select v-model="rpForm.processId" style="width:100%" placeholder="请选择工序" @change="onProcessSelected">
            <el-option v-for="p in allProcessOptions" :key="p.processId" :label="p.processName + ' (' + p.processCode + ')'" :value="p.processId" />
          </el-select>
        </el-form-item>
        <el-form-item label="序号" prop="orderNum">
          <el-input-number v-model="rpForm.orderNum" :min="1" :max="99" style="width:100%" />
        </el-form-item>
        <el-form-item label="关系" prop="linkType">
          <span style="font-size:12px;color:#909399;display:block;margin-bottom:4px">与下一道工序的关系（下一道工序按序号自动确定）</span>
          <el-select v-model="rpForm.linkType" style="width:100%">
            <el-option v-for="d in linkTypeOptions" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="准备时长(分)" prop="defaultPreTime"><el-input-number v-model="rpForm.defaultPreTime" :min="0" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="等待时长(分)" prop="defaultSufTime"><el-input-number v-model="rpForm.defaultSufTime" :min="0" style="width:100%" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="甘特图颜色"><el-color-picker v-model="rpForm.colorCode" /></el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="关键工序"><el-radio-group v-model="rpForm.keyFlag"><el-radio label="Y">是</el-radio><el-radio label="N">否</el-radio></el-radio-group></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="检验工序"><el-radio-group v-model="rpForm.isCheck"><el-radio label="Y">是</el-radio><el-radio label="N">否</el-radio></el-radio-group></el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="rpForm.processType === 'OUTSOURCE'">
          <el-col :span="12">
            <el-form-item label="外发工序"><el-radio-group v-model="rpForm.isOutsource"><el-radio label="1">是</el-radio><el-radio label="0">否</el-radio></el-radio-group></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注"><el-input v-model="rpForm.remark" type="textarea" maxlength="500" /></el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitRpForm">确 定</el-button>
          <el-button @click="rpOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 关联产品编辑弹窗 -->
    <el-dialog :title="prodTitle" v-model="prodOpen" width="500px" append-to-body>
      <el-form ref="prodForm" :model="prodForm" :rules="prodRules" label-width="100px">
        <el-form-item label="产品物料" prop="itemId">
          <template v-if="prodForm.recordId">
            <el-input :model-value="prodForm.itemName + ' (' + prodForm.itemCode + ')'" :disabled="true" />
          </template>
          <template v-else>
            <el-row :gutter="8">
              <el-col :span="16">
                <el-input :model-value="prodForm.itemName ? prodForm.itemName + ' (' + prodForm.itemCode + ')' : ''" placeholder="请选择产品物料" :disabled="true" />
              </el-col>
              <el-col :span="8">
                <el-button type="primary" icon="Search" size="small" @click="openItemSelect">选择物料</el-button>
              </el-col>
            </el-row>
          </template>
        </el-form-item>
        <el-form-item label="基准批量" prop="quantity"><el-input-number v-model="prodForm.quantity" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="标准生产用时" prop="productionTime"><el-input-number v-model="prodForm.productionTime" :min="0" :precision="2" style="width:70%" /><el-select v-model="prodForm.timeUnitType" style="width:28%;margin-left:2%"><el-option v-for="d in timeTypeOptions" :key="d.value" :label="d.label" :value="d.value" /></el-select></el-form-item>
        <el-form-item label="备注"><el-input v-model="prodForm.remark" type="textarea" maxlength="500" /></el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitProdForm">确 定</el-button>
          <el-button @click="prodOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- BOM/参数查看弹窗 -->
    <el-dialog :title="bomTitle" v-model="bomOpen" width="900px" append-to-body @open="bomSearchKey=''">
      <el-tabs v-model="bomTab">
        <el-tab-pane label="BOM物料清单" name="bom">
          <el-row :gutter="10" class="mb8">
            <el-col :span="6">
              <el-input v-model="bomSearchKey" placeholder="搜索物料" size="small" clearable />
            </el-col>
            <el-col :span="1.5">
              <el-button type="primary" plain icon="Plus" size="small" @click="handleAddBomItem" v-if="optType!=='view'">新增物料</el-button>
            </el-col>
          </el-row>
          <el-table :data="filteredBomList" v-loading="bomLoading">
            <el-table-column label="工序" align="center" width="100"><template #default="scope">{{ getProcessName(scope.row.processId) }}</template></el-table-column>
            <el-table-column label="物料编码" align="center" prop="itemCode" width="120" />
            <el-table-column label="物料名称" align="center" prop="itemName" />
            <el-table-column label="规格" align="center" prop="specification" />
            <el-table-column label="单位" align="center" prop="unitName" width="70" />
            <el-table-column label="用量" align="center" prop="quantity" width="100"><template #default="scope"><el-input-number v-if="optType!=='view'" v-model="scope.row.quantity" :min="0" :precision="4" size="small" controls-position="right" @change="(v)=>handleBomQtyChange(scope.row,v)" /><span v-else>{{ scope.row.quantity }}</span></template></el-table-column>
            <el-table-column label="操作" align="center" width="70" v-if="optType!=='view'"><template #default="scope"><el-button size="small" link @click="handleDelBomItem(scope.row)">删除</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="工序参数标准值" name="param">
          <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
              <el-button type="primary" plain icon="Plus" size="small" @click="handleAddParamItem" v-if="optType!=='view'">新增参数</el-button>
            </el-col>
          </el-row>
          <el-table :data="paramList" v-loading="paramLoading">
            <el-table-column label="工序" align="center" width="100"><template #default="scope">{{ getProcessName(scope.row.processId) }}</template></el-table-column>
            <el-table-column label="参数名称" align="center" width="160"><template #default="scope">{{ getTemplateName(scope.row.templateId) }}</template></el-table-column>
            <el-table-column label="标准图样" align="center" width="70"><template #default="scope"><image-preview v-if="getTemplateImageUrl(scope.row.templateId)" :src="getTemplateImageUrl(scope.row.templateId)" :width="40" :height="40" /><span v-else style="color: #909399">-</span></template></el-table-column>
            <el-table-column label="参数值" align="center" prop="paramValue" width="200"><template #default="scope"><el-input v-if="optType!=='view'" v-model="scope.row.paramValue" size="small" /><span v-else>{{ scope.row.paramValue }}</span></template></el-table-column>
            <el-table-column label="操作" align="center" width="70" v-if="optType!=='view'"><template #default="scope"><el-button size="small" link @click="handleDelParamItem(scope.row)">删除</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <div class="dialog-footer"><el-button @click="bomOpen = false">关 闭</el-button></div>
      </template>
    </el-dialog>

    <!-- 新增BOM物料弹窗 -->
    <el-dialog title="新增BOM物料" v-model="_bomAddOpen" width="480px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="所属工序">
          <el-select v-model="_bomItemForm.processId" style="width:100%">
            <el-option v-for="p in processList" :key="p.processId" :label="p.processName" :value="p.processId" />
          </el-select>
        </el-form-item>
        <el-form-item label="物料">
          <el-row :gutter="8">
            <el-col :span="18"><el-input :model-value="_bomItemForm.itemName ? _bomItemForm.itemName + ' (' + _bomItemForm.itemCode + ')' : '请选择物料'" disabled /></el-col>
            <el-col :span="6"><el-button type="primary" size="small" @click="$refs.bomItemSelectRef.open()">选择</el-button></el-col>
          </el-row>
        </el-form-item>
        <el-form-item label="用量">
          <el-input-number v-model="_bomItemForm.quantity" :min="0.0001" :precision="4" style="width:100%" />
          <span style="margin-left:8px;color:#909399">{{ _bomItemForm.unitName || '' }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="confirmAddBomItem">确 定</el-button>
        <el-button @click="_bomAddOpen = false">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 工序BOM弹窗 -->
    <el-dialog :title="'BOM物料 — ' + _procBomTitle" v-model="_procBomOpen" width="700px" append-to-body>
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button type="primary" plain size="small" @click="handleAddProcBomItem" v-if="optType!=='view'">新增物料</el-button>
        </el-col>
      </el-row>
      <el-table :data="_procBomList" v-loading="_procBomLoading" size="small">
        <el-table-column label="物料编码" prop="itemCode" width="130" />
        <el-table-column label="物料名称" prop="itemName" />
        <el-table-column label="规格" prop="specification" width="120" />
        <el-table-column label="单位" prop="unitName" width="70" />
        <el-table-column label="用量" width="100"><template #default="scope"><el-input-number v-if="optType!=='view'" v-model="scope.row.quantity" :min="0.0001" :precision="4" size="small" controls-position="right" @change="(v)=>handleProcBomQtyChange(scope.row,v)" /><span v-else>{{ scope.row.quantity }}</span></template></el-table-column>
        <el-table-column label="操作" width="60" v-if="optType!=='view'"><template #default="scope"><el-button size="small" link @click="handleDelProcBomItem(scope.row)">删除</el-button></template></el-table-column>
      </el-table>
      <template #footer><el-button @click="_procBomOpen = false">关 闭</el-button></template>
    </el-dialog>

    <!-- 工序参数弹窗 -->
    <el-dialog :title="'工序参数 — ' + _procParamTitle" v-model="_procParamOpen" width="600px" append-to-body>
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button type="warning" plain size="small" @click="handleInitParamFromTemplate" v-if="optType!=='view'">从模版初始化</el-button>
        </el-col>
      </el-row>
      <el-table :data="_procParamList" v-loading="_procParamLoading" size="small">
        <el-table-column label="参数名称" width="160"><template #default="scope">{{ getTemplateName(scope.row.templateId) }}</template></el-table-column>
        <el-table-column label="标准图样" align="center" width="70"><template #default="scope"><image-preview v-if="getTemplateImageUrl(scope.row.templateId)" :src="getTemplateImageUrl(scope.row.templateId)" :width="40" :height="40" /><span v-else style="color: #909399">-</span></template></el-table-column>
        <el-table-column label="参数值" prop="paramValue"><template #default="scope"><el-input v-if="optType!=='view'" v-model="scope.row.paramValue" size="small" /><span v-else>{{ scope.row.paramValue }}</span></template></el-table-column>
      </el-table>
      <template #footer>
        <el-button type="primary" @click="handleSaveProcParams" v-if="optType!=='view'">确 定</el-button>
        <el-button @click="_procParamOpen = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { listRoute, getRoute, delRoute, addRoute, updateRoute } from '@/api/mes/pro/proroute'
import { listRouteProcessByRouteId, addRouteProcess, updateRouteProcess, delRouteProcess } from '@/api/mes/pro/routeprocess'
import { listRouteProductByRouteId, addRouteProduct, updateRouteProduct, delRouteProduct } from '@/api/mes/pro/routeproduct'
import { listRouteProductBomByRouteId, addRouteProductBom, updateRouteProductBom, delRouteProductBom } from '@/api/mes/pro/routeproductbom'
import { listRouteProcessParamByRouteProductId, addRouteProcessParam, updateRouteProcessParam, delRouteProcessParam } from '@/api/mes/pro/routeprocessparam'
import { listParamTemplateByProcessId } from '@/api/mes/pro/paramtemplate'
import { listAllProcess } from '@/api/mes/pro/process'
import { genSerialCode } from '@/api/mes/sys/autocoderule'
import ItemSelect from '@/components/itemSelect/single.vue'

export default {
  components: { ItemSelect },
  name: 'Proroute',
  data() {
    return {
      autoGenFlag: false, optType: undefined, activeTab: 'base',
      loading: true, processLoading: false, productLoading: false, bomLoading: false, paramLoading: false,
      ids: [], single: true, multiple: true, showSearch: true, total: 0,
      routeList: [], processList: [], productList: [], bomList: [], paramList: [], allProcessOptions: [],
      title: '', open: false,
      // 路线工序弹窗
      rpOpen: false, rpTitle: '', rpForm: {},
      // 关联产品搜索
      productSearchKey: '',
      // 关联产品弹窗
      prodOpen: false, prodTitle: '', prodForm: {},
      // BOM弹窗
      bomOpen: false, bomTitle: '', bomTab: 'bom', bomSearchKey: '', currentRouteProductId: null,
      _bomAddOpen: false, _bomItemForm: { processId: null, itemId: null, itemCode: null, itemName: null, specification: null, unitOfMeasure: null, unitName: null, quantity: 1 },
      // 工序BOM弹窗
      _procBomOpen: false, _procBomTitle: '', _procBomLoading: false, _procBomList: [],
      // 工序参数弹窗
      _procParamOpen: false, _procParamTitle: '', _procParamLoading: false, _procParamList: [],
      _currentProcProcessId: null, _templateMap: {},
      sys_yes_no_options: [{ label: '是', value: '1' }, { label: '否', value: '0' }],
      processTypeMap: { INTERNAL: '自制', OUTSOURCE: '外发', SLITTING: '分切' },
      linkTypeOptions: [
        { label: '串行(SS) — 必须完成本工序才能开始下一道', value: 'SS' },
        { label: '并行(FS) — 可与下一道工序同时进行', value: 'FS' },
        { label: '协同(CC) — 需与下一道协同作业', value: 'CC' },
      ],
      linkTypeMap: { SS: '串行', FS: '并行', CC: '协同' },
      timeTypeOptions: [{ label: '秒', value: 'SECOND' }, { label: '分钟', value: 'MINUTE' }, { label: '小时', value: 'HOUR' }, { label: '天', value: 'DAY' }],
      queryParams: { pageNum: 1, pageSize: 10, routeCode: null, routeName: null, enableFlag: null },
      form: {},
      rules: {
        routeCode: [{ required: true, message: '路线编码不能为空', trigger: 'blur' }, { max: 64, message: '字段过长', trigger: 'blur' }],
        routeName: [{ required: true, message: '路线名称不能为空', trigger: 'blur' }, { max: 255, message: '字段过长', trigger: 'blur' }],
      },
      rpRules: { processId: [{ required: true, message: '工序不能为空', trigger: 'change' }] },
      prodRules: { itemId: [{ required: true, message: '产品物料不能为空', trigger: 'blur' }], quantity: [{ required: true, message: '批量不能为空', trigger: 'blur' }] },
    }
  },
  created() { this.getList(); this.loadAllProcess() },
  computed: {
    filteredProductList() {
      if (!this.productSearchKey) return this.productList
      const key = this.productSearchKey.toLowerCase()
      return this.productList.filter(p =>
        (p.itemCode && p.itemCode.toLowerCase().includes(key)) ||
        (p.itemName && p.itemName.toLowerCase().includes(key))
      )
    },
    filteredBomList() {
      if (!this.bomSearchKey) return this.bomList
      const key = this.bomSearchKey.toLowerCase()
      return this.bomList.filter(b =>
        (b.itemCode && b.itemCode.toLowerCase().includes(key)) ||
        (b.itemName && b.itemName.toLowerCase().includes(key))
      )
    },
  },
  methods: {
    getList() { this.loading = true; listRoute(this.queryParams).then(r => { this.routeList = r.rows; this.total = r.total; this.loading = false }) },
    loadAllProcess() { listAllProcess().then(r => { this.allProcessOptions = r.data || [] }) },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { routeId: null, routeCode: null, routeName: null, routeDesc: null, enableFlag: '1', remark: null }
      this.autoGenFlag = false; this.activeTab = 'base'; this.processList = []; this.productList = []; this.productSearchKey = ''
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.$refs.queryForm?.resetFields(); this.handleQuery() },
    handleSelectionChange(sel) { this.ids = sel.map(i => i.routeId); this.single = sel.length !== 1; this.multiple = !sel.length },
    handleAdd() { this.reset(); this.open = true; this.title = '新增工艺路线'; this.optType = 'add' },
    handleView(row) {
      this.reset(); const id = row.routeId || this.ids[0]
      getRoute(id).then(r => { this.form = r.data; this.open = true; this.title = '查看工艺路线'; this.optType = 'view'; this.loadProcessList(id); this.loadProductList(id) })
    },
    handleUpdate(row) {
      this.reset(); const id = row.routeId || this.ids[0]
      getRoute(id).then(r => { this.form = r.data; this.open = true; this.title = '修改工艺路线'; this.optType = 'edit'; this.loadProcessList(id); this.loadProductList(id) })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          if (this.form.routeId != null) { updateRoute(this.form).then(() => { this.$modal.msgSuccess('修改成功'); this.open = false; this.getList() }) }
          else { addRoute(this.form).then(r => { this.$modal.msgSuccess('新增成功'); this.form.routeId = r.data?.routeId || r.data; this.activeTab = 'process' }) }
        }
      })
    },
    handleDelete(row) {
      const ids = row.routeId || this.ids.join(',')
      this.$modal.confirm('确认删除路线编号为"' + ids + '"的数据项？').then(() => delRoute(ids)).then(() => { this.getList(); this.$modal.msgSuccess('删除成功') }).catch(() => {})
    },
    handleAutoGenChange(f) { if (f) { genSerialCode('ROUTE_CODE').then(r => { this.form.routeCode = r.data }) } else { this.form.routeCode = null } },
    // 工序组成
    loadProcessList(routeId) { this.processLoading = true; listRouteProcessByRouteId(routeId).then(r => { this.processList = r.data || []; this.processLoading = false }).catch(() => { this.processLoading = false }) },
    handleAddRouteProcess() {
      this.rpForm = { routeId: this.form.routeId, processId: null, processCode: null, processName: null, processType: null, orderNum: this.processList.length + 1, linkType: 'SS', defaultPreTime: 0, defaultSufTime: 0, colorCode: '#00AEF3', keyFlag: 'N', isCheck: 'N', isOutsource: '0', remark: null }
      this.rpTitle = '添加工序'; this.rpOpen = true
    },
    handleUpdateRouteProcess(row) { this.rpForm = { ...row }; this.rpTitle = '修改工序'; this.rpOpen = true },
    handleDeleteRouteProcess(row) { this.$modal.confirm('确认删除该工序？').then(() => delRouteProcess(row.recordId)).then(() => { this.loadProcessList(this.form.routeId); this.$modal.msgSuccess('删除成功') }).catch(() => {}) },
    onProcessSelected(val) { const p = this.allProcessOptions.find(i => i.processId === val); if (p) { this.rpForm.processCode = p.processCode; this.rpForm.processName = p.processName; this.rpForm.processType = p.processType } },
    submitRpForm() {
      this.$refs.rpForm.validate(valid => {
        if (!valid) return
        // 自动计算下一道工序（按序号+1匹配已添加的工序）
        const next = this.processList.find(p => p.orderNum === this.rpForm.orderNum + 1)
        if (next) {
          this.rpForm.nextProcessId = next.processId
          this.rpForm.nextProcessCode = next.processCode
          this.rpForm.nextProcessName = next.processName
        } else {
          this.rpForm.nextProcessId = null
          this.rpForm.nextProcessCode = null
          this.rpForm.nextProcessName = null
        }
        if (this.rpForm.recordId != null) { updateRouteProcess(this.rpForm).then(() => { this.$modal.msgSuccess('修改成功'); this.rpOpen = false; this.loadProcessList(this.form.routeId) }) }
        else { addRouteProcess(this.rpForm).then(() => { this.$modal.msgSuccess('新增成功'); this.rpOpen = false; this.loadProcessList(this.form.routeId) }) }
      })
    },
    getProcessName(pid) { const p = this.processList.find(i => i.processId === pid); return p ? p.processName : pid },
    // 关联产品 — ItemSelect 选择器
    openItemSelect() { this.$refs.itemSelectRef.open() },
    onItemSelected(row) {
      this.prodForm.itemId = row.itemId
      this.prodForm.itemCode = row.itemCode
      this.prodForm.itemName = row.itemName
      this.prodForm.specification = row.specification
      this.prodForm.unitOfMeasure = row.unitOfMeasure
      this.prodForm.unitName = row.unitName
    },
    loadProductList(routeId) { this.productLoading = true; listRouteProductByRouteId(routeId).then(r => { this.productList = r.data || []; this.productLoading = false }).catch(() => { this.productLoading = false }) },
    handleAddProduct() {
      this.prodForm = { routeId: this.form.routeId, itemId: null, itemCode: null, itemName: null, specification: null, unitOfMeasure: null, unitName: null, quantity: 1, productionTime: 1, timeUnitType: 'MINUTE', remark: null }
      this.prodTitle = '关联产品'; this.prodOpen = true
    },
    handleUpdateProduct(row) { this.prodForm = { ...row }; this.prodTitle = '修改产品关联'; this.prodOpen = true },
    handleDeleteProduct(row) { this.$modal.confirm('确认删除该产品关联？').then(() => delRouteProduct(row.recordId)).then(() => { this.loadProductList(this.form.routeId); this.$modal.msgSuccess('删除成功') }).catch(() => {}) },
    submitProdForm() {
      this.$refs.prodForm.validate(valid => {
        if (!valid) return
        if (this.prodForm.recordId != null) { updateRouteProduct(this.prodForm).then(() => { this.$modal.msgSuccess('修改成功'); this.prodOpen = false; this.loadProductList(this.form.routeId) }) }
        else { addRouteProduct(this.prodForm).then(() => { this.$modal.msgSuccess('关联成功'); this.prodOpen = false; this.loadProductList(this.form.routeId) }) }
      })
    },
    // BOM/参数查看
    onProductRowClick(row) { },
    handleViewProductBom(row) {
      this.bomTitle = '产品工艺详情 - ' + (row.itemName || row.itemCode)
      this.currentRouteProductId = row.recordId
      this.bomOpen = true; this.bomTab = 'bom'
      this.loadBomData(); this.loadParamData()
    },
    loadBomData() {
      this.bomLoading = true
      listRouteProductBomByRouteId(this.form.routeId).then(r => {
        this.bomList = (r.data || []).filter(i => !this.currentBomProductId || i.productId === this.currentBomProductId)
        this.bomLoading = false
      }).catch(() => { this.bomLoading = false })
    },
    loadParamData() {
      this.paramLoading = true
      // 加载所有工序的模版字典
      this._templateMap = {}
      const processIds = [...new Set(this.processList.map(p => p.processId))]
      let loaded = 0
      if (processIds.length === 0) {
        listRouteProcessParamByRouteProductId(this.currentRouteProductId).then(r => {
          this.paramList = r.data || []; this.paramLoading = false
        }).catch(() => { this.paramLoading = false })
        return
      }
      Promise.all(processIds.map(pid =>
        listParamTemplateByProcessId(pid).then(tr => {
          (tr.data || []).forEach(t => { this._templateMap[t.templateId] = t })
        }).catch(() => { /* 单个失败不影响其他 */ })
      )).finally(() => {
        listRouteProcessParamByRouteProductId(this.currentRouteProductId).then(r => {
          this.paramList = r.data || []; this.paramLoading = false
        }).catch(() => { this.paramLoading = false })
      })
    },
    // BOM 操作
    handleAddBomItem() {
      this._bomProcessId = this.processList.length > 0 ? this.processList[0].processId : null
      this._bomItemForm = { processId: this._bomProcessId, itemId: null, itemCode: null, itemName: null, specification: null, unitOfMeasure: null, unitName: null, quantity: 1 }
      this._bomAddOpen = true
    },
    onBomItemSelected(row) {
      this._bomItemForm.itemId = row.itemId
      this._bomItemForm.itemCode = row.itemCode
      this._bomItemForm.itemName = row.itemName
      this._bomItemForm.specification = row.specification
      this._bomItemForm.unitOfMeasure = row.unitOfMeasure
      this._bomItemForm.unitName = row.unitName
    },
    handleBomQtyChange(row, val) {
      if (row.recordId) {
        updateRouteProductBom({ ...row, quantity: val }).then(() => {})
      }
    },
    handleDelBomItem(row) {
      this.$modal.confirm('确认删除该BOM物料？').then(() => delRouteProductBom(row.recordId)).then(() => { this.loadBomData(); this.$modal.msgSuccess('删除成功') }).catch(() => {})
    },
    // === 工序BOM弹窗（Tab2 工序组成每行的 BOM 按钮）===
    handleOpenProcessBom(row) {
      if (this.productList.length === 0) { this.$modal.msgWarning('请先在「关联产品」Tab 关联至少一个产品'); return }
      this._currentProcProcessId = row.processId
      this._procBomTitle = row.processName || row.processCode
      this.currentRouteProductId = this.productList[0].recordId
      this._procBomOpen = true
      this._loadProcBomData()
    },
    handleOpenProcessParam(row) {
      if (this.productList.length === 0) { this.$modal.msgWarning('请先在「关联产品」Tab 关联至少一个产品'); return }
      this._currentProcProcessId = row.processId
      this._procParamTitle = row.processName || row.processCode
      this.currentRouteProductId = this.productList[0].recordId
      this._procParamOpen = true
      this._loadProcParamData()
    },
    _loadProcBomData() {
      this._procBomLoading = true
      listRouteProductBomByRouteId(this.form.routeId).then(r => {
        this._procBomList = (r.data || []).filter(b => b.processId === this._currentProcProcessId)
        this._procBomLoading = false
      }).catch(() => { this._procBomLoading = false })
    },
    _loadProcParamData() {
      this._procParamLoading = true
      if (!this.currentRouteProductId) { this._procParamList = []; this._procParamLoading = false; return }
      // 先加载模版字典
      listParamTemplateByProcessId(this._currentProcProcessId).then(tr => {
        this._templateMap = {}
        ;(tr.data || []).forEach(t => { this._templateMap[t.templateId] = t })
        // 再加载参数值
        listRouteProcessParamByRouteProductId(this.currentRouteProductId).then(r => {
          this._procParamList = (r.data || []).filter(p => p.processId === this._currentProcProcessId)
          this._procParamLoading = false
        }).catch(() => { this._procParamList = []; this._procParamLoading = false })
      }).catch(() => {
        listRouteProcessParamByRouteProductId(this.currentRouteProductId).then(r => {
          this._procParamList = (r.data || []).filter(p => p.processId === this._currentProcProcessId)
          this._procParamLoading = false
        }).catch(() => { this._procParamList = []; this._procParamLoading = false })
      })
    },
    getTemplateName(templateId) {
      const t = this._templateMap[templateId]
      return t ? (t.paramName || t.paramCode) : '模板#' + templateId
    },
    getTemplateImageUrl(templateId) {
      const t = this._templateMap[templateId]
      return t ? t.imageUrl : ''
    },
    handleAddProcBomItem() {
      this._bomItemForm.processId = this._currentProcProcessId
      this._bomItemForm.itemId = null; this._bomItemForm.itemCode = null; this._bomItemForm.itemName = null
      this._bomItemForm.specification = null; this._bomItemForm.unitOfMeasure = null; this._bomItemForm.unitName = null; this._bomItemForm.quantity = 1
      this._bomAddOpen = true
    },
    confirmAddBomItem() {
      if (!this._bomItemForm.itemId) { this.$modal.msgWarning('请选择物料'); return }
      // productId：关联产品 Tab 打开时传 actual productId，工序 BOM 弹窗打开时传 null（不限产品）
      const prod = this.productList.find(p => p.recordId === this.currentRouteProductId)
      addRouteProductBom({
        routeId: this.form.routeId, processId: this._bomItemForm.processId,
        productId: prod ? prod.itemId : null,
        itemId: this._bomItemForm.itemId, itemCode: this._bomItemForm.itemCode, itemName: this._bomItemForm.itemName,
        specification: this._bomItemForm.specification, unitOfMeasure: this._bomItemForm.unitOfMeasure, unitName: this._bomItemForm.unitName,
        quantity: this._bomItemForm.quantity
      }).then(() => { this._bomAddOpen = false; this._loadProcBomData(); this.$modal.msgSuccess('新增成功') }).catch(() => { this._bomAddOpen = false })
    },
    handleProcBomQtyChange(row, val) { if (row.recordId) updateRouteProductBom({ ...row, quantity: val }).then(() => {}) },
    handleDelProcBomItem(row) {
      this.$modal.confirm('确认删除该物料？').then(() => delRouteProductBom(row.recordId)).then(() => { this._loadProcBomData(); this.$modal.msgSuccess('删除成功') }).catch(() => {})
    },
    handleSaveProcParams() {
      const updates = this._procParamList.filter(p => p.recordId)
      if (updates.length === 0) { this._procParamOpen = false; return }
      import('@/api/mes/pro/routeprocessparam').then(m => {
        m.batchUpdateRouteProcessParam(updates).then(() => {
          this.$modal.msgSuccess('保存成功')
          this._procParamOpen = false
        })
      })
    },
    handleInitParamFromTemplate() {
      if (!this.currentRouteProductId || !this._currentProcProcessId) return
      import('@/api/mes/pro/routeprocessparam').then(m => {
        m.batchInitFromTemplate(this.currentRouteProductId, this._currentProcProcessId).then(r => {
          this.$modal.msgSuccess(r.msg || '初始化成功')
          this._loadProcParamData()
        })
      })
    },

    // 参数操作 (旧 — 关联产品Tab中的)
    handleAddParamItem() {
      addRouteProcessParam({ routeProductId: this.currentRouteProductId, processId: null, templateId: null, paramValue: '' }).then(() => {
        this.loadParamData(); this.$modal.msgSuccess('新增成功')
      })
    },
    handleDelParamItem(row) {
      this.$modal.confirm('确认删除该参数？').then(() => delRouteProcessParam(row.recordId)).then(() => { this.loadParamData(); this.$modal.msgSuccess('删除成功') }).catch(() => {})
    },
    handleEnableChange(row) {
      const newVal = row.enableFlag
      const text = newVal === '1' ? '启用' : '停用'
      this.$modal.confirm('确认要' + text + '"' + row.routeName + '"吗？').then(() => {
        updateRoute({ routeId: row.routeId, enableFlag: newVal }).then(() => this.$modal.msgSuccess(text + '成功'))
      }).catch(() => {
        row.enableFlag = newVal === '1' ? '0' : '1'
        this.getList()
      })
    },
  },
}

</script>
