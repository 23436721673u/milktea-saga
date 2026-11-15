package com.tea.dao;

import com.tea.entity.Drink;
import com.tea.util.DbUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 奶茶商品 DAO。 */
public class DrinkDao {

    private static final String COLUMNS = "id, name, category, price, stock, is_active, created_at";

    /** 全部在售奶茶，按分类分组展示。 */
    public List<Drink> listAllActive() {
        String sql = "SELECT " + COLUMNS + " FROM drink WHERE is_active = 1 "
                + "ORDER BY FIELD(category, '奶茶', '果茶', '特调'), id";
        return queryList(sql, null);
    }

    /** 按 ID 查询（含已下架，调用方自行判断 isOnSale）。 */
    public Drink findById(long id) {
        String sql = "SELECT " + COLUMNS + " FROM drink WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DaoException("查询奶茶失败", e);
        }
    }

    /** 新增奶茶，回填自增主键。 */
    public int insert(Drink d) {
        String sql = "INSERT INTO drink (name, category, price, stock, is_active) VALUES (?, ?, ?, ?, 1)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getName());
            ps.setString(2, d.getCategory());
            ps.setBigDecimal(3, d.getPrice());
            ps.setInt(4, d.getStock());
            int n = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    d.setId(keys.getLong(1));
                }
            }
            return n;
        } catch (SQLException e) {
            throw new DaoException("新增奶茶失败", e);
        }
    }

    /** 修改价格与库存（只改在售商品）。 */
    public int updatePriceAndStock(long id, BigDecimal price, int stock) {
        String sql = "UPDATE drink SET price = ?, stock = ? WHERE id = ? AND is_active = 1";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, price);
            ps.setInt(2, stock);
            ps.setLong(3, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("修改奶茶失败", e);
        }
    }

    /** 下架 = 软删除：is_active 置 0，历史订单仍能 JOIN 出名字和当时价格。 */
    public int deactivate(long id) {
        String sql = "UPDATE drink SET is_active = 0 WHERE id = ? AND is_active = 1";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("下架奶茶失败", e);
        }
    }

    /** 库存预警：在售且库存 &lt; threshold，库存少的排前面。 */
    public List<Drink> listLowStock(int threshold) {
        String sql = "SELECT " + COLUMNS + " FROM drink WHERE is_active = 1 AND stock < ? ORDER BY stock, id";
        return queryList(sql, threshold);
    }

    /**
     * 在外部事务里扣库存。WHERE 带 stock &gt;= ? 条件，库存不够时影响行数为 0，
     * 由调用方抛异常触发回滚 —— 这就是防超卖的"乐观条件更新"。
     */
    public int decrementStockWithConn(Connection conn, long drinkId, int qty) throws SQLException {
        String sql = "UPDATE drink SET stock = stock - ? WHERE id = ? AND is_active = 1 AND stock >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setLong(2, drinkId);
            ps.setInt(3, qty);
            return ps.executeUpdate();
        }
    }

    private List<Drink> queryList(String sql, Integer intParam) {
        List<Drink> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (intParam != null) {
                ps.setInt(1, intParam);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("查询奶茶列表失败", e);
        }
        return list;
    }

    private Drink map(ResultSet rs) throws SQLException {
        Drink d = new Drink();
        d.setId(rs.getLong("id"));
        d.setName(rs.getString("name"));
        d.setCategory(rs.getString("category"));
        d.setPrice(rs.getBigDecimal("price"));
        d.setStock(rs.getInt("stock"));
        d.setIsActive(rs.getInt("is_active"));
        // DATETIME 按原值读取，不做时区换算（JVM 与 MySQL 时区不一致时也不会偏移）
        d.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return d;
    }
}
