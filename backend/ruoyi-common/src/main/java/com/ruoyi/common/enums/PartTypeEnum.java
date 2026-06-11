package com.ruoyi.common.enums;

/**
 * 编码分段类型枚举
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public enum PartTypeEnum
{
    INPUTCHAR("INPUTCHAR", "输入字符", 0),
    NOWDATE("NOWDATE", "当前日期", 1),
    FIXCHAR("FIXCHAR", "固定字符", 2),
    SERIALNO("SERIALNO", "流水号", 3);

    private final String code;
    private final String info;
    private final int order;

    PartTypeEnum(String code, String info, int order)
    {
        this.code = code;
        this.info = info;
        this.order = order;
    }

    public String getCode()
    {
        return code;
    }

    public String getInfo()
    {
        return info;
    }

    public int getOrder()
    {
        return order;
    }

    /**
     * 根据编码获取枚举
     */
    public static PartTypeEnum getByCode(String code)
    {
        for (PartTypeEnum e : values())
        {
            if (e.getCode().equals(code))
            {
                return e;
            }
        }
        return null;
    }
}
