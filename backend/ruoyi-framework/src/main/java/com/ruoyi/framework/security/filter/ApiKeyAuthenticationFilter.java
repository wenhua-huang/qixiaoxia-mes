package com.ruoyi.framework.security.filter;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysApiKey;
import com.ruoyi.system.service.ISysApiKeyService;

/**
 * 外部系统 API Key 认证过滤器
 *
 * 拦截 /open-api/** 请求，校验 Header X-API-Key：
 * 1. 按 SHA-256 反查 qxx_sys_api_key，命中且启用且未过期才放行
 * 2. 命中 → 按 API Key 绑定的 factory_id 构造 LoginUser 注入 SecurityContextHolder，
 *    使 FactoryIdInterceptor 能正常注入 factory_id（满足多租户隔离）
 * 3. 不命中 → 返回 401 JSON
 *
 * 注意：非 /open-api/** 请求直接放行（交给后续 JWT filter / 权限体系）。
 * SecurityContextHolder 为 ThreadLocal，请求结束由 Spring Security 自动清理，
 * 但本过滤器在 finally 中主动 clear 以防线程池复用残留。
 *
 * @author qixiaoxia
 * @date 2026-08-05
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter
{
    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);

    /** 开放接口路径前缀 */
    public static final String OPEN_API_PREFIX = "/open-api/";

    /** API Key 请求头 */
    public static final String API_KEY_HEADER = "X-API-Key";

    /** API Key 模式下的系统账号标识（createBy 审计用） */
    private static final String API_USER_NAME = "open-api";

    @Autowired
    private ISysApiKeyService sysApiKeyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        // 非开放接口，放行交给后续认证链
        if (!isOpenApiRequest(request))
        {
            chain.doFilter(request, response);
            return;
        }

        String rawKey = request.getHeader(API_KEY_HEADER);
        if (StringUtils.isEmpty(rawKey))
        {
            renderUnauthorized(response, "缺少 API Key，请在请求头携带 " + API_KEY_HEADER);
            return;
        }

        SysApiKey apiKey = sysApiKeyService.verify(rawKey);
        if (apiKey == null)
        {
            renderUnauthorized(response, "无效或已过期的 API Key");
            return;
        }

        try
        {
            // 按 API Key 绑定的工厂构造登录上下文（FactoryIdInterceptor 依赖此上下文注入 factory_id）
            SecurityContextHolder.getContext().setAuthentication(buildAuthentication(request, apiKey));
            chain.doFilter(request, response);
        }
        finally
        {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean isOpenApiRequest(HttpServletRequest request)
    {
        return request.getRequestURI().startsWith(OPEN_API_PREFIX);
    }

    private UsernamePasswordAuthenticationToken buildAuthentication(HttpServletRequest request, SysApiKey apiKey)
    {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(0L);
        sysUser.setUserName(API_USER_NAME);
        sysUser.setFactoryId(apiKey.getFactoryId());
        LoginUser loginUser = new LoginUser(0L, 0L, sysUser, java.util.Collections.emptySet());
        loginUser.setLoginTime(System.currentTimeMillis());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return auth;
    }

    private void renderUnauthorized(HttpServletResponse response, String msg)
    {
        log.warn("[open-api] 认证失败：{}", msg);
        ServletUtils.renderString(response, JSON.toJSONString(AjaxResult.error(HttpStatus.UNAUTHORIZED, msg)));
    }
}
