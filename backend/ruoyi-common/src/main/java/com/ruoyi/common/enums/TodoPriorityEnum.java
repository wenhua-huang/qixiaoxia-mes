package com.ruoyi.common.enums;

/**
 * 待办优先级枚举
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public enum TodoPriorityEnum
{
    URGENT("URGENT", "紧急"),
    HIGH("HIGH", "高"),
    NORMAL("NORMAL", "普通"),
    LOW("LOW", "低");

    private final String code;
    private final String info;

    TodoPriorityEnum(String code, String info)
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
    public static TodoPriorityEnum getByCode(String code)
    {
        for (TodoPriorityEnum e : values())
        {
            if (e.getCode().equals(code))
            {
                return e;
            }
        }
        return null;
    }
}
