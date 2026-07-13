package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdWorkstation;
import com.ruoyi.system.domain.mes.pro.ProUserWorkstation;
import com.ruoyi.system.domain.mes.pro.ProWorkrecord;

/**
 * 上下工会话记录Service接口
 * <p>
 * 会话模式：上工 INSERT(status=ACTIVE)，下工 UPDATE 同一条(status=CLOSED)
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public interface IProWorkrecordService
{
    /** 上工打卡：注入当前用户 + 防重(一人一岗) + 工位绑定软校验 + 冗余字段反查 + INSERT */
    public int clockIn(ProWorkrecord e);

    /** 下工结算：查在岗会话 → 算工时 → UPDATE status=CLOSED */
    public int clockOut(ProWorkrecord e);

    /** 查当前登录用户的在岗会话（移动端进页面判上工/下工按钮） */
    public ProWorkrecord selectActiveSession();

    /** 查当前登录用户的绑定工位（绑定优先，快捷选择） */
    public List<ProUserWorkstation> selectMyWorkstations();

    /** 查当前登录用户的打卡历史（移动端，固定按 userId 过滤，支持 status/时间范围） */
    public List<ProWorkrecord> selectMyHistory(ProWorkrecord e);

    /** 按编码查工作站（移动端扫码/手输编码后解析真实工位，绑定优先不强制） */
    public MdWorkstation resolveWorkstationByCode(String workstationCode);

    /** 标准 CRUD（PC 端管理用） */
    public ProWorkrecord selectProWorkrecordByRecordId(Long recordId);
    public List<ProWorkrecord> selectProWorkrecordList(ProWorkrecord e);
    public List<ProWorkrecord> selectAll();
    public int insertProWorkrecord(ProWorkrecord e);
    public int updateProWorkrecord(ProWorkrecord e);
    public int deleteProWorkrecordByRecordIds(Long[] recordIds);
    public int deleteProWorkrecordByRecordId(Long recordId);
}
