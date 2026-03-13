package com.cartisan.test.fixture;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * 泛型对象构建器。
 *
 * <p>通过反射自动填充 POJO/JPA 实体字段，支持字段覆盖和递归深度限制。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 自动填充
 * Order order = FixtureBuilder.of(Order.class).build();
 *
 * // 覆盖字段
 * Order order = FixtureBuilder.of(Order.class)
 *     .with("status", OrderStatus.CANCELLED)
 *     .build();
 *
 * // 控制递归深度
 * Order order = FixtureBuilder.of(Order.class)
 *     .maxDepth(1)
 *     .build();
 * }</pre>
 *
 * @param <T> 目标类型
 * @since 0.1.0
 */
public final class FixtureBuilder<T> {

    private final Class<T> clazz;
    private final Map<String, Object> overrides = new HashMap<>();
    private int maxDepth = 3;

    private FixtureBuilder(Class<T> clazz) {
        this.clazz = clazz;
    }

    /**
     * 创建指定类的 FixtureBuilder。
     *
     * @param clazz 目标类（需有无参构造）
     * @param <T>   目标类型
     * @return FixtureBuilder 实例
     * @throws FixtureBuildException 如果类无无参构造或是 Record 类型
     */
    public static <T> FixtureBuilder<T> of(Class<T> clazz) {
        if (clazz.isRecord()) {
            throw new FixtureBuildException("Record types not supported yet: " + clazz.getName());
        }
        try {
            clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new FixtureBuildException(clazz.getName() + " has no no-arg constructor", e);
        }
        return new FixtureBuilder<>(clazz);
    }

    /**
     * 设置字段覆盖值。
     *
     * @param fieldName 字段名
     * @param value     覆盖值
     * @return this
     */
    public FixtureBuilder<T> with(String fieldName, Object value) {
        overrides.put(fieldName, value);
        return this;
    }

    /**
     * 设置递归填充的最大深度。
     *
     * @param depth 最大深度（必须 > 0）
     * @return this
     */
    public FixtureBuilder<T> maxDepth(int depth) {
        if (depth <= 0) {
            throw new IllegalArgumentException("maxDepth must be > 0");
        }
        this.maxDepth = depth;
        return this;
    }

    /**
     * 构建对象实例。
     *
     * @return 填充后的对象实例
     * @throws FixtureBuildException 如果构建失败
     */
    public T build() {
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            fillFields(instance, 0);
            applyOverrides(instance);
            return instance;
        } catch (FixtureBuildException e) {
            throw e; // 直接重新抛出，不要包装
        } catch (Exception e) {
            throw new FixtureBuildException("Failed to build " + clazz.getName(), e);
        }
    }

    /**
     * 递归填充字段。
     */
    private void fillFields(Object obj, int currentDepth) {
        if (currentDepth >= maxDepth) {
            setDefaultValues(obj);
            return;
        }

        for (Field field : obj.getClass().getDeclaredFields()) {
            if (overrides.containsKey(field.getName())) {
                continue;
            }
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            try {
                Object value = generateValueByType(field.getType(), currentDepth + 1);
                field.set(obj, value);
            } catch (IllegalAccessException e) {
                // 忽略无法设置的字段
            }
        }
    }

    /**
     * 根据类型生成值。
     */
    private Object generateValueByType(Class<?> type, int nextDepth) {
        if (type == int.class || type == Integer.class) {
            return 0;
        }
        if (type == long.class || type == Long.class) {
            return 0L;
        }
        if (type == boolean.class || type == Boolean.class) {
            return false;
        }
        if (type == double.class || type == Double.class) {
            return 0.0;
        }
        if (type == float.class || type == Float.class) {
            return 0.0f;
        }
        if (type == short.class || type == Short.class) {
            return (short) 0;
        }
        if (type == byte.class || type == Byte.class) {
            return (byte) 0;
        }
        if (type == char.class || type == Character.class) {
            return '\0';
        }
        if (type == String.class) {
            return FixtureStrings.randomString();
        }
        if (type == java.math.BigDecimal.class) {
            return FixtureNumbers.randomAmount();
        }
        if (type == java.time.LocalDateTime.class) {
            return FixtureDates.now();
        }
        if (type.isEnum()) {
            return type.getEnumConstants()[0];
        }
        // 自定义类型递归（深度限制）
        if (!type.isPrimitive() && !type.getName().startsWith("java.")) {
            try {
                return FixtureBuilder.of(type).maxDepth(maxDepth - nextDepth).build();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 设置超深时的默认值。
     */
    private void setDefaultValues(Object obj) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            if (overrides.containsKey(field.getName())) {
                continue;
            }
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            try {
                Class<?> type = field.getType();
                if (type == int.class) {
                    field.setInt(obj, 0);
                } else if (type == long.class) {
                    field.setLong(obj, 0L);
                } else if (type == boolean.class) {
                    field.setBoolean(obj, false);
                } else if (type == double.class) {
                    field.setDouble(obj, 0.0);
                } else if (type == float.class) {
                    field.setFloat(obj, 0.0f);
                } else if (type == short.class) {
                    field.setShort(obj, (short) 0);
                } else if (type == byte.class) {
                    field.setByte(obj, (byte) 0);
                } else if (type == char.class) {
                    field.setChar(obj, '\0');
                } else {
                    field.set(obj, null);
                }
            } catch (IllegalAccessException e) {
                // 忽略
            }
        }
    }

    /**
     * 应用用户指定的覆盖值。
     */
    private void applyOverrides(Object instance) {
        for (Map.Entry<String, Object> entry : overrides.entrySet()) {
            try {
                Field field = findField(instance.getClass(), entry.getKey());
                if (field == null) {
                    throw new FixtureBuildException("Field '" + entry.getKey() + "' not found in " + instance.getClass().getSimpleName());
                }
                field.setAccessible(true);
                field.set(instance, entry.getValue());
            } catch (IllegalAccessException e) {
                throw new FixtureBuildException("Failed to set field '" + entry.getKey() + "'", e);
            }
        }
    }

    /**
     * 查找字段（包括父类）。
     */
    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
