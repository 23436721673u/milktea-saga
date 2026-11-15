package com.tea.entity;

import java.math.BigDecimal;

/** 订单明细实体（对应 order_item 表）。 */
public class OrderItem {

    private Long id;
    private Long orderId;
    private Long drinkId;
    private Integer quantity;
    /** 下单时的单价快照：之后奶茶改价，历史订单金额不受影响。 */
    private BigDecimal unitPrice = BigDecimal.ZERO;
    /** 小计 = unitPrice × quantity */
    private BigDecimal subtotal = BigDecimal.ZERO;

    /** 展示字段：表里没有，JOIN drink 查询时填充。 */
    private String drinkName;

    public OrderItem() {
    }

    public OrderItem(Long drinkId, String drinkName, int quantity, BigDecimal unitPrice) {
        this.drinkId = drinkId;
        this.drinkName = drinkName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /** 修改杯数时同步重算小计，保证 subtotal 永远等于 unitPrice × quantity。 */
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        if (unitPrice != null && quantity != null) {
            this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getDrinkId() { return drinkId; }
    public void setDrinkId(Long drinkId) { this.drinkId = drinkId; }

    public Integer getQuantity() { return quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public String getDrinkName() { return drinkName; }
    public void setDrinkName(String drinkName) { this.drinkName = drinkName; }

    @Override
    public String toString() {
        return String.format("%s × %d  ¥%s = ¥%s",
                drinkName != null ? drinkName : "drink#" + drinkId,
                quantity, unitPrice.toPlainString(), subtotal.toPlainString());
    }
}
