
package com.smartblog.ui.navigation;

import java.util.HashMap;
import java.util.Map;

public class ViewParams {
    private final Map<String, Object> data = new HashMap<>();
    public ViewParams put(String key, Object value) { data.put(key, value); return this; }
    @SuppressWarnings("unchecked")
    public <T> T get(String key) { return (T) data.get(key); }
}
