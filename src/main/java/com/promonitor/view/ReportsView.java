package com.promonitor.view;

import com.promonitor.controller.MainController;
import com.promonitor.model.Report;
import com.promonitor.model.enums.ReportType;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ReportsView {
    private final MainController controller;
    private BorderPane content;
    private Report currentReport;
    private PieChart usageChart;
    private TextArea reportTextArea;

    public ReportsView(MainController controller) {
        this.controller = controller;
        createContent();
    }

    /**
     * Tạo nội dung cho view
     */
    private void createContent() {
        content = new BorderPane();
        content.setPadding(new Insets(15));
        content.getStyleClass().add("reports-view");

        // Tiêu đề
        Label titleLabel = new Label("Báo cáo thời gian sử dụng");
        titleLabel.getStyleClass().add("view-title");
        content.setTop(titleLabel);

        // Layout chính
        VBox mainBox = new VBox(20);
        mainBox.setPadding(new Insets(10, 0, 0, 0));

        // Khu vực tạo báo cáo
        HBox reportOptionsBox = createReportOptionsBox();

        // Khu vực hiển thị báo cáo
        SplitPane reportDisplay = createReportDisplayPane();
        VBox.setVgrow(reportDisplay, Priority.ALWAYS);

        mainBox.getChildren().addAll(reportOptionsBox, reportDisplay);
        content.setCenter(mainBox);
    }

    /**
     * Tạo phần tùy chọn báo cáo
     */
    private HBox createReportOptionsBox() {
        HBox box = new HBox(15);
        box.setAlignment(Pos.CENTER_LEFT);

        // Loại báo cáo
        Label typeLabel = new Label("Loại báo cáo:");
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
                return null; // Not used
            }
        });
        reportTypeCombo.getSelectionModel().select(ReportType.DAILY);

        // Ngày tùy chọn (hiển thị khi chọn báo cáo tùy chỉnh)
        Label startDateLabel = new Label("Từ ngày:");
        DatePicker startDatePicker = new DatePicker(LocalDate.now().minusDays(7));

        Label endDateLabel = new Label("Đến ngày:");
        DatePicker endDatePicker = new DatePicker(LocalDate.now());

        HBox dateBox = new HBox(10, startDateLabel, startDatePicker, endDateLabel, endDatePicker);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        dateBox.setVisible(false);

        // Logic hiển thị date picker khi chọn báo cáo tùy chỉnh
        reportTypeCombo.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> dateBox.setVisible(newVal == ReportType.CUSTOM));

        Button generateBtn = new Button("Tạo báo cáo");
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

        box.getChildren().addAll(typeLabel, reportTypeCombo, dateBox, generateBtn);

        return box;
    }

    /**
     * Tạo phần hiển thị báo cáo
     */
    private SplitPane createReportDisplayPane() {
        SplitPane splitPane = new SplitPane();

        // Bên trái - Biểu đồ
        VBox chartBox = new VBox(10);
        chartBox.setPadding(new Insets(10));

        Label chartLabel = new Label("Biểu đồ sử dụng ứng dụng");
        chartLabel.getStyleClass().add("section-header");

        usageChart = new PieChart();
        usageChart.setTitle("Thời gian sử dụng ứng dụng");
        usageChart.setLabelsVisible(true);
        VBox.setVgrow(usageChart, Priority.ALWAYS);

        chartBox.getChildren().addAll(chartLabel, usageChart);

        // Bên phải - Chi tiết và gợi ý
        VBox detailsBox = new VBox(10);
        detailsBox.setPadding(new Insets(10));

        Label detailsLabel = new Label("Chi tiết báo cáo");
        detailsLabel.getStyleClass().add("section-header");

        reportTextArea = new TextArea();
        reportTextArea.setEditable(false);
        reportTextArea.setWrapText(true);
        VBox.setVgrow(reportTextArea, Priority.ALWAYS);

        // Nút xuất báo cáo
        HBox exportBox = new HBox(10);
        exportBox.setAlignment(Pos.CENTER_RIGHT);

        Button exportPDFBtn = new Button("Xuất PDF");
        exportPDFBtn.setOnAction(e -> exportReport("PDF"));

        Button exportCSVBtn = new Button("Xuất CSV");
        exportCSVBtn.setOnAction(e -> exportReport("CSV"));

        exportBox.getChildren().addAll(exportPDFBtn, exportCSVBtn);

        detailsBox.getChildren().addAll(detailsLabel, reportTextArea, exportBox);

        // Thêm vào split pane
        splitPane.getItems().addAll(chartBox, detailsBox);
        splitPane.setDividerPositions(0.5);

        return splitPane;
    }

    /**
     * Hiển thị báo cáo hiện tại
     */
    private void displayReport() {
        if (currentReport == null) return;

        if (!currentReport.generateReport()) {
            showError("Không thể tạo báo cáo", "Không có đủ dữ liệu để tạo báo cáo.");
            return;
        }

        usageChart.setData(currentReport.generateChartData());

        StringBuilder reportText = new StringBuilder();
        Map<String, Object> data = currentReport.getReportData();

        reportText.append("BÁO CÁO SỬ DỤNG PROMONITOR\n");
        reportText.append("==========================\n\n");

        reportText.append("Loại báo cáo: ").append(currentReport.getReportType().getDisplayName()).append("\n");
        reportText.append("Khoảng thời gian: ").append(currentReport.getStartDate()).append(" đến ")
                .append(currentReport.getEndDate()).append("\n");
        reportText.append("Người dùng: ").append(data.get("username")).append("\n\n");

        reportText.append("TỔNG THỜI GIAN SỬ DỤNG: ").append(data.get("totalUsageTime")).append("\n\n");

        reportText.append("CHI TIẾT SỬ DỤNG ỨNG DỤNG:\n");
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

    /**
     * Xuất báo cáo
     */
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

            // Thêm nút để mở file
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

    /**
     * Hiển thị thông báo lỗi
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Lấy nội dung đã tạo
     */
    public Node getContent() {
        return content;
    }
}