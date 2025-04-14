package com.promonitor.view;

import com.promonitor.util.AlertHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

public class ApplicationDetailsView extends VBox {

    private final String appName;
    private final double minutes;
    private final double percentage;

    public ApplicationDetailsView(String appName, double minutes, double totalMinutes) {
        this.appName = appName;
        this.minutes = minutes;
        this.percentage = totalMinutes > 0 ? (minutes / totalMinutes) * 100 : 0;

        setSpacing(10);
        setPadding(new Insets(5));
        getStyleClass().add("app-details-view");
        setPrefHeight(380);
        setMaxHeight(380);

        createContent();
    }

    private void createContent() {
        HBox appInfoBox = new HBox(10);
        appInfoBox.setAlignment(Pos.CENTER_LEFT);
        appInfoBox.setPadding(new Insets(20, 0, 5, 0));

        Circle appIconBg = new Circle(15);
        appIconBg.setFill(Color.valueOf("#4a6bff"));

        FontAwesomeIconView appIcon = new FontAwesomeIconView(FontAwesomeIcon.DESKTOP);
        appIcon.setGlyphSize(15);
        appIcon.setFill(Color.WHITE);

        StackPane iconStack = new StackPane(appIconBg, appIcon);

        Label appNameLabel = new Label(appName);
        appNameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        appInfoBox.getChildren().addAll(iconStack, appNameLabel);

        VBox statsBox = new VBox(2);
        statsBox.getStyleClass().add("stats-container");
        statsBox.setPadding(new Insets(8));

        addStatItem(statsBox, "Thời gian sử dụng  ", formatMinutes(minutes));
        addStatItem(statsBox, "Phần trăm sử dụng  ", String.format("%.1f%%", percentage));

        ProgressBar usageBar = new ProgressBar(percentage / 100);
        Color progressColor;
        if (percentage > 70) {
            usageBar.getStyleClass().add("progress-bar-red");
            progressColor = Color.valueOf("#e74c3c");
        } else if (percentage > 40) {
            usageBar.getStyleClass().add("progress-bar-orange");
            progressColor = Color.valueOf("#f39c12");
        } else if (percentage > 20) {
            usageBar.getStyleClass().add("progress-bar-blue");
            progressColor = Color.valueOf("#3498db");
        } else {
            usageBar.getStyleClass().add("progress-bar-green");
            progressColor = Color.valueOf("#2ecc71");
        }
        usageBar.setPrefHeight(6);
        usageBar.setPrefWidth(Double.MAX_VALUE);
        VBox.setMargin(usageBar, new Insets(10, 0, 5, 0));

        VBox usageInsightBox = createUsageInsightBox(progressColor);
        VBox.setMargin(usageInsightBox, new Insets(5, 0, 10, 0));

        Button setLimitBtn = new Button("Đặt giới hạn sử dụng");
        setLimitBtn.getStyleClass().add("primary-button");
        setLimitBtn.setPrefWidth(Double.MAX_VALUE);
        setLimitBtn.setOnAction(e -> AlertHelper.showToast(this, "Tính năng này sẽ được cập nhật trong phiên bản tới", AlertHelper.ToastType.ERROR));

        FontAwesomeIconView limitIcon = new FontAwesomeIconView(FontAwesomeIcon.CLOCK_ALT);
        limitIcon.setGlyphSize(12);
        setLimitBtn.setGraphic(limitIcon);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("transparent-scroll-pane");

        VBox detailsContent = new VBox(8);
        detailsContent.getChildren().addAll(appInfoBox, statsBox, usageBar, usageInsightBox, setLimitBtn);

        scrollPane.setContent(detailsContent);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(scrollPane);
    }

    private VBox createUsageInsightBox(Color progressColor) {
        VBox insightBox = new VBox(5);
        insightBox.setPadding(new Insets(5));
        insightBox.getStyleClass().add("insight-box");

        // Insight heading with icon
        HBox headingBox = new HBox(8);
        headingBox.setAlignment(Pos.CENTER_LEFT);

        FontAwesomeIconView insightIcon = new FontAwesomeIconView(FontAwesomeIcon.INFO_CIRCLE);
        insightIcon.setGlyphSize(14);
        insightIcon.setFill(progressColor);

        Label insightLabel = new Label("Phân tích sử dụng");
        insightLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        insightLabel.setTextFill(progressColor);

        headingBox.getChildren().addAll(insightIcon, insightLabel);

        Label descriptionLabel = new Label(getUsageDescription());
        descriptionLabel.setWrapText(true);
        descriptionLabel.setTextAlignment(TextAlignment.LEFT);
        descriptionLabel.getStyleClass().add("usage-description");


        insightBox.getChildren().addAll(headingBox, descriptionLabel);
        return insightBox;
    }

    private void addStatItem(VBox container, String label, String value) {
        HBox statItem = new HBox();
        statItem.setAlignment(Pos.CENTER_LEFT);

        Label statLabel = new Label(label);
        statLabel.getStyleClass().add("stat-label");
        statLabel.setFont(Font.font("System", 12));
        HBox.setHgrow(statLabel, Priority.ALWAYS);

        Label statValue = new Label(value);
        statValue.getStyleClass().add("stat-value");
        statValue.setFont(Font.font("System", FontWeight.BOLD, 12));

        statItem.getChildren().addAll(statLabel, statValue);
        container.getChildren().add(statItem);
    }

    private String formatMinutes(double minutes) {
        int hours = (int) (minutes / 60);
        int mins = (int) (minutes % 60);

        if (hours > 0) {
            return String.format("%d giờ %d phút", hours, mins);
        } else {
            return String.format("%d phút", mins);
        }
    }

    private String getUsageDescription() {
        int hours = (int) (minutes / 60);

        if (percentage > 70) {
            if (hours >= 4) {
                return "Bạn đã sử dụng ứng dụng này quá nhiều (" + formatMinutes(minutes) +
                        "), chiếm " + String.format("%.1f", percentage) +
                        "% tổng thời gian hoạt động." +
                        "Nên cân nhắc đặt giới hạn để đảm bảo cân bằng thời gian.";
            } else {
                return "Bạn đã sử dụng ứng dụng này nhiều (" + formatMinutes(minutes) +
                        "), chiếm tỷ lệ cao (" + String.format("%.1f", percentage) +
                        "%) trong các hoạt động của bạn.";
            }
        } else if (percentage > 40) {
            if (hours >= 2) {
                return "Thời gian sử dụng ứng dụng khá nhiều (" + formatMinutes(minutes) +
                        "), chiếm " + String.format("%.1f", percentage) +
                        "% tổng thời gian." +
                        "Có thể cân nhắc phân bổ thời gian cho các hoạt động khác.";
            } else {
                return "Mức độ sử dụng đáng chú ý với " + formatMinutes(minutes) +
                        ", chiếm " + String.format("%.1f", percentage) +
                        "% hoạt động.";
            }
        } else if (percentage > 20) {
            return "Mức độ sử dụng trung bình với " + formatMinutes(minutes) +
                    ", chiếm " + String.format("%.1f", percentage) +
                    "% hoạt động.";
        } else {
            return "Mức độ sử dụng thấp, chỉ " + formatMinutes(minutes) +
                    " (" + String.format("%.1f", percentage) +
                    "% tổng thời gian).";
        }
    }
}