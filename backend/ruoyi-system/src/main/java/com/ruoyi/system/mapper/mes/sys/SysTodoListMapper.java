package com.ruoyi.system.mapper.mes.sys;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mes.sys.SysTodoList;

/**
 * 通用待办事项Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入，SQL 无需手写）
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public interface SysTodoListMapper
{
    public SysTodoList selectSysTodoListByTodoId(Long todoId);
    public List<SysTodoList> selectSysTodoListList(SysTodoList sysTodoList);
    public int insertSysTodoList(SysTodoList sysTodoList);
    public int updateSysTodoList(SysTodoList sysTodoList);
    public List<Map<String, Object>> countByStatus(@Param("userId") Long userId);
    public int deleteSysTodoListByTodoId(Long todoId);
    public int deleteSysTodoListByTodoIds(Long[] todoIds);

    /**
     * 按业务来源查待处理（PENDING/PROCESSING）待办（质检判定完成后回写用）
     *
     * @param sourceDocType 业务类型（IQC/IPQC/OQC/RQC）
     * @param sourceDocId   业务单据ID（检验单主键）
     * @return 待办列表
     */
    public List<SysTodoList> selectPendingBySource(@Param("sourceDocType") String sourceDocType,
                                                   @Param("sourceDocId") Long sourceDocId);

    /**
     * 按「来源类型 + 来源单据ID + 来源单据编码」四元组查一条 PENDING 待办
     * （PRO_QC_BLOCK 拦截待办幂等键：同一 IPQC 对同一任务只建一条）。
     */
    public SysTodoList selectPendingByDocAndCode(@Param("sourceDocType") String sourceDocType,
                                                 @Param("sourceDocId") Long sourceDocId,
                                                 @Param("sourceDocCode") String sourceDocCode);

    /**
     * 按同样四元组批量关闭 PENDING 待办（授权放行后联动）。
     *
     * @return 更新行数
     */
    public int completePendingByDocAndCode(@Param("sourceDocType") String sourceDocType,
                                           @Param("sourceDocId") Long sourceDocId,
                                           @Param("sourceDocCode") String sourceDocCode,
                                           @Param("handleTime") java.util.Date handleTime,
                                           @Param("handleResult") String handleResult,
                                           @Param("updateBy") String updateBy);
}
