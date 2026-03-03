package com.smartblog.core.convert;

import com.smartblog.core.model.UserRole;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter to persist UserRole enum as STRING but
 * map database values case-insensitively when reading into the entity.
 */
@Converter(autoApply = false)
public class UserRoleConverter implements AttributeConverter<UserRole, String> {

    @Override
    public String convertToDatabaseColumn(UserRole attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public UserRole convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return UserRole.READER;
        }
        try {
            return UserRole.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return UserRole.READER;
        }
    }
}
