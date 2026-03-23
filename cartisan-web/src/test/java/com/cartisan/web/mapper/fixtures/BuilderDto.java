package com.cartisan.web.mapper.fixtures;

import lombok.Builder;

/**
 * 测试用的 Lombok Builder DTO。
 * 用于验证 lombok-mapstruct-binding 集成。
 */
@Builder
public class BuilderDto {
    private final Long id;
    private final String name;
    private final String email;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}
