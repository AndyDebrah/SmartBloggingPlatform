package com.smartblog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Central runtime toggle bean referenced by cache annotations.
 */
@Component("optimizationToggle")
public class OptimizationToggle {

    @Value("${app.optimization.caching.enabled:true}")
    private boolean cachingEnabled;

    /**
     * Indicates whether cache annotations should be active for Epic 4/5 comparisons.
     */
    public boolean isCachingEnabled() {
        return cachingEnabled;
    }
}
