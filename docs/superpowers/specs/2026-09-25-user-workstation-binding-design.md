# 用户工作站绑定页完善设计（方案 A：批量绑定）

- 日期：2026-09-25
- 分支：`feature/user-workstation-batch-bind`
- 范围：生产管理 → 用户工作站（菜单 2309，表 `qxx_pro_user_workstation`）

## 1. 背景与问题

"用户工作站"页维护**系统用户与工位的绑定关系**，唯一业务消费方是手机端上下工打卡页（`app/pages/mes/pro/clock.vue`）：进入打卡页调 `GET /mes/pro/workrecord/myWorkstations` 取当前登录人名下启用的工位，渲染成"我的工位"快捷按钮；未绑定的人仍可扫码/输编码上工（绑定优先、不强制）。

现有页面是代码生成器原样产出，存在四个问题：

1. 新增时用户 ID、工位 ID 都要手输数字（`el-input-number`），用户名/工位名称靠手填，管理员需先到别处查 ID；
2. 后端 `insertProUserWorkstation` 完全信任前端传值，不校验用户/工位是否存在、不停用校验、不回填名称；
3. 无防重复：同一（人, 工位）可插入任意多条启用绑定，打卡页出现重复快捷按钮；
4. 只能逐条新增，班组配多个工位需要重复操作。

## 2. 与"工作站-人员"表的关系（不合并）

系统另有 `qxx_md_workstation_worker`（字段含 `role_type`：操作工/调机工/检验员），在 基础数据 → 工作站 编辑弹窗中维护，复用 `components/UserSelect/multi.vue`。该表目前**没有任何业务消费方**（仅随工位删除级联清理）。

本次**不合并**两套关系：打卡链路继续读 `qxx_pro_user_workstation`，不改动 `qxx_md_workstation_worker` 及其维护入口。统一两套关系属于独立架构决策，另行排期。

## 3. 目标 / 非目标

### 目标

- 绑定操作改为"多选人员 × 多选工位"批量建立，一次提交；
- 人员、工位通过选择器选取，ID 不可手输；所有冗余名称由后端回填；
- 防重复绑定；对历史停用绑定做"重新启用"复用而非新建；
- 列表支持按用户（用户名/昵称）、工位（编码/名称）模糊搜索；
- 手机打卡链路零改动。

### 非目标

- 不动表结构（无 Flyway、无 DDL）、不合并 `qxx_md_workstation_worker`；
- 不改 App、不改打卡接口 `myWorkstations` 的返回结构；
- 不恢复物理删除入口（维持现状：以启停用代替删除；后端 DELETE 接口保留但页面不放按钮）；
- 不做角色（操作工/调机工/检验员）概念，绑定关系不带角色。

## 4. 后端设计

包路径维持 `com.ruoyi...mes.pro`（ProUserWorkstation* 四件 + Controller）。

### 4.1 新增批量绑定接口

`POST /mes/pro/userworkstation/batch`，权限 `@PreAuthorize("@ss.hasPermi('mes:pro:userworkstation:add')")`，写操作日志 `@Log(title="用户工作站", businessType=INSERT)`。

请求体（新增 DTO，放 domain 包，命名 `UserWorkstationBatchRequest`）：

```
List<Long> userIds;       // 必填，1..50
List<Long> workstationIds; // 必填，1..20
String remark;             // 选填，<=500
```

Service 方法 `batchBind(UserWorkstationBatchRequest req)`：先获取工厂级 Redisson 锁（见 4.5），锁内以编程式事务（`TransactionTemplate`，先锁后事务）循环每个（user, workstation）对，规则：

