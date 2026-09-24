# 用户工作站批量绑定 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把"生产管理→用户工作站"从手输 ID 的生成器页面改造成"多选人员 × 多选工位"批量绑定，后端负责名称回填、防重复、停用重启用，手机打卡链路零改动。

**Architecture:** 后端新增批量绑定服务方法（笛卡尔积逐对处理：已启用跳过 / 已停用重启用 / 不存在则新建）与本域工位选项接口；单条 insert/update 同步加回填与防重。PC 前端页面拆为列表页 + BindDialog 组件，复用 `components/UserSelect/multi.vue`。无 DDL、无 Flyway、App 不改。

**Tech Stack:** Spring Boot 4 / MyBatis / JUnit5 + Mockito + AssertJ；Vue 3.5 + TS + Element Plus。

## Global Constraints

- 规格文档：`docs/superpowers/specs/2026-09-25-user-workstation-binding-design.md`（一切歧义以它为准）
- 分支：`feature/user-workstation-batch-bind`（已建好，规格文档已提交）
- 业务 INSERT **不写 factory_id**（FactoryIdInterceptor 注入）；SQL 的 `<if>` 查询条件照写，factory_id 不手写
- 后端函数 ≤ 50 行；前端组件 ≤ 300 行；重复逻辑抽方法
- 业务异常用 `com.ruoyi.common.exception.ServiceException`，不吞异常
- 后端验证红线：mvn package → 重启 :8081 → token 实测真实接口，禁止只看编译通过
- 本地环境：MySQL 在 Docker（容器名 qxx-mysql，localhost:3307，root/qxx123456，库 mes），后端 8081；其他 worktree 的 8082/8083 进程不要碰
- vue-tsc 有 800+ 历史基线错误，前端类型验证靠 build 通过 + 基线对比，不追求零 tsc
- 不删表、不动 `qxx_md_workstation_worker`、不动 App、不发 H5

---

### Task 1: 后端数据契约与 Mapper 层

**Files:**
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/ProUserWorkstation.java`
- Create: `backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/UserWorkstationBatchRequest.java`
- Create: `backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/UserWorkstationBatchResult.java`
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/mapper/mes/pro/ProUserWorkstationMapper.java`
- Modify: `backend/ruoyi-system/src/main/resources/mapper/mes/pro/ProUserWorkstationMapper.xml`

**Interfaces:**
- Produces:
  - `ProUserWorkstation` 增加非持久查询字段 `String userKeyword / workstationKeyword`（getter/setter）
  - `UserWorkstationBatchRequest`：`List<Long> userIds/workstationIds`、`String remark`（getter/setter）
  - `UserWorkstationBatchResult`：`int successCount/reactivatedCount/skipCount`、`List<String> skips`；方法 `incSuccess()/incReactivated()/incSkip()`、`addSkip(String)`、各 getter
  - Mapper 新增 `List<ProUserWorkstation> selectByUserAndWorkstation(@Param("userId") Long userId, @Param("workstationId") Long workstationId)`

- [ ] **Step 1: ProUserWorkstation 增加两个查询字段**

在 `ProUserWorkstation.java` 的 `private Date operationTime;` 之后加：

```java
    // —— 非持久查询字段（仅列表查询用，不进 insert/update）——
    private String userKeyword;
    private String workstationKeyword;
```

在对应 getter/setter 区域（`setOperationTime` 之后）加：

```java
    public String getUserKeyword() { return userKeyword; }
    public void setUserKeyword(String userKeyword) { this.userKeyword = userKeyword; }
    public String getWorkstationKeyword() { return workstationKeyword; }
    public void setWorkstationKeyword(String workstationKeyword) { this.workstationKeyword = workstationKeyword; }
```

- [ ] **Step 2: 新建 UserWorkstationBatchRequest**

`backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/UserWorkstationBatchRequest.java`：

```java
package com.ruoyi.system.domain.mes.pro;

import java.util.List;

/**
 * 用户工作站批量绑定请求：多选人员 × 多选工位（笛卡尔积逐对处理）
 */
public class UserWorkstationBatchRequest
{
    private List<Long> userIds;
    private List<Long> workstationIds;
    private String remark;

    public List<Long> getUserIds() { return userIds; }
    public void setUserIds(List<Long> userIds) { this.userIds = userIds; }
    public List<Long> getWorkstationIds() { return workstationIds; }
    public void setWorkstationIds(List<Long> workstationIds) { this.workstationIds = workstationIds; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
```

- [ ] **Step 3: 新建 UserWorkstationBatchResult**

`backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/UserWorkstationBatchResult.java`：

```java
package com.ruoyi.system.domain.mes.pro;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量绑定结果统计（跳过明细为 "用户名 / 工位名"）
 */
public class UserWorkstationBatchResult
{
    private int successCount;
    private int reactivatedCount;
    private int skipCount;
    private final List<String> skips = new ArrayList<>();

    public void incSuccess() { successCount++; }
    public void incReactivated() { reactivatedCount++; }
    public void incSkip() { skipCount++; }
    public void addSkip(String detail) { skips.add(detail); }

    public int getSuccessCount() { return successCount; }
    public int getReactivatedCount() { return reactivatedCount; }
    public int getSkipCount() { return skipCount; }
    public List<String> getSkips() { return skips; }
}
```

- [ ] **Step 4: Mapper 接口增加按对查询**

把 `ProUserWorkstationMapper.java` 整体替换为：

```java
package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mes.pro.ProUserWorkstation;

public interface ProUserWorkstationMapper {
    ProUserWorkstation selectProUserWorkstationByRecordId(Long recordId);
    List<ProUserWorkstation> selectProUserWorkstationList(ProUserWorkstation e);

    /** 查同一工厂下某（人,工位）的全部绑定记录（含停用），factory_id 由拦截器注入 */
    List<ProUserWorkstation> selectByUserAndWorkstation(@Param("userId") Long userId,
                                                        @Param("workstationId") Long workstationId);

    int insertProUserWorkstation(ProUserWorkstation e);
    int updateProUserWorkstation(ProUserWorkstation e);
    int deleteProUserWorkstationByRecordId(Long recordId);
    int deleteProUserWorkstationByRecordIds(Long[] recordIds);
}
```

- [ ] **Step 5: XML 增加按对查询与关键字条件**

在 `ProUserWorkstationMapper.xml` 的 `selectProUserWorkstationList` 的 `<where>` 块内，`enableFlag` 的 `<if>` 之后加两个条件：

