# Qcadoo MES 架构分析报告

> 信息来源：项目源码 `/Users/huangwenhua/company/self/mes/`（Qcadoo MES，AGPLv3）
>
> 编写日期：2026-06-06
>
> 目的：为 qixiaoxia-mes（企小侠文化纸盒MES）的架构设计提供参考

---

## 一、项目概览

Qcadoo MES 是基于 **Qcadoo Framework**（闭源插件框架）的制造执行系统，2010 年启动，社区版以 AGPLv3 开源。

| 维度 | Qcadoo MES | ktg-mes | qixiaoxia-mes |
|------|-----------|---------|---------------|
| 框架 | Qcadoo Framework (Spring 3 + AspectJ + Tiles) | RuoYi (Spring Boot 2.5 + MyBatis) | RuoYi-Vue (Spring Boot 4.x + MyBatis) |
| 前端 | JSP + jQuery/SlickGrid | Vue 2.6 + Element UI | Vue 3 + TypeScript + Element Plus |
| 模块化 | 插件化（~50 插件，自定义 packaging） | Maven 多模块 + 包名分层 | Maven 多模块 |
| 数据库 | PostgreSQL + Hibernate | MySQL 8.0 + MyBatis | MySQL 8.0 + MyBatis |
| ORM | XML 声明式 → 框架自动生成 | 手写 SQL + XML Mapper | 手写 SQL + XML Mapper |
| 测试 | 无 | 无 | ✅ 有测试约定 |

⚠️ **核心差异**：Qcadoo 的插件框架是**闭源**的，本文分析的是其**架构思想**，而非框架本身。qixiaoxia-mes 可以用 Spring Boot 生态中的等价方案落地。

---

## 二、三大架构思想

### 2.1 模块隔离：插件化架构

Qcadoo 通过 **三层防线** 实现真正的模块隔离：

**第一层：Maven 模块物理隔离**

```
mes/
├── pom.xml                      # 父 POM，无业务代码
├── mes-plugins/pom.xml          # 聚合器，声明 50 个子模块
│   ├── mes-plugins-basic/       # 独立 Maven 模块 (packaging: qcadoo-plugin)
│   ├── mes-plugins-orders/
│   ├── mes-plugins-technologies/
│   └── ...(50个)
└── mes-application/             # WAR 组装器
```

**第二层：`qcadoo-plugin.xml` — 插件元数据契约**

每个插件通过此文件声明身份、依赖、数据模型、视图、菜单、安全角色。框架启动时按依赖拓扑排序加载。

```xml
<plugin plugin="orders" group="planning" version="1.5">
    <dependencies>
        <dependency><plugin>timeNormsForOperations</plugin><version>[1.4.0</version></dependency>
    </dependencies>
    <modules>
        <model:model model="order" resource="model/order.xml"/>
        <model:model model="orderStateChange" resource="model/orderStateChange.xml"/>
        ...
        <menu:menu-item name="orders" category="planning" view="ordersList"/>
        <view:view resource="view/ordersList.xml"/>
        <custom:custom class="...OrderStateServiceRegisterModule"/>
    </modules>
</plugin>
```

**第三层：跨插件模型扩展（`model:model-field`）**

关键文件：`mes-plugins-orders/src/main/resources/qcadoo-plugin.xml:122-149`

插件 A 可以在不修改插件 B 源码的情况下，扩展 B 的数据模型：

```xml
<!-- orders 插件扩展 basic 插件的 product 模型 -->
<model:model-field plugin="basic" model="product">
    <model:hasMany name="orders" plugin="orders" model="order" joinField="product"/>
</model:model-field>

<!-- orders 插件扩展 basic 插件的 parameter(系统参数) 模型 -->
<model:model-field plugin="basic" model="parameter">
    <model:boolean name="allowQuantityChangeInAcceptedOrder" default="false"/>
</model:model-field>
```

框架自动执行：
- `ALTER TABLE basic_product ADD COLUMN ...` （对应字段）
- ORM 映射注入（product.orders 关联可查）
- 校验规则注入
- UI 组件注入（如通过 `view:view-hook`）

**qixiaoxia-mes 落地方案**：

```
qixiaoxia-mes/backend/
├── qixiaoxia-common/              # 共享层 (Entity基类、事件总线)
├── qixiaoxia-modules/
│   ├── qixiaoxia-md/              # 基础数据模块
│   ├── qixiaoxia-wm/              # 仓储模块
│   ├── qixiaoxia-pro/             # 生产模块
│   ├── qixiaoxia-qc/              # 质检模块
│   └── qixiaoxia-report/          # 报表模块
└── qixiaoxia-admin/               # 启动器
```

