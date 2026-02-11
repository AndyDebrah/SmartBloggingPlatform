package com.smartblog.application.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Performance benchmarking utility for Epic 4.1.
 * Records query execution times and generates comparison reports for pre/post optimization.
 *
 * Usage for comparison:
 * PerformanceBenchmark bench = new PerformanceBenchmark();
 * bench.recordBefore("List all posts", () -> postService.list(1, 100));
 * // ... apply optimization (index, caching, etc.) ...
 * bench.recordAfter("List all posts", () -> postService.list(1, 100));
 * ComparisonReport report = bench.generateComparisonReport();
 */
public class PerformanceBenchmark {

    private final List<BenchmarkResult> results = new ArrayList<>();
    private final List<OptimizationPair> optimizationPairs = new ArrayList<>();

    /**
     * Record a "before optimization" benchmark.
     * @param testName Description of the test
     * @param operation The operation to measure
     * @return The result of the operation
     */
    public <T> T recordBefore(String testName, Supplier<T> operation) {
        long startTime = System.nanoTime();
        T result = operation.get();
        long endTime = System.nanoTime();

        double durationMs = (endTime - startTime) / 1_000_000.0;
        BenchmarkResult br = new BenchmarkResult(testName, durationMs, "BEFORE");
        results.add(br);

        // Store for pairing
        OptimizationPair pair = findOrCreatePair(testName);
        pair.before = br;

        System.out.println(String.format("[BENCHMARK BEFORE] %s took %.3f ms", testName, durationMs));
        return result;
    }

    /**
     * Record an "after optimization" benchmark.
     * @param testName Description of the test (must match recordBefore testName)
     * @param operation The operation to measure
     * @return The result of the operation
     */
    public <T> T recordAfter(String testName, Supplier<T> operation) {
        long startTime = System.nanoTime();
        T result = operation.get();
        long endTime = System.nanoTime();

        double durationMs = (endTime - startTime) / 1_000_000.0;
        BenchmarkResult br = new BenchmarkResult(testName, durationMs, "AFTER");
        results.add(br);

        // Store for pairing
        OptimizationPair pair = findOrCreatePair(testName);
        pair.after = br;

        System.out.println(String.format("[BENCHMARK AFTER] %s took %.3f ms", testName, durationMs));
        return result;
    }

    private OptimizationPair findOrCreatePair(String testName) {
        for (OptimizationPair pair : optimizationPairs) {
            if (pair.testName.equals(testName)) {
                return pair;
            }
        }
        OptimizationPair newPair = new OptimizationPair(testName);
        optimizationPairs.add(newPair);
        return newPair;
    }

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
     * Generate a comparison report from all optimization pairs.
     * @return ComparisonReport showing before/after optimizations
     */
    public ComparisonReport generateComparisonReport() {
        return new ComparisonReport(optimizationPairs);
    }

    /**
     * Clear all recorded results and optimization pairs.
     */
    public void reset() {
        results.clear();
        optimizationPairs.clear();
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
    public record BenchmarkResult(String testName, double durationMs, String phase) {
        public BenchmarkResult(String testName, double durationMs) {
            this(testName, durationMs, "STANDARD");
        }

        @Override
        public String toString() {
            return String.format("[%s] %-40s %8.3f ms", phase, testName, durationMs);
        }
    }

    /**
     * Holds before/after optimization pair for comparison.
     */
    static class OptimizationPair {
        String testName;
        BenchmarkResult before;
        BenchmarkResult after;

        OptimizationPair(String testName) {
            this.testName = testName;
        }

        /**
         * Calculate improvement percentage.
         * Positive percentage means improvement (faster), negative means regression (slower).
         * @return Improvement percentage
         */
        public double getImprovementPercentage() {
            if (before == null || after == null) {
                return 0.0;
            }
            return ((before.durationMs() - after.durationMs()) / before.durationMs()) * 100.0;
        }

        /**
         * Calculate absolute time saved in milliseconds.
         * @return Time saved (positive if faster, negative if slower)
         */
        public double getTimeSavedMs() {
            if (before == null || after == null) {
                return 0.0;
            }
            return before.durationMs() - after.durationMs();
        }

        public boolean isComplete() {
            return before != null && after != null;
        }
    }

    /**
     * Comparison report for optimization results.
     */
    public static class ComparisonReport {
        private final List<OptimizationPair> pairs;

        public ComparisonReport(List<OptimizationPair> pairs) {
            this.pairs = new ArrayList<>(pairs);
        }

        public List<OptimizationPair> getCompletePairs() {
            return pairs.stream().filter(OptimizationPair::isComplete).toList();
        }

        public double getAverageImprovementPercentage() {
            List<OptimizationPair> complete = getCompletePairs();
            if (complete.isEmpty()) return 0.0;
            return complete.stream()
                    .mapToDouble(OptimizationPair::getImprovementPercentage)
                    .average()
                    .orElse(0.0);
        }

        public double getTotalTimeSavedMs() {
            return getCompletePairs().stream()
                    .mapToDouble(OptimizationPair::getTimeSavedMs)
                    .sum();
        }

        /**
         * Generate formatted comparison report.
         */
        public String toFormattedString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=".repeat(80)).append("\n");
            sb.append("OPTIMIZATION COMPARISON REPORT\n");
            sb.append("=".repeat(80)).append("\n\n");

            List<OptimizationPair> complete = getCompletePairs();
            if (complete.isEmpty()) {
                sb.append("No complete optimization pairs found.\n");
                sb.append("Use recordBefore() and recordAfter() with matching test names.\n");
                return sb.toString();
            }

            sb.append("Optimization Results:\n");
            sb.append("-".repeat(80)).append("\n");
            for (OptimizationPair pair : complete) {
                sb.append(String.format("\nTest: %s\n", pair.testName));
                sb.append(String.format("  Before:       %.3f ms\n", pair.before.durationMs()));
                sb.append(String.format("  After:        %.3f ms\n", pair.after.durationMs()));
                sb.append(String.format("  Time Saved:   %.3f ms\n", pair.getTimeSavedMs()));
                sb.append(String.format("  Improvement:  %.2f%%\n", pair.getImprovementPercentage()));

                if (pair.getImprovementPercentage() > 0) {
                    sb.append("  Status:       ✓ IMPROVED\n");
                } else if (pair.getImprovementPercentage() < 0) {
                    sb.append("  Status:       ✗ REGRESSION\n");
                } else {
                    sb.append("  Status:       = NO CHANGE\n");
                }
            }
            sb.append("-".repeat(80)).append("\n\n");

            sb.append("Summary:\n");
            sb.append(String.format("  Total tests:             %d\n", complete.size()));
            sb.append(String.format("  Average improvement:     %.2f%%\n", getAverageImprovementPercentage()));
            sb.append(String.format("  Total time saved:        %.3f ms\n", getTotalTimeSavedMs()));

            sb.append("\n").append("=".repeat(80)).append("\n");
            return sb.toString();
        }

        @Override
        public String toString() {
            return toFormattedString();
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
