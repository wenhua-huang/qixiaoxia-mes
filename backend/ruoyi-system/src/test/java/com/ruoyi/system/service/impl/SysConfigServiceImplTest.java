package com.ruoyi.system.service.impl;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.mapper.SysConfigMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 参数配置服务单元测试
 * <p>
 * 技术栈：JUnit 5 + Mockito，不启动 Spring 容器
 *
 * @author qixiaoxia
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("参数配置服务单元测试")
class SysConfigServiceImplTest {

    @Mock
    private SysConfigMapper configMapper;

    @Mock
    private RedisCache redisCache;

    @InjectMocks
    private SysConfigServiceImpl configService;

    // ==================== selectConfigById ====================

    @Test
    @DisplayName("should return config when configId exists")
    void shouldReturnConfig_whenConfigIdExists() {
        SysConfig config = new SysConfig();
        config.setConfigId(1L);
        config.setConfigName("主框架配置");
        config.setConfigKey("sys.index.skinName");

        when(configMapper.selectConfig(any(SysConfig.class))).thenReturn(config);

        SysConfig result = configService.selectConfigById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getConfigId()).isEqualTo(1L);
        assertThat(result.getConfigName()).isEqualTo("主框架配置");
        verify(configMapper, times(1)).selectConfig(any(SysConfig.class));
    }

    @Test
    @DisplayName("should return null when configId not found")
    void shouldReturnNull_whenConfigIdNotFound() {
        when(configMapper.selectConfig(any(SysConfig.class))).thenReturn(null);

        SysConfig result = configService.selectConfigById(999L);

        assertThat(result).isNull();
    }

    // ==================== selectConfigByKey ====================

    @Test
    @DisplayName("should return value from cache when cache hit")
    void shouldReturnValueFromCache_whenCacheHit() {
        when(redisCache.getCacheObject(CacheConstants.SYS_CONFIG_KEY + "sys.index.skinName"))
                .thenReturn("skin-blue");

        String result = configService.selectConfigByKey("sys.index.skinName");

        assertThat(result).isEqualTo("skin-blue");
        // 缓存命中时不应查 DB
        verify(configMapper, never()).selectConfig(any(SysConfig.class));
    }

    @Test
    @DisplayName("should query DB and write cache when cache miss")
    void shouldQueryDbAndWriteCache_whenCacheMiss() {
        when(redisCache.getCacheObject(CacheConstants.SYS_CONFIG_KEY + "test.key"))
                .thenReturn(null);

        SysConfig dbConfig = new SysConfig();
        dbConfig.setConfigKey("test.key");
        dbConfig.setConfigValue("dbValue");
        when(configMapper.selectConfig(any(SysConfig.class))).thenReturn(dbConfig);

        String result = configService.selectConfigByKey("test.key");

        assertThat(result).isEqualTo("dbValue");
        verify(redisCache).setCacheObject(CacheConstants.SYS_CONFIG_KEY + "test.key", "dbValue");
    }

    // ==================== deleteConfigByIds ====================

    @Test
    @DisplayName("should throw ServiceException when deleting built-in config")
    void shouldThrowServiceException_whenDeleteBuiltInConfig() {
        SysConfig builtInConfig = new SysConfig();
        builtInConfig.setConfigId(1L);
        builtInConfig.setConfigKey("sys.user.initPassword");
        builtInConfig.setConfigType(UserConstants.YES); // Y = 内置参数

        when(configMapper.selectConfig(any(SysConfig.class))).thenReturn(builtInConfig);

        assertThatThrownBy(() -> configService.deleteConfigByIds(new Long[]{1L}))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("内置参数")
                .hasMessageContaining("不能删除");

        // 抛异常时应确保未执行删除操作
        verify(configMapper, never()).deleteConfigById(anyLong());
        verify(redisCache, never()).deleteObject(anyString());
    }

    @Test
    @DisplayName("should delete non-built-in config and clear cache")
    void shouldDeleteConfigAndClearCache_whenNotBuiltIn() {
        SysConfig normalConfig = new SysConfig();
        normalConfig.setConfigId(100L);
        normalConfig.setConfigKey("custom.settings");
        normalConfig.setConfigType("N"); // N = 非内置

        when(configMapper.selectConfig(any(SysConfig.class))).thenReturn(normalConfig);

        configService.deleteConfigByIds(new Long[]{100L});

        verify(configMapper, times(1)).deleteConfigById(100L);
        verify(redisCache, times(1)).deleteObject(CacheConstants.SYS_CONFIG_KEY + "custom.settings");
    }

    // ==================== checkConfigKeyUnique ====================

    @Test
    @DisplayName("should return unique when config key not exists")
    void shouldReturnUnique_whenConfigKeyNotExists() {
        when(configMapper.checkConfigKeyUnique("new.key")).thenReturn(null);

        SysConfig config = new SysConfig();
        config.setConfigKey("new.key");
        boolean result = configService.checkConfigKeyUnique(config);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return not unique when config key is duplicate with different id")
    void shouldReturnNotUnique_whenConfigKeyIsDuplicateWithDifferentId() {
        SysConfig existing = new SysConfig();
        existing.setConfigId(10L);
        existing.setConfigKey("existing.key");
        when(configMapper.checkConfigKeyUnique("existing.key")).thenReturn(existing);

        SysConfig config = new SysConfig();
        config.setConfigId(5L);
        config.setConfigKey("existing.key");
        boolean result = configService.checkConfigKeyUnique(config);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should return unique when config key exists but belongs to same config")
    void shouldReturnUnique_whenConfigKeyBelongsToSameConfig() {
        SysConfig existing = new SysConfig();
        existing.setConfigId(5L);
        existing.setConfigKey("same.key");
        when(configMapper.checkConfigKeyUnique("same.key")).thenReturn(existing);

        SysConfig config = new SysConfig();
        config.setConfigId(5L);
        config.setConfigKey("same.key");
        boolean result = configService.checkConfigKeyUnique(config);

        assertThat(result).isTrue();
    }

    // ==================== insertConfig ====================

    @Test
    @DisplayName("should insert config and write cache when insert succeeds")
    void shouldInsertConfigAndWriteCache_whenInsertSucceeds() {
        SysConfig newConfig = new SysConfig();
        newConfig.setConfigKey("new.config");
        newConfig.setConfigValue("newValue");
        when(configMapper.insertConfig(any(SysConfig.class))).thenReturn(1);

        int result = configService.insertConfig(newConfig);

        assertThat(result).isEqualTo(1);
        verify(redisCache).setCacheObject(CacheConstants.SYS_CONFIG_KEY + "new.config", "newValue");
    }

    @Test
    @DisplayName("should not write cache when insert returns zero")
    void shouldNotWriteCache_whenInsertReturnsZero() {
        SysConfig newConfig = new SysConfig();
        newConfig.setConfigKey("fail.config");
        newConfig.setConfigValue("failValue");
        when(configMapper.insertConfig(any(SysConfig.class))).thenReturn(0);

        int result = configService.insertConfig(newConfig);

        assertThat(result).isEqualTo(0);
        verify(redisCache, never()).setCacheObject(anyString(), any());
    }
}