```xml
            <if test="userKeyword != null and userKeyword != ''">
                and (user_name like concat('%', #{userKeyword}, '%')
                     or nick_name like concat('%', #{userKeyword}, '%'))
            </if>
            <if test="workstationKeyword != null and workstationKeyword != ''">
                and (workstation_code like concat('%', #{workstationKeyword}, '%')
                     or workstation_name like concat('%', #{workstationKeyword}, '%'))
            </if>
```

在 `selectProUserWorkstationByRecordId` 之后加：

```xml
    <select id="selectByUserAndWorkstation" resultMap="ProUserWorkstationResult">
        <include refid="selectProUserWorkstationVo"/>
        where user_id = #{userId} and workstation_id = #{workstationId}
        order by record_id
    </select>
```

- [ ] **Step 6: 编译验证**

Run: `cd backend && mvn -pl ruoyi-system -am compile -q`
Expected: BUILD SUCCESS，无输出错误。

- [ ] **Step 7: Commit**

```bash
git add backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/UserWorkstationBatchRequest.java \
        backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/UserWorkstationBatchResult.java \
        backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/mes/pro/ProUserWorkstation.java \
        backend/ruoyi-system/src/main/java/com/ruoyi/system/mapper/mes/pro/ProUserWorkstationMapper.java \
        backend/ruoyi-system/src/main/resources/mapper/mes/pro/ProUserWorkstationMapper.xml
git commit -m "feat(pro): 用户工作站批量绑定契约层（DTO/结果对象/按对查询/关键字）"
```

---

### Task 2: Service 层加固与批量绑定（TDD）

**Files:**
- Test: `backend/ruoyi-system/src/test/java/com/ruoyi/system/service/mes/pro/ProUserWorkstationServiceImplTest.java`
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/pro/IProUserWorkstationService.java`
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/pro/impl/ProUserWorkstationServiceImpl.java`

**Interfaces:**
- Consumes: Task 1 的全部产物；`ISysUserService.selectUserById(Long)`（`com.ruoyi.system.service.ISysUserService`，返回 `SysUser`，取 getUserId/getUserName/getNickName）；`MdWorkstationMapper.selectMdWorkstationByWorkstationId(Long)` 与 `selectMdWorkstationList(MdWorkstation)`（`com.ruoyi.system.mapper.mes.md.MdWorkstationMapper`；MdWorkstation 取 getWorkstationId/Code/Name/EnableFlag）
- Produces: 接口新增 `UserWorkstationBatchResult batchBind(UserWorkstationBatchRequest req)` 与 `List<MdWorkstation> selectWorkstationOptions()`

- [ ] **Step 1: 先写失败的单元测试**

新建 `backend/ruoyi-system/src/test/java/com/ruoyi/system/service/mes/pro/ProUserWorkstationServiceImplTest.java`：

