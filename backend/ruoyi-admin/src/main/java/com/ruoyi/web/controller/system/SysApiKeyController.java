package com.ruoyi.web.controller.system;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.SysApiKey;
import com.ruoyi.system.service.ISysApiKeyService;

/**
 * 外部系统 API Key 凭证管理
 *
 * 供管理员为 CRM 等外部系统签发/吊销接入凭证。
 * 明文 apiKey 仅在生成时返回一次，列表/详情接口不暴露（@JsonIgnore）。
 *
 * @author qixiaoxia
 * @date 2026-08-05
 */
@RestController
@RequestMapping("/system/apikey")
public class SysApiKeyController extends BaseController
{
    @Autowired
    private ISysApiKeyService sysApiKeyService;

    /**
     * 凭证列表（不含明文 key）
     */
    @PreAuthorize("@ss.hasPermi('system:apikey:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysApiKey query)
    {
        startPage();
        List<SysApiKey> list = sysApiKeyService.selectApiKeyList(query);
        return getDataTable(list);
    }

    /**
     * 生成凭证（明文 key 仅本次返回，请调用方妥善保存）
     */
    @Log(title = "API Key 凭证", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('system:apikey:add')")
    @PostMapping("/gen")
    public AjaxResult gen(@RequestParam String name,
                          @RequestParam Long factoryId,
                          @RequestParam(required = false) Date expiresAt,
                          @RequestParam(required = false) String remark)
    {
        SysApiKey apiKey = sysApiKeyService.createApiKey(name, factoryId, expiresAt, remark);
        // 仅返回明文 apiKey + 基础信息，不返回 hash
        Map<String, Object> data = new HashMap<>();
        data.put("id", apiKey.getId());
        data.put("apiKey", apiKey.getApiKey());
        data.put("name", apiKey.getName());
        data.put("factoryId", apiKey.getFactoryId());
        data.put("expiresAt", apiKey.getExpiresAt());
        data.put("msg", "明文 API Key 仅本次返回，请立即保存，后续无法再次查看");
        return AjaxResult.success(data);
    }

    /**
     * 启用/停用
     */
    @Log(title = "API Key 凭证", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('system:apikey:edit')")
    @PutMapping("/toggle/{id}")
    public AjaxResult toggle(@PathVariable Long id, @RequestParam String enabled)
    {
        return toAjax(sysApiKeyService.toggleEnabled(id, enabled));
    }

    /**
     * 删除（吊销）
     */
    @Log(title = "API Key 凭证", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('system:apikey:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        return toAjax(sysApiKeyService.deleteApiKeyById(id));
    }
}
