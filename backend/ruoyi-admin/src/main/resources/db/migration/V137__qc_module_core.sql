-- ============================================================
-- V137：质量管理模块(qc)
-- 表：检测项/模板(3表)/缺陷字典 + IQC/IPQC/OQC/RQC 单头 + 统一行表 + 缺陷记录
-- 幂等：新表 create（重跑由 Flyway version 保证）；系统表 INSERT 用 WHERE NOT EXISTS
-- 种子不预置 template_product：物料未绑定模板=免检，上线零影响
-- 注：V136 已被领料单状态字典修复占用（qr-code-p1 分支），qc 模块顺延为 V137
-- ============================================================
SET NAMES utf8mb4;

-- ══════════ 1. 检测项 ══════════
create table qxx_qc_index (
  index_id        bigint(20)   not null auto_increment comment '检测项ID',
  factory_id      bigint(20)   not null comment '工厂ID',
  index_code      varchar(64)  not null comment '检测项编码',
  index_name      varchar(255) not null comment '检测项名称',
  index_type      varchar(16)  not null comment '检测项类型(IQC/IPQC/OQC/RQC)',
  qc_tool         varchar(255) default null comment '检测工具',
  qc_result_type  varchar(16)  not null comment '值类型:NUMBER/TEXT/DICT/FILE/COUNT',
  dict_type       varchar(64)  default null comment '字典型关联的sys_dict_type(DICT型必填)',
  qc_result_spc   varchar(255) default null comment '值属性(如长度mm/色差ΔE)',
  enable_flag     char(1)      default '1' not null comment '是否启用',
  remark          varchar(500) default '' comment '备注',
  create_by       varchar(64)  default '',
  create_time     datetime     default current_timestamp,
  update_by       varchar(64)  default '',
  update_time     datetime     default current_timestamp on update current_timestamp,
  key idx_factory_id (factory_id),
  primary key (index_id),
  unique key uk_index_code (index_code)
) engine=innodb auto_increment=100 default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='检测项表';

-- ══════════ 2. 检验模板 ══════════
create table qxx_qc_template (
  template_id     bigint(20)   not null auto_increment comment '模板ID',
  factory_id      bigint(20)   not null comment '工厂ID',
  template_code   varchar(64)  not null comment '模板编码',
  template_name   varchar(255) not null comment '模板名称',
  qc_types        varchar(64)  not null comment '适用检验种类(多选逗号分隔,如IQC,RQC)',
  enable_flag     char(1)      default '1' not null comment '是否启用',
  remark          varchar(500) default '' comment '备注',
  create_by       varchar(64)  default '',
  create_time     datetime     default current_timestamp,
  update_by       varchar(64)  default '',
  update_time     datetime     default current_timestamp on update current_timestamp,
  key idx_factory_id (factory_id),
  primary key (template_id),
  unique key uk_template_code (template_code)
) engine=innodb auto_increment=100 default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='检验模板表';

create table qxx_qc_template_index (
  record_id       bigint(20)   not null auto_increment comment '记录ID',
  factory_id      bigint(20)   not null comment '工厂ID',
  template_id     bigint(20)   not null comment '模板ID',
  index_id        bigint(20)   not null comment '检测项ID',
  index_code      varchar(64)  not null comment '检测项编码',
  index_name      varchar(255) not null comment '检测项名称',
  index_type      varchar(16)  not null comment '检测项类型',
  qc_tool         varchar(255) default null comment '检测工具',
  qc_result_type  varchar(16)  default null comment '值类型',
  check_method    varchar(500) default null comment '检测方法/要求',
  stander_val     double(12,4) default null comment '标准值(数值型)',
  unit_of_measure varchar(64)  default null comment '单位',
  threshold_min   double(12,4) default null comment '允许下偏差(实测<标准+下偏差=不合格)',
  threshold_max   double(12,4) default null comment '允许上偏差(实测>标准+上偏差=不合格)',
  order_num       int(11)      default 1 comment '排序',
  remark          varchar(500) default '' comment '备注',
  create_by       varchar(64)  default '',
  create_time     datetime     default current_timestamp,
  update_by       varchar(64)  default '',
  update_time     datetime     default current_timestamp on update current_timestamp,
  key idx_factory_id (factory_id),
  key idx_template (template_id),
  primary key (record_id)
) engine=innodb auto_increment=100 default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='模板-检测项表';

create table qxx_qc_template_product (
  record_id            bigint(20)   not null auto_increment comment '记录ID',
  factory_id           bigint(20)   not null comment '工厂ID',
  template_id          bigint(20)   not null comment '模板ID',
  item_id              bigint(20)   not null comment '物料ID',
  item_code            varchar(64)  default null comment '物料编码',
  item_name            varchar(255) default null comment '物料名称',
  specification        varchar(500) default null comment '规格型号',
  unit_of_measure      varchar(64)  default null comment '单位',
  process_id           bigint(20)   default null comment '工序ID(仅IPQC工序级绑定,可空)',
  process_code         varchar(64)  default null comment '工序编码',
  process_name         varchar(255) default null comment '工序名称',
  quantity_check       int(11)      default 1 comment '抽检样本量',
  quantity_unqualified int(11)      default 0 comment '最大不合格数(Ac值,超过整批拒收)',
  cr_rate              double(12,2) default 0.00 comment '致命缺陷率阈值(%)',
  maj_rate              double(12,2) default 0.00 comment '严重缺陷率阈值(%)',
  min_rate              double(12,2) default 0.00 comment '轻微缺陷率阈值(%)',
  remark               varchar(500) default '' comment '备注',
  create_by            varchar(64)  default '',
  create_time          datetime     default current_timestamp,
  update_by            varchar(64)  default '',
  update_time          datetime     default current_timestamp on update current_timestamp,
  key idx_factory_id (factory_id),
  key idx_template (template_id),
  key idx_item (item_id),
  primary key (record_id)
) engine=innodb auto_increment=100 default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='模板-物料表';

-- ══════════ 3. 缺陷字典 ══════════
create table qxx_qc_defect (
  defect_id       bigint(20)   not null auto_increment comment '缺陷ID',
  factory_id      bigint(20)   not null comment '工厂ID',
  defect_code     varchar(64)  not null comment '缺陷编码',
  defect_name     varchar(500) not null comment '缺陷描述',
  index_type      varchar(16)  not null comment '适用检验类型(IQC/IPQC/OQC/RQC)',
  defect_level    varchar(16)  not null comment '等级:CRITICAL/MAJOR/MINOR',
  process_method  varchar(500) default null comment '处置方法(返工/让步接收/退货/报废)',
  enable_flag     char(1)      default '1' not null comment '是否启用',
  remark          varchar(500) default '' comment '备注',
  create_by       varchar(64)  default '',
  create_time     datetime     default current_timestamp,
  update_by       varchar(64)  default '',
  update_time     datetime     default current_timestamp on update current_timestamp,
  key idx_factory_id (factory_id),
  primary key (defect_id),
  unique key uk_defect_code (defect_code)
) engine=innodb auto_increment=100 default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='缺陷字典表';

