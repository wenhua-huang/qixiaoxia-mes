package com.ruoyi.system.service.mes.pro;

import java.util.List;

import com.ruoyi.system.domain.mes.pro.ProException;
import com.ruoyi.system.domain.mes.pro.ProTask;

/**
 * 生产异常单 Service 接口（E1–E6）。
 *
 * <p>手机上报（E6）→ PC 补全定责（E1/E2）→ 七出口处理（E3）→ 回流手动收口。
 * 完工硬拦见 {@link IProExceptionBlockService}。
 *
 * @author qixiaoxia
 */
public interface IProExceptionService
{
    /** 异常台账分页/列表查询 */
    List<ProException> selectProExceptionList(ProException query);

    /** 异常单详情 */
    ProException selectProExceptionByExceptionId(Long exceptionId);

    /**
     * 手机端上报异常（E6）。入参必带 taskId、exceptionType、description（+可选影响数量/照片）；
     * 后端从任务快照回填工单/工序/上报人，状态 OPEN、责任方 PENDING，自动生成异常单号。
     *
     * @return 新建异常单ID
     */
    Long reportException(ProException exception);

    /**
     * 手机上报页只读关联对象：按任务带出工单/工序上下文。
     */
    ProTask reportContext(Long taskId);

    /**
     * PC 补全异常单（E1/E2）。仅 OPEN 可改、异常类型不可变；
     * 责任方首次从 PENDING 选定后锁定；按类型校验四类专属字段。
     */
    int updateProException(ProException exception);

    /**
     * 选出口处理（E3）：先锁后事务。回流 4 出口置 PROCESSING 并挂链产生单据；
     * 终结 3 出口直接 CLOSED。
     *
     * @param input 携带 resolveType，以及按出口不同的 resolveQuantity/newExpectedTime/scrapQuantity/conclusion
     * @return 处理后的最新异常单
     */
    ProException resolveException(Long exceptionId, ProException input);

    /**
     * 回流类异常手动收口：PROCESSING → CLOSED，必填处理结论。
     */
    int closeException(Long exceptionId, String conclusion);

    /**
     * 作废异常单（E1：挂错对象作废重开）。仅 OPEN 可作废，必填作废原因；
     * VOID 为终态，不产生关联单据、不参与完工硬拦。
     */
    int voidException(Long exceptionId, String reason);
}
