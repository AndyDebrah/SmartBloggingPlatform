package com.smartblog.application.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for PerformanceBenchmark utility.
 * Tests Epic 4.1 requirement for pre/post optimization comparison reports.
 */
class PerformanceBenchmarkTest {

    private PerformanceBenchmark benchmark;

    @BeforeEach
    void setUp() {
        benchmark = new PerformanceBenchmark();
    }

    @Test
    void testRecordBasic() {
        // Act: Record a simple benchmark
        String result = benchmark.record("test operation", () -> {
            sleep(10);
            return "success";
        });

        // Assert: Operation completes and benchmark is recorded
        assertEquals("success", result);
        assertEquals(1, benchmark.size());
    }

    @Test
    void testRecordVoid() {
        // Act: Record a void operation
        benchmark.recordVoid("void operation", () -> sleep(10));

        // Assert: Benchmark is recorded
        assertEquals(1, benchmark.size());
    }

    @Test
    void testRecordAverage() {
        // Act: Record average over 3 iterations
        String result = benchmark.recordAverage("average test", () -> {
            sleep(5);
            return "done";
        }, 3);

        // Assert: Returns result and records one average benchmark
        assertEquals("done", result);
        assertEquals(1, benchmark.size());
    }

    @Test
    void testRecordBeforeAndAfter() {
        // Arrange & Act: Simulate optimization - before is slower, after is faster
        String beforeResult = benchmark.recordBefore("query posts", () -> {
            sleep(50);  // Simulates slow query without index
            return "posts";
        });

        String afterResult = benchmark.recordAfter("query posts", () -> {
            sleep(10);  // Simulates fast query with index
            return "posts";
        });

        // Assert: Both operations complete
        assertEquals("posts", beforeResult);
        assertEquals("posts", afterResult);
        assertEquals(2, benchmark.size());
    }

    @Test
    void testGenerateComparisonReport() {
        // Arrange: Record before/after for two optimizations
        benchmark.recordBefore("list users", () -> {
            sleep(100);
            return "users";
        });
        benchmark.recordAfter("list users", () -> {
            sleep(30);
            return "users";
        });

        benchmark.recordBefore("search posts", () -> {
            sleep(80);
            return "posts";
        });
        benchmark.recordAfter("search posts", () -> {
            sleep(20);
            return "posts";
        });

        // Act: Generate comparison report
        PerformanceBenchmark.ComparisonReport report = benchmark.generateComparisonReport();

        // Assert: Report shows improvements
        assertNotNull(report);
        assertEquals(2, report.getCompletePairs().size());
        assertTrue(report.getAverageImprovementPercentage() > 0, 
            "Average improvement should be positive");
        assertTrue(report.getTotalTimeSavedMs() > 0, 
            "Total time saved should be positive");

        String reportString = report.toFormattedString();
        assertTrue(reportString.contains("OPTIMIZATION COMPARISON REPORT"));
        assertTrue(reportString.contains("list users"));
        assertTrue(reportString.contains("search posts"));
        assertTrue(reportString.contains("IMPROVED"));
    }

    @Test
    void testComparisonReportWithIncompleteData() {
        // Arrange: Record only "before" without matching "after"
        benchmark.recordBefore("incomplete test", () -> "data");

        // Act: Generate report
        PerformanceBenchmark.ComparisonReport report = benchmark.generateComparisonReport();

        // Assert: Report shows no complete pairs
        assertNotNull(report);
        assertEquals(0, report.getCompletePairs().size());
        assertEquals(0.0, report.getAverageImprovementPercentage());
        
        String reportString = report.toFormattedString();
        assertTrue(reportString.contains("No complete optimization pairs found"));
    }

    @Test
    void testOptimizationImprovementPercentage() {
        // Arrange: Record optimization with 50% improvement
        benchmark.recordBefore("calculate", () -> {
            sleep(100);
            return 42;
        });
        benchmark.recordAfter("calculate", () -> {
            sleep(50);  // 50% faster
            return 42;
        });

        // Act: Generate report
        PerformanceBenchmark.ComparisonReport report = benchmark.generateComparisonReport();

        // Assert: Improvement is approximately 50%
        double improvement = report.getAverageImprovementPercentage();
        assertTrue(improvement > 40 && improvement < 60, 
            "Improvement should be around 50%, was: " + improvement + "%");
    }

    @Test
    void testRegressionDetection() {
        // Arrange: Record regression where "after" is slower (simulating bad optimization)
        benchmark.recordBefore("query", () -> {
            sleep(20);
            return "result";
        });
        benchmark.recordAfter("query", () -> {
            sleep(50);  // Slower!
            return "result";
        });

        // Act: Generate report
        PerformanceBenchmark.ComparisonReport report = benchmark.generateComparisonReport();

        // Assert: Report shows negative improvement (regression)
        assertTrue(report.getAverageImprovementPercentage() < 0, 
            "Should detect regression");
        assertTrue(report.getTotalTimeSavedMs() < 0, 
            "Time saved should be negative for regression");
        
        String reportString = report.toFormattedString();
        assertTrue(reportString.contains("REGRESSION"));
    }

    @Test
    void testGenerateStandardReport() {
        // Arrange: Record multiple standard benchmarks
        benchmark.record("test1", () -> {
            sleep(10);
            return "a";
        });
        benchmark.record("test2", () -> {
            sleep(20);
            return "b";
        });

        // Act: Generate standard report
        PerformanceBenchmark.BenchmarkReport report = benchmark.generateReport();

        // Assert: Report contains all results
        assertNotNull(report);
        assertEquals(2, report.getResults().size());
        assertTrue(report.getTotalDuration() > 0);
        assertTrue(report.getAverageDuration() > 0);
        assertNotNull(report.getSlowest());
        assertNotNull(report.getFastest());

        String reportString = report.toFormattedString();
        assertTrue(reportString.contains("PERFORMANCE BENCHMARK REPORT"));
    }

    @Test
    void testReset() {
        // Arrange: Record some benchmarks
        benchmark.record("test1", () -> "a");
        benchmark.recordBefore("test2", () -> "b");
        benchmark.recordAfter("test2", () -> "c");

        assertEquals(3, benchmark.size());

        // Act: Reset
        benchmark.reset();

        // Assert: All data cleared
        assertEquals(0, benchmark.size());
        PerformanceBenchmark.ComparisonReport report = benchmark.generateComparisonReport();
        assertEquals(0, report.getCompletePairs().size());
    }

    /**
     * Helper method to simulate operation duration.
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