-- ══════════ 4. 四张检验单头（统一含：来源溯源/物料/数量/缺陷/判定/状态） ══════════
create table qxx_qc_iqc (
  iqc_id                   bigint(20)   not null auto_increment comment '来料检验单ID',
  factory_id               bigint(20)   not null comment '工厂ID',
  iqc_code                 varchar(64)  not null comment '检验单编码',
  iqc_name                 varchar(255) default null comment '检验单名称',
  template_id              bigint(20)   not null comment '模板ID',
  source_doc_id            bigint(20)   default null comment '来源单据ID(qxx_wm_item_recpt.recpt_id)',
  source_doc_type          varchar(32)  default 'wm_item_recpt' comment '来源类型',
  source_doc_code          varchar(64)  default null comment '来源单据编码',
  source_line_id           bigint(20)   default null comment '来源行ID',
  vendor_id                bigint(20)   default null comment '供应商ID',
  vendor_code              varchar(64)  default null comment '供应商编码',
  vendor_name              varchar(255) default null comment '供应商名称',
  vendor_batch             varchar(64)  default null comment '供应商批次号',
  item_id                  bigint(20)   not null comment '物料ID',
  item_code                varchar(64)  default null comment '物料编码',
  item_name                varchar(255) default null comment '物料名称',
  specification            varchar(500) default null comment '规格型号',
  unit_of_measure          varchar(64)  default null comment '单位',
  quantity_received        double(12,2) default 0 comment '本次接收数量',
  quantity_check           int(11)      default null comment '本次实际检测数量',
  quantity_min_check       int(11)      default 1 comment '抽检样本量(模板快照)',
  quantity_max_unqualified int(11)      default 0 comment 'Ac值(模板快照)',
  quantity_qualified       int(11)      default 0 comment '合格数',
  quantity_unqualified     int(11)      default 0 comment '不合格数',
  cr_rate_limit            double(12,2) default 0 comment '致命缺陷率阈值(模板快照,%)',
  maj_rate_limit            double(12,2) default 0 comment '严重缺陷率阈值(模板快照,%)',
  min_rate_limit            double(12,2) default 0 comment '轻微缺陷率阈值(模板快照,%)',
  cr_quantity              int(11)      default 0 comment '致命缺陷数(判定汇总)',
  maj_quantity              int(11)      default 0 comment '严重缺陷数',
  min_quantity              int(11)      default 0 comment '轻微缺陷数',
  cr_rate                  double(12,2) default 0 comment '致命缺陷率(判定汇总,%)',
  maj_rate                  double(12,2) default 0 comment '严重缺陷率(%)',
  min_rate                  double(12,2) default 0 comment '轻微缺陷率(%)',
  check_result             varchar(16)  default null comment 'PASS/FAIL/CONCESSION',
  concession_reason        varchar(500) default null comment '让步理由(CONCESSION必填)',
  receive_date             datetime     default null comment '来料日期',
  inspect_date             datetime     default null comment '检验日期',
  inspector                varchar(64)  default null comment '检验员',
  status                   varchar(16)  default 'PENDING' comment 'PENDING/INSPECTING/COMPLETED/CLOSED',
  remark                   varchar(500) default '' comment '备注',
  create_by                varchar(64)  default '',
  create_time              datetime     default current_timestamp,
  update_by                varchar(64)  default '',
  update_time              datetime     default current_timestamp on update current_timestamp,
  key idx_factory_id (factory_id),
  key idx_source (source_doc_type, source_doc_id),
  key idx_item (item_id),
  primary key (iqc_id),
  unique key uk_iqc_code (iqc_code)
) engine=innodb auto_increment=100 default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='来料检验单';

create table qxx_qc_ipqc (
  ipqc_id                  bigint(20)   not null auto_increment comment '过程检验单ID',
  factory_id               bigint(20)   not null comment '工厂ID',
  ipqc_code                varchar(64)  not null comment '检验单编码',
  ipqc_name                varchar(255) default null comment '检验单名称',
  ipqc_type                varchar(16)  not null comment 'FIRST_CHECK/TOUR_CHECK/LAST_CHECK/SPOT_CHECK',
  template_id              bigint(20)   not null comment '模板ID',
  source_doc_id            bigint(20)   default null comment '来源单据ID(pro_card_process.record_id 或 qxx_wm_product_recpt.recpt_id)',
  source_doc_type          varchar(32)  default null comment '来源类型:pro_card_process/wm_product_recpt',
  source_doc_code          varchar(64)  default null comment '来源单据编码',
  source_line_id           bigint(20)   default null comment '来源行ID',
  workorder_id             bigint(20)   default null comment '工单ID',
  workorder_code           varchar(64)  default null comment '工单编码',
  workorder_name           varchar(255) default null comment '工单名称',
  card_id                  bigint(20)   default null comment '流转卡ID',
  card_code                varchar(64)  default null comment '流转卡编码',
  task_id                  bigint(20)   default null comment '任务ID',
  task_code                varchar(64)  default null comment '任务编码',
  process_id               bigint(20)   default null comment '工序ID',
  process_code             varchar(64)  default null comment '工序编码',
  process_name             varchar(255) default null comment '工序名称',
  workstation_id           bigint(20)   default null comment '工位ID(可空)',
  workstation_code         varchar(64)  default null comment '工位编码',
  workstation_name         varchar(255) default null comment '工位名称',
  item_id                  bigint(20)   not null comment '物料ID',
  item_code                varchar(64)  default null comment '物料编码',
  item_name                varchar(255) default null comment '物料名称',
  specification            varchar(500) default null comment '规格型号',
  unit_of_measure          varchar(64)  default null comment '单位',
  quantity_check           double(12,4) default null comment '实际检测数量',
  quantity_min_check       int(11)      default 1 comment '抽检样本量(模板快照)',
  quantity_max_unqualified int(11)      default 0 comment 'Ac值(模板快照)',
  quantity_qualified       double(12,4) default 0 comment '合格数',
  quantity_unqualified     double(12,4) default 0 comment '不合格数',
  cr_rate_limit            double(12,2) default 0 comment '致命缺陷率阈值(模板快照,%)',
  maj_rate_limit            double(12,2) default 0 comment '严重缺陷率阈值(模板快照,%)',
  min_rate_limit            double(12,2) default 0 comment '轻微缺陷率阈值(模板快照,%)',
  cr_quantity              int(11)      default 0 comment '致命缺陷数',
  maj_quantity              int(11)      default 0 comment '严重缺陷数',
  min_quantity              int(11)      default 0 comment '轻微缺陷数',
  cr_rate                  double(12,2) default 0 comment '致命缺陷率(%)',
  maj_rate                  double(12,2) default 0 comment '严重缺陷率(%)',
  min_rate                  double(12,2) default 0 comment '轻微缺陷率(%)',
  check_result             varchar(16)  default null comment 'PASS/FAIL/CONCESSION',
  concession_reason        varchar(500) default null comment '让步理由',
  inspect_date             datetime     default null comment '检验日期',
  inspector                varchar(64)  default null comment '检验员',
  status                   varchar(16)  default 'PENDING' comment 'PENDING/INSPECTING/COMPLETED/CLOSED',
  remark                   varchar(500) default '' comment '备注',
  create_by                varchar(64)  default '',
  create_time              datetime     default current_timestamp,
  update_by                varchar(64)  default '',
  update_time              datetime     default current_timestamp on update current_timestamp,
  key idx_factory_id (factory_id),
  key idx_source (source_doc_type, source_doc_id),
  key idx_item (item_id),
  key idx_workorder (workorder_id),
  primary key (ipqc_id),
  unique key uk_ipqc_code (ipqc_code)
) engine=innodb auto_increment=100 default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='过程检验单';

