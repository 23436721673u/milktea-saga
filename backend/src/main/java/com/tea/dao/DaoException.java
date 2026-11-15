package com.tea.dao;

import java.sql.SQLException;

/**
 * DAO 层统一的运行时异常：把受检的 SQLException 包一层往上抛，
 * 同时保留 SQLState / 错误码，方便 Service 层识别"唯一键冲突"等情况。
 */
public class DaoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** MySQL 唯一键冲突错误码 */
    private static final int ER_DUP_ENTRY = 1062;

    public DaoException(String message, SQLException cause) {
        super(message + "：" + cause.getMessage(), cause);
    }

    public boolean isDuplicateKey() {
        return getCause() instanceof SQLException e && e.getErrorCode() == ER_DUP_ENTRY;
    }
}
