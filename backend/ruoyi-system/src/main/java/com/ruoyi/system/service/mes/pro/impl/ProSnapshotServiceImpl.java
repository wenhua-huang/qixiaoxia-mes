package com.ruoyi.system.service.mes.pro.impl;

import java.util.*;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pro.*;
import com.ruoyi.system.mapper.mes.pro.*;
import com.ruoyi.system.service.mes.pro.IProSnapshotService;
import static com.ruoyi.system.domain.mes.pro.ProConstants.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProSnapshotServiceImpl implements IProSnapshotService
{
    @Autowired private ProSnapshotMapper mapper;
    @Autowired private ProTaskMapper taskMapper;

    @Override public ProSnapshot selectById(Long id) { return mapper.selectProSnapshotById(id); }
    @Override public List<ProSnapshot> selectList(ProSnapshot q) { return mapper.selectProSnapshotList(q); }

    @Override
    public int insert(ProSnapshot s) {
        s.setStatus(SNAPSHOT_DRAFT);
        s.setCreateTime(DateUtils.getNowDate());
        s.setCreateBy(SecurityUtils.getUsername());
        return mapper.insertProSnapshot(s);
    }

    @Transactional
    public Long createWithTasks(Long workorderId, String name) {
        ProTask q = new ProTask(); q.setWorkorderId(workorderId);
        List<ProTask> tasks = taskMapper.selectProTaskList(q);
        if (tasks.isEmpty()) return -1L;

        // 防重：与最近一条同工单DRAFT快照比较
        String json = buildCompactJson(tasks);
        ProSnapshot lastQ = new ProSnapshot(); lastQ.setScopeId(workorderId); lastQ.setStatus(SNAPSHOT_DRAFT);
        List<ProSnapshot> recentList = mapper.selectProSnapshotList(lastQ);
        if (!recentList.isEmpty() && json.equals(recentList.get(0).getRemark())) {
            return -2L; // 数据未变
        }

        ProSnapshot s = new ProSnapshot();
        s.setName(name != null ? name : ("快照 " + DateUtils.getNowDate()));
        s.setScopeType("WORKORDER"); s.setScopeId(workorderId);
        s.setRemark(json);
        s.setStatus(SNAPSHOT_DRAFT);
        s.setCreateTime(DateUtils.getNowDate());
        s.setCreateBy(SecurityUtils.getUsername());
        mapper.insertProSnapshot(s);
        return s.getId();
    }

    @Override @Transactional
    public int publish(Long id) {
        ProSnapshot s = mapper.selectProSnapshotById(id);
        if (s == null) return 0;
        if (SNAPSHOT_PUBLISHED.equals(s.getStatus())) return -1;
        if (SNAPSHOT_DISCARDED.equals(s.getStatus())) return -2;
        if (s.getRemark() == null) return 0;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> saved = (List<Map<String, Object>>) (List<?>) JSON.parseArray(s.getRemark(), Map.class);
        if (saved == null) return 0;
        for (Map<String, Object> st : saved) {
            Long taskId = st.get("id") != null ? Long.valueOf(st.get("id").toString()) : null;
            if (taskId == null) continue;
            ProTask db = taskMapper.selectProTaskByTaskId(taskId);
            if (db != null && st.get("s") != null && st.get("e") != null) {
                db.setStartTime(new Date(((Number) st.get("s")).longValue()));
                db.setEndTime(new Date(((Number) st.get("e")).longValue()));
                if (st.get("d") != null) db.setDuration(((Number) st.get("d")).intValue());
                db.setUpdateTime(DateUtils.getNowDate());
                taskMapper.updateProTask(db);
            }
        }
        s.setStatus(SNAPSHOT_PUBLISHED); s.setUpdateTime(DateUtils.getNowDate());
        return mapper.updateProSnapshot(s);
    }

    @Override public int discard(Long id) {
        ProSnapshot s = mapper.selectProSnapshotById(id);
        if (s != null && SNAPSHOT_PUBLISHED.equals(s.getStatus())) return -1; // 已发布不可废弃
        ProSnapshot u = new ProSnapshot(); u.setId(id);
        u.setStatus(SNAPSHOT_DISCARDED); u.setUpdateTime(DateUtils.getNowDate());
        return mapper.updateProSnapshot(u);
    }

    @Override public int deleteByIds(Long[] ids) { return mapper.deleteProSnapshotByIds(ids); }

    /** 压缩任务数据为JSON（仅存 id/s/e/d） */
    private String buildCompactJson(List<ProTask> tasks) {
        List<Map<String, Object>> compact = new ArrayList<>();
        for (ProTask t : tasks) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", t.getTaskId());
            item.put("s", t.getStartTime() != null ? t.getStartTime().getTime() : null);
            item.put("e", t.getEndTime() != null ? t.getEndTime().getTime() : null);
            item.put("d", t.getDuration());
            compact.add(item);
        }
        return JSON.toJSONString(compact);
    }
}