create table qxx_qc_oqc (
  oqc_id                   bigint(20)   not null auto_increment comment '出货检验单ID',
  factory_id               bigint(20)   not null comment '工厂ID',
  oqc_code                 varchar(64)  not null comment '检验单编码',
  oqc_name                 varchar(255) default null comment '检验单名称',
  template_id              bigint(20)   not null comment '模板ID',
  source_doc_id            bigint(20)   default null comment '来源单据ID(qxx_wm_product_sales.sales_id)',
  source_doc_type          varchar(32)  default 'wm_product_sales' comment '来源类型',
  source_doc_code          varchar(64)  default null comment '来源单据编码',
  source_line_id           bigint(20)   default null comment '来源行ID',
  client_id                bigint(20)   default null comment '客户ID',
  client_code              varchar(64)  default null comment '客户编码',
  client_name              varchar(255) default null comment '客户名称',
  batch_code               varchar(64)  default null comment '批次号',
  item_id                  bigint(20)   not null comment '物料ID',
  item_code                varchar(64)  default null comment '物料编码',
  item_name                varchar(255) default null comment '物料名称',
  specification            varchar(500) default null comment '规格型号',
  unit_of_measure          varchar(64)  default null comment '单位',
  quantity_out             double(12,2) default 0 comment '发货数量',
  quantity_check           int(11)      default null comment '实际检测数量',
  quantity_min_check       int(11)      default 1 comment '抽检样本量(模板快照)',
  quantity_max_unqualified int(11)      default 0 comment 'Ac值(模板快照)',
  quantity_qualified       int(11)      default 0 comment '合格数',
  quantity_unqualified     int(11)      default 0 comment '不合格数',
  cr_rate_limit            double(12,2) default 0 comment '致命缺陷率阈值(模板快照,%)',
  maj_rate_limit            double(12,2) default 0 comment '严重缺陷率阈值(模板快照,%)',
  min_rate_limit            double(12,2) default 0 comment '轻微缺陷率阈值(模板快照,%)',
  cr_quantity              int(11)      default 0 comment '致命缺陷数',
  maj_quantity              int(11)      default 0 comment '严重缺陷数',
  min_quantity              int(11)      default 0 comment '轻微缺陷数',
  cr_rate                  double(12,2) default 0 comment '致命缺陷率(%)',
  maj_rate                  double(12,2) default 0 comment '严重缺陷率(%)',
  min_rate                  double(12,2) default 0 comment '轻微缺陷率(%)',
  check_result             varchar(16)  default null comment 'PASS/FAIL/CONCESSION',
  concession_reason        varchar(500) default null comment '让步理由',
  out_date                 datetime     default null comment '出货日期',
  inspect_date             datetime     default null comment '检验日期',
  inspector                varchar(64)  default null comment '检验员',
  status                   varchar(16)  default 'PENDING' comment 'PENDING/INSPECTING/COMPLETED/CLOSED',
  remark                   varchar(500) default '' comment '备注',
  create_by                varchar(64)  default '',
  create_time              datetime     default current_timestamp,
  update_by                varchar(64)  default '',
  update_time              datetime     default current_timestamp on update current_timestamp,
  key idx_factory_id (factory_id),
  key idx_source (source_doc_type, source_doc_id),
  key idx_item (item_id),
  primary key (oqc_id),
  unique key uk_oqc_code (oqc_code)
) engine=innodb auto_increment=100 default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='出货检验单';

create table qxx_qc_rqc (
  rqc_id                   bigint(20)   not null auto_increment comment '退料检验单ID',
  factory_id               bigint(20)   not null comment '工厂ID',
  rqc_code                 varchar(64)  not null comment '检验单编码',
  rqc_name                 varchar(255) default null comment '检验单名称',
  rqc_type                 varchar(16)  default 'PROD_RETURN' comment 'PROD_RETURN/PURCHASE_RETURN/QC_REJECT',
  template_id              bigint(20)   not null comment '模板ID',
  source_doc_id            bigint(20)   default null comment '来源单据ID(qxx_wm_rt_issue.rt_id)',
  source_doc_type          varchar(32)  default 'wm_rt_issue' comment '来源类型',
  source_doc_code          varchar(64)  default null comment '来源单据编码',
  source_line_id           bigint(20)   default null comment '来源行ID',
  workorder_id             bigint(20)   default null comment '工单ID(生产退料)',
  workorder_code           varchar(64)  default null comment '工单编码',
  vendor_id                bigint(20)   default null comment '供应商ID(采购退货)',
  vendor_code              varchar(64)  default null comment '供应商编码',
  vendor_name              varchar(255) default null comment '供应商名称',
  item_id                  bigint(20)   not null comment '物料ID',
  item_code                varchar(64)  default null comment '物料编码',
  item_name                varchar(255) default null comment '物料名称',
  specification            varchar(500) default null comment '规格型号',
  unit_of_measure          varchar(64)  default null comment '单位',
  batch_code               varchar(128) default null comment '批次号',
  quantity_check           double(12,4) default null comment '实际检测数量',
  quantity_min_check       int(11)      default 1 comment '抽检样本量(模板快照)',
  quantity_max_unqualified int(11)      default 0 comment 'Ac值(模板快照)',
  quantity_qualified       double(12,4) default 0 comment '合格数',
  quantity_unqualified     double(12,4) default 0 comment '不合格数',
  cr_rate_limit            double(12,2) default 0 comment '致命缺陷率阈值(模板快照,%)',
  maj_rate_limit            double(12,2) default 0 comment '严重缺陷率阈值(模板快照,%)',
  min_rate_limit            double(12,2) default 0 comment '轻微缺陷率阈值(模板快照,%)',
  cr_quantity              int(11)      default 0 comment '致命缺陷数',
  maj_quantity              int(11)      default 0 comment '严重缺陷数',
  min_quantity              int(11)      default 0 comment '轻微缺陷数',
  cr_rate                  double(12,2) default 0 comment '致命缺陷率(%)',
  maj_rate                  double(12,2) default 0 comment '严重缺陷率(%)',
  min_rate                  double(12,2) default 0 comment '轻微缺陷率(%)',
  check_result             varchar(16)  default null comment 'PASS/FAIL/CONCESSION',
  concession_reason        varchar(500) default null comment '让步理由',
  return_reason            varchar(500) default null comment '退料原因',
  responsibility           varchar(16)  default null comment '责任归属:SUPPLIER/PRODUCTION/STORAGE/OTHER',
  inspect_date             datetime     default null comment '检验日期',
  inspector                varchar(64)  default null comment '检验员',
  status                   varchar(16)  default 'PENDING' comment 'PENDING/INSPECTING/COMPLETED/CLOSED',
  remark                   varchar(500) default '' comment '备注',
  create_by                varchar(64)  default '',
  create_time              datetime     default current_timestamp,
  update_by                varchar(64)  default '',
  update_time              datetime     default current_timestamp on update current_timestamp,
  key idx_factory_id (factory_id),
  key idx_source (source_doc_type, source_doc_id),
  key idx_item (item_id),
  primary key (rqc_id),
  unique key uk_rqc_code (rqc_code)
) engine=innodb auto_increment=100 default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='退料检验单';

-- ══════════ 5. 统一检验单行（多态 qc_type+qc_id） ══════════
create table qxx_qc_order_line (
  line_id         bigint(20)   not null auto_increment comment '行ID',
  factory_id      bigint(20)   not null comment '工厂ID',
  qc_type         varchar(16)  not null comment '检验单类型:IQC/IPQC/OQC/RQC',
  qc_id           bigint(20)   not null comment '检验单ID(多态)',
  index_id        bigint(20)   not null comment '检测项ID',
  index_code      varchar(64)  default null comment '检测项编码',
  index_name      varchar(255) default null comment '检测项名称',
  index_type      varchar(16)  default null comment '检测项类型',
  qc_tool         varchar(255) default null comment '检测工具',
  qc_result_type  varchar(16)  default null comment '值类型',
  check_method    varchar(500) default null comment '检测方法',
  stander_val     double(12,4) default null comment '标准值(快照)',
  unit_of_measure varchar(64)  default null comment '单位',
  threshold_min   double(12,4) default null comment '允许下偏差(快照)',
  threshold_max   double(12,4) default null comment '允许上偏差(快照)',
  check_val_text  varchar(500) default null comment '实测值(数值型存数字文本/文本型存内容/字典型存dict_value)',
  cr_quantity     int(11)      default 0 comment '致命缺陷数(该检测项)',
  maj_quantity     int(11)      default 0 comment '严重缺陷数',
  min_quantity     int(11)      default 0 comment '轻微缺陷数',
  line_result     varchar(16)  default null comment '行结果:PASS/FAIL',
  order_num       int(11)      default 1 comment '排序',
  remark          varchar(500) default '' comment '备注',
  create_by       varchar(64)  default '',
  create_time     datetime     default current_timestamp,
  update_by       varchar(64)  default '',
  update_time     datetime     default current_timestamp on update current_timestamp,
  key idx_factory_id (factory_id),
  key idx_qc_order (qc_type, qc_id),
  primary key (line_id)
) engine=innodb auto_increment=100 default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='检验单行(四类共用)';

