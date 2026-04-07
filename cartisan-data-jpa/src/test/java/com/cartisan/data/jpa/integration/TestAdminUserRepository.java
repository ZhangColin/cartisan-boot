package com.cartisan.data.jpa.integration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestAdminUserRepository extends JpaRepository<TestAdminUser, Long> {
}
