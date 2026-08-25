package com.ruoyi.system.service.mes.pro;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * JDBC HashMap 时间列转换：MySQL 驱动对聚合函数/裸 datetime 列在 resultType=HashMap 时
 * 可能返回 LocalDateTime/LocalDate，而业务 VO 字段为 java.util.Date，强转将抛
 * ClassCastException。统一在此兼容转换。
 *
 * @author qixiaoxia
 * @date 2026-08-23
 */
public final class ProDates
{
    private ProDates()
    {
    }

    /**
     * 将 JDBC 返回的时间对象安全转换为 {@link Date}；null 或非时间类型返回 null。
     */
    public static Date toDate(Object o)
    {
        if (o == null)
        {
            return null;
        }
        if (o instanceof Date d)
        {
            return d;
        }
        if (o instanceof LocalDateTime ldt)
        {
            return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
        }
        if (o instanceof LocalDate ld)
        {
            return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        return null;
    }
}