```java
package com.ruoyi.system.service.mes.pro;

import java.util.ArrayList;
import java.util.List;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.md.MdWorkstation;
import com.ruoyi.system.domain.mes.pro.ProUserWorkstation;
import com.ruoyi.system.domain.mes.pro.UserWorkstationBatchRequest;
import com.ruoyi.system.domain.mes.pro.UserWorkstationBatchResult;
import com.ruoyi.system.mapper.mes.md.MdWorkstationMapper;
import com.ruoyi.system.mapper.mes.pro.ProUserWorkstationMapper;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.system.service.mes.pro.impl.ProUserWorkstationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * 用户工作站绑定服务单元测试
 * 覆盖：新建回填 / 已启用跳过 / 已停用重启用 / 用户或工位非法 / 单条防重 / 启停用部分更新
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("用户工作站绑定服务单元测试")
class ProUserWorkstationServiceImplTest {

    @Mock private ProUserWorkstationMapper mapper;
    @Mock private ISysUserService userService;
    @Mock private MdWorkstationMapper workstationMapper;
    @InjectMocks private ProUserWorkstationServiceImpl service;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("admin");
    }

    @AfterEach
    void tearDown() { securityUtilsMock.close(); }

    private SysUser user(long id) {
        SysUser u = new SysUser();
        u.setUserId(id);
        u.setUserName("zhangsan");
        u.setNickName("张三");
        return u;
    }

    private MdWorkstation ws(long id, String enable) {
        MdWorkstation w = new MdWorkstation();
        w.setWorkstationId(id);
        w.setWorkstationCode("WS-001");
        w.setWorkstationName("一号工位");
        w.setEnableFlag(enable);
        return w;
    }

    private ProUserWorkstation row(long recordId, String enable) {
        ProUserWorkstation r = new ProUserWorkstation();
        r.setRecordId(recordId);
        r.setUserId(1L);
        r.setWorkstationId(100L);
        r.setEnableFlag(enable);
        return r;
    }

    private UserWorkstationBatchRequest req() {
        UserWorkstationBatchRequest req = new UserWorkstationBatchRequest();
        req.setUserIds(List.of(1L));
        req.setWorkstationIds(List.of(100L));
        return req;
    }

    @Test
    @DisplayName("全新组合：新建并由后端回填名称/时间/启用标志")
    void batch_newPair_insertsWithFilledNames() {
        when(userService.selectUserById(1L)).thenReturn(user(1L));
        when(workstationMapper.selectMdWorkstationByWorkstationId(100L)).thenReturn(ws(100L, "1"));
        when(mapper.selectByUserAndWorkstation(1L, 100L)).thenReturn(new ArrayList<>());

        UserWorkstationBatchResult r = service.batchBind(req());

        assertThat(r.getSuccessCount()).isEqualTo(1);
        assertThat(r.getReactivatedCount()).isZero();
        assertThat(r.getSkipCount()).isZero();
        verify(mapper).insertProUserWorkstation(argThat(e ->
                e.getUserId().equals(1L)
                && "zhangsan".equals(e.getUserName())
                && "张三".equals(e.getNickName())
                && e.getWorkstationId().equals(100L)
                && "WS-001".equals(e.getWorkstationCode())
                && "一号工位".equals(e.getWorkstationName())
                && "1".equals(e.getEnableFlag())
                && e.getOperationTime() != null));
        verify(mapper, never()).updateProUserWorkstation(any());
    }

    @Test
    @DisplayName("已存在启用绑定：跳过且不写库")
    void batch_enabledExists_skips() {
        when(userService.selectUserById(1L)).thenReturn(user(1L));
        when(workstationMapper.selectMdWorkstationByWorkstationId(100L)).thenReturn(ws(100L, "1"));
        when(mapper.selectByUserAndWorkstation(1L, 100L)).thenReturn(List.of(row(9L, "1")));

        UserWorkstationBatchResult r = service.batchBind(req());

        assertThat(r.getSkipCount()).isEqualTo(1);
        assertThat(r.getSkips()).hasSize(1);
        verify(mapper, never()).insertProUserWorkstation(any());
        verify(mapper, never()).updateProUserWorkstation(any());
    }

    @Test
    @DisplayName("仅有停用绑定：重新启用并刷新名称")
    void batch_disabledExists_reactivates() {
        when(userService.selectUserById(1L)).thenReturn(user(1L));
        when(workstationMapper.selectMdWorkstationByWorkstationId(100L)).thenReturn(ws(100L, "1"));
        when(mapper.selectByUserAndWorkstation(1L, 100L)).thenReturn(List.of(row(8L, "0")));

        UserWorkstationBatchResult r = service.batchBind(req());

        assertThat(r.getReactivatedCount()).isEqualTo(1);
        assertThat(r.getSuccessCount()).isZero();
        verify(mapper).updateProUserWorkstation(argThat(e ->
                e.getRecordId().equals(8L) && "1".equals(e.getEnableFlag())
                && "张三".equals(e.getNickName()) && e.getOperationTime() != null));
        verify(mapper, never()).insertProUserWorkstation(any());
    }

    @Test
    @DisplayName("用户不存在：整批报错不写库")
    void batch_userMissing_throws() {
        when(userService.selectUserById(2L)).thenReturn(null);
        UserWorkstationBatchRequest req = req();
        req.setUserIds(List.of(2L));

        assertThatThrownBy(() -> service.batchBind(req))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("用户不存在");
        verify(mapper, never()).insertProUserWorkstation(any());
    }

    @Test
    @DisplayName("工位停用：整批报错不写库")
    void batch_workstationDisabled_throws() {
        when(userService.selectUserById(1L)).thenReturn(user(1L));
        when(workstationMapper.selectMdWorkstationByWorkstationId(100L)).thenReturn(ws(100L, "0"));

        assertThatThrownBy(() -> service.batchBind(req()))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("工位不存在或已停用");
        verify(mapper, never()).insertProUserWorkstation(any());
    }

    @Test
    @DisplayName("入参为空/超上限：报错")
    void batch_illegalArgs_throws() {
        UserWorkstationBatchRequest empty = new UserWorkstationBatchRequest();
        assertThatThrownBy(() -> service.batchBind(empty))
                .isInstanceOf(ServiceException.class).hasMessageContaining("人员");

        UserWorkstationBatchRequest tooMany = req();
        tooMany.setUserIds(java.util.stream.Stream.generate(() -> 1L).limit(51).toList());
        assertThatThrownBy(() -> service.batchBind(tooMany))
                .isInstanceOf(ServiceException.class).hasMessageContaining("50");
    }

    @Test
    @DisplayName("单条新增遇启用绑定：防重报错")
    void insert_duplicate_throws() {
        when(userService.selectUserById(1L)).thenReturn(user(1L));
        when(workstationMapper.selectMdWorkstationByWorkstationId(100L)).thenReturn(ws(100L, "1"));
        when(mapper.selectByUserAndWorkstation(1L, 100L)).thenReturn(List.of(row(9L, "1")));

        ProUserWorkstation e = new ProUserWorkstation();
        e.setUserId(1L);
        e.setWorkstationId(100L);
        assertThatThrownBy(() -> service.insertProUserWorkstation(e))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已绑定此工位");
    }

    @Test
    @DisplayName("启停用开关：仅传 recordId+enableFlag 的部分更新可用，不触发校验")
    void update_enableFlagOnly_partialUpdate() {
        ProUserWorkstation old = row(7L, "1");
        when(mapper.selectProUserWorkstationByRecordId(7L)).thenReturn(old);

        ProUserWorkstation patch = new ProUserWorkstation();
        patch.setRecordId(7L);
        patch.setEnableFlag("0");
        service.updateProUserWorkstation(patch);

        verify(mapper).updateProUserWorkstation(argThat(e ->
                e.getRecordId().equals(7L) && "0".equals(e.getEnableFlag())));
        verify(userService, never()).selectUserById(any());
    }

    @Test
    @DisplayName("改绑到已被占用的（人,工位）：拦截")
    void update_pairChange_duplicateBlocked() {
        ProUserWorkstation old = row(7L, "1");
        ProUserWorkstation patch = new ProUserWorkstation();
        patch.setRecordId(7L);
        patch.setUserId(2L);
        when(mapper.selectProUserWorkstationByRecordId(7L)).thenReturn(old);
        when(userService.selectUserById(2L)).thenReturn(user(2L));
        when(workstationMapper.selectMdWorkstationByWorkstationId(100L)).thenReturn(ws(100L, "1"));
        when(mapper.selectByUserAndWorkstation(2L, 100L)).thenReturn(List.of(row(77L, "1")));

        assertThatThrownBy(() -> service.updateProUserWorkstation(patch))
                .isInstanceOf(ServiceException.class).hasMessageContaining("已绑定此工位");
        verify(mapper, never()).updateProUserWorkstation(any());
    }
}
```

- [ ] **Step 2: 跑测试确认失败（编译失败：方法不存在）**

Run: `cd backend && mvn -pl ruoyi-system -am test -Dtest=ProUserWorkstationServiceImplTest -q`
Expected: 编译失败，`batchBind / selectWorkstationOptions` 方法不存在。

- [ ] **Step 3: 扩展 Service 接口**

把 `IProUserWorkstationService.java` 整体替换为：

```java
package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdWorkstation;
import com.ruoyi.system.domain.mes.pro.ProUserWorkstation;
import com.ruoyi.system.domain.mes.pro.UserWorkstationBatchRequest;
import com.ruoyi.system.domain.mes.pro.UserWorkstationBatchResult;

/**
 * ProUserWorkstationService接口
 */
public interface IProUserWorkstationService
{
    ProUserWorkstation selectProUserWorkstationByRecordId(Long recordId);
    List<ProUserWorkstation> selectProUserWorkstationList(ProUserWorkstation e);
    List<ProUserWorkstation> selectAll();
    List<MdWorkstation> selectWorkstationOptions();
    UserWorkstationBatchResult batchBind(UserWorkstationBatchRequest request);
    int insertProUserWorkstation(ProUserWorkstation e);
    int updateProUserWorkstation(ProUserWorkstation e);
    int deleteProUserWorkstationByRecordIds(Long[] recordIds);
    int deleteProUserWorkstationByRecordId(Long recordId);
}
```

- [ ] **Step 4: 实现 Service**

