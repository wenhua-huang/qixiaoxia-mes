/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `description` varchar(200) COLLATE utf8mb4_general_ci NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `script` varchar(1000) COLLATE utf8mb4_general_ci NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `gen_table` (
  `table_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_name` varchar(200) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '表名称',
  `table_comment` varchar(500) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '表描述',
  `sub_table_name` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联子表的表名',
  `sub_table_fk_name` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '子表关联的外键名',
  `class_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '实体类名称',
  `tpl_category` varchar(200) COLLATE utf8mb4_general_ci DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
  `tpl_web_type` varchar(30) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '前端模板类型（element-ui模版 element-plus模版）',
  `package_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生成包路径',
  `module_name` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生成模块名',
  `business_name` varchar(30) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生成业务名',
  `function_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生成功能名',
  `function_author` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生成功能作者',
  `form_col_num` int DEFAULT '1' COMMENT '表单布局（单列 双列 三列）',
  `gen_type` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
  `gen_path` varchar(200) COLLATE utf8mb4_general_ci DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
  `options` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '其它生成选项',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`table_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='代码生成业务表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `gen_table_column` (
  `column_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_id` bigint DEFAULT NULL COMMENT '归属表编号',
  `column_name` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '列名称',
  `column_comment` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '列描述',
  `column_type` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '列类型',
  `java_type` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'JAVA类型',
  `java_field` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'JAVA字段名',
  `is_pk` char(1) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否主键（1是）',
  `is_increment` char(1) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否自增（1是）',
  `is_required` char(1) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否必填（1是）',
  `is_insert` char(1) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否为插入字段（1是）',
  `is_edit` char(1) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否编辑字段（1是）',
  `is_list` char(1) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否列表字段（1是）',
  `is_query` char(1) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否查询字段（1是）',
  `query_type` varchar(200) COLLATE utf8mb4_general_ci DEFAULT 'EQ' COMMENT '查询方式（等于、不等于、大于、小于、范围）',
  `html_type` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
  `dict_type` varchar(200) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典类型',
  `sort` int DEFAULT NULL COMMENT '排序',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`column_id`)
) ENGINE=InnoDB AUTO_INCREMENT=179 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='代码生成业务表字段';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `QRTZ_BLOB_TRIGGERS` (
  `sched_name` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'è°ƒåº¦åç§°',
  `trigger_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggersè¡¨trigger_nameçš„å¤–é”®',
  `trigger_group` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggersè¡¨trigger_groupçš„å¤–é”®',
  `blob_data` blob COMMENT 'å­˜æ”¾æŒä¹…åŒ–Triggerå¯¹è±¡',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_blob_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `QRTZ_TRIGGERS` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Blobç±»åž‹çš„è§¦å‘å™¨è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `QRTZ_CALENDARS` (
  `sched_name` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'è°ƒåº¦åç§°',
  `calendar_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'æ—¥åŽ†åç§°',
  `calendar` blob NOT NULL COMMENT 'å­˜æ”¾æŒä¹…åŒ–calendarå¯¹è±¡',
  PRIMARY KEY (`sched_name`,`calendar_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='æ—¥åŽ†ä¿¡æ¯è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `QRTZ_CRON_TRIGGERS` (
  `sched_name` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'è°ƒåº¦åç§°',
  `trigger_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggersè¡¨trigger_nameçš„å¤–é”®',
  `trigger_group` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggersè¡¨trigger_groupçš„å¤–é”®',
  `cron_expression` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'cronè¡¨è¾¾å¼',
  `time_zone_id` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'æ—¶åŒº',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_cron_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `QRTZ_TRIGGERS` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Cronç±»åž‹çš„è§¦å‘å™¨è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `QRTZ_FIRED_TRIGGERS` (
  `sched_name` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'è°ƒåº¦åç§°',
  `entry_id` varchar(95) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'è°ƒåº¦å™¨å®žä¾‹id',
  `trigger_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggersè¡¨trigger_nameçš„å¤–é”®',
  `trigger_group` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggersè¡¨trigger_groupçš„å¤–é”®',
  `instance_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'è°ƒåº¦å™¨å®žä¾‹å',
  `fired_time` bigint NOT NULL COMMENT 'è§¦å‘çš„æ—¶é—´',
  `sched_time` bigint NOT NULL COMMENT 'å®šæ—¶å™¨åˆ¶å®šçš„æ—¶é—´',
  `priority` int NOT NULL COMMENT 'ä¼˜å…ˆçº§',
  `state` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'çŠ¶æ€',
  `job_name` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'ä»»åŠ¡åç§°',
  `job_group` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'ä»»åŠ¡ç»„å',
  `is_nonconcurrent` varchar(1) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'æ˜¯å¦å¹¶å‘',
  `requests_recovery` varchar(1) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'æ˜¯å¦æŽ¥å—æ¢å¤æ‰§è¡Œ',
  PRIMARY KEY (`sched_name`,`entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='å·²è§¦å‘çš„è§¦å‘å™¨è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `QRTZ_JOB_DETAILS` (
  `sched_name` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'è°ƒåº¦åç§°',
  `job_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'ä»»åŠ¡åç§°',
  `job_group` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'ä»»åŠ¡ç»„å',
  `description` varchar(250) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'ç›¸å…³ä»‹ç»',
  `job_class_name` varchar(250) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'æ‰§è¡Œä»»åŠ¡ç±»åç§°',
  `is_durable` varchar(1) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'æ˜¯å¦æŒä¹…åŒ–',
  `is_nonconcurrent` varchar(1) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'æ˜¯å¦å¹¶å‘',
  `is_update_data` varchar(1) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'æ˜¯å¦æ›´æ–°æ•°æ®',
  `requests_recovery` varchar(1) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'æ˜¯å¦æŽ¥å—æ¢å¤æ‰§è¡Œ',
  `job_data` blob COMMENT 'å­˜æ”¾æŒä¹…åŒ–jobå¯¹è±¡',
  PRIMARY KEY (`sched_name`,`job_name`,`job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='ä»»åŠ¡è¯¦ç»†ä¿¡æ¯è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `QRTZ_LOCKS` (
  `sched_name` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'è°ƒåº¦åç§°',
  `lock_name` varchar(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'æ‚²è§‚é”åç§°',
  PRIMARY KEY (`sched_name`,`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='å­˜å‚¨çš„æ‚²è§‚é”ä¿¡æ¯è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `QRTZ_PAUSED_TRIGGER_GRPS` (
  `sched_name` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'è°ƒåº¦åç§°',
  `trigger_group` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggersè¡¨trigger_groupçš„å¤–é”®',
  PRIMARY KEY (`sched_name`,`trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='æš‚åœçš„è§¦å‘å™¨è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `QRTZ_SCHEDULER_STATE` (
  `sched_name` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'è°ƒåº¦åç§°',
  `instance_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'å®žä¾‹åç§°',
  `last_checkin_time` bigint NOT NULL COMMENT 'ä¸Šæ¬¡æ£€æŸ¥æ—¶é—´',
  `checkin_interval` bigint NOT NULL COMMENT 'æ£€æŸ¥é—´éš”æ—¶é—´',
  PRIMARY KEY (`sched_name`,`instance_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='è°ƒåº¦å™¨çŠ¶æ€è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `QRTZ_SIMPLE_TRIGGERS` (
  `sched_name` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'è°ƒåº¦åç§°',
  `trigger_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggersè¡¨trigger_nameçš„å¤–é”®',
  `trigger_group` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggersè¡¨trigger_groupçš„å¤–é”®',
  `repeat_count` bigint NOT NULL COMMENT 'é‡å¤çš„æ¬¡æ•°ç»Ÿè®¡',
  `repeat_interval` bigint NOT NULL COMMENT 'é‡å¤çš„é—´éš”æ—¶é—´',
  `times_triggered` bigint NOT NULL COMMENT 'å·²ç»è§¦å‘çš„æ¬¡æ•°',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_simple_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `QRTZ_TRIGGERS` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='ç®€å•è§¦å‘å™¨çš„ä¿¡æ¯è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `QRTZ_SIMPROP_TRIGGERS` (
  `sched_name` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'è°ƒåº¦åç§°',
  `trigger_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggersè¡¨trigger_nameçš„å¤–é”®',
  `trigger_group` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggersè¡¨trigger_groupçš„å¤–é”®',
  `str_prop_1` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Stringç±»åž‹çš„triggerçš„ç¬¬ä¸€ä¸ªå‚æ•°',
  `str_prop_2` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Stringç±»åž‹çš„triggerçš„ç¬¬äºŒä¸ªå‚æ•°',
  `str_prop_3` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Stringç±»åž‹çš„triggerçš„ç¬¬ä¸‰ä¸ªå‚æ•°',
  `int_prop_1` int DEFAULT NULL COMMENT 'intç±»åž‹çš„triggerçš„ç¬¬ä¸€ä¸ªå‚æ•°',
  `int_prop_2` int DEFAULT NULL COMMENT 'intç±»åž‹çš„triggerçš„ç¬¬äºŒä¸ªå‚æ•°',
  `long_prop_1` bigint DEFAULT NULL COMMENT 'longç±»åž‹çš„triggerçš„ç¬¬ä¸€ä¸ªå‚æ•°',
  `long_prop_2` bigint DEFAULT NULL COMMENT 'longç±»åž‹çš„triggerçš„ç¬¬äºŒä¸ªå‚æ•°',
  `dec_prop_1` decimal(13,4) DEFAULT NULL COMMENT 'decimalç±»åž‹çš„triggerçš„ç¬¬ä¸€ä¸ªå‚æ•°',
  `dec_prop_2` decimal(13,4) DEFAULT NULL COMMENT 'decimalç±»åž‹çš„triggerçš„ç¬¬äºŒä¸ªå‚æ•°',
  `bool_prop_1` varchar(1) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Booleanç±»åž‹çš„triggerçš„ç¬¬ä¸€ä¸ªå‚æ•°',
  `bool_prop_2` varchar(1) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Booleanç±»åž‹çš„triggerçš„ç¬¬äºŒä¸ªå‚æ•°',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  CONSTRAINT `qrtz_simprop_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `trigger_name`, `trigger_group`) REFERENCES `QRTZ_TRIGGERS` (`sched_name`, `trigger_name`, `trigger_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='åŒæ­¥æœºåˆ¶çš„è¡Œé”è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `QRTZ_TRIGGERS` (
  `sched_name` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'è°ƒåº¦åç§°',
  `trigger_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'è§¦å‘å™¨çš„åå­—',
  `trigger_group` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'è§¦å‘å™¨æ‰€å±žç»„çš„åå­—',
  `job_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_job_detailsè¡¨job_nameçš„å¤–é”®',
  `job_group` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_job_detailsè¡¨job_groupçš„å¤–é”®',
  `description` varchar(250) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'ç›¸å…³ä»‹ç»',
  `next_fire_time` bigint DEFAULT NULL COMMENT 'ä¸Šä¸€æ¬¡è§¦å‘æ—¶é—´ï¼ˆæ¯«ç§’ï¼‰',
  `prev_fire_time` bigint DEFAULT NULL COMMENT 'ä¸‹ä¸€æ¬¡è§¦å‘æ—¶é—´ï¼ˆé»˜è®¤ä¸º-1è¡¨ç¤ºä¸è§¦å‘ï¼‰',
  `priority` int DEFAULT NULL COMMENT 'ä¼˜å…ˆçº§',
  `trigger_state` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'è§¦å‘å™¨çŠ¶æ€',
  `trigger_type` varchar(8) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'è§¦å‘å™¨çš„ç±»åž‹',
  `start_time` bigint NOT NULL COMMENT 'å¼€å§‹æ—¶é—´',
  `end_time` bigint DEFAULT NULL COMMENT 'ç»“æŸæ—¶é—´',
  `calendar_name` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'æ—¥ç¨‹è¡¨åç§°',
  `misfire_instr` smallint DEFAULT NULL COMMENT 'è¡¥å¿æ‰§è¡Œçš„ç­–ç•¥',
  `job_data` blob COMMENT 'å­˜æ”¾æŒä¹…åŒ–jobå¯¹è±¡',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`),
  KEY `sched_name` (`sched_name`,`job_name`,`job_group`),
  CONSTRAINT `qrtz_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `job_name`, `job_group`) REFERENCES `QRTZ_JOB_DETAILS` (`sched_name`, `job_name`, `job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='è§¦å‘å™¨è¯¦ç»†ä¿¡æ¯è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_cal_holiday` (
  `holiday_id` bigint NOT NULL AUTO_INCREMENT COMMENT '节假日设置ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `holiday_date` date NOT NULL COMMENT '日期',
  `holiday_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '节假日名称',
  `holiday_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型:HOLIDAY-节假日,WORKDAY-调休工作日',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`holiday_id`),
  UNIQUE KEY `uk_factory_holiday_date` (`factory_id`,`holiday_date`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=229 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节假日设置表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_cal_plan` (
  `plan_id` bigint NOT NULL AUTO_INCREMENT COMMENT '排班计划ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `plan_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '排班计划编码(唯一)',
  `plan_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '排班计划名称',
  `calendar_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '日历类型:WEEKLY-周历,MONTHLY-月历,QUARTERLY-季历,YEARLY-年历,CUSTOM-自定义',
  `start_date` date NOT NULL COMMENT '排班开始日期',
  `end_date` date NOT NULL COMMENT '排班结束日期',
  `shift_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '班次类型:TWOSHIFT-两班倒,THREESHIFT-三班倒,DAYONLY-常白班,CUSTOM-自定义',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT '计划状态:DRAFT-草稿,ACTIVE-生效中,CLOSED-已关闭',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`plan_id`),
  UNIQUE KEY `uk_plan_code` (`plan_code`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=210 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班计划表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_cal_plan_team` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联记录ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `plan_id` bigint NOT NULL COMMENT '排班计划ID(关联qxx_cal_plan)',
  `plan_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '排班计划编码',
  `plan_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '排班计划名称',
  `team_id` bigint NOT NULL COMMENT '班组ID(关联qxx_cal_team)',
  `team_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '班组编码',
  `team_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '班组名称',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=209 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计划班组关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_cal_shift` (
  `shift_id` bigint NOT NULL AUTO_INCREMENT COMMENT '班次ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `plan_id` bigint NOT NULL COMMENT '排班计划ID(关联qxx_cal_plan)',
  `shift_seq` int NOT NULL COMMENT '班次序号(如1-白班,2-夜班)',
  `shift_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '班次名称(如白班/夜班/中班)',
  `start_time` time NOT NULL COMMENT '班次开始时间(如08:00:00)',
  `end_time` time NOT NULL COMMENT '班次结束时间(如20:00:00)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`shift_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=210 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计划班次表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_cal_team` (
  `team_id` bigint NOT NULL AUTO_INCREMENT COMMENT '班组ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `team_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '班组编码(唯一)',
  `team_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '班组名称',
  `team_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '班组类型:DAY-白班,NIGHT-夜班,MIDDLE-中班,ROTATION-轮班',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`team_id`),
  UNIQUE KEY `uk_team_code` (`team_code`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=221 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班组表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_cal_team_member` (
  `member_id` bigint NOT NULL AUTO_INCREMENT COMMENT '成员关联ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `team_id` bigint NOT NULL COMMENT '班组ID(关联qxx_cal_team)',
  `team_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '班组编码',
  `team_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '班组名称',
  `user_id` bigint NOT NULL COMMENT '用户ID(关联sys_user)',
  `user_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户姓名',
  `phone` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系电话',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`member_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=214 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班组成员表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_cal_teamshift` (
  `teamshift_id` bigint NOT NULL AUTO_INCREMENT COMMENT '排班明细ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `shift_date` date NOT NULL COMMENT '排班日期',
  `team_id` bigint NOT NULL COMMENT '班组ID(关联qxx_cal_team)',
  `team_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '班组编码',
  `team_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '班组名称',
  `shift_id` bigint NOT NULL COMMENT '班次ID(关联qxx_cal_shift)',
  `shift_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '班次名称',
  `plan_id` bigint NOT NULL COMMENT '排班计划ID(关联qxx_cal_plan)',
  `plan_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '排班计划编码',
  `plan_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '排班计划名称',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `order_num` int DEFAULT '1' COMMENT '序号(1白班/2夜班)',
  PRIMARY KEY (`teamshift_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=246 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班组排班明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_dv_machinery` (
  `machinery_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'è®¾å¤‡ID',
  `factory_id` bigint NOT NULL COMMENT 'å·¥åŽ‚ID',
  `machinery_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'è®¾å¤‡ç¼–ç ',
  `machinery_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'è®¾å¤‡åç§°',
  `machinery_brand` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'è®¾å¤‡å“ç‰Œ',
  `machinery_spec` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'è®¾å¤‡è§„æ ¼åž‹å·',
  `machinery_type_id` bigint NOT NULL COMMENT 'è®¾å¤‡ç±»åž‹ID',
  `machinery_type_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'è®¾å¤‡ç±»åž‹ç¼–ç ',
  `machinery_type_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'è®¾å¤‡ç±»åž‹åç§°',
  `workshop_id` bigint DEFAULT '0' COMMENT 'æ‰€å±žè½¦é—´ID',
  `workshop_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'æ‰€å±žè½¦é—´ç¼–ç ',
  `workshop_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'æ‰€å±žè½¦é—´åç§°',
  `last_mainten_time` datetime DEFAULT NULL COMMENT 'æœ€åŽä¿å…»æ—¶é—´',
  `last_check_time` datetime DEFAULT NULL COMMENT 'æœ€åŽç‚¹æ£€æ—¶é—´',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'IDLE' COMMENT 'è®¾å¤‡çŠ¶æ€',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT 'æ˜¯å¦å¯ç”¨',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'å¤‡æ³¨',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'åˆ›å»ºè€…',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'æ›´æ–°è€…',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`machinery_id`),
  UNIQUE KEY `uk_machinery_code` (`machinery_code`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=212 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='è®¾å¤‡å°è´¦è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_dv_machinery_type` (
  `machinery_type_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'è®¾å¤‡ç±»åž‹ID',
  `factory_id` bigint NOT NULL COMMENT 'å·¥åŽ‚ID',
  `machinery_type_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'è®¾å¤‡ç±»åž‹ç¼–ç ',
  `machinery_type_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'è®¾å¤‡ç±»åž‹åç§°',
  `parent_type_id` bigint NOT NULL DEFAULT '0' COMMENT 'çˆ¶ç±»åž‹ID(0=æ ¹èŠ‚ç‚¹)',
  `order_num` int DEFAULT '0' COMMENT 'åŒçº§æŽ’åºå·',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT 'æ˜¯å¦å¯ç”¨',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'å¤‡æ³¨',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'åˆ›å»ºè€…',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'æ›´æ–°è€…',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`machinery_type_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=205 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='è®¾å¤‡ç±»åž‹è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_md_client` (
  `client_id` bigint NOT NULL AUTO_INCREMENT COMMENT '客户ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `client_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '客户编码(唯一)',
  `client_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '客户全称',
  `client_nick` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户简称',
  `client_en` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户英文名称(外贸客户必填)',
  `client_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户类型:DOMESTIC-内贸,FOREIGN-外贸,SPOT-现货',
  `salesperson` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务员姓名(如陈仁林/陈丽丽)',
  `credit_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '统一社会信用代码',
  `address` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户地址',
  `website` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户官网',
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户邮箱',
  `tel` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户电话',
  `contact1` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系人1姓名',
  `contact1_tel` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系人1电话',
  `contact1_email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系人1邮箱',
  `contact2` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系人2姓名',
  `contact2_tel` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系人2电话',
  `contact2_email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系人2邮箱',
  `shipping_address` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '收货地址(可多个,分号分隔)',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`client_id`),
  UNIQUE KEY `uk_client_code` (`client_code`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=209 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_md_factory` (
  `factory_id` bigint NOT NULL AUTO_INCREMENT COMMENT '工厂ID',
  `factory_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工厂编码(如SX-圣享,WL-万隆)',
  `factory_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工厂全称',
  `short_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工厂简称',
  `address` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工厂地址',
  `contact` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工厂负责人',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系电话',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`factory_id`),
  UNIQUE KEY `uk_factory_code` (`factory_code`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=103 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工厂定义表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_md_item` (
  `item_id` bigint NOT NULL AUTO_INCREMENT COMMENT '产品物料ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品物料编码(唯一)',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号描述',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主单位编码(如PCS-个,ROLL-卷,KG-千克,TON-吨,M-米)',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主单位名称',
  `unit2` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅助单位编码(如ROLL-卷/TON-吨,纸袋/礼品盒通用)',
  `unit2_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅助单位名称',
  `conversion_rate` decimal(10,4) DEFAULT '1.0000' COMMENT '主单位→辅助单位换算率(如1卷=0.697吨)',
  `item_type_id` bigint DEFAULT '0' COMMENT '物料类型ID(关联qxx_md_item_type,变体继承父产品)',
  `item_type_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '物料类型编码(变体继承父产品,后端带出)',
  `item_type_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '物料类型名称(变体继承父产品,后端带出)',
  `parent_id` bigint DEFAULT '0' COMMENT '父产品ID(0=顶层SPU/原料,非0=变体,parent_id>0时item_type_id/code/name继承父产品)',
  `product_size` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '产品尺寸(长*宽*高mm),如254*127*330mm',
  `package_spec` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '装箱规格(XX个/箱),如250个/箱',
  `printing_req` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '印刷要求描述,如1色满版黑印刷',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `safe_stock_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '是否设置安全库存(1-是,0-否)',
  `min_stock` decimal(14,4) DEFAULT '0.0000' COMMENT '安全库存最低量(主单位)',
  `max_stock` decimal(14,4) DEFAULT '0.0000' COMMENT '安全库存最高量(主单位)',
  `alert_stock` decimal(14,4) DEFAULT '0.0000' COMMENT '预警库存量(低于此值触发预警)',
  `high_value` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '是否高价值物资(1-是,0-否)',
  `batch_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用批次管理(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`item_id`),
  UNIQUE KEY `uk_item_code` (`item_code`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=247 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物料产品表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_md_item_attr_gift_box` (
  `attr_id` bigint NOT NULL AUTO_INCREMENT COMMENT '属性ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`attr_id`),
  UNIQUE KEY `uk_item_id` (`item_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物料礼品盒属性表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_md_item_attr_paper` (
  `attr_id` bigint NOT NULL AUTO_INCREMENT COMMENT '属性ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `paper_width` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '纸张门幅(mm),如925mm',
  `paper_weight` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '纸张克重(g),如120g',
  `paper_source` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '纸张来源/品牌,如联盛A2/蓝叶-牛卡',
  `paper_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '纸张种类:乌卡/俄卡/箱板纸/白牛皮/TC箱板纸/瑞典赤牛',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`attr_id`),
  UNIQUE KEY `uk_item_id` (`item_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=205 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物料纸张属性表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_md_item_attr_paper_bag` (
  `attr_id` bigint NOT NULL AUTO_INCREMENT COMMENT '属性ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `rope_spec` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '绳料规格要求,如7.5cm间距黄牛皮色圆纸绳',
  `mouth_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '口部提拔:锯齿口/平口/翻口',
  `bottom_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '底板类型:无/灰底白板/加强底板',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`attr_id`),
  UNIQUE KEY `uk_item_id` (`item_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=222 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物料纸袋成品属性表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_md_item_batch_config` (
  `config_id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `produce_date_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '是否追踪生产日期(1-是,0-否)',
  `expire_date_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '是否追踪有效期(1-是,0-否)',
  `recpt_date_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '是否追踪入库日期(1-是,0-否)',
  `vendor_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '是否追踪供应商(1-是,0-否)',
  `client_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '是否追踪客户(1-是,0-否)',
  `co_code_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '是否追踪销售订单号(1-是,0-否)',
  `po_code_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '是否追踪采购订单号(1-是,0-否)',
  `workorder_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '是否追踪生产工单(1-是,0-否)',
  `task_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '是否追踪生产任务(1-是,0-否)',
  `workstation_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '是否追踪工作站(1-是,0-否)',
  `tool_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '是否追踪工具(1-是,0-否)',
  `mold_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '是否追踪模具(1-是,0-否)',
  `lot_number_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '是否追踪生产批号(1-是,0-否)',
  `quality_status_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '是否追踪质量状态(1-是,0-否)',
  `paper_roll_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '是否追踪纸卷号(纸张行业可选,非纸张物料=0,1-是,0-否)',
  `paper_width_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '是否追踪门幅(纸张行业可选,非纸张物料=0,1-是,0-否)',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`config_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=202 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物料批次属性配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_md_item_type` (
  `item_type_id` bigint NOT NULL AUTO_INCREMENT COMMENT '产品物料类型ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `item_type_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品物料类型编码',
  `item_type_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品物料类型名称',
  `parent_type_id` bigint NOT NULL DEFAULT '0' COMMENT '父类型ID(0表示根节点,MySQL 8.0递归CTE查子树)',
  `item_or_product` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品物料标识:RAW-原料,SEMI-半成品,FINISHED-成品,AUXILIARY-辅料,PACK-包材',
  `order_num` int DEFAULT '1' COMMENT '同级排序号',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`item_type_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=225 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物料产品分类表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_md_item_vendor` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'è®°å½•ID',
  `factory_id` bigint NOT NULL DEFAULT '1' COMMENT 'å·¥åŽ‚ID',
  `item_id` bigint NOT NULL COMMENT 'ç‰©æ–™ID(å…³è”qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ç‰©æ–™ç¼–ç ',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ç‰©æ–™åç§°',
  `vendor_id` bigint NOT NULL COMMENT 'ä¾›åº”å•†ID(å…³è”qxx_md_vendor)',
  `vendor_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ä¾›åº”å•†ç¼–ç ',
  `vendor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ä¾›åº”å•†åç§°',
  `is_preferred` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT 'æ˜¯å¦é¦–é€‰ä¾›åº”å•†(Y/N)',
  `min_order_qty` decimal(14,2) DEFAULT NULL COMMENT 'æœ€å°èµ·è®¢é‡',
  `lead_time_days` int DEFAULT NULL COMMENT 'é‡‡è´­æå‰æœŸ(å¤©)',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT 'æ˜¯å¦å¯ç”¨(1-æ˜¯,0-å¦)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'å¤‡æ³¨',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'åˆ›å»ºè€…',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'æ›´æ–°è€…',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  PRIMARY KEY (`record_id`),
  UNIQUE KEY `uk_item_vendor` (`factory_id`,`item_id`,`vendor_id`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_vendor_id` (`vendor_id`),
  KEY `idx_item_id` (`item_id`)
) ENGINE=InnoDB AUTO_INCREMENT=201 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ç‰©æ–™ä¾›åº”å•†å…³ç³»è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_md_product_bom` (
  `bom_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'BOM行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `item_id` bigint NOT NULL COMMENT '成品物料ID(关联qxx_md_item)',
  `bom_item_id` bigint NOT NULL COMMENT 'BOM子物料ID(关联qxx_md_item,名称/规格/编码通过JOIN获取)',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '子物料BOM用量单位(可能≠库存单位,如库存按卷/BOM按kg)',
  `quantity` decimal(14,4) NOT NULL DEFAULT '1.0000' COMMENT '单个成品消耗子物料数量',
  `scrap_rate` decimal(5,4) DEFAULT '0.0000' COMMENT '损耗率(如0.05=5%)',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注(如绳长/间距等说明)',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`bom_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=279 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品BOM关系表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_md_product_sip` (
  `sip_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'SIP ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `item_id` bigint NOT NULL COMMENT '产品ID(关联qxx_md_item)',
  `sip_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SIP名称',
  `sip_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'SIP文件URL',
  `order_num` int DEFAULT '1' COMMENT '排序号',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`sip_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品SIP表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_md_product_sop` (
  `sop_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'SOP ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `item_id` bigint NOT NULL COMMENT '产品ID(关联qxx_md_item)',
  `sop_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SOP名称',
  `sop_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'SOP文件URL',
  `order_num` int DEFAULT '1' COMMENT '排序号',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`sop_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品SOP表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_md_unit_measure` (
  `unit_id` bigint NOT NULL AUTO_INCREMENT COMMENT '单位ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `unit_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码(如PCS/ROLL/KG/TON/M/G)',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称(如个/卷/千克/吨/米/克)',
  `primary_unit` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属主单位编码(当本单位是辅助单位时)',
  `conversion_rate` decimal(14,6) DEFAULT '1.000000' COMMENT '与主单位的换算率(如1吨=1000千克)',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`unit_id`),
  UNIQUE KEY `uk_unit_code` (`unit_code`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=231 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='单位管理表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_md_vendor` (
  `vendor_id` bigint NOT NULL AUTO_INCREMENT COMMENT '供应商ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `vendor_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '供应商编码(唯一)',
  `vendor_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '供应商全称',
  `outsource_factory_id` bigint DEFAULT NULL COMMENT '外协场景：该供应商对应的系统工厂ID(关联qxx_md_factory)',
  `vendor_nick` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商简称',
  `vendor_en` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商英文名称',
  `vendor_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'MATERIAL' COMMENT '供应商类型:MATERIAL-原材料供应商,OUTSOURCE-外协加工商,BOTH-两者皆是',
  `vendor_des` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商简介/经营范围',
  `vendor_logo` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商LOGO图片地址',
  `vendor_level` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商等级:A-优秀,B-良好,C-一般,D-待评估',
  `vendor_score` int DEFAULT '0' COMMENT '供应商评分(0-100)',
  `address` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商详细地址',
  `website` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商官网地址',
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商企业邮箱',
  `tel` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商企业电话',
  `contact1` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主要联系人姓名',
  `contact1_tel` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主要联系人电话',
  `contact1_email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主要联系人邮箱',
  `contact2` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备用联系人姓名',
  `contact2_tel` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备用联系人电话',
  `contact2_email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备用联系人邮箱',
  `credit_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '统一社会信用代码',
  `settlement_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '结算方式:MONTHLY-月结,CASH-现结,OTHER-其他',
  `payment_terms` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '付款条件/账期',
  `coop_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE' COMMENT '合作状态:ACTIVE-合作中,INACTIVE-暂停,PENDING-待审核,STOPPED-终止',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`vendor_id`),
  UNIQUE KEY `uk_vendor_code` (`vendor_code`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_outsource_factory_id` (`outsource_factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=216 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_md_workshop` (
  `workshop_id` bigint NOT NULL AUTO_INCREMENT COMMENT '车间ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `workshop_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '车间编码(如PRINT-印刷车间,BAG-制袋车间)',
  `workshop_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '车间名称',
  `address` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '车间位置/地址',
  `manager` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '车间负责人',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`workshop_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=210 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='车间管理表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_md_workstation` (
  `workstation_id` bigint NOT NULL AUTO_INCREMENT COMMENT '工作站ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `workstation_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工作站编码(如PRINT-01/BAG-01)',
  `workstation_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工作站名称(如1号印刷机/2号全自动制袋机)',
  `workshop_id` bigint NOT NULL COMMENT '所属车间ID(关联qxx_md_workshop)',
  `workstation_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作站类型:PRINT-印刷机,BAG_AUTO-全自动制袋机,BAG_SEMI-半自动制袋机,OTHER-其他',
  `process_id` bigint DEFAULT NULL COMMENT '工序ID(关联qxx_pro_process)',
  `process_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '可执行工序类型:PRINT-印刷,BAG_MAKE-制袋,SLITTING-分切,INSPECT-检验',
  `capacity` int DEFAULT '0' COMMENT '单位时间产能(个/小时)',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'IDLE' COMMENT '当前状态:IDLE-空闲,RUNNING-运行中,MAINTENANCE-保养中,BREAKDOWN-故障',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`workstation_id`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_process_id` (`process_id`)
) ENGINE=InnoDB AUTO_INCREMENT=220 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作站管理表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_md_workstation_machine` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID(关联qxx_md_workstation)',
  `machinery_id` bigint NOT NULL COMMENT '设备ID(关联qxx_dv_machinery)',
  `machinery_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备编码',
  `machinery_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备名称',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=201 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作站-设备关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_md_workstation_worker` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID(关联qxx_md_workstation)',
  `user_id` bigint NOT NULL COMMENT '用户ID(关联sys_user)',
  `user_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户姓名',
  `role_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'OPERATOR' COMMENT '角色:OPERATOR-操作工,SETUP-调机员,INSPECTOR-检验员',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=201 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作站-人员关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_andon_config` (
  `config_id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `andon_reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '呼叫原因(如:设备故障/缺料/质量问题/堵料)',
  `andon_level` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '紧急级别:LEVEL1-紧急(停产),LEVEL2-重要(影响效率),LEVEL3-一般(需协助)',
  `handler_role_id` bigint DEFAULT NULL COMMENT '默认处置角色ID',
  `handler_role_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '默认处置角色名称',
  `handler_user_id` bigint DEFAULT NULL COMMENT '默认处置人ID',
  `handler_user_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '默认处置人用户名',
  `handler_nick_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '默认处置人昵称',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`config_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='安灯呼叫配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_andon_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID(关联qxx_md_workstation)',
  `workstation_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作站编码',
  `workstation_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作站名称',
  `user_id` bigint NOT NULL COMMENT '呼叫用户ID',
  `user_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '呼叫用户名',
  `nick_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '呼叫用户昵称',
  `workorder_id` bigint DEFAULT NULL COMMENT '关联生产工单ID(关联qxx_pro_workorder)',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联生产工单编码',
  `workorder_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联生产工单名称',
  `process_id` bigint DEFAULT NULL COMMENT '关联工序ID(关联qxx_pro_process)',
  `process_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联工序编码',
  `process_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联工序名称',
  `machinery_id` bigint DEFAULT NULL COMMENT '关联设备ID(关联qxx_dv_machinery)',
  `machinery_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联设备编码',
  `machinery_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联设备名称',
  `andon_reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '呼叫原因',
  `andon_level` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT 'LEVEL3' COMMENT '紧急级别:LEVEL1-紧急,LEVEL2-重要,LEVEL3-一般',
  `handle_time` datetime DEFAULT NULL COMMENT '处置完成时间',
  `handler_user_id` bigint DEFAULT NULL COMMENT '处置人ID',
  `handler_user_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '处置人用户名',
  `handler_nick_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '处置人昵称',
  `status` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE' COMMENT '处置状态:ACTIVE-呼叫中,HANDLING-处置中,CLOSED-已关闭',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='安灯呼叫记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_card` (
  `card_id` bigint NOT NULL AUTO_INCREMENT COMMENT '流转卡ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `card_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '流转卡编码(唯一)',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID(关联qxx_pro_workorder)',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产工单编码',
  `workorder_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产工单名称',
  `task_id` bigint DEFAULT NULL COMMENT '生产任务ID(关联qxx_pro_task)',
  `task_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产任务编码',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产批次号',
  `item_id` bigint DEFAULT NULL COMMENT '产品物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主单位名称',
  `barcode_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '流转卡赋码地址(条码/二维码图片URL)',
  `quantity_transfered` decimal(12,2) DEFAULT NULL COMMENT '流转数量(本流转卡承载的数量)',
  `current_process_id` bigint DEFAULT NULL COMMENT '当前工序ID(关联qxx_pro_process,记录流转卡所在工序)',
  `current_process_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '当前工序名称',
  `status` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '流转卡状态:ACTIVE-流转中,COMPLETED-已完工,SCRAPPED-已报废',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`card_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=220 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流转卡表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_card_process` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `card_id` bigint NOT NULL COMMENT '流转卡ID(关联qxx_pro_card)',
  `card_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '流转卡编码',
  `seq_num` int DEFAULT '1' COMMENT '工序序号(第几道工序)',
  `process_id` bigint DEFAULT NULL COMMENT '工序ID(关联qxx_pro_process)',
  `process_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工序编码',
  `process_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工序名称',
  `process_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'INTERNAL' COMMENT '工序类型:INTERNAL-自制工序,OUTSOURCE-外发工序,SLITTING-分切工序',
  `task_id` bigint DEFAULT NULL COMMENT '排产任务ID(关联qxx_pro_task)',
  `task_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '排产任务编码',
  `input_time` datetime DEFAULT NULL COMMENT '进入工序时间(上道工序完成时间)',
  `output_time` datetime DEFAULT NULL COMMENT '出工序时间(本道工序完成时间)',
  `quantity_input` decimal(12,2) DEFAULT NULL COMMENT '投入数量',
  `quantity_output` decimal(12,2) DEFAULT NULL COMMENT '产出数量',
  `quantity_unqualified` decimal(12,2) DEFAULT NULL COMMENT '不合格品数量',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID(关联qxx_md_workstation)',
  `workstation_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作站编码',
  `workstation_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作站名称',
  `user_id` bigint NOT NULL COMMENT '操作人用户ID',
  `user_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人用户名',
  `nick_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人昵称',
  `vendor_id` bigint DEFAULT NULL COMMENT '外协厂ID(关联qxx_md_vendor,外发工序专用)',
  `vendor_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外协厂编码',
  `vendor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外协厂名称',
  `outsource_factory_id` bigint DEFAULT NULL COMMENT '外协场景：该供应商对应的系统工厂ID(关联qxx_md_factory)',
  `ipqc_id` bigint DEFAULT NULL COMMENT '过程检验单ID(关联qxx_qc_ipqc)',
  `ipqc_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '过程检验单编码',
  `feedback_id` bigint DEFAULT NULL COMMENT '报工记录ID(关联qxx_pro_feedback)',
  `issue_detail_id` bigint DEFAULT NULL COMMENT '领料明细ID(关联qxx_wm_issue_detail,建立card↔原料批次直接追溯)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_outsource_factory_id` (`outsource_factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=211 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流转卡工序信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_changeover` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '换型ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID',
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '换型名称',
  `from_process_id` bigint DEFAULT NULL COMMENT '从工序ID(NULL=任意工序)',
  `to_process_id` bigint NOT NULL COMMENT '到工序ID',
  `workstation_id` bigint DEFAULT NULL COMMENT '工作站ID(NULL=通用)',
  `duration_minutes` int NOT NULL DEFAULT '0' COMMENT '换型时长(分钟)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_from_to_ws` (`from_process_id`,`to_process_id`,`workstation_id`)
) ENGINE=InnoDB AUTO_INCREMENT=202 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工序换型时间规范表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_doc_generation_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'æ—¥å¿—ID',
  `factory_id` bigint NOT NULL COMMENT 'å·¥åŽ‚ID(å…³è”qxx_md_factory)',
  `workorder_id` bigint NOT NULL COMMENT 'å·¥å•ID(å…³è”qxx_pro_workorder)',
  `doc_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'å•æ®ç±»åž‹:ISSUE-é¢†æ–™å•,RETURN-é€€æ–™å•,RECPT-å…¥åº“å•,PUR_ORDER-é‡‡è´­å•',
  `doc_id` bigint NOT NULL COMMENT 'ç”Ÿæˆçš„ç›®æ ‡å•æ®ID',
  `doc_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ç”Ÿæˆçš„ç›®æ ‡å•æ®ç¼–ç ',
  `generation_batch` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ç”Ÿæˆæ‰¹æ¬¡å·(UUID,åŒä¸€æ‰¹æ“ä½œå…±äº«)',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE' COMMENT 'ACTIVE-æœ‰æ•ˆ,REVOKED-å·²æ’¤é”€',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  PRIMARY KEY (`log_id`),
  UNIQUE KEY `uk_wo_doc` (`workorder_id`,`doc_type`,`doc_id`),
  KEY `idx_workorder_id` (`workorder_id`),
  KEY `idx_generation_batch` (`generation_batch`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=255 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='å·¥å•å•æ®ç”Ÿæˆæ—¥å¿—(å¹‚ç­‰ä¿éšœ)';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_feedback` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `feedback_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '报工类型:INTERNAL-自制报工,OUTSOURCE_AGENT-外发代填(我方代录),OUTSOURCE_VENDOR-外发直填(外协厂自主报工)',
  `feedback_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '报工单编码',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID(关联qxx_md_workstation)',
  `workstation_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作站编码',
  `workstation_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作站名称',
  `workorder_id` bigint NOT NULL COMMENT '生产工单ID(关联qxx_pro_workorder)',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产工单编码',
  `workorder_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产工单名称',
  `route_id` bigint NOT NULL COMMENT '工艺路线ID(关联qxx_pro_route)',
  `route_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工艺路线编码',
  `process_id` bigint NOT NULL COMMENT '工序ID(关联qxx_pro_process)',
  `process_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工序编码',
  `process_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工序名称',
  `task_id` bigint DEFAULT NULL COMMENT '生产任务ID(关联qxx_pro_task)',
  `task_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产任务编码',
  `item_id` bigint NOT NULL COMMENT '产品物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品物料名称',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主单位名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `vendor_id` bigint DEFAULT NULL COMMENT '外协厂ID(关联qxx_md_vendor,feedback_type为外发时必填)',
  `vendor_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外协厂编码',
  `vendor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外协厂名称',
  `outsource_factory_id` bigint DEFAULT NULL COMMENT '外协场景：该供应商对应的系统工厂ID(关联qxx_md_factory)',
  `outsource_issue_id` bigint DEFAULT NULL COMMENT '外协领料单ID(关联qxx_wm_outsource_issue,外发工序报工时关联)',
  `outsource_issue_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外协领料单编码',
  `outsource_recpt_id` bigint DEFAULT NULL COMMENT '外协入库单ID(关联qxx_wm_outsource_recpt,外协完工返回时关联)',
  `expire_date` datetime DEFAULT NULL COMMENT '有效期至',
  `lot_number` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产批号',
  `quantity` decimal(14,2) DEFAULT NULL COMMENT '排产数量(任务计划数)',
  `quantity_feedback` decimal(14,2) DEFAULT NULL COMMENT '本次报工数量(工序产出总数)',
  `quantity_qualified` decimal(14,2) DEFAULT NULL COMMENT '合格品数量',
  `quantity_unqualified` decimal(14,2) DEFAULT NULL COMMENT '不合格品数量',
  `quantity_uncheck` decimal(14,2) DEFAULT NULL COMMENT '待检测数量(待质检判定)',
  `quantity_labor_scrap` decimal(14,2) DEFAULT NULL COMMENT '工废数量(操作原因报废)',
  `quantity_material_scrap` decimal(14,2) DEFAULT NULL COMMENT '料废数量(材料原因报废)',
  `quantity_other_scrap` decimal(14,2) DEFAULT NULL COMMENT '其他报废数量',
  `user_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '报工用户名(操作工账号)',
  `nick_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '报工用户昵称',
  `feedback_channel` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '报工途径:PC-电脑端,PAD-平板端,SCAN-扫码枪,WECHAT-微信小程序',
  `feedback_time` datetime DEFAULT NULL COMMENT '报工时间',
  `record_user` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '记录人(录单人)',
  `record_nick` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '记录人昵称',
  `status` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT 'PREPARE' COMMENT '报工状态:PREPARE-待确认,CONFIRMED-已确认,AUDITED-已审核',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_outsource_factory_id` (`outsource_factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=249 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报工记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_feedback_consume` (
  `consume_id` bigint NOT NULL AUTO_INCREMENT COMMENT '消耗ID',
  `factory_id` bigint NOT NULL DEFAULT '1' COMMENT '工厂ID',
  `feedback_id` bigint NOT NULL COMMENT '报工ID(关联qxx_pro_feedback.record_id)',
  `workorder_id` bigint DEFAULT NULL COMMENT '工单ID(关联qxx_pro_workorder)',
  `item_id` bigint DEFAULT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '物料编码',
  `item_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '物料名称',
  `quantity` decimal(18,4) DEFAULT NULL COMMENT '消耗数量',
  `batch_code` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '批次号',
  PRIMARY KEY (`consume_id`),
  KEY `idx_feedback_id` (`feedback_id`)
) ENGINE=InnoDB AUTO_INCREMENT=400 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报工物料消耗表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_feedback_param` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `feedback_id` bigint NOT NULL COMMENT '报工记录ID(关联qxx_pro_feedback.record_id)',
  `workorder_param_id` bigint DEFAULT NULL COMMENT '工单参数ID(关联qxx_pro_workorder_param,对比基准)',
  `template_id` bigint NOT NULL COMMENT '参数模版ID(关联qxx_pro_param_template,取值约束)',
  `actual_value` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '实际参数值(操作工填报)',
  `is_deviation` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '是否偏差(Y-超出min/max,N-正常,NULL-无约束未判定)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '偏差说明',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`record_id`),
  UNIQUE KEY `uk_feedback_param` (`feedback_id`,`template_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报工实际参数值表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_material_trace` (
  `trace_id` bigint NOT NULL AUTO_INCREMENT COMMENT '追溯记录ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `trace_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '追溯类型:ISSUE-投料消耗,PRODUCE-生产产出,SLIT-分切,MERGE-合并,ADJUST-调整',
  `parent_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '父类型:BATCH/ROLL_DETAIL/MATERIAL_STOCK/CARD',
  `parent_id` bigint NOT NULL COMMENT '父记录ID',
  `child_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '子类型:BATCH/ROLL_DETAIL/MATERIAL_STOCK/CARD',
  `child_id` bigint NOT NULL COMMENT '子记录ID',
  `quantity` decimal(14,4) NOT NULL COMMENT '转移数量',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID(关联qxx_pro_workorder)',
  `card_id` bigint DEFAULT NULL COMMENT '流转卡ID(关联qxx_pro_card)',
  `card_process_id` bigint DEFAULT NULL COMMENT '流转卡工序ID(关联qxx_pro_card_process)',
  `issue_id` bigint DEFAULT NULL COMMENT '领料单ID(关联qxx_wm_issue_header)',
  `issue_detail_id` bigint DEFAULT NULL COMMENT '领料明细ID(关联qxx_wm_issue_detail)',
  `feedback_id` bigint DEFAULT NULL COMMENT '报工记录ID(关联qxx_pro_feedback)',
  `transaction_id` bigint DEFAULT NULL COMMENT '库存事务ID(关联qxx_wm_transaction)',
  `process_id` bigint DEFAULT NULL COMMENT '工序ID(关联qxx_pro_process)',
  `trace_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '追溯时间(业务发生时间)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`trace_id`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_parent` (`factory_id`,`parent_type`,`parent_id`),
  KEY `idx_child` (`factory_id`,`child_type`,`child_id`),
  KEY `idx_workorder` (`workorder_id`),
  KEY `idx_card` (`card_id`)
) ENGINE=InnoDB AUTO_INCREMENT=248 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物料谱系追溯表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_param_template` (
  `template_id` bigint NOT NULL AUTO_INCREMENT COMMENT '模版ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `process_id` bigint NOT NULL COMMENT '工序ID(关联qxx_pro_process)',
  `param_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '参数编码(英文标识,同一工序下唯一)',
  `param_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '参数名称(中文显示名)',
  `param_group` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT 'PROCESS' COMMENT '参数分组:MACHINE-设备参数,PROCESS-工艺参数,MATERIAL-材料参数,QUALITY-质量参数,PRODUCT-产品规格参数',
  `param_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'VARCHAR' COMMENT '参数值类型:INT,DECIMAL,VARCHAR,ENUM,DATE,BOOLEAN',
  `unit` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '单位(如mm,μm,℃,m/min,mm²,kg)',
  `enum_values` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '枚举可选值(JSON数组,param_type=ENUM时必填,如["光膜","哑膜"])',
  `default_value` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '默认值(新建路线工序时自动填充)',
  `min_value` decimal(14,4) DEFAULT NULL COMMENT '最小值(超出时触发偏差预警)',
  `max_value` decimal(14,4) DEFAULT NULL COMMENT '最大值(超出时触发偏差预警)',
  `sort_order` int DEFAULT '1' COMMENT '排序号(同一工序下参数显示顺序)',
  `is_required` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '是否必填(Y-是,N-否)',
  `is_report_visible` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'Y' COMMENT '报工时是否显示(Y-是=操作工可见可填,N-否=仅工艺设计时可见)',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`template_id`),
  UNIQUE KEY `uk_process_param` (`factory_id`,`process_id`,`param_code`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=241 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工序参数模版定义表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_process` (
  `process_id` bigint NOT NULL AUTO_INCREMENT COMMENT '工序ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `process_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工序编码(唯一)',
  `process_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工序名称',
  `process_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'INTERNAL' COMMENT '工序类型:INTERNAL-自制工序,OUTSOURCE-外发工序,SLITTING-分切工序',
  `attention` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工艺要求/注意事项',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`process_id`),
  UNIQUE KEY `uk_process_code` (`process_code`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=218 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产工序表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_process_content` (
  `content_id` bigint NOT NULL AUTO_INCREMENT COMMENT '内容ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `process_id` bigint NOT NULL COMMENT '工序ID(关联qxx_pro_process)',
  `order_num` int DEFAULT '0' COMMENT '顺序编号(作业步骤序号)',
  `content_text` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '作业内容说明',
  `device` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅助设备/工具',
  `material` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅助材料/辅料',
  `doc_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '作业指导书URL(SOP文件)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_check` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '是否需要质检(Y-是,N-否)',
  PRIMARY KEY (`content_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=227 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工序作业内容表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_route` (
  `route_id` bigint NOT NULL AUTO_INCREMENT COMMENT '工艺路线ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `route_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工艺路线编码(唯一)',
  `route_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工艺路线名称',
  `route_desc` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工艺路线说明',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`route_id`),
  UNIQUE KEY `uk_route_code` (`route_code`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=232 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工艺路线表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_route_process` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `route_id` bigint NOT NULL COMMENT '工艺路线ID(关联qxx_pro_route)',
  `process_id` bigint NOT NULL COMMENT '工序ID(关联qxx_pro_process)',
  `process_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工序编码',
  `process_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工序名称',
  `process_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'INTERNAL' COMMENT '工序类型:INTERNAL-自制工序,OUTSOURCE-外发工序,SLITTING-分切工序',
  `order_num` int DEFAULT '1' COMMENT '工序序号(1,2,3...)',
  `next_process_id` bigint DEFAULT NULL COMMENT '下一道工序ID',
  `next_process_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '下一道工序编码',
  `next_process_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '下一道工序名称',
  `link_type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT 'SS' COMMENT '与下一道工序关系:SS-顺序,FS-并行',
  `default_pre_time` int DEFAULT '0' COMMENT '默认准备时长(分钟)',
  `default_suf_time` int DEFAULT '0' COMMENT '默认等待时长(分钟)',
  `color_code` char(7) COLLATE utf8mb4_unicode_ci DEFAULT '#00AEF3' COMMENT '甘特图显示颜色(16进制)',
  `key_flag` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '是否关键工序:Y-是,N-否',
  `is_check` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '是否检验工序:Y-是,N-否(检验工序需质检)',
  `is_outsource` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '是否外发工序(1-是,0-否)',
  `vendor_id` bigint DEFAULT NULL COMMENT '外协厂ID(关联qxx_md_vendor,外发工序指定)',
  `vendor_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外协厂编码',
  `vendor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外协厂名称',
  `outsource_factory_id` bigint DEFAULT NULL COMMENT '外协场景：该供应商对应的系统工厂ID(关联qxx_md_factory)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_outsource_factory_id` (`outsource_factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=314 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工艺路线-工序组成表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_route_process_param` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `route_product_id` bigint NOT NULL COMMENT '路线产品ID(关联qxx_pro_route_product.record_id)',
  `process_id` bigint NOT NULL COMMENT '工序ID(关联qxx_pro_process,冗余便于按工序查询)',
  `template_id` bigint NOT NULL COMMENT '参数模版ID(关联qxx_pro_param_template)',
  `param_value` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标准参数值(统一存字符串,前端按param_type校验)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`record_id`),
  UNIQUE KEY `uk_route_product_param` (`route_product_id`,`template_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=609 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='路线产品工序标准参数值表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_route_product` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `route_id` bigint NOT NULL COMMENT '工艺路线ID(关联qxx_pro_route)',
  `item_id` bigint NOT NULL COMMENT '产品物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主单位名称',
  `quantity` int DEFAULT '1' COMMENT '基准生产数量(工艺路线的标准批量)',
  `production_time` decimal(12,2) DEFAULT '1.00' COMMENT '标准生产用时',
  `time_unit_type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT 'MINUTE' COMMENT '时间单位:SECOND-秒,MINUTE-分钟,HOUR-小时,DAY-天',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=401 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品与工艺路线关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_route_product_bom` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `route_id` bigint NOT NULL COMMENT '工艺路线ID(关联qxx_pro_route)',
  `process_id` bigint NOT NULL COMMENT '工序ID(关联qxx_pro_process,指定该物料在哪个工序投入)',
  `product_id` bigint NOT NULL COMMENT '产品物料ID(关联qxx_md_item,成品/半成品)',
  `item_id` bigint NOT NULL COMMENT 'BOM物料ID(关联qxx_md_item,原料/辅料)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'BOM物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'BOM物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主单位名称',
  `quantity` decimal(12,2) DEFAULT '1.00' COMMENT '用料比例(每个成品/半成品消耗的物料数量)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=294 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工艺路线产品BOM表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '快照ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID',
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '快照名称',
  `scope_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '排产范围类型',
  `scope_id` bigint DEFAULT NULL COMMENT '排产范围ID',
  `status` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT' COMMENT '状态',
  `snapshot_data` json DEFAULT NULL COMMENT '快照JSON',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=209 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排产快照表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_task` (
  `task_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `task_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务编码',
  `task_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务名称',
  `workorder_id` bigint NOT NULL COMMENT '生产工单ID(关联qxx_pro_workorder)',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '生产工单编码',
  `workorder_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工单名称',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID(关联qxx_md_workstation)',
  `workstation_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工作站编码',
  `workstation_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工作站名称',
  `route_id` bigint NOT NULL COMMENT '工艺路线ID(关联qxx_pro_route)',
  `route_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工艺路线编码',
  `process_id` bigint NOT NULL COMMENT '工序ID(关联qxx_pro_process)',
  `process_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工序编码',
  `process_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工序名称',
  `item_id` bigint NOT NULL COMMENT '产品物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主单位名称',
  `quantity` decimal(14,2) NOT NULL DEFAULT '1.00' COMMENT '排产数量',
  `quantity_produced` decimal(14,2) DEFAULT '0.00' COMMENT '已生产数量',
  `quantity_qualified` decimal(14,2) DEFAULT '0.00' COMMENT '合格品累计数量',
  `quantity_unqualified` decimal(14,2) DEFAULT '0.00' COMMENT '不合格品累计数量',
  `quantity_changed` decimal(14,2) DEFAULT '0.00' COMMENT '调整数量',
  `client_id` bigint DEFAULT NULL COMMENT '客户ID(关联qxx_md_client)',
  `client_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户编码',
  `client_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户名称',
  `client_nick` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户简称',
  `machine_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '机台号/设备编号(排产到具体机器)',
  `setup_duration` int DEFAULT '0' COMMENT '调机时长(分钟)',
  `unit_duration` decimal(10,2) DEFAULT '0.00' COMMENT '单位耗时(分钟,纸袋=制袋耗时/礼品盒=成型耗时)',
  `offline_qty` int DEFAULT '0' COMMENT '下机个数(每批次离线数量)',
  `start_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '计划开始时间',
  `duration` int DEFAULT '1' COMMENT '计划生产时长(分钟)',
  `end_time` datetime DEFAULT NULL COMMENT '计划完成时间',
  `color_code` char(7) COLLATE utf8mb4_unicode_ci DEFAULT '#00AEF3' COMMENT '甘特图显示颜色(16进制)',
  `request_date` datetime DEFAULT NULL COMMENT '需求日期(客户交期)',
  `finish_date` datetime DEFAULT NULL COMMENT '实际完成日期',
  `cancel_date` datetime DEFAULT NULL COMMENT '取消日期',
  `vendor_id` bigint DEFAULT NULL COMMENT '外协供应商ID(关联qxx_md_vendor,外发工序时填写)',
  `vendor_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外协供应商编码',
  `outsource_factory_id` bigint DEFAULT NULL COMMENT '外协场景：该供应商对应的系统工厂ID(关联qxx_md_factory)',
  `status` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT 'NORMAL' COMMENT '生产状态:PREPARE-待排产,NORMAL-正常,PRODUCING-生产中,COMPLETED-已完成,PAUSED-暂停,CANCEL-取消',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `predecessor_id` bigint DEFAULT NULL COMMENT '前置任务ID',
  `snapshot_id` bigint DEFAULT NULL COMMENT '排产快照ID',
  `gantt_sort` int DEFAULT '0' COMMENT '甘特图行排序号',
  PRIMARY KEY (`task_id`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_outsource_factory_id` (`outsource_factory_id`),
  KEY `idx_predecessor_id` (`predecessor_id`),
  KEY `idx_snapshot_id` (`snapshot_id`)
) ENGINE=InnoDB AUTO_INCREMENT=304 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产任务/排产表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_user_workstation` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名',
  `nick_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户昵称',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID(关联qxx_md_workstation)',
  `workstation_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作站编码',
  `workstation_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作站名称',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `operation_time` datetime DEFAULT NULL COMMENT '绑定操作时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户工作站绑定表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_workorder` (
  `workorder_id` bigint NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工单编码(唯一)',
  `workorder_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工单名称',
  `workorder_type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT 'SELF' COMMENT '工单类型:SELF-自制,OUTSOURCE-外协',
  `order_source` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '来源类型:SALES_ORDER-销售订单,MANUAL-手工创建,PLAN-计划生成',
  `source_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '来源单据编码(销售订单号等)',
  `product_id` bigint NOT NULL COMMENT '产品ID(关联qxx_md_item)',
  `product_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品编码',
  `product_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品名称',
  `product_spc` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '产品规格型号',
  `route_product_id` bigint DEFAULT NULL COMMENT '路线产品ID',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主单位名称',
  `quantity` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '计划生产数量',
  `quantity_produced` decimal(14,2) DEFAULT '0.00' COMMENT '已生产数量',
  `quantity_changed` decimal(14,2) DEFAULT '0.00' COMMENT '调整数量(增补/扣减)',
  `quantity_scheduled` decimal(14,2) DEFAULT '0.00' COMMENT '已排产数量',
  `client_id` bigint DEFAULT NULL COMMENT '客户ID(关联qxx_md_client)',
  `client_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户编码',
  `client_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户名称',
  `client_order_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户订单号(PO号)',
  `product_size` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '产品尺寸(长*宽*高mm),如254*127*330mm',
  `printing_req` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '印刷要求描述,如1色满版黑印刷/彩印',
  `rope_spec` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '绳料规格要求(纸袋专用,礼品盒留NULL)',
  `package_req` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '包装要求描述,如250个/箱,贴唛头',
  `shipping_req` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发货要求描述',
  `order_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'NEW' COMMENT '订单类型:NEW-新单,REPEAT-返单',
  `vendor_id` bigint DEFAULT NULL COMMENT '外协供应商ID(关联qxx_md_vendor)',
  `vendor_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外协供应商编码',
  `vendor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外协供应商名称',
  `outsource_factory_id` bigint DEFAULT NULL COMMENT '外协场景：该供应商对应的系统工厂ID(关联qxx_md_factory)',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次号',
  `request_date` datetime DEFAULT NULL COMMENT '需求日期(客户要求的交期)',
  `cancel_date` datetime DEFAULT NULL COMMENT '取消日期',
  `finish_date` datetime DEFAULT NULL COMMENT '实际完成日期',
  `status` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT 'PREPARE' COMMENT '工单状态:PREPARE-待生产,PRODUCING-生产中,COMPLETED-已完成,CANCEL-已取消,CLOSED-已关闭',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`workorder_id`),
  UNIQUE KEY `uk_workorder_code` (`workorder_code`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_outsource_factory_id` (`outsource_factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=305 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产工单表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_workorder_bom` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `workorder_id` bigint NOT NULL COMMENT '生产工单ID(关联qxx_pro_workorder)',
  `item_id` bigint NOT NULL COMMENT 'BOM物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'BOM物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'BOM物料名称',
  `item_spc` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主单位名称',
  `unit2` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅助单位编码(如ROLL-卷/TON-吨,纸袋/礼品盒通用)',
  `unit2_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅助单位名称',
  `conversion_rate` decimal(10,4) DEFAULT '1.0000' COMMENT '主单位→辅助单位换算率',
  `process_id` bigint DEFAULT NULL COMMENT '工序ID',
  `process_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工序名称',
  `item_or_product` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料产品标识:RAW-原料,SEMI-半成品,FINISHED-成品,AUXILIARY-辅料,PACK-包材',
  `quantity` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '单位用量(每个成品消耗的数量)',
  `total_quantity` decimal(14,2) DEFAULT '0.00' COMMENT '预计总用量(单位用量*计划数量)',
  `total_quantity2` decimal(14,2) DEFAULT '0.00' COMMENT '预计总用量(辅助单位)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=470 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单BOM组成表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_workorder_change` (
  `change_id` bigint NOT NULL AUTO_INCREMENT COMMENT '变更ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID',
  `workorder_id` bigint NOT NULL COMMENT '工单ID',
  `change_type` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '变更类型:BOM/QTY/PARAM/STATUS',
  `field_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '变更字段',
  `old_value` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '原值',
  `new_value` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '新值',
  `change_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '变更原因',
  `approver` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批人',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `status` varchar(32) COLLATE utf8mb4_general_ci DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`change_id`),
  KEY `idx_workorder_id` (`workorder_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单变更记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_workorder_param` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `workorder_id` bigint NOT NULL COMMENT '工单ID(关联qxx_pro_workorder)',
  `route_product_id` bigint NOT NULL COMMENT '路线产品ID(关联qxx_pro_route_product,追溯来源)',
  `template_id` bigint NOT NULL COMMENT '参数模版ID(关联qxx_pro_param_template)',
  `standard_value` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '路线标准值(快照,从L2复制锁定)',
  `adjusted_value` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工单调整值(计划员修改后的值,为null则沿用standard_value)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '调整原因说明',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`record_id`),
  UNIQUE KEY `uk_workorder_param` (`workorder_id`,`route_product_id`,`template_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1070 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单工序参数值表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pro_workrecord` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名',
  `nick_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户昵称',
  `workstation_id` bigint NOT NULL COMMENT '工作站ID(关联qxx_md_workstation)',
  `workstation_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作站编码',
  `workstation_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作站名称',
  `task_id` bigint DEFAULT NULL COMMENT '生产任务ID(关联qxx_pro_task)',
  `task_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产任务编码',
  `operation_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作类型:ON-上工,OFF-下工',
  `operation_time` datetime DEFAULT NULL COMMENT '操作时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='上下工记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pur_order` (
  `order_id` bigint NOT NULL AUTO_INCREMENT COMMENT '采购订单ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `order_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '采购订单编码(唯一)',
  `order_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '采购订单名称',
  `vendor_id` bigint NOT NULL COMMENT '供应商ID(关联qxx_md_vendor,vendor_type=MATERIAL)',
  `vendor_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商编码',
  `vendor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商名称',
  `purchase_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'PAPER' COMMENT '采购类型:PAPER-纸张,AUX-辅料(绳子/胶水/油墨),PACK-包材(纸箱),OTHER-其他',
  `order_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '下单日期',
  `expected_date` datetime DEFAULT NULL COMMENT '预计到货日期',
  `purchaser` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '采购员(申购人)',
  `approver` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批人',
  `total_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '采购总数量(主单位)',
  `total_amount` decimal(14,2) DEFAULT '0.00' COMMENT '采购总金额(元)',
  `currency` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT 'CNY' COMMENT '币种:CNY-人民币,USD-美元',
  `source_order_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联客户订单号(如PO#ORD66003MT)',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT' COMMENT '状态:DRAFT-草稿,APPROVED-已审批,ORDERED-已下单,RECEIVING-收货中,RECEIVED-已收货,CLOSED-已关闭,CANCEL-已取消',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `workorder_id` bigint DEFAULT NULL COMMENT 'å…³è”å·¥å•ID',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å…³è”å·¥å•ç¼–ç ',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_order_code` (`order_code`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=222 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单头表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_pur_order_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `order_id` bigint NOT NULL COMMENT '采购订单ID(关联qxx_pur_order)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主单位编码(如TON-吨,ROLL-卷,PCS-个,KG-千克)',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主单位名称',
  `unit2` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅助单位编码(如ROLL-卷,与主单位联动)',
  `unit2_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅助单位名称',
  `conversion_rate` decimal(10,4) DEFAULT '1.0000' COMMENT '主单位→辅助单位换算率',
  `quantity_ordered` decimal(14,4) DEFAULT '0.0000' COMMENT '订购数量(主单位)',
  `quantity_ordered2` decimal(14,4) DEFAULT '0.0000' COMMENT '订购数量(辅助单位,如卷数)',
  `quantity_received` decimal(14,4) DEFAULT '0.0000' COMMENT '已收货数量(主单位)',
  `quantity_received2` decimal(14,4) DEFAULT '0.0000' COMMENT '已收货数量(辅助单位)',
  `unit_price` decimal(14,4) DEFAULT '0.0000' COMMENT '单价(元/主单位,如元/吨)',
  `amount` decimal(14,2) DEFAULT '0.00' COMMENT '行金额(不含税)',
  `tax_rate` decimal(5,2) DEFAULT '0.00' COMMENT '税率(%)',
  `paper_width` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '门幅要求(mm),如925mm',
  `paper_weight` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '克重要求(g),如120g',
  `paper_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '纸张种类:乌卡/俄卡/箱板纸/白牛皮/TC箱板纸/瑞典赤牛',
  `roll_count` int DEFAULT '0' COMMENT '预计卷数(纸张行业用，其他行业=0)',
  `source_order_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联客户订单号',
  `expected_date` datetime DEFAULT NULL COMMENT '预计到货日期',
  `arrival_notice_id` bigint DEFAULT NULL COMMENT '到货通知单ID(关联qxx_wm_arrival_notice,收货后回写)',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'ORDERED' COMMENT '行状态:ORDERED-已下单,RECEIVING-收货中,RECEIVED-已收货,CLOSED-已关闭',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  UNIQUE KEY `uk_order_item` (`order_id`,`item_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=228 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单行表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_tm_tool` (
  `tool_id` bigint NOT NULL AUTO_INCREMENT COMMENT '工装夹具ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `tool_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工装夹具编码(唯一)',
  `tool_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工装夹具名称',
  `brand` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '品牌',
  `spec` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `tool_type_id` bigint NOT NULL COMMENT '工装夹具类型ID(关联qxx_tm_tool_type)',
  `tool_type_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '工装夹具类型编码',
  `tool_type_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '工装夹具类型名称',
  `quantity` int DEFAULT '1' COMMENT '总数量',
  `available_quantity` int DEFAULT '1' COMMENT '可用数量(总数量减去使用中和保养中的数量)',
  `mainten_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '保养类型:DAY-每天,WEEK-每周,MONTH-每月,QUARTER-每季,HALFYEAR-每半年,YEAR-每年,USAGE-按使用次数',
  `next_mainten_date` date DEFAULT NULL COMMENT '下次保养日期',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'STORE' COMMENT '状态:STORE-在库,USING-使用中,MAINTENANCE-保养中,SCRAPPED-报废',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`tool_id`),
  UNIQUE KEY `uk_tool_code` (`tool_code`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=223 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工装夹具清单表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_tm_tool_type` (
  `tool_type_id` bigint NOT NULL AUTO_INCREMENT COMMENT '工装夹具类型ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `tool_type_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型编码(唯一)',
  `tool_type_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型名称',
  `need_code_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否需要编码(1-需要,0-不需要)',
  `mainten_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '保养类型:DAY-每天,WEEK-每周,MONTH-每月,QUARTER-每季,HALFYEAR-每半年,YEAR-每年,USAGE-按使用次数',
  `mainten_cycle` int DEFAULT '0' COMMENT '保养周期(与保养类型配合,如:月+3=每3个月保养一次)',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`tool_type_id`),
  UNIQUE KEY `uk_tool_type_code` (`tool_type_code`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=216 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工装夹具类型表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_barcode` (
  `barcode_id` bigint NOT NULL AUTO_INCREMENT COMMENT '条码ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `barcode_code` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '条码内容',
  `barcode_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'PRODUCT' COMMENT '条码类型:PRODUCT-产品,PACKAGE-装箱,ITEM-物料,PALLET-托盘',
  `item_id` bigint DEFAULT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '物料名称',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID(关联qxx_pro_workorder)',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产工单编码',
  `quantity` decimal(14,4) DEFAULT '1.0000' COMMENT '数量',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '单位',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次号',
  `print_count` int DEFAULT '0' COMMENT '打印次数',
  `last_print_time` datetime DEFAULT NULL COMMENT '最后打印时间',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`barcode_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='条码清单表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_batch` (
  `batch_id` bigint NOT NULL AUTO_INCREMENT COMMENT '批次ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '批次编码(唯一)',
  `batch_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次名称',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `produce_date` datetime DEFAULT NULL COMMENT '生产日期',
  `expire_date` datetime DEFAULT NULL COMMENT '有效期至',
  `recpt_date` datetime DEFAULT NULL COMMENT '入库日期',
  `vendor_id` bigint DEFAULT NULL COMMENT '供应商ID',
  `vendor_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商编码',
  `vendor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商名称',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产工单编码',
  `quality_status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'NORMAL' COMMENT '质量状态:NORMAL-正常,HOLD-冻结,REJECT-不合格',
  `lot_number` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ç”Ÿäº§æ‰¹å·',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE' COMMENT '批次状态:ACTIVE-活跃,CLOSED-关闭,EXPIRED-过期',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`batch_id`),
  UNIQUE KEY `uk_batch_code` (`batch_code`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=212 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='批次记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_issue_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `issue_id` bigint NOT NULL COMMENT '领料单ID(关联qxx_wm_issue_header)',
  `line_id` bigint NOT NULL COMMENT '行ID(关联qxx_wm_issue_line)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '领料数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `material_stock_id` bigint NOT NULL COMMENT '库存记录ID(关联qxx_wm_material_stock)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`detail_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=222 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产领料单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_issue_header` (
  `issue_id` bigint NOT NULL AUTO_INCREMENT COMMENT '领料单ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `issue_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '领料单编码',
  `issue_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '领料单名称',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID(关联qxx_pro_workorder)',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产工单编码',
  `workorder_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `task_id` bigint DEFAULT NULL COMMENT '生产任务ID(关联qxx_pro_task)',
  `task_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产任务编码',
  `workstation_id` bigint DEFAULT NULL COMMENT '工作站ID(关联qxx_md_workstation)',
  `workstation_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作站编码',
  `workstation_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作站名称',
  `warehouse_id` bigint NOT NULL COMMENT '发料仓库ID(关联qxx_wm_warehouse)',
  `warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL,
  `location_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `location_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `area_id` bigint DEFAULT NULL,
  `area_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `area_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `issue_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '领料日期',
  `total_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '领料总数量',
  `quantity_issued_total` decimal(14,4) DEFAULT '0.0000' COMMENT '已发料累计数量',
  `quantity_received` decimal(14,4) DEFAULT '0.0000' COMMENT '已收料累计数量',
  `version` int DEFAULT '0' COMMENT '乐观锁版本号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `issue_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'PRODUCE' COMMENT '领料类型:PRODUCE-生产领料,MISC-杂项领料',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT' COMMENT '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账',
  `approve_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核人',
  `approve_time` datetime DEFAULT NULL COMMENT '审核时间',
  `cancel_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '作废原因',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`issue_id`),
  UNIQUE KEY `uk_issue_code` (`factory_id`,`issue_code`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=246 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产领料单头表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_issue_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `issue_id` bigint NOT NULL COMMENT '领料单ID(关联qxx_wm_issue_header)',
  `issue_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '领料单编码(冗余)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity_issue` decimal(14,4) DEFAULT '0.0000' COMMENT '申请领料数量',
  `quantity_issued` decimal(14,4) DEFAULT '0.0000' COMMENT '实际已发料数量(累计)',
  `unit2` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅单位编码',
  `unit2_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅单位名称',
  `conversion_rate` decimal(18,6) DEFAULT NULL COMMENT '主辅单位转换率',
  `quantity_issue2` decimal(14,4) DEFAULT '0.0000' COMMENT '辅单位申请数量',
  `quantity_issued2` decimal(14,4) DEFAULT '0.0000' COMMENT '辅单位已发料数量',
  `process_id` bigint DEFAULT NULL COMMENT '工序ID',
  `process_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工序编码',
  `process_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工序名称',
  `quantity_recpt` decimal(14,4) DEFAULT '0.0000' COMMENT '实际发料数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=242 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产领料单行表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_item_recpt` (
  `recpt_id` bigint NOT NULL AUTO_INCREMENT COMMENT '入库单ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `recpt_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '入库单编码(自动生成)',
  `recpt_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '入库单名称',
  `pur_order_id` bigint DEFAULT NULL COMMENT '采购订单ID(关联qxx_pur_order,采购入库时填写)',
  `pur_order_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '采购订单编码',
  `vendor_id` bigint DEFAULT NULL COMMENT '供应商ID(关联qxx_md_vendor)',
  `vendor_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商编码',
  `vendor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商名称',
  `warehouse_id` bigint NOT NULL COMMENT '入库仓库ID(关联qxx_wm_warehouse)',
  `warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库名称',
  `recpt_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '入库日期',
  `recpt_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'PURCHASE' COMMENT '入库类型:PURCHASE-采购入库,PRODUCE-生产入库,OUTSOURCE-外协入库,MISC-杂项入库,RETURN-退货入库',
  `total_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '入库总数量(主单位)',
  `iqc_id` bigint DEFAULT NULL COMMENT '质检单ID(关联qxx_qc_iqc)',
  `iqc_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '质检单编码',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT' COMMENT '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账,CANCEL-取消',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `workorder_id` bigint DEFAULT NULL COMMENT 'å…³è”å·¥å•ID(ç”Ÿäº§å…¥åº“)',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å…³è”å·¥å•ç¼–ç ',
  PRIMARY KEY (`recpt_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=237 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物料入库单表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_item_recpt_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `recpt_id` bigint NOT NULL COMMENT '入库单ID(关联qxx_wm_item_recpt)',
  `line_id` bigint NOT NULL COMMENT '行ID(关联qxx_wm_item_recpt_line)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主单位名称',
  `unit2` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅助单位编码',
  `unit2_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅助单位名称',
  `quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '入库数量(主单位)',
  `quantity2` decimal(14,4) DEFAULT '0.0000' COMMENT '入库数量(辅助单位)',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID(关联qxx_wm_batch)',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `material_stock_id` bigint NOT NULL COMMENT '库存记录ID(关联qxx_wm_material_stock)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`detail_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物料入库单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_item_recpt_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `recpt_id` bigint NOT NULL COMMENT '入库单ID(关联qxx_wm_item_recpt)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主单位名称',
  `unit2` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅助单位编码',
  `unit2_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅助单位名称',
  `conversion_rate` decimal(10,4) DEFAULT '1.0000' COMMENT '换算率',
  `quantity_recpt` decimal(14,4) DEFAULT '0.0000' COMMENT '入库数量(主单位)',
  `quantity_recpt2` decimal(14,4) DEFAULT '0.0000' COMMENT '入库数量(辅助单位,如重量吨)',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID(关联qxx_wm_batch)',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `expire_date` datetime DEFAULT NULL COMMENT '有效期至',
  `lot_number` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ç”Ÿäº§æ‰¹å·',
  `produce_date` datetime DEFAULT NULL COMMENT 'ç”Ÿäº§æ—¥æœŸ',
  `notice_line_id` bigint DEFAULT NULL COMMENT '到货通知行ID(关联qxx_wm_arrival_notice_line)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=243 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物料入库单行表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_material_stock` (
  `material_stock_id` bigint NOT NULL AUTO_INCREMENT COMMENT '库存记录ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主单位名称',
  `unit2` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅助单位编码(纸张行业:ROLL-卷/TON-吨)',
  `unit2_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅助单位名称',
  `conversion_rate` decimal(10,4) DEFAULT '1.0000' COMMENT '主单位→辅助单位换算率',
  `quantity_onhand` decimal(14,4) DEFAULT '0.0000' COMMENT '现有库存量(主单位)',
  `quantity_onhand2` decimal(14,4) DEFAULT '0.0000' COMMENT '现有库存量(辅助单位)',
  `quantity_available` decimal(14,4) DEFAULT '0.0000' COMMENT '可用库存量(主单位,扣减已分配)',
  `batch_id` bigint NOT NULL DEFAULT '0' COMMENT '批次ID(关联qxx_wm_batch,0=无批次管理)',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID(关联qxx_wm_warehouse)',
  `warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID(关联qxx_wm_storage_location)',
  `location_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '库区名称',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID(关联qxx_wm_storage_area)',
  `area_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '库位编码',
  `area_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '库位名称',
  `vendor_id` bigint NOT NULL DEFAULT '0' COMMENT '供应商ID(关联qxx_md_vendor,0=不适用)',
  `vendor_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商编码',
  `vendor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商名称',
  `workorder_id` bigint NOT NULL DEFAULT '0' COMMENT '生产工单ID(关联qxx_pro_workorder,0=不适用)',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产工单编码',
  `expire_date` datetime DEFAULT NULL COMMENT '有效期至',
  `lot_number` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产批号',
  `quality_status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'NORMAL' COMMENT '质量状态:NORMAL-正常,HOLD-冻结,REJECT-不合格,SCRAP-报废',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'NORMAL' COMMENT '库存状态:NORMAL-正常,ALERT-预警,DEPLETED-耗尽(辅料使用)',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`material_stock_id`),
  UNIQUE KEY `uk_stock` (`factory_id`,`item_id`,`batch_id`,`warehouse_id`,`vendor_id`,`workorder_id`,`quality_status`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=215 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_materialrequest_notice` (
  `notice_id` bigint NOT NULL AUTO_INCREMENT COMMENT '备料通知单ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `notice_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '备料通知单编码',
  `notice_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备料通知单名称',
  `workorder_id` bigint NOT NULL COMMENT '生产工单ID(关联qxx_pro_workorder)',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产工单编码',
  `workstation_id` bigint DEFAULT NULL COMMENT '工作站ID(关联qxx_md_workstation)',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID(关联qxx_wm_warehouse)',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库名称',
  `request_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '需求日期',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING' COMMENT '状态:PENDING-待备料,PREPARED-已备料,ISSUED-已发料,CLOSED-关闭',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`notice_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='备料通知单表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_materialrequest_notice_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `notice_id` bigint NOT NULL COMMENT '备料通知单ID(关联qxx_wm_materialrequest_notice)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity_request` decimal(14,4) DEFAULT '0.0000' COMMENT '申请备料数量',
  `quantity_prepared` decimal(14,4) DEFAULT '0.0000' COMMENT '已备料数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='备料通知单行表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_misc_issue` (
  `issue_id` bigint NOT NULL AUTO_INCREMENT COMMENT '杂项出库单ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `issue_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '杂项出库单编码',
  `issue_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '杂项出库单名称',
  `issue_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '出库原因:ADJUST-盘亏调整,SCRAP-报废,OTHER-其他',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID(关联qxx_wm_warehouse)',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库名称',
  `issue_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '出库日期',
  `total_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '出库总数量',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT' COMMENT '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`issue_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='杂项出库单表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_misc_issue_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `issue_id` bigint NOT NULL COMMENT '杂项出库单ID(关联qxx_wm_misc_issue)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '出库数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='杂项出库单行表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_misc_recpt` (
  `recpt_id` bigint NOT NULL AUTO_INCREMENT COMMENT '杂项入库单ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `recpt_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '杂项入库单编码',
  `recpt_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '杂项入库单名称',
  `recpt_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '入库原因:OPENING-期初导入,ADJUST-盘盈调整,OTHER-其他',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID(关联qxx_wm_warehouse)',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库名称',
  `recpt_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '入库日期',
  `total_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '入库总数量',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT' COMMENT '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`recpt_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='杂项入库单表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_misc_recpt_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `recpt_id` bigint NOT NULL COMMENT '杂项入库单ID(关联qxx_wm_misc_recpt)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '入库数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='杂项入库单行表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_outsource_issue` (
  `issue_id` bigint NOT NULL AUTO_INCREMENT COMMENT '外协领料单ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `issue_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '外协领料单编码',
  `issue_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外协领料单名称',
  `vendor_id` bigint NOT NULL COMMENT '外协厂ID(关联qxx_md_vendor,vendor_type=OUTSOURCE)',
  `vendor_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外协厂编码',
  `vendor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外协厂名称',
  `outsource_factory_id` bigint DEFAULT NULL COMMENT '外协场景：该供应商对应的系统工厂ID(关联qxx_md_factory)',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID(关联qxx_pro_workorder)',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产工单编码',
  `outsource_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'PRODUCE' COMMENT '外发类型:PRODUCE-外发生产(印刷+制袋),ROPE-外发贴绳,OTHER-其他',
  `warehouse_id` bigint NOT NULL COMMENT '发料仓库ID(关联qxx_wm_warehouse)',
  `warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库名称',
  `issue_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发料日期',
  `expected_return_date` datetime DEFAULT NULL COMMENT '预计返回日期',
  `total_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '发料总数量',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT' COMMENT '状态:DRAFT-草稿,ISSUED-已发出,IN_PROGRESS-外协生产中,RETURNED-已返回,CLOSED-关闭',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注(如:4月21日到纸,4月22日发到欧诺)',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`issue_id`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_outsource_factory_id` (`outsource_factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外协领料单头表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_outsource_issue_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `issue_id` bigint NOT NULL COMMENT '外协领料单ID(关联qxx_wm_outsource_issue)',
  `line_id` bigint NOT NULL COMMENT '行ID(关联qxx_wm_outsource_issue_line)',
  `item_id` bigint NOT NULL COMMENT '物料ID',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '发料数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `material_stock_id` bigint NOT NULL COMMENT '库存记录ID(关联qxx_wm_material_stock)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`detail_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外协领料单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_outsource_issue_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `issue_id` bigint NOT NULL COMMENT '外协领料单ID(关联qxx_wm_outsource_issue)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `item_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '物料类型:RAW-纸张原材料,SEMI-半成品袋子,AUX-辅料(绳子),PACK-包材',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `unit2` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅助单位编码(纸张双单位)',
  `unit2_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅助单位名称',
  `quantity_issue` decimal(14,4) DEFAULT '0.0000' COMMENT '发料数量(主单位)',
  `quantity_issue2` decimal(14,4) DEFAULT '0.0000' COMMENT '发料数量(辅助单位,如重量吨)',
  `bundle_count` int DEFAULT '0' COMMENT '卷数/捆数',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外协领料单行表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_outsource_recpt` (
  `recpt_id` bigint NOT NULL AUTO_INCREMENT COMMENT '外协入库单ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `recpt_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '外协入库单编码',
  `recpt_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外协入库单名称',
  `issue_id` bigint DEFAULT NULL COMMENT '关联外协领料单ID(关联qxx_wm_outsource_issue)',
  `issue_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联外协领料单编码',
  `vendor_id` bigint NOT NULL COMMENT '外协厂ID(关联qxx_md_vendor)',
  `vendor_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外协厂编码',
  `vendor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外协厂名称',
  `outsource_factory_id` bigint DEFAULT NULL COMMENT '外协场景：该供应商对应的系统工厂ID(关联qxx_md_factory)',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID(关联qxx_pro_workorder)',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产工单编码',
  `warehouse_id` bigint NOT NULL COMMENT '入库仓库ID(关联qxx_wm_warehouse)',
  `warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库名称',
  `recpt_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '入库日期',
  `total_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '入库总数量',
  `total_box` int DEFAULT '0' COMMENT '入库总箱数',
  `ipqc_id` bigint DEFAULT NULL COMMENT '过程质检单ID(关联qxx_qc_ipqc)',
  `ipqc_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '过程质检单编码',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT' COMMENT '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`recpt_id`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_outsource_factory_id` (`outsource_factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外协入库单表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_outsource_recpt_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `recpt_id` bigint NOT NULL COMMENT '外协入库单ID(关联qxx_wm_outsource_recpt)',
  `line_id` bigint NOT NULL COMMENT '行ID(关联qxx_wm_outsource_recpt_line)',
  `item_id` bigint NOT NULL COMMENT '物料ID',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '入库数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `material_stock_id` bigint NOT NULL COMMENT '库存记录ID(关联qxx_wm_material_stock)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`detail_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外协入库单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_outsource_recpt_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `recpt_id` bigint NOT NULL COMMENT '外协入库单ID(关联qxx_wm_outsource_recpt)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity_recpt` decimal(14,4) DEFAULT '0.0000' COMMENT '入库数量',
  `quantity_box` int DEFAULT '0' COMMENT '入库箱数',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外协入库单行表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_package` (
  `package_id` bigint NOT NULL AUTO_INCREMENT COMMENT '装箱单ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `package_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '装箱单编码',
  `package_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '装箱单名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父装箱ID(层级装箱,0=顶层)',
  `ancestors` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所有父节点ID',
  `package_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'CARTON' COMMENT '装箱类型:CARTON-纸箱,PALLET-托盘,CONTAINER-集装箱',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID(关联qxx_pro_workorder)',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产工单编码',
  `quantity_per_box` int DEFAULT '0' COMMENT '每箱标准数量(如250个/箱)',
  `box_count` int DEFAULT '0' COMMENT '箱数',
  `total_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '总数量',
  `weight` decimal(14,4) DEFAULT '0.0000' COMMENT '重量(吨)',
  `length` decimal(10,2) DEFAULT '0.00' COMMENT '长(cm)',
  `width` decimal(10,2) DEFAULT '0.00' COMMENT '宽(cm)',
  `height` decimal(10,2) DEFAULT '0.00' COMMENT '高(cm)',
  `volume` decimal(14,4) DEFAULT '0.0000' COMMENT '体积(立方米)',
  `box_label` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '箱唛/唛头',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'OPEN' COMMENT '状态:OPEN-开放,CLOSED-关闭,SHIPPED-已发货',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`package_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='装箱单表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_package_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `package_id` bigint NOT NULL COMMENT '装箱单ID(关联qxx_wm_package)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '装箱数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `barcode_code` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '条码内容',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='装箱明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_product_produce` (
  `produce_id` bigint NOT NULL AUTO_INCREMENT COMMENT '产出记录ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `produce_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产出记录编码',
  `produce_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '产出记录名称',
  `workorder_id` bigint NOT NULL COMMENT '生产工单ID(关联qxx_pro_workorder)',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产工单编码',
  `task_id` bigint DEFAULT NULL COMMENT '生产任务ID(关联qxx_pro_task)',
  `task_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产任务编码',
  `workstation_id` bigint DEFAULT NULL COMMENT '工作站ID(关联qxx_md_workstation)',
  `workstation_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作站编码',
  `workstation_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作站名称',
  `process_id` bigint DEFAULT NULL COMMENT '工序ID(关联qxx_pro_process)',
  `process_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工序编码',
  `process_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工序名称',
  `feedback_id` bigint DEFAULT NULL COMMENT '报工记录ID(关联qxx_pro_feedback)',
  `warehouse_id` bigint NOT NULL COMMENT '线边库ID(关联qxx_wm_warehouse)',
  `warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '线边库编码',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '线边库名称',
  `produce_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '产出日期',
  `total_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '产出总数量',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT' COMMENT '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`produce_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品产出记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_product_produce_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `produce_id` bigint NOT NULL COMMENT '产出记录ID(关联qxx_wm_product_produce)',
  `line_id` bigint NOT NULL COMMENT '行ID(关联qxx_wm_product_produce_line)',
  `item_id` bigint NOT NULL COMMENT '产品物料ID',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '产出数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '线边库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `material_stock_id` bigint NOT NULL COMMENT '库存记录ID(关联qxx_wm_material_stock)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`detail_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品产出记录明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_product_produce_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `produce_id` bigint NOT NULL COMMENT '产出记录ID(关联qxx_wm_product_produce)',
  `item_id` bigint NOT NULL COMMENT '产品物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity_produce` decimal(14,4) DEFAULT '0.0000' COMMENT '产出数量',
  `quantity_qualified` decimal(14,4) DEFAULT '0.0000' COMMENT '合格数量',
  `quantity_unqualified` decimal(14,4) DEFAULT '0.0000' COMMENT '不合格数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '线边库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品产出记录行表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_product_recpt` (
  `recpt_id` bigint NOT NULL AUTO_INCREMENT COMMENT '入库单ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `recpt_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '入库单编码',
  `recpt_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '入库单名称',
  `produce_id` bigint DEFAULT NULL COMMENT '产出记录ID(关联qxx_wm_product_produce)',
  `produce_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '产出记录编码',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID(关联qxx_pro_workorder)',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产工单编码',
  `source_warehouse_id` bigint DEFAULT NULL COMMENT '来源仓库(线边库)ID',
  `warehouse_id` bigint NOT NULL COMMENT '目标仓库(成品库)ID(关联qxx_wm_warehouse)',
  `warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标仓库编码',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标仓库名称',
  `recpt_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '入库日期',
  `total_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '入库总数量(个数)',
  `total_box` int DEFAULT '0' COMMENT '入库总箱数',
  `ipqc_id` bigint DEFAULT NULL COMMENT '过程质检单ID(关联qxx_qc_ipqc)',
  `ipqc_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '过程质检单编码',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT' COMMENT '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`recpt_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品入库单表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_product_recpt_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `recpt_id` bigint NOT NULL COMMENT '入库单ID(关联qxx_wm_product_recpt)',
  `line_id` bigint NOT NULL COMMENT '行ID(关联qxx_wm_product_recpt_line)',
  `item_id` bigint NOT NULL COMMENT '物料ID',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '入库数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `material_stock_id` bigint NOT NULL COMMENT '库存记录ID(关联qxx_wm_material_stock)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`detail_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品入库单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_product_recpt_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `recpt_id` bigint NOT NULL COMMENT '入库单ID(关联qxx_wm_product_recpt)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity_recpt` decimal(14,4) DEFAULT '0.0000' COMMENT '入库数量(个数)',
  `quantity_box` int DEFAULT '0' COMMENT '入库箱数',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '目标仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品入库单行表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_product_sales` (
  `sales_id` bigint NOT NULL AUTO_INCREMENT COMMENT '出库单ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `sales_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '出库单编码',
  `sales_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '出库单名称',
  `client_id` bigint NOT NULL COMMENT '客户ID(关联qxx_md_client)',
  `client_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户编码',
  `client_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户名称',
  `client_order_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户订单号(PO号)',
  `salesperson` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务员',
  `warehouse_id` bigint NOT NULL COMMENT '出货仓库ID(关联qxx_wm_warehouse)',
  `warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库名称',
  `sales_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '出库日期',
  `total_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '出库总数量',
  `total_box` int DEFAULT '0' COMMENT '总箱数',
  `total_volume` decimal(14,4) DEFAULT '0.0000' COMMENT '总体积(立方米)',
  `total_weight` decimal(14,4) DEFAULT '0.0000' COMMENT '总重量(吨)',
  `logistics_company` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '物流公司名称',
  `tracking_no` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '快递/物流单号',
  `logistics_fee` decimal(10,2) DEFAULT '0.00' COMMENT '物流费用(元)',
  `shipping_address` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '收货详细地址',
  `receiver_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '收货人姓名',
  `receiver_tel` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '收货人电话',
  `pallet_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '是否打托盘(1-是,0-否)',
  `box_label` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '箱唛/唛头描述',
  `sales_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'FOREIGN' COMMENT '销售类型:FOREIGN-外贸,DOMESTIC-内贸,SPOT-现货',
  `oqc_id` bigint DEFAULT NULL COMMENT '出货检验单ID(关联qxx_qc_oqc)',
  `oqc_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '出货检验单编码',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT' COMMENT '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账,SHIPPED-已发货',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`sales_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售出库单表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_product_sales_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `sales_id` bigint NOT NULL COMMENT '出库单ID(关联qxx_wm_product_sales)',
  `line_id` bigint NOT NULL COMMENT '行ID(关联qxx_wm_product_sales_line)',
  `item_id` bigint NOT NULL COMMENT '物料ID',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '出库数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `material_stock_id` bigint NOT NULL COMMENT '库存记录ID(关联qxx_wm_material_stock)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`detail_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售出库单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_product_sales_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `sales_id` bigint NOT NULL COMMENT '出库单ID(关联qxx_wm_product_sales)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity_sales` decimal(14,4) DEFAULT '0.0000' COMMENT '出库数量(个数)',
  `quantity_box` int DEFAULT '0' COMMENT '出库箱数',
  `box_spec` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '装箱规格(如250个/箱)',
  `box_length` decimal(10,2) DEFAULT '0.00' COMMENT '箱长(cm)',
  `box_width` decimal(10,2) DEFAULT '0.00' COMMENT '箱宽(cm)',
  `box_height` decimal(10,2) DEFAULT '0.00' COMMENT '箱高(cm)',
  `volume` decimal(14,4) DEFAULT '0.0000' COMMENT '体积(立方米)',
  `weight` decimal(14,4) DEFAULT '0.0000' COMMENT '重量(吨)',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售出库单行表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_roll_detail` (
  `roll_id` bigint NOT NULL AUTO_INCREMENT COMMENT '纸卷明细ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `roll_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '纸卷号(唯一,如RX20260608-001,可贴条码)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID(关联qxx_wm_batch)',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `recpt_id` bigint NOT NULL COMMENT '入库单ID(关联qxx_wm_item_recpt,哪张入库单收的)',
  `recpt_detail_id` bigint DEFAULT NULL COMMENT '入库明细ID(关联qxx_wm_item_recpt_detail)',
  `vendor_id` bigint DEFAULT NULL COMMENT '供应商ID(关联qxx_md_vendor)',
  `vendor_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商编码',
  `vendor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商名称',
  `vendor_roll_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商原始卷号(来料时的编号)',
  `parent_roll_id` bigint DEFAULT NULL COMMENT '父卷ID(自引用qxx_wm_roll_detail,分切场景:子卷→母卷)',
  `actual_width` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '实际门幅(mm),如923mm',
  `actual_weight_gsm` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '实际克重(g/㎡),如118g',
  `actual_length` decimal(14,2) DEFAULT '0.00' COMMENT '实际长度(米)',
  `actual_weight` decimal(14,4) DEFAULT '0.0000' COMMENT '实际重量(吨),到货过磅值',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '计量单位(如TON-吨,KG-千克,M-米)',
  `original_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '原始数量(入库时)',
  `remaining_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '剩余数量(扣减后)',
  `warehouse_id` bigint NOT NULL COMMENT '所在仓库ID(关联qxx_wm_warehouse)',
  `warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID(关联qxx_wm_storage_location)',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID(关联qxx_wm_storage_area)',
  `material_stock_id` bigint DEFAULT NULL COMMENT '库存记录ID(关联qxx_wm_material_stock,汇总库存行)',
  `last_issue_id` bigint DEFAULT NULL COMMENT '最近领料单ID(关联qxx_wm_issue_header)',
  `last_workorder_id` bigint DEFAULT NULL COMMENT '最近生产工单ID(关联qxx_pro_workorder)',
  `last_workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最近生产工单编码',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'IN_STOCK' COMMENT '纸卷状态:IN_STOCK-在库,PARTIAL-部分已用,CONSUMED-已用完,RETURNED-已退回,SCRAPPED-已报废',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`roll_id`),
  UNIQUE KEY `uk_roll_code` (`roll_code`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='纸卷明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_rt_issue` (
  `rt_id` bigint NOT NULL AUTO_INCREMENT COMMENT '退料单ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `rt_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '退料单编码',
  `rt_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '退料单名称',
  `issue_id` bigint DEFAULT NULL COMMENT '原领料单ID(关联qxx_wm_issue_header)',
  `issue_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原领料单编码',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID(关联qxx_pro_workorder)',
  `workorder_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产工单编码',
  `workorder_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `workstation_id` bigint DEFAULT NULL COMMENT '工作站ID(关联qxx_md_workstation)',
  `workstation_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `workstation_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `warehouse_id` bigint NOT NULL COMMENT '退入仓库ID(关联qxx_wm_warehouse)',
  `warehouse_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库名称',
  `rt_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '退料日期',
  `total_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '退料总数量',
  `unit_of_measure` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `rqc_id` bigint DEFAULT NULL COMMENT '退料质检单ID(关联qxx_qc_rqc)',
  `rqc_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '退料质检单编码',
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT' COMMENT '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`rt_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产退料单头表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_rt_issue_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `rt_id` bigint NOT NULL COMMENT '退料单ID(关联qxx_wm_rt_issue)',
  `line_id` bigint NOT NULL COMMENT '行ID(关联qxx_wm_rt_issue_line)',
  `item_id` bigint NOT NULL COMMENT '物料ID',
  `item_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '退料数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `material_stock_id` bigint NOT NULL COMMENT '库存记录ID(关联qxx_wm_material_stock)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`detail_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产退料单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_rt_issue_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `rt_id` bigint NOT NULL COMMENT '退料单ID(关联qxx_wm_rt_issue)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity_rt` decimal(14,4) DEFAULT '0.0000' COMMENT '退料数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产退料单行表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_rt_sales` (
  `rt_id` bigint NOT NULL AUTO_INCREMENT COMMENT '退货单ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `rt_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '退货单编码',
  `rt_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '退货单名称',
  `sales_id` bigint DEFAULT NULL COMMENT '原销售出库单ID(关联qxx_wm_product_sales)',
  `sales_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原销售出库单编码',
  `client_id` bigint NOT NULL COMMENT '客户ID(关联qxx_md_client)',
  `client_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户编码',
  `client_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户名称',
  `warehouse_id` bigint NOT NULL COMMENT '退货入库仓库ID(关联qxx_wm_warehouse)',
  `warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库名称',
  `rt_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '退货日期',
  `total_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '退货总数量',
  `rqc_id` bigint DEFAULT NULL COMMENT '退料质检单ID(关联qxx_qc_rqc)',
  `rqc_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '退料质检单编码',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT' COMMENT '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`rt_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售退货单表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_rt_sales_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `rt_id` bigint NOT NULL COMMENT '退货单ID(关联qxx_wm_rt_sales)',
  `line_id` bigint NOT NULL COMMENT '行ID(关联qxx_wm_rt_sales_line)',
  `item_id` bigint NOT NULL COMMENT '物料ID',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '退货数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `material_stock_id` bigint NOT NULL COMMENT '库存记录ID(关联qxx_wm_material_stock)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`detail_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售退货单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_rt_sales_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `rt_id` bigint NOT NULL COMMENT '退货单ID(关联qxx_wm_rt_sales)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity_rt` decimal(14,4) DEFAULT '0.0000' COMMENT '退货数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售退货单行表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_rt_vendor` (
  `rt_id` bigint NOT NULL AUTO_INCREMENT COMMENT '退货单ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `rt_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '退货单编码',
  `rt_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '退货单名称',
  `recpt_id` bigint DEFAULT NULL COMMENT '原入库单ID(关联qxx_wm_item_recpt)',
  `recpt_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原入库单编码',
  `vendor_id` bigint NOT NULL COMMENT '供应商ID(关联qxx_md_vendor)',
  `vendor_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商编码',
  `vendor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应商名称',
  `warehouse_id` bigint NOT NULL COMMENT '退货仓库ID(关联qxx_wm_warehouse)',
  `warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库名称',
  `rt_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '退货日期',
  `total_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '退货总数量',
  `rqc_id` bigint DEFAULT NULL COMMENT '退料质检单ID(关联qxx_qc_rqc)',
  `rqc_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '退料质检单编码',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT' COMMENT '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`rt_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购退货单表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_rt_vendor_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `rt_id` bigint NOT NULL COMMENT '退货单ID(关联qxx_wm_rt_vendor)',
  `line_id` bigint NOT NULL COMMENT '行ID(关联qxx_wm_rt_vendor_line)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '退货数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `material_stock_id` bigint NOT NULL COMMENT '库存记录ID(关联qxx_wm_material_stock)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`detail_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购退货单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_rt_vendor_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `rt_id` bigint NOT NULL COMMENT '退货单ID(关联qxx_wm_rt_vendor)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity_rt` decimal(14,4) DEFAULT '0.0000' COMMENT '退货数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购退货单行表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_stock_taking` (
  `taking_id` bigint NOT NULL AUTO_INCREMENT COMMENT '盘点任务ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `taking_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '盘点任务编码',
  `plan_id` bigint NOT NULL COMMENT '盘点方案ID(关联qxx_wm_stock_taking_plan)',
  `plan_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '盘点方案编码',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `book_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '账面数量',
  `actual_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '实际盘点数量',
  `difference` decimal(14,4) DEFAULT '0.0000' COMMENT '差异数量',
  `diff_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '差异原因',
  `taking_user` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '盘点人',
  `taking_date` datetime DEFAULT NULL COMMENT '盘点日期',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING' COMMENT '状态:PENDING-待盘点,TAKEN-已盘点,ADJUSTED-已调整',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`taking_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='盘点任务表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_stock_taking_plan` (
  `plan_id` bigint NOT NULL AUTO_INCREMENT COMMENT '盘点方案ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `plan_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '盘点方案编码',
  `plan_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '盘点方案名称',
  `plan_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'FULL' COMMENT '盘点类型:FULL-全盘,PARTIAL-抽盘,CYCLE-循环盘点',
  `warehouse_id` bigint DEFAULT NULL COMMENT '盘点仓库ID(关联qxx_wm_warehouse)',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '盘点仓库名称',
  `plan_date` datetime DEFAULT NULL COMMENT '计划盘点日期',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'PREPARE' COMMENT '状态:PREPARE-准备中,TAKING-盘点中,COMPLETED-已完成,ADJUSTED-已调整',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`plan_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='盘点方案表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_storage_area` (
  `area_id` bigint NOT NULL AUTO_INCREMENT COMMENT '库位ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `area_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '库位编码(唯一)',
  `area_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '库位名称',
  `location_id` bigint NOT NULL COMMENT '库区ID(关联qxx_wm_storage_location)',
  `location_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '库区编码',
  `location_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '库区名称',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID(关联qxx_wm_warehouse)',
  `warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库名称',
  `max_volume` decimal(14,2) DEFAULT '0.00' COMMENT '最大容积',
  `max_weight` decimal(14,2) DEFAULT '0.00' COMMENT '最大承重(吨)',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`area_id`),
  UNIQUE KEY `uk_area_code` (`area_code`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库位表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_storage_location` (
  `location_id` bigint NOT NULL AUTO_INCREMENT COMMENT '库区ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `location_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '库区编码(唯一)',
  `location_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '库区名称',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID(关联qxx_wm_warehouse)',
  `warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库名称',
  `area` decimal(14,2) DEFAULT '0.00' COMMENT '库区面积(平方米)',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`location_id`),
  UNIQUE KEY `uk_location_code` (`location_code`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库区表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_transaction` (
  `transaction_id` bigint NOT NULL AUTO_INCREMENT COMMENT '事务ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `transaction_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '事务类型:RECEIPT-入库,ISSUE-出库,TRANSFER-调拨,RETURN-退货,ADJUST-调整,SPLIT-分切',
  `source_doc_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '来源单据类型(如wm_item_recpt)',
  `source_doc_id` bigint NOT NULL COMMENT '来源单据ID',
  `source_doc_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '来源单据编码',
  `source_line_id` bigint DEFAULT NULL COMMENT '来源单据行ID',
  `material_stock_id` bigint NOT NULL COMMENT '库存记录ID(关联qxx_wm_material_stock)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '变动数量(正=入库,负=出库)',
  `unit2` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅助单位编码',
  `unit2_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '辅助单位名称',
  `quantity2` decimal(14,4) DEFAULT '0.0000' COMMENT '变动数量(辅助单位)',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID(关联qxx_wm_batch)',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库名称',
  `location_id` bigint DEFAULT NULL COMMENT '库区ID',
  `area_id` bigint DEFAULT NULL COMMENT '库位ID',
  `workorder_id` bigint DEFAULT NULL COMMENT '生产工单ID(关联qxx_pro_workorder)',
  `workorder_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生产工单编码',
  `vendor_id` bigint DEFAULT NULL COMMENT '供应商ID(关联qxx_md_vendor)',
  `client_id` bigint DEFAULT NULL COMMENT '客户ID(关联qxx_md_client)',
  `related_transaction_id` bigint DEFAULT NULL COMMENT '关联事务ID(调拨时使用)',
  `transaction_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '事务时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`transaction_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=383 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存事务表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_transfer` (
  `transfer_id` bigint NOT NULL AUTO_INCREMENT COMMENT '调拨单ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `transfer_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调拨单编码',
  `transfer_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '调拨单名称',
  `source_warehouse_id` bigint NOT NULL COMMENT '来源仓库ID(关联qxx_wm_warehouse)',
  `source_warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '来源仓库编码',
  `source_warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '来源仓库名称',
  `target_warehouse_id` bigint NOT NULL COMMENT '目标仓库ID(关联qxx_wm_warehouse)',
  `target_warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标仓库编码',
  `target_warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标仓库名称',
  `transfer_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '调拨日期',
  `total_quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '调拨总数量',
  `status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT' COMMENT '状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`transfer_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='调拨转移单表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_transfer_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `transfer_id` bigint NOT NULL COMMENT '调拨单ID(关联qxx_wm_transfer)',
  `line_id` bigint NOT NULL COMMENT '行ID(关联qxx_wm_transfer_line)',
  `item_id` bigint NOT NULL COMMENT '物料ID',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity` decimal(14,4) DEFAULT '0.0000' COMMENT '调拨数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `material_stock_id` bigint NOT NULL COMMENT '库存记录ID(关联qxx_wm_material_stock)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`detail_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='调拨转移单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_transfer_line` (
  `line_id` bigint NOT NULL AUTO_INCREMENT COMMENT '行ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `transfer_id` bigint NOT NULL COMMENT '调拨单ID(关联qxx_wm_transfer)',
  `item_id` bigint NOT NULL COMMENT '物料ID(关联qxx_md_item)',
  `item_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料编码',
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料名称',
  `specification` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit_of_measure` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位编码',
  `unit_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单位名称',
  `quantity_transfer` decimal(14,4) DEFAULT '0.0000' COMMENT '调拨数量',
  `batch_id` bigint DEFAULT NULL COMMENT '批次ID',
  `batch_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批次编码',
  `source_location_id` bigint DEFAULT NULL COMMENT '来源库区ID',
  `source_area_id` bigint DEFAULT NULL COMMENT '来源库位ID',
  `target_location_id` bigint DEFAULT NULL COMMENT '目标库区ID',
  `target_area_id` bigint DEFAULT NULL COMMENT '目标库位ID',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='调拨转移单行表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qxx_wm_warehouse` (
  `warehouse_id` bigint NOT NULL AUTO_INCREMENT COMMENT '仓库ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `warehouse_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '仓库编码(唯一)',
  `warehouse_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '仓库名称',
  `warehouse_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库类型:RAW-原料仓,FINISHED-成品仓,AUX-辅料仓,LINE-线边库,TEMP-临时仓',
  `address` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库位置/地址',
  `area` decimal(14,2) DEFAULT '0.00' COMMENT '仓库面积(平方米)',
  `charge` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '仓库负责人',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-是,0-否)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`warehouse_id`),
  UNIQUE KEY `uk_warehouse_code` (`warehouse_code`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=202 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓库表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_attachment` (
  `attachment_id` bigint NOT NULL AUTO_INCREMENT COMMENT '附件ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `source_doc_id` bigint DEFAULT NULL COMMENT '关联的业务单据ID',
  `source_doc_type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务单据类型(如wm_item_recpt/pro_workorder等)',
  `file_url` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件访问URL(相对路径)',
  `base_path` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '存储域名/Base路径',
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '存储文件名(UUID)',
  `orignal_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原始文件名(含扩展名)',
  `file_type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件类型(png/jpg/pdf/docx/xlsx等)',
  `file_size` double(12,2) DEFAULT NULL COMMENT '文件大小(KB)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`attachment_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=201 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通用附件表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_auto_code_part` (
  `part_id` bigint NOT NULL AUTO_INCREMENT COMMENT '分段ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `rule_id` bigint NOT NULL COMMENT '规则ID(关联sys_auto_code_rule)',
  `part_index` int NOT NULL COMMENT '分段序号(从1开始)',
  `part_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分段类型:INPUTCHAR-输入字符,NOWDATE-当前日期,FIXCHAR-固定字符,SERIALNO-流水号',
  `part_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分段编码',
  `part_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分段名称',
  `part_length` int NOT NULL COMMENT '分段长度',
  `date_format` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '日期时间格式(如yyyyMMdd)',
  `input_character` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '输入字符(INPUTCHAR类型时使用)',
  `fix_character` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '固定字符(FIXCHAR类型时使用)',
  `seria_start_no` int DEFAULT NULL COMMENT '流水号起始值',
  `seria_step` int DEFAULT NULL COMMENT '流水号步长',
  `seria_now_no` int DEFAULT NULL COMMENT '流水号当前值',
  `cycle_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '流水号是否循环(1-是,0-否)',
  `cycle_method` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '循环方式:YEAR-按年,MONTH-按月,DAY-按天,HOUR-按小时,MINUTE-按分钟,OTHER-按传入字符变',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`part_id`),
  UNIQUE KEY `uk_factory_rule_part` (`factory_id`,`rule_id`,`part_index`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=481 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='编码生成规则组成表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_auto_code_result` (
  `code_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `rule_id` bigint NOT NULL COMMENT '规则ID(关联sys_auto_code_rule)',
  `gen_date` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '生成日期时间(用于循环判断)',
  `gen_index` int DEFAULT NULL COMMENT '最后产生的序号',
  `last_result` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最后产生的完整编码值',
  `last_serial_no` int DEFAULT NULL COMMENT '最后产生的流水号',
  `last_input_char` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最后传入的参数(循环方式为OTHER时使用)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`code_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=281 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='编码生成记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_auto_code_rule` (
  `rule_id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `rule_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则编码',
  `rule_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则名称',
  `rule_desc` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规则描述',
  `max_length` int DEFAULT NULL COMMENT '最大长度',
  `is_padded` char(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '是否补齐(1-是,0-否)',
  `padded_char` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '补齐字符(如0)',
  `padded_method` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'L' COMMENT '补齐方式(L-左补齐,R-右补齐)',
  `enable_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1' COMMENT '是否启用(1-启用,0-停用)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`rule_id`),
  UNIQUE KEY `uk_rule_code` (`rule_code`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1019 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='编码生成规则表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_config` (
  `config_id` int NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  `config_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) COLLATE utf8mb4_general_ci DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`config_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='参数配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_dept` (
  `dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门id',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `parent_id` bigint DEFAULT '0' COMMENT '父部门id',
  `ancestors` varchar(50) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(30) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '部门名称',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `leader` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '负责人',
  `phone` varchar(11) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系电话',
  `email` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`dept_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=202 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='部门表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_dict_data` (
  `dict_code` bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `dict_sort` int DEFAULT '0' COMMENT '字典排序',
  `dict_label` varchar(100) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) COLLATE utf8mb4_general_ci DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_code`)
) ENGINE=InnoDB AUTO_INCREMENT=352 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典数据表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_dict_type` (
  `dict_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  `dict_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典名称',
  `dict_type` varchar(100) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典类型',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `dict_type` (`dict_type`)
) ENGINE=InnoDB AUTO_INCREMENT=208 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典类型表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_job` (
  `job_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `job_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '任务名称',
  `job_group` varchar(64) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
  `invoke_target` varchar(500) COLLATE utf8mb4_general_ci NOT NULL COMMENT '调用目标字符串',
  `cron_expression` varchar(255) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT 'cron执行表达式',
  `misfire_policy` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '3' COMMENT '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
  `concurrent` char(1) COLLATE utf8mb4_general_ci DEFAULT '1' COMMENT '是否并发执行（0允许 1禁止）',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1暂停）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '备注信息',
  PRIMARY KEY (`job_id`,`job_name`,`job_group`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='定时任务调度表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_job_log` (
  `job_log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
  `job_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务名称',
  `job_group` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务组名',
  `invoke_target` varchar(500) COLLATE utf8mb4_general_ci NOT NULL COMMENT '调用目标字符串',
  `job_message` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '日志信息',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '执行状态（0正常 1失败）',
  `exception_info` varchar(2000) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '异常信息',
  `start_time` datetime DEFAULT NULL COMMENT '执行开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '执行结束时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`job_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='定时任务调度日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_logininfor` (
  `info_id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  `user_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '用户账号',
  `ipaddr` varchar(128) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '登录IP地址',
  `login_location` varchar(255) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '登录地点',
  `browser` varchar(50) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '浏览器类型',
  `os` varchar(50) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '操作系统',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `msg` varchar(255) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '提示消息',
  `login_time` datetime DEFAULT NULL COMMENT '访问时间',
  PRIMARY KEY (`info_id`),
  KEY `idx_sys_logininfor_s` (`status`),
  KEY `idx_sys_logininfor_lt` (`login_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1167 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统访问记录';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_menu` (
  `menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜单名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `path` varchar(200) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组件路径',
  `query` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '路由参数',
  `route_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '路由名称',
  `is_frame` int DEFAULT '1' COMMENT '是否为外链（0是 1否）',
  `is_cache` int DEFAULT '0' COMMENT '是否缓存（0缓存 1不缓存）',
  `menu_type` char(1) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) COLLATE utf8mb4_general_ci DEFAULT '#' COMMENT '菜单图标',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`menu_id`),
  UNIQUE KEY `uk_parent_perms` (`parent_id`,`perms`)
) ENGINE=InnoDB AUTO_INCREMENT=28245 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜单权限表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_message` (
  `message_id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `user_id` bigint NOT NULL COMMENT '接收用户ID(关联sys_user)',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息标题',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '消息内容',
  `message_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息类型:SYSTEM-系统消息,APPROVAL-审批通知,WARNING-预警通知,NOTICE-公告',
  `source_doc_id` bigint DEFAULT NULL COMMENT '关联业务单据ID',
  `source_doc_type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联业务单据类型',
  `read_flag` char(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '是否已读(1-已读,0-未读)',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`message_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=201 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统消息表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_notice` (
  `notice_id` int NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `notice_title` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告标题',
  `notice_type` char(1) COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告类型（1通知 2公告）',
  `notice_content` longblob COMMENT '公告内容',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '公告状态（0正常 1关闭）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知公告表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_notice_read` (
  `read_id` bigint NOT NULL AUTO_INCREMENT COMMENT '已读主键',
  `notice_id` int NOT NULL COMMENT '公告id',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `read_time` datetime NOT NULL COMMENT '阅读时间',
  PRIMARY KEY (`read_id`),
  UNIQUE KEY `uk_user_notice` (`user_id`,`notice_id`) COMMENT '同一用户同一公告只记录一次'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公告已读记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_oper_log` (
  `oper_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `title` varchar(50) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '模块标题',
  `business_type` int DEFAULT '0' COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` varchar(200) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '请求方式',
  `operator_type` int DEFAULT '0' COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '操作人员',
  `dept_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '部门名称',
  `oper_url` varchar(255) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(128) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '主机地址',
  `oper_location` varchar(255) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '操作地点',
  `oper_param` varchar(2000) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(2000) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '返回参数',
  `status` int DEFAULT '0' COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(2000) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  `cost_time` bigint DEFAULT '0' COMMENT '消耗时间',
  PRIMARY KEY (`oper_id`),
  KEY `idx_sys_oper_log_bt` (`business_type`),
  KEY `idx_sys_oper_log_s` (`status`),
  KEY `idx_sys_oper_log_ot` (`oper_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2494 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作日志记录';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_post` (
  `post_id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `post_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '岗位编码',
  `post_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '岗位名称',
  `post_sort` int NOT NULL COMMENT '显示顺序',
  `status` char(1) COLLATE utf8mb4_general_ci NOT NULL COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`post_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='岗位信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_role` (
  `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `role_name` varchar(30) COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色权限字符串',
  `role_sort` int NOT NULL COMMENT '显示顺序',
  `data_scope` char(1) COLLATE utf8mb4_general_ci DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
  `menu_check_strictly` tinyint(1) DEFAULT '1' COMMENT '菜单树选择项是否关联显示',
  `dept_check_strictly` tinyint(1) DEFAULT '1' COMMENT '部门树选择项是否关联显示',
  `status` char(1) COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色状态（0正常 1停用）',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=106 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_role_dept` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID',
  PRIMARY KEY (`role_id`,`dept_id`,`factory_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色和部门关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID',
  PRIMARY KEY (`role_id`,`menu_id`,`factory_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色和菜单关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_todo_list` (
  `todo_id` bigint NOT NULL AUTO_INCREMENT COMMENT '待办ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `user_id` bigint NOT NULL COMMENT '待办人用户ID(关联sys_user)',
  `todo_title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '待办标题',
  `todo_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '待办类型:APPROVAL-审批,QC_CHECK-质检,DV_CHECK-点检,MAINTEN-保养,REPAIR-维修,OTHER-其他',
  `source_doc_id` bigint DEFAULT NULL COMMENT 'å…³è”ä¸šåŠ¡å•æ®ID',
  `source_doc_type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'å…³è”ä¸šåŠ¡å•æ®ç±»åž‹',
  `source_doc_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联业务单据编码',
  `priority` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'NORMAL' COMMENT '优先级:URGENT-紧急,HIGH-高,NORMAL-普通,LOW-低',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING' COMMENT '状态:PENDING-待处理,PROCESSING-处理中,COMPLETED-已完成,REJECTED-已驳回',
  `deadline` datetime DEFAULT NULL COMMENT '截止时间',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `handle_result` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '处理结果/意见',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`todo_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=202 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通用待办事项表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID(关联qxx_md_factory)',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `user_name` varchar(30) COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户账号',
  `nick_name` varchar(30) COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户昵称',
  `user_type` varchar(2) COLLATE utf8mb4_general_ci DEFAULT '00' COMMENT '用户类型（00系统用户）',
  `email` varchar(50) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(11) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '手机号码',
  `sex` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` varchar(100) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '头像地址',
  `password` varchar(100) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '密码',
  `status` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '账号状态（0正常 1停用）',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `login_ip` varchar(128) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
  `pwd_update_date` datetime DEFAULT NULL COMMENT '密码最后更新时间',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=106 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_user_post` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID',
  PRIMARY KEY (`user_id`,`post_id`,`factory_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户与岗位关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `factory_id` bigint NOT NULL COMMENT '工厂ID',
  PRIMARY KEY (`user_id`,`role_id`,`factory_id`),
  KEY `idx_factory_id` (`factory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户和角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
