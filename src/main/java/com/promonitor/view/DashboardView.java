package com.promonitor.view;

import com.promonitor.controller.MainController;
import com.promonitor.model.TimeTracker;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.util.*;

public class DashboardView {
    private final MainController controller;

    private DoughnutChart usageChart;
    private TableView<TimeTracker> topAppsTable;
    private Label totalTimeLabel;

    public DashboardView(MainController controller) {
        this.controller = controller;
        createContent();
    }

    private void createContent() {
        BorderPane content = new BorderPane();
        content.setPadding(new Insets(15));
        content.getStyleClass().add("dashboard-view");

        HBox overviewBox = createOverviewSection();
        content.setTop(overviewBox);

        HBox centerBox = new HBox(20);
        centerBox.setPadding(new Insets(15, 0, 0, 0));

        usageChart = new DoughnutChart();
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

        VBox totalTimeBox = createInfoBox();
        totalTimeLabel = (Label) totalTimeBox.getChildren().get(1);

        box.getChildren().addAll(totalTimeBox);

        return box;
    }

    private VBox createInfoBox() {
        VBox box = new VBox(5);
        box.getStyleClass().add("info-box");
        box.setPadding(new Insets(10));
        box.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Tổng thời gian sử dụng");
        titleLabel.getStyleClass().add("info-title");

        Label valueLabel = new Label("00:00:00");
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
        Duration totalTime = controller.getTotalComputerUsageTime();
        long totalHours = totalTime.toHours();
        int totalMinutes = totalTime.toMinutesPart();
        int totalSeconds = totalTime.toSecondsPart();
        totalTimeLabel.setText(String.format("%02d:%02d:%02d", totalHours, totalMinutes, totalSeconds));

        List<TimeTracker> topTrackers = controller.getAllTimeTrackers();
        ObservableList<PieChart.Data> currentData = usageChart.getData();

        Set<String> updatedApps = new HashSet<>();
        double otherTime = 0;

        Map<String, PieChart.Data> dataMap = new HashMap<>();
        for (PieChart.Data data : currentData) {
            dataMap.put(data.getName().split(" \\(")[0], data);
        }

        for (int i = 0; i < topTrackers.size(); i++) {
            TimeTracker tracker = topTrackers.get(i);
            String appName = tracker.getApplication().getName();
            double minutes = 0;
            if (i <= 5) {
                minutes = tracker.getTotalTimeInMinutes();
                if (minutes >= 1) {
                    String fullName = appName + " (" + minutes + " phút)";
                    updatedApps.add(appName);
                    if (dataMap.containsKey(appName)) {
                        PieChart.Data existingData = dataMap.get(appName);
                        existingData.setPieValue(minutes);
                        existingData.setName(fullName);
                    } else {
                        currentData.add(new PieChart.Data(fullName, minutes));
                    }
                }
            } else
                otherTime += minutes;
        }
        currentData.removeIf(data -> {
            String appName = data.getName().split(" \\(")[0];
            return !updatedApps.contains(appName);
        });
        if (otherTime > 0) {
            currentData.add(new PieChart.Data("Khác (" + otherTime + " phút)", otherTime));
        }
        topAppsTable.getItems().clear();
        topAppsTable.getItems().addAll(topTrackers);
    }
}