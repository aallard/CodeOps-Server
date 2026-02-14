package com.codeops.config;

import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

class AsyncConfigTest {

    @Test
    void getAsyncExecutor_returnsConfiguredExecutor() {
        AsyncConfig config = new AsyncConfig();
        Executor executor = config.getAsyncExecutor();
        assertNotNull(executor);
        assertInstanceOf(ThreadPoolTaskExecutor.class, executor);

        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) executor;
        assertEquals(5, pool.getCorePoolSize());
        assertEquals(20, pool.getMaxPoolSize());
        assertEquals("codeops-async-", pool.getThreadNamePrefix());
        pool.shutdown();
    }

    @Test
    void getAsyncUncaughtExceptionHandler_returnsHandler() {
        AsyncConfig config = new AsyncConfig();
        AsyncUncaughtExceptionHandler handler = config.getAsyncUncaughtExceptionHandler();
        assertNotNull(handler);
    }

    @Test
    void asyncExceptionHandler_doesNotThrow() throws NoSuchMethodException {
        AsyncConfig config = new AsyncConfig();
        AsyncUncaughtExceptionHandler handler = config.getAsyncUncaughtExceptionHandler();
        assertDoesNotThrow(() -> handler.handleUncaughtException(
                new RuntimeException("test error"),
                AsyncConfig.class.getMethod("getAsyncExecutor"),
                new Object[]{}
        ));
    }
}
