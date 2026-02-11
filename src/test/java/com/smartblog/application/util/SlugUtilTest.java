package com.smartblog.application.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SlugUtilTest {

    @Test
    void toSlugHandlesNullAndPunctuation() {
        assertEquals("", SlugUtil.toSlug(null));
        assertEquals("hello-world", SlugUtil.toSlug("Hello, World!"));
        assertEquals("multiple-spaces-and-dashes", SlugUtil.toSlug("Multiple   spaces -- and dashes"));
        assertEquals("numbers-123-and-letters", SlugUtil.toSlug("Numbers 123 and Letters"));
    }
}
