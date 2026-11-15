package com.tea.service;

import com.tea.dao.DrinkDao;
import com.tea.dao.MemberDao;
import com.tea.dao.OrderItemDao;
import com.tea.dao.TeaOrderDao;
import com.tea.entity.Drink;
import com.tea.entity.Member;
import com.tea.entity.OrderItem;
import com.tea.entity.TeaOrder;
import com.tea.util.DbUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 下单结算 Service —— 本项目核心。
 * <p>
 * 一笔订单在 <b>同一个 Connection、同一个事务</b> 里完成四件事：
 * <ol>
 *   <li>INSERT tea_order 订单主表</li>
 *   <li>批量 INSERT order_item 订单明细</li>
 *   <li>UPDATE drink 扣库存（WHERE stock &gt;= ? 防超卖）</li>
 *   <li>UPDATE member 加积分（散客跳过）</li>
 * </ol>
 * 任何一步失败都 rollback，数据库里不会出现"扣了库存却没有订单"的中间状态。
 */
public class OrderService {

    /**
     * 回滚演示开关：启动时加 -Dmilktea.rollbackDemo=true，
     * 会在写完主表和明细、扣库存之前人为抛异常，用于验收"事务真的回滚了"。
     */
    static final String ROLLBACK_DEMO_PROPERTY = "milktea.rollbackDemo";

    private static final BigDecimal MAX_ORDER_AMOUNT = new BigDecimal("999999.99");

    private final TeaOrderDao teaOrderDao = new TeaOrderDao();
    private final OrderItemDao orderItemDao = new OrderItemDao();
    private final DrinkDao drinkDao = new DrinkDao();
    private final MemberDao memberDao = new MemberDao();

    /** 下单结果：订单号、总金额、积分变化（散客 pointsBefore/pointsAfter 为 null）。 */
    public record PlaceOrderResult(long orderId, BigDecimal totalAmount,
                                   Integer pointsBefore, Integer pointsAfter) {
    }

    /** 订单详情：主表 + 会员（可为 null）+ 明细。 */
    public record OrderDetail(TeaOrder order, Member member, List<OrderItem> items) {
    }

    /** 积分规则：满 1 元加 1 分，不足 1 元的部分舍去。 */
    public static int pointsFor(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.DOWN).intValueExact();
    }

    /**
     * 把同一款奶茶的多次加购合并成一行（珍珠×1 再加 珍珠×2 → 珍珠×3），
     * 否则逐行校验库存会漏掉"合计超出库存"的情况。
     */
    public static List<OrderItem> mergeItems(List<OrderItem> items) {
        Map<Long, OrderItem> merged = new LinkedHashMap<>();
        for (OrderItem it : items) {
            OrderItem exist = merged.get(it.getDrinkId());
            if (exist == null) {
                merged.put(it.getDrinkId(),
                        new OrderItem(it.getDrinkId(), it.getDrinkName(), it.getQuantity(), it.getUnitPrice()));
            } else {
                exist.setQuantity(exist.getQuantity() + it.getQuantity());
            }
        }
        return new ArrayList<>(merged.values());
    }

    public static BigDecimal sum(List<OrderItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem it : items) {
            total = total.add(it.getSubtotal());
        }
        return total;
    }

    /**
     * 下单。
     *
     * @param memberId 会员 ID，散客传 null
     * @param cart     购物车明细（drinkId + quantity），单价以数据库当前价格为准重新取快照
     */
    public PlaceOrderResult placeOrder(Long memberId, List<OrderItem> cart) {
        if (cart == null || cart.isEmpty()) {
            throw new BusinessException("订单至少要 1 杯");
        }

        // ---------- 事务前校验：商品在售、库存足够；单价以数据库为准 ----------
        List<OrderItem> items = new ArrayList<>();
        for (OrderItem it : mergeItems(cart)) {
            if (it.getQuantity() == null || it.getQuantity() <= 0) {
                throw new BusinessException("杯数必须 ≥ 1");
            }
            Drink d = drinkDao.findById(it.getDrinkId());
            if (d == null || !d.isOnSale()) {
                throw new BusinessException("奶茶不存在或已下架：id=" + it.getDrinkId());
            }
            if (d.getStock() < it.getQuantity()) {
                throw new BusinessException(String.format("库存不足：%s 仅剩 %d 杯", d.getName(), d.getStock()));
            }
            items.add(new OrderItem(d.getId(), d.getName(), it.getQuantity(), d.getPrice()));
        }
        BigDecimal total = sum(items);
        if (total.compareTo(MAX_ORDER_AMOUNT) > 0) {
            throw new BusinessException("单笔订单金额不能超过 ¥" + MAX_ORDER_AMOUNT.toPlainString());
        }

        Integer pointsBefore = null;
        if (memberId != null) {
            Member m = memberDao.findById(memberId);
            if (m == null) {
                throw new BusinessException("会员不存在：id=" + memberId);
            }
            pointsBefore = m.getPoints();
        }

        // ================= 事务开始 =================
        Connection conn = null;
        try {
            conn = DbUtil.getConnection();
            conn.setAutoCommit(false);                                   // 1. 关闭自动提交

            TeaOrder order = new TeaOrder(memberId, total);
            long orderId = teaOrderDao.insertWithConn(conn, order);     // 2. 订单主表
            orderItemDao.insertBatchWithConn(conn, orderId, items);    // 3. 订单明细

            if (Boolean.getBoolean(ROLLBACK_DEMO_PROPERTY)) {
                throw new SQLException("【回滚演示】主表和明细已写入，人为制造失败");
            }

            for (OrderItem it : items) {                                 // 4. 扣库存（防超卖）
                int n = drinkDao.decrementStockWithConn(conn, it.getDrinkId(), it.getQuantity());
                if (n != 1) {
                    throw new SQLException("库存不足或商品已下架（可能被其他店员抢先下单）：" + it.getDrinkName());
                }
            }

            Integer pointsAfter = null;
            if (memberId != null) {                                      // 5. 加积分（散客跳过）
                memberDao.addPointsWithConn(conn, memberId, pointsFor(total));
                pointsAfter = memberDao.findPointsWithConn(conn, memberId);
            }

            conn.commit();                                               // 6. 全部成功才提交
            return new PlaceOrderResult(orderId, total, pointsBefore, pointsAfter);

        } catch (SQLException e) {
            rollbackQuietly(conn);                                       // 7. 任一步失败 → 整单回滚
            throw new BusinessException("下单失败，已回滚：" + e.getMessage(), e);
        } catch (RuntimeException e) {
            rollbackQuietly(conn);
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignore) {
                    // 关闭失败不影响业务结果
                }
            }
        }
    }

    /** 查询订单详情：主表 + 会员 + JOIN 出奶茶名的明细。不存在返回 null。 */
    public OrderDetail findOrderDetail(long orderId) {
        TeaOrder o = teaOrderDao.findById(orderId);
        if (o == null) {
            return null;
        }
        Member m = o.getMemberId() == null ? null : memberDao.findById(o.getMemberId());
        return new OrderDetail(o, m, orderItemDao.findByOrderIdWithDrinkName(orderId));
    }

    private static void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignore) {
                // 回滚失败时连接关闭后 MySQL 也会丢弃未提交事务
            }
        }
    }
}
