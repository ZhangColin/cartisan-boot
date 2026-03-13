package com.cartisan.test.archunit.fixtures.violation.application;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

/**
 * 违规：应用服务直接使用 JPA EntityManager
 * 违反规则：applicationShouldNotAccessDatabaseDirectly
 */
@Service
public class BadAppServiceWithJPA {

    @PersistenceContext  // ❌ 应用服务不应该直接访问数据库
    private EntityManager entityManager;

    public void doSomething() {
        entityManager.createQuery("SELECT e FROM Entity e");
    }
}
