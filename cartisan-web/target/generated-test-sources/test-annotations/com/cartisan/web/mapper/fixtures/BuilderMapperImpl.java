package com.cartisan.web.mapper.fixtures;

import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-03T18:35:32+0800",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.10 (Oracle Corporation)"
)
public class BuilderMapperImpl implements BuilderMapper {

    @Override
    public BuilderDto convert(SimpleEntity source) {
        if ( source == null ) {
            return null;
        }

        BuilderDto.BuilderDtoBuilder builderDto = BuilderDto.builder();

        builderDto.id( source.id() );
        builderDto.name( source.name() );
        builderDto.email( source.email() );

        return builderDto.build();
    }

    @Override
    public BuilderDto toBuilderDto(SimpleEntity entity) {
        if ( entity == null ) {
            return null;
        }

        BuilderDto.BuilderDtoBuilder builderDto = BuilderDto.builder();

        builderDto.id( entity.id() );
        builderDto.name( entity.name() );
        builderDto.email( entity.email() );

        return builderDto.build();
    }
}
