package com.ruoyi.common.enums;

/**
 * 消息类型枚举
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public enum MessageTypeEnum
{
    SYSTEM("SYSTEM", "系统消息"),
    APPROVAL("APPROVAL", "审批通知"),
    WARNING("WARNING", "预警通知"),
    NOTICE("NOTICE", "公告");

    private final String code;
    private final String info;

    MessageTypeEnum(String code, String info)
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
    public static MessageTypeEnum getByCode(String code)
    {
        for (MessageTypeEnum e : values())
        {
            if (e.getCode().equals(code))
            {
                return e;
            }
        }
        return null;
    }
}