-- ══════════ 6. 缺陷记录 ══════════
create table qxx_qc_defect_record (
  record_id        bigint(20)   not null auto_increment comment '记录ID',
  factory_id       bigint(20)   not null comment '工厂ID',
  qc_type          varchar(16)  not null comment 'IQC/IPQC/OQC/RQC',
  qc_id            bigint(20)   not null comment '检验单ID',
  line_id          bigint(20)   default null comment '检验单行ID',
  defect_id        bigint(20)   default null comment '缺陷字典ID',
  defect_code      varchar(64)  default null comment '缺陷编码',
  defect_name      varchar(500) not null comment '缺陷描述',
  defect_level     varchar(16)  not null comment 'CRITICAL/MAJOR/MINOR',
  defect_quantity  int(11)      default 1 comment '缺陷数量(不合格样品数)',
  process_method   varchar(500) default null comment '处置方法',
  defect_image     varchar(500) default null comment '缺陷图片URL',
  remark           varchar(500) default '' comment '备注',
  create_by        varchar(64)  default '',
  create_time      datetime     default current_timestamp,
  update_by        varchar(64)  default '',
  update_time      datetime     default current_timestamp on update current_timestamp,
  key idx_factory_id (factory_id),
  key idx_qc_order (qc_type, qc_id),
  primary key (record_id)
) engine=innodb auto_increment=100 default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='检验缺陷记录';

-- ══════════ 7. 字典（系统表无 factory_id） ══════════
-- 7.1 质检检验类型（4 条）
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '质检检验类型', 'mes_qc_type', '0', 'admin', NOW(), '质检四类检验业务'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='mes_qc_type');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '来料检验', 'IQC', 'mes_qc_type', '', 'primary', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_type' AND dict_value='IQC');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '过程检验', 'IPQC', 'mes_qc_type', '', 'warning', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_type' AND dict_value='IPQC');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '出货检验', 'OQC', 'mes_qc_type', '', 'success', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_type' AND dict_value='OQC');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '退料检验', 'RQC', 'mes_qc_type', '', 'info', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_type' AND dict_value='RQC');

-- 7.2 质检单状态（4 条）
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '质检单状态', 'mes_qc_status', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='mes_qc_status');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '待检验', 'PENDING', 'mes_qc_status', '', 'warning', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_status' AND dict_value='PENDING');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '检验中', 'INSPECTING', 'mes_qc_status', '', 'primary', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_status' AND dict_value='INSPECTING');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '已完成', 'COMPLETED', 'mes_qc_status', '', 'success', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_status' AND dict_value='COMPLETED');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '已关闭', 'CLOSED', 'mes_qc_status', '', 'info', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_status' AND dict_value='CLOSED');

-- 7.3 质检判定结果（3 条）
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '质检判定结果', 'mes_qc_check_result', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='mes_qc_check_result');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '合格', 'PASS', 'mes_qc_check_result', '', 'success', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_check_result' AND dict_value='PASS');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '不合格', 'FAIL', 'mes_qc_check_result', '', 'danger', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_check_result' AND dict_value='FAIL');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '让步接收', 'CONCESSION', 'mes_qc_check_result', '', 'warning', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_check_result' AND dict_value='CONCESSION');

-- 7.4 质检值类型（5 条）
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '质检值类型', 'mes_qc_result_type', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='mes_qc_result_type');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '数值型', 'NUMBER', 'mes_qc_result_type', '', 'default', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_result_type' AND dict_value='NUMBER');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '文本型', 'TEXT', 'mes_qc_result_type', '', 'default', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_result_type' AND dict_value='TEXT');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '字典型', 'DICT', 'mes_qc_result_type', '', 'default', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_result_type' AND dict_value='DICT');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '文件型', 'FILE', 'mes_qc_result_type', '', 'default', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_result_type' AND dict_value='FILE');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '计数型', 'COUNT', 'mes_qc_result_type', '', 'default', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_result_type' AND dict_value='COUNT');

-- 7.5 缺陷等级（3 条）
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '缺陷等级', 'mes_qc_defect_level', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='mes_qc_defect_level');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '致命缺陷', 'CRITICAL', 'mes_qc_defect_level', '', 'danger', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_defect_level' AND dict_value='CRITICAL');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '严重缺陷', 'MAJOR', 'mes_qc_defect_level', '', 'warning', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_defect_level' AND dict_value='MAJOR');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '轻微缺陷', 'MINOR', 'mes_qc_defect_level', '', 'info', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_defect_level' AND dict_value='MINOR');

-- 7.6 过程检验类型（4 条）
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '过程检验类型', 'mes_qc_ipqc_type', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='mes_qc_ipqc_type');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '首检', 'FIRST_CHECK', 'mes_qc_ipqc_type', '', 'default', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_ipqc_type' AND dict_value='FIRST_CHECK');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '巡检', 'TOUR_CHECK', 'mes_qc_ipqc_type', '', 'default', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_ipqc_type' AND dict_value='TOUR_CHECK');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '尾检(完工检)', 'LAST_CHECK', 'mes_qc_ipqc_type', '', 'default', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_ipqc_type' AND dict_value='LAST_CHECK');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '抽检', 'SPOT_CHECK', 'mes_qc_ipqc_type', '', 'default', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_ipqc_type' AND dict_value='SPOT_CHECK');

-- 7.7 退料检验类型（3 条）
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '退料检验类型', 'mes_qc_rqc_type', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='mes_qc_rqc_type');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '生产退料', 'PROD_RETURN', 'mes_qc_rqc_type', '', 'default', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_rqc_type' AND dict_value='PROD_RETURN');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '采购退货', 'PURCHASE_RETURN', 'mes_qc_rqc_type', '', 'default', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_rqc_type' AND dict_value='PURCHASE_RETURN');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '质检退货', 'QC_REJECT', 'mes_qc_rqc_type', '', 'default', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_rqc_type' AND dict_value='QC_REJECT');

-- 7.8 退料责任归属（4 条）
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '退料责任归属', 'mes_qc_responsibility', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='mes_qc_responsibility');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '供应商', 'SUPPLIER', 'mes_qc_responsibility', '', 'default', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_responsibility' AND dict_value='SUPPLIER');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '生产部门', 'PRODUCTION', 'mes_qc_responsibility', '', 'default', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_responsibility' AND dict_value='PRODUCTION');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '仓储部门', 'STORAGE', 'mes_qc_responsibility', '', 'default', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_responsibility' AND dict_value='STORAGE');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '其他', 'OTHER', 'mes_qc_responsibility', '', 'default', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_qc_responsibility' AND dict_value='OTHER');

-- ══════════ 8. 自动编码规则（4 套，模式照抄 V92） ══════════
-- 8.1 IQC：'IQC'+yyyyMMdd+3位流水(每日重置)
INSERT INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag)
SELECT 1, 'QC_IQC_CODE', '来料检验单编码', '格式:IQC20260816001', 15, 'N', '0', 'L', '1'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_rule WHERE rule_code='QC_IQC_CODE');
SET @rid_iqc = (SELECT rule_id FROM sys_auto_code_rule WHERE rule_code='QC_IQC_CODE' AND factory_id=1 LIMIT 1);
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, fix_character)
SELECT 1, @rid_iqc, 1, 'FIXCHAR', 'PREFIX_IQC', '前缀IQC', 3, 'IQC'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id=@rid_iqc AND part_index=1);
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format)
SELECT 1, @rid_iqc, 2, 'NOWDATE', 'DATE_PART', '日期', 8, 'yyyyMMdd'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id=@rid_iqc AND part_index=2);
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, seria_start_no, seria_step, cycle_flag, cycle_method)
SELECT 1, @rid_iqc, 3, 'SERIALNO', 'SERIAL_PART', '流水号', 3, 1, 1, 'Y', 'DAY'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id=@rid_iqc AND part_index=3);

