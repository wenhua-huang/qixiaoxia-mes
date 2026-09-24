package com.ruoyi.system.service.mes.pro.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
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
    /** 绑定写入/更新工厂级粗锁：低频管理操作（单批最多 1000 对），工厂内串行最简单可靠 */
    private static final String LOCK_BIND_PREFIX = "mes:pro:userworkstation:bind:";

    private final ProUserWorkstationMapper proUserWorkstationMapper;
    private final ISysUserService userService;
    private final MdWorkstationMapper workstationMapper;
    private final RedisLockTemplate lockTemplate;
    private final TransactionTemplate txTemplate;

    public ProUserWorkstationServiceImpl(ProUserWorkstationMapper proUserWorkstationMapper,
            ISysUserService userService, MdWorkstationMapper workstationMapper,
            RedisLockTemplate lockTemplate, PlatformTransactionManager transactionManager) {
        this.proUserWorkstationMapper = proUserWorkstationMapper;
        this.userService = userService;
        this.workstationMapper = workstationMapper;
        this.lockTemplate = lockTemplate;
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

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
    public UserWorkstationBatchResult batchBind(UserWorkstationBatchRequest req) {
        // 先锁后事务：check-then-act 在工厂级锁内串行，无唯一索引下兜底「一对只许一行」
        return lockTemplate.execute(bindLockKey(),
                () -> txTemplate.execute(tx -> doBatchBind(req)));
    }

    private UserWorkstationBatchResult doBatchBind(UserWorkstationBatchRequest req) {
        if (req == null) {
            throw new ServiceException("请选择绑定人员");
        }
        // 先去重+剔空再校验：[null] 不得静默成 0/0/0
        List<Long> userIds = normalizeIds(req.getUserIds());
        List<Long> stationIds = normalizeIds(req.getWorkstationIds());
        validateRequest(userIds, stationIds, req.getRemark());
        List<SysUser> users = resolveUsers(userIds);
        List<MdWorkstation> stations = resolveStations(stationIds);
        UserWorkstationBatchResult result = new UserWorkstationBatchResult();
        for (SysUser user : users) {
            for (MdWorkstation station : stations) {
                bindOne(user, station, req.getRemark(), result);
            }
        }
        return result;
    }

    @Override
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
        UserWorkstationBatchResult r = batchBind(req);
        // 并发落败（锁外守卫通过、锁内查重已存在）：批处理 0 写入，单条语义仍按重复报错
        if (r.getSuccessCount() + r.getReactivatedCount() == 0) {
            throw new ServiceException("该用户已绑定此工位，请勿重复绑定");
        }
        return 1;
    }

    @Override
    public int updateProUserWorkstation(ProUserWorkstation e) {
        if (e.getRecordId() == null) throw new ServiceException("记录ID不能为空");
        // 所有更新统一进锁：是否改绑也以锁内重读的最新记录判定，消除锁外读旧值的 TOCTOU
        return lockTemplate.execute(bindLockKey(),
                () -> txTemplate.execute(tx -> doUpdate(e)));
    }

    private Integer doUpdate(ProUserWorkstation patch) {
        ProUserWorkstation current =
                proUserWorkstationMapper.selectProUserWorkstationByRecordId(patch.getRecordId());
        if (current == null) throw new ServiceException("绑定记录不存在");
        if (isPairChanging(patch, current)) {
            applyChangedPair(patch, current);
        }
        return applyUpdate(patch);
    }

    private boolean isPairChanging(ProUserWorkstation patch, ProUserWorkstation current) {
        return (patch.getUserId() != null && !patch.getUserId().equals(current.getUserId()))
                || (patch.getWorkstationId() != null && !patch.getWorkstationId().equals(current.getWorkstationId()));
    }

    private int applyUpdate(ProUserWorkstation e) {
        e.setUpdateTime(DateUtils.getNowDate());
        e.setUpdateBy(SecurityUtils.getUsername());
        return proUserWorkstationMapper.updateProUserWorkstation(e);
    }

    private static String bindLockKey() {
        return LOCK_BIND_PREFIX + SecurityUtils.getFactoryId();
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

    /** 剔空+去重；null 列表按空列表处理 */
    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    private void validateRequest(List<Long> userIds, List<Long> stationIds, String remark) {
        if (userIds.isEmpty()) {
            throw new ServiceException("请选择绑定人员");
        }
        if (stationIds.isEmpty()) {
            throw new ServiceException("请选择绑定工位");
        }
        if (userIds.size() > MAX_USERS) {
            throw new ServiceException("单次最多绑定 " + MAX_USERS + " 人");
        }
        if (stationIds.size() > MAX_WORKSTATIONS) {
            throw new ServiceException("单次最多绑定 " + MAX_WORKSTATIONS + " 个工位");
        }
        if (remark != null && remark.length() > REMARK_MAX) {
            throw new ServiceException("备注不能超过 " + REMARK_MAX + " 字");
        }
    }

    private List<SysUser> resolveUsers(List<Long> ids) {
        List<SysUser> users = new ArrayList<>();
        for (Long uid : ids) {
            SysUser u = userService.selectUserById(uid);
            if (u == null) throw new ServiceException("用户不存在：" + uid);
            users.add(u);
        }
        return users;
    }

    private List<MdWorkstation> resolveStations(List<Long> ids) {
        List<MdWorkstation> stations = new ArrayList<>();
        for (Long wid : ids) {
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

    /** 改绑（人/工位变化）：解析新名称并拦截与其他记录冲突（无论启用与否，一对只允许一行） */
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
                .anyMatch(x -> !x.getRecordId().equals(patch.getRecordId()));
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
}
