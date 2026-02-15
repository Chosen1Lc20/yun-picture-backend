package com.lc.yunpicturebackend.common;

import com.lc.yunpicturebackend.exception.ErrorCode;

/**
 * 通用返回结果工具类
 */
public class ResultUtils {
    public static <T> BaseResponse<T> success(T data){
        return new BaseResponse<>(0,data,"ok");
    }

    public static <T> BaseResponse<T> failure(){
        return new BaseResponse<>(1,null,"失败");
    }

    public static BaseResponse<?> error(ErrorCode errorCode){
        return new BaseResponse<>(errorCode);
    }
    public static BaseResponse<?> error(ErrorCode errorCode, String message){
        return new BaseResponse<>(errorCode.getCode(),null,message);
    }
    public static BaseResponse<?> error(int code, String message){
        return new BaseResponse<>(code,null,message);
    }
}
