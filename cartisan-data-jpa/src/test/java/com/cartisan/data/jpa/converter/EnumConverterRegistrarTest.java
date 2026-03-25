package com.cartisan.data.jpa.converter;

import com.cartisan.data.jpa.annotation.EnumConvert;
import com.cartisan.core.domain.BaseEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

enum TestPaymentStatus implements BaseEnum<TestPaymentStatus> {
    PAID(1, "已支付"),
    UNPAID(0, "未支付");

    private final Integer code;
    private final String name;

    TestPaymentStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public Integer getCode() { return code; }

    @Override
    public String getName() { return name; }
}

@Entity
class TestPaymentOrder {
    @Column(name = "id")
    private Long id;

    @Column(name = "payment_status")
    @EnumConvert(TestPaymentStatus.class)
    private TestPaymentStatus paymentStatus;

    @Column(name = "delivery_status")
    @EnumConvert(TestPaymentStatus.class)
    private TestPaymentStatus deliveryStatus;

    @Column(name = "amount")
    private Double amount;

    // Getters
    public Long getId() { return id; }
    public TestPaymentStatus getPaymentStatus() { return paymentStatus; }
    public TestPaymentStatus getDeliveryStatus() { return deliveryStatus; }
    public Double getAmount() { return amount; }
}

class EnumConverterRegistrarTest {

    @Test
    void shouldCreateConverter_forEnumType() {
        var registrar = new EnumConverterRegistrar();

        UniversalEnumConverter<?> converter = registrar.createConverter(TestPaymentStatus.class);

        assertThat(converter.getEnumType()).isEqualTo(TestPaymentStatus.class);
    }

    @Test
    void shouldConvertEnumToDatabaseColumn() {
        var converter = new UniversalEnumConverter<TestPaymentStatus>(TestPaymentStatus.class);

        assertThat(converter.convertToDatabaseColumn(TestPaymentStatus.PAID)).isEqualTo(1);
    }

    @Test
    void shouldConvertDatabaseColumnToEnum() {
        var converter = new UniversalEnumConverter<TestPaymentStatus>(TestPaymentStatus.class);

        assertThat(converter.convertToEntityAttribute(1)).isEqualTo(TestPaymentStatus.PAID);
    }

    @Test
    void shouldScanEnumTypes_whenEntityHasEnumConvertField() {
        var registrar = new EnumConverterRegistrar();

        // Test the scanFields method directly since we can't easily mock EntityManagerFactory
        Set<Class<?>> enumTypes = new HashSet<>();
        registrar.scanFields(TestPaymentOrder.class, enumTypes);

        assertThat(enumTypes).containsExactly(TestPaymentStatus.class);
    }

    @Test
    void shouldDeduplicateSameEnumTypes_whenMultipleFieldsUseSameEnum() {
        var registrar = new EnumConverterRegistrar();

        // Test the scanFields method directly since we can't easily mock EntityManagerFactory
        Set<Class<?>> enumTypes = new HashSet<>();
        registrar.scanFields(TestPaymentOrder.class, enumTypes);

        // Should only contain TestPaymentStatus once even though it's used in two fields
        assertThat(enumTypes).hasSize(1).containsExactly(TestPaymentStatus.class);
    }

    @Test
    void shouldScanEnumTypes_fromMultipleEntities() {
        var registrar = new EnumConverterRegistrar();

        // Test with first entity
        Set<Class<?>> enumTypes = new HashSet<>();
        registrar.scanFields(TestPaymentOrder.class, enumTypes);
        assertThat(enumTypes).containsExactly(TestPaymentStatus.class);

        // Clear and test with second entity
        enumTypes.clear();
        registrar.scanFields(AnotherEntity.class, enumTypes);
        assertThat(enumTypes).containsExactly(AnotherStatus.class);

        // Test with both entities together
        enumTypes.clear();
        registrar.scanFields(TestPaymentOrder.class, enumTypes);
        registrar.scanFields(AnotherEntity.class, enumTypes);

        // Should contain both test enums
        assertThat(enumTypes).hasSize(2)
            .containsExactlyInAnyOrder(TestPaymentStatus.class, AnotherStatus.class);
    }

    @Test
    void shouldNotScanEnumTypes_whenEntityHasNoEnumConvertFields() {
        var registrar = new EnumConverterRegistrar();

        // Test with an entity that has no @EnumConvert annotations
        Set<Class<?>> enumTypes = new HashSet<>();
        registrar.scanFields(NoEnumEntity.class, enumTypes);

        assertThat(enumTypes).isEmpty();
    }
}

@Entity
class AnotherEntity {
    @Column(name = "status")
    @EnumConvert(AnotherStatus.class)
    private AnotherStatus status;
}

enum AnotherStatus implements BaseEnum<AnotherStatus> {
    ACTIVE(1, "激活"),
    INACTIVE(0, "未激活");

    private final Integer code;
    private final String name;

    AnotherStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public Integer getCode() { return code; }
    @Override
    public String getName() { return name; }
}

@Entity
class NoEnumEntity {
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;
}