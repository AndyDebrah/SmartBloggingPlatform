package com.smartblog.config;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;

@Configuration
public class AsyncConfig {

    @Value("${app.async.corePoolSize:10}")
    private int corePoolSize;

    @Value("${app.async.maxPoolSize:50}")
    private int maxPoolSize;

    @Value("${app.async.queueCapacity:500}")
    private int queueCapacity;

    @Value("${app.async.threadNamePrefix:epic2-exec-}")
    private String threadNamePrefix;

    @Value("${app.async.awaitTerminationSeconds:30}")
    private int awaitTerminationSeconds;

    @Bean("epic2TaskExecutor")
    public Executor taskExecutor() {
        int safeCore = Math.max(1, corePoolSize);
        int safeMax = Math.max(safeCore, maxPoolSize);
        int safeQueueCapacity = Math.max(1, queueCapacity);
        int safeAwaitTerminationSeconds = Math.max(1, awaitTerminationSeconds);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(safeCore);
        executor.setMaxPoolSize(safeMax);
        executor.setQueueCapacity(safeQueueCapacity);
        executor.setThreadNamePrefix(StringUtils.hasText(threadNamePrefix) ? threadNamePrefix : "epic2-exec-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(safeAwaitTerminationSeconds);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
