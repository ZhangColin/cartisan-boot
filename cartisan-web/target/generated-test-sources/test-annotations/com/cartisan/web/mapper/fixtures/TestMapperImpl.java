package com.cartisan.web.mapper.fixtures;

import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-03T20:58:39+0800",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.10 (Oracle Corporation)"
)
public class TestMapperImpl implements TestMapper {

    @Override
    public SimpleDto convert(SimpleEntity source) {
        if ( source == null ) {
            return null;
        }

        Long id = null;
        String name = null;
        String email = null;

        id = source.id();
        name = source.name();
        email = source.email();

        SimpleDto simpleDto = new SimpleDto( id, name, email );

        return simpleDto;
    }
}