-- 8.2 IPQC：'IPQC'+yyyyMMdd+3位流水(每日重置)
INSERT INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag)
SELECT 1, 'QC_IPQC_CODE', '过程检验单编码', '格式:IPQC20260816001', 16, 'N', '0', 'L', '1'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_rule WHERE rule_code='QC_IPQC_CODE');
SET @rid_ipqc = (SELECT rule_id FROM sys_auto_code_rule WHERE rule_code='QC_IPQC_CODE' AND factory_id=1 LIMIT 1);
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, fix_character)
SELECT 1, @rid_ipqc, 1, 'FIXCHAR', 'PREFIX_IPQC', '前缀IPQC', 4, 'IPQC'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id=@rid_ipqc AND part_index=1);
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format)
SELECT 1, @rid_ipqc, 2, 'NOWDATE', 'DATE_PART', '日期', 8, 'yyyyMMdd'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id=@rid_ipqc AND part_index=2);
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, seria_start_no, seria_step, cycle_flag, cycle_method)
SELECT 1, @rid_ipqc, 3, 'SERIALNO', 'SERIAL_PART', '流水号', 3, 1, 1, 'Y', 'DAY'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id=@rid_ipqc AND part_index=3);

-- 8.3 OQC：'OQC'+yyyyMMdd+3位流水(每日重置)
INSERT INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag)
SELECT 1, 'QC_OQC_CODE', '出货检验单编码', '格式:OQC20260816001', 15, 'N', '0', 'L', '1'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_rule WHERE rule_code='QC_OQC_CODE');
SET @rid_oqc = (SELECT rule_id FROM sys_auto_code_rule WHERE rule_code='QC_OQC_CODE' AND factory_id=1 LIMIT 1);
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, fix_character)
SELECT 1, @rid_oqc, 1, 'FIXCHAR', 'PREFIX_OQC', '前缀OQC', 3, 'OQC'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id=@rid_oqc AND part_index=1);
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format)
SELECT 1, @rid_oqc, 2, 'NOWDATE', 'DATE_PART', '日期', 8, 'yyyyMMdd'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id=@rid_oqc AND part_index=2);
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, seria_start_no, seria_step, cycle_flag, cycle_method)
SELECT 1, @rid_oqc, 3, 'SERIALNO', 'SERIAL_PART', '流水号', 3, 1, 1, 'Y', 'DAY'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id=@rid_oqc AND part_index=3);

-- 8.4 RQC：'RQC'+yyyyMMdd+3位流水(每日重置)
INSERT INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag)
SELECT 1, 'QC_RQC_CODE', '退料检验单编码', '格式:RQC20260816001', 15, 'N', '0', 'L', '1'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_rule WHERE rule_code='QC_RQC_CODE');
SET @rid_rqc = (SELECT rule_id FROM sys_auto_code_rule WHERE rule_code='QC_RQC_CODE' AND factory_id=1 LIMIT 1);
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, fix_character)
SELECT 1, @rid_rqc, 1, 'FIXCHAR', 'PREFIX_RQC', '前缀RQC', 3, 'RQC'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id=@rid_rqc AND part_index=1);
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format)
SELECT 1, @rid_rqc, 2, 'NOWDATE', 'DATE_PART', '日期', 8, 'yyyyMMdd'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id=@rid_rqc AND part_index=2);
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, seria_start_no, seria_step, cycle_flag, cycle_method)
SELECT 1, @rid_rqc, 3, 'SERIALNO', 'SERIAL_PART', '流水号', 3, 1, 1, 'Y', 'DAY'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id=@rid_rqc AND part_index=3);

-- ══════════ 9. 菜单（28900 目录 + 7 页面 + 39 按钮） ══════════
-- 9.1 目录
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 28900, '质量管理', 0, 8, 'qc', NULL, 1, 0, 'M', '0', '0', '', 'form', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28900);

-- 9.2 页面（C）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 28901, '检测项目', 28900, 1, 'qcindex', 'mes/qc/index/index', 1, 0, 'C', '0', '0', 'mes:qc:index:list', 'edit', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28901);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 28902, '检验模板', 28900, 2, 'qctemplate', 'mes/qc/template/index', 1, 0, 'C', '0', '0', 'mes:qc:template:list', 'clipboard', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28902);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 28903, '缺陷字典', 28900, 3, 'qcdefect', 'mes/qc/defect/index', 1, 0, 'C', '0', '0', 'mes:qc:defect:list', 'dict', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28903);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 28904, '来料检验单', 28900, 4, 'qciqc', 'mes/qc/iqc/index', 1, 0, 'C', '0', '0', 'mes:qc:iqc:list', 'shopping', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28904);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 28905, '过程检验单', 28900, 5, 'qcipqc', 'mes/qc/ipqc/index', 1, 0, 'C', '0', '0', 'mes:qc:ipqc:list', 'build', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28905);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 28906, '出货检验单', 28900, 6, 'qcoqc', 'mes/qc/oqc/index', 1, 0, 'C', '0', '0', 'mes:qc:oqc:list', 'upload', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28906);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 28907, '退料检验单', 28900, 7, 'qcrqc', 'mes/qc/rqc/index', 1, 0, 'C', '0', '0', 'mes:qc:rqc:list', 'switch', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28907);

-- 9.3 按钮权限（F）：检测项目 28911-28915
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28911, '检测项目列表', 28901, 1, 'F', '0', '0', 'mes:qc:index:list', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28911);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28912, '检测项目查询', 28901, 2, 'F', '0', '0', 'mes:qc:index:query', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28912);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28913, '检测项目新增', 28901, 3, 'F', '0', '0', 'mes:qc:index:add', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28913);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28914, '检测项目修改', 28901, 4, 'F', '0', '0', 'mes:qc:index:edit', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28914);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28915, '检测项目删除', 28901, 5, 'F', '0', '0', 'mes:qc:index:remove', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28915);

-- 检验模板 28921-28925
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28921, '检验模板列表', 28902, 1, 'F', '0', '0', 'mes:qc:template:list', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28921);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28922, '检验模板查询', 28902, 2, 'F', '0', '0', 'mes:qc:template:query', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28922);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28923, '检验模板新增', 28902, 3, 'F', '0', '0', 'mes:qc:template:add', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28923);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28924, '检验模板修改', 28902, 4, 'F', '0', '0', 'mes:qc:template:edit', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28924);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28925, '检验模板删除', 28902, 5, 'F', '0', '0', 'mes:qc:template:remove', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28925);

-- 缺陷字典 28931-28935
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28931, '缺陷字典列表', 28903, 1, 'F', '0', '0', 'mes:qc:defect:list', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28931);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28932, '缺陷字典查询', 28903, 2, 'F', '0', '0', 'mes:qc:defect:query', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28932);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28933, '缺陷字典新增', 28903, 3, 'F', '0', '0', 'mes:qc:defect:add', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28933);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28934, '缺陷字典修改', 28903, 4, 'F', '0', '0', 'mes:qc:defect:edit', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28934);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28935, '缺陷字典删除', 28903, 5, 'F', '0', '0', 'mes:qc:defect:remove', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28935);

-- 来料检验单 28941-28945 + 28946 判定
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28941, '来料检验单列表', 28904, 1, 'F', '0', '0', 'mes:qc:iqc:list', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28941);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28942, '来料检验单查询', 28904, 2, 'F', '0', '0', 'mes:qc:iqc:query', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28942);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28943, '来料检验单新增', 28904, 3, 'F', '0', '0', 'mes:qc:iqc:add', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28943);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28944, '来料检验单修改', 28904, 4, 'F', '0', '0', 'mes:qc:iqc:edit', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28944);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28945, '来料检验单删除', 28904, 5, 'F', '0', '0', 'mes:qc:iqc:remove', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28945);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28946, '来料检验单判定', 28904, 6, 'F', '0', '0', 'mes:qc:iqc:judge', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28946);

