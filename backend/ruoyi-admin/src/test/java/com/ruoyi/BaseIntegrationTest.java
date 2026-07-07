package com.ruoyi;

import com.ruoyi.RuoYiApplication;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.common.core.redis.RedisCache;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

/**
 * 集成测试基类。
 * <p>
 * 职责：
 * <ul>
 *   <li>启动 MySQL 8.0.33 Testcontainer 并初始化 schema</li>
 *   <li>通过 @DynamicPropertySource 动态注入数据源配置</li>
 *   <li>提供 truncateTables() 方法用于测试间数据隔离</li>
 *   <li>提供 loginAsAdmin() 方法获取认证 token</li>
 * </ul>
 * <p>
 * 使用前需要启动本地 Redis：<pre>docker compose up -d redis</pre>
 *
 * @author qixiaoxia
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = RuoYiApplication.class,
    properties = {
        "spring.profiles.active=test,druid",
        // 修复 Flyway "connection disabled" — Druid 借出连接前必须先验证
        "spring.datasource.druid.testOnBorrow=true",
        "spring.datasource.druid.validationQuery=SELECT 1"
    }
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    static final MySQLContainer<?> mysql = new MySQLContainer<>(
            DockerImageName.parse("mysql:8.0.33").asCompatibleSubstituteFor("mysql")
    )
            .withDatabaseName("mes_integration_test")
            .withUsername("test")
            .withPassword("test")
            .withUrlParam("useUnicode", "true")
            .withUrlParam("characterEncoding", "UTF-8")
            .withUrlParam("serverTimezone", "GMT%2B8")
            .withUrlParam("allowMultiQueries", "true")
            .waitingFor(Wait.forHealthcheck());

    @LocalServerPort
    protected int port;

    protected final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisCache redisCache;

    private static String adminToken;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Start container BEFORE registering properties (DynamicPropertySource runs before @BeforeAll)
        mysql.start();

        registry.add("spring.datasource.druid.master.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.druid.master.username", mysql::getUsername);
        registry.add("spring.datasource.druid.master.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        // SQL init 通过 classpath 资源（由 maven-resources-plugin 复制到 test-classes/sql/）
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations", () ->
            "classpath:sql/ry_20260417.sql,classpath:sql/test_mes_ddl.sql");
        registry.add("spring.sql.init.continue-on-error", () -> "true");
    }

    @AfterAll
    static void stopContainer() {
        mysql.stop();
    }

    /**
     * 清理测试数据。子类应在 @BeforeEach 中调用此方法，
     * 传入本次测试涉及的表名（按外键安全顺序，子表在前）。
     */
    protected void truncateTables(String... tables) {
        if (tables.length == 0) return;
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        for (String table : tables) {
            jdbcTemplate.execute("TRUNCATE TABLE " + table);
        }
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    /**
     * 以管理员身份登录，返回 Bearer token。
     * 首次调用时会先关闭验证码（更新 DB + 清除 Redis 缓存），然后登录。
     * 后续调用复用缓存的 token。
     */
    protected String loginAsAdmin() {
        if (adminToken != null) {
            return adminToken;
        }

        // 首次登录前：关闭验证码并清除 Redis 缓存
        // （@PostConstruct 已缓存 captchaEnabled=true，需要同时更新 DB 和 Redis）
        jdbcTemplate.execute(
                "UPDATE sys_config SET config_value = 'false' WHERE config_key = 'sys.account.captchaEnabled'"
        );
        redisCache.deleteObject(CacheConstants.SYS_CONFIG_KEY + "sys.account.captchaEnabled");

        String loginUrl = "http://localhost:" + port + "/login";

        LoginBody loginBody = new LoginBody();
        loginBody.setUsername("admin");
        loginBody.setPassword("admin123");
        loginBody.setCode("");
        loginBody.setUuid("");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginBody> request = new HttpEntity<>(loginBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(loginUrl, request, Map.class);
        Map<String, Object> body = response.getBody();

        if (body == null || !"操作成功".equals(body.get("msg")) || body.get(Constants.TOKEN) == null) {
            throw new RuntimeException("Admin login failed. Response: " + body);
        }

        adminToken = (String) body.get(Constants.TOKEN);
        return adminToken;
    }

    /**
     * 创建带认证头的 HttpHeaders。
     */
    protected HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(loginAsAdmin());
        return headers;
    }

    /**
     * 构建带认证的 POST 请求实体。
     */
    protected <T> HttpEntity<T> authRequest(T body) {
        return new HttpEntity<>(body, authHeaders());
    }

    /**
     * 构建带认证的 GET/PUT/DELETE 请求实体（无 body）。
     */
    protected HttpEntity<Void> authRequest() {
        return new HttpEntity<>(authHeaders());
    }
}
