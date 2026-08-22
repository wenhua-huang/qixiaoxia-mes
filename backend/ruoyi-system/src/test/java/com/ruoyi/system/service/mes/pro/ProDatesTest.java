package com.ruoyi.system.service.mes.pro;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.Test;

class ProDatesTest
{
    @Test
    void toDate_null_returns_null()
    {
        assertThat(ProDates.toDate(null)).isNull();
    }

    @Test
    void toDate_passes_through_util_Date()
    {
        Date d = new Date();
        assertThat(ProDates.toDate(d)).isSameAs(d);
    }

    @Test
    void toDate_converts_LocalDateTime()
    {
        LocalDateTime ldt = LocalDateTime.of(2026, 8, 23, 10, 30, 0);
        Date result = ProDates.toDate(ldt);
        assertThat(result).isNotNull();
        Date expected = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
        assertThat(result.getTime()).isEqualTo(expected.getTime());
    }

    @Test
    void toDate_converts_LocalDate_at_start_of_day()
    {
        LocalDate ld = LocalDate.of(2026, 8, 23);
        Date result = ProDates.toDate(ld);
        Date expected = Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
        assertThat(result).isNotNull();
        assertThat(result.getTime()).isEqualTo(expected.getTime());
    }

    @Test
    void toDate_non_temporal_returns_null()
    {
        assertThat(ProDates.toDate("2026-08-23")).isNull();
        assertThat(ProDates.toDate(12345L)).isNull();
    }
}
