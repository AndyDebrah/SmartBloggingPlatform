package com.smartblog.ui.view.performance;

import java.util.List;

import com.smartblog.application.service.CommentService;
import com.smartblog.application.service.PostService;
import com.smartblog.application.service.TagService;
import com.smartblog.application.util.PerformanceBenchmark;
import com.smartblog.application.util.PerformanceBenchmark.BenchmarkResult;
import com.smartblog.infrastructure.caching.CacheManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


/**
 * ViewModel for Performance Report view.
 * Runs benchmark tests and provides data for UI display.
 */
public class PerformanceViewModel {

    private final PostService postService;
    private final CommentService commentService;
    private final TagService tagService;
    private final PerformanceBenchmark benchmark;

    public PerformanceViewModel(PostService postService, CommentService commentService, TagService tagService) {
        this.postService = postService;
        this.commentService = commentService;
        this.tagService = tagService;
        this.benchmark = new PerformanceBenchmark();
    }

    /**
     * Run all performance benchmark tests into the controller-owned benchmark.
     */
    public void runBenchmarks() {
        benchmark.reset();
        runTestsInto(benchmark, "");
    }

    private void runTestsInto(PerformanceBenchmark bench, String suffix) {
        // Test 1: List posts
        bench.recordAverage("List 100 posts" + suffix,
                () -> postService.list(1, 100), 5);

        // Test 2: Keyword search
        bench.recordAverage("Search posts by keyword 'java'" + suffix,
                () -> postService.search("java", 1, 50), 5);

        // Test 3: Posts by author
        bench.recordAverage("List posts by author" + suffix,
                () -> postService.listByAuthor(1L, 1, 50), 5);

        // Test 4: Tag filter
        bench.recordAverage("Search by tag 'JavaFX'" + suffix,
                () -> postService.searchByTag("JavaFX", 1, 50), 5);

        // Test 5: Combined search
        bench.recordAverage("Combined search" + suffix,
                () -> postService.searchCombined("java", "JavaFX", null, "date_desc", 1, 50), 5);

        // Test 6: Comments retrieval (assuming post ID 1 exists)
        bench.recordAverage("List comments for post" + suffix,
                () -> commentService.listForPost(1L, 1, 100), 5);

        // Test 7: Tag listing
        bench.recordAverage("List all tags" + suffix,
                () -> tagService.listAll(), 5);

        // Test 8: Cache performance
        String searchKeyword = "java";
        postService.search(searchKeyword, 1, 50); // Warm up cache
        bench.record("First search (cached)" + suffix,
                () -> postService.search(searchKeyword, 1, 50));
        bench.record("Second search (cached)" + suffix,
                () -> postService.search(searchKeyword, 1, 50));
    }

    /**
     * Run benchmarks into a temporary benchmark instance and return captured results.
     * If clearCaches is true, the CacheManager will be cleared before the run.
     */
    public List<BenchmarkResult> runBenchmarksCaptured(boolean clearCaches, String suffix) {
        if (clearCaches) {
            CacheManager.clearAll();
        }

        PerformanceBenchmark local = new PerformanceBenchmark();
        runTestsInto(local, suffix == null ? "" : suffix);
        return local.generateReport().getResults();
    }

    public ObservableList<BenchmarkResult> getResults() {
        return FXCollections.observableArrayList(benchmark.generateReport().getResults());
    }

    public int getTotalTests() {
        return benchmark.size();
    }

    public double getTotalDuration() {
        return benchmark.generateReport().getTotalDuration();
    }

    public double getAverageDuration() {
        return benchmark.generateReport().getAverageDuration();
    }

    public BenchmarkResult getSlowestTest() {
        return benchmark.generateReport().getSlowest();
    }

    public BenchmarkResult getFastestTest() {
        return benchmark.generateReport().getFastest();
    }

    public String exportReport() {
        return benchmark.generateReport().toFormattedString();
    }
}
