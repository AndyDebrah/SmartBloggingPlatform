package com.smartblog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.aop.TimedAspect;

/**
 * Micrometer/AOP bridge for `@Timed` annotations used in Module 8 optimization epics.
 */
@Configuration
public class MetricsConfig {

    /**
     * Registers the aspect that records method timings into MeterRegistry.
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
