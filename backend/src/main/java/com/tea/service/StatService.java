package com.tea.service;

import com.tea.dao.DaoException;
import com.tea.entity.Drink;
import com.tea.util.DbUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** 数据统计 Service：聚合函数 + GROUP BY + ORDER BY + LIMIT。 */
public class StatService {

    private final DrinkService drinkService = new DrinkService();

    /** 销量榜一行。 */
    public record SaleRank(String drinkName, int totalQty, BigDecimal totalAmount) {
    }

    /** 月度营业额汇总。 */
    public record MonthlySummary(String period, int orderCount, BigDecimal totalAmount, BigDecimal avgAmount) {
    }

    /** 本月销量榜 TOP N（按杯数倒序，杯数相同按营业额倒序）。只统计已支付订单。 */
    public List<SaleRank> topSalesThisMonth(int topN) {
        String sql = "SELECT d.name AS drink_name, SUM(oi.quantity) AS total_qty, SUM(oi.subtotal) AS total_amount "
                + "FROM order_item oi "
                + "JOIN drink d     ON d.id = oi.drink_id "
                + "JOIN tea_order o ON o.id = oi.order_id "
                + "WHERE o.status = 'PAID' "
                + "  AND o.created_at >= DATE_FORMAT(CURDATE(), '%Y-%m-01') "
                + "  AND o.created_at <  DATE_FORMAT(CURDATE() + INTERVAL 1 MONTH, '%Y-%m-01') "
                + "GROUP BY d.id, d.name "
                + "ORDER BY total_qty DESC, total_amount DESC "
                + "LIMIT ?";
        List<SaleRank> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, topN);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new SaleRank(rs.getString("drink_name"), rs.getInt("total_qty"),
                            rs.getBigDecimal("total_amount")));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("查询销量榜失败", e);
        }
        return list;
    }

    /** 本月营业额：订单数、总额、客单价。IFNULL 防止当月没有订单时 SUM 返回 NULL。 */
    public MonthlySummary monthlySummary() {
        String sql = "SELECT DATE_FORMAT(CURDATE(), '%Y-%m') AS period, COUNT(*) AS cnt, "
                + "       IFNULL(SUM(total_amount), 0) AS sum_amt "
                + "FROM tea_order "
                + "WHERE status = 'PAID' "
                + "  AND created_at >= DATE_FORMAT(CURDATE(), '%Y-%m-01') "
                + "  AND created_at <  DATE_FORMAT(CURDATE() + INTERVAL 1 MONTH, '%Y-%m-01')";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            int cnt = rs.getInt("cnt");
            BigDecimal sum = rs.getBigDecimal("sum_amt").setScale(2, RoundingMode.HALF_UP);
            BigDecimal avg = cnt == 0 ? BigDecimal.ZERO.setScale(2)
                    : sum.divide(BigDecimal.valueOf(cnt), 2, RoundingMode.HALF_UP);
            return new MonthlySummary(rs.getString("period"), cnt, sum, avg);
        } catch (SQLException e) {
            throw new DaoException("查询营业额失败", e);
        }
    }

    /** 库存预警直接复用商品模块。 */
    public List<Drink> lowStock() {
        return drinkService.listLowStock();
    }
}
