package com.cartisan.data.jpa.integration;

import jakarta.persistence.*;

/**
 * 测试用管理员实体。
 */
@Entity
@Table(name = "test_admin_users")
public class TestAdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username")
    private String username;

    @Column(name = "status")
    // 注意：无需 @Convert 或 @Enumerated 注解
    private TestAdminUserStatus status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public TestAdminUserStatus getStatus() {
        return status;
    }

    public void setStatus(TestAdminUserStatus status) {
        this.status = status;
    }
}
