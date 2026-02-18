package com.smartblog.core.mapper;

import com.smartblog.core.dto.TagDTO;
import com.smartblog.core.model.Tag;

/**
 * Mapper utility for converting Tag entities to DTOs.
 */
public class TagMapper {

    private TagMapper() {
        // Utility class
    }

    public static TagDTO toDTO(Tag tag) {
        if (tag == null) {
            return null;
        }

        return new TagDTO(
                tag.getId(),
                tag.getName());
    }
}
