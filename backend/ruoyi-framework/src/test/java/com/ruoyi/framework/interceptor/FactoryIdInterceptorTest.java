package com.ruoyi.framework.interceptor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FactoryIdInterceptor 单元测试 — 测试 SQL 注入、别名提取、关键字过滤等核心逻辑。
 */
@DisplayName("FactoryIdInterceptor 单元测试")
class FactoryIdInterceptorTest {

    private final FactoryIdInterceptor interceptor = new FactoryIdInterceptor();

    // ==================== 别名提取 ====================

    @Nested
    @DisplayName("getTableAlias")
    class GetTableAlias {

        @Test
        @DisplayName("有别名 → 返回别名")
        void shouldReturnAlias() {
            String sql = "SELECT * FROM qxx_md_unit_measure u WHERE u.id = ?";
            assertThat(interceptor.getTableAlias(sql)).isEqualTo("u");
        }

        @Test
        @DisplayName("无别名 → 返回表名")
        void shouldReturnTableName() {
            String sql = "SELECT * FROM qxx_md_unit_measure WHERE unit_id = ?";
            // 单表无别名，且 WHERE 不被当作别名 → 返回表名
            assertThat(interceptor.getTableAlias(sql)).isEqualTo("qxx_md_unit_measure");
        }

        @Test
        @DisplayName("JOIN 多表 → 返回第一个匹配表的别名")
        void shouldReturnFirstMatchingAlias() {
            String sql = "SELECT * FROM sys_role r LEFT JOIN sys_user_role ur ON r.role_id = ur.role_id";
            assertThat(interceptor.getTableAlias(sql)).isEqualTo("r");
        }

        @Test
        @DisplayName("sys_user LEFT JOIN sys_dept → 返回 u")
        void shouldReturnUserAliasForJoin() {
            String sql = "SELECT * FROM sys_user u LEFT JOIN sys_dept d ON u.dept_id = d.dept_id WHERE u.id = ?";
            assertThat(interceptor.getTableAlias(sql)).isEqualTo("u");
        }

        @Test
        @DisplayName("子查询 from sys_dept where 被跳过 → 匹配主 from")
        void shouldSkipSubqueryWhereKeyword() {
            String sql = "SELECT d.*, (SELECT name FROM sys_dept WHERE id = d.parent_id) pn FROM sys_dept d WHERE d.id = ?";
            assertThat(interceptor.getTableAlias(sql)).isEqualTo("d");
        }

        @Test
        @DisplayName("非 qxx_ 表 → 返回空")
        void shouldReturnEmptyForNonFactoryTable() {
            String sql = "SELECT * FROM sys_config c WHERE c.config_id = ?";
            assertThat(interceptor.getTableAlias(sql)).isEmpty();
        }
    }

    // ==================== SQL 关键字过滤 ====================

    @Nested
    @DisplayName("isSqlKeyword")
    class IsSqlKeyword {

        @Test
        @DisplayName("SQL 关键字返回 true")
        void shouldReturnTrueForKeywords() {
            assertThat(interceptor.isSqlKeyword("where")).isTrue();
            assertThat(interceptor.isSqlKeyword("limit")).isTrue();
            assertThat(interceptor.isSqlKeyword("order")).isTrue();
            assertThat(interceptor.isSqlKeyword("group")).isTrue();
            assertThat(interceptor.isSqlKeyword("select")).isTrue();
            assertThat(interceptor.isSqlKeyword("from")).isTrue();
            assertThat(interceptor.isSqlKeyword("join")).isTrue();
        }

        @Test
        @DisplayName("表别名返回 false")
        void shouldReturnFalseForAliases() {
            assertThat(interceptor.isSqlKeyword("u")).isFalse();
            assertThat(interceptor.isSqlKeyword("d")).isFalse();
            assertThat(interceptor.isSqlKeyword("ur")).isFalse();
            assertThat(interceptor.isSqlKeyword("t")).isFalse();
        }
    }

    // ==================== injectWhere ====================

    @Nested
    @DisplayName("injectWhere")
    class InjectWhere {

        @Test
        @DisplayName("有 WHERE → 追加 AND factory_id")
        void shouldAppendAndToExistingWhere() {
            String sql = "SELECT * FROM qxx_md_unit_measure WHERE unit_code = ?";
            String result = interceptor.injectWhere(sql, "factory_id");
            assertThat(result).contains("AND factory_id = __FACTORY_ID__");
            assertThat(result).startsWith("SELECT");
        }

        @Test
        @DisplayName("无 WHERE → 新增 WHERE")
        void shouldAddWhereClause() {
            String sql = "SELECT * FROM qxx_md_unit_measure";
            String result = interceptor.injectWhere(sql, "factory_id");
            assertThat(result).contains("WHERE factory_id = __FACTORY_ID__");
        }

