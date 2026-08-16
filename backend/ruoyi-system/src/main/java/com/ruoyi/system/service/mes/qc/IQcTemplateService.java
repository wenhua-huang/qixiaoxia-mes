package com.ruoyi.system.service.mes.qc;

import java.util.List;
import com.ruoyi.system.domain.mes.qc.QcTemplate;

/**
 * 质检检验模板Service接口（头表+双子表级联）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public interface IQcTemplateService
{
    public List<QcTemplate> selectQcTemplateList(QcTemplate qcTemplate);

    public QcTemplate selectQcTemplateByTemplateId(Long templateId);

    /**
     * 查询模板（含检测项行 indexRows 与物料绑定行 productRows）
     */
    public QcTemplate selectQcTemplateWithRows(Long templateId);

    public int insertQcTemplate(QcTemplate qcTemplate);

    public int updateQcTemplate(QcTemplate qcTemplate);

    public int deleteQcTemplateByTemplateId(Long templateId);

    public int deleteQcTemplateByTemplateIds(Long[] templateIds);
}
