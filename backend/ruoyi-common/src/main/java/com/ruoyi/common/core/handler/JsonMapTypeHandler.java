package com.ruoyi.common.core.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import com.alibaba.fastjson2.JSON;

/**
 * MyBatis JSON TypeHandler：将 Java Map 与数据库 JSON 列互相转换。
 * <p>
 * 使用 fastjson2（项目已有依赖）做序列化/反序列化。
 * 可复用于采购行/销售行/工单等单据的 line_attrs JSON 列。
 * <p>
 * 用法（Mapper XML）：
 * <pre>
 * &lt;result property="lineAttrs" column="line_attrs"
 *   typeHandler="com.ruoyi.common.core.handler.JsonMapTypeHandler" /&gt;
 *
 * &lt;insert ...&gt;
 *   &lt;if test="lineAttrs != null"&gt;#{lineAttrs, typeHandler=com.ruoyi.common.core.handler.JsonMapTypeHandler},&lt;/if&gt;
 * &lt;/insert&gt;
 * </pre>
 *
 * @author qixiaoxia
 */
@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(Map.class)
public class JsonMapTypeHandler extends BaseTypeHandler<Map<String, Object>>
{
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Map<String, Object> parameter, JdbcType jdbcType)
            throws SQLException
    {
        ps.setString(i, JSON.toJSONString(parameter));
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, String columnName) throws SQLException
    {
        return parse(rs.getString(columnName));
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, int columnIndex) throws SQLException
    {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public Map<String, Object> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException
    {
        return parse(cs.getString(columnIndex));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String json)
    {
        if (json == null || json.isEmpty())
        {
            return null;
        }
        return JSON.parseObject(json, Map.class);
    }
}
