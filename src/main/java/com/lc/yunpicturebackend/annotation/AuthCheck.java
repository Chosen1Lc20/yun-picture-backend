package com.lc.yunpicturebackend.annotation;

import cn.hutool.core.annotation.Alias;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)

public @interface AuthCheck {
    /**
     * 必须有某个角色
     * 属性定义格式：数据类型 属性名() [default 默认值];
     */
    String mustRole() default "";

}