把 `ProUserWorkstationServiceImpl.java` 整体替换为：

```java
package com.ruoyi.system.service.mes.pro.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.mes.md.MdWorkstation;
import com.ruoyi.system.mapper.mes.md.MdWorkstationMapper;
import com.ruoyi.system.mapper.mes.pro.ProUserWorkstationMapper;
import com.ruoyi.system.domain.mes.pro.ProUserWorkstation;
import com.ruoyi.system.domain.mes.pro.UserWorkstationBatchRequest;
import com.ruoyi.system.domain.mes.pro.UserWorkstationBatchResult;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.system.service.mes.pro.IProUserWorkstationService;

@Service
public class ProUserWorkstationServiceImpl implements IProUserWorkstationService
{
    private static final int MAX_USERS = 50;
    private static final int MAX_WORKSTATIONS = 20;
    private static final int REMARK_MAX = 500;

    @Autowired private ProUserWorkstationMapper proUserWorkstationMapper;
    @Autowired private ISysUserService userService;
    @Autowired private MdWorkstationMapper workstationMapper;

    @Override
    public ProUserWorkstation selectProUserWorkstationByRecordId(Long recordId) {
        return proUserWorkstationMapper.selectProUserWorkstationByRecordId(recordId);
    }

    @Override
    public List<ProUserWorkstation> selectProUserWorkstationList(ProUserWorkstation e) {
        return proUserWorkstationMapper.selectProUserWorkstationList(e);
    }

    @Override
    public List<ProUserWorkstation> selectAll() {
        ProUserWorkstation cond = new ProUserWorkstation();
        cond.setEnableFlag("1");
        return proUserWorkstationMapper.selectProUserWorkstationList(cond);
    }

    @Override
    public List<MdWorkstation> selectWorkstationOptions() {
        MdWorkstation cond = new MdWorkstation();
        cond.setEnableFlag("1");
        List<MdWorkstation> list = workstationMapper.selectMdWorkstationList(cond);
        list.sort(Comparator.comparing(MdWorkstation::getWorkstationCode,
                Comparator.nullsLast(String::compareTo)));
        return list;
    }

    @Override
    @Transactional
    public UserWorkstationBatchResult batchBind(UserWorkstationBatchRequest req) {
        validateRequest(req);
        List<SysUser> users = resolveUsers(req.getUserIds());
        List<MdWorkstation> stations = resolveStations(req.getWorkstationIds());
        UserWorkstationBatchResult result = new UserWorkstationBatchResult();
        for (SysUser user : users) {
            for (MdWorkstation station : stations) {
                bindOne(user, station, req.getRemark(), result);
            }
        }
        return result;
    }

    @Override
    @Transactional
    public int insertProUserWorkstation(ProUserWorkstation e) {
        if (e.getUserId() == null || e.getWorkstationId() == null) {
            throw new ServiceException("用户和工位不能为空");
        }
        UserWorkstationBatchRequest req = new UserWorkstationBatchRequest();
        req.setUserIds(List.of(e.getUserId()));
        req.setWorkstationIds(List.of(e.getWorkstationId()));
        req.setRemark(e.getRemark());
        batchBind(req);
        return 1;
    }

    @Override
    public int updateProUserWorkstation(ProUserWorkstation e) {
        if (e.getRecordId() == null) throw new ServiceException("记录ID不能为空");
        ProUserWorkstation old = proUserWorkstationMapper.selectProUserWorkstationByRecordId(e.getRecordId());
        if (old == null) throw new ServiceException("绑定记录不存在");

        boolean pairChanging = (e.getUserId() != null && !e.getUserId().equals(old.getUserId()))
                || (e.getWorkstationId() != null && !e.getWorkstationId().equals(old.getWorkstationId()));
        if (pairChanging) {
            applyChangedPair(e, old);
        }
        e.setUpdateTime(DateUtils.getNowDate());
        e.setUpdateBy(SecurityUtils.getUsername());
        return proUserWorkstationMapper.updateProUserWorkstation(e);
    }

    @Override
    public int deleteProUserWorkstationByRecordIds(Long[] recordIds) {
        return proUserWorkstationMapper.deleteProUserWorkstationByRecordIds(recordIds);
    }

    @Override
    public int deleteProUserWorkstationByRecordId(Long recordId) {
        return proUserWorkstationMapper.deleteProUserWorkstationByRecordId(recordId);
    }

    // ══════════ 私有 ══════════

    private void validateRequest(UserWorkstationBatchRequest req) {
        if (req == null || req.getUserIds() == null || req.getUserIds().isEmpty()) {
            throw new ServiceException("请选择绑定人员");
        }
        if (req.getWorkstationIds() == null || req.getWorkstationIds().isEmpty()) {
            throw new ServiceException("请选择绑定工位");
        }
        if (req.getUserIds().size() > MAX_USERS) {
            throw new ServiceException("单次最多绑定 " + MAX_USERS + " 人");
        }
        if (req.getWorkstationIds().size() > MAX_WORKSTATIONS) {
            throw new ServiceException("单次最多绑定 " + MAX_WORKSTATIONS + " 个工位");
        }
        if (req.getRemark() != null && req.getRemark().length() > REMARK_MAX) {
            throw new ServiceException("备注不能超过 " + REMARK_MAX + " 字");
        }
    }

    private List<SysUser> resolveUsers(List<Long> ids) {
        List<SysUser> users = new ArrayList<>();
        for (Long uid : distinct(ids)) {
            SysUser u = userService.selectUserById(uid);
            if (u == null) throw new ServiceException("用户不存在：" + uid);
            users.add(u);
        }
        return users;
    }

    private List<MdWorkstation> resolveStations(List<Long> ids) {
        List<MdWorkstation> stations = new ArrayList<>();
        for (Long wid : distinct(ids)) {
            MdWorkstation w = workstationMapper.selectMdWorkstationByWorkstationId(wid);
            if (w == null || !"1".equals(w.getEnableFlag())) {
                throw new ServiceException("工位不存在或已停用：" + wid);
            }
            stations.add(w);
        }
        return stations;
    }

    private void bindOne(SysUser user, MdWorkstation station, String remark, UserWorkstationBatchResult result) {
        List<ProUserWorkstation> exist =
                proUserWorkstationMapper.selectByUserAndWorkstation(user.getUserId(), station.getWorkstationId());
        ProUserWorkstation enabled = exist.stream().filter(x -> "1".equals(x.getEnableFlag())).findFirst().orElse(null);
        if (enabled != null) {
            result.incSkip();
            result.addSkip(user.getUserName() + " / " + station.getWorkstationName());
            return;
        }
        ProUserWorkstation target = exist.stream().filter(x -> "0".equals(x.getEnableFlag())).findFirst()
                .orElseGet(ProUserWorkstation::new);
        fillNames(target, user, station);
        target.setEnableFlag("1");
        target.setOperationTime(DateUtils.getNowDate());
        if (remark != null) target.setRemark(remark);

        if (target.getRecordId() == null) {
            target.setCreateBy(SecurityUtils.getUsername());
            target.setCreateTime(DateUtils.getNowDate());
            proUserWorkstationMapper.insertProUserWorkstation(target);
            result.incSuccess();
        } else {
            target.setUpdateBy(SecurityUtils.getUsername());
            target.setUpdateTime(DateUtils.getNowDate());
            proUserWorkstationMapper.updateProUserWorkstation(target);
            result.incReactivated();
        }
    }

    /** 改绑（人/工位变化）：解析新名称并拦截与其他启用记录冲突 */
    private void applyChangedPair(ProUserWorkstation patch, ProUserWorkstation old) {
        Long uid = patch.getUserId() != null ? patch.getUserId() : old.getUserId();
        Long wid = patch.getWorkstationId() != null ? patch.getWorkstationId() : old.getWorkstationId();
        SysUser user = userService.selectUserById(uid);
        if (user == null) throw new ServiceException("用户不存在：" + uid);
        MdWorkstation station = workstationMapper.selectMdWorkstationByWorkstationId(wid);
        if (station == null || !"1".equals(station.getEnableFlag())) {
            throw new ServiceException("工位不存在或已停用：" + wid);
        }
        boolean conflict = proUserWorkstationMapper.selectByUserAndWorkstation(uid, wid).stream()
                .anyMatch(x -> "1".equals(x.getEnableFlag()) && !x.getRecordId().equals(patch.getRecordId()));
        if (conflict) throw new ServiceException("该用户已绑定此工位，请勿重复绑定");
        patch.setUserId(uid);
        patch.setWorkstationId(wid);
        fillNames(patch, user, station);
    }

    private void fillNames(ProUserWorkstation e, SysUser user, MdWorkstation station) {
        e.setUserId(user.getUserId());
        e.setUserName(user.getUserName());
        e.setNickName(user.getNickName());
        e.setWorkstationId(station.getWorkstationId());
        e.setWorkstationCode(station.getWorkstationCode());
        e.setWorkstationName(station.getWorkstationName());
    }

    private List<Long> distinct(List<Long> ids) {
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }
}
```

