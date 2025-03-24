package com.promonitor.view;

import com.promonitor.controller.MainController;
import com.promonitor.model.Report;
import com.promonitor.model.enums.ReportType;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ReportsView {
    private final MainController controller;
    private BorderPane content;
    private Report currentReport;
    private DoughnutChart usageChart;
    private TextArea reportTextArea;

    public ReportsView(MainController controller) {
        this.controller = controller;
        createContent();
    }


    private void createContent() {
        content = new BorderPane();
        content.setPadding(new Insets(10));
        content.getStyleClass().add("reports-view");

        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setSpacing(10);

        GridPane headerGrid = new GridPane();
        headerGrid.setHgap(10);
        headerGrid.setVgap(2);
        headerGrid.setAlignment(Pos.CENTER_LEFT);

        FontAwesomeIconView reportIcon = new FontAwesomeIconView(FontAwesomeIcon.BAR_CHART);
        reportIcon.setGlyphSize(30);
        reportIcon.setFill(Color.valueOf("#4a6bff"));

        StackPane iconContainer = new StackPane(reportIcon);
        iconContainer.setAlignment(Pos.CENTER);
        iconContainer.setMinHeight(40);
        headerGrid.add(iconContainer, 0, 0, 1, 2);

        Label titleLabel = new Label("Báo cáo thời gian sử dụng");
        titleLabel.getStyleClass().add("view-title");
        headerGrid.add(titleLabel, 1, 0);

        Label descriptionLabel = new Label("Xem thống kê và phân tích thời gian sử dụng của các ứng dụng");
        descriptionLabel.getStyleClass().add("description-text");
        headerGrid.add(descriptionLabel, 1, 1);

        content.setTop(headerGrid);

        VBox mainBox = new VBox(10);
        mainBox.setPadding(new Insets(10, 0, 0, 0));

        HBox reportOptionsBox = createReportOptionsBox();

        SplitPane reportDisplay = createReportDisplayPane();
        VBox.setVgrow(reportDisplay, Priority.ALWAYS);

        mainBox.getChildren().addAll(reportOptionsBox, reportDisplay);
        content.setCenter(mainBox);
    }

    private HBox createReportOptionsBox() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(8));
        box.getStyleClass().add("report-box");
        box.setMaxHeight(80);

        FontAwesomeIconView typeIcon = new FontAwesomeIconView(FontAwesomeIcon.FILE_TEXT_ALT);
        typeIcon.setGlyphSize(14);
        typeIcon.setFill(Color.valueOf("#5a6978"));

        Label typeLabel = new Label("Loại:");
        ComboBox<ReportType> reportTypeCombo = new ComboBox<>(
                FXCollections.observableArrayList(ReportType.values())
        );
        reportTypeCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(ReportType type) {
                return type != null ? type.getDisplayName() : "";
            }

            @Override
            public ReportType fromString(String string) {
                return null;
            }
        });
        reportTypeCombo.getSelectionModel().select(ReportType.DAILY);
        reportTypeCombo.setPrefWidth(150); // Reduced width

        HBox typeBox = new HBox(5); // Reduced spacing
        typeBox.setAlignment(Pos.CENTER_LEFT);
        typeBox.getChildren().addAll(typeIcon, typeLabel, reportTypeCombo);

        // Date range with more compact layout
        FontAwesomeIconView calendarIcon = new FontAwesomeIconView(FontAwesomeIcon.CALENDAR);
        calendarIcon.setGlyphSize(14); // Smaller icon
        calendarIcon.setFill(Color.valueOf("#5a6978"));

        Label startDateLabel = new Label("Từ:");
        DatePicker startDatePicker = new DatePicker(LocalDate.now().minusDays(7));
        startDatePicker.setPrefWidth(120); // Reduced width

        Label endDateLabel = new Label("Đến:");
        DatePicker endDatePicker = new DatePicker(LocalDate.now());
        endDatePicker.setPrefWidth(120); // Reduced width

        HBox dateBox = new HBox(5, calendarIcon, startDateLabel, startDatePicker, endDateLabel, endDatePicker);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        dateBox.setVisible(false);

        reportTypeCombo.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> dateBox.setVisible(newVal == ReportType.CUSTOM));

        // Generate button with icon only to save space
        FontAwesomeIconView generateIcon = new FontAwesomeIconView(FontAwesomeIcon.REFRESH);
        generateIcon.setGlyphSize(14);
        generateIcon.setFill(Color.WHITE);

        Button generateBtn = new Button();
        generateBtn.getStyleClass().add("primary-button");
        generateBtn.setGraphic(generateIcon);
        generateBtn.setTooltip(new Tooltip("Tạo báo cáo"));
        generateBtn.getStyleClass().add("button");
        generateBtn.setPrefSize(36, 36); // Square button for a more compact look
        generateBtn.setOnAction(e -> {
            ReportType selectedType = reportTypeCombo.getSelectionModel().getSelectedItem();

            if (selectedType == ReportType.CUSTOM) {
                currentReport = controller.createCustomReport(
                        startDatePicker.getValue(),
                        endDatePicker.getValue()
                );
            } else {
                currentReport = controller.createReport(selectedType);
            }

            displayReport();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        box.getChildren().addAll(typeBox, dateBox, spacer, generateBtn);

        return box;
    }

    private SplitPane createReportDisplayPane() {
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.HORIZONTAL); // Ensure horizontal split

        VBox chartContainer = new VBox(15);
        chartContainer.setPadding(new Insets(10));
        chartContainer.getStyleClass().add("left-pane");
        chartContainer.setMaxHeight(400);
        VBox.setVgrow(chartContainer, Priority.ALWAYS);

        HBox chartHeaderBox = new HBox(5);
        chartHeaderBox.setAlignment(Pos.CENTER_LEFT);
        chartHeaderBox.setMaxHeight(30);

        FontAwesomeIconView chartIcon = new FontAwesomeIconView(FontAwesomeIcon.PIE_CHART);
        chartIcon.setGlyphSize(14);
        chartIcon.setFill(Color.valueOf("#4a6bff"));

        Label chartLabel = new Label("Biểu đồ sử dụng");
        chartLabel.getStyleClass().add("section-header");

        chartHeaderBox.getChildren().addAll(chartIcon, chartLabel);

        usageChart = new DoughnutChart();
        VBox.setVgrow(usageChart, Priority.ALWAYS);

        chartContainer.getChildren().addAll(chartHeaderBox, usageChart);

        VBox detailsContainer = new VBox(15);
        detailsContainer.setPadding(new Insets(10));
        detailsContainer.getStyleClass().add("right-pane");
        detailsContainer.setMaxHeight(400);
        VBox.setVgrow(detailsContainer, Priority.ALWAYS);

        HBox detailsHeaderBox = new HBox(5);
        detailsHeaderBox.setAlignment(Pos.CENTER_LEFT);
        detailsHeaderBox.setMaxHeight(30);

        FontAwesomeIconView detailsIcon = new FontAwesomeIconView(FontAwesomeIcon.LIST_ALT);
        detailsIcon.setGlyphSize(14);
        detailsIcon.setFill(Color.valueOf("#4a6bff"));

        Label detailsLabel = new Label("Chi tiết báo cáo");
        detailsLabel.getStyleClass().add("section-header");

        detailsHeaderBox.getChildren().addAll(detailsIcon, detailsLabel);

        BorderPane textContainer = new BorderPane();
        textContainer.getStyleClass().add("text-container");
        VBox.setVgrow(textContainer, Priority.ALWAYS);

        reportTextArea = new TextArea();
        reportTextArea.setEditable(false);
        reportTextArea.setWrapText(true);
        textContainer.setCenter(reportTextArea);

        HBox exportBox = new HBox(10);
        exportBox.setAlignment(Pos.CENTER_RIGHT);
        exportBox.setPadding(new Insets(8, 0, 0, 0));
        exportBox.setMinHeight(40);
        exportBox.setMaxHeight(40);

        Button exportPDFBtn = createExportButton("PDF", FontAwesomeIcon.FILE_PDF_ALT, Color.valueOf("#e74c3c"));
        exportPDFBtn.setOnAction(e -> exportReport("PDF"));

        Button exportCSVBtn = createExportButton("CSV", FontAwesomeIcon.FILE_EXCEL_ALT, Color.valueOf("#27ae60"));
        exportCSVBtn.setOnAction(e -> exportReport("CSV"));

        exportBox.getChildren().addAll(exportPDFBtn, exportCSVBtn);

        detailsContainer.getChildren().addAll(detailsHeaderBox, textContainer, exportBox);

        splitPane.getItems().addAll(chartContainer, detailsContainer);
        splitPane.setDividerPositions(0.4);

        return splitPane;
    }

    private Button createExportButton(String text, FontAwesomeIcon icon, Color color) {
        FontAwesomeIconView iconView = new FontAwesomeIconView(icon);
        iconView.setGlyphSize(14);
        iconView.setFill(color);

        Button button = new Button(text);
        button.setGraphic(iconView);
        button.getStyleClass().add("icon-text-button");
        button.setPrefHeight(32);

        return button;
    }

    private void displayReport() {
        if (currentReport == null) return;

        if (!currentReport.generateReport()) {
            showError("Không thể tạo báo cáo", "Không có đủ dữ liệu để tạo báo cáo.");
            return;
        }

        usageChart.setData(currentReport.generateChartData());

        StringBuilder reportText = new StringBuilder();
        Map<String, Object> data = currentReport.getReportData();

        usageChart.setCenterText(data.get("totalUsageTime").toString());

        reportText.append("BÁO CÁO SỬ DỤNG PROMONITOR\n");
        reportText.append("==========================\n\n");

        reportText.append("Loại báo cáo: ").append(currentReport.getReportType().getDisplayName()).append("\n");
        reportText.append("Khoảng thời gian: ").append(currentReport.getStartDate()).append(" đến ")
                .append(currentReport.getEndDate()).append("\n");
        reportText.append("Người dùng: ").append(data.get("username")).append("\n\n");

        reportText.append("TỔNG THỜI GIAN SỬ DỤNG: ").append(data.get("totalUsageTime")).append("\n\n");

        reportText.append("CHI TIẾT SỬ DỤNG ỨNG DỤNG (Tổng):\n");
        List<Map<String, Object>> appUsage = (List<Map<String, Object>>) data.get("appUsageData");

        for (Map<String, Object> app : appUsage) {
            reportText.append("  • ").append(app.get("appName")).append(": ")
                    .append(app.get("usageTime")).append("\n");
        }

        reportText.append("\nGỢI Ý TỐI ƯU HÓA THỜI GIAN:\n");
        List<String> suggestions = currentReport.generateSuggestions();
        for (String suggestion : suggestions) {
            reportText.append("  • ").append(suggestion).append("\n");
        }

        reportText.append("\nBáo cáo được tạo vào: ").append(data.get("generatedAt"));

        reportTextArea.setText(reportText.toString());
    }

    private void exportReport(String format) {
        if (currentReport == null) {
            showError("Không thể xuất báo cáo", "Vui lòng tạo báo cáo trước khi xuất.");
            return;
        }

        String filePath = currentReport.exportReport(format);

        if (filePath != null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Xuất báo cáo thành công");
            alert.setHeaderText("Đã xuất báo cáo thành công");
            alert.setContentText("Báo cáo đã được lưu tại:\n" + filePath);

            ButtonType openButton = new ButtonType("Mở file");
            alert.getButtonTypes().add(openButton);

            alert.showAndWait().ifPresent(response -> {
                if (response == openButton) {
                    try {
                        java.awt.Desktop.getDesktop().open(new File(filePath));
                    } catch (Exception e) {
                        showError("Không thể mở file", "Không thể mở file báo cáo: " + e.getMessage());
                    }
                }
            });
        } else {
            showError("Xuất báo cáo thất bại", "Đã xảy ra lỗi khi xuất báo cáo.");
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public Node getContent() {
        return content;
    }
}