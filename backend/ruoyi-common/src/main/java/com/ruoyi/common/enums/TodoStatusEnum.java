package com.ruoyi.common.enums;

/**
 * 待办状态枚举
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public enum TodoStatusEnum
{
    PENDING("PENDING", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    COMPLETED("COMPLETED", "已完成"),
    REJECTED("REJECTED", "已驳回");

    private final String code;
    private final String info;

    TodoStatusEnum(String code, String info)
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
    public static TodoStatusEnum getByCode(String code)
    {
        for (TodoStatusEnum e : values())
        {
            if (e.getCode().equals(code))
            {
                return e;
            }
        }
        return null;
    }
}
