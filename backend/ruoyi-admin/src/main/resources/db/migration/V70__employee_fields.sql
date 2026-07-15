-- ============================================================
-- 企小侠文化纸盒MES系统 — 员工管理字段扩展
-- 版本: v1.0
-- 日期: 2026-07-15
-- 说明: 扩展 sys_user 增加员工属性，新建员工技能表
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. sys_user 新增员工属性字段
-- ============================================================
ALTER TABLE sys_user ADD COLUMN openid         varchar(100)  DEFAULT NULL  COMMENT '微信openid(小程序登录绑定)';
ALTER TABLE sys_user ADD COLUMN wage_type      varchar(20)   DEFAULT NULL  COMMENT '工资类型:MONTHLY-月工资,PIECE-计件,HOURLY-计时';
ALTER TABLE sys_user ADD COLUMN employee_type  varchar(20)   DEFAULT NULL  COMMENT '员工类型:REGULAR-正式工,TEMPORARY-临时工';
ALTER TABLE sys_user ADD COLUMN hire_date      date          DEFAULT NULL  COMMENT '入职日期';

-- ============================================================
-- 2. 员工技能表
-- ============================================================
DROP TABLE IF EXISTS qxx_md_employee_skill;
CREATE TABLE qxx_md_employee_skill (
  skill_id            bigint(20)      not null auto_increment    comment '技能ID',
  factory_id          bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  user_id             bigint(20)      not null                   comment '用户ID(关联sys_user)',
  user_name           varchar(64)     default ''                 comment '用户姓名',
  skill_name          varchar(100)    not null                   comment '技能名称',
  skill_level         varchar(20)     default null               comment '技能等级:JUNIOR-初级,MIDDLE-中级,SENIOR-高级',
  remark              varchar(500)    default ''                 comment '备注',
  create_by           varchar(64)     default ''                 comment '创建者',
  create_time         datetime        default current_timestamp  comment '创建时间',
  update_by           varchar(64)     default ''                 comment '更新者',
  update_time         datetime        default current_timestamp on update current_timestamp comment '更新时间',
  key idx_factory_id (factory_id),
  key idx_user_id (user_id),
  primary key (skill_id)
) engine=innodb auto_increment=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '员工技能表';

-- ============================================================
-- 3. 字典类型（幂等：不存在才插入）
-- ============================================================

-- 工资类型
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '工资类型', 'mes_wage_type', '0', 'admin', NOW(), '员工工资计算方式：月工资、计件、计时'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mes_wage_type');

-- 员工类型
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '员工类型', 'mes_employee_type', '0', 'admin', NOW(), '员工用工类型：正式工、临时工'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mes_employee_type');

-- 技能等级
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '技能等级', 'mes_skill_level', '0', 'admin', NOW(), '员工技能等级：初级、中级、高级'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mes_skill_level');

-- ============================================================
-- 4. 字典数据（幂等：按 dict_type + dict_value 去重）
-- ============================================================

-- 工资类型 (mes_wage_type)
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '月工资', 'MONTHLY', 'mes_wage_type', '', 'primary', 'Y', '0', 'admin', NOW(), '按月发放固定工资'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_wage_type' AND dict_value = 'MONTHLY');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '计件', 'PIECE', 'mes_wage_type', '', 'success', 'N', '0', 'admin', NOW(), '按完成件数计算工资'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_wage_type' AND dict_value = 'PIECE');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '计时', 'HOURLY', 'mes_wage_type', '', 'warning', 'N', '0', 'admin', NOW(), '按工作小时计算工资'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_wage_type' AND dict_value = 'HOURLY');

-- 员工类型 (mes_employee_type)
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '正式工', 'REGULAR', 'mes_employee_type', '', 'primary', 'Y', '0', 'admin', NOW(), '正式劳动合同员工'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_employee_type' AND dict_value = 'REGULAR');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '临时工', 'TEMPORARY', 'mes_employee_type', '', 'warning', 'N', '0', 'admin', NOW(), '临时/短期用工'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_employee_type' AND dict_value = 'TEMPORARY');

-- 技能等级 (mes_skill_level)
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '初级', 'JUNIOR', 'mes_skill_level', '', 'info', 'Y', '0', 'admin', NOW(), '初级技能水平'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_skill_level' AND dict_value = 'JUNIOR');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '中级', 'MIDDLE', 'mes_skill_level', '', 'primary', 'N', '0', 'admin', NOW(), '中级技能水平'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_skill_level' AND dict_value = 'MIDDLE');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '高级', 'SENIOR', 'mes_skill_level', '', 'success', 'N', '0', 'admin', NOW(), '高级技能水平'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_skill_level' AND dict_value = 'SENIOR');
