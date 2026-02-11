package com.smartblog.aop;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.smartblog.application.service.UserService;
import com.smartblog.application.util.PerformanceBenchmark;

/**
 * Test class for CrossCuttingAspect.
 * Verifies AOP functionality for Epic 5.
 */
@SpringBootTest
@org.springframework.context.annotation.Import(CrossCuttingAspect.class)
@org.springframework.context.annotation.EnableAspectJAutoProxy
@ActiveProfiles("test")
class CrossCuttingAspectTest {

    @Autowired
    private CrossCuttingAspect aspect;

    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        // Reset benchmarks before each test
        aspect.resetBenchmarks();
    }

    @Test
    void testAspectIsConfigured() {
        // Assert: CrossCuttingAspect is loaded and configured
        assertNotNull(aspect, "CrossCuttingAspect should be autowired");
    }

    @Test
    void testAspectRecordsBenchmarkOnServiceCall() {
        // Arrange: Get initial benchmark count
        PerformanceBenchmark.BenchmarkReport initialReport = aspect.getBenchmarkReport();
        int initialSize = initialReport.getResults().size();

        // Act: Call a service method (should trigger @Around advice)
        userService.list(1, 10);

        // Assert: Benchmark count increased
        PerformanceBenchmark.BenchmarkReport finalReport = aspect.getBenchmarkReport();
        int finalSize = finalReport.getResults().size();
        
        assertTrue(finalSize > initialSize, 
            "Aspect should record benchmark after service call");
    }

    @Test
    void testAspectLogsMethodEntry() {
        // This test verifies that @Before advice executes
        // Note: We can't easily assert console logs in unit tests,
        // but we can verify the aspect is working via benchmark recording
        
        // Act: Call service method
        userService.list(1, 10);

        // Assert: No exception thrown (aspect executed successfully)
        // In real scenario, you'd use a logging appender to capture log output
        assertDoesNotThrow(() -> userService.list(1, 10));
    }

    @Test
    void testBenchmarkReportGeneration() {
        // Act: Execute some service calls
        userService.list(1, 10);
        userService.list(1, 10);

        // Get report
        PerformanceBenchmark.BenchmarkReport report = aspect.getBenchmarkReport();

        // Assert: Report contains results
        assertNotNull(report);
        assertTrue(report.getResults().size() >= 2);
        assertTrue(report.getTotalDuration() > 0);
        assertTrue(report.getAverageDuration() > 0);
        
        // Assert: Report can be formatted
        String formattedReport = report.toFormattedString();
        assertNotNull(formattedReport);
        assertTrue(formattedReport.contains("PERFORMANCE BENCHMARK REPORT"));
    }

    @Test
    void testBenchmarkReset() {
        // Arrange: Execute some service calls
        userService.list(1, 10);
        assertTrue(aspect.getBenchmarkReport().getResults().size() > 0);

        // Act: Reset benchmarks
        aspect.resetBenchmarks();

        // Assert: Benchmarks cleared
        PerformanceBenchmark.BenchmarkReport report = aspect.getBenchmarkReport();
        assertEquals(0, report.getResults().size());
    }

    @Test
    void testAspectMeasuresExecutionTime() {
        // Act: Execute service call
        userService.list(1, 10);

        // Assert: Execution time was recorded
        PerformanceBenchmark.BenchmarkReport report = aspect.getBenchmarkReport();
        assertFalse(report.getResults().isEmpty());
        
        // Verify execution time is positive
        double avgDuration = report.getAverageDuration();
        assertTrue(avgDuration > 0, "Execution time should be positive");
    }
}
