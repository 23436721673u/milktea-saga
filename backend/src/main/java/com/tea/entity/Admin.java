package com.tea.entity;

import java.time.LocalDateTime;

/** 店员账号实体（对应 admin 表）。 */
public class Admin {

    private Long id;
    private String username;
    /** SHA-256 摘要，不是明文。 */
    private String password;
    private LocalDateTime createdAt;

    public Admin() {
    }

    public Admin(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Admin{id=" + id + ", username='" + username + "'}";
    }
}
