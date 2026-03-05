package com.smartblog.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

class AsyncConfigTest {

    @Test
    void taskExecutor_shouldApplyConfiguredValues() {
        AsyncConfig config = new AsyncConfig();
        ReflectionTestUtils.setField(config, "corePoolSize", 8);
        ReflectionTestUtils.setField(config, "maxPoolSize", 32);
        ReflectionTestUtils.setField(config, "queueCapacity", 300);
        ReflectionTestUtils.setField(config, "threadNamePrefix", "epic2-exec-");
        ReflectionTestUtils.setField(config, "awaitTerminationSeconds", 45);

        Executor executor = config.taskExecutor();
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) executor;
        try {
            assertThat(pool.getCorePoolSize()).isEqualTo(8);
            assertThat(pool.getMaxPoolSize()).isEqualTo(32);
            assertThat(pool.getThreadNamePrefix()).isEqualTo("epic2-exec-");
            assertThat(pool.getThreadPoolExecutor().getQueue().remainingCapacity()).isEqualTo(300);
            assertThat(pool.getThreadPoolExecutor().getRejectedExecutionHandler())
                    .isInstanceOf(java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy.class);
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void taskExecutor_shouldSanitizeInvalidValues() {
        AsyncConfig config = new AsyncConfig();
        ReflectionTestUtils.setField(config, "corePoolSize", 0);
        ReflectionTestUtils.setField(config, "maxPoolSize", 0);
        ReflectionTestUtils.setField(config, "queueCapacity", -10);
        ReflectionTestUtils.setField(config, "threadNamePrefix", " ");
        ReflectionTestUtils.setField(config, "awaitTerminationSeconds", 0);

        Executor executor = config.taskExecutor();
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) executor;
        try {
            assertThat(pool.getCorePoolSize()).isEqualTo(1);
            assertThat(pool.getMaxPoolSize()).isEqualTo(1);
            assertThat(pool.getThreadNamePrefix()).isEqualTo("epic2-exec-");
            assertThat(pool.getThreadPoolExecutor().getQueue().remainingCapacity()).isEqualTo(1);
        } finally {
            pool.shutdown();
        }
    }
}

