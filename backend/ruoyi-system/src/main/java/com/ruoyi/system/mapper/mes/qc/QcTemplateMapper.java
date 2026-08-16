package com.ruoyi.system.mapper.mes.qc;

import java.util.List;
import com.ruoyi.system.domain.mes.qc.QcTemplate;

/**
 * 质检检验模板Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public interface QcTemplateMapper
{
    public List<QcTemplate> selectQcTemplateList(QcTemplate qcTemplate);

    public QcTemplate selectQcTemplateByTemplateId(Long templateId);

    public QcTemplate checkTemplateCodeUnique(String templateCode);

    public int insertQcTemplate(QcTemplate qcTemplate);

    public int updateQcTemplate(QcTemplate qcTemplate);

    public int deleteQcTemplateByTemplateId(Long templateId);

    public int deleteQcTemplateByTemplateIds(Long[] templateIds);

    /**
     * 删除保护：统计引用该模板的来料检验单数
     */
    public int countIqcReference(Long templateId);

    /**
     * 删除保护：统计引用该模板的过程检验单数
     */
    public int countIpqcReference(Long templateId);

    /**
     * 删除保护：统计引用该模板的出货检验单数
     */
    public int countOqcReference(Long templateId);

    /**
     * 删除保护：统计引用该模板的退料检验单数
     */
    public int countRqcReference(Long templateId);
}