-- 过程检验单 28951-28956
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28951, '过程检验单列表', 28905, 1, 'F', '0', '0', 'mes:qc:ipqc:list', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28951);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28952, '过程检验单查询', 28905, 2, 'F', '0', '0', 'mes:qc:ipqc:query', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28952);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28953, '过程检验单新增', 28905, 3, 'F', '0', '0', 'mes:qc:ipqc:add', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28953);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28954, '过程检验单修改', 28905, 4, 'F', '0', '0', 'mes:qc:ipqc:edit', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28954);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28955, '过程检验单删除', 28905, 5, 'F', '0', '0', 'mes:qc:ipqc:remove', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28955);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28956, '过程检验单判定', 28905, 6, 'F', '0', '0', 'mes:qc:ipqc:judge', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28956);

-- 出货检验单 28961-28966
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28961, '出货检验单列表', 28906, 1, 'F', '0', '0', 'mes:qc:oqc:list', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28961);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28962, '出货检验单查询', 28906, 2, 'F', '0', '0', 'mes:qc:oqc:query', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28962);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28963, '出货检验单新增', 28906, 3, 'F', '0', '0', 'mes:qc:oqc:add', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28963);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28964, '出货检验单修改', 28906, 4, 'F', '0', '0', 'mes:qc:oqc:edit', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28964);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28965, '出货检验单删除', 28906, 5, 'F', '0', '0', 'mes:qc:oqc:remove', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28965);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28966, '出货检验单判定', 28906, 6, 'F', '0', '0', 'mes:qc:oqc:judge', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28966);

-- 退料检验单 28971-28976
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28971, '退料检验单列表', 28907, 1, 'F', '0', '0', 'mes:qc:rqc:list', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28971);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28972, '退料检验单查询', 28907, 2, 'F', '0', '0', 'mes:qc:rqc:query', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28972);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28973, '退料检验单新增', 28907, 3, 'F', '0', '0', 'mes:qc:rqc:add', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28973);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28974, '退料检验单修改', 28907, 4, 'F', '0', '0', 'mes:qc:rqc:edit', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28974);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28975, '退料检验单删除', 28907, 5, 'F', '0', '0', 'mes:qc:rqc:remove', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28975);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28976, '退料检验单判定', 28907, 6, 'F', '0', '0', 'mes:qc:rqc:judge', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=28976);

-- 9.4 角色授权（sys_role_menu 主键含 factory_id，Flyway 裸 JDBC 必须显式写 factory_id=0；模式照抄 V92）
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, menu_id, 0 FROM sys_menu WHERE menu_id IN (
    28900, 28901, 28902, 28903, 28904, 28905, 28906, 28907,
    28911, 28912, 28913, 28914, 28915,
    28921, 28922, 28923, 28924, 28925,
    28931, 28932, 28933, 28934, 28935,
    28941, 28942, 28943, 28944, 28945, 28946,
    28951, 28952, 28953, 28954, 28955, 28956,
    28961, 28962, 28963, 28964, 28965, 28966,
    28971, 28972, 28973, 28974, 28975, 28976
) AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id AND rm.factory_id = 0
);

INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 11, menu_id, 0 FROM sys_menu WHERE menu_id IN (
    28900, 28901, 28902, 28903, 28904, 28905, 28906, 28907,
    28911, 28912, 28913, 28914, 28915,
    28921, 28922, 28923, 28924, 28925,
    28931, 28932, 28933, 28934, 28935,
    28941, 28942, 28943, 28944, 28945, 28946,
    28951, 28952, 28953, 28954, 28955, 28956,
    28961, 28962, 28963, 28964, 28965, 28966,
    28971, 28972, 28973, 28974, 28975, 28976
) AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 11 AND rm.menu_id = sys_menu.menu_id AND rm.factory_id = 0
);

-- ══════════ 10. 种子：检测项（17 条，factory_id 显式；多工厂按需复制） ══════════
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, qc_result_spc, create_by)
SELECT 1, 'IQC-PAPER-001', '原纸克重', 'IQC', '电子秤', 'NUMBER', 'g/m²', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='IQC-PAPER-001');
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, qc_result_spc, create_by)
SELECT 1, 'IQC-PAPER-002', '原纸幅宽', 'IQC', '卷尺', 'NUMBER', 'mm', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='IQC-PAPER-002');
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, create_by)
SELECT 1, 'IQC-GEN-001', '来料外观检查', 'IQC', '目视', 'TEXT', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='IQC-GEN-001');
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, create_by)
SELECT 1, 'IQC-GEN-002', '来料数量核对', 'IQC', '点数', 'COUNT', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='IQC-GEN-002');
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, qc_result_spc, create_by)
SELECT 1, 'IPQC-PRINT-001', '印刷色差ΔE', 'IPQC', '色差仪', 'NUMBER', 'ΔE', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='IPQC-PRINT-001');
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, qc_result_spc, create_by)
SELECT 1, 'IPQC-BAG-001', '制袋长度', 'IPQC', '钢尺', 'NUMBER', 'mm', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='IPQC-BAG-001');
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, qc_result_spc, create_by)
SELECT 1, 'IPQC-BAG-002', '制袋宽度', 'IPQC', '钢尺', 'NUMBER', 'mm', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='IPQC-BAG-002');
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, qc_result_spc, create_by)
SELECT 1, 'IPQC-BAG-003', '提绳长度', 'IPQC', '钢尺', 'NUMBER', 'mm', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='IPQC-BAG-003');
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, qc_result_spc, create_by)
SELECT 1, 'IPQC-BAG-004', '胶合强度', 'IPQC', '拉力计', 'NUMBER', 'N', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='IPQC-BAG-004');
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, create_by)
SELECT 1, 'IPQC-GEN-001', '过程外观目视', 'IPQC', '目视', 'TEXT', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='IPQC-GEN-001');
-- OQC 通用核对项（DICT 型，dict_type 用 mes_qc_check_result）
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, dict_type, create_by)
SELECT 1, 'OQC-GEN-001', '装箱核对', 'OQC', '目视', 'DICT', 'mes_qc_check_result', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='OQC-GEN-001');
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, dict_type, create_by)
SELECT 1, 'OQC-GEN-002', '外观检查', 'OQC', '目视', 'DICT', 'mes_qc_check_result', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='OQC-GEN-002');
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, dict_type, create_by)
SELECT 1, 'OQC-GEN-003', '箱唛核对', 'OQC', '目视', 'DICT', 'mes_qc_check_result', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='OQC-GEN-003');
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, dict_type, create_by)
SELECT 1, 'OQC-GEN-004', '封箱检查', 'OQC', '目视', 'DICT', 'mes_qc_check_result', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='OQC-GEN-004');
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, dict_type, create_by)
SELECT 1, 'OQC-GEN-005', '托盘检查', 'OQC', '目视', 'DICT', 'mes_qc_check_result', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='OQC-GEN-005');
-- RQC 通用项
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_result_type, create_by)
SELECT 1, 'RQC-GEN-001', '退料原因确认', 'RQC', 'TEXT', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='RQC-GEN-001');
INSERT INTO qxx_qc_index (factory_id, index_code, index_name, index_type, qc_tool, qc_result_type, create_by)
SELECT 1, 'RQC-GEN-002', '退料数量清点', 'RQC', '点数', 'COUNT', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_index WHERE index_code='RQC-GEN-002');

