package com.ruoyi.common.enums;

/**
 * 流水号循环方式枚举
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public enum CycleMethodEnum
{
    YEAR("YEAR", "按年"),
    MONTH("MONTH", "按月"),
    DAY("DAY", "按日"),
    HOUR("HOUR", "按小时"),
    MINUTE("MINUTE", "按分钟"),
    OTHER("OTHER", "按传入字符变");

    private final String code;
    private final String info;

    CycleMethodEnum(String code, String info)
    {
        this.code = code;
        this.info = info;
    }

    public String getCode()
    {
        return code;
    }

    public String getInfo()
    {
        return info;
    }

    /**
     * 根据编码获取枚举
     */
    public static CycleMethodEnum getByCode(String code)
    {
        for (CycleMethodEnum e : values())
        {
            if (e.getCode().equals(code))
            {
                return e;
            }
        }
        return null;
    }
}
