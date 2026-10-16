package com.nodatanolife.bifusion_be.global.converter;

import jakarta.persistence.AttributeConverter;

/**
 * enum을 이름 문자열로 저장한다. (@Enumerated(STRING)과 저장 값은 같다)
 *
 * @Enumerated를 쓰면 Hibernate가 새 컬럼에 enum 값 목록으로 CHECK 제약을 만들어,
 * 나중에 enum 값을 추가하면 prod에서 저장이 막힌다. 컨버터로 매핑하면 일반 문자열 컬럼이 되어 CHECK 제약이 생기지 않는다.
 * 값이 늘어날 수 있는 enum 컬럼은 이 클래스를 상속한 컨버터로 매핑한다.
 */
public abstract class EnumNameConverter<E extends Enum<E>> implements AttributeConverter<E, String> {

    private final Class<E> enumType;

    protected EnumNameConverter(Class<E> enumType) {
        this.enumType = enumType;
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        return dbData != null ? Enum.valueOf(enumType, dbData) : null;
    }
}
