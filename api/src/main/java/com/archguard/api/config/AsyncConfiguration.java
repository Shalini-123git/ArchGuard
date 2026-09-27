package com.archguard.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/** Supplies a bounded executor so scan requests cannot create unbounded worker threads. */
@Configuration
public class AsyncConfiguration {
    @Bean("scanExecutor")
    public ThreadPoolTaskExecutor scanExecutor(ArchGuardProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getScan().getCorePoolSize());
        executor.setMaxPoolSize(properties.getScan().getMaxPoolSize());
        executor.setQueueCapacity(properties.getScan().getQueueCapacity());
        executor.setThreadNamePrefix("archguard-scan-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
