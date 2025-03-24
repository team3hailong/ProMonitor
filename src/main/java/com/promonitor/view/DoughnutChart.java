package com.promonitor.view;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class DoughnutChart extends StackPane {
    private final PieChart pieChart;
    private final Text centerText;
    private final Circle innerCircle;
    private final StackPane chartCenterPane;

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

    public ObservableList<PieChart.Data> getData() {
        return pieChart.getData();
    }

    public void setData(ObservableList<PieChart.Data> data) {
        pieChart.setData(data);
        javafx.application.Platform.runLater(() -> installTooltips(data));
    }

    private void installTooltips(ObservableList<PieChart.Data> data) {
        for (PieChart.Data d : pieChart.getData()) {
            Tooltip.uninstall(d.getNode(), null);
        }
        //System.out.println("Installing tooltips for " + data.size() + " segments");
        for (PieChart.Data d : data) {
            //System.out.println("  Segment: " + d.getName() + ", Node: " + (d.getNode() == null ? "NULL" : "OK"));
            final Tooltip tooltip = new Tooltip();
            String name = d.getName();
            double value = d.getPieValue();

            // Format the tooltip text
            String tooltipText = name;
            if (!name.contains("(")) {
                tooltipText = name + " (" + formatMinutes(value) + ")";
            }

            tooltip.setText(tooltipText);
            tooltip.setFont(Font.font("System", FontWeight.NORMAL, 14));

            Tooltip.install(d.getNode(), tooltip);

            d.getNode().setOnMouseEntered(event -> {
                d.getNode().setScaleX(1.15);
                d.getNode().setScaleY(1.15);
            });

            d.getNode().setOnMouseExited(event -> {
                d.getNode().setScaleX(1);
                d.getNode().setScaleY(1);
            });
        }
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