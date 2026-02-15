package com.lc.yunpicturebackend.exception;

/**
 * 类似断言类的工具
 */
public class ThrowUtils {
    /**
     * 条件成立则抛异常
     * @param condition 情况
     * @param runtimeException 异常
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException){
        if(condition){
            throw runtimeException;
        }
    }

    /**
     * 条件成立则抛异常
     * @param condition 情况
     * @param errorCode 错误码
     */
    public static void throwIf(boolean condition, ErrorCode errorCode){
        throwIf(condition, new BusinessException(errorCode));
    }

    /**
     * 条件成立则抛异常
     * @param condition 情况
     * @param errorCode 错误码
     * @param message 信息
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String message){
        throwIf(condition, new BusinessException(errorCode, message));
    }
}
