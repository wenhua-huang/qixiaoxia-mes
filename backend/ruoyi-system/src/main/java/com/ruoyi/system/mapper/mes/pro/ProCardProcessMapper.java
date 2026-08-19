package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mes.pro.ProCardProcess;

public interface ProCardProcessMapper {
    ProCardProcess selectProCardProcessByRecordId(Long recordId);
    List<ProCardProcess> selectProCardProcessList(ProCardProcess e);
    int insertProCardProcess(ProCardProcess e);
    int updateProCardProcess(ProCardProcess e);
    int deleteProCardProcessByRecordId(Long recordId);
    int deleteProCardProcessByRecordIds(Long[] recordIds);

    /**
     * 按流转卡+工序唯一定位流转卡工序行（IPQC 工序检验建单定位来源单据用）
     *
     * @param cardId    流转卡ID
     * @param processId 工序ID
     * @return 流转卡工序行；null = 该卡无此工序
     */
    ProCardProcess selectByCardAndProcess(@Param("cardId") Long cardId,
                                          @Param("processId") Long processId);

    /**
     * 回填流转卡工序的 IPQC 挂点（两列专用 UPDATE，避免并发覆盖整头）
     *
     * @param recordId 流转卡工序 record_id
     * @param ipqcId   过程检验单ID
     * @param ipqcCode 过程检验单编码
     */
    int updateCardProcessRefs(@Param("recordId") Long recordId,
                              @Param("ipqcId") Long ipqcId,
                              @Param("ipqcCode") String ipqcCode);
}
