
package com.smartblog.core.mapper;

import com.smartblog.core.dto.TagDTO;
import com.smartblog.core.model.Tag;

/**
 * Simple conversion between Tag and TagDTO.
 */
public final class TagMapper {
    private TagMapper() {}
    public static TagDTO toDTO(Tag t) {
        return new TagDTO(t.getId(), t.getName());
    }
}
