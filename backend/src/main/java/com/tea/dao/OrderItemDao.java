package com.tea.dao;

import com.tea.entity.OrderItem;
import com.tea.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** 订单明细 DAO。 */
public class OrderItemDao {

    /** 在外部事务里批量插入明细（addBatch 一次发给数据库，减少网络往返）。 */
    public void insertBatchWithConn(Connection conn, long orderId, List<OrderItem> items) throws SQLException {
        String sql = "INSERT INTO order_item (order_id, drink_id, quantity, unit_price, subtotal) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (OrderItem it : items) {
                it.setOrderId(orderId);
                ps.setLong(1, orderId);
                ps.setLong(2, it.getDrinkId());
                ps.setInt(3, it.getQuantity());
                ps.setBigDecimal(4, it.getUnitPrice());
                ps.setBigDecimal(5, it.getSubtotal());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * 多表 JOIN：查某订单的全部明细，并带出奶茶名。
     * 奶茶下架只是 is_active = 0，记录还在，所以历史订单依然能显示名字。
     */
    public List<OrderItem> findByOrderIdWithDrinkName(long orderId) {
        String sql = "SELECT oi.id, oi.order_id, oi.drink_id, oi.quantity, oi.unit_price, oi.subtotal, "
                + "       d.name AS drink_name "
                + "FROM order_item oi "
                + "JOIN drink d ON d.id = oi.drink_id "
                + "WHERE oi.order_id = ? "
                + "ORDER BY oi.id";
        List<OrderItem> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderItem it = new OrderItem();
                    it.setId(rs.getLong("id"));
                    it.setOrderId(rs.getLong("order_id"));
                    it.setDrinkId(rs.getLong("drink_id"));
                    it.setUnitPrice(rs.getBigDecimal("unit_price"));
                    it.setQuantity(rs.getInt("quantity"));
                    it.setSubtotal(rs.getBigDecimal("subtotal"));   // 以数据库落库的小计为准
                    it.setDrinkName(rs.getString("drink_name"));
                    list.add(it);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("查询订单明细失败", e);
        }
        return list;
    }
}
