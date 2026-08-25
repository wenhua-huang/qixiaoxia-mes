package com.ruoyi.system.service.mes.pro;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.cal.CalTeamMember;
import com.ruoyi.system.mapper.mes.cal.CalTeamMemberMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 班组解析器：按用户反查当前所属班组，供报工/上下工快照。
 * 一期假设一人主属一个班组；多归属时取 member_id 最小的一条。
 */
@Service
public class TeamResolver {

    @Autowired
    private CalTeamMemberMapper teamMemberMapper;

    /** 班组快照（不可为空对象，字段可能为 null） */
    public record TeamSnapshot(Long teamId, String teamCode, String teamName) {
        public static TeamSnapshot empty() {
            return new TeamSnapshot(null, null, null);
        }
    }

    public TeamSnapshot resolveByUserId(Long userId) {
        if (userId == null) {
            return TeamSnapshot.empty();
        }
        CalTeamMember q = new CalTeamMember();
        q.setUserId(userId);
        return pick(teamMemberMapper.selectCalTeamMemberList(q));
    }

    public TeamSnapshot resolveByUserName(String userName) {
        if (StringUtils.isEmpty(userName)) {
            return TeamSnapshot.empty();
        }
        CalTeamMember q = new CalTeamMember();
        q.setUserName(userName);
        List<CalTeamMember> list = teamMemberMapper.selectCalTeamMemberList(q);
        // CalTeamMemberMapper.xml 对 user_name 用 LIKE concat('%', #{userName}, '%') 子串匹配，
        // 不能修改共享 XML（影响其他调用方），故在此内存中做 equals 精确等值过滤，避免 "admin" 误匹配 "admin2"。
        List<CalTeamMember> exact = list.stream()
            .filter(m -> userName.equals(m.getUserName()))
            .toList();
        return pick(exact);
    }

    /** 取 memberId 最小的一条映射为快照；空列表返回空快照。 */
    private TeamSnapshot pick(List<CalTeamMember> list) {
        return list.stream()
            .min(Comparator.comparing(m -> m.getMemberId() == null ? Long.MAX_VALUE : m.getMemberId()))
            .map(m -> new TeamSnapshot(m.getTeamId(), m.getTeamCode(), m.getTeamName()))
            .orElseGet(TeamSnapshot::empty);
    }
}
