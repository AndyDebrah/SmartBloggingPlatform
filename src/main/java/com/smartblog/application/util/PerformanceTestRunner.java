package com.smartblog.application.util;

import com.smartblog.bootstrap.AppBootstrap;
import com.smartblog.core.dto.PostDTO;

import java.util.List;

/**
 * Performance test runner for Epic 4: Performance and Query Optimization.
 *
 * This class demonstrates measurable performance gains from:
 * 1. Database indexes
 * 2. Caching (already implemented with Caffeine)
 * 3. Query optimization
 *
 * Run this BEFORE and AFTER applying the V2__performance_indexes.sql migration.
 */
public class PerformanceTestRunner {

    public static void main(String[] args) {
        System.out.println("Starting Performance Benchmark Tests...\n");

        var ctx = AppBootstrap.start();
        var bench = new PerformanceBenchmark();

        // Test 1: List all posts (tests basic query performance)
        System.out.println("\n1. Testing list all posts...");
        bench.recordAverage("List 100 posts",
                () -> ctx.postService.list(1, 100),
                5);

        // Test 2: Search by keyword (tests full-text search)
        System.out.println("\n2. Testing keyword search...");
        bench.recordAverage("Search posts by keyword 'java'",
                () -> ctx.postService.search("java", 1, 100),
                5);

        // Test 3: List posts by author (tests author_id index)
        System.out.println("\n3. Testing posts by author...");
        var author = ctx.userService.findByUsername("author").orElse(null);
        if (author != null) {
            bench.recordAverage("List posts by author",
                    () -> ctx.postService.listByAuthor(author.id(), 1, 100),
                    5);
        }

        // Test 4: Search with tag filter (tests JOIN performance)
        System.out.println("\n4. Testing search with tag filter...");
        bench.recordAverage("Search by tag 'JavaFX'",
                () -> ctx.postService.searchByTag("JavaFX", 1, 100),
                5);

        // Test 5: Combined search (tests multiple indexes)
        System.out.println("\n5. Testing combined search...");
        bench.recordAverage("Combined search (keyword + tag + sort)",
                () -> ctx.postService.searchCombined("java", null, "JavaFX", "date_desc", 1, 100),
                5);

        // Test 6: List comments for post (tests comment indexes)
        System.out.println("\n6. Testing comments retrieval...");
        List<PostDTO> posts = ctx.postService.list(1, 1);
        if (!posts.isEmpty()) {
            long postId = posts.get(0).id();
            bench.recordAverage("List comments for post",
                    () -> ctx.commentService.listForPost(postId, 1, 100),
                    5);
        }

        // Test 7: List all tags (tests tag query performance)
        System.out.println("\n7. Testing tag listing...");
        bench.recordAverage("List all tags",
                () -> ctx.tagService.listAll(),
                5);

        // Test 8: Cache performance test (second run should be faster)
        System.out.println("\n8. Testing cache performance...");
        bench.record("First search (no cache)",
                () -> ctx.postService.search("test", 1, 50));
        bench.record("Second search (with cache)",
                () -> ctx.postService.search("test", 1, 50));

        // Generate and print report
        System.out.println("\n" + "=".repeat(80));
        System.out.println("GENERATING PERFORMANCE REPORT");
        System.out.println("=".repeat(80) + "\n");

        var report = bench.generateReport();
        System.out.println(report.toFormattedString());

        // Save report to file (optional)
        saveReportToFile(report);

        System.out.println("\nBenchmark complete!");
        System.out.println("Run this again AFTER applying V2__performance_indexes.sql migration");
        System.out.println("to see the performance improvements.\n");
    }

    private static void saveReportToFile(PerformanceBenchmark.BenchmarkReport report) {
        try {
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String filename = "performance_report_" + timestamp + ".txt";

            java.nio.file.Files.writeString(
                    java.nio.file.Path.of(filename),
                    report.toFormattedString()
            );

            System.out.println("\n✓ Report saved to: " + filename);
        } catch (Exception e) {
            System.err.println("Failed to save report: " + e.getMessage());
        }
    }
}