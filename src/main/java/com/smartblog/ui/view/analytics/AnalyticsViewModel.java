
package com.smartblog.ui.view.analytics;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class AnalyticsViewModel {

    // Replace these with your analytics service results
    public int totalPosts() { return 0; }
    public int totalComments() { return 0; }

    public ObservableList<javafx.scene.chart.PieChart.Data> postStatusData() {
        return FXCollections.observableArrayList(
                new javafx.scene.chart.PieChart.Data("Published", 0),
                new javafx.scene.chart.PieChart.Data("Draft", 0)
        );
    }

    public ObservableList<javafx.scene.chart.PieChart.Data> topTagsData() {
        return FXCollections.observableArrayList(
                new javafx.scene.chart.PieChart.Data("java", 0),
                new javafx.scene.chart.PieChart.Data("mysql", 0)
        );
    }

    public javafx.scene.chart.XYChart.Series<String, Number> postsPerMonthSeries() {
        var s = new javafx.scene.chart.XYChart.Series<String, Number>();
        s.setName("Posts");
        // s.getData().add(new XYChart.Data<>("2026-01", 12));
        return s;
    }
}
