package com.eldercare.common;

/**
 * 业务异常：业务校验不通过时抛出，由全局异常处理器统一转换
 */
public class BusinessException extends RuntimeException {

    private final Integer code;

    /**
     * @param code    错误码（400 参数错误 / 401 未登录 / 403 无权限 / 404 不存在）
     * @param message 错误提示
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(400, message);
    }

    public Integer getCode() {
        return code;
    }
}