用 Spring Event 实现跨模块通信：

```java
// wm 模块：入库完成时发布事件
@Transactional
public void completeItemRecpt(Long recptId) {
    eventPublisher.publishEvent(new ItemRecptCompletedEvent(this, recptId, items));
}

// qc 模块：监听入库事件，自动创建检验任务（wm 模块完全不知情）
@EventListener
public void onItemRecptCompleted(ItemRecptCompletedEvent event) {
    createInspectionTask(event.getRecptId(), event.getItems());
}
```

---

### 2.2 状态机：states 插件

#### 核心接口

关键文件：`mes-plugins-states/src/main/java/com/qcadoo/mes/states/`

```
StateEnum              ← 状态枚举接口（canChangeTo + getStringValue）
StateChangeContext     ← 转换上下文（持有当前实体、状态、校验消息）
StateChangeEntityDescriber ← 状态变更实体的元数据描述
StateChangeService     ← 执行状态转换
StateChangeServiceResolver ← 路由到对应的 Service
StateChangeViewClient  ← UI 层触发转换
AbstractStateServiceRegisterModule ← 插件启动时注册
```

#### 状态转换规则 —— 枚举编码

关键文件：`mes-plugins-orders/src/main/java/com/qcadoo/mes/orders/states/constants/OrderState.java:31-81`

```java
public enum OrderState implements StateEnum {
    PENDING("01pending") {
        public boolean canChangeTo(StateEnum target) {
            return ACCEPTED.equals(target)
                || IN_PROGRESS.equals(target)
                || DECLINED.equals(target);
        }
    },
    ACCEPTED("02accepted") {
        public boolean canChangeTo(StateEnum target) {
            return IN_PROGRESS.equals(target)
                || DECLINED.equals(target);
        }
    },
    IN_PROGRESS("03inProgress") {
        public boolean canChangeTo(StateEnum target) {
            return COMPLETED.equals(target)
                || INTERRUPTED.equals(target)
                || ABANDONED.equals(target);
        }
    },
    COMPLETED("04completed") {
        public boolean canChangeTo(StateEnum target) { return false; }  // 终态
    },
    // ...
}
```

**状态转换图**：

```
              ┌─────────┐
              │ PENDING │
              └────┬────┘
         ┌─────────┼─────────┐
         ▼         ▼         ▼
    ┌────────┐ ┌──────────┐ ┌──────────┐
    │ACCEPTED│ │IN_PROGRESS│ │ DECLINED │ 终态
    └───┬────┘ └────┬─────┘ └──────────┘
        │         ┌──┴───────┬──────────┐
        └───────→─┤COMPLETED?│INTERRUPTED│ABANDONED?
                  └──────────┘─────┬─────┘──┬─────────┘
                    终态       ┌───┴───┐    终态
                              │→ 恢复  │
                              │→ 废弃  │
                              └───────┘
```

#### 状态变更审计

每次状态转换都记录为一条 `XxxStateChange` 实体：

关键文件：`mes-plugins-orders/src/main/resources/orders/model/orderStateChange.xml`

```xml
<model name="orderStateChange">
    <fields>
        <datetime name="dateAndTime" />
        <enum name="sourceState" values="01pending,02accepted,..."/>
        <enum name="targetState" values="..." required="true"/>
        <enum name="status" values="01inProgress,02paused,03successful,04failure,05canceled"/>
        <integer name="phase" />
        <string name="worker" />
        <belongsTo name="shift" model="shift" plugin="basic" />
        <hasMany name="messages" joinField="orderStateChange" model="message" plugin="states" />
        <belongsTo name="order" model="order" />
        <string name="comment" />
    </fields>
</model>
```

#### qixiaoxia-mes 落地方案

**状态枚举定义**：