- [ ] **Step 5: 跑测试确认全部通过**

Run: `cd backend && mvn -pl ruoyi-system -am test -Dtest=ProUserWorkstationServiceImplTest -q`
Expected: Tests run: 9, Failures: 0, Errors: 0, BUILD SUCCESS。

- [ ] **Step 6: Commit**

```bash
git add backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/pro/IProUserWorkstationService.java \
        backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/pro/impl/ProUserWorkstationServiceImpl.java \
        backend/ruoyi-system/src/test/java/com/ruoyi/system/service/mes/pro/ProUserWorkstationServiceImplTest.java
git commit -m "feat(pro): 用户工作站批量绑定服务（防重/重启用/名称回填）+ 单测"
```

---

### Task 3: Controller 端点 + 本地重启实测（后端红线）

**Files:**
- Modify: `backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/mes/pro/ProUserWorkstationController.java`

**Interfaces:**
- Consumes: Task 2 的 `batchBind` / `selectWorkstationOptions`
- Produces: `POST /mes/pro/userworkstation/batch`、`GET /mes/pro/userworkstation/workstationOptions`

- [ ] **Step 1: 增加两个端点**

在 Controller 的 `listAll()` 方法之后插入（注意新增 import：`UserWorkstationBatchRequest`、`UserWorkstationBatchResult` 不需要显式 import 若用 AjaxResult 链——实际方法签名需要，加 import）：

```java
    @PreAuthorize("@ss.hasPermi('mes:pro:userworkstation:query')")
    @GetMapping("/workstationOptions")
    public AjaxResult workstationOptions() {
        return success(proUserWorkstationService.selectWorkstationOptions());
    }

    @PreAuthorize("@ss.hasPermi('mes:pro:userworkstation:add')")
    @Log(title = "用户工作站", businessType = BusinessType.INSERT)
    @PostMapping("/batch")
    public AjaxResult batch(@RequestBody UserWorkstationBatchRequest request) {
        return success(proUserWorkstationService.batchBind(request));
    }
```

import 区增加：

```java
import com.ruoyi.system.domain.mes.pro.UserWorkstationBatchRequest;
```

（`workstationOptions` 是字面路径，Spring 优先于 `/{recordId}` 匹配，与现有 `/listAll` 同理，不会被吞。）

- [ ] **Step 2: 打包 + 重启**

先确认 8081 是本会话要重启的进程（`lsof -i:8081 -sTCP:LISTEN`），8082/8083 不动：

Run: `cd backend && mvn -pl ruoyi-admin -am package -DskipTests -q`
Expected: BUILD SUCCESS，jar 更新。

然后重启（按本机开发流程；若 8081 已有旧进程，kill 后 nohup 起新 jar，等 captchaImage 200）：

```bash
kill $(lsof -ti:8081 -sTCP:LISTEN) 2>/dev/null
cd backend/ruoyi-admin
nohup java -jar target/ruoyi-admin.jar > /tmp/ruoyi-backend.log 2>&1 &
for i in $(seq 1 30); do sleep 2; curl -s -o /dev/null -w '%{http_code}' http://localhost:8081/captchaImage | grep -q 200 && break; done
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8081/captchaImage
```
Expected: 最后一行 `200`。

