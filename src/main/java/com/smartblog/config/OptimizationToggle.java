package com.smartblog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("optimizationToggle")
public class OptimizationToggle {

    @Value("${app.optimization.caching.enabled:true}")
    private boolean cachingEnabled;

    public boolean isCachingEnabled() {
        return cachingEnabled;
    }
}
