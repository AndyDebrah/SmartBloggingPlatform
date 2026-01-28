package com.smartblog.application.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Performance benchmarking utility for Epic 4.
 * Records query execution times and generates comparison reports.
 *
 * Usage:
 * PerformanceBenchmark bench = new PerformanceBenchmark();
 * bench.record("List all posts", () -> postService.list(1, 100));
 * BenchmarkReport report = bench.generateReport();
 */
public class PerformanceBenchmark {

    private final List<BenchmarkResult> results = new ArrayList<>();

    /**
     * Record a single benchmark test.
     * @param testName Description of the test
     * @param operation The operation to measure
     * @return The result of the operation
     */
    public <T> T record(String testName, Supplier<T> operation) {
        long startTime = System.nanoTime();
        T result = operation.get();
        long endTime = System.nanoTime();

        double durationMs = (endTime - startTime) / 1_000_000.0;
        results.add(new BenchmarkResult(testName, durationMs));

        System.out.println(String.format("[BENCHMARK] %s took %.3f ms", testName, durationMs));
        return result;
    }

    /**
     * Record a benchmark without returning a value.
     */
    public void recordVoid(String testName, Runnable operation) {
        record(testName, () -> {
            operation.run();
            return null;
        });
    }

    /**
     * Run a benchmark multiple times and record the average.
     * @param testName Description of the test
     * @param operation The operation to measure
     * @param iterations Number of times to run the test
     */
    public <T> T recordAverage(String testName, Supplier<T> operation, int iterations) {
        long totalNanos = 0;
        T result = null;

        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            result = operation.get();
            long endTime = System.nanoTime();
            totalNanos += (endTime - startTime);
        }

        double avgDurationMs = (totalNanos / (double) iterations) / 1_000_000.0;
        results.add(new BenchmarkResult(testName + " (avg of " + iterations + " runs)", avgDurationMs));

        System.out.println(String.format("[BENCHMARK] %s average: %.3f ms", testName, avgDurationMs));
        return result;
    }

    /**
     * Generate a report of all recorded benchmarks.
     */
    public BenchmarkReport generateReport() {
        return new BenchmarkReport(new ArrayList<>(results));
    }

    /**
     * Clear all recorded results.
     */
    public void reset() {
        results.clear();
    }

    /**
     * Get the number of recorded benchmarks.
     */
    public int size() {
        return results.size();
    }

    /**
     * Result of a single benchmark test.
     */
    public record BenchmarkResult(String testName, double durationMs) {
        @Override
        public String toString() {
            return String.format("%-50s %8.3f ms", testName, durationMs);
        }
    }

    /**
     * Report containing all benchmark results with analysis.
     */
    public static class BenchmarkReport {
        private final List<BenchmarkResult> results;

        public BenchmarkReport(List<BenchmarkResult> results) {
            this.results = results;
        }

        public List<BenchmarkResult> getResults() {
            return results;
        }

        public double getTotalDuration() {
            return results.stream().mapToDouble(BenchmarkResult::durationMs).sum();
        }

        public double getAverageDuration() {
            if (results.isEmpty()) return 0;
            return getTotalDuration() / results.size();
        }

        public BenchmarkResult getSlowest() {
            return results.stream()
                    .max((a, b) -> Double.compare(a.durationMs(), b.durationMs()))
                    .orElse(null);
        }

        public BenchmarkResult getFastest() {
            return results.stream()
                    .min((a, b) -> Double.compare(a.durationMs(), b.durationMs()))
                    .orElse(null);
        }

        /**
         * Generate a formatted text report.
         */
        public String toFormattedString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=" .repeat(80)).append("\n");
            sb.append("PERFORMANCE BENCHMARK REPORT\n");
            sb.append("=" .repeat(80)).append("\n\n");

            sb.append("Test Results:\n");
            sb.append("-".repeat(80)).append("\n");
            for (BenchmarkResult result : results) {
                sb.append(result.toString()).append("\n");
            }
            sb.append("-".repeat(80)).append("\n\n");

            sb.append("Summary Statistics:\n");
            sb.append(String.format("  Total tests:      %d\n", results.size()));
            sb.append(String.format("  Total duration:   %.3f ms\n", getTotalDuration()));
            sb.append(String.format("  Average duration: %.3f ms\n", getAverageDuration()));

            BenchmarkResult slowest = getSlowest();
            if (slowest != null) {
                sb.append(String.format("  Slowest test:     %s (%.3f ms)\n",
                    slowest.testName(), slowest.durationMs()));
            }

            BenchmarkResult fastest = getFastest();
            if (fastest != null) {
                sb.append(String.format("  Fastest test:     %s (%.3f ms)\n",
                    fastest.testName(), fastest.durationMs()));
            }

            sb.append("\n").append("=".repeat(80)).append("\n");
            return sb.toString();
        }

        @Override
        public String toString() {
            return toFormattedString();
        }
    }
}