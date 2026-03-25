package com.cartisan.data.jpa.integration;

import com.cartisan.core.domain.BaseEnum;
import com.cartisan.data.jpa.annotation.EnumConvert;
import com.cartisan.data.jpa.converter.TestUserStatusConverter;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

/**
 * 枚举增强集成测试。
 * <p>
 * 验证 BaseEnum ↔ JPA ↔ Database 的完整流程。
 */
@DataJpaTest
@ContextConfiguration(classes = EnumIntegrationTestConfiguration.class)
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EnumIntegrationTest {

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    void shouldPersistEnumAsInteger() {
        // 删除表（如果存在）
        testEntityManager.getEntityManager().createNativeQuery("DROP TABLE IF EXISTS test_users")
            .executeUpdate();

        // 创建测试表
        testEntityManager.getEntityManager().createNativeQuery("CREATE TABLE test_users (id BIGINT PRIMARY KEY, status INTEGER)")
            .executeUpdate();

        TestUser user = new TestUser();
        user.id = 1L;
        user.status = TestUserStatus.ACTIVE;

        testEntityManager.getEntityManager().persist(user);
        testEntityManager.getEntityManager().flush();
        testEntityManager.getEntityManager().clear();

        // 验证数据库中存储的是 int
        Integer dbValue = (Integer) testEntityManager.getEntityManager().createNativeQuery("SELECT status FROM test_users WHERE id = 1")
            .getSingleResult();
        assertThat(dbValue).isEqualTo(1);
    }

    @Test
    void shouldLoadIntegerAsEnum() {
        // 删除表（如果存在）
        testEntityManager.getEntityManager().createNativeQuery("DROP TABLE IF EXISTS test_users")
            .executeUpdate();

        // 插入原始数据
        testEntityManager.getEntityManager().createNativeQuery("CREATE TABLE test_users (id BIGINT PRIMARY KEY, status INTEGER)")
            .executeUpdate();
        testEntityManager.getEntityManager().createNativeQuery("INSERT INTO test_users (id, status) VALUES (2, 0)")
            .executeUpdate();

        TestUser user = testEntityManager.getEntityManager().find(TestUser.class, 2L);

        assertThat(user.status).isEqualTo(TestUserStatus.INACTIVE);
    }

    @Test
    void shouldHandleNullEnumValue() {
        // 删除表（如果存在）
        testEntityManager.getEntityManager().createNativeQuery("DROP TABLE IF EXISTS test_users")
            .executeUpdate();

        // 创建测试表
        testEntityManager.getEntityManager().createNativeQuery("CREATE TABLE test_users (id BIGINT PRIMARY KEY, status INTEGER)")
            .executeUpdate();

        TestUser user = new TestUser();
        user.id = 3L;
        user.status = null;

        testEntityManager.getEntityManager().persist(user);
        testEntityManager.getEntityManager().flush();
        testEntityManager.getEntityManager().clear();

        TestUser loaded = testEntityManager.getEntityManager().find(TestUser.class, 3L);
        assertThat(loaded.status).isNull();
    }
}

// 测试实体
@Entity
@Table(name = "test_users")
class TestUser {
    @Id
    Long id;

    @Convert(converter = TestUserStatusConverter.class)
    @Column(name = "status")
    TestUserStatus status;
}


