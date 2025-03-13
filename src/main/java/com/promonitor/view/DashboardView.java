package com.promonitor.view;

import com.promonitor.controller.MainController;
import com.promonitor.model.TimeTracker;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.util.List;

public class DashboardView {
    private final MainController controller;
    private BorderPane content;

    private PieChart usageChart;
    private TableView<TimeTracker> topAppsTable;
    private Label totalTimeLabel;

    public DashboardView(MainController controller) {
        this.controller = controller;
        createContent();
    }

    private void createContent() {
        content = new BorderPane();
        content.setPadding(new Insets(15));
        content.getStyleClass().add("dashboard-view");

        HBox overviewBox = createOverviewSection();
        content.setTop(overviewBox);

        HBox centerBox = new HBox(20);
        centerBox.setPadding(new Insets(15, 0, 0, 0));

        usageChart = new PieChart();
        usageChart.setTitle("Thời gian sử dụng ứng dụng");
        usageChart.setLabelsVisible(true);
        usageChart.setLegendVisible(true);
        HBox.setHgrow(usageChart, Priority.ALWAYS);

        VBox tableBox = createTopAppsTable();
        HBox.setHgrow(tableBox, Priority.ALWAYS);

        centerBox.getChildren().addAll(usageChart, tableBox);
        content.setCenter(centerBox);

        updateData();
    }

    private HBox createOverviewSection() {
        HBox box = new HBox(30);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(0, 0, 15, 0));

        VBox totalTimeBox = createInfoBox("Tổng thời gian sử dụng", "00:00:00");
        totalTimeLabel = (Label) totalTimeBox.getChildren().get(1);

        box.getChildren().addAll(totalTimeBox);

        return box;
    }

    private VBox createInfoBox(String title, String value) {
        VBox box = new VBox(5);
        box.getStyleClass().add("info-box");
        box.setPadding(new Insets(10));
        box.setAlignment(Pos.CENTER);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("info-title");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("info-value");

        box.getChildren().addAll(titleLabel, valueLabel);

        return box;
    }

    private VBox createTopAppsTable() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Top 5 ứng dụng sử dụng nhiều nhất");
        titleLabel.getStyleClass().add("section-title");

        topAppsTable = new TableView<>();
        topAppsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<TimeTracker, String> appNameCol = new TableColumn<>("Ứng dụng");
        appNameCol.setCellValueFactory(cellData ->
                cellData.getValue().getApplication().nameProperty());

        TableColumn<TimeTracker, String> usageCol = new TableColumn<>("Thời gian sử dụng");
        usageCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFormattedTotalTime()));

        topAppsTable.getColumns().addAll(appNameCol, usageCol);

        box.getChildren().addAll(titleLabel, topAppsTable);
        VBox.setVgrow(topAppsTable, Priority.ALWAYS);

        return box;
    }

    public void updateData() {
        // Cập nhật tổng thời gian sử dụng
        Duration totalTime = controller.getTotalComputerUsageTime();
        long totalHours = totalTime.toHours();
        int totalMinutes = totalTime.toMinutesPart();
        int totalSeconds = totalTime.toSecondsPart();
        totalTimeLabel.setText(String.format("%02d:%02d:%02d", totalHours, totalMinutes, totalSeconds));

        // Cập nhật biểu đồ
        List<TimeTracker> topTrackers = controller.getTopApplications(5);
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        for (TimeTracker tracker : topTrackers) {
            String appName = tracker.getApplication().getName();
            double minutes = tracker.getTotalTimeInMinutes();

            // Chỉ hiển thị ứng dụng có thời gian sử dụng đáng kể
            if (minutes > 1) {
                pieChartData.add(new PieChart.Data(appName + " (" + minutes + " phút)", minutes));
            }
        }

        usageChart.setData(pieChartData);

        // Cập nhật bảng top ứng dụng
        topAppsTable.getItems().clear();
        topAppsTable.getItems().addAll(topTrackers);
    }

    public Node getContent() {
        return content;
    }
}