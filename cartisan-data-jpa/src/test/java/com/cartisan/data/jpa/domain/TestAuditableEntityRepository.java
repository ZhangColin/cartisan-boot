package com.cartisan.data.jpa.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 测试用 Repository，用于审计功能集成测试。
 */
interface TestAuditableEntityRepository extends JpaRepository<TestAuditableEntity, Long> {
}
