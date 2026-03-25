package com.cartisan.data.jpa.converter;

import com.cartisan.data.jpa.integration.TestUserStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * TestUserStatus 的 JPA 转换器。
 */
@Converter(autoApply = true)
public class TestUserStatusConverter implements AttributeConverter<TestUserStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(TestUserStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public TestUserStatus convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        // Manual lookup since the static method isn't accessible
        if (dbData.equals(TestUserStatus.ACTIVE.getCode())) {
            return TestUserStatus.ACTIVE;
        } else if (dbData.equals(TestUserStatus.INACTIVE.getCode())) {
            return TestUserStatus.INACTIVE;
        }
        throw new IllegalArgumentException("Unknown status code: " + dbData);
    }
}