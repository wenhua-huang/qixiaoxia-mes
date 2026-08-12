package com.ruoyi.system.service.impl;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.sign.Sha256Utils;
import com.ruoyi.system.domain.SysApiKey;
import com.ruoyi.system.mapper.SysApiKeyMapper;
import com.ruoyi.system.service.ISysApiKeyService;

/**
 * 外部系统 API Key 凭证 服务实现
 *
 * 生成：SecureRandom 产生 32 字节随机数，Base64URL 编码为明文 key（仅在 createApiKey 返回一次），
 *      同时存 SHA-256 hex 用于后续校验比对。
 * 校验：verify(rawKey) 对明文做 SHA-256 后按 hash 反查，命中且启用且未过期才放行。
 *
 * @author qixiaoxia
 * @date 2026-08-05
 */
@Service
public class SysApiKeyServiceImpl implements ISysApiKeyService
{
    /** 随机字节数（256 bit） */
    private static final int KEY_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private SysApiKeyMapper sysApiKeyMapper;

    @Override
    public SysApiKey createApiKey(String name, Long factoryId, Date expiresAt, String remark)
    {
        if (StringUtils.isEmpty(name)) throw new ServiceException("凭证名称不能为空");
        if (factoryId == null) throw new ServiceException("绑定工厂不能为空");

        // 生成明文 key（32 字节随机 → Base64URL 无填充，约 43 字符）
        byte[] buf = new byte[KEY_BYTES];
        secureRandom.nextBytes(buf);
        String rawKey = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        String hash = Sha256Utils.hash(rawKey);

        SysApiKey apiKey = new SysApiKey();
        apiKey.setApiKey(rawKey);
        apiKey.setApiKeyHash(hash);
        apiKey.setName(name);
        apiKey.setFactoryId(factoryId);
        apiKey.setEnabled("Y");
        apiKey.setExpiresAt(expiresAt);
        apiKey.setRemark(remark);
        apiKey.setCreateBy(SecurityUtils.getUsername());
        apiKey.setCreateTime(DateUtils.getNowDate());
        sysApiKeyMapper.insertApiKey(apiKey);
        // 返回的对象含明文 apiKey，供 Controller 一次性返回给调用方
        return apiKey;
    }

    @Override
    public SysApiKey verify(String rawKey)
    {
        if (StringUtils.isEmpty(rawKey)) return null;
        String hash = Sha256Utils.hash(rawKey);
        SysApiKey apiKey = sysApiKeyMapper.selectByApiKeyHash(hash);
        if (apiKey == null) return null;
        if (!"Y".equals(apiKey.getEnabled())) return null;
        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().before(new Date())) return null;
        return apiKey;
    }

    @Override
    public List<SysApiKey> selectApiKeyList(SysApiKey query)
    {
        return sysApiKeyMapper.selectApiKeyList(query);
    }

    @Override
    public SysApiKey selectApiKeyById(Long id)
    {
        return sysApiKeyMapper.selectApiKeyById(id);
    }

    @Override
    public int toggleEnabled(Long id, String enabled)
    {
        SysApiKey apiKey = sysApiKeyMapper.selectApiKeyById(id);
        if (apiKey == null) throw new ServiceException("凭证不存在");
        SysApiKey update = new SysApiKey();
        update.setId(id);
        update.setEnabled("Y".equals(enabled) ? "Y" : "N");
        update.setUpdateBy(SecurityUtils.getUsername());
        update.setUpdateTime(DateUtils.getNowDate());
        return sysApiKeyMapper.updateEnabled(update);
    }

    @Override
    public int deleteApiKeyById(Long id)
    {
        return sysApiKeyMapper.deleteApiKeyById(id);
    }
}
