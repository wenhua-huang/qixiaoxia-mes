package com.ruoyi.system.service.mes.pro;

import com.ruoyi.system.domain.mes.cal.CalTeamMember;
import com.ruoyi.system.mapper.mes.cal.CalTeamMemberMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamResolverTest {

    @Mock private CalTeamMemberMapper teamMemberMapper;
    @InjectMocks private TeamResolver teamResolver;

    @Test
    @DisplayName("resolveByUserId: 有班组时返回 teamId/code/name")
    void should_return_team_when_member_exists() {
        CalTeamMember m = new CalTeamMember();
        m.setTeamId(10L); m.setTeamCode("DAY"); m.setTeamName("甲班");
        when(teamMemberMapper.selectCalTeamMemberList(any())).thenReturn(List.of(m));

        TeamResolver.TeamSnapshot snap = teamResolver.resolveByUserId(7L);

        assertThat(snap.teamId()).isEqualTo(10L);
        assertThat(snap.teamCode()).isEqualTo("DAY");
        assertThat(snap.teamName()).isEqualTo("甲班");
    }

    @Test
    @DisplayName("resolveByUserId: 无班组返回空快照（字段为null，不抛异常）")
    void should_return_empty_when_no_team() {
        when(teamMemberMapper.selectCalTeamMemberList(any())).thenReturn(List.of());

        TeamResolver.TeamSnapshot snap = teamResolver.resolveByUserId(7L);

        assertThat(snap.teamId()).isNull();
        assertThat(snap.teamCode()).isNull();
        assertThat(snap.teamName()).isNull();
    }

    @Test
    @DisplayName("resolveByUserId: 多班组取 memberId 最小的一条")
    void should_return_first_when_multiple_teams() {
        CalTeamMember a = new CalTeamMember(); a.setMemberId(2L); a.setTeamId(20L); a.setTeamCode("B");
        CalTeamMember b = new CalTeamMember(); b.setMemberId(1L); b.setTeamId(30L); b.setTeamCode("A");
        when(teamMemberMapper.selectCalTeamMemberList(any())).thenReturn(List.of(a, b));

        TeamResolver.TeamSnapshot snap = teamResolver.resolveByUserId(7L);

        assertThat(snap.teamId()).isEqualTo(30L);
        assertThat(snap.teamCode()).isEqualTo("A");
    }

    @Test
    @DisplayName("resolveByUserId: userId 为 null 返回空快照，不查库")
    void should_return_empty_for_null_userid() {
        TeamResolver.TeamSnapshot snap = teamResolver.resolveByUserId(null);

        assertThat(snap.teamId()).isNull();
        verify(teamMemberMapper, never()).selectCalTeamMemberList(any());
    }

    @Test
    @DisplayName("resolveByUserName: 入参为null/空返回空快照，不查库")
    void should_return_empty_for_blank_username() {
        assertThat(teamResolver.resolveByUserName(null).teamId()).isNull();
        assertThat(teamResolver.resolveByUserName("").teamId()).isNull();
        verify(teamMemberMapper, never()).selectCalTeamMemberList(any());
    }

    @Test
    @DisplayName("resolveByUserName: mapper LIKE 子串返回多条时，内存精确等值过滤后取精确匹配那条")
    void should_exact_filter_username_when_mapper_returns_substring_matches() {
        // mapper XML 对 user_name 用 LIKE concat('%', #{userName}, '%')，
        // 查 "admin" 会把 "admin2" 也带出来；解析器必须在内存里做 equals 精确过滤。
        CalTeamMember exact = new CalTeamMember();
        exact.setMemberId(5L); exact.setUserName("admin");
        exact.setTeamId(100L); exact.setTeamCode("ADMIN"); exact.setTeamName("管理员班");

        CalTeamMember substring = new CalTeamMember();
        substring.setMemberId(3L); substring.setUserName("admin2");
        substring.setTeamId(200L); substring.setTeamCode("OTHER"); substring.setTeamName("其他班");

        // substring 的 memberId 更小，若未做精确过滤会错误地选中它
        when(teamMemberMapper.selectCalTeamMemberList(any())).thenReturn(List.of(exact, substring));

        TeamResolver.TeamSnapshot snap = teamResolver.resolveByUserName("admin");

        assertThat(snap.teamId()).isEqualTo(100L);
        assertThat(snap.teamCode()).isEqualTo("ADMIN");
        assertThat(snap.teamName()).isEqualTo("管理员班");
    }

    @Test
    @DisplayName("resolveByUserName: 只有子串匹配、无精确匹配时返回空快照")
    void should_return_empty_when_only_substring_match() {
        CalTeamMember substring = new CalTeamMember();
        substring.setMemberId(1L); substring.setUserName("admin2");
        substring.setTeamId(200L); substring.setTeamCode("OTHER");
        when(teamMemberMapper.selectCalTeamMemberList(any())).thenReturn(List.of(substring));

        TeamResolver.TeamSnapshot snap = teamResolver.resolveByUserName("admin");

        assertThat(snap.teamId()).isNull();
        assertThat(snap.teamCode()).isNull();
        assertThat(snap.teamName()).isNull();
    }
}
