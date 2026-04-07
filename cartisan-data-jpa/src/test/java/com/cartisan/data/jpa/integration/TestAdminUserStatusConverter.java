package com.cartisan.data.jpa.integration;

import com.cartisan.data.jpa.converter.BaseEnumConverter;
import jakarta.persistence.Converter;

/**
 * TestAdminUserStatus 的 JPA 转换器。
 */
@Converter(autoApply = true)
public class TestAdminUserStatusConverter extends BaseEnumConverter<TestAdminUserStatus> {
    public TestAdminUserStatusConverter() {
        super(TestAdminUserStatus.class);
    }
}
