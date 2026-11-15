package com.tea.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 数据库连接工具类：全局唯一获取 Connection 的入口。
 * <p>
 * 配置读取优先级：环境变量 MILKTEA_DB_URL / MILKTEA_DB_USER / MILKTEA_DB_PASSWORD
 * &gt; classpath 下的 db.properties &gt; 代码内默认值。
 * db.properties 已被 .gitignore 忽略，本机密码不会被提交到仓库。
 */
public final class DbUtil {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/milktea_saga"
                    + "?useUnicode=true&characterEncoding=UTF-8"
                    + "&useSSL=false&allowPublicKeyRetrieval=true"
                    + "&serverTimezone=Asia/Shanghai";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "123456";

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    static {
        Properties props = new Properties();
        try (InputStream in = DbUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError("读取 db.properties 失败：" + e.getMessage());
        }
        URL = pick("MILKTEA_DB_URL", props.getProperty("db.url"), DEFAULT_URL);
        USER = pick("MILKTEA_DB_USER", props.getProperty("db.user"), DEFAULT_USER);
        PASSWORD = pick("MILKTEA_DB_PASSWORD", props.getProperty("db.password"), DEFAULT_PASSWORD);

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("MySQL 驱动加载失败，请检查 pom.xml 是否引入 mysql-connector-j");
        }
    }

    private DbUtil() {
    }

    /** 获取一个新连接，调用方负责关闭（推荐 try-with-resources）。 */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /** 启动自检：能连上返回 null，连不上返回可读的错误原因。 */
    public static String checkConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(2) ? null : "连接无效";
        } catch (SQLException e) {
            return e.getMessage();
        }
    }

    private static String pick(String envKey, String fileValue, String defaultValue) {
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        if (fileValue != null && !fileValue.isBlank()) {
            return fileValue.trim();
        }
        return defaultValue;
    }
}
