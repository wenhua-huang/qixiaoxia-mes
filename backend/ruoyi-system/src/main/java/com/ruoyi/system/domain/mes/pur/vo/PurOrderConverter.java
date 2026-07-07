package com.ruoyi.system.domain.mes.pur.vo;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import com.ruoyi.system.domain.mes.pur.PurOrder;

/**
 * PurOrder Entity ↔ VO MapStruct 转换器
 *
 * @author qixiaoxia
 * @date 2026-07-07
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PurOrderConverter
{
    /**
     * Entity → VO（receivedQuantity 由调用方后续设置或 SQL 直接映射）
     */
    PurOrderVO toVO(PurOrder entity);
}
