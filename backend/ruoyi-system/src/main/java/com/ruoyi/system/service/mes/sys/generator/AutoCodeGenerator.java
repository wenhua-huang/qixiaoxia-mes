package com.ruoyi.system.service.mes.sys.generator;

import java.util.List;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.sys.SysAutoCodePart;
import com.ruoyi.system.domain.mes.sys.SysAutoCodeResult;
import com.ruoyi.system.domain.mes.sys.SysAutoCodeRule;
import com.ruoyi.system.service.mes.sys.ISysAutoCodePartService;
import com.ruoyi.system.service.mes.sys.ISysAutoCodeResultService;
import com.ruoyi.system.service.mes.sys.ISysAutoCodeRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 自动编码生成器 — 编排整个编码生成流程
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
@Service
public class AutoCodeGenerator
{
    @Autowired
    private ISysAutoCodeRuleService ruleService;

    @Autowired
    private ISysAutoCodePartService partService;

    @Autowired
    private ISysAutoCodeResultService resultService;

    @Autowired
    private PartTypeHandlerDispatcher dispatcher;

    /**
     * 生成流水编码（线程安全）
     *
     * @param ruleCode       规则编码
     * @param inputCharacter 传入字符（INPUTCHAR 和 OTHER 循环模式时使用，可为 null）
     * @return 完整编码字符串
     */
    public synchronized String genSerialCode(String ruleCode, String inputCharacter)
    {
        // 1. 查规则
        SysAutoCodeRule rule = ruleService.selectSysAutoCodeRuleByRuleCode(ruleCode);
        if (rule == null)
        {
            throw new RuntimeException("编码规则[" + ruleCode + "]不存在！");
        }
        if (!"1".equals(rule.getEnableFlag()))
        {
            throw new RuntimeException("编码规则[" + ruleCode + "]已停用！");
        }

        // 2. 查分段列表（按 part_index 排序）
        List<SysAutoCodePart> parts = partService.selectSysAutoCodePartByRuleId(rule.getRuleId());
        if (parts == null || parts.isEmpty())
        {
            throw new RuntimeException("编码规则[" + ruleCode + "]未配置分段！");
        }

        // 校验 SERIALNO 分段数量
        long serialCount = parts.stream().filter(p -> "SERIALNO".equals(p.getPartType())).count();
        if (serialCount == 0)
        {
            throw new RuntimeException("编码规则[" + ruleCode + "]缺少流水号(SERIALNO)分段！");
        }
        if (serialCount > 1)
        {
            throw new RuntimeException("编码规则[" + ruleCode + "]配置了多个流水号分段，最多允许一个！");
        }

        // 3. 遍历分段，生成各部分字符串
        StringBuilder sb = new StringBuilder();
        PartTypeSerialNoHandler serialHandler = null;

        for (SysAutoCodePart part : parts)
        {
            part.setInputCharacter(inputCharacter);
            part.setRuleId(rule.getRuleId());

            String partResult = dispatcher.choiceExecute(part);
            sb.append(partResult);

            if ("SERIALNO".equals(part.getPartType()))
            {
                serialHandler = (PartTypeSerialNoHandler) dispatcher.getHandler("SERIALNO");
            }
        }

        String rawCode = sb.toString();

        // 4. 补齐
        String finalCode = padCode(rawCode, rule);

        // 5. 记录结果到 sys_auto_code_result
        if (serialHandler != null)
        {
            saveResult(rule.getRuleId(), finalCode, parts, serialHandler);
        }

        return finalCode;
    }

    /**
     * 补齐编码（仅使用 padChar 的首字符进行补齐，避免多字符 padChar 溢出 maxLength）
     */
    private String padCode(String rawCode, SysAutoCodeRule rule)
    {
        if (!"1".equals(rule.getIsPadded()) || rule.getMaxLength() == null)
        {
            return rawCode;
        }
        int maxLen = rule.getMaxLength();
        if (rawCode.length() >= maxLen)
        {
            return rawCode;
        }
        String padCharStr = rule.getPaddedChar() != null && !rule.getPaddedChar().isEmpty()
                ? rule.getPaddedChar() : "0";
        char padChar = padCharStr.charAt(0);
        int padCount = maxLen - rawCode.length();
        StringBuilder padding = new StringBuilder();
        for (int i = 0; i < padCount; i++)
        {
            padding.append(padChar);
        }
        if ("R".equals(rule.getPaddedMethod()))
        {
            return rawCode + padding.toString();
        }
        else
        {
            return padding.toString() + rawCode;
        }
    }

    /**
     * 保存生成结果
     */
    private void saveResult(Long ruleId, String finalCode, List<SysAutoCodePart> parts,
                            PartTypeSerialNoHandler serialHandler)
    {
        SysAutoCodePart serialPart = parts.stream()
                .filter(p -> "SERIALNO".equals(p.getPartType()))
                .findFirst().orElse(null);
        if (serialPart == null) return;

        Integer currentSerialNo = serialPart.getSeriaNowNo();
        SysAutoCodeResult existingResult = serialHandler.getExistingResult();
        boolean isNewCycle = serialHandler.isNewCycle();

        if (existingResult != null && !isNewCycle)
        {
            existingResult.setLastSerialNo(currentSerialNo);
            existingResult.setLastResult(finalCode);
            existingResult.setGenIndex(existingResult.getGenIndex() != null
                    ? existingResult.getGenIndex() + 1 : 1);
            existingResult.setLastInputChar(serialPart.getInputCharacter());
            resultService.updateSysAutoCodeResult(existingResult);
        }
        else
        {
            SysAutoCodeResult newResult = new SysAutoCodeResult();
            newResult.setRuleId(ruleId);
            newResult.setGenDate(CodeGenHelper.computeCycleKey(
                    serialPart.getCycleFlag() != null && "1".equals(serialPart.getCycleFlag())
                            ? serialPart.getCycleMethod() : "DAY",
                    serialPart.getInputCharacter()));
            newResult.setGenIndex(1);
            newResult.setLastSerialNo(currentSerialNo);
            newResult.setLastResult(finalCode);
            newResult.setLastInputChar(serialPart.getInputCharacter());
            try
            {
                newResult.setCreateBy(SecurityUtils.getUsername());
            }
            catch (Exception e)
            {
                newResult.setCreateBy("system");
            }
            resultService.insertSysAutoCodeResult(newResult);
        }
    }
}
