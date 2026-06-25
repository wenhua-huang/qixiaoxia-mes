-- ============================================================
-- 圣享工厂种子数据
-- 用法: mysql -uroot -p mes < seed_shengxiang.sql
-- 幂等: INSERT IGNORE 防重复
-- 生成: 2026-06-11 14:42:07
-- ============================================================

-- MySQL dump 10.13  Distrib 8.0.45, for Linux (aarch64)
--
-- Host: localhost    Database: mes
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `qxx_md_factory`
--

LOCK TABLES `qxx_md_factory` WRITE;
/*!40000 ALTER TABLE `qxx_md_factory` DISABLE KEYS */;
INSERT IGNORE INTO `qxx_md_factory` (`factory_id`, `factory_code`, `factory_name`, `short_name`, `address`, `contact`, `phone`, `enable_flag`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1,'SX','圣享工厂','圣享',NULL,NULL,NULL,'1','','admin','2026-06-10 01:22:16','','2026-06-10 01:22:16'),(100,'FT_CURL_1781101159','CURL测试工厂',NULL,NULL,NULL,NULL,'1','','','2026-06-10 22:19:20','','2026-06-10 22:19:19'),(101,'TEST_FACTORY','测试工厂',NULL,NULL,NULL,NULL,'1','','','2026-06-10 22:21:39','','2026-06-10 22:21:38'),(102,'FT_CURL_1781101965','CURL测试工厂',NULL,NULL,NULL,NULL,'1','','','2026-06-10 22:32:45','','2026-06-10 22:32:45');
/*!40000 ALTER TABLE `qxx_md_factory` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `qxx_md_workshop`
--

LOCK TABLES `qxx_md_workshop` WRITE;
/*!40000 ALTER TABLE `qxx_md_workshop` DISABLE KEYS */;
INSERT IGNORE INTO `qxx_md_workshop` (`workshop_id`, `factory_id`, `workshop_code`, `workshop_name`, `address`, `manager`, `enable_flag`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (202,1,'PRINT','印刷车间','圣享工厂1楼','吴兵','1','','','2026-06-10 23:16:20','','2026-06-10 23:16:20'),(203,1,'BAG','制袋车间','圣享工厂1楼','吴兵','1','','','2026-06-10 23:16:21','','2026-06-10 23:16:20');
/*!40000 ALTER TABLE `qxx_md_workshop` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `qxx_md_item_type`
--

LOCK TABLES `qxx_md_item_type` WRITE;
/*!40000 ALTER TABLE `qxx_md_item_type` DISABLE KEYS */;
INSERT IGNORE INTO `qxx_md_item_type` (`item_type_id`, `factory_id`, `item_type_code`, `item_type_name`, `parent_type_id`, `item_or_product`, `order_num`, `enable_flag`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (206,1,'SEMI','半成品',223,'SEMI',1,'1','','','2026-06-10 23:16:21','','2026-06-11 00:10:32'),(207,1,'FINISHED','成品',223,'FINISHED',1,'1','','','2026-06-10 23:16:21','','2026-06-11 00:10:32'),(208,1,'AUXILIARY','辅料',223,'AUXILIARY',1,'1','','','2026-06-10 23:16:21','','2026-06-11 00:10:32'),(209,1,'PACK','包材',223,'PACK',1,'1','','','2026-06-10 23:16:21','','2026-06-11 00:10:32'),(214,1,'RAW-PAPER','纸张',222,'RAW',1,'1','','','2026-06-10 23:25:02','','2026-06-10 23:30:04'),(215,1,'RAW-ROPE','绳子/织带',222,'RAW',1,'1','','','2026-06-10 23:25:02','','2026-06-10 23:30:04'),(216,1,'RAW-GLUE','胶水',222,'RAW',1,'1','','','2026-06-10 23:25:02','','2026-06-10 23:30:04'),(217,1,'RAW-INK','油墨',222,'RAW',1,'1','','','2026-06-10 23:25:02','','2026-06-10 23:30:04'),(218,1,'SEMI-PRINTED','印刷后半成品',206,'SEMI',2,'1','','','2026-06-10 23:25:02','','2026-06-11 13:12:33'),(219,1,'FINISHED-BAG','纸袋成品',207,'FINISHED',1,'1','','','2026-06-10 23:25:02','','2026-06-10 23:25:01'),(220,1,'AUX-ROPE','绳子辅料',208,'AUXILIARY',1,'1','','','2026-06-10 23:25:02','','2026-06-10 23:25:01'),(221,1,'PACK-BOX','纸箱包材',209,'PACK',1,'1','','','2026-06-10 23:25:02','','2026-06-10 23:25:01'),(222,1,'RAW','原料',223,'RAW',1,'1','','','2026-06-10 23:30:04','','2026-06-11 00:10:32'),(223,1,'ROOT','物料产品分类',0,'',0,'1','','','2026-06-11 00:10:31','','2026-06-11 13:55:25');
/*!40000 ALTER TABLE `qxx_md_item_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `qxx_md_unit_measure`
--

LOCK TABLES `qxx_md_unit_measure` WRITE;
/*!40000 ALTER TABLE `qxx_md_unit_measure` DISABLE KEYS */;
INSERT IGNORE INTO `qxx_md_unit_measure` (`unit_id`, `factory_id`, `unit_code`, `unit_name`, `primary_unit`, `conversion_rate`, `enable_flag`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (200,1,'PCS','个',NULL,1.000000,'Y','计数单位，用于成品/纸箱/塑料盖等','admin','2026-06-10 10:02:01','','2026-06-10 19:59:39'),(201,1,'KG','千克',NULL,1.000000,'Y','重量主单位，用于纸张/原料称重','admin','2026-06-10 10:02:01','','2026-06-10 10:04:16'),(202,1,'M','米',NULL,1.000000,'Y','长度单位，用于纸张门幅/米数','admin','2026-06-10 10:02:01','','2026-06-10 10:04:16'),(203,1,'ROLL','卷',NULL,1.000000,'Y','纸张计数单位，一卷纸的完整包装','admin','2026-06-10 10:02:01','','2026-06-10 10:04:16'),(204,1,'PAIR','对',NULL,1.000000,'Y','成对计数单位，用于绳子/织带/指甲扣','admin','2026-06-10 10:02:01','','2026-06-10 10:04:16'),(205,1,'BARREL','桶',NULL,1.000000,'Y','容量单位，用于油墨/胶水','admin','2026-06-10 10:02:01','','2026-06-10 10:04:16'),(206,1,'SET','套',NULL,1.000000,'Y','成套单位，用于工具/模具','admin','2026-06-10 10:02:01','','2026-06-10 10:04:16'),(210,1,'BOX','箱','PCS',250.000000,'Y','包装单位，1箱=250个（按实际装箱规格调整）','admin','2026-06-10 10:02:01','','2026-06-10 10:04:16'),(211,1,'TON','吨','KG',1000.000000,'Y','重量单位，1吨=1000千克','admin','2026-06-10 10:02:01','','2026-06-10 10:04:16'),(212,1,'G','克','KG',0.001000,'Y','重量单位，1克=0.001千克','admin','2026-06-10 10:02:01','','2026-06-10 10:04:16'),(215,1,'T_ZZZ','测试',NULL,1.000000,'Y','','','2026-06-10 11:00:37','','2026-06-10 11:00:36'),(224,1,'T_VER','验证',NULL,1.000000,'Y','','','2026-06-10 16:02:09','','2026-06-10 16:02:09'),(226,1,'T_A2','别名测试',NULL,1.000000,'Y','','','2026-06-10 16:50:37','','2026-06-10 16:50:37'),(227,1,'T_INS2','INSERT测试',NULL,1.000000,'Y','','','2026-06-10 17:02:50','','2026-06-10 17:02:49'),(228,1,'T_A3','A3',NULL,1.000000,'Y','','','2026-06-10 17:41:13','','2026-06-10 17:41:12'),(229,1,'API_T1','测试',NULL,1.000000,'Y','','','2026-06-10 18:05:35','','2026-06-10 18:05:35'),(230,1,'FK_UNIT','FK单位',NULL,1.000000,'Y','','','2026-06-10 18:08:19','','2026-06-10 18:08:18');
/*!40000 ALTER TABLE `qxx_md_unit_measure` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `qxx_md_item`
--

LOCK TABLES `qxx_md_item` WRITE;
/*!40000 ALTER TABLE `qxx_md_item` DISABLE KEYS */;
INSERT IGNORE INTO `qxx_md_item` (`item_id`, `factory_id`, `item_code`, `item_name`, `specification`, `unit_of_measure`, `unit_name`, `unit2`, `unit2_name`, `conversion_rate`, `item_type_id`, `item_type_code`, `item_type_name`, `parent_id`, `product_size`, `package_spec`, `printing_req`, `enable_flag`, `safe_stock_flag`, `min_stock`, `max_stock`, `alert_stock`, `high_value`, `batch_flag`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (201,1,'PAPER-XBZ-A','箱板纸 A级 925mm 120g','925mm*120g','ROLL','卷','TON','吨',0.6970,214,'RAW-PAPER','纸张',0,NULL,NULL,'','Y','1',10.0000,200.0000,20.0000,'0','1','1','','2026-06-10 23:19:12','','2026-06-11 02:18:31'),(202,1,'PAPER-XBZ-B','箱板纸 B级 1020mm 140g','1020mm*140g','ROLL','卷','TON','吨',0.7500,214,'RAW-PAPER','纸张',0,NULL,NULL,NULL,'1','0',0.0000,0.0000,0.0000,'0','1','','','2026-06-10 23:19:12','','2026-06-11 11:35:52'),(203,1,'PAPER-NK','牛卡纸 1100mm 150g','1100mm*150g','ROLL','卷','TON','吨',0.8200,214,'RAW-PAPER','纸张',0,NULL,NULL,NULL,'1','0',0.0000,0.0000,0.0000,'0','1','','','2026-06-10 23:19:12','','2026-06-11 00:03:49'),(204,1,'PAPER-WNP','白牛皮纸 980mm 100g','980mm*100g','ROLL','卷','TON','吨',0.6500,214,'RAW-PAPER','纸张',0,NULL,NULL,NULL,'1','0',0.0000,0.0000,0.0000,'0','1','','','2026-06-10 23:19:12','','2026-06-11 00:03:49'),(205,1,'PAPER-RC','瑞典赤牛 1050mm 160g','1050mm*160g','ROLL','卷','TON','吨',0.7800,214,'RAW-PAPER','纸张',0,NULL,NULL,NULL,'1','0',0.0000,0.0000,0.0000,'0','1','','','2026-06-10 23:19:12','','2026-06-11 00:03:49'),(206,1,'AUX-ROPE-001','红色圆纸绳 7.5cm间距','7.5cm间距','PCS','对',NULL,NULL,1.0000,215,'','',0,NULL,NULL,NULL,'1','0',0.0000,0.0000,0.0000,'0','1','','','2026-06-10 23:19:12','','2026-06-11 00:05:57'),(207,1,'AUX-ROPE-002','螺纹带+指甲扣','标准','PCS','对',NULL,NULL,1.0000,215,'','',0,NULL,NULL,NULL,'1','0',0.0000,0.0000,0.0000,'0','1','','','2026-06-10 23:19:12','','2026-06-11 00:05:57'),(208,1,'AUX-GLUE-001','制袋胶水','25kg/桶','KG','千克',NULL,NULL,1.0000,216,'','',0,NULL,NULL,NULL,'1','0',0.0000,0.0000,0.0000,'0','1','','','2026-06-10 23:19:12','','2026-06-11 00:05:57'),(209,1,'AUX-INK-001','印刷油墨 黑色','20L/桶','KG','千克',NULL,NULL,1.0000,217,'','',0,NULL,NULL,NULL,'1','0',0.0000,0.0000,0.0000,'0','1','','','2026-06-10 23:19:12','','2026-06-11 00:05:57'),(210,1,'AUX-PPCOVER','PP塑料盖','标准口径','PCS','个',NULL,NULL,1.0000,220,'','',0,NULL,NULL,NULL,'1','0',0.0000,0.0000,0.0000,'0','1','','','2026-06-10 23:19:12','','2026-06-11 00:05:58'),(211,1,'FIN-BENQU-001','奔趣纸袋 小号','254*127*330mm','PCS','个','BOX','箱',250.0000,219,'FINISHED-BAG','纸袋成品',0,'254*127*330mm','250个/箱','1色满版黑印刷','1','0',0.0000,0.0000,0.0000,'0','1','','','2026-06-10 23:19:12','','2026-06-11 00:18:33'),(212,1,'FIN-SOS-001','SOS方底袋 大号','400*200*500mm','PCS','个','BOX','箱',200.0000,219,'FINISHED-BAG','纸袋成品',0,'400*200*500mm','200个/箱','彩色LOGO印刷','1','0',0.0000,0.0000,0.0000,'0','1','','','2026-06-10 23:19:12','','2026-06-11 00:03:49'),(213,1,'FIN-BLACK-001','黑色旗帜纸袋 中号','320*150*400mm','PCS','个','BOX','箱',250.0000,219,'FINISHED-BAG','纸袋成品',0,'320*150*400mm','250个/箱','黑色满版印刷+旗帜图案','1','0',0.0000,0.0000,0.0000,'0','1','','','2026-06-10 23:19:12','','2026-06-11 00:03:49'),(214,1,'FIN-PICO-001','PICO纸袋 标准','280*140*350mm','PCS','个','BOX','箱',300.0000,219,'FINISHED-BAG','纸袋成品',0,'280*140*350mm','300个/箱','双色印刷','1','0',0.0000,0.0000,0.0000,'0','1','','','2026-06-10 23:19:12','','2026-06-11 00:03:49'),(215,1,'FIN-CUP-001','单杯袋 标准','150*100*250mm','PCS','个','BOX','箱',500.0000,219,'FINISHED-BAG','纸袋成品',0,'150*100*250mm','500个/箱','单色印刷','1','0',0.0000,0.0000,0.0000,'0','1','','','2026-06-10 23:19:12','','2026-06-11 00:03:49');
/*!40000 ALTER TABLE `qxx_md_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `qxx_md_item_attr_paper`
--

LOCK TABLES `qxx_md_item_attr_paper` WRITE;
/*!40000 ALTER TABLE `qxx_md_item_attr_paper` DISABLE KEYS */;
INSERT IGNORE INTO `qxx_md_item_attr_paper` (`attr_id`, `factory_id`, `item_id`, `paper_width`, `paper_weight`, `paper_source`, `paper_type`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (200,1,201,'925mm','120g','联盛A2','箱板纸','','','2026-06-10 23:19:12','','2026-06-11 02:18:31'),(201,1,202,'1020mm','140g','联盛A2','箱板纸','','','2026-06-10 23:19:12','','2026-06-11 11:35:53'),(202,1,203,'1100mm','150g','蓝叶-牛卡','牛卡纸','','','2026-06-10 23:19:12','','2026-06-10 23:19:11'),(203,1,204,'980mm','100g','德欣纸业','白牛皮纸','','','2026-06-10 23:19:12','','2026-06-10 23:19:11'),(204,1,205,'1050mm','160g','进口','瑞典赤牛','','','2026-06-10 23:19:12','','2026-06-10 23:19:11');
/*!40000 ALTER TABLE `qxx_md_item_attr_paper` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `qxx_md_item_attr_paper_bag`
--

LOCK TABLES `qxx_md_item_attr_paper_bag` WRITE;
/*!40000 ALTER TABLE `qxx_md_item_attr_paper_bag` DISABLE KEYS */;
INSERT IGNORE INTO `qxx_md_item_attr_paper_bag` (`attr_id`, `factory_id`, `item_id`, `rope_spec`, `mouth_type`, `bottom_type`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (200,1,211,'7.5cm间距黄牛皮色圆纸绳','锯齿口','灰底白板','','','2026-06-10 23:19:12','','2026-06-11 00:18:33'),(201,1,212,'10cm间距白色圆纸绳','平口','加强底板','','','2026-06-10 23:19:12','','2026-06-10 23:19:12'),(202,1,213,'7.5cm间距黑纸绳','翻口','加强底板','','','2026-06-10 23:19:12','','2026-06-10 23:19:12'),(203,1,214,'7.5cm间距黄牛皮纸绳','锯齿口','无底板','','','2026-06-10 23:19:12','','2026-06-10 23:19:12'),(204,1,215,'5cm间距细纸绳','平口','灰底白板','','','2026-06-10 23:19:12','','2026-06-10 23:19:12');
/*!40000 ALTER TABLE `qxx_md_item_attr_paper_bag` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `qxx_md_item_attr_gift_box`
--

LOCK TABLES `qxx_md_item_attr_gift_box` WRITE;
/*!40000 ALTER TABLE `qxx_md_item_attr_gift_box` DISABLE KEYS */;
/*!40000 ALTER TABLE `qxx_md_item_attr_gift_box` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `qxx_md_item_batch_config`
--

LOCK TABLES `qxx_md_item_batch_config` WRITE;
/*!40000 ALTER TABLE `qxx_md_item_batch_config` DISABLE KEYS */;
INSERT IGNORE INTO `qxx_md_item_batch_config` (`config_id`, `factory_id`, `item_id`, `produce_date_flag`, `expire_date_flag`, `recpt_date_flag`, `vendor_flag`, `client_flag`, `co_code_flag`, `po_code_flag`, `workorder_flag`, `task_flag`, `workstation_flag`, `tool_flag`, `mold_flag`, `lot_number_flag`, `quality_status_flag`, `paper_roll_flag`, `paper_width_flag`, `enable_flag`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (200,1,200,'1',NULL,NULL,'1',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'1','','','2026-06-10 22:19:20','','2026-06-10 22:19:20'),(201,1,201,'1','1','0','1','0','0','0','0','0','0','0','0','0','0','0','0','1','','','2026-06-11 02:11:58','','2026-06-11 02:11:57');
/*!40000 ALTER TABLE `qxx_md_item_batch_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `qxx_md_product_bom`
--

LOCK TABLES `qxx_md_product_bom` WRITE;
/*!40000 ALTER TABLE `qxx_md_product_bom` DISABLE KEYS */;
INSERT IGNORE INTO `qxx_md_product_bom` (`bom_id`, `factory_id`, `item_id`, `bom_item_id`, `unit_of_measure`, `quantity`, `scrap_rate`, `enable_flag`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (200,1,200,201,'PCS',3.0000,0.0200,'1','','','2026-06-10 22:21:40','','2026-06-10 22:21:39'),(201,1,211,201,'KG',0.0500,0.0300,'1','','','2026-06-11 02:24:57','','2026-06-11 02:24:57'),(202,1,211,206,'PCS',1.0000,0.0100,'1','','','2026-06-11 02:24:57','','2026-06-11 02:24:57'),(203,1,211,208,'KG',0.0030,0.0200,'1','','','2026-06-11 02:24:57','','2026-06-11 02:24:57'),(204,1,211,209,'KG',0.0010,0.0100,'1','','','2026-06-11 02:24:57','','2026-06-11 02:24:57'),(205,1,212,203,'KG',0.0800,0.0400,'1','','','2026-06-11 02:24:57','','2026-06-11 02:24:57'),(206,1,212,207,'PCS',1.0000,0.0100,'1','','','2026-06-11 02:24:58','','2026-06-11 02:24:57'),(207,1,212,208,'KG',0.0050,0.0200,'1','','','2026-06-11 02:24:58','','2026-06-11 02:24:57'),(208,1,212,209,'KG',0.0020,0.0100,'1','','','2026-06-11 02:24:58','','2026-06-11 02:24:57'),(209,1,213,205,'KG',0.0600,0.0300,'1','','','2026-06-11 02:24:58','','2026-06-11 02:24:57'),(210,1,213,206,'PCS',1.0000,0.0100,'1','','','2026-06-11 02:24:58','','2026-06-11 02:24:57'),(211,1,213,208,'KG',0.0040,0.0200,'1','','','2026-06-11 02:24:58','','2026-06-11 02:24:57'),(212,1,213,209,'KG',0.0030,0.0200,'1','','','2026-06-11 02:24:58','','2026-06-11 02:24:57'),(213,1,214,204,'KG',0.0400,0.0300,'1','','','2026-06-11 02:24:58','','2026-06-11 02:24:57'),(214,1,214,206,'PCS',1.0000,0.0100,'1','','','2026-06-11 02:24:58','','2026-06-11 02:24:57'),(215,1,214,208,'KG',0.0020,0.0200,'1','','','2026-06-11 02:24:58','','2026-06-11 02:24:58'),(216,1,215,202,'KG',0.0200,0.0300,'1','','','2026-06-11 02:24:58','','2026-06-11 02:24:58'),(217,1,215,206,'PCS',1.0000,0.0100,'1','','','2026-06-11 02:24:58','','2026-06-11 02:24:58'),(218,1,215,208,'KG',0.0010,0.0200,'1','','','2026-06-11 02:24:58','','2026-06-11 02:24:58');
/*!40000 ALTER TABLE `qxx_md_product_bom` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `qxx_md_vendor`
--

LOCK TABLES `qxx_md_vendor` WRITE;
/*!40000 ALTER TABLE `qxx_md_vendor` DISABLE KEYS */;
INSERT IGNORE INTO `qxx_md_vendor` (`vendor_id`, `factory_id`, `vendor_code`, `vendor_name`, `outsource_factory_id`, `vendor_nick`, `vendor_en`, `vendor_type`, `vendor_des`, `vendor_logo`, `vendor_level`, `vendor_score`, `address`, `website`, `email`, `tel`, `contact1`, `contact1_tel`, `contact1_email`, `contact2`, `contact2_tel`, `contact2_email`, `credit_code`, `settlement_type`, `payment_terms`, `coop_status`, `enable_flag`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (202,1,'VEN-DEXIN','德欣纸业',NULL,'德欣',NULL,'MATERIAL','箱板纸/牛卡纸供应商',NULL,'B',0,'浙江省温州市',NULL,NULL,NULL,'德欣','13800000001',NULL,NULL,NULL,NULL,NULL,'MONTHLY','月结30天','ACTIVE','1','','','2026-06-10 23:19:12','','2026-06-11 02:17:53'),(203,1,'VEN-XUANCHUN','宣春华',NULL,'宣春华',NULL,'MATERIAL','白牛皮纸供应商',NULL,'C',0,'浙江省',NULL,NULL,NULL,'宣春华',NULL,NULL,NULL,NULL,NULL,NULL,'CASH','现结','ACTIVE','1','','','2026-06-10 23:19:13','','2026-06-11 02:17:53'),(204,1,'VEN-DONGCHENG','温州东诚',NULL,'东诚',NULL,'MATERIAL','五层加强瓦楞纸箱',NULL,'A',0,'浙江省温州市',NULL,NULL,NULL,'缪存溢',NULL,NULL,NULL,NULL,NULL,NULL,'MONTHLY','月结60天','ACTIVE','1','','','2026-06-10 23:19:13','','2026-06-11 02:17:53'),(205,1,'VEN-SHAOBEN','绍本织带',NULL,'绍本',NULL,'MATERIAL','螺纹带+指甲扣供应商',NULL,'B',0,'浙江省',NULL,NULL,NULL,'绍本',NULL,NULL,NULL,NULL,NULL,NULL,'MONTHLY','月结30天','ACTIVE','1','','','2026-06-10 23:19:13','','2026-06-11 02:17:53'),(206,1,'VEN-AINAJIE','爱娜洁纸杯厂',NULL,'爱娜洁',NULL,'MATERIAL','PP盖供应商',NULL,'C',0,'浙江省',NULL,NULL,NULL,'爱娜洁',NULL,NULL,NULL,NULL,NULL,NULL,'CASH','现结','ACTIVE','1','','','2026-06-10 23:19:13','','2026-06-11 02:17:53'),(207,1,'VEN-ZHENGQING','浙江征庆',NULL,'征庆',NULL,'MATERIAL','印刷油墨供应商',NULL,'B',0,'浙江省',NULL,NULL,NULL,'浙江征庆',NULL,NULL,NULL,NULL,NULL,NULL,'MONTHLY','月结30天','ACTIVE','1','','','2026-06-10 23:19:13','','2026-06-11 02:17:53'),(208,1,'OUT-WANLONG','万隆',NULL,'万隆',NULL,'OUTSOURCE','整单外发（印刷+制袋）',NULL,'A',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'MONTHLY','月结30天','ACTIVE','1','','','2026-06-10 23:19:13','','2026-06-11 02:17:53'),(209,1,'OUT-JIRONG','吉荣',NULL,'吉荣',NULL,'OUTSOURCE','整单外发（印刷+制袋）',NULL,'B',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'MONTHLY','月结30天','ACTIVE','1','','','2026-06-10 23:19:13','','2026-06-11 02:17:53'),(210,1,'OUT-HAOZHUO','浩卓',NULL,'浩卓',NULL,'OUTSOURCE','整单外发（印刷+制袋）',NULL,'B',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'MONTHLY','月结30天','ACTIVE','1','','','2026-06-10 23:19:13','','2026-06-11 02:17:53'),(211,1,'OUT-OUNUO','欧诺',NULL,'欧诺',NULL,'OUTSOURCE','整单外发（印刷+制袋）',NULL,'C',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'CASH','现结','ACTIVE','1','','','2026-06-10 23:19:13','','2026-06-11 02:17:53'),(212,1,'OUT-SHENGHAO','温州圣皓包装',NULL,'圣皓',NULL,'OUTSOURCE','专业贴绳合作伙伴',NULL,'A',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'MONTHLY','加工费月结30天','ACTIVE','1','','','2026-06-10 23:19:13','','2026-06-11 02:17:53');
/*!40000 ALTER TABLE `qxx_md_vendor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `qxx_md_client`
--

LOCK TABLES `qxx_md_client` WRITE;
/*!40000 ALTER TABLE `qxx_md_client` DISABLE KEYS */;
INSERT IGNORE INTO `qxx_md_client` (`client_id`, `factory_id`, `client_code`, `client_name`, `client_nick`, `client_en`, `client_type`, `salesperson`, `credit_code`, `address`, `website`, `email`, `tel`, `contact1`, `contact1_tel`, `contact1_email`, `contact2`, `contact2_tel`, `contact2_email`, `shipping_address`, `enable_flag`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (201,1,'CLI-FOREIGN-001','海外客户 A','海外A','Overseas A','FOREIGN','陈仁林',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'1','','','2026-06-10 23:19:13','','2026-06-10 23:19:12'),(202,1,'CLI-FOREIGN-002','海外客户 B','海外B','Overseas B','FOREIGN','陈仁林',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'1','','','2026-06-10 23:19:13','','2026-06-10 23:19:12'),(203,1,'CLI-DOMESTIC-001','内贸客户 A','内贸A',NULL,'DOMESTIC','陈丽丽',NULL,'浙江省温州市',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'1','','','2026-06-10 23:19:13','','2026-06-10 23:19:13'),(204,1,'CLI-DOMESTIC-002','内贸客户 B','内贸B',NULL,'DOMESTIC','陈丽丽',NULL,'浙江省杭州市',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'1','','','2026-06-10 23:19:13','','2026-06-10 23:19:13'),(205,1,'CLI-SPOT-001','现货客户','现货',NULL,'SPOT','陈丽丽',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'1','','','2026-06-10 23:19:13','','2026-06-10 23:19:13');
/*!40000 ALTER TABLE `qxx_md_client` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `sys_dict_type` (MES字典类型)
--

INSERT IGNORE INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(20, '工作站类型', 'mes_workstation_type', '0', 'admin', NOW(), '', NULL, '工作站类型列表'),
(21, '工序类型',   'mes_process_type',     '0', 'admin', NOW(), '', NULL, '工序类型列表'),
(22, '工作站状态', 'mes_workstation_status','0', 'admin', NOW(), '', NULL, '工作站状态列表');

--
-- Dumping data for table `sys_dict_data` (MES字典数据)
--

INSERT IGNORE INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(100, 1, '印刷机',       'PRINT',       'mes_workstation_type', '', 'primary',   'Y', '0', 'admin', NOW(), '', NULL, '工作站类型-印刷机'),
(101, 2, '全自动制袋机', 'BAG_AUTO',    'mes_workstation_type', '', 'success',   'N', '0', 'admin', NOW(), '', NULL, '工作站类型-全自动制袋机'),
(102, 3, '半自动制袋机', 'BAG_SEMI',    'mes_workstation_type', '', 'warning',   'N', '0', 'admin', NOW(), '', NULL, '工作站类型-半自动制袋机'),
(103, 4, '其他',         'OTHER',       'mes_workstation_type', '', 'info',      'N', '0', 'admin', NOW(), '', NULL, '工作站类型-其他'),
(110, 1, '印刷',         'PRINT',       'mes_process_type',     '', 'primary',   'Y', '0', 'admin', NOW(), '', NULL, '工序类型-印刷'),
(111, 2, '制袋',         'BAG_MAKE',    'mes_process_type',     '', 'success',   'N', '0', 'admin', NOW(), '', NULL, '工序类型-制袋'),
(112, 3, '分切',         'SLITTING',    'mes_process_type',     '', 'warning',   'N', '0', 'admin', NOW(), '', NULL, '工序类型-分切'),
(113, 4, '检验',         'INSPECT',     'mes_process_type',     '', 'info',      'N', '0', 'admin', NOW(), '', NULL, '工序类型-检验'),
(120, 1, '空闲',         'IDLE',        'mes_workstation_status','', 'success',   'Y', '0', 'admin', NOW(), '', NULL, '工作站状态-空闲'),
(121, 2, '运行中',       'RUNNING',     'mes_workstation_status','', 'primary',   'N', '0', 'admin', NOW(), '', NULL, '工作站状态-运行中'),
(122, 3, '保养中',       'MAINTENANCE', 'mes_workstation_status','', 'warning',   'N', '0', 'admin', NOW(), '', NULL, '工作站状态-保养中'),
(123, 4, '故障',         'BREAKDOWN',   'mes_workstation_status','', 'danger',    'N', '0', 'admin', NOW(), '', NULL, '工作站状态-故障');

-- 客户类型
INSERT IGNORE INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(30, '客户类型',     'mes_client_type',        '0', 'admin', NOW(), '', NULL, '客户类型列表'),
(31, '物料产品分类', 'mes_item_type',          '0', 'admin', NOW(), '', NULL, '物料/产品分类列表'),
(32, '供应商类型',   'mes_vendor_type',        '0', 'admin', NOW(), '', NULL, '供应商类型列表'),
(33, '结算方式',     'mes_settlement_type',    '0', 'admin', NOW(), '', NULL, '结算方式列表'),
(34, '合作状态',     'mes_coop_status',        '0', 'admin', NOW(), '', NULL, '合作状态列表'),
(35, '仓库类型',     'mes_warehouse_type',     '0', 'admin', NOW(), '', NULL, '仓库类型列表'),
(36, '设备状态',     'mes_machinery_status',   '0', 'admin', NOW(), '', NULL, '设备状态列表'),
(37, '订单状态',     'mes_order_status',        '0', 'admin', NOW(), '', NULL, '采购/生产订单状态列表'),
(38, '采购类型',     'mes_purchase_type',       '0', 'admin', NOW(), '', NULL, '采购类型列表');

INSERT IGNORE INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
-- 客户类型 (mes_client_type)
(200, 1, '内贸',     'DOMESTIC', 'mes_client_type', '', 'primary', 'Y', '0', 'admin', NOW(), '', NULL, '客户类型-内贸'),
(201, 2, '外贸',     'FOREIGN',  'mes_client_type', '', 'success', 'N', '0', 'admin', NOW(), '', NULL, '客户类型-外贸'),
(202, 3, '现货',     'SPOT',     'mes_client_type', '', 'warning', 'N', '0', 'admin', NOW(), '', NULL, '客户类型-现货'),
-- 物料产品分类 (mes_item_type)
(210, 1, '原料',     'RAW',       'mes_item_type', '', 'primary', 'Y', '0', 'admin', NOW(), '', NULL, '物料分类-原料'),
(211, 2, '半成品',   'SEMI',      'mes_item_type', '', 'success', 'N', '0', 'admin', NOW(), '', NULL, '物料分类-半成品'),
(212, 3, '成品',     'FINISHED',  'mes_item_type', '', 'warning', 'N', '0', 'admin', NOW(), '', NULL, '物料分类-成品'),
(213, 4, '辅料',     'AUXILIARY', 'mes_item_type', '', 'info',    'N', '0', 'admin', NOW(), '', NULL, '物料分类-辅料'),
(214, 5, '包材',     'PACK',      'mes_item_type', '', 'danger',  'N', '0', 'admin', NOW(), '', NULL, '物料分类-包材'),
-- 供应商类型 (mes_vendor_type)
(220, 1, '原材料供应商', 'MATERIAL',  'mes_vendor_type', '', 'primary', 'Y', '0', 'admin', NOW(), '', NULL, '供应商类型-原材料'),
(221, 2, '外协加工商',   'OUTSOURCE', 'mes_vendor_type', '', 'success', 'N', '0', 'admin', NOW(), '', NULL, '供应商类型-外协'),
(222, 3, '两者皆是',     'BOTH',      'mes_vendor_type', '', 'warning', 'N', '0', 'admin', NOW(), '', NULL, '供应商类型-两者皆是'),
-- 结算方式 (mes_settlement_type)
(230, 1, '月结',     'MONTHLY', 'mes_settlement_type', '', 'primary', 'Y', '0', 'admin', NOW(), '', NULL, '结算方式-月结'),
(231, 2, '现结',     'CASH',    'mes_settlement_type', '', 'success', 'N', '0', 'admin', NOW(), '', NULL, '结算方式-现结'),
(232, 3, '其他',     'OTHER',   'mes_settlement_type', '', 'info',    'N', '0', 'admin', NOW(), '', NULL, '结算方式-其他'),
-- 合作状态 (mes_coop_status)
(240, 1, '合作中',   'ACTIVE',   'mes_coop_status', '', 'success', 'Y', '0', 'admin', NOW(), '', NULL, '合作状态-合作中'),
(241, 2, '暂停',     'INACTIVE', 'mes_coop_status', '', 'warning', 'N', '0', 'admin', NOW(), '', NULL, '合作状态-暂停'),
(242, 3, '待审核',   'PENDING',  'mes_coop_status', '', 'info',    'N', '0', 'admin', NOW(), '', NULL, '合作状态-待审核'),
(243, 4, '终止',     'STOPPED',  'mes_coop_status', '', 'danger',  'N', '0', 'admin', NOW(), '', NULL, '合作状态-终止'),
-- 仓库类型 (mes_warehouse_type)
(250, 1, '原料仓',   'RAW',      'mes_warehouse_type', '', 'primary', 'Y', '0', 'admin', NOW(), '', NULL, '仓库类型-原料仓'),
(251, 2, '成品仓',   'FINISHED', 'mes_warehouse_type', '', 'success', 'N', '0', 'admin', NOW(), '', NULL, '仓库类型-成品仓'),
(252, 3, '辅料仓',   'AUX',      'mes_warehouse_type', '', 'info',    'N', '0', 'admin', NOW(), '', NULL, '仓库类型-辅料仓'),
(253, 4, '线边库',   'LINE',     'mes_warehouse_type', '', 'warning', 'N', '0', 'admin', NOW(), '', NULL, '仓库类型-线边库'),
(254, 5, '临时仓',   'TEMP',     'mes_warehouse_type', '', 'danger',  'N', '0', 'admin', NOW(), '', NULL, '仓库类型-临时仓'),
-- 设备状态 (mes_machinery_status)
(260, 1, '空闲',     'IDLE',       'mes_machinery_status', '', 'success', 'Y', '0', 'admin', NOW(), '', NULL, '设备状态-空闲'),
(261, 2, '运行中',   'RUNNING',    'mes_machinery_status', '', 'primary', 'N', '0', 'admin', NOW(), '', NULL, '设备状态-运行中'),
(262, 3, '保养中',   'MAINTENANCE','mes_machinery_status', '', 'warning', 'N', '0', 'admin', NOW(), '', NULL, '设备状态-保养中'),
(263, 4, '故障停机', 'BREAKDOWN',  'mes_machinery_status', '', 'danger',  'N', '0', 'admin', NOW(), '', NULL, '设备状态-故障停机'),
-- 订单状态 (mes_order_status)
(270, 1, '草稿',     'DRAFT',      'mes_order_status', '', 'info',    'Y', '0', 'admin', NOW(), '', NULL, '订单状态-草稿'),
(271, 2, '已审批',   'APPROVED',   'mes_order_status', '', 'warning', 'N', '0', 'admin', NOW(), '', NULL, '订单状态-已审批'),
(272, 3, '已下单',   'ORDERED',    'mes_order_status', '', 'primary', 'N', '0', 'admin', NOW(), '', NULL, '订单状态-已下单'),
(273, 4, '收货中',   'RECEIVING',  'mes_order_status', '', 'success', 'N', '0', 'admin', NOW(), '', NULL, '订单状态-收货中'),
(274, 5, '已收货',   'RECEIVED',   'mes_order_status', '', 'success', 'N', '0', 'admin', NOW(), '', NULL, '订单状态-已收货'),
(275, 6, '已关闭',   'CLOSED',     'mes_order_status', '', 'danger',  'N', '0', 'admin', NOW(), '', NULL, '订单状态-已关闭'),
(276, 7, '已取消',   'CANCEL',     'mes_order_status', '', 'danger',  'N', '0', 'admin', NOW(), '', NULL, '订单状态-已取消'),
-- 采购类型 (mes_purchase_type)
(280, 1, '纸张',     'PAPER',      'mes_purchase_type', '', 'primary', 'Y', '0', 'admin', NOW(), '', NULL, '采购类型-纸张'),
(281, 2, '辅料',     'AUX',        'mes_purchase_type', '', 'success', 'N', '0', 'admin', NOW(), '', NULL, '采购类型-辅料'),
(282, 3, '包材',     'PACK',       'mes_purchase_type', '', 'warning', 'N', '0', 'admin', NOW(), '', NULL, '采购类型-包材'),
(283, 4, '其他',     'OTHER',      'mes_purchase_type', '', 'info',    'N', '0', 'admin', NOW(), '', NULL, '采购类型-其他');

--
-- Dumping data for table `qxx_md_workstation`
--

LOCK TABLES `qxx_md_workstation` WRITE;
/*!40000 ALTER TABLE `qxx_md_workstation` DISABLE KEYS */;
INSERT IGNORE INTO `qxx_md_workstation` (`workstation_id`, `factory_id`, `workstation_code`, `workstation_name`, `workshop_id`, `workstation_type`, `process_id`, `process_type`, `capacity`, `status`, `enable_flag`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (200,1,'WST_CURL_1781101160','CURL测试工作站',200,NULL,NULL,NULL,0,'IDLE','1','','','2026-06-10 22:19:20','','2026-06-10 22:19:20'),(201,1,'TEST_WS_001','测试印刷机',200,NULL,NULL,NULL,100,'IDLE','1','','','2026-06-10 22:21:39','','2026-06-10 22:21:38'),(202,1,'WST_1781101966','CURL工作站',200,NULL,NULL,NULL,0,'IDLE','1','','','2026-06-10 22:32:46','','2026-06-10 22:32:46'),(203,1,'PRINT-01','1号印刷机',200,'PRINT',NULL,'PRINT',5000,'IDLE','1','','','2026-06-10 23:19:13','','2026-06-10 23:19:13'),(204,1,'PRINT-02','2号印刷机',200,'PRINT',NULL,'PRINT',5000,'IDLE','1','','','2026-06-10 23:19:13','','2026-06-10 23:19:13'),(205,1,'BAG-01','1号制袋机',200,'BAG_SEMI',NULL,'BAG_MAKE',3000,'IDLE','1','','','2026-06-10 23:19:13','','2026-06-10 23:19:13'),(206,1,'BAG-02','2号制袋机',200,'BAG_SEMI',NULL,'BAG_MAKE',3000,'IDLE','1','','','2026-06-10 23:19:13','','2026-06-10 23:19:13'),(207,1,'BAG-03','3号制袋机（全自动）',200,'BAG_AUTO',NULL,'BAG_MAKE',8000,'RUNNING','1','','','2026-06-10 23:19:13','','2026-06-10 23:19:13'),(208,1,'BAG-04','4号制袋机',200,'BAG_SEMI',NULL,'BAG_MAKE',3000,'IDLE','1','','','2026-06-10 23:19:14','','2026-06-10 23:19:13'),(209,1,'BAG-05','5号制袋机（全自动）',200,'BAG_AUTO',NULL,'BAG_MAKE',8000,'RUNNING','1','','','2026-06-10 23:19:14','','2026-06-10 23:19:13'),(210,1,'BAG-06','6号制袋机',200,'BAG_SEMI',NULL,'BAG_MAKE',3000,'IDLE','1','','','2026-06-10 23:19:14','','2026-06-10 23:19:13'),(211,1,'BAG-07','7号制袋机（全自动）',200,'BAG_AUTO',NULL,'BAG_MAKE',8000,'RUNNING','1','','','2026-06-10 23:19:14','','2026-06-10 23:19:13'),(212,1,'BAG-08','8号制袋机（全自动）',200,'BAG_AUTO',NULL,'BAG_MAKE',8000,'IDLE','1','','','2026-06-10 23:19:14','','2026-06-10 23:19:13'),(213,1,'BAG-09','9号制袋机（半自动）',200,'BAG_SEMI',NULL,'BAG_MAKE',2000,'IDLE','1','','','2026-06-10 23:19:14','','2026-06-10 23:19:13');
/*!40000 ALTER TABLE `qxx_md_workstation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `qxx_md_workstation_machine`
--

LOCK TABLES `qxx_md_workstation_machine` WRITE;
/*!40000 ALTER TABLE `qxx_md_workstation_machine` DISABLE KEYS */;
INSERT IGNORE INTO `qxx_md_workstation_machine` (`record_id`, `factory_id`, `workstation_id`, `machinery_id`, `machinery_code`, `machinery_name`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (200,1,202,0,'M-CURL','CURL电机','','','2026-06-10 22:32:46','','2026-06-10 22:32:46');
/*!40000 ALTER TABLE `qxx_md_workstation_machine` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `qxx_md_workstation_worker`
--

LOCK TABLES `qxx_md_workstation_worker` WRITE;
/*!40000 ALTER TABLE `qxx_md_workstation_worker` DISABLE KEYS */;
INSERT IGNORE INTO `qxx_md_workstation_worker` (`record_id`, `factory_id`, `workstation_id`, `user_id`, `user_name`, `role_type`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (200,1,202,0,'CURL操作工','OPERATOR','','','2026-06-10 22:32:46','','2026-06-10 22:32:46');
/*!40000 ALTER TABLE `qxx_md_workstation_worker` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `qxx_md_product_sop`
--

LOCK TABLES `qxx_md_product_sop` WRITE;
/*!40000 ALTER TABLE `qxx_md_product_sop` DISABLE KEYS */;
/*!40000 ALTER TABLE `qxx_md_product_sop` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `qxx_md_product_sip`
--

LOCK TABLES `qxx_md_product_sip` WRITE;
/*!40000 ALTER TABLE `qxx_md_product_sip` DISABLE KEYS */;
/*!40000 ALTER TABLE `qxx_md_product_sip` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `qxx_dv_machinery_type` (设备类型 — 圣享工厂)
--

LOCK TABLES `qxx_dv_machinery_type` WRITE;
/*!40000 ALTER TABLE `qxx_dv_machinery_type` DISABLE KEYS */;
INSERT IGNORE INTO `qxx_dv_machinery_type` (`machinery_type_id`, `factory_id`, `machinery_type_code`, `machinery_type_name`, `parent_type_id`, `order_num`, `enable_flag`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(199,1,'ROOT','总设备',0,0,'1','','admin',NOW(),'',NOW()),
(200,1,'PRINT','印刷机',199,1,'1','','admin',NOW(),'',NOW()),
(201,1,'BAG','制袋机',199,2,'1','','admin',NOW(),'',NOW()),
(202,1,'BAG-AUTO','全自动制袋机',201,1,'1','','admin',NOW(),'',NOW()),
(203,1,'BAG-SEMI','半自动制袋机',201,2,'1','','admin',NOW(),'',NOW());
/*!40000 ALTER TABLE `qxx_dv_machinery_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `qxx_dv_machinery` (设备台账 — 圣享工厂)
--

LOCK TABLES `qxx_dv_machinery` WRITE;
/*!40000 ALTER TABLE `qxx_dv_machinery` DISABLE KEYS */;
INSERT IGNORE INTO `qxx_dv_machinery` (`machinery_id`, `factory_id`, `machinery_code`, `machinery_name`, `machinery_brand`, `machinery_spec`, `machinery_type_id`, `machinery_type_code`, `machinery_type_name`, `workshop_id`, `workshop_code`, `workshop_name`, `status`, `enable_flag`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(200,1,'PRINT-01','1号印刷机','海德堡',NULL,200,'PRINT','印刷机',202,'PRINT','印刷车间','RUNNING','1','','admin',NOW(),'',NOW()),
(201,1,'PRINT-02','2号印刷机','海德堡',NULL,200,'PRINT','印刷机',202,'PRINT','印刷车间','IDLE','1','','admin',NOW(),'',NOW()),
(202,1,'BAG-01','1号制袋机','正博',NULL,203,'BAG-SEMI','半自动制袋机',203,'BAG','制袋车间','IDLE','1','','admin',NOW(),'',NOW()),
(203,1,'BAG-02','2号制袋机','正博',NULL,203,'BAG-SEMI','半自动制袋机',203,'BAG','制袋车间','IDLE','1','','admin',NOW(),'',NOW()),
(204,1,'BAG-03','3号制袋机（全自动）','正博',NULL,202,'BAG-AUTO','全自动制袋机',203,'BAG','制袋车间','RUNNING','1','','admin',NOW(),'',NOW()),
(205,1,'BAG-04','4号制袋机','正博',NULL,203,'BAG-SEMI','半自动制袋机',203,'BAG','制袋车间','IDLE','1','','admin',NOW(),'',NOW()),
(206,1,'BAG-05','5号制袋机（全自动）','正博',NULL,202,'BAG-AUTO','全自动制袋机',203,'BAG','制袋车间','RUNNING','1','','admin',NOW(),'',NOW()),
(207,1,'BAG-06','6号制袋机','正博',NULL,203,'BAG-SEMI','半自动制袋机',203,'BAG','制袋车间','IDLE','1','','admin',NOW(),'',NOW()),
(208,1,'BAG-07','7号制袋机（全自动）','正博',NULL,202,'BAG-AUTO','全自动制袋机',203,'BAG','制袋车间','RUNNING','1','','admin',NOW(),'',NOW()),
(209,1,'BAG-08','8号制袋机（全自动）','正博',NULL,202,'BAG-AUTO','全自动制袋机',203,'BAG','制袋车间','IDLE','1','','admin',NOW(),'',NOW()),
(210,1,'BAG-09','9号制袋机（半自动）','正博',NULL,203,'BAG-SEMI','半自动制袋机',203,'BAG','制袋车间','IDLE','1','','admin',NOW(),'',NOW());
/*!40000 ALTER TABLE `qxx_dv_machinery` ENABLE KEYS */;
UNLOCK TABLES;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-11 14:41:43