-- ══════════ 11. 种子：缺陷字典（7 条） ══════════
INSERT INTO qxx_qc_defect (factory_id, defect_code, defect_name, index_type, defect_level, process_method, create_by)
SELECT 1, 'DEF-001', '印刷色差超标', 'IPQC', 'MINOR', '返工', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_defect WHERE defect_code='DEF-001');
INSERT INTO qxx_qc_defect (factory_id, defect_code, defect_name, index_type, defect_level, process_method, create_by)
SELECT 1, 'DEF-002', '尺寸超差', 'IPQC', 'MAJOR', '返工', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_defect WHERE defect_code='DEF-002');
INSERT INTO qxx_qc_defect (factory_id, defect_code, defect_name, index_type, defect_level, process_method, create_by)
SELECT 1, 'DEF-003', '袋底爆裂', 'IPQC', 'CRITICAL', '报废', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_defect WHERE defect_code='DEF-003');
INSERT INTO qxx_qc_defect (factory_id, defect_code, defect_name, index_type, defect_level, process_method, create_by)
SELECT 1, 'DEF-004', '绳长偏差', 'IPQC', 'MINOR', '让步接收', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_defect WHERE defect_code='DEF-004');
INSERT INTO qxx_qc_defect (factory_id, defect_code, defect_name, index_type, defect_level, process_method, create_by)
SELECT 1, 'DEF-005', '胶合不牢', 'IPQC', 'MAJOR', '返工', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_defect WHERE defect_code='DEF-005');
INSERT INTO qxx_qc_defect (factory_id, defect_code, defect_name, index_type, defect_level, process_method, create_by)
SELECT 1, 'DEF-006', '破损污渍', 'IQC', 'MINOR', '退货', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_defect WHERE defect_code='DEF-006');
INSERT INTO qxx_qc_defect (factory_id, defect_code, defect_name, index_type, defect_level, process_method, create_by)
SELECT 1, 'DEF-007', '数量短缺', 'IQC', 'MAJOR', '补货', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_defect WHERE defect_code='DEF-007');

-- ══════════ 12. 种子：默认模板（4 个头 + 17 行） ══════════
INSERT INTO qxx_qc_template (factory_id, template_code, template_name, qc_types, remark, create_by)
SELECT 1, 'TPL-IQC-STD', '标准来料检验模板', 'IQC', '含克重/幅宽/外观/数量', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_template WHERE template_code='TPL-IQC-STD');
INSERT INTO qxx_qc_template (factory_id, template_code, template_name, qc_types, remark, create_by)
SELECT 1, 'TPL-IPQC-STD', '标准过程检验模板', 'IPQC', '含色差/制袋尺寸/提绳/胶合/外观', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_template WHERE template_code='TPL-IPQC-STD');
INSERT INTO qxx_qc_template (factory_id, template_code, template_name, qc_types, remark, create_by)
SELECT 1, 'TPL-OQC-STD', '标准出货检验模板', 'OQC', '含装箱/外观/箱唛/封箱/托盘核对', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_template WHERE template_code='TPL-OQC-STD');
INSERT INTO qxx_qc_template (factory_id, template_code, template_name, qc_types, remark, create_by)
SELECT 1, 'TPL-RQC-STD', '标准退料检验模板', 'RQC', '含退料原因确认/数量清点', 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM qxx_qc_template WHERE template_code='TPL-RQC-STD');

-- 模板行：stander_val/threshold 由用户在页面按物料/模板行编辑，种子留空
SET @tid_iqc = (SELECT template_id FROM qxx_qc_template WHERE template_code='TPL-IQC-STD' AND factory_id=1 LIMIT 1);
INSERT INTO qxx_qc_template_index (factory_id, template_id, index_id, index_code, index_name, index_type, qc_tool, qc_result_type, stander_val, unit_of_measure, threshold_min, threshold_max, order_num, create_by)
SELECT 1, @tid_iqc, i.index_id, i.index_code, i.index_name, i.index_type, i.qc_tool, i.qc_result_type, NULL, i.qc_result_spc, NULL, NULL, 1, 'admin'
FROM qxx_qc_index i WHERE i.index_code='IQC-GEN-001'
AND NOT EXISTS (SELECT 1 FROM qxx_qc_template_index x WHERE x.template_id=@tid_iqc AND x.index_code='IQC-GEN-001');
INSERT INTO qxx_qc_template_index (factory_id, template_id, index_id, index_code, index_name, index_type, qc_tool, qc_result_type, stander_val, unit_of_measure, threshold_min, threshold_max, order_num, create_by)
SELECT 1, @tid_iqc, i.index_id, i.index_code, i.index_name, i.index_type, i.qc_tool, i.qc_result_type, NULL, i.qc_result_spc, NULL, NULL, 2, 'admin'
FROM qxx_qc_index i WHERE i.index_code='IQC-PAPER-001'
AND NOT EXISTS (SELECT 1 FROM qxx_qc_template_index x WHERE x.template_id=@tid_iqc AND x.index_code='IQC-PAPER-001');
INSERT INTO qxx_qc_template_index (factory_id, template_id, index_id, index_code, index_name, index_type, qc_tool, qc_result_type, stander_val, unit_of_measure, threshold_min, threshold_max, order_num, create_by)
SELECT 1, @tid_iqc, i.index_id, i.index_code, i.index_name, i.index_type, i.qc_tool, i.qc_result_type, NULL, i.qc_result_spc, NULL, NULL, 3, 'admin'
FROM qxx_qc_index i WHERE i.index_code='IQC-PAPER-002'
AND NOT EXISTS (SELECT 1 FROM qxx_qc_template_index x WHERE x.template_id=@tid_iqc AND x.index_code='IQC-PAPER-002');
INSERT INTO qxx_qc_template_index (factory_id, template_id, index_id, index_code, index_name, index_type, qc_tool, qc_result_type, stander_val, unit_of_measure, threshold_min, threshold_max, order_num, create_by)
SELECT 1, @tid_iqc, i.index_id, i.index_code, i.index_name, i.index_type, i.qc_tool, i.qc_result_type, NULL, i.qc_result_spc, NULL, NULL, 4, 'admin'
FROM qxx_qc_index i WHERE i.index_code='IQC-GEN-002'
AND NOT EXISTS (SELECT 1 FROM qxx_qc_template_index x WHERE x.template_id=@tid_iqc AND x.index_code='IQC-GEN-002');

