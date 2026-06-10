package com.ruoyi.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 跳过工厂ID自动注入注解
 * 标注在 Mapper 方法上，FactoryIdInterceptor 将不对该方法做 factory_id 注入。
 * 仅用于平台级全局查询等极少数场景。
 *
 * @author qixiaoxia
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SkipFactoryId
{
}
