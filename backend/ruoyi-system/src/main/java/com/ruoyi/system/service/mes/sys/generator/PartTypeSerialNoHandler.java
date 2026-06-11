package com.ruoyi.system.service.mes.sys.generator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.ruoyi.common.enums.PartTypeEnum;
import com.ruoyi.system.domain.mes.sys.SysAutoCodePart;
import com.ruoyi.system.domain.mes.sys.SysAutoCodeResult;
import com.ruoyi.system.service.mes.sys.ISysAutoCodeResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * SERIALNO 处理器 — 流水号自增 + 循环判断
 * <p>
 * genSerialCode() 已 synchronized，实例字段在调用期间安全，无需 ThreadLocal。
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
@Order(3)
@Component
public class PartTypeSerialNoHandler implements PartTypeTemplate
{
    @Autowired
    private ISysAutoCodeResultService sysAutoCodeResultService;

    /** 本次调用查到的已有 result（由 AutoCodeGenerator 读取） */
    private SysAutoCodeResult existingResult;
    /** 本次调用是否新周期（由 AutoCodeGenerator 读取） */
    private boolean isNewCycle;

    @Override
    public String partHandle(SysAutoCodePart part)
    {
        // 重置实例状态（synchronized 保证线程安全）
        this.existingResult = null;
        this.isNewCycle = false;

        int startNo = part.getSeriaStartNo() != null ? part.getSeriaStartNo() : 1;
        int step = part.getSeriaStep() != null ? part.getSeriaStep() : 1;

        Long ruleId = part.getRuleId();
        String cycleFlag = part.getCycleFlag();
        String cycleMethod = part.getCycleMethod();

        SysAutoCodeResult result;
        if ("1".equals(cycleFlag) && cycleMethod != null && !cycleMethod.isEmpty())
        {
            String cycleKey = CodeGenHelper.computeCycleKey(cycleMethod, part.getInputCharacter());
            if ("OTHER".equals(cycleMethod))
            {
                result = sysAutoCodeResultService.selectSysAutoCodeResultForUpdate(ruleId, null, cycleKey);
            }
            else
            {
                result = sysAutoCodeResultService.selectSysAutoCodeResultForUpdate(ruleId, cycleKey, null);
            }
        }
        else
        {
            result = sysAutoCodeResultService.selectSysAutoCodeResultForUpdate(ruleId, null, null);
        }

        int newSerialNo;
        if (result != null && result.getLastSerialNo() != null)
        {
            newSerialNo = result.getLastSerialNo() + step;
            this.isNewCycle = false;
        }
        else
        {
            newSerialNo = startNo;
            this.isNewCycle = true;
        }
        this.existingResult = result;

        part.setSeriaNowNo(newSerialNo);

        int partLength = part.getPartLength() != null ? part.getPartLength() : 3;
        String format = "%0" + partLength + "d";
        return String.format(format, newSerialNo);
    }

    public SysAutoCodeResult getExistingResult() { return existingResult; }
    public boolean isNewCycle() { return isNewCycle; }

    @Override
    public String getPartType() { return PartTypeEnum.SERIALNO.getCode(); }
}