```java
public enum WorkOrderState {
    PENDING {
        public boolean canChangeTo(WorkOrderState t) { return t == RELEASED || t == CANCELLED; }
    },
    RELEASED {
        public boolean canChangeTo(WorkOrderState t) { return t == IN_PROGRESS || t == CANCELLED; }
    },
    IN_PROGRESS {
        public boolean canChangeTo(WorkOrderState t) { return t == COMPLETED || t == INTERRUPTED; }
    },
    COMPLETED {
        public boolean canChangeTo(WorkOrderState t) { return false; }
    },
    CANCELLED {
        public boolean canChangeTo(WorkOrderState t) { return false; }
    },
    INTERRUPTED {
        public boolean canChangeTo(WorkOrderState t) { return t == IN_PROGRESS || t == CANCELLED; }
    };

    public abstract boolean canChangeTo(WorkOrderState target);

    public void transitionTo(WorkOrderState target, Entity entity, String worker) {
        if (!this.canChangeTo(target)) {
            throw new IllegalStateTransitionException(this, target);
        }
        createStateChangeRecord(entity, this, target, worker);
        entity.setState(target);
    }
}
```

**审计表**（统一定义一次，所有业务实体复用）：

```sql
CREATE TABLE qxx_state_change (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    entity_type VARCHAR(50) NOT NULL COMMENT '实体类型(如 ORDER/TASK/ITEM_RECPT)',
    entity_id BIGINT NOT NULL COMMENT '实体ID',
    source_state VARCHAR(30) NOT NULL COMMENT '源状态',
    target_state VARCHAR(30) NOT NULL COMMENT '目标状态',
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '转换结果',
    worker VARCHAR(50) COMMENT '操作人',
    shift_id BIGINT COMMENT '班次ID',
    reason TEXT COMMENT '变更原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_entity (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='状态变更审计记录';
```

---

### 2.3 Hook 扩展机制

Qcadoo 的 Hook 分两类，都通过 XML 声明 + Java 方法实现，框架自动注入。

#### Model Hook：数据生命周期拦截

关键文件：`mes-plugins-basic/src/main/resources/basic/model/product.xml:91-107`

```xml
<hooks>
    <validatesWith class="...ProductHooks" method="validateAdditionalUnit"/>
    <validatesWith class="...ProductHooks" method="checkIfParentIsFamily"/>
    <onCreate class="...ProductHooks" method="onCreate"/>
    <onSave   class="...ProductHooks" method="onSave"/>
    <onCopy   class="...ProductHooks" method="onCopy"/>
    <onUpdate class="...ProductHooks" method="calculateConversionIfUnitChanged"/>
</hooks>
```

**跨插件 Model Hook**：

```xml
<!-- orders 插件 hook 到 basic.parameter 的保存校验 -->
<model:model-hook plugin="basic" model="parameter">
    <model:validatesWith class="...ParameterValidatorsO" method="validatesWith"/>
</model:model-hook>
```

#### View Hook：UI 层动态注入

关键文件：`mes-plugins-orders/src/main/resources/qcadoo-plugin.xml:642-645`

```xml
<!-- orders 插件在 basic 的 productDetails 视图渲染前注入行为 -->
<view:view-hook plugin="basic" view="productDetails"
                type="beforeRender"
                class="com.qcadoo.mes.orders.hooks.ProductDetailsViewHooksO"
                method="updateRibbonState"/>
```

```java
@Service
public class ProductDetailsViewHooksO {
    public void updateRibbonState(final ViewDefinitionState view) {
        // 获取 basic 插件的产品详情页中的表单
        FormComponent productForm = (FormComponent) view.getComponentByReference("form");
        Entity product = productForm.getEntity();

        // 获取 Ribbon 工具栏中 orders 分组的按钮
        WindowComponent window = (WindowComponent) view.getComponentByReference("window");
        RibbonGroup orders = window.getRibbon().getGroupByName("orders");

        // 根据产品是否已保存，动态启用/禁用按钮
        boolean isSaved = product.getId() != null;
        orders.getItemByName("showOrdersWithProductMain").setEnabled(isSaved);
    }
}
```

**效果**：打开 basic 插件的"产品详情"页面时，orders 插件自动在工具栏中注入"查看关联订单"按钮。basic 插件完全不知道 orders 的存在。

#### qixiaoxia-mes 落地方案

**后端 Hook**：用 Spring AOP + 可选依赖接口

```java
// md 模块定义扩展点
public interface EntitySaveHook<T> {
    void beforeSave(T entity);
    void afterSave(T entity);
}

// md 模块的 ItemService 预留 Hook 注入点
@Service
public class ItemService {
    @Autowired(required = false)  // 可选依赖，qc 模块未加载也能工作
    private List<EntitySaveHook<Item>> saveHooks;

    @Transactional
    public void saveItem(Item item) {
        if (saveHooks != null) saveHooks.forEach(h -> h.beforeSave(item));
        itemMapper.insert(item);
        if (saveHooks != null) saveHooks.forEach(h -> h.afterSave(item));
    }
}

// qc 模块注册 Hook（md 模块完全不知情）
@Component
public class ItemInspectionHook implements EntitySaveHook<Item> {
    @Override
    public void beforeSave(Item item) {
        item.setNeedInspection(determineInspectionRequirement(item));
    }
}
```

