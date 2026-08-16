package com.ruoyi.system.service.mes.qc.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.qc.QcTemplate;
import com.ruoyi.system.domain.mes.qc.QcTemplateIndex;
import com.ruoyi.system.domain.mes.qc.QcTemplateProduct;
import com.ruoyi.system.mapper.mes.qc.QcTemplateIndexMapper;
import com.ruoyi.system.mapper.mes.qc.QcTemplateMapper;
import com.ruoyi.system.mapper.mes.qc.QcTemplateProductMapper;
import com.ruoyi.system.service.mes.qc.IQcTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 质检检验模板Service业务层处理（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * 头表 + 检测项行/物料绑定行双子表级联：全删全插（行数少、编辑频度低，替换策略最简单且无孤儿行）。
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
@Service
public class QcTemplateServiceImpl implements IQcTemplateService
{
    /** 启用标志（与 qxx_qc_template.enable_flag 字典一致） */
    private static final String ENABLE_FLAG_ON = "1";

    @Autowired
    private QcTemplateMapper qcTemplateMapper;

    @Autowired
    private QcTemplateIndexMapper qcTemplateIndexMapper;

    @Autowired
    private QcTemplateProductMapper qcTemplateProductMapper;

    @Override
    public List<QcTemplate> selectQcTemplateList(QcTemplate qcTemplate)
    {
        return qcTemplateMapper.selectQcTemplateList(qcTemplate);
    }

    @Override
    public QcTemplate selectQcTemplateByTemplateId(Long templateId)
    {
        return qcTemplateMapper.selectQcTemplateByTemplateId(templateId);
    }

    @Override
    public QcTemplate selectQcTemplateWithRows(Long templateId)
    {
        QcTemplate template = qcTemplateMapper.selectQcTemplateByTemplateId(templateId);
        if (template != null)
        {
            template.setIndexRows(qcTemplateIndexMapper.selectByTemplateId(templateId));
            template.setProductRows(qcTemplateProductMapper.selectByTemplateId(templateId));
        }
        return template;
    }

    @Override
    @Transactional
    public int insertQcTemplate(QcTemplate qcTemplate)
    {
        // 编码唯一校验（精确等值匹配，避免 LIKE 子串误判）
        if (qcTemplateMapper.checkTemplateCodeUnique(qcTemplate.getTemplateCode()) != null)
        {
            throw new ServiceException("模板编码已存在");
        }
        qcTemplate.setCreateTime(DateUtils.getNowDate());
        int rows = qcTemplateMapper.insertQcTemplate(qcTemplate);
        insertIndexRows(qcTemplate);
        checkBindUnique(qcTemplate);
        insertProductRows(qcTemplate);
        return rows;
    }

    @Override
    @Transactional
    public int updateQcTemplate(QcTemplate qcTemplate)
    {
        qcTemplate.setUpdateTime(DateUtils.getNowDate());
        // 头
        int rows = qcTemplateMapper.updateQcTemplate(qcTemplate);
        // 检测项行：全删全插（null=本次未提交子表，不清空，防仅改头字段时误删行）
        if (qcTemplate.getIndexRows() != null)
        {
            qcTemplateIndexMapper.deleteByTemplateId(qcTemplate.getTemplateId());
            insertIndexRows(qcTemplate);
        }
        // 物料绑定行：全删全插 + 启用唯一性校验
        if (qcTemplate.getProductRows() != null)
        {
            checkBindUnique(qcTemplate);
            qcTemplateProductMapper.deleteByTemplateId(qcTemplate.getTemplateId());
            insertProductRows(qcTemplate);
        }
        return rows;
    }

    @Override
    @Transactional
    public int deleteQcTemplateByTemplateId(Long templateId)
    {
        checkNoOrderReference(templateId);
        qcTemplateIndexMapper.deleteByTemplateId(templateId);
        qcTemplateProductMapper.deleteByTemplateId(templateId);
        return qcTemplateMapper.deleteQcTemplateByTemplateId(templateId);
    }

    @Override
    @Transactional
    public int deleteQcTemplateByTemplateIds(Long[] templateIds)
    {
        int rows = 0;
        for (Long templateId : templateIds)
        {
            rows += deleteQcTemplateByTemplateId(templateId);
        }
        return rows;
    }

