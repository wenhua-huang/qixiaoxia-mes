package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmTransaction;

public interface WmTransactionMapper
{
    /** 幂等检查：根据来源单据行查询已存在的事务记录 */
    public WmTransaction selectExistingTransaction(WmTransaction tx);

    public List<WmTransaction> selectWmTransactionList(WmTransaction entity);
    public List<WmTransaction> selectWmTransactionAll();
    public WmTransaction selectWmTransactionByTransactionId(Long transactionId);
    public int insertWmTransaction(WmTransaction entity);
    public int updateWmTransaction(WmTransaction entity);
    public int deleteWmTransactionByTransactionId(Long transactionId);
    public int deleteWmTransactionByTransactionIds(Long[] transactionIds);
}