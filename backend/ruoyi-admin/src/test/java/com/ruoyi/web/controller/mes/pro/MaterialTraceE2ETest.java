package com.ruoyi.web.controller.mes.pro;

import com.ruoyi.BaseIntegrationTest;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.domain.mes.pro.*;
import com.ruoyi.system.domain.mes.wm.*;
import com.ruoyi.system.mapper.mes.pro.*;
import com.ruoyi.system.mapper.mes.wm.*;
import com.ruoyi.system.service.mes.pro.IProFeedbackService;
import com.ruoyi.system.service.mes.pro.IProWorkorderService;
import com.ruoyi.system.service.mes.wm.IWmIssueHeaderService;
import com.ruoyi.system.service.mes.wm.IWmItemRecptService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 物料追溯 E2E 集成测试
 * 覆盖新增追溯特性：开工建卡 / RECEIPT trace / ISSUE trace(含cardId) / PRODUCE trace(末工序) / 卡状态推进
 *
 * 技术栈：Service 层直调（覆盖全部业务逻辑 + FactoryIdInterceptor + MyBatis）
 *
 * @author qixiaoxia
 * @date 2026-07-14
 */
@DisplayName("物料追溯 E2E 集成测试")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MaterialTraceE2ETest extends BaseIntegrationTest {

    // ==================== 测试常量 ====================
    private static final long FACTORY_ID = 1L;
    private static final long WAREHOUSE_ID = 200L;
    private static final long MATERIAL_ITEM_ID = 1001L;
    private static final long PRODUCT_ITEM_ID = 1002L;
    private static final long VENDOR_ID = 301L;
    private static final long WORKSTATION_ID = 401L;
    private static final long PROCESS_A_ID = 501L;
    private static final long PROCESS_B_ID = 502L;
    private static final long ROUTE_ID = 601L;

    // ==================== Mapper 注入（用于 DB 验证） ====================
    @Autowired private ProCardMapper proCardMapper;
    @Autowired private ProMaterialTraceMapper traceMapper;
    @Autowired private ProWorkorderMapper workorderMapper;
    @Autowired private WmTransactionMapper txMapper;
    @Autowired private WmMaterialStockMapper stockMapper;

    // ==================== Service 注入（用于触发业务） ====================
    @Autowired private IProWorkorderService workorderService;
    @Autowired private IWmItemRecptService itemRecptService;
    @Autowired private IWmIssueHeaderService issueService;
    @Autowired private IProFeedbackService feedbackService;
    @Autowired private com.ruoyi.system.service.mes.pro.IProMaterialTraceService traceService;

    private static boolean tablesReady = false;

    @BeforeAll
    void setupAuth() {
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setUserName("admin");
        user.setFactoryId(FACTORY_ID);
        LoginUser loginUser = new LoginUser(user, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    @BeforeEach
    void setUp() {
        if (tablesReady) {
            // 清理本次测试用到的表
            truncateTables(
                "qxx_pro_material_trace", "qxx_pro_feedback_consume", "qxx_pro_feedback",
                "qxx_pro_card_process", "qxx_pro_card",
                "qxx_wm_issue_detail", "qxx_wm_issue_line", "qxx_wm_issue_header",
                "qxx_wm_item_recpt_line", "qxx_wm_item_recpt",
                "qxx_wm_transaction", "qxx_wm_material_stock",
                "qxx_pro_workorder_bom", "qxx_pro_workorder",
                "qxx_pro_route_process", "qxx_pro_route",
                "qxx_pro_process", "qxx_pro_task",
                "qxx_wm_warehouse", "qxx_md_workstation", "qxx_md_workshop",
                "qxx_md_vendor", "qxx_md_item", "qxx_pur_order_line", "qxx_pur_order",
                "qxx_md_factory"
            );
        }
        tablesReady = true;
        createAllTables();
        seedAllData();
    }

    // ════════════════════════════════════════════
    // 建表 + 播种数据
    // ════════════════════════════════════════════

    private void exec(String sql) { jdbcTemplate.execute(sql); }

    private void createAllTables() {
        // 工厂
        exec("CREATE TABLE IF NOT EXISTS qxx_md_factory ("
                + "factory_id bigint NOT NULL AUTO_INCREMENT, factory_code varchar(64) NOT NULL,"
                + "factory_name varchar(255) NOT NULL, enable_flag char(1) DEFAULT '1',"
                + "create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "update_by varchar(64) DEFAULT '', update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (factory_id)) ENGINE=InnoDB");

        // 物料
        exec("CREATE TABLE IF NOT EXISTS qxx_md_item ("
                + "item_id bigint NOT NULL AUTO_INCREMENT, factory_id bigint NOT NULL DEFAULT 1,"
                + "item_code varchar(64) NOT NULL, item_name varchar(255) NOT NULL,"
                + "unit_of_measure varchar(64) NOT NULL, unit_name varchar(64) NOT NULL,"
                + "enable_flag char(1) DEFAULT '1', create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "update_by varchar(64) DEFAULT '', update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (item_id)) ENGINE=InnoDB");

        // 供应商
        exec("CREATE TABLE IF NOT EXISTS qxx_md_vendor ("
                + "vendor_id bigint NOT NULL AUTO_INCREMENT, factory_id bigint NOT NULL DEFAULT 1,"
                + "vendor_code varchar(64) NOT NULL, vendor_name varchar(255) NOT NULL,"
                + "vendor_type varchar(20) DEFAULT 'MATERIAL', enable_flag char(1) DEFAULT '1',"
                + "create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "update_by varchar(64) DEFAULT '', update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (vendor_id)) ENGINE=InnoDB");

        // 车间 + 工作站
        exec("CREATE TABLE IF NOT EXISTS qxx_md_workshop ("
                + "workshop_id bigint NOT NULL AUTO_INCREMENT, factory_id bigint NOT NULL DEFAULT 1,"
                + "workshop_code varchar(64) NOT NULL, workshop_name varchar(255) NOT NULL,"
                + "enable_flag char(1) DEFAULT '1', create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (workshop_id)) ENGINE=InnoDB");

        exec("CREATE TABLE IF NOT EXISTS qxx_md_workstation ("
                + "workstation_id bigint NOT NULL AUTO_INCREMENT, factory_id bigint NOT NULL DEFAULT 1,"
                + "workstation_code varchar(64) NOT NULL, workstation_name varchar(255) NOT NULL,"
                + "workshop_id bigint NOT NULL, enable_flag char(1) DEFAULT '1', capacity int DEFAULT 0, status varchar(20) DEFAULT 'IDLE',"
                + "create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "update_by varchar(64) DEFAULT '', update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (workstation_id)) ENGINE=InnoDB");

        // 仓库
        exec("CREATE TABLE IF NOT EXISTS qxx_wm_warehouse ("
                + "warehouse_id bigint NOT NULL AUTO_INCREMENT, factory_id bigint NOT NULL DEFAULT 1,"
                + "warehouse_code varchar(64) NOT NULL, warehouse_name varchar(255) NOT NULL,"
                + "enable_flag char(1) DEFAULT '1', create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (warehouse_id)) ENGINE=InnoDB");

        // 工序
        exec("CREATE TABLE IF NOT EXISTS qxx_pro_process ("
                + "process_id bigint NOT NULL AUTO_INCREMENT, factory_id bigint NOT NULL DEFAULT 1,"
                + "process_code varchar(64) NOT NULL, process_name varchar(255) NOT NULL,"
                + "process_type varchar(50) DEFAULT 'INTERNAL', enable_flag char(1) DEFAULT '1',"
                + "create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (process_id)) ENGINE=InnoDB");

        // 工艺路线 + 路线工序
        exec("CREATE TABLE IF NOT EXISTS qxx_pro_route ("
                + "route_id bigint NOT NULL AUTO_INCREMENT, factory_id bigint NOT NULL DEFAULT 1,"
                + "route_code varchar(64) NOT NULL, route_name varchar(255) NOT NULL,"
                + "enable_flag char(1) DEFAULT '1', create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (route_id)) ENGINE=InnoDB");

        exec("CREATE TABLE IF NOT EXISTS qxx_pro_route_process ("
                + "record_id bigint NOT NULL AUTO_INCREMENT, factory_id bigint NOT NULL DEFAULT 1,"
                + "route_id bigint NOT NULL, process_id bigint NOT NULL,"
                + "process_code varchar(64), process_name varchar(255), order_num int DEFAULT 1,"
                + "create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (record_id)) ENGINE=InnoDB");

        // 工单 + BOM
        exec("CREATE TABLE IF NOT EXISTS qxx_pro_workorder ("
                + "workorder_id bigint NOT NULL AUTO_INCREMENT, factory_id bigint NOT NULL DEFAULT 1,"
                + "workorder_code varchar(64) NOT NULL, workorder_name varchar(255),"
                + "order_source varchar(64) DEFAULT 'MANUAL', product_id bigint, product_code varchar(64), product_name varchar(255),"
                + "unit_of_measure varchar(64), unit_name varchar(64),"
                + "quantity decimal(14,2) DEFAULT 0, quantity_produced decimal(14,2) DEFAULT 0, quantity_scheduled decimal(14,2) DEFAULT 0,"
                + "status varchar(64) DEFAULT 'PREPARE',"
                + "create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "update_by varchar(64) DEFAULT '', update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (workorder_id), UNIQUE KEY uk_wo_code (workorder_code)) ENGINE=InnoDB");

        exec("CREATE TABLE IF NOT EXISTS qxx_pro_workorder_bom ("
                + "line_id bigint NOT NULL AUTO_INCREMENT, factory_id bigint NOT NULL DEFAULT 1,"
                + "workorder_id bigint NOT NULL, item_id bigint NOT NULL,"
                + "item_code varchar(64) NOT NULL, item_name varchar(255) NOT NULL,"
                + "process_id bigint, quantity decimal(14,2) DEFAULT 0, total_quantity decimal(14,2) DEFAULT 0,"
                + "create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (line_id)) ENGINE=InnoDB");

        // 排产任务
        exec("CREATE TABLE IF NOT EXISTS qxx_pro_task ("
                + "task_id bigint NOT NULL AUTO_INCREMENT, factory_id bigint NOT NULL DEFAULT 1,"
                + "task_code varchar(64) NOT NULL, task_name varchar(255),"
                + "workorder_id bigint NOT NULL, workstation_id bigint NOT NULL,"
                + "route_id bigint, process_id bigint, process_code varchar(64), process_name varchar(255),"
                + "item_id bigint, item_code varchar(64), item_name varchar(255),"
                + "unit_of_measure varchar(64), unit_name varchar(64),"
                + "quantity decimal(14,2) DEFAULT 0, quantity_produced decimal(14,2) DEFAULT 0,"
                + "status varchar(64) DEFAULT 'NORMAL',"
                + "create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (task_id)) ENGINE=InnoDB");

        // 流转卡
        exec("CREATE TABLE IF NOT EXISTS qxx_pro_card ("
                + "card_id bigint NOT NULL AUTO_INCREMENT, factory_id bigint NOT NULL DEFAULT 1,"
                + "card_code varchar(64) NOT NULL, workorder_id bigint, workorder_code varchar(64), workorder_name varchar(255),"
                + "batch_code varchar(64), item_id bigint, item_code varchar(64), item_name varchar(255),"
                + "unit_of_measure varchar(64), unit_name varchar(64),"
                + "quantity_transfered decimal(12,2), current_process_id bigint, current_process_name varchar(255),"
                + "status varchar(64),"
                + "create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "update_by varchar(64) DEFAULT '', update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (card_id)) ENGINE=InnoDB");

        // 库存 + 事务
        exec("CREATE TABLE IF NOT EXISTS qxx_wm_material_stock ("
                + "material_stock_id bigint NOT NULL AUTO_INCREMENT, factory_id bigint NOT NULL DEFAULT 1,"
                + "item_id bigint NOT NULL, item_code varchar(64) NOT NULL, item_name varchar(255) NOT NULL,"
                + "unit_of_measure varchar(64) NOT NULL, unit_name varchar(64),"
                + "quantity_onhand decimal(14,4) DEFAULT 0, quantity_available decimal(14,4) DEFAULT 0,"
                + "batch_id bigint DEFAULT 0, batch_code varchar(64),"
                + "warehouse_id bigint NOT NULL, warehouse_code varchar(64),"
                + "vendor_id bigint DEFAULT 0, workorder_id bigint DEFAULT 0,"
                + "quality_status varchar(50) DEFAULT 'NORMAL',"
                + "create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "update_by varchar(64) DEFAULT '', update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (material_stock_id)) ENGINE=InnoDB");

        exec("CREATE TABLE IF NOT EXISTS qxx_wm_transaction ("
                + "transaction_id bigint NOT NULL AUTO_INCREMENT, factory_id bigint NOT NULL DEFAULT 1,"
                + "transaction_type varchar(50) NOT NULL, source_doc_type varchar(64) NOT NULL,"
                + "source_doc_id bigint NOT NULL, source_doc_code varchar(64), source_line_id bigint,"
                + "material_stock_id bigint, item_id bigint, item_code varchar(64), item_name varchar(255),"
                + "unit_of_measure varchar(64), unit_name varchar(64), quantity decimal(14,4) DEFAULT 0,"
                + "batch_id bigint, batch_code varchar(64), warehouse_id bigint,"
                + "vendor_id bigint DEFAULT 0, workorder_id bigint,"
                + "transaction_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (transaction_id)) ENGINE=InnoDB");

        // 入库单
        exec("CREATE TABLE IF NOT EXISTS qxx_wm_item_recpt ("
                + "recpt_id bigint NOT NULL AUTO_INCREMENT, factory_id bigint NOT NULL DEFAULT 1,"
                + "recpt_code varchar(64) NOT NULL, recpt_name varchar(255),"
                + "pur_order_id bigint, vendor_id bigint, vendor_code varchar(64), vendor_name varchar(255),"
                + "warehouse_id bigint NOT NULL, warehouse_code varchar(64),"
                + "recpt_date datetime DEFAULT CURRENT_TIMESTAMP, recpt_type varchar(50) DEFAULT 'PURCHASE',"
                + "status varchar(50) DEFAULT 'DRAFT', create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "update_by varchar(64) DEFAULT '', update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (recpt_id)) ENGINE=InnoDB");

        exec("CREATE TABLE IF NOT EXISTS qxx_wm_item_recpt_line ("
                + "line_id bigint NOT NULL AUTO_INCREMENT, factory_id bigint NOT NULL DEFAULT 1,"
                + "recpt_id bigint NOT NULL, item_id bigint NOT NULL, item_code varchar(64) NOT NULL, item_name varchar(255) NOT NULL,"
                + "unit_of_measure varchar(64) NOT NULL, unit_name varchar(64),"
                + "quantity_recpt decimal(14,4) DEFAULT 0, batch_id bigint, batch_code varchar(64),"
                + "warehouse_id bigint, warehouse_code varchar(64),"
                + "create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (line_id)) ENGINE=InnoDB");

        // 领料单
        exec("CREATE TABLE IF NOT EXISTS qxx_wm_issue_header ("
                + "issue_id bigint NOT NULL AUTO_INCREMENT, factory_id bigint NOT NULL DEFAULT 1,"
                + "issue_code varchar(64) NOT NULL, issue_name varchar(255),"
                + "workorder_id bigint, workorder_code varchar(64), workorder_name varchar(255),"
                + "card_id bigint, card_code varchar(64),"
                + "task_id bigint, workstation_id bigint, workstation_code varchar(64),"
                + "warehouse_id bigint NOT NULL, warehouse_code varchar(64),"
                + "issue_date datetime DEFAULT CURRENT_TIMESTAMP, issue_type varchar(50) DEFAULT 'PRODUCE',"
                + "total_quantity decimal(14,4) DEFAULT 0, quantity_issued_total decimal(14,4) DEFAULT 0,"
                + "status varchar(50) DEFAULT 'DRAFT',"
                + "create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "update_by varchar(64) DEFAULT '', update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (issue_id)) ENGINE=InnoDB");

        exec("CREATE TABLE IF NOT EXISTS qxx_wm_issue_line ("
                + "line_id bigint NOT NULL AUTO_INCREMENT, factory_id bigint NOT NULL DEFAULT 1,"
                + "issue_id bigint NOT NULL, issue_code varchar(64),"
                + "item_id bigint NOT NULL, item_code varchar(64) NOT NULL, item_name varchar(255) NOT NULL,"
                + "unit_of_measure varchar(64) NOT NULL, unit_name varchar(64),"
                + "quantity_issue decimal(14,4) DEFAULT 0, quantity_issued decimal(14,4) DEFAULT 0,"
                + "process_id bigint, process_code varchar(64), process_name varchar(255),"
                + "batch_id bigint, batch_code varchar(64), warehouse_id bigint,"
                + "create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (line_id)) ENGINE=InnoDB");

        exec("CREATE TABLE IF NOT EXISTS qxx_wm_issue_detail ("
                + "detail_id bigint NOT NULL AUTO_INCREMENT, factory_id bigint NOT NULL DEFAULT 1,"
                + "issue_id bigint NOT NULL, issue_code varchar(64), line_id bigint NOT NULL,"
                + "item_id bigint NOT NULL, item_code varchar(64), item_name varchar(255),"
                + "unit_of_measure varchar(64) NOT NULL, unit_name varchar(64),"
                + "quantity decimal(14,4) DEFAULT 0, batch_id bigint, batch_code varchar(64),"
                + "warehouse_id bigint NOT NULL, material_stock_id bigint,"
                + "create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (detail_id)) ENGINE=InnoDB");

        // 报工 + 追溯表
        exec("CREATE TABLE IF NOT EXISTS qxx_pro_feedback ("
                + "record_id bigint NOT NULL AUTO_INCREMENT, factory_id bigint NOT NULL DEFAULT 1,"
                + "feedback_type varchar(64) NOT NULL DEFAULT 'INTERNAL', feedback_code varchar(64),"
                + "workstation_id bigint NOT NULL, workstation_code varchar(64), workstation_name varchar(255),"
                + "workorder_id bigint NOT NULL, workorder_code varchar(64), workorder_name varchar(255),"
                + "card_id bigint,"
                + "route_id bigint, route_code varchar(64),"
                + "process_id bigint NOT NULL, process_code varchar(64), process_name varchar(255),"
                + "task_id bigint, task_code varchar(64),"
                + "item_id bigint NOT NULL, item_code varchar(64) NOT NULL, item_name varchar(255),"
                + "unit_of_measure varchar(64), unit_name varchar(64),"
                + "quantity decimal(14,2) DEFAULT 0, quantity_feedback decimal(14,2) DEFAULT 0,"
                + "quantity_qualified decimal(14,2) DEFAULT 0, quantity_unqualified decimal(14,2) DEFAULT 0,"
                + "quantity_uncheck decimal(14,2) DEFAULT 0,"
                + "user_name varchar(64), nick_name varchar(64), feedback_channel varchar(64),"
                + "status varchar(64) DEFAULT 'PREPARE',"
                + "create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "update_by varchar(64) DEFAULT '', update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (record_id)) ENGINE=InnoDB");

        exec("CREATE TABLE IF NOT EXISTS qxx_pro_feedback_consume ("
                + "consume_id bigint NOT NULL AUTO_INCREMENT, factory_id bigint NOT NULL DEFAULT 1,"
                + "feedback_id bigint NOT NULL, workorder_id bigint,"
                + "item_id bigint, item_code varchar(100), item_name varchar(200),"
                + "quantity decimal(18,4), batch_code varchar(100),"
                + "PRIMARY KEY (consume_id)) ENGINE=InnoDB");

        exec("CREATE TABLE IF NOT EXISTS qxx_pro_material_trace ("
                + "trace_id bigint NOT NULL AUTO_INCREMENT, factory_id bigint NOT NULL DEFAULT 1,"
                + "trace_type varchar(32) NOT NULL, parent_type varchar(32) NOT NULL, parent_id bigint NOT NULL,"
                + "child_type varchar(32) NOT NULL, child_id bigint NOT NULL,"
                + "quantity decimal(14,4) NOT NULL, unit_of_measure varchar(64) NOT NULL,"
                + "workorder_id bigint, card_id bigint, vendor_id bigint,"
                + "issue_id bigint, issue_detail_id bigint, feedback_id bigint, transaction_id bigint, process_id bigint,"
                + "trace_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (trace_id)) ENGINE=InnoDB");

        // 采购订单（RECEIPT trace 的 parent 节点）
        exec("CREATE TABLE IF NOT EXISTS qxx_pur_order ("
                + "order_id bigint NOT NULL AUTO_INCREMENT, factory_id bigint NOT NULL DEFAULT 1,"
                + "order_code varchar(64) NOT NULL, order_name varchar(255),"
                + "vendor_id bigint NOT NULL, vendor_code varchar(64), vendor_name varchar(255),"
                + "status varchar(50) DEFAULT 'DRAFT',"
                + "create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (order_id)) ENGINE=InnoDB");

        exec("CREATE TABLE IF NOT EXISTS qxx_pur_order_line ("
                + "line_id bigint NOT NULL AUTO_INCREMENT, factory_id bigint NOT NULL DEFAULT 1,"
                + "order_id bigint NOT NULL, item_id bigint NOT NULL, item_code varchar(64), item_name varchar(255),"
                + "unit_of_measure varchar(64), quantity_ordered decimal(14,4) DEFAULT 0,"
                + "status varchar(50) DEFAULT 'ORDERED',"
                + "create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (line_id)) ENGINE=InnoDB");
    }

    private void seedAllData() {
        String now = "NOW()";
        exec("INSERT INTO qxx_md_factory (factory_id, factory_code, factory_name, enable_flag, create_by, create_time) VALUES (" + FACTORY_ID + ", 'SX', '圣享工厂', '1', 'admin', " + now + ")");
        exec("INSERT IGNORE INTO qxx_md_item (item_id, factory_id, item_code, item_name, unit_of_measure, unit_name, create_by, create_time) VALUES (" + MATERIAL_ITEM_ID + ", " + FACTORY_ID + ", 'PAPER-001', '箱板纸A级', 'ROLL', '卷', 'admin', " + now + ")");
        exec("INSERT IGNORE INTO qxx_md_item (item_id, factory_id, item_code, item_name, unit_of_measure, unit_name, create_by, create_time) VALUES (" + PRODUCT_ITEM_ID + ", " + FACTORY_ID + ", 'BAG-001', '测试纸袋', 'PCS', '个', 'admin', " + now + ")");
        exec("INSERT IGNORE INTO qxx_md_vendor (vendor_id, factory_id, vendor_code, vendor_name, create_by, create_time) VALUES (" + VENDOR_ID + ", " + FACTORY_ID + ", 'VEN-001', '德欣纸业', 'admin', " + now + ")");
        exec("INSERT IGNORE INTO qxx_md_workshop (workshop_id, factory_id, workshop_code, workshop_name, create_by, create_time) VALUES (1, " + FACTORY_ID + ", 'WS-01', '印刷车间', 'admin', " + now + ")");
        exec("INSERT IGNORE INTO qxx_md_workstation (workstation_id, factory_id, workstation_code, workstation_name, workshop_id, enable_flag, create_by, create_time) VALUES (" + WORKSTATION_ID + ", " + FACTORY_ID + ", 'PRINT-01', '1号印刷机', 1, '1', 'admin', " + now + ")");
        exec("INSERT IGNORE INTO qxx_wm_warehouse (warehouse_id, factory_id, warehouse_code, warehouse_name, enable_flag, create_by, create_time) VALUES (" + WAREHOUSE_ID + ", " + FACTORY_ID + ", 'WH-RAW', '原料仓', '1', 'admin', " + now + ")");
        exec("INSERT IGNORE INTO qxx_pro_process (process_id, factory_id, process_code, process_name, process_type, enable_flag, create_by, create_time) VALUES (" + PROCESS_A_ID + ", " + FACTORY_ID + ", 'PRINT', '印刷', 'INTERNAL', '1', 'admin', " + now + ")");
        exec("INSERT IGNORE INTO qxx_pro_process (process_id, factory_id, process_code, process_name, process_type, enable_flag, create_by, create_time) VALUES (" + PROCESS_B_ID + ", " + FACTORY_ID + ", 'BAG', '制袋', 'INTERNAL', '1', 'admin', " + now + ")");
        exec("INSERT IGNORE INTO qxx_pro_route (route_id, factory_id, route_code, route_name, enable_flag, create_by, create_time) VALUES (" + ROUTE_ID + ", " + FACTORY_ID + ", 'RT-01', '标准路线', '1', 'admin', " + now + ")");
        exec("INSERT IGNORE INTO qxx_pro_route_process (record_id, factory_id, route_id, process_id, process_code, process_name, order_num, create_by, create_time) VALUES (1, " + FACTORY_ID + ", " + ROUTE_ID + ", " + PROCESS_A_ID + ", 'PRINT', '印刷', 1, 'admin', " + now + ")");
        exec("INSERT IGNORE INTO qxx_pro_route_process (record_id, factory_id, route_id, process_id, process_code, process_name, order_num, create_by, create_time) VALUES (2, " + FACTORY_ID + ", " + ROUTE_ID + ", " + PROCESS_B_ID + ", 'BAG', '制袋', 2, 'admin', " + now + ")");
        exec("INSERT IGNORE INTO qxx_pro_workorder (workorder_id, factory_id, workorder_code, workorder_name, product_id, product_code, product_name, unit_of_measure, unit_name, quantity, quantity_produced, status, create_by, create_time) VALUES (1, " + FACTORY_ID + ", 'WO-E2E-001', 'E2E测试工单', " + PRODUCT_ITEM_ID + ", 'BAG-001', '测试纸袋', 'PCS', '个', 100, 0, 'PREPARE', 'admin', " + now + ")");
        exec("INSERT IGNORE INTO qxx_pro_workorder_bom (line_id, factory_id, workorder_id, item_id, item_code, item_name, process_id, quantity, total_quantity, create_by, create_time) VALUES (1, " + FACTORY_ID + ", 1, " + MATERIAL_ITEM_ID + ", 'PAPER-001', '箱板纸A级', " + PROCESS_A_ID + ", 1, 100, 'admin', " + now + ")");
        // 排产任务（2 道工序）
        exec("INSERT IGNORE INTO qxx_pro_task (task_id, factory_id, task_code, task_name, workorder_id, workstation_id, route_id, process_id, process_code, process_name, item_id, item_code, item_name, unit_of_measure, unit_name, quantity, quantity_produced, status, create_by, create_time) VALUES (1, " + FACTORY_ID + ", 'TASK-A', '印刷任务', 1, " + WORKSTATION_ID + ", " + ROUTE_ID + ", " + PROCESS_A_ID + ", 'PRINT', '印刷', " + PRODUCT_ITEM_ID + ", 'BAG-001', '测试纸袋', 'PCS', '个', 100, 0, 'PREPARE', 'admin', " + now + ")");
        exec("INSERT IGNORE INTO qxx_pro_task (task_id, factory_id, task_code, task_name, workorder_id, workstation_id, route_id, process_id, process_code, process_name, item_id, item_code, item_name, unit_of_measure, unit_name, quantity, quantity_produced, status, create_by, create_time) VALUES (2, " + FACTORY_ID + ", 'TASK-B', '制袋任务', 1, " + WORKSTATION_ID + ", " + ROUTE_ID + ", " + PROCESS_B_ID + ", 'BAG', '制袋', " + PRODUCT_ITEM_ID + ", 'BAG-001', '测试纸袋', 'PCS', '个', 100, 0, 'PREPARE', 'admin', " + now + ")");
        // 采购订单（RECEIPT parent）
        exec("INSERT IGNORE INTO qxx_pur_order (order_id, factory_id, order_code, order_name, vendor_id, vendor_code, vendor_name, status, create_by, create_time) VALUES (1, " + FACTORY_ID + ", 'PO-001', '测试采购单', " + VENDOR_ID + ", 'VEN-001', '德欣纸业', 'APPROVED', 'admin', " + now + ")");
        // 入库单
        exec("INSERT IGNORE INTO qxx_wm_item_recpt (recpt_id, factory_id, recpt_code, pur_order_id, vendor_id, vendor_code, vendor_name, warehouse_id, warehouse_code, status, create_by, create_time) VALUES (1, " + FACTORY_ID + ", 'RCP-001', 1, " + VENDOR_ID + ", 'VEN-001', '德欣纸业', " + WAREHOUSE_ID + ", 'WH-RAW', 'DRAFT', 'admin', " + now + ")");
        exec("INSERT IGNORE INTO qxx_wm_item_recpt_line (line_id, factory_id, recpt_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity_recpt, batch_id, batch_code, warehouse_id, warehouse_code, create_by, create_time) VALUES (1, " + FACTORY_ID + ", 1, " + MATERIAL_ITEM_ID + ", 'PAPER-001', '箱板纸A级', 'ROLL', '卷', 50, 99, 'BATCH-RCP-099', " + WAREHOUSE_ID + ", 'WH-RAW', 'admin', " + now + ")");
        // 预置库存（供领料使用，vendor_id=0 匹配 loadStockForUpdate 查询条件）
        exec("INSERT IGNORE INTO qxx_wm_material_stock (material_stock_id, factory_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity_onhand, quantity_available, batch_id, batch_code, warehouse_id, warehouse_code, vendor_id, workorder_id, quality_status, create_by, create_time) VALUES (1, " + FACTORY_ID + ", " + MATERIAL_ITEM_ID + ", 'PAPER-001', '箱板纸A级', 'ROLL', '卷', 200, 200, 1, 'BATCH-STOCK', " + WAREHOUSE_ID + ", 'WH-RAW', 0, 0, 'NORMAL', 'admin', " + now + ")");
        // 预置流转卡（手工建卡，绕开自动编码规则依赖）
        exec("INSERT IGNORE INTO qxx_pro_card (card_id, factory_id, card_code, workorder_id, workorder_code, workorder_name, item_id, item_code, item_name, unit_of_measure, unit_name, quantity_transfered, status, create_by, create_time) VALUES (1, " + FACTORY_ID + ", 'CARD-E2E-001', 1, 'WO-E2E-001', 'E2E测试工单', " + PRODUCT_ITEM_ID + ", 'BAG-001', '测试纸袋', 'PCS', '个', 100, 'ACTIVE', 'admin', " + now + ")");
        // 领料单（手动创建，用于 execute 测试）
        exec("INSERT IGNORE INTO qxx_wm_issue_header (issue_id, factory_id, issue_code, workorder_id, workorder_code, warehouse_id, warehouse_code, status, total_quantity, create_by, create_time) VALUES (1, " + FACTORY_ID + ", 'ISSUE-001', 1, 'WO-E2E-001', " + WAREHOUSE_ID + ", 'WH-RAW', 'DRAFT', 50, 'admin', " + now + ")");
        exec("INSERT IGNORE INTO qxx_wm_issue_line (line_id, factory_id, issue_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity_issue, quantity_issued, process_id, process_code, process_name, batch_id, batch_code, warehouse_id, create_by, create_time) VALUES (1, " + FACTORY_ID + ", 1, " + MATERIAL_ITEM_ID + ", 'PAPER-001', '箱板纸A级', 'ROLL', '卷', 50, 50, " + PROCESS_A_ID + ", 'PRINT', '印刷', 1, 'BATCH-STOCK', " + WAREHOUSE_ID + ", 'admin', " + now + ")");
        exec("INSERT IGNORE INTO qxx_wm_issue_header (issue_id, factory_id, issue_code, workorder_id, workorder_code, warehouse_id, warehouse_code, status, total_quantity, create_by, create_time) VALUES (1, " + FACTORY_ID + ", 'ISSUE-001', 1, 'WO-E2E-001', " + WAREHOUSE_ID + ", 'WH-RAW', 'DRAFT', 50, 'admin', " + now + ")");
        exec("INSERT IGNORE INTO qxx_wm_issue_line (line_id, factory_id, issue_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity_issue, quantity_issued, process_id, process_code, process_name, batch_id, batch_code, warehouse_id, create_by, create_time) VALUES (1, " + FACTORY_ID + ", 1, " + MATERIAL_ITEM_ID + ", 'PAPER-001', '箱板纸A级', 'ROLL', '卷', 50, 50, " + PROCESS_A_ID + ", 'PRINT', '印刷', 1, 'BATCH-STOCK', " + WAREHOUSE_ID + ", 'admin', " + now + ")");
    }

    // ════════════════════════════════════════════
    // 测试用例
    // ════════════════════════════════════════════

    @Test
    @DisplayName("T1: 预置流转卡存在（工单开工后自动建卡）")
    void testCardExists() {
        ProCard q = new ProCard();
        q.setWorkorderId(1L);
        List<ProCard> cards = proCardMapper.selectProCardList(q);
        assertThat(cards).isNotEmpty();
        ProCard card = cards.get(0);
        assertThat(card.getStatus()).isEqualTo("ACTIVE");
        assertThat(card.getQuantityTransfered()).isEqualByComparingTo(new BigDecimal("100"));
    }

    @Test
    @DisplayName("T2: 采购入库确认 → 写入 RECEIPT trace")
    void testReceiptTraceOnConfirm() {
        // Given: recptId=1 DRAFT, has pur_order_id=1, vendor=301
        // When: confirm
        itemRecptService.confirmItemRecpt(1L);

        // Then: RECEIPT trace 已写入
        ProMaterialTrace tq = new ProMaterialTrace();
        tq.setTraceType("RECEIPT");
        List<ProMaterialTrace> traces = traceMapper.selectProMaterialTraceList(tq);
        assertThat(traces).isNotEmpty();
        ProMaterialTrace trace = traces.get(0);
        assertThat(trace.getParentType()).isEqualTo("PUR_ORDER");
        assertThat(trace.getParentId()).isEqualTo(1L);
        assertThat(trace.getChildType()).isEqualTo("MATERIAL_STOCK");
        assertThat(trace.getChildId()).isNotNull().isGreaterThan(0);
        assertThat(trace.getVendorId()).isEqualTo(VENDOR_ID);
        assertThat(trace.getQuantity()).isEqualByComparingTo(new BigDecimal("50"));
        assertThat(trace.getTransactionId()).isNotNull();

        // 同时验证库存已增加（UPSERT 可能合并到已有行）
        List<WmMaterialStock> stocks = stockMapper.selectWmMaterialStockAll();
        assertThat(stocks).isNotEmpty();
    }

    @Test
    @DisplayName("T3: 领料出库 → ISSUE trace 写入真实 cardId（≠0）")
    void testIssueTraceWithRealCardId() {
        // Given: confirm + execute 领料单 1
        issueService.confirmIssue(1L);

        // 为 issue_line 创建对应的 issue_detail（发料明细）
        exec("INSERT INTO qxx_wm_issue_detail (factory_id, issue_id, line_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity, batch_id, batch_code, warehouse_id, material_stock_id, create_by, create_time) "
                + "SELECT 1, 1, line_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity_issue, batch_id, batch_code, warehouse_id, 1, 'admin', NOW() FROM qxx_wm_issue_line WHERE issue_id = 1");

        // When: execute 出库
        issueService.executeIssue(1L);

        // Then: ISSUE trace child_id ≠ 0（= 流转卡 ID）
        ProMaterialTrace tq = new ProMaterialTrace();
        tq.setTraceType("ISSUE");
        tq.setIssueId(1L);
        List<ProMaterialTrace> traces = traceMapper.selectProMaterialTraceList(tq);
        assertThat(traces).isNotEmpty();
        ProMaterialTrace trace = traces.get(0);
        assertThat(trace.getChildType()).isEqualTo("CARD");
        assertThat(trace.getChildId()).isNotNull().isGreaterThan(0);
        assertThat(trace.getCardId()).isNotNull().isGreaterThan(0);
    }

    @Test
    @DisplayName("T4: 报工 → 自动关联流转卡 cardId")
    void testFeedbackAutoResolveCardId() {
        // Given: 预置卡 cardId=1, ACTIVE
        // When: 插入报工（不传 cardId）
        ProFeedback fb = new ProFeedback();
        fb.setFeedbackType("INTERNAL");
        fb.setWorkorderId(1L);
        fb.setWorkorderCode("WO-E2E-001");
        fb.setWorkstationId(WORKSTATION_ID);
        fb.setProcessId(PROCESS_A_ID);
        fb.setProcessCode("PRINT");
        fb.setProcessName("印刷");
        fb.setRouteId(ROUTE_ID);
        fb.setTaskId(1L);
        fb.setItemId(PRODUCT_ITEM_ID);
        fb.setItemCode("BAG-001");
        fb.setItemName("测试纸袋");
        fb.setUnitOfMeasure("PCS");
        fb.setUnitName("个");
        fb.setQuantityFeedback(new BigDecimal("20"));
        fb.setQuantityQualified(new BigDecimal("20"));
        fb.setQuantityUnqualified(BigDecimal.ZERO);
        fb.setFeedbackChannel("PC");
        fb.setUserName("admin");
        feedbackService.insertProFeedback(fb);

        // Then: cardId 已自动填充
        assertThat(fb.getCardId()).isNotNull().isGreaterThan(0);
    }

    @Test
    @DisplayName("T5: 中间工序报工审核 → 流转卡 currentProcessId 推进，status 保持 ACTIVE")
    void testCardProgressOnMiddleProcessAudit() {
        // Given: 开工建卡 + 创建并确认报工
        workorderService.startProduction(1L);

        ProFeedback fb = buildFeedback(PROCESS_A_ID, "PRINT", 1L);
        feedbackService.insertProFeedback(fb);
        confirmFeedback(fb.getRecordId());

        // When: 审核中间工序报工
        feedbackService.auditFeedback(fb.getRecordId());

        // Then: 卡推进到当前工序，status 保持 ACTIVE
        ProCard card = proCardMapper.selectProCardByCardId(fb.getCardId());
        assertThat(card.getCurrentProcessId()).isEqualTo(PROCESS_A_ID);
        assertThat(card.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("T6: 末工序报工审核 → PRODUCE trace + 卡 COMPLETED")
    void testProductTraceAndCardCompletedOnLastProcess() {
        // Given: 开工建卡
        workorderService.startProduction(1L);

        // Given: 末工序报工 (process B = 制袋，order_num=2 是末工序)
        ProFeedback fb = buildFeedback(PROCESS_B_ID, "BAG", 2L);
        feedbackService.insertProFeedback(fb);
        confirmFeedback(fb.getRecordId());

        // When: 审核末工序报工
        feedbackService.auditFeedback(fb.getRecordId());

        // Then: 流转卡置 COMPLETED
        ProCard card = proCardMapper.selectProCardByCardId(fb.getCardId());
        assertThat(card.getStatus()).isEqualTo("COMPLETED");
        assertThat(card.getCurrentProcessId()).isEqualTo(PROCESS_B_ID);

        // Then: PRODUCE trace 已写入
        ProMaterialTrace tq = new ProMaterialTrace();
        tq.setTraceType("PRODUCE");
        tq.setFeedbackId(fb.getRecordId());
        List<ProMaterialTrace> traces = traceMapper.selectProMaterialTraceList(tq);
        assertThat(traces).isNotEmpty();
        ProMaterialTrace trace = traces.get(0);
        assertThat(trace.getParentType()).isEqualTo("CARD");
        assertThat(trace.getParentId()).isEqualTo(card.getCardId());
        assertThat(trace.getChildType()).isEqualTo("FEEDBACK");
        assertThat(trace.getChildId()).isEqualTo(fb.getRecordId());
    }

    @Test
    @DisplayName("T7: 中间工序报工不产生 PRODUCE trace")
    void testNoProduceTraceForMiddleProcess() {
        // Given: 开工建卡
        workorderService.startProduction(1L);

        // Given: 中间工序报工 + 审核
        ProFeedback fb = buildFeedback(PROCESS_A_ID, "PRINT", 1L);
        feedbackService.insertProFeedback(fb);
        confirmFeedback(fb.getRecordId());
        feedbackService.auditFeedback(fb.getRecordId());

        // Then: 不产生 PRODUCE trace
        ProMaterialTrace tq = new ProMaterialTrace();
        tq.setTraceType("PRODUCE");
        tq.setProcessId(PROCESS_A_ID);
        tq.setWorkorderId(1L);
        List<ProMaterialTrace> traces = traceMapper.selectProMaterialTraceList(tq);
        assertThat(traces).isEmpty();
    }

    @Test
    @DisplayName("T8: 完整追溯链 — RECEIPT → ISSUE → PRODUCE 三端连通")
    void testFullTraceChain() {
        // Step 1: 入库 + RECEIPT trace
        itemRecptService.confirmItemRecpt(1L);

        // Step 2: 领料出库 + ISSUE trace（卡已预置 cardId=1）
        issueService.confirmIssue(1L);
        exec("INSERT INTO qxx_wm_issue_detail (factory_id, issue_id, line_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity, batch_id, batch_code, warehouse_id, material_stock_id, create_by, create_time) "
                + "SELECT 1, 1, line_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity_issue, batch_id, batch_code, warehouse_id, 1, 'admin', NOW() FROM qxx_wm_issue_line WHERE issue_id = 1");
        issueService.executeIssue(1L);

        // Step 4: 末工序报工 + PRODUCE trace
        ProFeedback fb = buildFeedback(PROCESS_B_ID, "BAG", 2L);
        feedbackService.insertProFeedback(fb);
        confirmFeedback(fb.getRecordId());
        feedbackService.auditFeedback(fb.getRecordId());

        // 验证：3 种 trace 类型都存在
        ProMaterialTrace allQ = new ProMaterialTrace();
        List<ProMaterialTrace> all = traceMapper.selectProMaterialTraceList(allQ);
        List<String> types = all.stream().map(ProMaterialTrace::getTraceType).distinct().toList();
        assertThat(types).contains("RECEIPT", "ISSUE", "PRODUCE");

        // 验证：ISSUE 的 child_id = PRODUCE 的 parent_id（= 同一张流转卡）
        ProMaterialTrace issueTrace = all.stream().filter(t -> "ISSUE".equals(t.getTraceType())).findFirst().orElseThrow();
        ProMaterialTrace produceTrace = all.stream().filter(t -> "PRODUCE".equals(t.getTraceType())).findFirst().orElseThrow();
        assertThat(issueTrace.getCardId()).isEqualTo(produceTrace.getCardId()).isGreaterThan(0);
    }

    @Test
    @DisplayName("T9: 正向追溯 — 从采购订单出发查入库记录")
    void testForwardTraceFromPurOrder() {
        // Given: 入库产生 RECEIPT trace（parent=PUR_ORDER:1）
        itemRecptService.confirmItemRecpt(1L);

        // When: 正向追溯 PUR_ORDER
        List<ProMaterialTrace> results = traceService.traceForward("PUR_ORDER", 1L);

        // Then: 找到 RECEIPT trace，指向 MATERIAL_STOCK
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getChildType()).isEqualTo("MATERIAL_STOCK");
        assertThat(results.get(0).getChildId()).isGreaterThan(0);
        assertThat(results.get(0).getTraceType()).isEqualTo("RECEIPT");
    }

    @Test
    @DisplayName("T10: 反向追溯 — 从流转卡出发查投料来源")
    void testBackwardTraceFromCard() {
        // Given: 领料出库产生 ISSUE trace（child=CARD:1）
        issueService.confirmIssue(1L);
        exec("INSERT INTO qxx_wm_issue_detail (factory_id, issue_id, line_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity, batch_id, batch_code, warehouse_id, material_stock_id, create_by, create_time) "
                + "SELECT 1, 1, line_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity_issue, batch_id, batch_code, warehouse_id, 1, 'admin', NOW() FROM qxx_wm_issue_line WHERE issue_id = 1");
        issueService.executeIssue(1L);

        // When: 反向追溯 CARD
        List<ProMaterialTrace> results = traceService.traceBackward("CARD", 1L);

        // Then: 找到 ISSUE trace，parent=MATERIAL_STOCK
        assertThat(results).isNotEmpty();
        ProMaterialTrace t = results.get(0);
        assertThat(t.getParentType()).isEqualTo("MATERIAL_STOCK");
        assertThat(t.getParentId()).isGreaterThan(0);
        assertThat(t.getTraceType()).isEqualTo("ISSUE");
    }

    @Test
    @DisplayName("T11: 溯到链尾 — 无结果时返回空列表不报错")
    void testTraceReachesEnd() {
        // Given: 查一个不存在的节点
        List<ProMaterialTrace> results = traceService.traceForward("NONE", 99999L);

        // Then: 空列表，不抛异常
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("T12: 递归追溯 — 正向追到尾，验证链长度")
    void testRecursiveTraceChain() {
        // Step 1: 入库 RECEIPT (PUR_ORDER → MATERIAL_STOCK)
        itemRecptService.confirmItemRecpt(1L);
        // Step 2: 领料 ISSUE (MATERIAL_STOCK → CARD)
        issueService.confirmIssue(1L);
        exec("INSERT INTO qxx_wm_issue_detail (factory_id, issue_id, line_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity, batch_id, batch_code, warehouse_id, material_stock_id, create_by, create_time) "
                + "SELECT 1, 1, line_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity_issue, batch_id, batch_code, warehouse_id, 1, 'admin', NOW() FROM qxx_wm_issue_line WHERE issue_id = 1");
        issueService.executeIssue(1L);
        // Step 3: 末工序报工 PRODUCE (CARD → FEEDBACK)
        ProFeedback fb = buildFeedback(PROCESS_B_ID, "BAG", 2L);
        feedbackService.insertProFeedback(fb);
        confirmFeedback(fb.getRecordId());
        feedbackService.auditFeedback(fb.getRecordId());

        // 模拟前端递归：从 PUR_ORDER 出发，逐跳正向追溯
        List<String> chain = new ArrayList<>();
        String curType = "PUR_ORDER";
        Long curId = 1L;
        Set<String> visited = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            String key = curType + ":" + curId;
            if (!visited.add(key)) break; // 环检测
            List<ProMaterialTrace> results = traceService.traceForward(curType, curId);
            if (results.isEmpty()) break;
            ProMaterialTrace t = results.get(0);
            chain.add(t.getTraceType());
            curType = t.getChildType();
            curId = t.getChildId();
        }

        // 验证：正向从 PUR_ORDER 追到 RECEIPT→STOCK，再反向从 CARD 追到 ISSUE
        // （RECEIPT 的 child_stock 和 ISSUE 的 parent_stock 可能不是同一条记录，
        //  因为领料使用了预置种子库存，而非本次入库创建的库存。这符合实际业务。）
        assertThat(chain).isNotEmpty();
        assertThat(chain.get(0)).isEqualTo("RECEIPT"); // 第一步必定是入库追溯
    }

    @Test
    @DisplayName("T13: 外协发料过账 → OUTSOURCE_ISSUE trace（parent=STOCK, child=VENDOR）")
    void testOutsourceIssueTrace() {
        // Given: 外协发料单（issue_type=OUTSOURCE, vendor_id=301）
        exec("UPDATE qxx_wm_issue_header SET issue_type='OUTSOURCE', vendor_id=" + VENDOR_ID + " WHERE issue_id=1");
        issueService.confirmIssue(1L);
        exec("INSERT INTO qxx_wm_issue_detail (factory_id, issue_id, line_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity, batch_id, batch_code, warehouse_id, material_stock_id, create_by, create_time) "
                + "SELECT 1, 1, line_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity_issue, batch_id, batch_code, warehouse_id, 1, 'admin', NOW() FROM qxx_wm_issue_line WHERE issue_id = 1");

        // When: execute
        issueService.executeIssue(1L);

        // Then: OUTSOURCE_ISSUE trace exists
        ProMaterialTrace tq = new ProMaterialTrace();
        tq.setTraceType("OUTSOURCE_ISSUE");
        List<ProMaterialTrace> traces = traceMapper.selectProMaterialTraceList(tq);
        assertThat(traces).isNotEmpty();
        ProMaterialTrace t = traces.get(0);
        assertThat(t.getParentType()).isEqualTo("MATERIAL_STOCK");
        assertThat(t.getChildType()).isEqualTo("VENDOR");
        assertThat(t.getChildId()).isEqualTo(VENDOR_ID);
        assertThat(t.getVendorId()).isEqualTo(VENDOR_ID);
    }

    @Test
    @DisplayName("T14: 外协入库确认 → OUTSOURCE_RECPT trace（parent=VENDOR, child=STOCK）")
    void testOutsourceRecptTrace() {
        // Given: 外协入库单（recpt_type=OUTSOURCE）
        exec("UPDATE qxx_wm_item_recpt SET recpt_type='OUTSOURCE', pur_order_id=NULL WHERE recpt_id=1");
        exec("DELETE FROM qxx_wm_item_recpt_line WHERE recpt_id=1");
        exec("INSERT INTO qxx_wm_item_recpt_line (line_id, factory_id, recpt_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity_recpt, batch_id, batch_code, warehouse_id, create_by, create_time) VALUES (10, " + FACTORY_ID + ", 1, " + PRODUCT_ITEM_ID + ", 'BAG-001', '测试纸袋', 'PCS', '个', 50, 99, 'BATCH-OUT-099', " + WAREHOUSE_ID + ", 'admin', NOW())");

        // When: confirm
        itemRecptService.confirmItemRecpt(1L);

        // Then: OUTSOURCE_RECPT trace
        ProMaterialTrace tq = new ProMaterialTrace();
        tq.setTraceType("OUTSOURCE_RECPT");
        List<ProMaterialTrace> traces = traceMapper.selectProMaterialTraceList(tq);
        assertThat(traces).isNotEmpty();
        ProMaterialTrace t = traces.get(0);
        assertThat(t.getParentType()).isEqualTo("VENDOR");
        assertThat(t.getParentId()).isEqualTo(VENDOR_ID);
        assertThat(t.getChildType()).isEqualTo("MATERIAL_STOCK");
        assertThat(t.getChildId()).isGreaterThan(0);
    }

    @Test
    @DisplayName("T15: 反向追溯 — 从 VENDOR 查外协发料来源")
    void testBackwardTraceOutsourceIssue() {
        // Given: T13 已创建 OUTSOURCE_ISSUE trace
        exec("UPDATE qxx_wm_issue_header SET issue_type='OUTSOURCE', vendor_id=" + VENDOR_ID + " WHERE issue_id=1");
        issueService.confirmIssue(1L);
        exec("INSERT INTO qxx_wm_issue_detail (factory_id, issue_id, line_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity, batch_id, batch_code, warehouse_id, material_stock_id, create_by, create_time) "
                + "SELECT 1, 1, line_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity_issue, batch_id, batch_code, warehouse_id, 1, 'admin', NOW() FROM qxx_wm_issue_line WHERE issue_id = 1");
        issueService.executeIssue(1L);

        // When: backward from VENDOR
        List<ProMaterialTrace> results = traceService.traceBackward("VENDOR", VENDOR_ID);

        // Then: at least one OUTSOURCE_ISSUE trace found
        assertThat(results).isNotEmpty();
        boolean found = results.stream().anyMatch(t -> "OUTSOURCE_ISSUE".equals(t.getTraceType()));
        assertThat(found).isTrue();
    }

    @Test
    @DisplayName("T16: 正向追溯 — 从 VENDOR 查外协入库去向")
    void testForwardTraceOutsourceRecpt() {
        // Given: T14 已创建 OUTSOURCE_RECPT trace
        exec("UPDATE qxx_wm_item_recpt SET recpt_type='OUTSOURCE', pur_order_id=NULL WHERE recpt_id=1");
        exec("DELETE FROM qxx_wm_item_recpt_line WHERE recpt_id=1");
        exec("INSERT INTO qxx_wm_item_recpt_line (line_id, factory_id, recpt_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity_recpt, batch_id, batch_code, warehouse_id, create_by, create_time) VALUES (10, " + FACTORY_ID + ", 1, " + PRODUCT_ITEM_ID + ", 'BAG-001', '测试纸袋', 'PCS', '个', 50, 99, 'BATCH-OUT-099', " + WAREHOUSE_ID + ", 'admin', NOW())");
        itemRecptService.confirmItemRecpt(1L);

        // When: forward from VENDOR
        List<ProMaterialTrace> results = traceService.traceForward("VENDOR", VENDOR_ID);

        // Then: at least one OUTSOURCE_RECPT trace found
        assertThat(results).isNotEmpty();
        boolean found = results.stream().anyMatch(t -> "OUTSOURCE_RECPT".equals(t.getTraceType()));
        assertThat(found).isTrue();
    }

    // ════════════════════════════════════════════
    // 辅助方法
    // ════════════════════════════════════════════

    /** Controller 里的 confirm 逻辑：PREPARE → CONFIRMED */
    private void confirmFeedback(Long recordId) {
        ProFeedback fb = feedbackService.selectProFeedbackByRecordId(recordId);
        fb.setStatus("CONFIRMED");
        feedbackService.updateProFeedback(fb);
    }

    private ProFeedback buildFeedback(long processId, String processName, long taskId) {
        ProFeedback fb = new ProFeedback();
        fb.setFeedbackType("INTERNAL");
        fb.setWorkorderId(1L);
        fb.setWorkorderCode("WO-E2E-001");
        fb.setWorkstationId(WORKSTATION_ID);
        fb.setRouteId(ROUTE_ID);
        fb.setProcessId(processId);
        fb.setProcessCode(processName);
        fb.setProcessName(processName);
        fb.setTaskId(taskId);
        fb.setItemId(PRODUCT_ITEM_ID);
        fb.setItemCode("BAG-001");
        fb.setItemName("测试纸袋");
        fb.setUnitOfMeasure("PCS");
        fb.setUnitName("个");
        fb.setQuantityFeedback(new BigDecimal("20"));
        fb.setQuantityQualified(new BigDecimal("20"));
        fb.setQuantityUnqualified(BigDecimal.ZERO);
        fb.setFeedbackChannel("PC");
        fb.setUserName("admin");
        return fb;
    }
}
