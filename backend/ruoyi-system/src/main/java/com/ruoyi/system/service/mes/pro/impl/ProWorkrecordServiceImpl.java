package com.ruoyi.system.service.mes.pro.impl;

import java.util.Date;
import java.util.List;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.mes.md.MdWorkstation;
import com.ruoyi.system.domain.mes.pro.ProConstants;
import com.ruoyi.system.domain.mes.pro.ProTask;
import com.ruoyi.system.domain.mes.pro.ProUserWorkstation;
import com.ruoyi.system.domain.mes.pro.ProWorkrecord;
import com.ruoyi.system.mapper.mes.md.MdWorkstationMapper;
import com.ruoyi.system.mapper.mes.pro.ProTaskMapper;
import com.ruoyi.system.mapper.mes.pro.ProUserWorkstationMapper;
import com.ruoyi.system.mapper.mes.pro.ProWorkrecordMapper;
import com.ruoyi.system.service.mes.pro.IProWorkrecordService;

/**
 * 上下工会话记录Service业务层
 * <p>
 * 会话模式：上工 INSERT(ACTIVE)，下工 UPDATE 同一条(CLOSED + clock_out_time + work_duration)
 * 一人一岗：同一用户同时只能有一个在岗会话
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@Service
public class ProWorkrecordServiceImpl implements IProWorkrecordService
{
    private static final Logger log = LoggerFactory.getLogger(ProWorkrecordServiceImpl.class);

    @Autowired private ProWorkrecordMapper proWorkrecordMapper;
    @Autowired private ProUserWorkstationMapper userWorkstationMapper;
    @Autowired private MdWorkstationMapper mdWorkstationMapper;
    @Autowired private ProTaskMapper proTaskMapper;
    @Autowired private RedisLockTemplate lockTemplate;

    // ════════════════════════════════════════════
    // 业务方法：上工 / 下工
    // ════════════════════════════════════════════

    /** 上工打卡 */
    @Override
    public int clockIn(ProWorkrecord e) {
        // 注入当前用户身份（与报工一致，后端兜底）
        Long userId = SecurityUtils.getUserId();
        e.setUserId(userId);
        e.setUserName(SecurityUtils.getUsername());
        e.setNickName(SecurityUtils.getLoginUser().getUser().getNickName());
        Date now = DateUtils.getNowDate();
        e.setClockInTime(now);
        e.setStatus(ProConstants.WORKRECORD_ACTIVE);
        e.setCreateTime(now);
        e.setCreateBy(SecurityUtils.getUsername());

        // 冗余字段反查（工位编码名称 / 任务工序）
        fillRedundantFields(e);

        // Redis 锁（按用户）防重复上工
        return lockTemplate.execute("pro:clock:" + userId, () -> doClockIn(e, userId));
    }

    /** 下工结算 */
    @Override
    public int clockOut(ProWorkrecord e) {
        Long userId = SecurityUtils.getUserId();
        Date now = DateUtils.getNowDate();
        String username = SecurityUtils.getUsername();
        return lockTemplate.execute("pro:clock:" + userId, () -> doClockOut(e, userId, now, username));
    }

    /** 查当前用户的在岗会话（移动端进页面判按钮状态） */
    @Override
    public ProWorkrecord selectActiveSession() {
        ProWorkrecord q = newActiveQuery(SecurityUtils.getUserId());
        return q.getUserId() == null ? null : proWorkrecordMapper.selectActiveByUser(q);
    }

    /** 查当前用户的绑定工位（绑定优先，快捷选择） */
    @Override
    public List<ProUserWorkstation> selectMyWorkstations() {
        ProUserWorkstation q = new ProUserWorkstation();
        q.setUserId(SecurityUtils.getUserId());
        q.setEnableFlag("1");
        return userWorkstationMapper.selectProUserWorkstationList(q);
    }

    /** 查当前登录用户的打卡历史（固定按 userId 过滤，复用标准列表查询） */
    @Override
    public List<ProWorkrecord> selectMyHistory(ProWorkrecord e) {
        e.setUserId(SecurityUtils.getUserId());
        return proWorkrecordMapper.selectProWorkrecordList(e);
    }

    // ════════════════════════════════════════════
    // 锁内业务（私有）
    // ════════════════════════════════════════════

    private int doClockIn(ProWorkrecord e, Long userId) {
        // 防重：一人一岗
        ProWorkrecord active = proWorkrecordMapper.selectActiveByUser(newActiveQuery(userId));
        if (active != null) {
            throw new ServiceException("您已在[" + safe(active.getWorkstationName()) + "]上工，请先下工");
        }
        log.info("上工打卡 userId={}, workstationId={}, taskId={}", userId, e.getWorkstationId(), e.getTaskId());
        return proWorkrecordMapper.insertProWorkrecord(e);
    }

    private int doClockOut(ProWorkrecord e, Long userId, Date now, String username) {
        ProWorkrecord session = proWorkrecordMapper.selectActiveByUser(newActiveQuery(userId));
        if (session == null) {
            throw new ServiceException("没有进行中的上工记录，无需下工");
        }
        int minutes = (int) ((now.getTime() - session.getClockInTime().getTime()) / 60000L);
        session.setClockOutTime(now);
        session.setWorkDuration(Math.max(0, minutes));
        session.setStatus(ProConstants.WORKRECORD_CLOSED);
        session.setUpdateBy(username);
        session.setUpdateTime(now);
        log.info("下工打卡 userId={}, workstationId={}, duration={}min", userId, session.getWorkstationId(), minutes);
        return proWorkrecordMapper.closeSession(session);
    }

    // ════════════════════════════════════════════
    // 辅助（私有）
    // ════════════════════════════════════════════

    /** 冗余字段反查：工位编码名称、任务工序（与报工 autoFillCodes 同模式） */
    private void fillRedundantFields(ProWorkrecord e) {
        if (e.getWorkstationId() != null) {
            MdWorkstation ws = mdWorkstationMapper.selectMdWorkstationByWorkstationId(e.getWorkstationId());
            if (ws != null) {
                e.setWorkstationCode(ws.getWorkstationCode());
                e.setWorkstationName(ws.getWorkstationName());
            }
        }
        if (e.getTaskId() != null) {
            ProTask task = proTaskMapper.selectProTaskByTaskId(e.getTaskId());
            if (task != null) {
                e.setTaskCode(task.getTaskCode());
                if (e.getWorkstationId() == null && task.getWorkstationId() != null) {
                    e.setWorkstationId(task.getWorkstationId());
                    e.setWorkstationCode(task.getWorkstationCode());
                    e.setWorkstationName(task.getWorkstationName());
                }
                if (e.getProcessName() == null) {
                    e.setProcessName(task.getProcessName());
                }
            }
        }
    }

    private ProWorkrecord newActiveQuery(Long userId) {
        ProWorkrecord q = new ProWorkrecord();
        q.setUserId(userId);
        return q;
    }

    private String safe(String s) { return s == null ? "" : s; }

    // ════════════════════════════════════════════
    // 标准 CRUD（PC 端管理用）
    // ════════════════════════════════════════════

    @Override
    public ProWorkrecord selectProWorkrecordByRecordId(Long recordId) {
        return proWorkrecordMapper.selectProWorkrecordByRecordId(recordId);
    }

    @Override
    public List<ProWorkrecord> selectProWorkrecordList(ProWorkrecord e) {
        return proWorkrecordMapper.selectProWorkrecordList(e);
    }

    @Override
    public List<ProWorkrecord> selectAll() {
        return proWorkrecordMapper.selectProWorkrecordList(new ProWorkrecord());
    }

    @Override
    public int insertProWorkrecord(ProWorkrecord e) {
        e.setCreateTime(DateUtils.getNowDate());
        e.setCreateBy(SecurityUtils.getUsername());
        return proWorkrecordMapper.insertProWorkrecord(e);
    }

    @Override
    public int updateProWorkrecord(ProWorkrecord e) {
        e.setUpdateTime(DateUtils.getNowDate());
        e.setUpdateBy(SecurityUtils.getUsername());
        return proWorkrecordMapper.updateProWorkrecord(e);
    }

    @Override
    public int deleteProWorkrecordByRecordIds(Long[] recordIds) {
        return proWorkrecordMapper.deleteProWorkrecordByRecordIds(recordIds);
    }

    @Override
    public int deleteProWorkrecordByRecordId(Long recordId) {
        return proWorkrecordMapper.deleteProWorkrecordByRecordId(recordId);
    }
}
