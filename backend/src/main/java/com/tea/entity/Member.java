package com.tea.entity;

import java.time.LocalDateTime;

/** 会员实体（对应 member 表）。 */
public class Member {

    private Long id;
    private String name;
    /** 11 位手机号，数据库唯一约束。 */
    private String phone;
    /** 积分：满 1 元加 1 分。 */
    private Integer points;
    private LocalDateTime createdAt;

    public Member() {
    }

    public Member(String name, String phone) {
        this.name = name;
        this.phone = phone;
        this.points = 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return String.format("会员[%d] %s（%s）积分 %d", id, name, phone, points);
    }
}