- [ ] **Step 3: token + 真实接口实测**

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')
echo ${#TOKEN}
```
Expected: token 长度约 200。

查工位选项（取第一个工位 id 备用）：

```bash
curl -s "http://localhost:8081/mes/pro/userworkstation/workstationOptions" -H "Authorization: Bearer $TOKEN" \
  | python3 -m json.tool | head -20
```
Expected: code 200，data 为数组且只含启用工位，字段有 workstationId/workstationCode/workstationName。

用 admin 用户 id=1 与查到的第一个工位（下面用 `<WS_ID>` 替换）连续调两次 batch：

```bash
curl -s -X POST http://localhost:8081/mes/pro/userworkstation/batch -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"userIds":[1],"workstationIds":[<WS_ID>],"remark":"计划验证-可删"}' | python3 -m json.tool
curl -s -X POST http://localhost:8081/mes/pro/userworkstation/batch -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"userIds":[1],"workstationIds":[<WS_ID>],"remark":"计划验证-可删"}' | python3 -m json.tool
```
Expected: 第一次 `successCount:1`；第二次 `skipCount:1`、skips 含 "admin / <工位名>"。

验证关键字搜索与 myWorkstations 契约不变：

```bash
curl -s "http://localhost:8081/mes/pro/userworkstation/list?userKeyword=admin" -H "Authorization: Bearer $TOKEN" | python3 -m json.tool | head -30
curl -s "http://localhost:8081/mes/pro/workrecord/myWorkstations" -H "Authorization: Bearer $TOKEN" | python3 -m json.tool | head -30
```
Expected: list 能查到刚建的绑定且 total≥1；myWorkstations 返回 code 200 且数组含该工位（字段 workstationId/Code/Name）。

停用开关后重新启用验证 reactivated：

```bash
RID=$(curl -s "http://localhost:8081/mes/pro/userworkstation/list?userKeyword=admin" -H "Authorization: Bearer $TOKEN" | python3 -c 'import sys,json;print(json.load(sys.stdin)["rows"][0]["recordId"])')
curl -s -X PUT http://localhost:8081/mes/pro/userworkstation -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d "{\"recordId\":$RID,\"enableFlag\":\"0\"}" | python3 -m json.tool
curl -s -X POST http://localhost:8081/mes/pro/userworkstation/batch -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"userIds":[1],"workstationIds":[<WS_ID>]}' | python3 -m json.tool
```
Expected: 停用 code 200；再次 batch 返回 `reactivatedCount:1`。

- [ ] **Step 4: 清理验证数据**

再次停用该绑定并物理删除（走 DELETE 接口，仅删验证数据；页面不暴露删除按钮）：

```bash
curl -s -X DELETE "http://localhost:8081/mes/pro/userworkstation/$RID" -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
curl -s "http://localhost:8081/mes/pro/userworkstation/list?userKeyword=admin" -H "Authorization: Bearer $TOKEN" | python3 -c 'import sys,json;print("rows:",json.load(sys.stdin)["total"])'
```
Expected: 删除 code 200；rows 为 0（或回到验证前数量）。

- [ ] **Step 5: Commit**

```bash
git add backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/mes/pro/ProUserWorkstationController.java
git commit -m "feat(pro): 用户工作站批量绑定/工位选项 REST 端点"
```

---

### Task 4: 前端 API 与类型

**Files:**
- Modify: `frontend/src/api/mes/pro/userworkstation.ts`
- Modify: `frontend/src/types/api/mes/pro/userworkstation.ts`

**Interfaces:**
- Produces: API `workstationOptions()`、`batchBindUserWorkstation(data)`；类型 `WorkstationOption / UserWorkstationBatchParams / UserWorkstationBatchResult`；查询类型字段改为 `userKeyword/workstationKeyword`

- [ ] **Step 1: 扩展 API 模块**

在 `frontend/src/api/mes/pro/userworkstation.ts` 末尾追加：

```ts
// 工位选项（批量绑定弹窗用，只含启用工位）
export function workstationOptions() {
  return request({ url: '/mes/pro/userworkstation/workstationOptions', method: 'get' })
}
// 批量绑定（多选人员 × 多选工位）
export function batchBindUserWorkstation(data: { userIds: number[]; workstationIds: number[]; remark?: string }) {
  return request({ url: '/mes/pro/userworkstation/batch', method: 'post', data })
}
```

- [ ] **Step 2: 更新类型定义**

把 `frontend/src/types/api/mes/pro/userworkstation.ts` 整体替换为：

```ts
import { PageDomain, BaseEntity } from '@/types/api/common'

export interface UserWorkstationQueryParams extends PageDomain {
  userKeyword?: string
  workstationKeyword?: string
  enableFlag?: string
}

export interface UserWorkstation extends BaseEntity {
  recordId?: number
  userId?: number
  userName?: string
  nickName?: string
  workstationId?: number
  workstationCode?: string
  workstationName?: string
  enableFlag?: string
  operationTime?: string
  remark?: string
}

export interface WorkstationOption {
  workstationId: number
  workstationCode?: string
  workstationName?: string
  enableFlag?: string
}

export interface UserWorkstationBatchParams {
  userIds: number[]
  workstationIds: number[]
  remark?: string
}

export interface UserWorkstationBatchResult {
  successCount: number
  reactivatedCount: number
  skipCount: number
  skips: string[]
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/api/mes/pro/userworkstation.ts frontend/src/types/api/mes/pro/userworkstation.ts
git commit -m "feat(pro): 用户工作站批量绑定前端 API 与类型"
```

---

### Task 5: BindDialog 批量绑定弹窗组件

**Files:**
- Create: `frontend/src/views/mes/pro/userworkstation/components/BindDialog.vue`

**Interfaces:**
- Consumes: `UserMultiSelect`（`@/components/UserSelect/multi.vue`，props `showFlag`，emits `update:showFlag` / `onSelected(rows[])`，row 含 userId/userName/nickName）；`workstationOptions()`、`batchBindUserWorkstation()`
- Produces: props `showFlag: boolean`；emits `update:showFlag(boolean)`、`success()`（绑定成功后父组件刷新列表）

- [ ] **Step 1: 创建组件**

新建 `frontend/src/views/mes/pro/userworkstation/components/BindDialog.vue`：

```vue
<template>
  <el-dialog
    title="新增用户工位绑定"
    :model-value="showFlag"
    @update:model-value="(v: boolean) => emit('update:showFlag', v)"
    width="640px"
    append-to-body
    :close-on-click-modal="false"
    @close="onClose"
  >
    <el-form label-width="92px">
      <el-form-item label="绑定人员" required>
        <div class="pick-row">
          <el-button type="primary" plain icon="Plus" size="small" @click="userSelectVisible = true">
            选择人员
          </el-button>
          <span v-if="!selectedUsers.length" class="pick-hint">请选择（可多选）</span>
        </div>
        <div v-if="selectedUsers.length" class="tag-box">
          <el-tag
            v-for="u in selectedUsers"
            :key="u.userId"
            closable
            type="info"
            class="user-tag"
            @close="removeUser(u.userId)"
          >
            {{ u.nickName || u.userName }}
          </el-tag>
        </div>
      </el-form-item>

      <el-form-item label="绑定工位" required>
        <el-select
          v-model="form.workstationIds"
          multiple
          filterable
          collapse-tags
          collapse-tags-tooltip
          placeholder="请选择工位（可多选）"
          style="width: 100%"
          :loading="optionsLoading"
        >
          <el-option
            v-for="w in options"
            :key="w.workstationId"
            :label="`${w.workstationCode || ''} ${w.workstationName || ''}`.trim()"
            :value="w.workstationId"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="备注">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="2"
          maxlength="500"
          show-word-limit
          placeholder="选填，将写入本批绑定（含重新启用）记录"
        />
      </el-form-item>
    </el-form>

    <UserMultiSelect v-model:showFlag="userSelectVisible" @onSelected="onUsersSelected" />

    <template #footer>
      <el-button @click="emit('update:showFlag', false)">取 消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">确 定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts" name="UserWorkstationBindDialog">
import { ref, reactive, watch, getCurrentInstance } from 'vue'
import { ElMessageBox } from 'element-plus'
import UserMultiSelect from '@/components/UserSelect/multi.vue'
import { workstationOptions, batchBindUserWorkstation } from '@/api/mes/pro/userworkstation'
import type { WorkstationOption, UserWorkstationBatchResult } from '@/types/api/mes/pro/userworkstation'

interface SelectedUser { userId: number; userName?: string; nickName?: string }

const props = defineProps<{ showFlag: boolean }>()
const emit = defineEmits<{
  (e: 'update:showFlag', val: boolean): void
  (e: 'success'): void
}>()

const { proxy } = getCurrentInstance() as any

const selectedUsers = ref<SelectedUser[]>([])
const options = ref<WorkstationOption[]>([])
const optionsLoading = ref(false)
const userSelectVisible = ref(false)
const submitting = ref(false)
const form = reactive<{ workstationIds: number[]; remark: string }>({
  workstationIds: [],
  remark: ''
})

watch(() => props.showFlag, async (v) => {
  if (!v) return
  if (!options.value.length) {
    optionsLoading.value = true
    try {
      const res: any = await workstationOptions()
      options.value = res.data || []
    } catch { proxy.$modal.msgError('工位选项加载失败') } finally { optionsLoading.value = false }
  }
}, { immediate: true })

function onUsersSelected(rows: SelectedUser[]) {
  const known = new Set(selectedUsers.value.map(u => u.userId))
  rows.forEach(u => { if (!known.has(u.userId)) selectedUsers.value.push(u) })
}

function removeUser(uid: number) {
  selectedUsers.value = selectedUsers.value.filter(u => u.userId !== uid)
}

async function submit() {
  if (!selectedUsers.value.length) { proxy.$modal.msgError('请选择绑定人员'); return }
  if (!form.workstationIds.length) { proxy.$modal.msgError('请选择绑定工位'); return }
  submitting.value = true
  try {
    const res: any = await batchBindUserWorkstation({
      userIds: selectedUsers.value.map(u => u.userId),
      workstationIds: form.workstationIds,
      remark: form.remark || undefined
    })
    const r = (res.data || {}) as UserWorkstationBatchResult
    const msg = `新增 ${r.successCount || 0} 条，重新启用 ${r.reactivatedCount || 0} 条，跳过已绑定 ${r.skipCount || 0} 条`
    if (r.skips && r.skips.length) {
      ElMessageBox.alert(r.skips.slice(0, 20).join('<br/>'), `${msg}（跳过明细）`, {
        dangerouslyUseHTMLString: true
      })
    } else {
      proxy.$modal.msgSuccess(msg)
    }
    emit('success')
    emit('update:showFlag', false)
  } finally { submitting.value = false }
}

function onClose() {
  selectedUsers.value = []
  form.workstationIds = []
  form.remark = ''
  userSelectVisible.value = false
}
</script>

<style scoped>
.pick-row { display: flex; align-items: center; gap: 10px; }
.pick-hint { color: #909399; font-size: 12px; }
.tag-box { margin-top: 8px; display: flex; flex-wrap: wrap; gap: 8px; }
</style>
```

- [ ] **Step 2: 类型检查该组件**

Run: `cd frontend && npx vue-tsc --noEmit -p tsconfig.json 2>&1 | grep -c "userworkstation" || true`
Expected: 输出 `0`（该目录无新增类型错误；仓库历史基线错误忽略）。

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/mes/pro/userworkstation/components/BindDialog.vue
git commit -m "feat(pro): 用户工作站批量绑定弹窗（多选人员×工位）"
```

---

### Task 6: 列表页改造 + 构建浏览器实测

**Files:**
- Modify: `frontend/src/views/mes/pro/userworkstation/index.vue`（整体重写，约 190 行）

**Interfaces:**
- Consumes: Task 4 API/类型；Task 5 BindDialog
- Produces: 菜单 2309 对应页面（路由组件 name 保持 `ProUserWorkstation` 不变）

- [ ] **Step 1: 整体重写 index.vue**

把 `frontend/src/views/mes/pro/userworkstation/index.vue` 替换为：

```vue
<template>
  <div class="app-container">
    <!-- 搜索区域 -->
    <el-form :model="queryParams" ref="queryRef" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="用户" prop="userKeyword">
        <el-input v-model="queryParams.userKeyword" placeholder="用户名/昵称" clearable style="width:180px"
          @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="工位" prop="workstationKeyword">
        <el-input v-model="queryParams.workstationKeyword" placeholder="编码/名称" clearable style="width:180px"
          @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="enableFlag">
        <el-select v-model="queryParams.enableFlag" placeholder="全部" clearable style="width:110px">
          <el-option v-for="d in sys_yes_no" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" size="small" @click="handleAdd"
          v-hasPermi="['mes:pro:userworkstation:add']">新增绑定</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" size="small" @click="handleExport"
          v-hasPermi="['mes:pro:userworkstation:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="dataList" size="small">
      <el-table-column label="用户名" align="center" prop="userName" width="130" :show-overflow-tooltip="true" />
      <el-table-column label="昵称" align="center" prop="nickName" width="140" :show-overflow-tooltip="true" />
      <el-table-column label="工位编码" align="center" prop="workstationCode" width="140" :show-overflow-tooltip="true" />
      <el-table-column label="工位名称" align="center" prop="workstationName" min-width="150" :show-overflow-tooltip="true" />
      <el-table-column label="绑定时间" align="center" width="170">
        <template #default="scope">
          <span>{{ parseTime(scope.row.operationTime || scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="启用" align="center" width="80">
        <template #default="scope">
          <el-switch v-model="scope.row.enableFlag" active-value="1" inactive-value="0"
            @change="handleEnableChange(scope.row)" />
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 批量绑定弹窗 -->
    <BindDialog v-model:showFlag="bindOpen" @success="getList" />
  </div>
</template>

<script setup lang="ts" name="ProUserWorkstation">
import { ref, reactive, toRefs, getCurrentInstance, onMounted } from 'vue'
import type { UserWorkstation, UserWorkstationQueryParams } from '@/types/api/mes/pro/userworkstation'
import { listUserWorkstation, updateUserWorkstation } from '@/api/mes/pro/userworkstation'
import BindDialog from './components/BindDialog.vue'

const { proxy } = getCurrentInstance() as any
const { sys_yes_no } = proxy.useDict('sys_yes_no')

const loading = ref(true)
const bindOpen = ref(false)
const showSearch = ref(true)
const total = ref(0)
const dataList = ref<UserWorkstation[]>([])

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userKeyword: undefined,
    workstationKeyword: undefined,
    enableFlag: undefined
  } as UserWorkstationQueryParams
})
const { queryParams } = toRefs(data)

onMounted(() => getList())

function getList() {
  loading.value = true
  listUserWorkstation(queryParams.value)
    .then((r: any) => { dataList.value = r.rows; total.value = r.total })
    .catch(() => proxy.$modal.msgError('查询失败'))
    .finally(() => loading.value = false)
}

function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleAdd() { bindOpen.value = true }

function handleEnableChange(row: UserWorkstation & { enableFlag: string }) {
  const newVal = row.enableFlag
  const text = newVal === '1' ? '启用' : '停用'
  proxy.$modal.confirm(`确认要${text}"${row.nickName || row.userName}"的该工位绑定吗？`)
    .then(() => updateUserWorkstation({ recordId: row.recordId, enableFlag: newVal } as any)
      .then(() => proxy.$modal.msgSuccess(`${text}成功`)))
    .catch(() => {
      // 取消确认：还原开关并重查（避免乐观更新残留）
      ;(row as any).enableFlag = newVal === '1' ? '0' : '1'
      getList()
    })
}

function handleExport() {
  proxy.download('/mes/pro/userworkstation/export', { ...queryParams.value },
    `用户工作站绑定_${new Date().getTime()}.xlsx`)
}
</script>
```

- [ ] **Step 2: 全量构建**

Run: `cd frontend && npm run build:prod`
Expected: build 成功（vue-tsc 历史基线错误若阻断构建，先与 `git stash` 基线对比确认非本次新增；当前脚本 `vite build` 不含 tsc，应直接成功产出 dist）。

- [ ] **Step 3: 浏览器实测（前端红线：必须真点）**

前端 dev 已在 5173（用户正开着）。如未运行：`cd frontend && npm run dev`。浏览器登录后：

1. 进入 生产管理 → 用户工作站：搜索区为 用户 / 工位 / 状态三个条件，表格列无裸 ID，页面无报错；
2. 点"新增绑定"→"选择人员"弹出人员选择对话框（部门树+用户表格），勾选 2 人确定，tag 出现、可移除；
3. 工位下拉可搜索、多选；选 1-2 个工位，确定：
   - toast 显示"新增 N 条…"，列表能按用户关键字搜到；
   - 再次提交同样组合：弹"跳过明细"框，skipCount 正确；
4. 停用某行开关 → 确认后状态刷新；再对同对绑定 → "重新启用 1 条"；
5. 用户关键字、工位关键字、状态过滤均生效；导出下载 xlsx 可打开；
6. App 不动：无需重新发布；如方便可在 H5 打卡页确认"我的工位"仍正常（非阻塞，本地以 Task 3 的 myWorkstations 接口验证为准）。

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/mes/pro/userworkstation/index.vue
git commit -m "feat(pro): 用户工作站列表改关键字搜索+批量绑定入口"
```

---

### Task 7: 自检、合并与交付

**Files:** 无新增，审查与 git 操作。

- [ ] **Step 1: crt-review 三轮自检**

按 `.agents/skills/crt-review`：
1. 逐文件看 diff（`git diff main...HEAD`），核对：factory_id 无手写/无遗漏、函数长度、前后端字段命名 camelCase 一致、无残留手输 ID 路径、无对 App 的改动；
2. 后端再跑一次：`cd backend && mvn -pl ruoyi-system -am test -Dtest=ProUserWorkstationServiceImplTest -q`（9 passed）；
3. 前端 build 产物 grep 确认页面 chunk 含批量绑定文案：`grep -rl "新增用户工位绑定" frontend/dist/assets | head -1` 有输出。

- [ ] **Step 2: 推送分支，交用户评审合并**

```bash
git push -u origin feature/user-workstation-batch-bind
```
向用户汇报：变更文件清单、实测结果（接口三分支 + 浏览器流程）、待其过目后合并 main。

- [ ] **Step 3（用户批准合并后）: 合并 main + deploy 发布**

```bash
git checkout main && git pull --rebase
git merge --no-ff feature/user-workstation-batch-bind -m "Merge: 用户工作站批量绑定完善"
git push
```
然后按 `deploy` skill 发布（本机构建前端 dist → scp；服务器拉码编译 → `systemctl restart qxx-backend.service`，**禁止 kill+nohup**）。生产无 Flyway、表 0 行。

- [ ] **Step 4（发布后）: 生产冒烟**

用生产域名验证：登录 → 用户工作站页打开 → 建一条 admin 绑定 → `myWorkstations` 可见 → 停用；列表/搜索正常。App H5 无需发布。

---

## Self-Review 记录

- **Spec 覆盖**：4.1 batch（Task 2/3）、4.2 单条加固（Task 2 单测覆盖 insert/update）、4.3 关键字（Task 1 XML + Task 6 UI）、4.4 workstationOptions（Task 2/3 + Task 4/5）、4.5 错误处理（Task 2 validate/resolve）、第 5 节拆页（Task 5/6）、第 6 节 App 零改动（Task 3 myWorkstations 契约实测）、7.1 单测（Task 2 九个用例）、7.2 红线（Task 3/6）、第 8 节发布（Task 7）。无缺口。
- **占位符**：Task 3 的 `<WS_ID>`/`$RID` 是实测时的动态值且给出了获取方式，不是计划占位。
- **类型/命名一致性**：`batchBind / selectWorkstationOptions / selectByUserAndWorkstation / UserWorkstationBatchResult(successCount,reactivatedCount,skipCount,skips)` 在后端、前端类型、组件中逐处统一；API 函数名前端为 `batchBindUserWorkstation/workstationOptions`，仅在 TS 层使用，无跨层不一致。
