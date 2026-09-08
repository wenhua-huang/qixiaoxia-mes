-- ============================================================
-- V148：自动排产总开关（全局 sys_config）
--
-- 背景：需求要求可用一个开关禁用「自动排产/自动分配」。关闭后：前端隐藏甘特图
--       「自动排产」按钮（工具栏 + 待排产工单卡片），后端 scheduleWorkOrder
--       也直接返回 error 不排产（含建/改工单、甘特懒排产等自动触发路径）；
--       需排产时在甘特图手工新增任务/指派机台。开启后一切照旧。
--
-- 说明：sys_config 为平台级全局表（无 factory_id，FactoryIdInterceptor 不注入），
--       与现有 mes.progress.* 阈值键一致，在【系统管理-参数设置】页维护，
--       保存即刷 Redis 缓存、后端 selectConfigByKey 实时生效。
--       口径：仅显式 "false" 停用；缺省/空/其他值视为启用（缺省安全）。
--       默认值：按客户要求生产默认【关闭(false)】——新环境上线即不自动排产，需手动开启。
--       config_key 无唯一索引，用 WHERE NOT EXISTS 保证幂等（可重复执行）。
-- ============================================================

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '自动排产开关', 'mes.pro.schedule.autoEnabled', 'false', 'Y', 'admin', sysdate(),
       '甘特图自动排产开关：false 停用(默认)——隐藏自动排产按钮、后端不再自动排产，需在甘特图手工新增任务/指派机台；true 启用自动排产'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE config_key = 'mes.pro.schedule.autoEnabled'
);
