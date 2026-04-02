package com.cartisan.web.support;

import com.cartisan.web.config.TestUserStatus;
import com.cartisan.web.response.EnumOption;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnumOptionUtilsTest {

    @Test
    void shouldConvertEnumToOptions() {
        List<EnumOption> options = EnumOptionUtils.fromEnum(TestUserStatus.class);

        assertThat(options).hasSize(3);
        assertThat(options.get(0).code()).isEqualTo(1);
        assertThat(options.get(0).name()).isEqualTo("启用");
        assertThat(options.get(1).code()).isEqualTo(0);
        assertThat(options.get(1).name()).isEqualTo("禁用");
        assertThat(options.get(2).code()).isEqualTo(2);
        assertThat(options.get(2).name()).isEqualTo("待审核");
    }

    @Test
    void shouldConvertEnumArrayToOptions() {
        List<EnumOption> options = EnumOptionUtils.fromEnums(
            TestUserStatus.ACTIVE,
            TestUserStatus.DISABLED
        );

        assertThat(options).hasSize(2);
        assertThat(options.get(0).code()).isEqualTo(1);
        assertThat(options.get(0).name()).isEqualTo("启用");
        assertThat(options.get(1).code()).isEqualTo(0);
        assertThat(options.get(1).name()).isEqualTo("禁用");
    }
}