    /** 级联插入检测项行（回填头表主键；factory_id 由拦截器注入） */
    private void insertIndexRows(QcTemplate entity)
    {
        if (entity.getIndexRows() == null)
        {
            return;
        }
        for (QcTemplateIndex row : entity.getIndexRows())
        {
            row.setTemplateId(entity.getTemplateId());
            row.setCreateTime(DateUtils.getNowDate());
            qcTemplateIndexMapper.insertQcTemplateIndex(row);
        }
    }

    /** 级联插入物料绑定行（回填头表主键；factory_id 由拦截器注入） */
    private void insertProductRows(QcTemplate entity)
    {
        if (entity.getProductRows() == null)
        {
            return;
        }
        for (QcTemplateProduct row : entity.getProductRows())
        {
            row.setTemplateId(entity.getTemplateId());
            row.setCreateTime(DateUtils.getNowDate());
            qcTemplateProductMapper.insertQcTemplateProduct(row);
        }
    }

    /**
     * 同一(检验类型,物料,工序)只允许一条启用绑定 — 防查找歧义。
     * 本表无 enable_flag 列，"启用"随头模板：头模板停用时其绑定不参与查找，不构成冲突。
     */
    private void checkBindUnique(QcTemplate entity)
    {
        List<QcTemplateProduct> rows = entity.getProductRows();
        if (rows == null || rows.isEmpty())
        {
            return;
        }
        checkBindUniqueInBatch(rows);
        String enableFlag = entity.getEnableFlag() == null ? ENABLE_FLAG_ON : entity.getEnableFlag();
        if (!ENABLE_FLAG_ON.equals(enableFlag))
        {
            return;
        }
        String[] qcTypes = splitQcTypes(entity.getQcTypes());
        Long factoryId = SecurityUtils.getFactoryId();
        // 全库校验：对本模板外已存在的启用绑定做 (item_id, ifnull(process_id,0)) 查重
        for (QcTemplateProduct p : rows)
        {
            checkBindUniqueInDb(p, qcTypes, entity.getTemplateId(), factoryId);
        }
    }

    /** 提交行内查重：同(物料,工序)出现两次即查找歧义 */
    private void checkBindUniqueInBatch(List<QcTemplateProduct> rows)
    {
        Set<String> seen = new HashSet<>();
        for (QcTemplateProduct p : rows)
        {
            if (p.getItemId() == null)
            {
                continue;
            }
            String key = p.getItemId() + ":" + (p.getProcessId() == null ? 0L : p.getProcessId());
            if (!seen.add(key))
            {
                throw new ServiceException("物料[" + p.getItemName() + "]在同一物料/工序维度存在重复绑定，请合并去重");
            }
        }
    }

    /** 全库查重：逐检验类型统计本模板外的启用绑定（find_in_set 匹配头模板 qc_types） */
    private void checkBindUniqueInDb(QcTemplateProduct p, String[] qcTypes, Long templateId, Long factoryId)
    {
        for (String qcType : qcTypes)
        {
            int cnt = qcTemplateProductMapper.countEnabledBindExclude(templateId, p.getItemId(), p.getProcessId(), qcType, factoryId);
            if (cnt > 0)
            {
                throw new ServiceException("物料[" + p.getItemName() + "]在该检验维度已存在启用的模板绑定，请先停用原有绑定");
            }
        }
    }

    /** 头模板 qc_types 逗号分隔多选拆分（去空白） */
    private String[] splitQcTypes(String qcTypes)
    {
        if (qcTypes == null || qcTypes.trim().isEmpty())
        {
            return new String[0];
        }
        String[] parts = qcTypes.split(",");
        String[] result = new String[parts.length];
        int n = 0;
        for (String part : parts)
        {
            String trimmed = part.trim();
            if (!trimmed.isEmpty())
            {
                result[n++] = trimmed;
            }
        }
        String[] compact = new String[n];
        System.arraycopy(result, 0, compact, 0, n);
        return compact;
    }

    /** 删除保护：模板被任一检验单引用则禁止物理删除 */
    private void checkNoOrderReference(Long templateId)
    {
        int refs = qcTemplateMapper.countIqcReference(templateId)
                + qcTemplateMapper.countIpqcReference(templateId)
                + qcTemplateMapper.countOqcReference(templateId)
                + qcTemplateMapper.countRqcReference(templateId);
        if (refs > 0)
        {
            throw new ServiceException("模板已被检验单引用，请停用而非删除");
        }
    }
}
