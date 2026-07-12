package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProWorkrecord;

public interface ProWorkrecordMapper {
    ProWorkrecord selectProWorkrecordByRecordId(Long recordId);
    List<ProWorkrecord> selectProWorkrecordList(ProWorkrecord e);

    /** 查某用户当前在岗会话（上工防重 / 移动端进页面判状态） */
    ProWorkrecord selectActiveByUser(ProWorkrecord e);
    /** 查某工作站当前在岗会话（展示） */
    ProWorkrecord selectActiveByWorkstation(ProWorkrecord e);

    int insertProWorkrecord(ProWorkrecord e);
    /** 下工结算：只更 clock_out_time / work_duration / status（条件 status=ACTIVE） */
    int closeSession(ProWorkrecord e);
    int updateProWorkrecord(ProWorkrecord e);
    int deleteProWorkrecordByRecordId(Long recordId);
    int deleteProWorkrecordByRecordIds(Long[] recordIds);
}
