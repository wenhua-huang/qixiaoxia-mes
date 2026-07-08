package com.ruoyi.web.core.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Flyway 数据库迁移配置
 *
 * 不使用 Druid 连接池（Spring SQL init 后连接可能被标记 disabled），
 * 改用 DriverManagerDataSource — 每次 getConnection() 创建新物理连接，
 * 但 Flyway 内部对同一 migration 复用同一 Connection，支持 SET @var 跨语句。
 *
 * @author qixiaoxia
 * @date 2026-07-03
 */
@Configuration
public class FlywayConfig
{
    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String locations;

    @Value("${spring.flyway.table:flyway_schema_history}")
    private String table;

    @Value("${spring.flyway.baseline-on-migrate:false}")
    private boolean baselineOnMigrate;

    @Value("${spring.flyway.baseline-version:1}")
    private String baselineVersion;

    @Value("${spring.datasource.druid.master.url}")
    private String masterUrl;

    @Value("${spring.datasource.druid.master.username}")
    private String masterUsername;

    @Value("${spring.datasource.druid.master.password}")
    private String masterPassword;

    /**
     * 注：Flyway 使用独立 DriverManagerDataSource（不走 Druid 连接池），因为 Druid 在
     * Spring SQL init 之后可能将连接标记为 disabled。此机制与 validateOnMigrate 无关。
     *
     * validateOnMigrate 设为 true：flyway_schema_history 已对齐本地迁移文件
     * （测试环境 V43/V50 checksum 已修正、V44/45/46 missing 记录已清理；生产环境 checksum 本就一致），
     * 可正常开启校验，防止已执行迁移被篡改。
     *
     * 约束：已执行的迁移文件禁止修改（checksum 会变导致启动失败），新需求创建新版本迁移文件（V{N+1}__xxx.sql）。
     */
    @Bean
    public Flyway flyway()
    {
        DriverManagerDataSource flywayDs = new DriverManagerDataSource();
        flywayDs.setUrl(masterUrl);
        flywayDs.setUsername(masterUsername);
        flywayDs.setPassword(masterPassword);
        flywayDs.setDriverClassName("com.mysql.cj.jdbc.Driver");

        Flyway flyway = Flyway.configure()
                .dataSource(flywayDs)
                .locations(locations.split(","))
                .table(table)
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(org.flywaydb.core.api.MigrationVersion.fromVersion(baselineVersion))
                .validateOnMigrate(true)  // history 已对齐，开启校验
                .load();
        int count = flyway.migrate().migrationsExecuted;
        log.info("Flyway 迁移完成，执行了 {} 个 migration", count);
        return flyway;
    }
}
