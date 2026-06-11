package com.ruoyi.common.utils;

/**
 * 枚举工具类 — 提供通用的 getByCode 方法，避免每个枚举类重复实现
 *
 * @author qixiaoxia
 * @date 2025-06-12
 */
public class EnumUtils
{
    /**
     * 根据编码查找对应的枚举值（枚举需实现 CodeEnum 接口）
     *
     * @param values 枚举值数组
     * @param code   要查找的编码
     * @return 匹配的枚举值，找不到返回 null
     */
    public static <T extends CodeEnum> T getByCode(T[] values, String code)
    {
        if (code == null) return null;
        for (T e : values)
        {
            if (code.equals(e.getCode()))
            {
                return e;
            }
        }
        return null;
    }

    /**
     * 可编码枚举接口 — 实现了 getCode() 的枚举可实现此接口以使用 EnumUtils
     */
    public interface CodeEnum
    {
        String getCode();
    }
}
