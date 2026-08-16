package com.ruoyi.system.domain.mes.qc;

import java.io.Serializable;

/**
 * 整单判定配置（抽样数量 / Ac 值 / 缺陷率上限，来自检测方案快照或请求参数）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public class QcJudgeConfig implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 抽检数量 */
    private Integer quantityCheck;

    /** 允许不合格数(Ac) */
    private Integer acQuantity;

    /** 致命缺陷率上限(%) */
    private double crRateLimit;

    /** 严重缺陷率上限(%) */
    private double majRateLimit;

    /** 轻微缺陷率上限(%) */
    private double minRateLimit;

    public Integer getQuantityCheck()
    {
        return quantityCheck;
    }

    public void setQuantityCheck(Integer quantityCheck)
    {
        this.quantityCheck = quantityCheck;
    }

    public Integer getAcQuantity()
    {
        return acQuantity;
    }

    public void setAcQuantity(Integer acQuantity)
    {
        this.acQuantity = acQuantity;
    }

    public double getCrRateLimit()
    {
        return crRateLimit;
    }

    public void setCrRateLimit(double crRateLimit)
    {
        this.crRateLimit = crRateLimit;
    }

    public double getMajRateLimit()
    {
        return majRateLimit;
    }

    public void setMajRateLimit(double majRateLimit)
    {
        this.majRateLimit = majRateLimit;
    }

    public double getMinRateLimit()
    {
        return minRateLimit;
    }

    public void setMinRateLimit(double minRateLimit)
    {
        this.minRateLimit = minRateLimit;
    }
}
