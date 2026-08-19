package com.ruoyi.system.domain.mes.qc;

import java.io.Serializable;

/**
 * 整单判定结果（各等级缺陷数/缺陷率汇总 + 整单结果）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public class QcJudgeResult implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 致命缺陷数 */
    private int crQuantity;

    /** 严重缺陷数 */
    private int majQuantity;

    /** 轻微缺陷数 */
    private int minQuantity;

    /** 致命缺陷率(%) */
    private double crRate;

    /** 严重缺陷率(%) */
    private double majRate;

    /** 轻微缺陷率(%) */
    private double minRate;

    /** 不合格数量 */
    private int quantityUnqualified;

    /** 整单结果:PASS/FAIL */
    private String result;

    public int getCrQuantity()
    {
        return crQuantity;
    }

    public void setCrQuantity(int crQuantity)
    {
        this.crQuantity = crQuantity;
    }

    public int getMajQuantity()
    {
        return majQuantity;
    }

    public void setMajQuantity(int majQuantity)
    {
        this.majQuantity = majQuantity;
    }

    public int getMinQuantity()
    {
        return minQuantity;
    }

    public void setMinQuantity(int minQuantity)
    {
        this.minQuantity = minQuantity;
    }

    public double getCrRate()
    {
        return crRate;
    }

    public void setCrRate(double crRate)
    {
        this.crRate = crRate;
    }

    public double getMajRate()
    {
        return majRate;
    }

    public void setMajRate(double majRate)
    {
        this.majRate = majRate;
    }

    public double getMinRate()
    {
        return minRate;
    }

    public void setMinRate(double minRate)
    {
        this.minRate = minRate;
    }

    public int getQuantityUnqualified()
    {
        return quantityUnqualified;
    }

    public void setQuantityUnqualified(int quantityUnqualified)
    {
        this.quantityUnqualified = quantityUnqualified;
    }

    public String getResult()
    {
        return result;
    }

    public void setResult(String result)
    {
        this.result = result;
    }
}