**前端 Hook**：用 Vue 3 插槽 + 动态组件注册

```vue
<!-- md/item/detail.vue — 主数据模块为其他模块预留插槽 -->
<template>
  <el-form :model="item">
    <el-form-item label="物料名称"><el-input v-model="item.name"/></el-form-item>
    <!-- 扩展点：其他模块在此注入自定义内容 -->
    <slot name="module-extensions" :item="item" />
  </el-form>
</template>
```

---

## 三、表设计对比

### 3.1 核心理念差异

| | Qcadoo | ktg-mes | qixiaoxia-mes 建议 |
|------|--------|---------|---------------------|
| **建模方式** | XML 声明 → Hibernate 自动生成 DDL | 手写 SQL DDL | 手写 SQL + Flyway 版本管理 |
| **扩展方式** | `<model:model-field>` 跨插件透明扩展 | `attr1~attr4` 预留字段 | JSON 列 + 虚拟列索引 |
| **关系表达** | belongsTo/hasMany/manyToMany/tree | 隐式约定（凭字段名猜） | 显式 FK 约束 |
| **枚举约束** | `<enum values="01pending,..."/>` | `varchar(20)` 无约束 | `VARCHAR(30) + COMMENT 列出所有值` |
| **数值精度** | `<decimal>` + validatesScale | `double(12,4)` ⚠️ | `DECIMAL(14,4)` |
| **审计字段** | `auditable="true"` 声明一次 | 每表复制粘贴四列 | 代码模板统一生成 |

### 3.2 同一张"物料表"的对比

**Qcadoo 声明式**（30 行 XML → 框架自动生成完整 DDL + ORM + 校验）：

```xml
<model name="product" activable="true" auditable="true" versionable="true">
    <fields>
        <string name="number" required="true" unique="true"><validatesLength max="255"/></string>
        <string name="name" required="true"><validatesLength max="1024"/></string>
        <enum name="globalTypeOfMaterial" values="01component,02intermediate,03finalProduct,04waste,05package"/>
        <dictionary name="category" dictionary="categories"/>
        <belongsTo name="producer" model="company" plugin="basic"/>
        <belongsTo name="supplier" model="company" plugin="basic"/>
        <hasMany name="substituteComponents" model="substituteComponent" joinField="baseProduct" cascade="delete"/>
        <manyToMany name="qualityCards" model="qualityCard" joinField="products" lazy="true"/>
        <belongsTo name="parent" model="product"/>
        <hasMany name="children" joinField="parent" model="product"/>
        <decimal name="conversion" persistent="false"/>  <!-- 计算字段，不存储 -->
    </fields>
    <identifier expression="#number + ' - ' + #name"/>
</model>
```

**ktg-mes 手写 DDL**（需要同时维护 SQL + Entity + Mapper XML 三处）：

