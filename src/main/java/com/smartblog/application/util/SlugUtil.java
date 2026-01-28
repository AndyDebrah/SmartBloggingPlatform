package com.smartblog.application.util;

/**
 * Utility class for generating and handling slugs.
 */

public class SlugUtil {

    private SlugUtil(){}
    public static String toSlug(String input){
        String s = input == null ? "" : input.trim().toLowerCase();
        s = s.replaceAll("[^a-z0-9\\s-]", "");
        s = s.replaceAll("[\\s-]+", " ");
        s = s.replaceAll("\\s", "-");
        return s;
    }
}
