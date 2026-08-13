package com.ruoyi.web.controller.open;

import java.util.HashMap;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.mes.sal.CrmOrderCreateRequest;
import com.ruoyi.system.domain.mes.sal.SalOrder;
import com.ruoyi.system.service.mes.sal.ISalOrderService;

/**
 * 销售订单开放接口（供 CRM 等外部系统调用）
 *
 * 认证：由 ApiKeyAuthenticationFilter 校验 Header X-API-Key，
 *      工厂归属由 API Key 绑定的 factory_id 决定（本 Controller 内无 @PreAuthorize）。
 *
 * @author qixiaoxia
 * @date 2026-08-05
 */
@RestController
@RequestMapping("/open-api/sal/order")
public class SalOrderOpenController extends BaseController
{
    @Autowired
    private ISalOrderService salOrderService;

    /**
     * CRM 推单创建销售订单
     *
     * @param req 订单信息（orderName/clientName/lines 必填；orderCode 可选）
     * @return { orderId, orderCode }
     */
    @PostMapping("/create")
    public AjaxResult create(@Valid @RequestBody CrmOrderCreateRequest req)
    {
        SalOrder order = salOrderService.createFromCrm(req);
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", order.getOrderId());
        data.put("orderCode", order.getOrderCode());
        return AjaxResult.success(data);
    }
}
