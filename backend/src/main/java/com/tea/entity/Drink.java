package com.tea.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 奶茶商品实体（对应 drink 表）。 */
public class Drink {

    private Long id;
    private String name;
    /** 分类：奶茶 / 果茶 / 特调 */
    private String category;
    /** 金额一律用 BigDecimal，避免 double 精度误差。 */
    private BigDecimal price;
    private Integer stock;
    /** 1=在售 0=下架（软删除） */
    private Integer isActive;
    private LocalDateTime createdAt;

    public Drink() {
    }

    public Drink(String name, String category, BigDecimal price, Integer stock) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.isActive = 1;
    }

    public boolean isOnSale() {
        return isActive != null && isActive == 1;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Integer getIsActive() { return isActive; }
    public void setIsActive(Integer isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return String.format("[%d] %s（%s）¥%s 库存 %d%s", id, name, category,
                price == null ? "-" : price.toPlainString(), stock, isOnSale() ? "" : " [已下架]");
    }
}
