package com.ruoyi.framework.interceptor;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ruoyi.common.annotation.SkipFactoryId;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;

/**
 * 工厂ID自动注入拦截器 — 直接修改 SQL，不依赖参数类型。
 *
 * 拦截 StatementHandler.prepare（INSERT/UPDATE/DELETE）和
 * Executor.query（SELECT），对涉及 factory_id 列的表自动注入条件。
 *
 * 表匹配：qxx_* MES表 + sys_user + sys_dept。
 * 多表 JOIN 时自动提取主表别名，注入 &lt;alias&gt;.factory_id 消除歧义。
 *
 * @SkipFactoryId 可跳过。
 *
 * @author qixiaoxia
 */
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}),
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
public class FactoryIdInterceptor implements Interceptor
{
    private static final Logger log = LoggerFactory.getLogger(FactoryIdInterceptor.class);
    private static final Pattern FACTORY_ID_TABLE = Pattern.compile(
        "\\b(qxx_\\w+|sys_user|sys_user_post|sys_user_role|sys_dept|sys_role|sys_role_dept|sys_role_menu)\\b",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern FROM_ALIAS = Pattern.compile(
        "(?:from|join)\\s+(\\w+)(?:\\s+(?:as\\s+)?(\\w+))?", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALUES_BOUNDARY = Pattern.compile("\\)\\s*values\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Map<String, Boolean> SKIP_CACHE = new ConcurrentHashMap<>();

    @Override
    public Object intercept(Invocation invocation) throws Throwable
    {
        Object target = invocation.getTarget();
        boolean isSH = target instanceof StatementHandler;

        MappedStatement ms;
        BoundSql boundSql;
        Object parameter;

        if (isSH)
        {
            StatementHandler handler = (StatementHandler) target;
            boundSql = handler.getBoundSql();
            parameter = boundSql.getParameterObject();
            ms = findMappedStatement(handler);
        }
        else
        {
            ms = (MappedStatement) invocation.getArgs()[0];
            parameter = invocation.getArgs()[1];
            boundSql = (BoundSql) invocation.getArgs()[5];
        }

        if (parameter == null || ms == null || isSkipMethod(ms)) return invocation.proceed();

        String sql = boundSql.getSql();
        if (!FACTORY_ID_TABLE.matcher(sql).find()) return invocation.proceed();

        String tsql = sql.trim().toLowerCase();
        if (!tsql.startsWith("insert") && !tsql.startsWith("update")
                && !tsql.startsWith("delete") && !tsql.startsWith("select"))
            return invocation.proceed();

        // 避免 prepare 和 query 双重拦截
        if (isSH && tsql.startsWith("select")) return invocation.proceed();
        if (!isSH && !tsql.startsWith("select")) return invocation.proceed();

        Long factoryId;
        try { factoryId = SecurityUtils.getFactoryId(); }
        catch (ServiceException e) { return invocation.proceed(); }  // 未登录 → 跳过注入
        // 其他异常（如 DB 故障）向上抛，不让 SQL 在无 factory_id 隔离的情况下执行

        // 提取主表别名，多表 JOIN 时消除 factory_id 歧义
        String alias = getTableAlias(sql);
        String fkCol = alias.isEmpty() ? "factory_id" : alias + ".factory_id";

        BoundSql newBs = injectFactoryIdIntoSql(boundSql, ms, sql, factoryId, fkCol);

        if (isSH)
        {
            Object delegate = getField(target, "delegate");
            setField(delegate != null ? delegate : target, "boundSql", newBs);
        }
        else
        {
            invocation.getArgs()[5] = newBs;
        }
        return invocation.proceed();
    }

    // ---- 别名提取 ----

    /** 从 FROM/JOIN 子句中找第一个有 factory_id 列的表的限定名（别名或表名）。visible for testing */
    String getTableAlias(String sql)
    {
        String s = sql.replaceAll("\\s+", " ");
        java.util.regex.Matcher m = FROM_ALIAS.matcher(s);
        String fallbackTable = null;
        while (m.find())
        {
            String table = m.group(1);
            if (FACTORY_ID_TABLE.matcher(table).matches())
            {
                String alias = m.group(2);
                if (alias != null && !alias.isEmpty() && !isSqlKeyword(alias))
                    return alias;                  // 有效别名 → alias.factory_id
                if (alias == null || alias.isEmpty())
                    return table;                  // 无别名 → table_name.factory_id
                fallbackTable = table;             // 关键字别名 → 记下，继续找
            }
        }
        return fallbackTable != null ? fallbackTable : "";  // 全部是关键字别名 → 用最后一个表名
    }

    boolean isSqlKeyword(String w)   // visible for testing
    {
        String kw = w.toLowerCase();
        return kw.equals("where") || kw.equals("limit") || kw.equals("order")
            || kw.equals("group") || kw.equals("having") || kw.equals("join")
            || kw.equals("left") || kw.equals("right") || kw.equals("inner")
            || kw.equals("outer") || kw.equals("on") || kw.equals("and")
            || kw.equals("or") || kw.equals("set") || kw.equals("values")
            || kw.equals("select") || kw.equals("from") || kw.equals("update")
            || kw.equals("delete") || kw.equals("insert") || kw.equals("into");
    }

    // ---- 反射工具 ----

    private MappedStatement findMappedStatement(StatementHandler handler)
    {
        Object ms = getField(handler, "mappedStatement");
        if (ms instanceof MappedStatement) return (MappedStatement) ms;
        Object d = getField(handler, "delegate");
        if (d != null)
        {
            ms = getField(d, "mappedStatement");
            if (ms instanceof MappedStatement) return (MappedStatement) ms;
        }
        return null;
    }

    private Object getField(Object obj, String name)
    {
        Class<?> c = obj.getClass();
        while (c != null && c != Object.class)
        {
            try
            {
                java.lang.reflect.Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(obj);
            }
            catch (NoSuchFieldException e) { c = c.getSuperclass(); }
            catch (Exception e) { return null; }
        }
        return null;
    }

    private void setField(Object obj, String name, Object value)
    {
        Class<?> c = obj.getClass();
        while (c != null && c != Object.class)
        {
            try
            {
                java.lang.reflect.Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                f.set(obj, value);
                return;
            }
            catch (NoSuchFieldException e) { c = c.getSuperclass(); }
            catch (Exception e) { return; }
        }
    }

    // ---- SQL 注入 ----

    private BoundSql injectFactoryIdIntoSql(BoundSql original, MappedStatement ms,
                                             String sql, Long factoryId, String fkCol)
    {
        // INSERT 列名不能带表前缀，始终用 factory_id
        String newSql = (ms.getSqlCommandType() == SqlCommandType.INSERT)
                ? injectInsert(sql, "factory_id") : injectWhere(sql, fkCol);

        if (newSql == sql) return original;

        newSql = newSql.replace("__FACTORY_ID__", factoryId.toString());

        BoundSql newBs = new BoundSql(ms.getConfiguration(), newSql,
                original.getParameterMappings(), original.getParameterObject());
        for (Map.Entry<String, Object> e : original.getAdditionalParameters().entrySet())
            newBs.setAdditionalParameter(e.getKey(), e.getValue());
        return newBs;
    }

    /** INSERT: (a,b) values (?,?) → (a,b,factory_id) values (?,?,1)。visible for testing */
    String injectInsert(String sql, String fkCol)
    {
        String s = sql.replaceAll("\\s+", " ").trim();
        java.util.regex.Matcher m = VALUES_BOUNDARY.matcher(s);
        if (!m.find()) return sql;
        int vi = m.start();

        // 列：(a,b) → (a,b,factory_id)
        String cols = s.substring(0, vi + 1).replaceFirst("\\)\\s*$", ", " + fkCol + " )");
        // 值：每行的 ) 后（跟 , 或末尾）追加 factory_id
        // values (?,?) , (?,?) → values (?,?,1) , (?,?,1)
        String vals = s.substring(vi + 1);
        vals = vals.replaceAll("\\)(?=\\s*(?:,|$))", ", __FACTORY_ID__ )");
        return cols + vals;
    }

    /** SELECT/UPDATE/DELETE: WHERE ... → WHERE ... AND <alias>.factory_id = 1。visible for testing */
    String injectWhere(String sql, String fkCol)
    {
        String s = sql.replaceAll("\\s+", " ").trim();
        String lower = s.toLowerCase();
        int wi = lower.lastIndexOf("where ");

        if (wi >= 0)
        {
            int at = findInsertPoint(s, wi + 6);
            if (at < 0) at = s.length();
            return s.substring(0, at) + " AND " + fkCol + " = __FACTORY_ID__ " + s.substring(at);
        }
        else
        {
            int at = findWhereInsertPoint(s);
            if (at < 0) return sql;
            return s.substring(0, at) + " WHERE " + fkCol + " = __FACTORY_ID__ " + s.substring(at);
        }
    }

    /**
     * 从 WHERE 之后找到 AND 注入点。先找 ORDER BY/LIMIT 等关键字；找不到时从末尾倒查
     * 第一个非 IN(?) 闭括号的 `)`（子查询结束符），向前插入。覆盖所有现有 Mapper SQL 场景。
     */
    private int findInsertPoint(String s, int start)
    {
        String lower = s.toLowerCase();
        int min = Integer.MAX_VALUE;
        for (String kw : new String[]{"order by", "group by", "limit ", "for update"})
        {
            int idx = lower.indexOf(kw, start);
            if (idx > 0 && idx < min) min = idx;
        }
        // 从末尾倒查第一个 )（排除 IN (?) / func(?) 的闭括号）
        int idx = s.length();
        while (--idx > start)
        {
            if (s.charAt(idx) == ')')
            {
                int b = idx - 1;
                while (b > start && Character.isWhitespace(s.charAt(b))) b--;
                if (b > start && s.charAt(b) == '?') { idx = b; continue; } // 跳过 ? 直接前导的 )
                if (idx < min) min = idx;
                break;
            }
        }
        return min == Integer.MAX_VALUE ? -1 : min;
    }

    private int findWhereInsertPoint(String s)
    {
        String lower = s.toLowerCase();
        int fromIdx = lower.lastIndexOf(" from ");
        if (fromIdx < 0) return s.length();
        int min = Integer.MAX_VALUE;
        for (String kw : new String[]{"order by", "group by", "limit ", "for update"})
        {
            int idx = lower.indexOf(kw, fromIdx + 6);
            if (idx > 0 && idx < min) min = idx;
        }
        return min == Integer.MAX_VALUE ? s.length() : min;
    }

    // ---- @SkipFactoryId ----

    private boolean isSkipMethod(MappedStatement ms)
    {
        String sid = ms.getId();
        return SKIP_CACHE.computeIfAbsent(sid, id -> {
            try
            {
                int dot = id.lastIndexOf('.');
                Class<?> c = Class.forName(id.substring(0, dot));
                String mn = id.substring(dot + 1);
                for (Method m : c.getDeclaredMethods())
                    if (m.getName().equals(mn) && m.isAnnotationPresent(SkipFactoryId.class))
                        return true;
            }
            catch (Exception ignored) {}
            return false;
        });
    }

    @Override
    public Object plugin(Object target) { return Plugin.wrap(target, this); }
}