SET @tid_ipqc = (SELECT template_id FROM qxx_qc_template WHERE template_code='TPL-IPQC-STD' AND factory_id=1 LIMIT 1);
INSERT INTO qxx_qc_template_index (factory_id, template_id, index_id, index_code, index_name, index_type, qc_tool, qc_result_type, stander_val, unit_of_measure, threshold_min, threshold_max, order_num, create_by)
SELECT 1, @tid_ipqc, i.index_id, i.index_code, i.index_name, i.index_type, i.qc_tool, i.qc_result_type, NULL, i.qc_result_spc, NULL, NULL, 1, 'admin'
FROM qxx_qc_index i WHERE i.index_code='IPQC-PRINT-001'
AND NOT EXISTS (SELECT 1 FROM qxx_qc_template_index x WHERE x.template_id=@tid_ipqc AND x.index_code='IPQC-PRINT-001');
INSERT INTO qxx_qc_template_index (factory_id, template_id, index_id, index_code, index_name, index_type, qc_tool, qc_result_type, stander_val, unit_of_measure, threshold_min, threshold_max, order_num, create_by)
SELECT 1, @tid_ipqc, i.index_id, i.index_code, i.index_name, i.index_type, i.qc_tool, i.qc_result_type, NULL, i.qc_result_spc, NULL, NULL, 2, 'admin'
FROM qxx_qc_index i WHERE i.index_code='IPQC-BAG-001'
AND NOT EXISTS (SELECT 1 FROM qxx_qc_template_index x WHERE x.template_id=@tid_ipqc AND x.index_code='IPQC-BAG-001');
INSERT INTO qxx_qc_template_index (factory_id, template_id, index_id, index_code, index_name, index_type, qc_tool, qc_result_type, stander_val, unit_of_measure, threshold_min, threshold_max, order_num, create_by)
SELECT 1, @tid_ipqc, i.index_id, i.index_code, i.index_name, i.index_type, i.qc_tool, i.qc_result_type, NULL, i.qc_result_spc, NULL, NULL, 3, 'admin'
FROM qxx_qc_index i WHERE i.index_code='IPQC-BAG-002'
AND NOT EXISTS (SELECT 1 FROM qxx_qc_template_index x WHERE x.template_id=@tid_ipqc AND x.index_code='IPQC-BAG-002');
INSERT INTO qxx_qc_template_index (factory_id, template_id, index_id, index_code, index_name, index_type, qc_tool, qc_result_type, stander_val, unit_of_measure, threshold_min, threshold_max, order_num, create_by)
SELECT 1, @tid_ipqc, i.index_id, i.index_code, i.index_name, i.index_type, i.qc_tool, i.qc_result_type, NULL, i.qc_result_spc, NULL, NULL, 4, 'admin'
FROM qxx_qc_index i WHERE i.index_code='IPQC-BAG-003'
AND NOT EXISTS (SELECT 1 FROM qxx_qc_template_index x WHERE x.template_id=@tid_ipqc AND x.index_code='IPQC-BAG-003');
INSERT INTO qxx_qc_template_index (factory_id, template_id, index_id, index_code, index_name, index_type, qc_tool, qc_result_type, stander_val, unit_of_measure, threshold_min, threshold_max, order_num, create_by)
SELECT 1, @tid_ipqc, i.index_id, i.index_code, i.index_name, i.index_type, i.qc_tool, i.qc_result_type, NULL, i.qc_result_spc, NULL, NULL, 5, 'admin'
FROM qxx_qc_index i WHERE i.index_code='IPQC-BAG-004'
AND NOT EXISTS (SELECT 1 FROM qxx_qc_template_index x WHERE x.template_id=@tid_ipqc AND x.index_code='IPQC-BAG-004');
INSERT INTO qxx_qc_template_index (factory_id, template_id, index_id, index_code, index_name, index_type, qc_tool, qc_result_type, stander_val, unit_of_measure, threshold_min, threshold_max, order_num, create_by)
SELECT 1, @tid_ipqc, i.index_id, i.index_code, i.index_name, i.index_type, i.qc_tool, i.qc_result_type, NULL, i.qc_result_spc, NULL, NULL, 6, 'admin'
FROM qxx_qc_index i WHERE i.index_code='IPQC-GEN-001'
AND NOT EXISTS (SELECT 1 FROM qxx_qc_template_index x WHERE x.template_id=@tid_ipqc AND x.index_code='IPQC-GEN-001');

SET @tid_oqc = (SELECT template_id FROM qxx_qc_template WHERE template_code='TPL-OQC-STD' AND factory_id=1 LIMIT 1);
INSERT INTO qxx_qc_template_index (factory_id, template_id, index_id, index_code, index_name, index_type, qc_tool, qc_result_type, stander_val, unit_of_measure, threshold_min, threshold_max, order_num, create_by)
SELECT 1, @tid_oqc, i.index_id, i.index_code, i.index_name, i.index_type, i.qc_tool, i.qc_result_type, NULL, i.qc_result_spc, NULL, NULL, 1, 'admin'
FROM qxx_qc_index i WHERE i.index_code='OQC-GEN-001'
AND NOT EXISTS (SELECT 1 FROM qxx_qc_template_index x WHERE x.template_id=@tid_oqc AND x.index_code='OQC-GEN-001');
INSERT INTO qxx_qc_template_index (factory_id, template_id, index_id, index_code, index_name, index_type, qc_tool, qc_result_type, stander_val, unit_of_measure, threshold_min, threshold_max, order_num, create_by)
SELECT 1, @tid_oqc, i.index_id, i.index_code, i.index_name, i.index_type, i.qc_tool, i.qc_result_type, NULL, i.qc_result_spc, NULL, NULL, 2, 'admin'
FROM qxx_qc_index i WHERE i.index_code='OQC-GEN-002'
AND NOT EXISTS (SELECT 1 FROM qxx_qc_template_index x WHERE x.template_id=@tid_oqc AND x.index_code='OQC-GEN-002');
INSERT INTO qxx_qc_template_index (factory_id, template_id, index_id, index_code, index_name, index_type, qc_tool, qc_result_type, stander_val, unit_of_measure, threshold_min, threshold_max, order_num, create_by)
SELECT 1, @tid_oqc, i.index_id, i.index_code, i.index_name, i.index_type, i.qc_tool, i.qc_result_type, NULL, i.qc_result_spc, NULL, NULL, 3, 'admin'
FROM qxx_qc_index i WHERE i.index_code='OQC-GEN-003'
AND NOT EXISTS (SELECT 1 FROM qxx_qc_template_index x WHERE x.template_id=@tid_oqc AND x.index_code='OQC-GEN-003');
INSERT INTO qxx_qc_template_index (factory_id, template_id, index_id, index_code, index_name, index_type, qc_tool, qc_result_type, stander_val, unit_of_measure, threshold_min, threshold_max, order_num, create_by)
SELECT 1, @tid_oqc, i.index_id, i.index_code, i.index_name, i.index_type, i.qc_tool, i.qc_result_type, NULL, i.qc_result_spc, NULL, NULL, 4, 'admin'
FROM qxx_qc_index i WHERE i.index_code='OQC-GEN-004'
AND NOT EXISTS (SELECT 1 FROM qxx_qc_template_index x WHERE x.template_id=@tid_oqc AND x.index_code='OQC-GEN-004');
INSERT INTO qxx_qc_template_index (factory_id, template_id, index_id, index_code, index_name, index_type, qc_tool, qc_result_type, stander_val, unit_of_measure, threshold_min, threshold_max, order_num, create_by)
SELECT 1, @tid_oqc, i.index_id, i.index_code, i.index_name, i.index_type, i.qc_tool, i.qc_result_type, NULL, i.qc_result_spc, NULL, NULL, 5, 'admin'
FROM qxx_qc_index i WHERE i.index_code='OQC-GEN-005'
AND NOT EXISTS (SELECT 1 FROM qxx_qc_template_index x WHERE x.template_id=@tid_oqc AND x.index_code='OQC-GEN-005');

SET @tid_rqc = (SELECT template_id FROM qxx_qc_template WHERE template_code='TPL-RQC-STD' AND factory_id=1 LIMIT 1);
INSERT INTO qxx_qc_template_index (factory_id, template_id, index_id, index_code, index_name, index_type, qc_tool, qc_result_type, stander_val, unit_of_measure, threshold_min, threshold_max, order_num, create_by)
SELECT 1, @tid_rqc, i.index_id, i.index_code, i.index_name, i.index_type, i.qc_tool, i.qc_result_type, NULL, i.qc_result_spc, NULL, NULL, 1, 'admin'
FROM qxx_qc_index i WHERE i.index_code='RQC-GEN-001'
AND NOT EXISTS (SELECT 1 FROM qxx_qc_template_index x WHERE x.template_id=@tid_rqc AND x.index_code='RQC-GEN-001');
INSERT INTO qxx_qc_template_index (factory_id, template_id, index_id, index_code, index_name, index_type, qc_tool, qc_result_type, stander_val, unit_of_measure, threshold_min, threshold_max, order_num, create_by)
SELECT 1, @tid_rqc, i.index_id, i.index_code, i.index_name, i.index_type, i.qc_tool, i.qc_result_type, NULL, i.qc_result_spc, NULL, NULL, 2, 'admin'
FROM qxx_qc_index i WHERE i.index_code='RQC-GEN-002'
AND NOT EXISTS (SELECT 1 FROM qxx_qc_template_index x WHERE x.template_id=@tid_rqc AND x.index_code='RQC-GEN-002');
