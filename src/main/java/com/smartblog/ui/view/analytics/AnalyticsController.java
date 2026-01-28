
package com.smartblog.ui.view.analytics;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;

public class AnalyticsController {
    @FXML private Label totalPostsLbl, totalCommentsLbl;
    @FXML private PieChart statusChart, topTagsChart;
    @FXML private LineChart<String, Number> postsPerMonth;

    private final AnalyticsViewModel vm = new AnalyticsViewModel();

    @FXML
    public void initialize() {
        totalPostsLbl.setText("Total Posts: " + vm.totalPosts());
        totalCommentsLbl.setText("Total Comments: " + vm.totalComments());

        statusChart.setData(vm.postStatusData());
        topTagsChart.setData(vm.topTagsData());
        postsPerMonth.getData().add(vm.postsPerMonthSeries());
    }
}
