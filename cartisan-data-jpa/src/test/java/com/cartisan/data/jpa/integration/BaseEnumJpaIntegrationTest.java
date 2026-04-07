package com.cartisan.data.jpa.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("BaseEnum JPA 集成测试")
class BaseEnumJpaIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TestAdminUserRepository repository;

    @AfterEach
    void cleanup() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("应该保存并正确读取枚举的 code 值")
    void shouldSaveAndLoadEnumWithCorrectCode() {
        // Given: 创建一个管理员，状态为 ACTIVE (code=1)
        TestAdminUser user = new TestAdminUser();
        user.setUsername("admin");
        user.setStatus(TestAdminUserStatus.ACTIVE);

        // When: 保存到数据库
        repository.save(user);
        TestAdminUser saved = repository.findById(user.getId()).orElseThrow();

        // Then: 读取的状态应该是 ACTIVE
        assertThat(saved.getStatus()).isEqualTo(TestAdminUserStatus.ACTIVE);

        // 验证：数据库中存储的是 code 值 1，不是 ordinal 0
        // 通过原生查询验证
        Integer statusInDb = (Integer) entityManager.getEntityManager()
            .createNativeQuery("SELECT status FROM test_admin_users WHERE id = ?")
            .setParameter(1, user.getId())
            .getSingleResult();

        assertThat(statusInDb).isEqualTo(1); // ACTIVE.getCode()
    }

    @Test
    @DisplayName("应该正确处理 DISABLED 状态 (code=0, ordinal=1)")
    void shouldHandleDisabledStatusCorrectly() {
        // Given: DISABLED(0, "禁用")
        // ordinal = 1（第二个声明）
        // code = 0

        TestAdminUser user = new TestAdminUser();
        user.setUsername("admin2");
        user.setStatus(TestAdminUserStatus.DISABLED);

        // When
        repository.save(user);
        TestAdminUser saved = repository.findById(user.getId()).orElseThrow();

        // Then
        assertThat(saved.getStatus()).isEqualTo(TestAdminUserStatus.DISABLED);

        // 验证数据库存储的是 code=0，不是 ordinal=1
        Integer statusInDb = (Integer) entityManager.getEntityManager()
            .createNativeQuery("SELECT status FROM test_admin_users WHERE id = ?")
            .setParameter(1, user.getId())
            .getSingleResult();

        assertThat(statusInDb).isEqualTo(0); // DISABLED.getCode()
    }

    @Test
    @DisplayName("应该正确处理 null 状态")
    void shouldHandleNullStatus() {
        // Given
        TestAdminUser user = new TestAdminUser();
        user.setUsername("admin3");
        user.setStatus(null);

        // When
        repository.save(user);
        TestAdminUser saved = repository.findById(user.getId()).orElseThrow();

        // Then
        assertThat(saved.getStatus()).isNull();
    }

    @Test
    @DisplayName("应该能查询指定状态的用户")
    void shouldFindByStatus() {
        // Given
        TestAdminUser user1 = new TestAdminUser();
        user1.setUsername("admin1");
        user1.setStatus(TestAdminUserStatus.ACTIVE);

        TestAdminUser user2 = new TestAdminUser();
        user2.setUsername("admin2");
        user2.setStatus(TestAdminUserStatus.DISABLED);

        TestAdminUser user3 = new TestAdminUser();
        user3.setUsername("admin3");
        user3.setStatus(TestAdminUserStatus.ACTIVE);

        repository.save(user1);
        repository.save(user2);
        repository.save(user3);

        // When
        var activeUsers = repository.findAll().stream()
            .filter(u -> u.getStatus() == TestAdminUserStatus.ACTIVE)
            .toList();

        // Then
        assertThat(activeUsers).hasSize(2);
        assertThat(activeUsers)
            .allMatch(u -> u.getStatus() == TestAdminUserStatus.ACTIVE);
    }
}
