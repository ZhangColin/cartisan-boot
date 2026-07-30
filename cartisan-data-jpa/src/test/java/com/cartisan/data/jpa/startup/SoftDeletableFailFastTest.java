package com.cartisan.data.jpa.startup;

import com.cartisan.data.jpa.startup.broken.TestSoftDeletableMissingDeletedColumn;
import com.cartisan.data.jpa.startup.valid.ValidSoftDeletableEntity;
import org.hibernate.HibernateException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * 软删除读过滤 fail-fast 测试（Issue #5）。
 *
 * <p>验证实现 {@link com.cartisan.data.jpa.domain.SoftDeletable} 但缺失 {@code deleted}
 * 持久化列的实体在<b>启动期</b>即失败，错误消息指明实体类与契约要求——而非运行期才抛 SQL 异常。</p>
 *
 * <p>测试基座与模块现有集成测试同层：编程式拉起 Spring 上下文，观察是否启动失败。
 * 不引入新 seam。</p>
 */
class SoftDeletableFailFastTest {

    private static final String ENTITY_SIMPLE_NAME = TestSoftDeletableMissingDeletedColumn.class.getSimpleName();

    @Test
    void shouldFailToStart_when_softDeletableEntityMissingDeletedColumn() {
        Throwable thrown = catchThrowable(() -> startContext(BrokenEntityConfig.class));

        assertThat(thrown)
                .as("实现 SoftDeletable 但缺 deleted 列的实体应使上下文启动失败")
                .isNotNull();

        assertThat(causeChain(thrown))
                .as("失败应源自框架的软删契约校验（HibernateException 族）")
                .anyMatch(HibernateException.class::isInstance);

        List<String> messages = causeChainMessages(thrown);
        assertThat(messages)
                .as("错误消息应指明违约的实体类")
                .anyMatch(m -> m.contains(ENTITY_SIMPLE_NAME));
        assertThat(messages)
                .as("错误消息应说明契约要求 'deleted' 列")
                .anyMatch(m -> m.contains("deleted"));
    }

    @Test
    void shouldStartSuccessfully_when_softDeletableEntityHasDeletedColumn() {
        try (ConfigurableApplicationContext ctx = startContext(ValidEntityConfig.class)) {
            assertThat(ctx.isRunning())
                    .as("合法 SoftDeletable 实体（含 deleted 列）应正常启动，校验不误报")
                    .isTrue();
        }
    }

    private ConfigurableApplicationContext startContext(Class<?> configClass) {
        return new SpringApplicationBuilder(configClass)
                .web(WebApplicationType.NONE)
                .properties(
                        "spring.datasource.generate-unique-name=true",
                        "spring.jpa.hibernate.ddl-auto=update",
                        // 失败用例会（应当）在启动期抛异常并打印堆栈；此处断言经异常链程序化校验，
                        // 故静默日志以保持测试输出洁净（合法用例的启动信息亦无需打印）。
                        "logging.level.root=OFF"
                )
                .run();
    }

    private static List<Throwable> causeChain(Throwable t) {
        List<Throwable> chain = new ArrayList<>();
        for (Throwable c = t; c != null; c = c.getCause()) {
            chain.add(c);
        }
        return chain;
    }

    private static List<String> causeChainMessages(Throwable t) {
        return causeChain(t).stream()
                .map(Throwable::getMessage)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = TestSoftDeletableMissingDeletedColumn.class)
    static class BrokenEntityConfig {
    }

    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = ValidSoftDeletableEntity.class)
    static class ValidEntityConfig {
    }
}