```sql
CREATE TABLE md_item (
  item_id       bigint(20) not null auto_increment,
  item_code     varchar(64) not null,
  item_name     varchar(255) not null,
  specification varchar(500),
  unit_of_measure varchar(64) not null,
  item_type_id  bigint(20) default 0,      -- 隐式外键，无 FK 约束
  -- ⚠️ 冗余：物料类型名直接冗余存储
  item_type_code varchar(64) default '',
  item_type_name varchar(255) default '',
  enable_flag   char(1) default 'Y',
  -- ⚠️ 预留扩展字段
  attr1 varchar(64), attr2 varchar(255), attr3 int(11), attr4 int(11),
  create_by varchar(64), create_time datetime,
  update_by varchar(64), update_time datetime,
  primary key (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**qixiaoxia-mes 建议写法**：

```sql
CREATE TABLE qxx_md_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_code VARCHAR(64) NOT NULL COMMENT '物料编码',
    item_name VARCHAR(255) NOT NULL COMMENT '物料名称',
    specification VARCHAR(500) COMMENT '规格型号',
    unit_code VARCHAR(64) NOT NULL COMMENT '单位编码',
    unit_name VARCHAR(64) COMMENT '单位名称',
    item_type_id BIGINT COMMENT '物料类型ID',
    -- ✅ FK 约束保护数据完整性
    CONSTRAINT fk_item_type FOREIGN KEY (item_type_id) REFERENCES qxx_md_item_type(id),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'
        COMMENT '状态: ACTIVE/INACTIVE/DISCONTINUED',
    -- ✅ JSON 扩展代替 attr1~attr4
    ext_json JSON COMMENT '扩展属性(如纸张行业字段)',
    -- 审计字段统一（通过 Flyway 模板统一生成）
    created_by VARCHAR(64) NOT NULL COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(64) COMMENT '更新人',
    updated_at DATETIME COMMENT '更新时间',
    INDEX idx_item_code (item_code),
    INDEX idx_item_type (item_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='物料主数据';
```

### 3.3 JSON 扩展列的最佳实践

MySQL 8.0 的 JSON 列支持虚拟列索引，完全替代 `attr1~attr4`：

```sql
-- 纸张行业扩展字段
INSERT INTO qxx_md_item (item_code, item_name, ext_json) VALUES
('PAPER-001', '双铜纸', JSON_OBJECT(
    'paper_grade', 'A级',
    'paper_size', '787*1092',
    'paper_thickness', 250,
    'coating', '双面涂布',
    'material_type', '铜版纸'
));

-- 查询时使用 JSON 路径
SELECT * FROM qxx_md_item
WHERE ext_json->>'$.paper_grade' = 'A级'
  AND CAST(ext_json->>'$.paper_thickness' AS UNSIGNED) > 200;

-- 对高频查询的扩展字段建虚拟列索引（不占额外存储空间）
ALTER TABLE qxx_md_item
    ADD COLUMN paper_grade VARCHAR(50)
        GENERATED ALWAYS AS (ext_json->>'$.paper_grade') VIRTUAL,
    ADD INDEX idx_paper_grade (paper_grade);
```

### 3.4 枚举字段规范

```sql
-- ❌ ktg-mes 做法：无约束
status varchar(20) comment '状态'

-- ✅ qixiaoxia-mes 做法：COMMENT 中声明所有枚举值
status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
    COMMENT '状态: PENDING/ACCEPTED/IN_PROGRESS/COMPLETED/DECLINED/INTERRUPTED/ABANDONED'
```

---

## 四、关键文件索引

| 分析主题 | Qcadoo 源码文件 |
|---------|----------------|
| 父 POM & 插件聚合 | `mes/pom.xml`, `mes-plugins/pom.xml` |
| basic 插件描述符 | `mes-plugins-basic/src/main/resources/qcadoo-plugin.xml` |
| orders 插件描述符（含跨插件扩展） | `mes-plugins-orders/src/main/resources/qcadoo-plugin.xml` |
| 声明式数据模型（product） | `mes-plugins-basic/src/main/resources/basic/model/product.xml` |
| 声明式数据模型（order） | `mes-plugins-orders/src/main/resources/orders/model/order.xml` |
| 状态变更实体（orderStateChange） | `mes-plugins-orders/src/main/resources/orders/model/orderStateChange.xml` |
| 状态枚举（OrderState） | `mes-plugins-orders/.../states/constants/OrderState.java` |
| 状态机框架 | `mes-plugins-states/.../states/{StateEnum,StateChangeService,StateChangeContext}.java` |
| 状态机注册 | `mes-plugins-states/.../states/module/AbstractStateServiceRegisterModule.java` |
| View Hook 实现 | `mes-plugins-orders/.../hooks/ProductDetailsViewHooksO.java` |
| DB 配置（hbm2ddl=update） | `mes-application/conf/dev/db.properties` |

---

## 五、总结

Qcadoo MES 值得借鉴的不是它的代码（太老太重），而是它的**设计思想**：

1. **模块隔离**：每个 MES 领域是独立模块，跨模块通过事件/接口通信而非直接依赖
2. **状态机**：业务状态转换用枚举编码规则、每次变更留审计记录
3. **Hook 扩展**：一个模块的功能可以通过预留的扩展点被另一个模块增强，而不修改源码

用 Spring Boot + Vue 3 生态很容易落地这些思想，关键是**从一开始就把扩展点设计进去**：
- 后端：Spring Event + `@Autowired(required = false)` 可选接口 + AOP 切面
- 前端：Vue 3 插槽 + 动态组件注册
- 数据库：JSON 扩展列 + 虚拟列索引 + 显式 FK 约束 + Flyway 版本管理
