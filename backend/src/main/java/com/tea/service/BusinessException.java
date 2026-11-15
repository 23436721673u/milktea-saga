package com.tea.service;

/**
 * 业务异常：Service 层校验不通过时抛出，Main 只把 message 友好地展示给店员，
 * 不把 SQLException 堆栈暴露到界面上。
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
