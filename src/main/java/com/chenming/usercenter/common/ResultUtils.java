package com.chenming.usercenter.common;

/**
 * 返回工具类
 *
 * @author chenming
 */
public class ResultUtils {

    /**
     * 成功
     * @param data
     * @return
     * @param <T>
     */
    public static <T> BaseResponse<T> success(T data){
        return new BaseResponse<>(0, data, "ok");
    }

    /**
     * 失败
     * @param errorCode
     * @return
     * @param <T>
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode, String message, String description){
        return new BaseResponse(errorCode.getCode(), message, description);
    }

    public static <T> BaseResponse<T> error(int code, String message, String description){
        return new BaseResponse(code, null, message, description);
    }

    public static <T> BaseResponse<T> error(ErrorCode errorCode, String description){
        return new BaseResponse(errorCode.getCode(), null, errorCode.getMessage(), description);
    }
}
