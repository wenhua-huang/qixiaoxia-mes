package com.ruoyi.common.enums;

/**
 * 待办类型枚举
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public enum TodoTypeEnum
{
    APPROVAL("APPROVAL", "审批"),
    QC_CHECK("QC_CHECK", "质检"),
    DV_CHECK("DV_CHECK", "点检"),
    MAINTEN("MAINTEN", "保养"),
    REPAIR("REPAIR", "维修"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String info;

    TodoTypeEnum(String code, String info)
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
    public static TodoTypeEnum getByCode(String code)
    {
        for (TodoTypeEnum e : values())
        {
            if (e.getCode().equals(code))
            {
                return e;
            }
        }
        return null;
    }
}