        @Test
        @DisplayName("有 LIMIT → AND 在 LIMIT 之前")
        void shouldInsertBeforeLimit() {
            String sql = "SELECT * FROM qxx_md_unit_measure WHERE unit_code = ? LIMIT ?";
            String result = interceptor.injectWhere(sql, "factory_id");
            assertThat(result).contains("AND factory_id = __FACTORY_ID__");
            assertThat(result).contains("LIMIT ?");
            int andPos = result.indexOf("AND factory_id");
            int limitPos = result.indexOf("LIMIT");
            assertThat(andPos).isLessThan(limitPos);
        }

        @Test
        @DisplayName("有 ORDER BY → AND 在 ORDER BY 之前")
        void shouldInsertBeforeOrderBy() {
            String sql = "SELECT * FROM qxx_md_unit_measure WHERE enable_flag = ? ORDER BY unit_id";
            String result = interceptor.injectWhere(sql, "factory_id");
            int andPos = result.indexOf("AND factory_id");
            int orderPos = result.indexOf("ORDER BY");
            assertThat(andPos).isLessThan(orderPos);
        }

        @Test
        @DisplayName("IN (?) → AND 在 IN 后面，不在括号里")
        void shouldNotInjectInsideInClause() {
            String sql = "DELETE FROM sys_role WHERE role_id IN ( ? )";
            String result = interceptor.injectWhere(sql, "factory_id");
            assertThat(result).contains("IN ( ? ) AND factory_id");
            assertThat(result).doesNotContain("IN ( ? AND factory_id");
        }

        @Test
        @DisplayName("COUNT 子查询 → AND 追加在末尾（已知限制，真实场景中 PageHelper 会先被外层拦截处理）")
        void shouldInjectAtEndForCountSubquery() {
            String sql = "SELECT count(0) FROM (SELECT * FROM qxx_md_unit_measure WHERE enable_flag = ?) tmp_count";
            String result = interceptor.injectWhere(sql, "factory_id");
            // 当前实现：在 ) 被 ? 前导跳过时，AND 追加在末尾
            assertThat(result).contains("AND factory_id = __FACTORY_ID__");
            assertThat(result.trim()).endsWith("AND factory_id = __FACTORY_ID__");
        }

        @Test
        @DisplayName("使用别名前缀")
        void shouldUseAliasPrefix() {
            String sql = "SELECT * FROM qxx_md_unit_measure u WHERE u.unit_code = ?";
            String result = interceptor.injectWhere(sql, "u.factory_id");
            assertThat(result).contains("AND u.factory_id = __FACTORY_ID__");
        }
    }

    // ==================== injectInsert ====================

    @Nested
    @DisplayName("injectInsert")
    class InjectInsert {

        @Test
        @DisplayName("INSERT VALUES → 追加列和值")
        void shouldAddColumnAndValue() {
            String sql = "INSERT INTO qxx_md_unit_measure ( unit_code, unit_name ) values ( ?, ? )";
            String result = interceptor.injectInsert(sql, "factory_id");
            assertThat(result).contains(", factory_id )");
            assertThat(result).contains(", __FACTORY_ID__ )");
        }

        @Test
        @DisplayName("INSERT 无空格 )values( → 也能匹配")
        void shouldHandleNoSpaceAroundValues() {
            String sql = "INSERT INTO sys_role ( role_name, role_key )values( ?, ? )";
            String result = interceptor.injectInsert(sql, "factory_id");
            assertThat(result).contains(", factory_id )");
            assertThat(result).contains(", __FACTORY_ID__ )");
        }

        @Test
        @DisplayName("批量 INSERT → 每行都加 factory_id")
        void shouldHandleBatchInsert() {
            String sql = "INSERT INTO sys_role_menu ( role_id, menu_id ) values ( ?, ? ) , ( ?, ? )";
            String result = interceptor.injectInsert(sql, "factory_id");
            assertThat(result).contains(", factory_id )");
            // 每行的 ) 后都有 factory_id 值
            int count = result.split("__FACTORY_ID__").length - 1;
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("不匹配的 SQL → 原样返回")
        void shouldReturnOriginalForNoMatch() {
            String sql = "INSERT INTO qxx_md_unit_measure VALUES ( ? )";
            String result = interceptor.injectInsert(sql, "factory_id");
            assertThat(result).isEqualTo(sql); // 无法解析 → 原样返回
        }

        @Test
        @DisplayName("换行符 → 归一化后正常匹配")
        void shouldHandleNewlines() {
            String sql = "INSERT INTO qxx_md_unit_measure (\n  unit_code,\n  unit_name\n) values (\n  ?,\n  ?\n)";
            String result = interceptor.injectInsert(sql, "factory_id");
            assertThat(result).contains(", factory_id )");
        }
    }
}
