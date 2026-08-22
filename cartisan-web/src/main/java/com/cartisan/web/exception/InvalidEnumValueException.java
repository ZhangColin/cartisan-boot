package com.cartisan.web.exception;

import com.cartisan.core.domain.BaseEnum;

import java.util.Objects;

/**
 * BaseEnum 非法取值异常。
 *
 * <p>由 {@code BaseEnumConverter}（@RequestParam/@PathVariable 绑定）与
 * {@code BaseEnumDeserializer}（JSON body 反序列化）在取值无法匹配枚举时抛出，
 * 携带枚举类型与非法入参值，供 {@link GlobalExceptionHandler} 组装自描述的
 * 400 错误信封（参数名 + 非法值 + 合法取值表）。</p>
 *
 * <p>继承 {@link IllegalArgumentException}：语义上就是非法参数，
 * 同时兼容按 {@code IllegalArgumentException} 匹配 cause 链的既有处理逻辑。</p>
 *
 * <p>典型 message（通过 {@link #message} 组装）：</p>
 * <pre>{@code
 * status 取值 99 非法，合法取值：1=启用, 0=禁用, 2=待审核
 * }</pre>
 *
 * @since 0.10.0
 */
public class InvalidEnumValueException extends IllegalArgumentException {

    private final Class<?> enumType;

    private final Object invalidValue;

    /**
     * 构造异常。
     *
     * @param enumType 目标枚举类型
     * @param invalidValue 非法入参值（String 原文或解析后的值）
     */
    public InvalidEnumValueException(Class<?> enumType, Object invalidValue) {
        // PIT-004：参数校验必须嵌套在 super() 调用内部，null 时抛出清晰的消息而非 formatMessage 内 NPE
        super(message(null, Objects.requireNonNull(enumType, "enumType cannot be null"), invalidValue));
        this.enumType = enumType;
        this.invalidValue = invalidValue;
    }

    /**
     * 组装错误 message：{@code 名字 取值 X 非法，合法取值：code=名称对照表}。
     *
     * <p>名字为空时回退到枚举简单名。取值表复用 {@link BaseEnum#codeTableOf}，
     * 与 springdoc 契约共用同一格式。</p>
     *
     * @param name 参数名（query/path）或字段名（JSON body），可为 null
     * @param enumType 枚举类型
     * @param invalidValue 非法入参值
     * @return 组装后的 message
     */
    public static String message(String name, Class<?> enumType, Object invalidValue) {
        String label = (name == null || name.isEmpty()) ? enumType.getSimpleName() : name;
        return label + " 取值 " + invalidValue + " 非法，合法取值：" + BaseEnum.codeTableOf(enumType);
    }

    /**
     * 获取目标枚举类型。
     *
     * @return 枚举类型
     */
    public Class<?> getEnumType() {
        return enumType;
    }

    /**
     * 获取非法入参值。
     *
     * @return 非法值（String 原文或解析后的值）
     */
    public Object getInvalidValue() {
        return invalidValue;
    }
}
