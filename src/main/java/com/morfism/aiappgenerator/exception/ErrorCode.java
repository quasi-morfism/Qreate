package com.morfism.aiappgenerator.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAMS_ERROR(40000, "请求参数错误 (Request parameter error)"),
    NOT_LOGIN_ERROR(40100, "未登录 (Not logged in)"),
    NO_AUTH_ERROR(40101, "无权限 (No permission)"),
    NOT_FOUND_ERROR(40400, "请求数据不存在 (Requested data not found)"),
    FORBIDDEN_ERROR(40300, "禁止访问 (Access forbidden)"),
    SYSTEM_ERROR(50000, "系统内部异常 (Internal system error)"),
    OPERATION_ERROR(50001, "操作失败 (Operation failed)");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
