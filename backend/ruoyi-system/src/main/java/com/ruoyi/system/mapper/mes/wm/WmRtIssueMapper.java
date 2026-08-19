package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mes.wm.WmRtIssue;

public interface WmRtIssueMapper {
    WmRtIssue selectWmRtIssueByRtId(Long rtId);
    List<WmRtIssue> selectWmRtIssueList(WmRtIssue e);
    int insertWmRtIssue(WmRtIssue e);
    int updateWmRtIssue(WmRtIssue e);
    int deleteWmRtIssueByRtId(Long rtId);
    int deleteWmRtIssueByRtIds(Long[] rtIds);

    /** 退料单头回写首张 RQC 检验单引用（生成工厂调用，免加载整单） */
    int updateRtIssueHeaderRefs(@Param("rtId") Long rtId,
                                @Param("rqcId") Long rqcId,
                                @Param("rqcCode") String rqcCode);
}
