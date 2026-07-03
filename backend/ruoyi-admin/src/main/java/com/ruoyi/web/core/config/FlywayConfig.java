package com.ruoyi.web.core.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway 数据库迁移配置
 *
 * 因为 RuoYi 使用 DynamicDataSource（主从多数据源），Flyway 自动配置会退避。
 * 本配置显式绑定 masterDataSource，让 Flyway 仅对主库执行迁移。
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

    @Bean
    public Flyway flyway(@Qualifier("masterDataSource") DataSource masterDataSource)
    {
        Flyway flyway = Flyway.configure()
                .dataSource(masterDataSource)
                .locations(locations.split(","))
                .table(table)
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(org.flywaydb.core.api.MigrationVersion.fromVersion(baselineVersion))
                .load();
        int count = flyway.migrate().migrationsExecuted;
        log.info("Flyway 迁移完成，执行了 {} 个 migration", count);
        return flyway;
    }
}
