package com.ruoyi.common.core.redis;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.ruoyi.common.exception.ServiceException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Redis 分布式锁模板 — 消除 tryLock/finally/unlock 样板代码
 *
 * <pre>
 *   lockTemplate.execute("wm:stock:lock:1:2:3", () -> doStockUpdate());
 * </pre>
 *
 * @author qixiaoxia
 * @date 2026-06-12
 */
@Component
public class RedisLockTemplate {

    @Autowired
    private RedissonClient redissonClient;

    private static final long DEFAULT_WAIT_SEC = 5;

    /** 默认 5s 等待 */
    public void execute(String lockKey, Runnable action) {
        execute(lockKey, DEFAULT_WAIT_SEC, action);
    }

    /** 指定等待时间 */
    public void execute(String lockKey, long waitSec, Runnable action) {
        executeWithResult(lockKey, waitSec, () -> { action.run(); return null; });
    }

    /** 带返回值 */
    public <T> T execute(String lockKey, Supplier<T> action) {
        return executeWithResult(lockKey, DEFAULT_WAIT_SEC, action);
    }

    /** 指定等待时间 + 带返回值 */
    public <T> T executeWithResult(String lockKey, long waitSec, Supplier<T> action) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(waitSec, TimeUnit.SECONDS)) {
                throw new ServiceException("系统繁忙，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("获取锁被中断");
        }
        try {
            return action.get();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
