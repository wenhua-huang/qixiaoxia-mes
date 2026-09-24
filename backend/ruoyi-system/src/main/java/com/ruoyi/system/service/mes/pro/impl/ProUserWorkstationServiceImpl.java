package com.ruoyi.system.service.mes.pro.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.mes.md.MdWorkstation;
import com.ruoyi.system.mapper.mes.md.MdWorkstationMapper;
import com.ruoyi.system.mapper.mes.pro.ProUserWorkstationMapper;
import com.ruoyi.system.domain.mes.pro.ProUserWorkstation;
import com.ruoyi.system.domain.mes.pro.UserWorkstationBatchRequest;
import com.ruoyi.system.domain.mes.pro.UserWorkstationBatchResult;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.system.service.mes.pro.IProUserWorkstationService;

@Service
public class ProUserWorkstationServiceImpl implements IProUserWorkstationService
{
    private static final int MAX_USERS = 50;
    private static final int MAX_WORKSTATIONS = 20;
    private static final int REMARK_MAX = 500;

    @Autowired private ProUserWorkstationMapper proUserWorkstationMapper;
    @Autowired private ISysUserService userService;
    @Autowired private MdWorkstationMapper workstationMapper;

    @Override
    public ProUserWorkstation selectProUserWorkstationByRecordId(Long recordId) {
        return proUserWorkstationMapper.selectProUserWorkstationByRecordId(recordId);
    }

    @Override
    public List<ProUserWorkstation> selectProUserWorkstationList(ProUserWorkstation e) {
        return proUserWorkstationMapper.selectProUserWorkstationList(e);
    }

    @Override
    public List<ProUserWorkstation> selectAll() {
        ProUserWorkstation cond = new ProUserWorkstation();
        cond.setEnableFlag("1");
        return proUserWorkstationMapper.selectProUserWorkstationList(cond);
    }

    @Override
    public List<MdWorkstation> selectWorkstationOptions() {
        MdWorkstation cond = new MdWorkstation();
        cond.setEnableFlag("1");
        List<MdWorkstation> list = workstationMapper.selectMdWorkstationList(cond);
        list.sort(Comparator.comparing(MdWorkstation::getWorkstationCode,
                Comparator.nullsLast(String::compareTo)));
        return list;
    }

    @Override
    @Transactional
    public UserWorkstationBatchResult batchBind(UserWorkstationBatchRequest req) {
        validateRequest(req);
        List<SysUser> users = resolveUsers(req.getUserIds());
        List<MdWorkstation> stations = resolveStations(req.getWorkstationIds());
        UserWorkstationBatchResult result = new UserWorkstationBatchResult();
        for (SysUser user : users) {
            for (MdWorkstation station : stations) {
                bindOne(user, station, req.getRemark(), result);
            }
        }
        return result;
    }

    @Override
    @Transactional
    public int insertProUserWorkstation(ProUserWorkstation e) {
        if (e.getUserId() == null || e.getWorkstationId() == null) {
            throw new ServiceException("用户和工位不能为空");
        }
        // 单条新增语义：遇启用绑定必须报错（批量绑定则是跳过）
        boolean enabledExists = proUserWorkstationMapper
                .selectByUserAndWorkstation(e.getUserId(), e.getWorkstationId()).stream()
                .anyMatch(x -> "1".equals(x.getEnableFlag()));
        if (enabledExists) {
            throw new ServiceException("该用户已绑定此工位，请勿重复绑定");
        }
        UserWorkstationBatchRequest req = new UserWorkstationBatchRequest();
        req.setUserIds(List.of(e.getUserId()));
        req.setWorkstationIds(List.of(e.getWorkstationId()));
        req.setRemark(e.getRemark());
        batchBind(req);
        return 1;
    }

    @Override
    public int updateProUserWorkstation(ProUserWorkstation e) {
        if (e.getRecordId() == null) throw new ServiceException("记录ID不能为空");
        ProUserWorkstation old = proUserWorkstationMapper.selectProUserWorkstationByRecordId(e.getRecordId());
        if (old == null) throw new ServiceException("绑定记录不存在");

        boolean pairChanging = (e.getUserId() != null && !e.getUserId().equals(old.getUserId()))
                || (e.getWorkstationId() != null && !e.getWorkstationId().equals(old.getWorkstationId()));
        if (pairChanging) {
            applyChangedPair(e, old);
        }
        e.setUpdateTime(DateUtils.getNowDate());
        e.setUpdateBy(SecurityUtils.getUsername());
        return proUserWorkstationMapper.updateProUserWorkstation(e);
    }

    @Override
    public int deleteProUserWorkstationByRecordIds(Long[] recordIds) {
        return proUserWorkstationMapper.deleteProUserWorkstationByRecordIds(recordIds);
    }

