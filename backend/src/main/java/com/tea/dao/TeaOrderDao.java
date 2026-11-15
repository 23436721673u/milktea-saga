package com.tea.dao;

import com.tea.entity.TeaOrder;
import com.tea.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 订单主表 DAO。 */
public class TeaOrderDao {

    private static final String COLUMNS = "id, member_id, total_amount, status, created_at";

    /** 在外部事务里插入订单主表，返回自增订单号。Connection 由 Service 传入，控制事务边界。 */
    public long insertWithConn(Connection conn, TeaOrder order) throws SQLException {
        String sql = "INSERT INTO tea_order (member_id, total_amount, status) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (order.getMemberId() == null) {
                ps.setNull(1, Types.BIGINT);   // 散客：member_id 写 NULL
            } else {
                ps.setLong(1, order.getMemberId());
            }
            ps.setBigDecimal(2, order.getTotalAmount());
            ps.setString(3, order.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    order.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("订单插入未返回主键");
    }

    public TeaOrder findById(long id) {
        String sql = "SELECT " + COLUMNS + " FROM tea_order WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DaoException("查询订单失败", e);
        }
    }

    /** 某会员的历史订单，最近的在前。 */
    public List<TeaOrder> listByMemberId(long memberId) {
        String sql = "SELECT " + COLUMNS + " FROM tea_order WHERE member_id = ? ORDER BY created_at DESC, id DESC";
        List<TeaOrder> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("查询会员订单失败", e);
        }
        return list;
    }

    private TeaOrder map(ResultSet rs) throws SQLException {
        TeaOrder o = new TeaOrder();
        o.setId(rs.getLong("id"));
        long mid = rs.getLong("member_id");
        o.setMemberId(rs.wasNull() ? null : mid);   // getLong 遇 NULL 返回 0，需用 wasNull 判断
        o.setTotalAmount(rs.getBigDecimal("total_amount"));
        o.setStatus(rs.getString("status"));
        // DATETIME 按原值读取，不做时区换算（JVM 与 MySQL 时区不一致时也不会偏移）
        o.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return o;
    }
}
