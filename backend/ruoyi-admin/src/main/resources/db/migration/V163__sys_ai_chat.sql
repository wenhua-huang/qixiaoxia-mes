-- ============================================================
-- V163: 帮助中心 AI 问答助手
--   1. 会话表 qxx_sys_ai_chat_session（user_id 隔离键，逻辑删除）
--   2. 消息表 qxx_sys_ai_chat_message
--   3. sys_config 种子：DeepSeek 接入三参数（apiKey 留空，界面上填）
-- 全部语句幂等（CREATE TABLE IF NOT EXISTS / WHERE NOT EXISTS）。
-- ============================================================

SET NAMES utf8mb4;

-- ══════════ 1. AI 问答会话表 ══════════
CREATE TABLE IF NOT EXISTS qxx_sys_ai_chat_session (
    session_id   bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    factory_id   bigint       NOT NULL COMMENT '工厂ID',
    user_id      bigint       NOT NULL COMMENT '会话归属用户ID(会话隔离键)',
    title        varchar(100) DEFAULT NULL COMMENT '会话标题(首问截取20字)',
    del_flag     char(1)      NOT NULL DEFAULT '0' COMMENT '删除标志 0存在 2删除',
    create_by    varchar(64)  DEFAULT NULL COMMENT '创建者',
    create_time  datetime     DEFAULT NULL COMMENT '创建时间',
    update_by    varchar(64)  DEFAULT NULL COMMENT '更新者',
    update_time  datetime     DEFAULT NULL COMMENT '更新时间(最后一条消息时间)',
    remark       varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (session_id),
    KEY idx_user (factory_id, user_id, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI问答会话';

-- ══════════ 2. AI 问答消息表 ══════════
CREATE TABLE IF NOT EXISTS qxx_sys_ai_chat_message (
    msg_id      bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    factory_id  bigint       NOT NULL COMMENT '工厂ID',
    session_id  bigint       NOT NULL COMMENT '会话ID',
    user_id     bigint       NOT NULL COMMENT '提问用户ID',
    role        varchar(16)  NOT NULL COMMENT '角色 USER用户/ASSISTANT助手',
    content     text         NOT NULL COMMENT '消息内容',
    create_time datetime     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (msg_id),
    KEY idx_session (session_id, msg_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI问答消息';

-- ══════════ 3. sys_config 种子（sys 系统表，无 factory_id；config_type=N 可界面编辑） ══════════
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT 'AI助手-接口地址', 'mes.ai.baseUrl', 'https://api.deepseek.com', 'N', 'admin', NOW(), 'OpenAI 兼容 /chat/completions 服务地址'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'mes.ai.baseUrl');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT 'AI助手-API密钥', 'mes.ai.apiKey', '', 'N', 'admin', NOW(), 'DeepSeek API Key（sk-...），为空时 AI 助手不可用'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'mes.ai.apiKey');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT 'AI助手-模型', 'mes.ai.model', 'deepseek-chat', 'N', 'admin', NOW(), '对话模型名'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'mes.ai.model');