    @Override
    public int deleteProUserWorkstationByRecordId(Long recordId) {
        return proUserWorkstationMapper.deleteProUserWorkstationByRecordId(recordId);
    }

    // ══════════ 私有 ══════════

    private void validateRequest(UserWorkstationBatchRequest req) {
        if (req == null || req.getUserIds() == null || req.getUserIds().isEmpty()) {
            throw new ServiceException("请选择绑定人员");
        }
        if (req.getWorkstationIds() == null || req.getWorkstationIds().isEmpty()) {
            throw new ServiceException("请选择绑定工位");
        }
        if (req.getUserIds().size() > MAX_USERS) {
            throw new ServiceException("单次最多绑定 " + MAX_USERS + " 人");
        }
        if (req.getWorkstationIds().size() > MAX_WORKSTATIONS) {
            throw new ServiceException("单次最多绑定 " + MAX_WORKSTATIONS + " 个工位");
        }
        if (req.getRemark() != null && req.getRemark().length() > REMARK_MAX) {
            throw new ServiceException("备注不能超过 " + REMARK_MAX + " 字");
        }
    }

    private List<SysUser> resolveUsers(List<Long> ids) {
        List<SysUser> users = new ArrayList<>();
        for (Long uid : distinct(ids)) {
            SysUser u = userService.selectUserById(uid);
            if (u == null) throw new ServiceException("用户不存在：" + uid);
            users.add(u);
        }
        return users;
    }

    private List<MdWorkstation> resolveStations(List<Long> ids) {
        List<MdWorkstation> stations = new ArrayList<>();
        for (Long wid : distinct(ids)) {
            MdWorkstation w = workstationMapper.selectMdWorkstationByWorkstationId(wid);
            if (w == null || !"1".equals(w.getEnableFlag())) {
                throw new ServiceException("工位不存在或已停用：" + wid);
            }
            stations.add(w);
        }
        return stations;
    }

    private void bindOne(SysUser user, MdWorkstation station, String remark, UserWorkstationBatchResult result) {
        List<ProUserWorkstation> exist =
                proUserWorkstationMapper.selectByUserAndWorkstation(user.getUserId(), station.getWorkstationId());
        ProUserWorkstation enabled = exist.stream().filter(x -> "1".equals(x.getEnableFlag())).findFirst().orElse(null);
        if (enabled != null) {
            result.incSkip();
            result.addSkip(user.getUserName() + " / " + station.getWorkstationName());
            return;
        }
        ProUserWorkstation target = exist.stream().filter(x -> "0".equals(x.getEnableFlag())).findFirst()
                .orElseGet(ProUserWorkstation::new);
        fillNames(target, user, station);
        target.setEnableFlag("1");
        target.setOperationTime(DateUtils.getNowDate());
        if (remark != null) target.setRemark(remark);

        if (target.getRecordId() == null) {
            target.setCreateBy(SecurityUtils.getUsername());
            target.setCreateTime(DateUtils.getNowDate());
            proUserWorkstationMapper.insertProUserWorkstation(target);
            result.incSuccess();
        } else {
            target.setUpdateBy(SecurityUtils.getUsername());
            target.setUpdateTime(DateUtils.getNowDate());
            proUserWorkstationMapper.updateProUserWorkstation(target);
            result.incReactivated();
        }
    }

    /** 改绑（人/工位变化）：解析新名称并拦截与其他启用记录冲突 */
    private void applyChangedPair(ProUserWorkstation patch, ProUserWorkstation old) {
        Long uid = patch.getUserId() != null ? patch.getUserId() : old.getUserId();
        Long wid = patch.getWorkstationId() != null ? patch.getWorkstationId() : old.getWorkstationId();
        SysUser user = userService.selectUserById(uid);
        if (user == null) throw new ServiceException("用户不存在：" + uid);
        MdWorkstation station = workstationMapper.selectMdWorkstationByWorkstationId(wid);
        if (station == null || !"1".equals(station.getEnableFlag())) {
            throw new ServiceException("工位不存在或已停用：" + wid);
        }
        boolean conflict = proUserWorkstationMapper.selectByUserAndWorkstation(uid, wid).stream()
                .anyMatch(x -> "1".equals(x.getEnableFlag()) && !x.getRecordId().equals(patch.getRecordId()));
        if (conflict) throw new ServiceException("该用户已绑定此工位，请勿重复绑定");
        patch.setUserId(uid);
        patch.setWorkstationId(wid);
        fillNames(patch, user, station);
    }

    private void fillNames(ProUserWorkstation e, SysUser user, MdWorkstation station) {
        e.setUserId(user.getUserId());
        e.setUserName(user.getUserName());
        e.setNickName(user.getNickName());
        e.setWorkstationId(station.getWorkstationId());
        e.setWorkstationCode(station.getWorkstationCode());
        e.setWorkstationName(station.getWorkstationName());
    }

    private List<Long> distinct(List<Long> ids) {
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }
}
