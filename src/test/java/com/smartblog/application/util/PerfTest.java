package com.smartblog.application.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PerfTest {

    @Test
    void measureReturnsSupplierValue() {
        String result = Perf.measure("label", () -> "ok");
        assertEquals("ok", result);
    }

    @Test
    void measurePropagatesException() {
        assertThrows(RuntimeException.class, () ->
            Perf.measure("fail", () -> { throw new RuntimeException("boom"); })
        );
    }
}
