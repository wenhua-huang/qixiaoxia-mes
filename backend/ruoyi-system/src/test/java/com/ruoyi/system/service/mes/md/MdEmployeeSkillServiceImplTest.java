package com.ruoyi.system.service.mes.md;

import java.util.Arrays;
import java.util.List;

import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.md.MdEmployeeSkill;
import com.ruoyi.system.mapper.mes.md.MdEmployeeSkillMapper;
import com.ruoyi.system.service.mes.md.impl.MdEmployeeSkillServiceImpl;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 员工技能Service单元测试
 * 覆盖：查询/新增/修改/删除 正常及边界场景
 *
 * @author qixiaoxia
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("员工技能服务单元测试")
class MdEmployeeSkillServiceImplTest {

    @Mock
    private MdEmployeeSkillMapper mdEmployeeSkillMapper;

    @InjectMocks
    private MdEmployeeSkillServiceImpl mdEmployeeSkillService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getUsername).thenReturn("admin");
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    @DisplayName("按ID查询技能 — 正常返回")
    void testSelectBySkillId() {
        MdEmployeeSkill skill = new MdEmployeeSkill();
        skill.setSkillId(1L);
        skill.setSkillName("印刷操作");
        skill.setSkillLevel("SENIOR");
        when(mdEmployeeSkillMapper.selectMdEmployeeSkillBySkillId(1L)).thenReturn(skill);

        MdEmployeeSkill result = mdEmployeeSkillService.selectMdEmployeeSkillBySkillId(1L);
        assertThat(result).isNotNull();
        assertThat(result.getSkillName()).isEqualTo("印刷操作");
        assertThat(result.getSkillLevel()).isEqualTo("SENIOR");
        verify(mdEmployeeSkillMapper).selectMdEmployeeSkillBySkillId(1L);
    }

    @Test
    @DisplayName("按用户ID查询技能列表 — 正常返回")
    void testSelectSkillListByUserId() {
        MdEmployeeSkill s1 = new MdEmployeeSkill();
        s1.setUserId(100L);
        s1.setSkillName("制袋调机");
        MdEmployeeSkill s2 = new MdEmployeeSkill();
        s2.setUserId(100L);
        s2.setSkillName("印刷操作");
        List<MdEmployeeSkill> list = Arrays.asList(s1, s2);

        when(mdEmployeeSkillMapper.selectMdEmployeeSkillList(any(MdEmployeeSkill.class))).thenReturn(list);

        MdEmployeeSkill query = new MdEmployeeSkill();
        query.setUserId(100L);
        List<MdEmployeeSkill> result = mdEmployeeSkillService.selectMdEmployeeSkillList(query);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSkillName()).isEqualTo("制袋调机");
        assertThat(result.get(1).getSkillName()).isEqualTo("印刷操作");
    }

    @Test
    @DisplayName("查询不存在的技能ID — 返回null")
    void testSelectBySkillIdNotFound() {
        when(mdEmployeeSkillMapper.selectMdEmployeeSkillBySkillId(999L)).thenReturn(null);
        MdEmployeeSkill result = mdEmployeeSkillService.selectMdEmployeeSkillBySkillId(999L);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("新增技能 — 成功插入并注入审计字段")
    void testInsertSkillSuccess() {
        MdEmployeeSkill skill = new MdEmployeeSkill();
        skill.setUserId(100L);
        skill.setUserName("zhangsan");
        skill.setSkillName("纸袋检验");
        skill.setSkillLevel("MIDDLE");
        when(mdEmployeeSkillMapper.insertMdEmployeeSkill(any(MdEmployeeSkill.class))).thenReturn(1);

        int rows = mdEmployeeSkillService.insertMdEmployeeSkill(skill);
        assertThat(rows).isEqualTo(1);
        assertThat(skill.getCreateBy()).isEqualTo("admin");
        assertThat(skill.getCreateTime()).isNotNull();
        verify(mdEmployeeSkillMapper).insertMdEmployeeSkill(skill);
    }

    @Test
    @DisplayName("新增技能 — 无障碍调机，不设等级")
    void testInsertSkillNoLevel() {
        MdEmployeeSkill skill = new MdEmployeeSkill();
        skill.setUserId(100L);
        skill.setUserName("zhangsan");
        skill.setSkillName("辅料管理");
        when(mdEmployeeSkillMapper.insertMdEmployeeSkill(any(MdEmployeeSkill.class))).thenReturn(1);

        int rows = mdEmployeeSkillService.insertMdEmployeeSkill(skill);
        assertThat(rows).isEqualTo(1);
        assertThat(skill.getSkillLevel()).isNull();
    }

    @Test
    @DisplayName("修改技能 — 更新等级")
    void testUpdateSkillLevel() {
        MdEmployeeSkill skill = new MdEmployeeSkill();
        skill.setSkillId(1L);
        skill.setSkillLevel("SENIOR");
        when(mdEmployeeSkillMapper.updateMdEmployeeSkill(any(MdEmployeeSkill.class))).thenReturn(1);

        int rows = mdEmployeeSkillService.updateMdEmployeeSkill(skill);
        assertThat(rows).isEqualTo(1);
        assertThat(skill.getUpdateBy()).isEqualTo("admin");
        assertThat(skill.getUpdateTime()).isNotNull();
        verify(mdEmployeeSkillMapper).updateMdEmployeeSkill(skill);
    }

    @Test
    @DisplayName("按ID删除技能 — 正常")
    void testDeleteBySkillId() {
        when(mdEmployeeSkillMapper.deleteMdEmployeeSkillBySkillId(1L)).thenReturn(1);
        int rows = mdEmployeeSkillService.deleteMdEmployeeSkillBySkillId(1L);
        assertThat(rows).isEqualTo(1);
    }

    @Test
    @DisplayName("批量删除技能 — 正常")
    void testDeleteBySkillIds() {
        Long[] ids = {1L, 2L, 3L};
        when(mdEmployeeSkillMapper.deleteMdEmployeeSkillBySkillIds(ids)).thenReturn(3);
        int rows = mdEmployeeSkillService.deleteMdEmployeeSkillBySkillIds(ids);
        assertThat(rows).isEqualTo(3);
    }

    @Test
    @DisplayName("批量删除空数组 — 不抛异常")
    void testDeleteByEmptyIds() {
        Long[] ids = {};
        when(mdEmployeeSkillMapper.deleteMdEmployeeSkillBySkillIds(ids)).thenReturn(0);
        int rows = mdEmployeeSkillService.deleteMdEmployeeSkillBySkillIds(ids);
        assertThat(rows).isEqualTo(0);
    }

    @Test
    @DisplayName("查询空技能列表 — 返回空列表")
    void testSelectEmptySkillList() {
        when(mdEmployeeSkillMapper.selectMdEmployeeSkillList(any(MdEmployeeSkill.class)))
            .thenReturn(java.util.Collections.emptyList());
        List<MdEmployeeSkill> result = mdEmployeeSkillService.selectMdEmployeeSkillList(new MdEmployeeSkill());
        assertThat(result).isEmpty();
    }
}
