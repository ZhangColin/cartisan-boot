package com.cartisan.web.config;

import com.cartisan.core.domain.BaseEnum;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

/**
 * BaseEnum Converter Factory。
 *
 * 将 String 参数（Integer code）转换为 BaseEnum 枚举类型。
 *
 * <p>支持 @RequestParam、@PathVariable 等场景直接使用枚举类型参数：</p>
 * <pre>
 * {@code
 * public void updateStatus(
 *         @PathVariable Long id,
 *         @RequestParam AdminStatus status) {  // "1" → AdminStatus.ACTIVE
 *     // ...
 * }
 * }
 * </pre>
 *
 * @since 0.3.0
 */
public class BaseEnumConverter implements ConverterFactory<String, BaseEnum<?>> {

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BaseEnum<?>> Converter<String, T> getConverter(Class<T> targetType) {
        return new StringToBaseEnumConverter<>((Class) targetType);
    }

    /**
     * String → BaseEnum Converter。
     * 只接受 Integer code 格式（如 "1"），不支持 name（如 "ACTIVE"）。
     */
    private static class StringToBaseEnumConverter<T extends Enum<T> & BaseEnum<T>>
            implements Converter<String, T> {

        private final Class<T> enumType;

        StringToBaseEnumConverter(Class<T> enumType) {
            this.enumType = enumType;
        }

        @Override
        public T convert(String source) {
            if (source == null || source.isEmpty()) {
                return null;
            }

            try {
                Integer code = Integer.valueOf(source);
                T result = BaseEnum.parseByCode(enumType, code);
                if (result == null) {
                    throw new IllegalArgumentException(
                        "Invalid enum code: " + code + " for " + enumType.getSimpleName()
                    );
                }
                return result;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    "Enum value must be Integer code, not string: " + source
                );
            }
        }
    }
}