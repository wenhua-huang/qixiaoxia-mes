-- ============================================================
-- 企小侠文化纸盒MES系统 — 质量管理模块(qc)表设计
-- 版本: v1.0
-- 日期: 2026-06-05
-- 数据库: MySQL 8.0+, 字符集 utf8mb4
-- 表前缀: qxx_qc_ (Quality Control 质量管理)
-- 说明: 检测项/检测模板/来料检验/过程检验/出货检验/退料检验/缺陷记录/检测结果等完整质量管理体系
--       纸袋行业扩展: 印刷颜色检测/制袋尺寸检测/绳长检测/胶合强度检测/装箱核对/箱唛核对
--       单据模式: Header-Line-Result-Detail 多层结构，支持数值型/文本型/字典型/文件型/计数型多种检测结果类型
-- ============================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================================
-- 一、检测基础数据
-- ============================================================

-- ----------------------------
-- 1、检测项表
-- 用途：定义所有可执行的检测项目，支持数值型/文本型/字典型/文件型/计数型五种检测值类型
-- ----------------------------
drop table if exists qxx_qc_index;
create table qxx_qc_index (
  index_id          bigint(20)      not null auto_increment    comment '检测项ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  index_code        varchar(64)     not null                   comment '检测项编码(唯一)',
  index_name        varchar(255)    not null                   comment '检测项名称',
  index_type        varchar(64)     not null                   comment '检测项类型(如IQC-来料检验/IPQC-过程检验/OQC-出货检验/RQC-退料检验)',
  qc_tool           varchar(255)    default null               comment '检测工具(如游标卡尺/色差仪/拉力计/目视)',
  qc_result_type    varchar(64)     not null                   comment '质检值类型:NUMBER-数值型,TEXT-文本型,DICT-字典型,FILE-文件型,COUNT-计数型',
  qc_result_spc     varchar(255)    default null               comment '值属性(数值型才需要:长度mm/重量g/拉力N/色差值ΔE等)',
  enable_flag       char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (index_id),
  unique key uk_index_code (index_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '检测项表';

-- ----------------------------
-- 2、检测模板表
-- 用途：定义检验模板，组合多个检测项形成检验方案
-- ----------------------------
drop table if exists qxx_qc_template;
create table qxx_qc_template (
  template_id       bigint(20)      not null auto_increment    comment '检测模板ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  template_code     varchar(64)     not null                   comment '检测模板编码(唯一)',
  template_name     varchar(255)    not null                   comment '检测模板名称',
  qc_types          varchar(255)    not null                   comment '适用检验种类(多选,逗号分隔:IQC-来料检验,IPQC-过程检验,OQC-出货检验,RQC-退料检验)',
  enable_flag       char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (template_id),
  unique key uk_template_code (template_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '检测模板表';

-- ----------------------------
-- 3、模板-检测项关联表
-- 用途：定义模板中包含哪些检测项及其具体检测参数(方法/标准值/上下限)
-- ----------------------------
drop table if exists qxx_qc_template_index;
create table qxx_qc_template_index (
  record_id         bigint(20)      not null auto_increment    comment '记录ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  template_id       bigint(20)      not null                   comment '检测模板ID(关联qxx_qc_template)',
  index_id          bigint(20)      not null                   comment '检测项ID(关联qxx_qc_index)',
  index_code        varchar(64)     not null                   comment '检测项编码',
  index_name        varchar(255)    not null                   comment '检测项名称',
  index_type        varchar(64)     not null                   comment '检测项类型',
  qc_tool           varchar(255)    default null               comment '检测工具',
  check_method      varchar(500)    default null               comment '检测方法/要求(如:每卷取3点测量取均值)',
  stander_val       double(12,4)    default null               comment '标准值(数值型检测项使用)',
  unit_of_measure   varchar(64)     default null               comment '单位(如mm/g/N/m)',
  threshold_max     double(12,4)    default null               comment '误差上限(超出即为不合格)',
  threshold_min     double(12,4)    default null               comment '误差下限(低于即为不合格)',
  doc_url           varchar(255)    default null               comment '说明文档/图片URL',
  order_num         int(11)         default 1                  comment '排序号(同模板内检测项执行顺序)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (record_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '模板-检测项关联表';

-- ----------------------------
-- 4、模板-产品关联表
-- 用途：定义模板适用的产品及其检验方案参数(抽检数量/不合格数/缺陷率阈值)
-- ----------------------------
drop table if exists qxx_qc_template_product;
create table qxx_qc_template_product (
  record_id            bigint(20)      not null auto_increment    comment '记录ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  template_id          bigint(20)      not null                   comment '检测模板ID(关联qxx_qc_template)',
  item_id              bigint(20)      not null                   comment '产品物料ID(关联qxx_md_item)',
  item_code            varchar(64)     default null               comment '产品物料编码',
  item_name            varchar(255)    default null               comment '产品物料名称',
  specification        varchar(500)    default null               comment '规格型号',
  unit_of_measure      varchar(64)     default null               comment '单位',
  quantity_check       int(11)         default 1                  comment '最低检测数(抽检样本量)',
  quantity_unqualified int(11)         default 0                  comment '最大不合格数(Ac值,超过则整批拒收)',
  cr_rate              double(12,2)    default 0.00               comment '最大致命缺陷率(CRITICAL,%)',
  maj_rate             double(12,2)    default 0.00               comment '最大严重缺陷率(MAJOR,%)',
  min_rate             double(12,2)    default 0.00               comment '最大轻微缺陷率(MINOR,%)',
  remark               varchar(500)    default ''                 comment '备注',
  create_by            varchar(64)     default ''                 comment '创建者',
  create_time          datetime        default current_timestamp  comment '创建时间',
  update_by            varchar(64)     default ''                 comment '更新者',
  update_time          datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (record_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '模板-产品关联表';

-- ----------------------------
-- 5、常见缺陷表
-- 用途：维护常见缺陷字典，纸袋行业特有缺陷如印刷偏位/色差/爆边/绳长偏差/胶合不牢
-- ----------------------------
drop table if exists qxx_qc_defect;
create table qxx_qc_defect (
  defect_id         bigint(20)      not null auto_increment    comment '缺陷ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  defect_code       varchar(64)     not null                   comment '缺陷编码(唯一)',
  defect_name       varchar(500)    not null                   comment '缺陷描述(如:印刷颜色偏深/袋底爆裂/绳长短于标准/胶合强度不足)',
  index_type        varchar(64)     not null                   comment '适用检验类型(IQC-来料检验/IPQC-过程检验/OQC-出货检验/RQC-退料检验)',
  defect_level      varchar(64)     not null                   comment '缺陷等级:CRITICAL-致命缺陷,MAJOR-严重缺陷,MINOR-轻微缺陷',
  process_method    varchar(500)    default null               comment '处置方法(如:返工/让步接收/退货/报废)',
  enable_flag       char(1)         default '1' not null       comment '是否启用(1-是,0-否)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (defect_id),
  unique key uk_defect_code (defect_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '常见缺陷表';

-- ============================================================
-- 二、来料检验
-- ============================================================

-- ----------------------------
-- 6、来料检验单表
-- 用途：对采购物料/外协半成品进行入库前检验，关联到货通知单和供应商
-- ----------------------------
drop table if exists qxx_qc_iqc;
create table qxx_qc_iqc (
  iqc_id                   bigint(20)      not null auto_increment    comment '来料检验单ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  iqc_code                 varchar(64)     not null                   comment '来料检验单编码(唯一)',
  iqc_name                 varchar(500)    default null               comment '来料检验单名称',
  template_id              bigint(20)      not null                   comment '检验模板ID(关联qxx_qc_template)',
  source_doc_id            bigint(20)      default null               comment '来源单据ID(关联qxx_wm_arrival_notice)',
  source_doc_type          varchar(64)     default null               comment '来源单据类型(如arrival_notice-到货通知单)',
  source_doc_code          varchar(64)     default null               comment '来源单据编码',
  source_line_id           bigint(20)      default null               comment '来源单据行ID(关联qxx_wm_arrival_notice_line)',
  vendor_id                bigint(20)      not null                   comment '供应商ID(关联qxx_md_vendor)',
  vendor_code              varchar(64)     default null               comment '供应商编码',
  vendor_name              varchar(255)    default null               comment '供应商全称',
  vendor_nick              varchar(255)    default null               comment '供应商简称',
  vendor_batch             varchar(64)     default null               comment '供应商批次号',
  item_id                  bigint(20)      not null                   comment '产品物料ID(关联qxx_md_item)',
  item_code                varchar(64)     default null               comment '产品物料编码',
  item_name                varchar(255)    default null               comment '产品物料名称',
  specification            varchar(500)    default null               comment '规格型号',
  unit_of_measure          varchar(64)     default null               comment '单位编码',
  quantity_min_check       int(11)         default 1                  comment '最低检测数(抽检样本量)',
  quantity_max_unqualified int(11)         default 0                  comment '最大不合格数(Ac值)',
  quantity_received        double(12,2)    not null                   comment '本次接收数量',
  quantity_check           int(11)         default null               comment '本次检测数量',
  quantity_qualified       int(11)         default 0                  comment '合格数',
  quantity_unqualified     int(11)         default 0                  comment '不合格数',
  cr_rate                  double(12,2)    default 0.00               comment '致命缺陷率(CRITICAL,%)',
  maj_rate                 double(12,2)    default 0.00               comment '严重缺陷率(MAJOR,%)',
  min_rate                 double(12,2)    default 0.00               comment '轻微缺陷率(MINOR,%)',
  cr_quantity              int(11)         default 0                  comment '致命缺陷数量',
  maj_quantity             int(11)         default 0                  comment '严重缺陷数量',
  min_quantity             int(11)         default 0                  comment '轻微缺陷数量',
  check_result             varchar(64)     default null               comment '检验结果:PASS-合格,FAIL-不合格,CONCESSION-让步接收',
  receive_date             datetime        default null               comment '来料日期',
  inspect_date             datetime        default null               comment '检验日期',
  inspector                varchar(64)     default null               comment '检验人员',
  status                   varchar(64)     default 'PENDING'          comment '单据状态:PENDING-待检验,INSPECTING-检验中,COMPLETED-已完成,CLOSED-已关闭',
  remark                   varchar(500)    default ''                 comment '备注',
  create_by                varchar(64)     default ''                 comment '创建者',
  create_time              datetime        default current_timestamp  comment '创建时间',
  update_by                varchar(64)     default ''                 comment '更新者',
  update_time              datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (iqc_id),
  unique key uk_iqc_code (iqc_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '来料检验单表';

-- ----------------------------
-- 7、来料检验单行表
-- 用途：来料检验单的检测项明细，记录每项检测的判定结果
-- ----------------------------
drop table if exists qxx_qc_iqc_line;
create table qxx_qc_iqc_line (
  line_id           bigint(20)      not null auto_increment    comment '记录ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  iqc_id            bigint(20)      not null                   comment '检验单ID(关联qxx_qc_iqc)',
  index_id          bigint(20)      not null                   comment '检测项ID(关联qxx_qc_index)',
  index_code        varchar(64)     default null               comment '检测项编码',
  index_name        varchar(255)    default null               comment '检测项名称',
  index_type        varchar(64)     default null               comment '检测项类型',
  qc_tool           varchar(255)    default null               comment '检测工具',
  check_method      varchar(500)    default null               comment '检测方法/要求',
  stander_val       double(12,4)    default null               comment '标准值',
  unit_of_measure   varchar(64)     default null               comment '单位',
  threshold_max     double(12,4)    default null               comment '误差上限',
  threshold_min     double(12,4)    default null               comment '误差下限',
  cr_quantity       int(11)         default 0                  comment '致命缺陷数量',
  maj_quantity      int(11)         default 0                  comment '严重缺陷数量',
  min_quantity      int(11)         default 0                  comment '轻微缺陷数量',
  line_result       varchar(64)     default null               comment '行检验结果:PASS-合格,FAIL-不合格',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (line_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '来料检验单行表';

-- ============================================================
-- 三、过程检验
-- ============================================================

-- ----------------------------
-- 8、过程检验单表
-- 用途：对生产过程进行巡检/首检/尾检，纸袋行业特有检测项：印刷颜色/制袋尺寸/绳长/胶合强度
-- ----------------------------
drop table if exists qxx_qc_ipqc;
create table qxx_qc_ipqc (
  ipqc_id                bigint(20)      not null auto_increment    comment '过程检验单ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  ipqc_code              varchar(64)     not null                   comment '过程检验单编码(唯一)',
  ipqc_name              varchar(255)    default null               comment '过程检验单名称',
  ipqc_type              varchar(64)     not null                   comment '检验类型:FIRST_CHECK-首检,TOUR_CHECK-巡检,LAST_CHECK-尾检,SPOT_CHECK-抽检',
  template_id            bigint(20)      not null                   comment '检验模板ID(关联qxx_qc_template)',
  source_doc_id          bigint(20)      default null               comment '来源单据ID',
  source_doc_type        varchar(64)     default null               comment '来源单据类型',
  source_doc_code        varchar(64)     default null               comment '来源单据编码',
  source_line_id         bigint(20)      default null               comment '来源单据行ID',
  workorder_id           bigint(20)      not null                   comment '生产工单ID(关联qxx_pro_workorder)',
  workorder_code         varchar(64)     default null               comment '生产工单编码',
  workorder_name         varchar(255)    default null               comment '生产工单名称',
  task_id                bigint(20)      default null               comment '生产任务ID(关联qxx_pro_task)',
  task_code              varchar(64)     default null               comment '生产任务编码',
  task_name              varchar(255)    default null               comment '生产任务名称',
  workstation_id         bigint(20)      not null                   comment '工作站ID(关联qxx_md_workstation)',
  workstation_code       varchar(64)     default null               comment '工作站编码',
  workstation_name       varchar(255)    default null               comment '工作站名称',
  process_id             bigint(20)      default null               comment '工序ID(关联qxx_pro_process)',
  process_code           varchar(64)     default null               comment '工序编码',
  process_name           varchar(255)    default null               comment '工序名称',
  item_id                bigint(20)      not null                   comment '产品物料ID(关联qxx_md_item)',
  item_code              varchar(64)     default null               comment '产品物料编码',
  item_name              varchar(255)    default null               comment '产品物料名称',
  specification          varchar(500)    default null               comment '规格型号',
  unit_of_measure        varchar(64)     default null               comment '单位编码',
  -- 纸袋行业特有过程检验字段
  print_color_result     varchar(64)     default null               comment '印刷颜色检测结果(纸袋特有:PASS-合格/FAIL-不合格,色差仪检测ΔE值)',
  bag_size_result        varchar(64)     default null               comment '制袋尺寸检测结果(纸袋特有:PASS-合格/FAIL-不合格,长宽高测量)',
  rope_length_result     varchar(64)     default null               comment '绳长检测结果(纸袋特有:PASS-合格/FAIL-不合格,测量绳料长度)',
  glue_strength_result   varchar(64)     default null               comment '胶合强度检测结果(纸袋特有:PASS-合格/FAIL-不合格,拉力测试)',
  bottom_result          varchar(64)     default null               comment '袋底检测结果(纸袋特有:PASS-合格/FAIL-不合格,袋底成型/加固检查)',
  mouth_result           varchar(64)     default null               comment '口部检测结果(纸袋特有:PASS-合格/FAIL-不合格,锯齿口/平口成型检查)',
  -- 检验数量统计
  quantity_check         double(12,4)    default 1.0000             comment '检测数量',
  quantity_qualified     double(12,4)    default 0.0000             comment '合格品数量',
  quantity_unqualified   double(12,4)    default 0.0000             comment '不合格数量',
  quantity_labor_scrap   double(12,4)    default null               comment '工废数量(操作原因报废)',
  quantity_material_scrap double(12,4)   default null               comment '料废数量(原料原因报废)',
  quantity_other_scrap   double(12,4)    default null               comment '其他报废数量',
  -- 缺陷统计
  cr_rate                double(12,2)    default 0.00               comment '致命缺陷率(CRITICAL,%)',
  maj_rate               double(12,2)    default 0.00               comment '严重缺陷率(MAJOR,%)',
  min_rate               double(12,2)    default 0.00               comment '轻微缺陷率(MINOR,%)',
  cr_quantity            double(12,4)    default 0.0000             comment '致命缺陷数量',
  maj_quantity           double(12,4)    default 0.0000             comment '严重缺陷数量',
  min_quantity           double(12,4)    default 0.0000             comment '轻微缺陷数量',
  check_result           varchar(64)     default null               comment '检验结果:PASS-合格,FAIL-不合格,REWORK-返工',
  inspect_date           datetime        default null               comment '检验日期',
  inspector              varchar(64)     default null               comment '检验人员',
  status                 varchar(64)     default 'PENDING'          comment '单据状态:PENDING-待检验,INSPECTING-检验中,COMPLETED-已完成,CLOSED-已关闭',
  remark                 varchar(500)    default ''                 comment '备注',
  create_by              varchar(64)     default ''                 comment '创建者',
  create_time            datetime        default current_timestamp  comment '创建时间',
  update_by              varchar(64)     default ''                 comment '更新者',
  update_time            datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (ipqc_id),
  unique key uk_ipqc_code (ipqc_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '过程检验单表';

-- ----------------------------
-- 9、过程检验单行表
-- 用途：过程检验单的检测项明细
-- ----------------------------
drop table if exists qxx_qc_ipqc_line;
create table qxx_qc_ipqc_line (
  line_id           bigint(20)      not null auto_increment    comment '记录ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  ipqc_id           bigint(20)      not null                   comment '检验单ID(关联qxx_qc_ipqc)',
  index_id          bigint(20)      not null                   comment '检测项ID(关联qxx_qc_index)',
  index_code        varchar(64)     default null               comment '检测项编码',
  index_name        varchar(255)    default null               comment '检测项名称',
  index_type        varchar(64)     default null               comment '检测项类型',
  qc_tool           varchar(255)    default null               comment '检测工具',
  check_method      varchar(500)    default null               comment '检测方法/要求',
  stander_val       double(12,4)    default null               comment '标准值',
  unit_of_measure   varchar(64)     default null               comment '单位',
  threshold_max     double(12,4)    default null               comment '误差上限',
  threshold_min     double(12,4)    default null               comment '误差下限',
  cr_quantity       double(12,4)    default 0.0000             comment '致命缺陷数量',
  maj_quantity      double(12,4)    default 0.0000             comment '严重缺陷数量',
  min_quantity      double(12,4)    default 0.0000             comment '轻微缺陷数量',
  line_result       varchar(64)     default null               comment '行检验结果:PASS-合格,FAIL-不合格',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (line_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '过程检验单行表';

-- ============================================================
-- 四、出货检验
-- ============================================================

-- ----------------------------
-- 10、出货检验单表
-- 用途：成品出货前最终检验，纸袋行业特有：装箱核对/外观检查/箱唛核对
-- ----------------------------
drop table if exists qxx_qc_oqc;
create table qxx_qc_oqc (
  oqc_id                   bigint(20)      not null auto_increment    comment '出货检验单ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  oqc_code                 varchar(64)     not null                   comment '出货检验单编码(唯一)',
  oqc_name                 varchar(500)    default null               comment '出货检验单名称',
  template_id              bigint(20)      not null                   comment '检验模板ID(关联qxx_qc_template)',
  source_doc_id            bigint(20)      default null               comment '来源单据ID(关联qxx_wm_sales_notice)',
  source_doc_type          varchar(64)     default null               comment '来源单据类型(sales_notice-发货通知单)',
  source_doc_code          varchar(64)     default null               comment '来源单据编码',
  source_line_id           bigint(20)      default null               comment '来源单据行ID(关联qxx_wm_sales_notice_line)',
  client_id                bigint(20)      not null                   comment '客户ID(关联qxx_md_client)',
  client_code              varchar(64)     default null               comment '客户编码',
  client_name              varchar(255)    default null               comment '客户全称',
  batch_code               varchar(64)     default null               comment '批次号',
  item_id                  bigint(20)      not null                   comment '产品物料ID(关联qxx_md_item)',
  item_code                varchar(64)     default null               comment '产品物料编码',
  item_name                varchar(255)    default null               comment '产品物料名称',
  specification            varchar(500)    default null               comment '规格型号',
  unit_of_measure          varchar(64)     default null               comment '单位编码',
  -- 出货特有检验字段(纸袋行业)
  box_check_result         varchar(64)     default null               comment '装箱核对结果(纸袋特有:PASS-合格/FAIL-不合格,核对装箱数量/箱规/标签)',
  appearance_result        varchar(64)     default null               comment '外观检查结果(纸袋特有:PASS-合格/FAIL-不合格,检查表面/印刷/清洁度)',
  box_mark_result          varchar(64)     default null               comment '箱唛核对结果(纸袋特有:PASS-合格/FAIL-不合格,核对唛头/收货人/PO号)',
  seal_result              varchar(64)     default null               comment '封箱检查结果(纸袋特有:PASS-合格/FAIL-不合格,检查封箱胶带/打包带)',
  pallet_result            varchar(64)     default null               comment '托盘检查结果(纸袋特有:PASS-合格/FAIL-不合格,检查托盘稳固/防潮)',
  -- 检验数量统计
  quantity_min_check       double(12,4)    default 1.0000             comment '最低检测数',
  quantity_max_unqualified double(12,4)    default 0.0000             comment '最大不合格数(Ac值)',
  quantity_out             double(12,4)    not null                   comment '发货数量',
  quantity_check           double(12,4)    not null                   comment '本次检测数量',
  quantity_unqualified     double(12,4)    default 0.0000             comment '不合格数量',
  quantity_qualified       double(12,4)    default 0.0000             comment '合格数量',
  cr_rate                  double(12,4)    default 0.0000             comment '致命缺陷率(CRITICAL,%)',
  maj_rate                 double(12,4)    default 0.0000             comment '严重缺陷率(MAJOR,%)',
  min_rate                 double(12,4)    default 0.0000             comment '轻微缺陷率(MINOR,%)',
  cr_quantity              double(12,4)    default 0.0000             comment '致命缺陷数量',
  maj_quantity             double(12,4)    default 0.0000             comment '严重缺陷数量',
  min_quantity             double(12,4)    default 0.0000             comment '轻微缺陷数量',
  check_result             varchar(64)     default null               comment '检验结果:PASS-合格,FAIL-不合格,CONCESSION-让步放行',
  out_date                 datetime        default null               comment '出货日期',
  inspect_date             datetime        default null               comment '检验日期',
  inspector                varchar(64)     default null               comment '检验人员',
  status                   varchar(64)     default 'PENDING'          comment '单据状态:PENDING-待检验,INSPECTING-检验中,COMPLETED-已完成,CLOSED-已关闭',
  remark                   varchar(500)    default ''                 comment '备注',
  create_by                varchar(64)     default ''                 comment '创建者',
  create_time              datetime        default current_timestamp  comment '创建时间',
  update_by                varchar(64)     default ''                 comment '更新者',
  update_time              datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (oqc_id),
  unique key uk_oqc_code (oqc_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '出货检验单表';

-- ----------------------------
-- 11、出货检验单行表
-- 用途：出货检验单的检测项明细
-- ----------------------------
drop table if exists qxx_qc_oqc_line;
create table qxx_qc_oqc_line (
  line_id           bigint(20)      not null auto_increment    comment '记录ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  oqc_id            bigint(20)      not null                   comment '检验单ID(关联qxx_qc_oqc)',
  index_id          bigint(20)      not null                   comment '检测项ID(关联qxx_qc_index)',
  index_code        varchar(64)     default null               comment '检测项编码',
  index_name        varchar(255)    default null               comment '检测项名称',
  index_type        varchar(64)     default null               comment '检测项类型',
  qc_tool           varchar(255)    default null               comment '检测工具',
  check_method      varchar(500)    default null               comment '检测方法/要求',
  stander_val       double(12,4)    default null               comment '标准值',
  unit_of_measure   varchar(64)     default null               comment '单位',
  threshold_max     double(12,4)    default null               comment '误差上限',
  threshold_min     double(12,4)    default null               comment '误差下限',
  cr_quantity       double(12,4)    default 0.0000             comment '致命缺陷数量',
  maj_quantity      double(12,4)    default 0.0000             comment '严重缺陷数量',
  min_quantity      double(12,4)    default 0.0000             comment '轻微缺陷数量',
  line_result       varchar(64)     default null               comment '行检验结果:PASS-合格,FAIL-不合格',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (line_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '出货检验单行表';

-- ============================================================
-- 五、退料检验
-- ============================================================

-- ----------------------------
-- 12、退料检验单表
-- 用途：对生产退料/退货进行检验，判定退料原因和责任归属
-- ----------------------------
drop table if exists qxx_qc_rqc;
create table qxx_qc_rqc (
  rqc_id                   bigint(20)      not null auto_increment    comment '退料检验单ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  rqc_code                 varchar(64)     not null                   comment '退料检验单编码(唯一)',
  rqc_name                 varchar(500)    default null               comment '退料检验单名称',
  template_id              bigint(20)      not null                   comment '检验模板ID(关联qxx_qc_template)',
  source_doc_id            bigint(20)      default null               comment '来源单据ID(退料通知单ID)',
  source_doc_type          varchar(64)     default null               comment '来源单据类型(return_notice-退料通知单)',
  source_doc_code          varchar(64)     default null               comment '来源单据编码',
  source_line_id           bigint(20)      default null               comment '来源单据行ID',
  rqc_type                 varchar(64)     default null               comment '退料检验类型:PROD_RETURN-生产退料,PURCHASE_RETURN-采购退货,QC_REJECT-质检退货',
  workorder_id             bigint(20)      default null               comment '生产工单ID(关联qxx_pro_workorder,生产退料时填写)',
  workorder_code           varchar(64)     default null               comment '生产工单编码',
  vendor_id                bigint(20)      default null               comment '供应商ID(关联qxx_md_vendor,采购退货时填写)',
  vendor_code              varchar(64)     default null               comment '供应商编码',
  vendor_name              varchar(255)    default null               comment '供应商全称',
  item_id                  bigint(20)      not null                   comment '产品物料ID(关联qxx_md_item)',
  item_code                varchar(64)     default null               comment '产品物料编码',
  item_name                varchar(255)    default null               comment '产品物料名称',
  specification            varchar(500)    default null               comment '规格型号',
  unit_of_measure          varchar(64)     default null               comment '单位编码',
  batch_id                 bigint(20)      default null               comment '批次ID(关联qxx_wm_batch)',
  batch_code               varchar(128)    default null               comment '批次号',
  quantity_check           double(12,4)    default 1.0000             comment '检测数量',
  quantity_qualified       double(12,4)    default 0.0000             comment '合格品数量',
  quantity_unqualified     double(12,4)    default 0.0000             comment '不合格数量',
  check_result             varchar(64)     default null               comment '检验结果:PASS-合格,FAIL-不合格,SCRAP-报废,REWORK-返工',
  return_reason            varchar(500)    default null               comment '退料原因(如:色差超标/尺寸偏差/胶合不牢/纸张破损)',
  responsibility           varchar(64)     default null               comment '责任归属:SUPPLIER-供应商,PRODUCTION-生产部门,STORAGE-仓储部门,OTHER-其他',
  inspect_date             datetime        default null               comment '检验日期',
  inspector                varchar(64)     default null               comment '检验人员',
  status                   varchar(64)     default 'PENDING'          comment '单据状态:PENDING-待检验,INSPECTING-检验中,COMPLETED-已完成,CLOSED-已关闭',
  remark                   varchar(500)    default ''                 comment '备注',
  create_by                varchar(64)     default ''                 comment '创建者',
  create_time              datetime        default current_timestamp  comment '创建时间',
  update_by                varchar(64)     default ''                 comment '更新者',
  update_time              datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (rqc_id),
  unique key uk_rqc_code (rqc_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '退料检验单表';

-- ----------------------------
-- 13、退料检验单行表
-- 用途：退料检验单的检测项明细
-- ----------------------------
drop table if exists qxx_qc_rqc_line;
create table qxx_qc_rqc_line (
  line_id           bigint(20)      not null auto_increment    comment '记录ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  rqc_id            bigint(20)      not null                   comment '检验单ID(关联qxx_qc_rqc)',
  index_id          bigint(20)      not null                   comment '检测项ID(关联qxx_qc_index)',
  index_code        varchar(64)     default null               comment '检测项编码',
  index_name        varchar(255)    default null               comment '检测项名称',
  index_type        varchar(64)     default null               comment '检测项类型',
  qc_tool           varchar(255)    default null               comment '检测工具',
  check_method      varchar(500)    default null               comment '检测方法/要求',
  stander_val       double(12,4)    default null               comment '标准值',
  unit_of_measure   varchar(64)     default null               comment '单位',
  threshold_max     double(12,4)    default null               comment '误差上限',
  threshold_min     double(12,4)    default null               comment '误差下限',
  cr_quantity       double(12,4)    default 0.0000             comment '致命缺陷数量',
  maj_quantity      double(12,4)    default 0.0000             comment '严重缺陷数量',
  min_quantity      double(12,4)    default 0.0000             comment '轻微缺陷数量',
  line_result       varchar(64)     default null               comment '行检验结果:PASS-合格,FAIL-不合格',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (line_id),
  key idx_factory_id (factory_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '退料检验单行表';

-- ============================================================
-- 六、缺陷与结果记录
-- ============================================================

-- ----------------------------
-- 14、检验缺陷记录表
-- 用途：多态关联各检验单(IQC/IPQC/OQC/RQC)，记录检验过程中发现的具体缺陷信息
-- ----------------------------
drop table if exists qxx_qc_defect_record;
create table qxx_qc_defect_record (
  record_id         bigint(20)      not null auto_increment    comment '缺陷记录ID',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  qc_type           varchar(64)     not null                   comment '检验单类型:IQC-来料检验,IPQC-过程检验,OQC-出货检验,RQC-退料检验',
  qc_id             bigint(20)      not null                   comment '检验单ID(多态关联,根据qc_type确定具体表)',
  line_id           bigint(20)      not null                   comment '检验单行ID(关联对应检验单行表)',
  defect_id         bigint(20)      default null               comment '缺陷ID(关联qxx_qc_defect)',
  defect_code       varchar(64)     default null               comment '缺陷编码',
  defect_name       varchar(500)    not null                   comment '缺陷描述(如:印刷色差超标ΔE>3/袋底爆裂/绳长偏短)',
  defect_level      varchar(64)     not null                   comment '缺陷等级:CRITICAL-致命缺陷,MAJOR-严重缺陷,MINOR-轻微缺陷',
  defect_quantity   int(11)         default 1                  comment '缺陷数量(发现该缺陷的样品数)',
  process_method    varchar(500)    default null               comment '处置方法(如:返工/让步接收/退货/报废/通知供应商)',
  defect_image      varchar(255)    default null               comment '缺陷图片URL(现场拍照留证)',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (record_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '检验缺陷记录表';

-- ----------------------------
-- 15、检测结果记录表
-- 用途：每个检验样本生成一条结果记录，关联具体的检验单行
-- ----------------------------
drop table if exists qxx_qc_result;
create table qxx_qc_result (
  result_id         bigint(20)      not null auto_increment    comment '检测结果ID',
  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  result_code       varchar(64)     not null                   comment '样品编号(如SAMPLE-001,唯一标识检测样本)',
  source_doc_id     bigint(20)      default null               comment '关联检验单ID(多态,根据source_doc_type确定)',
  source_doc_code   varchar(64)     default null               comment '关联检验单编码',
  source_doc_name   varchar(255)    default null               comment '关联检验单名称',
  source_doc_type   varchar(64)     default null               comment '关联检验单类型:IQC/IPQC/OQC/RQC',
  source_line_id    bigint(20)      default null               comment '关联检验单行ID',
  item_id           bigint(20)      not null                   comment '产品物料ID(关联qxx_md_item)',
  item_code         varchar(64)     default null               comment '产品物料编码',
  item_name         varchar(255)    default null               comment '产品物料名称',
  specification     varchar(500)    default null               comment '规格型号',
  unit_of_measure   varchar(64)     default null               comment '单位编码',
  sn_code           varchar(255)    default null               comment '物资SN号(唯一标识物料个体,用于追溯)',
  sample_position   varchar(64)     default null               comment '取样位置(如:印刷机出纸口/制袋机1号工位/卷料中部)',
  result_status     varchar(64)     default null               comment '样品结果:PASS-合格,FAIL-不合格',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (result_id),
  unique key uk_result_code (result_code)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '检测结果记录表';

-- ----------------------------
-- 16、检测结果明细表
-- 用途：每个检测结果下每个检测项的具体值，支持数值/文本/字典/文件/计数多种数据类型
-- ----------------------------
drop table if exists qxx_qc_result_detail;
create table qxx_qc_result_detail (
  detail_id         bigint(20)      not null auto_increment    comment '明细流水号',

  factory_id        bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  result_id         bigint(20)      not null                   comment '检测结果ID(关联qxx_qc_result)',
  index_id          bigint(20)      not null                   comment '检测项ID(关联qxx_qc_index)',
  index_code        varchar(64)     default null               comment '检测项编码',
  index_name        varchar(256)    default null               comment '检测项名称',
  index_type        varchar(64)     default null               comment '检测项类型',
  qc_tool           varchar(255)    default null               comment '检测工具',
  check_method      varchar(500)    default null               comment '检测方法/要求',
  stander_val       double(12,4)    default null               comment '标准值',
  unit_of_measure   varchar(64)     default null               comment '单位',
  threshold_max     double(12,4)    default null               comment '误差上限',
  threshold_min     double(12,4)    default null               comment '误差下限',
  qc_result_type    varchar(64)     not null                   comment '质检值类型:NUMBER-数值型,TEXT-文本型,DICT-字典型,FILE-文件型,COUNT-计数型',
  qc_result_spc     varchar(255)    default null               comment '值属性(数值型:长度mm/重量g/拉力N/色差值ΔE等)',
  -- 多态值字段(根据qc_result_type选择存储字段)
  qc_val_float      float(14,4)     default null               comment '浮点值(数值型检测结果,如测量值12.5000mm)',
  qc_val_integer    int(11)         default null               comment '整数值(计数型检测结果,如缺陷数3个)',
  qc_val_text       varchar(500)    default null               comment '文本值(文本型检测结果,如:颜色偏深/表面无划痕)',
  qc_val_dict       varchar(64)     default null               comment '字典值(字典型检测结果,如:OK-合格/NG-不合格/RE-返工)',
  qc_val_file       varchar(255)    default null               comment '文件URL(文件型检测结果,如现场检测照片/检测报告PDF)',
  defect_flag       varchar(64)     default 'NORMAL'           comment '判定结果:NORMAL-正常,ABNORMAL-异常,BELOW_LIMIT-低于下限,ABOVE_LIMIT-高于上限',
  remark            varchar(500)    default ''                 comment '备注',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime        default current_timestamp  comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  primary key (detail_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '检测结果明细表';
