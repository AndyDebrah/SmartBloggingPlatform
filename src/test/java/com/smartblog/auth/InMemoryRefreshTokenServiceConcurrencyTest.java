package com.smartblog.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

class InMemoryRefreshTokenServiceConcurrencyTest {

    @Test
    void validateAndConsume_shouldAllowSingleWinnerUnderConcurrency() throws InterruptedException, ExecutionException {
        InMemoryRefreshTokenService service = new InMemoryRefreshTokenService();
        String token = service.createToken("alice");

        int callers = 24;
        ExecutorService pool = Executors.newFixedThreadPool(callers);
        try {
            List<Callable<String>> tasks = new ArrayList<>();
            for (int i = 0; i < callers; i++) {
                tasks.add(() -> service.validateAndConsume(token));
            }

            List<Future<String>> futures = pool.invokeAll(tasks);
            int successCount = 0;
            for (Future<String> future : futures) {
                String value = future.get();
                if ("alice".equals(value)) {
                    successCount++;
                } else {
                    assertNull(value);
                }
            }

            assertEquals(1, successCount, "Exactly one thread should consume token successfully");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void revoke_shouldInvalidateTokenImmediately() {
        InMemoryRefreshTokenService service = new InMemoryRefreshTokenService();
        String token = service.createToken("bob");
        service.revoke(token);
        assertNull(service.validateAndConsume(token));
    }
}
