package com.smartblog.ui.view.performance;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.smartblog.application.service.CommentService;
import com.smartblog.application.service.PostService;
import com.smartblog.application.service.TagService;
import com.smartblog.application.util.PerformanceBenchmark.BenchmarkResult;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class PerformanceController {

    @FXML private TableView<BenchmarkResult> resultsTable;
    @FXML private TableColumn<BenchmarkResult, String> testNameColumn;
    @FXML private TableColumn<BenchmarkResult, Double> durationColumn;

    @FXML private Label totalTestsLbl;
    @FXML private Label totalDurationLbl;
    @FXML private Label avgDurationLbl;
    @FXML private Label slowestTestLbl;
    @FXML private Label fastestTestLbl;

    @FXML private Button runBenchmarkBtn;
    @FXML private Button runColdWarmBtn;
    @FXML private Button exportBtn;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label statusLbl;
    @FXML private CheckBox clearCacheChk;

    // Holds last combined results when running cold+warm
    private java.util.List<com.smartblog.application.util.PerformanceBenchmark.BenchmarkResult> lastCombinedResults = null;

    private PerformanceViewModel vm;

    public void setServices(PostService postService, CommentService commentService, TagService tagService) {
        this.vm = new PerformanceViewModel(postService, commentService, tagService);
    }

    @FXML
    public void initialize() {
        // Use explicit cell value factories to support record accessors
        testNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue() != null ? cell.getValue().testName() : ""));
        durationColumn.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue() != null ? cell.getValue().durationMs() : 0.0).asObject());

        exportBtn.setDisable(true);
        progressIndicator.setVisible(false);
        statusLbl.setText("Ready to run benchmarks");
    }

    @FXML
    private void handleRunBenchmarks() {
        // If the user requested a cold run, clear caches first
        if (clearCacheChk != null && clearCacheChk.isSelected()) {
            com.smartblog.infrastructure.caching.CacheManager.clearAll();
        }

        runBenchmarkBtn.setDisable(true);
        exportBtn.setDisable(true);
        progressIndicator.setVisible(true);
        statusLbl.setText("Running benchmarks...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                vm.runBenchmarks();
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            updateResults();
            exportBtn.setDisable(false);
            progressIndicator.setVisible(false);
            statusLbl.setText("Benchmarks completed successfully!");
        });

        task.setOnFailed(e -> {
            runBenchmarkBtn.setDisable(false);
            progressIndicator.setVisible(false);
            statusLbl.setText("Error running benchmarks");
            showError("Benchmark Error", "Failed to run benchmarks: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    private void updateResults() {
        resultsTable.setItems(vm.getResults());

        totalTestsLbl.setText(String.valueOf(vm.getTotalTests()));
        totalDurationLbl.setText(String.format("%.3f ms", vm.getTotalDuration()));
        avgDurationLbl.setText(String.format("%.3f ms", vm.getAverageDuration()));

        BenchmarkResult slowest = vm.getSlowestTest();
        if (slowest != null) {
            slowestTestLbl.setText(String.format("%s (%.3f ms)", slowest.testName(), slowest.durationMs()));
        }

        BenchmarkResult fastest = vm.getFastestTest();
        if (fastest != null) {
            fastestTestLbl.setText(String.format("%s (%.3f ms)", fastest.testName(), fastest.durationMs()));
        }
    }

    @FXML
    private void handleRunColdWarm() {
        runColdWarmBtn.setDisable(true);
        runBenchmarkBtn.setDisable(true);
        exportBtn.setDisable(true);
        progressIndicator.setVisible(true);
        statusLbl.setText("Running Cold then Warm benchmarks...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                // Cold run (clear caches)
                java.util.List<BenchmarkResult> cold = vm.runBenchmarksCaptured(true, " (cold)");

                // Warm run (no explicit clear)
                java.util.List<BenchmarkResult> warm = vm.runBenchmarksCaptured(false, " (warm)");

                // Combine
                lastCombinedResults = new java.util.ArrayList<>();
                lastCombinedResults.addAll(cold);
                lastCombinedResults.addAll(warm);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            // Build observable list
            javafx.collections.ObservableList<BenchmarkResult> items = javafx.collections.FXCollections.observableArrayList(lastCombinedResults);
            resultsTable.setItems(items);

            // Update summary based on combined
            double total = lastCombinedResults.stream().mapToDouble(BenchmarkResult::durationMs).sum();
            double avg = lastCombinedResults.isEmpty() ? 0 : total / lastCombinedResults.size();
            totalTestsLbl.setText(String.valueOf(lastCombinedResults.size()));
            totalDurationLbl.setText(String.format("%.3f ms", total));
            avgDurationLbl.setText(String.format("%.3f ms", avg));

            BenchmarkResult slowest = lastCombinedResults.stream().max((a,b)->Double.compare(a.durationMs(), b.durationMs())).orElse(null);
            if (slowest != null) slowestTestLbl.setText(String.format("%s (%.3f ms)", slowest.testName(), slowest.durationMs()));

            BenchmarkResult fastest = lastCombinedResults.stream().min((a,b)->Double.compare(a.durationMs(), b.durationMs())).orElse(null);
            if (fastest != null) fastestTestLbl.setText(String.format("%s (%.3f ms)", fastest.testName(), fastest.durationMs()));

            exportBtn.setDisable(false);
            progressIndicator.setVisible(false);
            runColdWarmBtn.setDisable(false);
            runBenchmarkBtn.setDisable(false);
            statusLbl.setText("Cold+Warm benchmarks completed successfully!");
        });

        task.setOnFailed(e -> {
            runColdWarmBtn.setDisable(false);
            runBenchmarkBtn.setDisable(false);
            progressIndicator.setVisible(false);
            statusLbl.setText("Error running Cold+Warm benchmarks");
            showError("Benchmark Error", "Failed to run Cold+Warm benchmarks: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    @FXML
    private void handleExport() {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String filename = "performance_report_" + timestamp + ".txt";
            File file = new File(filename);

            try (FileWriter writer = new FileWriter(file)) {
                if (lastCombinedResults != null) {
                    // Build a simple report for combined cold+warm results
                    writer.write("PERFORMANCE BENCHMARK REPORT (Cold + Warm)\n\n");
                    // If we have cold+warm pairs (cold entries followed by warm entries), print deltas
                    boolean hasPairs = lastCombinedResults.size() % 2 == 0;
                    if (hasPairs) {
                        int half = lastCombinedResults.size() / 2;
                        writer.write(String.format("%-50s %12s %12s %12s %12s\n", "Test", "Cold (ms)", "Warm (ms)", "Delta (ms)", "% change"));
                        writer.write("".repeat(80) + "\n");
                        for (int i = 0; i < half; i++) {
                            BenchmarkResult cold = lastCombinedResults.get(i);
                            BenchmarkResult warm = lastCombinedResults.get(i + half);
                            String baseName = cold.testName();
                            // strip suffixes if present
                            baseName = baseName.replaceAll(" \\([^)]*\\)$", "");
                            double coldMs = cold.durationMs();
                            double warmMs = warm.durationMs();
                            double delta = warmMs - coldMs;
                            String pct = coldMs == 0.0 ? "N/A" : String.format("%.2f%%", (delta / coldMs) * 100.0);
                            writer.write(String.format("%-50s %12.3f %12.3f %12.3f %12s\n", baseName, coldMs, warmMs, delta, pct));
                        }
                    } else {
                        for (BenchmarkResult r : lastCombinedResults) {
                            writer.write(String.format("%s : %.3f ms\n", r.testName(), r.durationMs()));
                        }
                    }
                } else {
                    writer.write(vm.exportReport());
                }
            }

            showInfo("Export Successful", "Report saved to: " + filename);
            statusLbl.setText("Report exported: " + filename);
        } catch (Exception e) {
            showError("Export Failed", "Could not export report: " + e.getMessage());
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleBack(javafx.event.ActionEvent event) {
        com.smartblog.ui.navigation.NavigationService.navigateBack();
    }
}