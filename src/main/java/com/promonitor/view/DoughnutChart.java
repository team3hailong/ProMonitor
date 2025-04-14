package com.promonitor.view;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class DoughnutChart extends StackPane {
    private ReportsView reportsView;

    private final PieChart pieChart;
    private final Text centerText;
    private final Circle innerCircle;
    private final StackPane chartCenterPane;
    private long totalMinutes = 0;

    public DoughnutChart() {
        pieChart = new PieChart();
        pieChart.setLabelsVisible(false);
        pieChart.setLegendVisible(false);

        chartCenterPane = new StackPane();
        innerCircle = new Circle();
        innerCircle.setFill(Color.WHITE);

        centerText = new Text("");
        centerText.setFont(Font.font("System", FontWeight.BOLD, 16));

        chartCenterPane.getChildren().addAll(innerCircle, centerText);

        getChildren().addAll(pieChart, chartCenterPane);

        chartCenterPane.setPickOnBounds(false);
        innerCircle.setMouseTransparent(true);
        centerText.setMouseTransparent(true);
        setPadding(new Insets(10));

        pieChart.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> updateChartCenterPosition());

        widthProperty().addListener((obs, oldVal, newVal) -> updateInnerCircle());
        heightProperty().addListener((obs, oldVal, newVal) -> updateInnerCircle());
    }

    public void setReportsView(ReportsView reportsView) {
        this.reportsView = reportsView;
    }

    private void updateChartCenterPosition() {
        chartCenterPane.setTranslateX(pieChart.getLayoutBounds().getMinX() + pieChart.getLayoutBounds().getWidth()/2 - chartCenterPane.getWidth()/2);
        chartCenterPane.setTranslateY(pieChart.getLayoutBounds().getMinY() + pieChart.getLayoutBounds().getHeight()/2 - chartCenterPane.getHeight()/2);
    }

    private void updateInnerCircle() {
        double chartSize = Math.min(pieChart.getWidth(), pieChart.getHeight());
        if (chartSize <= 0) {
            chartSize = Math.min(getWidth(), getHeight());
        }

        double innerRadiusRatio = 0.5;
        double radius = chartSize * innerRadiusRatio / 2;
        innerCircle.setRadius(radius);

        updateChartCenterPosition();
    }

    public void setCenterText(String text) {
        centerText.setText(text);
    }

    public void setData(ObservableList<PieChart.Data> data) {
        pieChart.setData(data);

        totalMinutes = 0;
        for (PieChart.Data d : data) {
            totalMinutes += (long) d.getPieValue();
        }

        javafx.application.Platform.runLater(() -> installTooltips(data));
    }

    private void installTooltips(ObservableList<PieChart.Data> data) {
        for (PieChart.Data d : pieChart.getData()) {
            Tooltip.uninstall(d.getNode(), null);
        }
        for (PieChart.Data d : data) {
            Node node = d.getNode();
            final Tooltip tooltip = new Tooltip();
            String name = d.getName();
            double value = d.getPieValue();

            String tooltipText = name;
            if (!name.contains("(")) {
                tooltipText = name + " (" + formatMinutes(value) + ")";
            }

            tooltip.setText(tooltipText);
            tooltip.setFont(Font.font("System", FontWeight.NORMAL, 14));

            Tooltip.install(node, tooltip);

            node.setOnMouseClicked(event -> {
                if (reportsView != null) {
                    showApplicationDetailsView(d);
                }
            });

            node.setOnMouseEntered(event -> {
                node.setScaleX(1.15);
                node.setScaleY(1.15);
            });

            node.setOnMouseExited(event -> {
                node.setScaleX(1);
                node.setScaleY(1);
            });
        }
    }


    private void showApplicationDetailsView(PieChart.Data data) {
        String appName = data.getName();
        double minutes = data.getPieValue();

        ApplicationDetailsView detailsView = new ApplicationDetailsView(
                appName,
                minutes,
                totalMinutes
        );

        reportsView.showApplicationDetails(detailsView);
    }

    private String formatMinutes(double minutes) {
        if (minutes < 1) {
            return String.format("%.1f phút", minutes);
        } else {
            int hrs = (int) (minutes / 60);
            int mins = (int) (minutes % 60);

            if (hrs > 0) {
                return String.format("%d giờ %d phút", hrs, mins);
            } else {
                return String.format("%d phút", mins);
            }
        }
    }

    @Override
    public void layoutChildren() {
        super.layoutChildren();
        updateChartCenterPosition();
    }

}