1. 用户：`ISysUserService.selectUserById(userId)`，为空抛 `ServiceException("用户不存在: " + userId)`；
2. 工位：`MdWorkstationMapper.selectMdWorkstationByWorkstationId(id)`，为空或 `enableFlag != '1'` 抛 `ServiceException("工位不存在或已停用: " + 名称/ID)`；任一工位非法则整批失败（0 写入），人员非法同理——前置校验全部通过后才进入写入；
3. 查同一 `factory_id` 下该对的已有记录（Mapper 新增 `selectByUserAndWorkstation(userId, workstationId)`，factory_id 由拦截器保证，SQL 仍只按两列查；调用在登录上下文内）：
   - 存在且 `enable_flag='1'` → 计入 `skipCount`，记入 skips 明细，不更新；
   - 存在且 `enable_flag='0'` → 更新为启用，刷新 userName/nickName/workstationCode/workstationName/remark/operation_time=now，计入 `reactivatedCount`；
   - 不存在 → insert：名称全部后端回填（SysUser.userName/nickName、MdWorkstation.workstationCode/workstationName），`enable_flag='1'`、`operation_time=now()`；不写 `factory_id`（FactoryIdInterceptor 注入）；计入 `successCount`。

返回（AjaxResult.success(Map) 或小结果对象）：

```
{ successCount, reactivatedCount, skipCount,
  skips: [ { userName, workstationName } ] }
```

> 笛卡尔积上限 50×20=1000 对，低频管理操作，单事务可接受。

### 4.2 单条接口同步加固（防绕过批量接口直调）

- `insertProUserWorkstation`：按 userId/workstationId 回填四个名称字段（忽略前端传入值）；工位停用/不存在报错；已存在启用绑定报 `ServiceException("该用户已绑定此工位")`；存在停用绑定则改为复用启用（同 4.1 规则），不再新增；`operationTime` 为空时置 now；
- `updateProUserWorkstation`：当 userId/workstationId 发生变化时重新校验+回填，并做排除自身的重复检查；只传 `recordId + enableFlag` 的启停用调用保持部分更新可用（现有动态 UPDATE 语义不变）；
- 名称列一律以后端解析为准，前端表单不再提交名称。

### 4.3 列表查询增强

`ProUserWorkstationMapper.xml` 的 `selectProUserWorkstationList` 增加两个关键字条件：

- `userKeyword` → `AND (user_name LIKE kw OR nick_name LIKE kw)`；
- `workstationKeyword` → `AND (workstation_code LIKE kw OR workstation_name LIKE kw)`。

domain 增加两个**非持久**查询字段 `userKeyword`、`workstationKeyword`（不进 insert/update）。factory_id 不在 XML 手写，沿用拦截器改写。

### 4.4 工位选项接口（本域专用，避免跨域权限）

`GET /mes/pro/userworkstation/workstationOptions`，权限 `mes:pro:userworkstation:query`：返回本厂启用工位的轻量列表 `[{workstationId, workstationCode, workstationName}]`，供绑定弹窗下拉。服务层注入 `MdWorkstationMapper`（enableFlag='1'，按 code 排序）。不直接让前端调 `/mes/md/workstation/listAll`，使本页权限自成闭环。

> 人员选择复用 `components/UserSelect/multi.vue`，它内部调 `/system/user/list`（需 system:user:list）。这与工作站页"操作人员"子表是同一既有依赖；本页定位为管理员配置页，接受该依赖，不为其新建用户查询接口。

### 4.5 并发与一致性

- 绑定写入（批量绑定、单条新增、改绑）用工厂级 Redisson 锁 `mes:pro:userworkstation:bind:{factoryId}` 串行化，**先锁后事务**（`RedisLockTemplate.execute` + `TransactionTemplate`，与质检放行 `ProQcBlockServiceImpl` 同范式，waitSec 用模板默认 5 秒）：查重 → insert / 重启用 / 改绑的 check-then-act 整体在锁内事务中完成，事务提交后才释放锁；
- 该锁是「同一（用户, 工位）只许一行」在**表无唯一索引**前提下的并发兜底；本次不动表结构，`qxx_pro_user_workstation` 仍无唯一索引；
- 取工厂级粗锁（而非逐（人,工位）细锁）：低频管理页操作，单批最多 1000 对，工厂内串行最简单可靠；
- 仅启停用（只传 recordId + enableFlag）不改变绑定对，不进锁。

### 4.6 错误处理

- 入参非法（空列表、超上限）→ `ServiceException`，前端弹错误消息；入参 ID 列表先剔空、去重再校验（`[null]` 不得静默成 0/0/0）；
- 前置校验阶段失败 → 抛异常整事务回滚，0 写入（不做部分成功）；
- 重复绑定属于正常业务分支，跳过并在结果中统计，不算错误。

