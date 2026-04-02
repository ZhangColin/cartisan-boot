package com.cartisan.web.controller;

import com.cartisan.web.enums.EnumRegistry;
import com.cartisan.web.response.EnumOption;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 枚举选项 Controller 基类。
 *
 * <p>业务项目可继承此类并自定义 URL 路径和业务逻辑。
 *
 * @since 0.9.0
 */
public abstract class EnumControllerBase {

    protected final EnumRegistry enumRegistry;

    protected EnumControllerBase(EnumRegistry enumRegistry) {
        this.enumRegistry = enumRegistry;
    }

    /**
     * 批量获取枚举选项。
     *
     * @param enumNames 枚举类名列表
     * @return key=类名, value=选项列表
     */
    protected Map<String, List<EnumOption>> batchEnums(List<String> enumNames) {
        Map<String, List<EnumOption>> result = new HashMap<>();
        for (String enumName : enumNames) {
            result.put(enumName, enumRegistry.getEnumOptions(enumName));
        }
        return result;
    }
}