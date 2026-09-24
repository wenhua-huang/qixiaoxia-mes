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
