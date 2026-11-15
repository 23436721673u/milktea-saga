package com.tea.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订单主表实体（对应 tea_order 表，避开 MySQL 保留字 order）。 */
public class TeaOrder {

    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private Long id;
    /** 会员 ID，散客为 null。 */
    private Long memberId;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private String status = STATUS_PAID;
    private LocalDateTime createdAt;

    public TeaOrder() {
    }

    public TeaOrder(Long memberId, BigDecimal totalAmount) {
        this.memberId = memberId;
        this.totalAmount = totalAmount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return String.format("Order[%d] member=%s ¥%s %s", id,
                memberId == null ? "散客" : memberId, totalAmount.toPlainString(), status);
    }
}
