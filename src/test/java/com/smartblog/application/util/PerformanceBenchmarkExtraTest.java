package com.smartblog.application.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class PerformanceBenchmarkExtraTest {

    @Test
    void recordAndReportBehaviors() {
        PerformanceBenchmark bench = new PerformanceBenchmark();

        // record a simple benchmark
        String result = bench.record("simple", () -> "ok");
        assertEquals("ok", result);
        assertEquals(1, bench.size());

        // record a void benchmark
        bench.recordVoid("voidTest", () -> {});
        assertEquals(2, bench.size());

        // record average (2 iterations)
        bench.recordAverage("avgTest", () -> "x", 2);
        assertTrue(bench.size() >= 3);

        PerformanceBenchmark.BenchmarkReport report = bench.generateReport();
        List<PerformanceBenchmark.BenchmarkResult> results = report.getResults();
        assertTrue(results.size() >= 3);

        // reset clears results
        bench.reset();
        assertEquals(0, bench.size());
    }
}
