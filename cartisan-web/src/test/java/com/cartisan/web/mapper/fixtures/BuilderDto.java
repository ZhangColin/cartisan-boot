package com.cartisan.web.mapper.fixtures;

/**
 * 测试用的传统 Java DTO（带 getter）。
 * 用于验证 MapStruct 可以映射到传统 POJO。
 */
public class BuilderDto {
    private final Long id;
    private final String name;
    private final String email;

    public BuilderDto(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

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
