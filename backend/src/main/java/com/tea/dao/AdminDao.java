package com.tea.dao;

import com.tea.entity.Admin;
import com.tea.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/** 店员账号 DAO：全部使用 PreparedStatement 占位符，杜绝 SQL 注入。 */
public class AdminDao {

    /** 按用户名 + 密码摘要查询（登录）。 */
    public Admin findByUsernameAndPassword(String username, String passwordHash) {
        String sql = "SELECT id, username, password, created_at FROM admin "
                + "WHERE username = ? AND password = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DaoException("查询店员失败", e);
        }
    }

    /** 用户名是否已被占用（注册前校验）。 */
    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM admin WHERE username = ? LIMIT 1";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DaoException("判断用户名是否存在失败", e);
        }
    }

    public int insert(Admin admin) {
        String sql = "INSERT INTO admin (username, password) VALUES (?, ?)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, admin.getUsername());
            ps.setString(2, admin.getPassword());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("新增店员失败", e);
        }
    }

    private Admin map(ResultSet rs) throws SQLException {
        Admin a = new Admin();
        a.setId(rs.getLong("id"));
        a.setUsername(rs.getString("username"));
        a.setPassword(rs.getString("password"));
        // DATETIME 按原值读取，不做时区换算（JVM 与 MySQL 时区不一致时也不会偏移）
        a.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return a;
    }
}