## 5. PC 前端设计

现页面 309 行，超过 300 行组件红线，拆为两个文件：

```
frontend/src/views/mes/pro/userworkstation/
  index.vue                 列表 + 搜索 + 工具栏 + 启停用
  components/BindDialog.vue 批量绑定弹窗
```

### 5.1 index.vue

- 搜索项：用户关键字（一个输入框，后端对 user_name/nick_name OR 模糊，参数 `userKeyword`）、工位关键字（`workstationKeyword`，对编码/名称 OR 模糊）、启用状态下拉；
- 列：用户名、昵称、工位编码、工位名称、绑定时间、备注、启用开关（沿用现有 `handleEnableChange` 二次确认，调单条 update 只传 recordId/enableFlag）；
- 工具栏：新增绑定（开 BindDialog）、导出（沿用 `/mes/pro/userworkstation/export`）；不放修改、删除按钮；
- 删除行选择与单条新增/修改/查看弹窗相关的全部逻辑与 `handleDelete`。

### 5.2 components/BindDialog.vue

- 复用 `@/components/UserSelect/multi.vue`（部门树 + 分页用户表 + 多选）选人员，已选人员用 `el-tag` 列出、可单个移除；至少选 1 人；
- 工位：`el-select multiple filterable`，数据来自本域新接口 `GET /mes/pro/userworkstation/workstationOptions`（4.4），option 文案"编码 名称"；至少选 1 个；
- 备注：选填 textarea（<=500），写入本批新建/重启用的每条记录；
- 提交调 `batchBind`，成功后 toast：`新增 X 条，重新启用 Y 条，跳过已绑定 Z 条`；Z>0 时在消息或弹窗中列出 skips 明细（ElMessageBox 展示前若干条）；emit 刷新列表。

### 5.3 API / 类型

- `frontend/src/api/mes/pro/userworkstation.ts`：新增 `batchBind(data)`、`workstationOptions()`；list 参数类型扩展 `userKeyword/workstationKeyword`；
- `frontend/src/types/api/mes/pro/userworkstation.ts`：查询参数与批量请求/结果类型补齐；
- 不再有前端提交 userName/nickName/workstationCode/workstationName 的路径。

## 6. App / 打卡

零改动。`myWorkstations` 仍返回当前用户启用绑定，字段不变（workstationId/workstationCode/workstationName）。重复绑定在写入端被杜绝后，打卡页 chips 不再可能重复。

## 7. 测试

### 7.1 后端单元测试（`ruoyi-system` test）

参照现有 `ProWorkrecordServiceUnitTest` 的 Mock 风格，新增 `ProUserWorkstationServiceImplTest`：

1. 批量：全新组合 → 回填名称、operationTime、计数 success；
2. 已启用重复 → 跳过计数、不更新；
3. 已停用记录 → 更新启用并刷新名称，计数 reactivated；
4. 用户不存在 / 工位不存在 / 工位停用 → 抛异常；
5. 单条 insert 的名称回填与重复拦截；
6. 启停用部分更新（仅 recordId+enableFlag）不受影响。

### 7.2 手动验证（后端红线）

1. `mvn -pl ruoyi-admin -am clean package -DskipTests`（本次无资源删除，package 即可，保留 clean 保险）；
2. 重启本地后端，`curl /captchaImage` 200；
3. token 实测：`workstationOptions` 只返启用工位；batch 接口（成功/跳过/重启用三种混合）、单条防重、停用开关、list 两个关键字过滤、`GET /mes/pro/workrecord/myWorkstations` 返回不变；
4. 浏览器实测：弹窗选人多选、工位多选、移除、toast 统计；列表搜索与启停用；
5. vue-tsc 按 stash 基线对比，无新增类型错误。

## 8. 发布

- 纯应用层改动，无 Flyway；生产该表 0 行，无数据迁移；
- 分支实现 → 自检（crt-review）→ 合并 main → deploy skill 发布（systemd 重启）；
- 发布后抽测：生产 batch 建一条测试绑定 → `myWorkstations` 可见 → 停用；App H5 无需重新发布。
