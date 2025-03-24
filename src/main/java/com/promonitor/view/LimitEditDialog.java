package com.promonitor.view;

import com.promonitor.controller.MainController;
import com.promonitor.model.Application;
import com.promonitor.model.Limit;
import com.promonitor.model.enums.LimitType;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.time.Duration;
import java.util.Objects;

public class LimitEditDialog extends Dialog<Limit> {
    private final Application target;

    private final ComboBox<LimitType> limitTypeCombo;
    private final TextField hoursField;
    private final TextField minutesField;

    public LimitEditDialog(LimitsView.LimitInfo limitInfo, MainController controller) {
        this.target = (Application) limitInfo.getTarget();
        Limit originalLimit = controller.getLimitManager().getLimit(target);

        setTitle("Chỉnh sửa giới hạn");

        // Thiết lập giao diện dialog
        DialogPane dialogPane = getDialogPane();
        dialogPane.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/limit-dialog.css")).toExternalForm());
        dialogPane.getStyleClass().add("limit-edit-dialog");

        // Thiết lập kích thước
        Stage stage = (Stage) dialogPane.getScene().getWindow();
        stage.setMinWidth(450);
        stage.setMinHeight(380);

        // Main layout
        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(25));
        mainLayout.getStyleClass().add("limit-form");

        // Header
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        VBox headerTextBox = new VBox(5);

        Text headerTitle = new Text("Chỉnh sửa giới hạn");
        headerTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        headerTitle.setFill(Color.web("#2c3e50"));
        headerTitle.getStyleClass().add("dialog-header");

        Text targetText = new Text(limitInfo.getTargetName());
        targetText.setFont(Font.font("System", FontWeight.NORMAL, 14));
        targetText.setFill(Color.web("#7f8c8d"));

        headerTextBox.getChildren().addAll(headerTitle, targetText);
        headerBox.getChildren().add(headerTextBox);

        // Thêm header vào layout chính
        mainLayout.getChildren().add(headerBox);

        // Separator có style
        Separator separator = new Separator();
        separator.getStyleClass().add("modern-separator");
        mainLayout.getChildren().add(separator);

        // Form container
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(20);
        grid.getStyleClass().add("form-grid");

        // Set column constraints
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setPercentWidth(30);

        ColumnConstraints column2 = new ColumnConstraints();
        column2.setPercentWidth(70);
        column2.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(column1, column2);

        // Limit Type (Loại giới hạn)
        Label limitTypeLabel = new Label("Loại giới hạn:");
        limitTypeLabel.getStyleClass().add("form-label");

        limitTypeCombo = new ComboBox<>();
        limitTypeCombo.setItems(FXCollections.observableArrayList(LimitType.values()));
        limitTypeCombo.setValue(originalLimit.getType());
        limitTypeCombo.getStyleClass().add("combo-box-styled");
        limitTypeCombo.setMaxWidth(Double.MAX_VALUE);

        limitTypeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(LimitType limitType) {
                if (limitType == null) return "";
                return switch (limitType) {
                    case DAILY -> "Hàng ngày";
                    case WEEKLY -> "Hàng tuần";
                    default -> limitType.toString();
                };
            }

            @Override
            public LimitType fromString(String string) {
                return null; // Not used for ComboBox
            }
        });

        // Thêm tooltip
        limitTypeCombo.setTooltip(new Tooltip("Chọn khoảng thời gian áp dụng giới hạn"));

        // Time fields
        // Hours
        Label hoursLabel = new Label("Giới hạn (giờ):");
        hoursLabel.getStyleClass().add("form-label");

        hoursField = new TextField();
        hoursField.getStyleClass().add("time-field");
        hoursField.setMaxWidth(Double.MAX_VALUE);

        // Minutes
        Label minutesLabel = new Label("Giới hạn (phút):");
        minutesLabel.getStyleClass().add("form-label");

        minutesField = new TextField();
        minutesField.getStyleClass().add("time-field");
        minutesField.setMaxWidth(Double.MAX_VALUE);

        // Set initial values
        Duration duration = originalLimit.getValue();
        long hours = duration.toHours();
        int minutes = duration.toMinutesPart();
        hoursField.setText(String.valueOf(hours));
        minutesField.setText(String.valueOf(minutes));

        // Input validation with improved UI feedback
        hoursField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                hoursField.setText(newValue.replaceAll("\\D", ""));
                hoursField.setStyle("-fx-border-color: #e74c3c;");
            } else {
                hoursField.setStyle("");
            }
        });

        minutesField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                minutesField.setText(newValue.replaceAll("\\D", ""));
                minutesField.setStyle("-fx-border-color: #e74c3c;");
            } else {
                minutesField.setStyle("");
                if (!newValue.isEmpty()) {
                    int val = Integer.parseInt(newValue);
                    if (val > 59) {
                        minutesField.setText("59");
                        minutesField.setStyle("-fx-border-color: #f39c12;");
                    }
                }
            }
        });

        // Add form elements to grid
        int row = 0;
        grid.add(limitTypeLabel, 0, row);
        grid.add(limitTypeCombo, 1, row++);

        grid.add(hoursLabel, 0, row);
        grid.add(hoursField, 1, row++);

        grid.add(minutesLabel, 0, row);
        grid.add(minutesField, 1, row++);

        mainLayout.getChildren().add(grid);

        HBox tipBox = new HBox(10);
        tipBox.setAlignment(Pos.CENTER_LEFT);
        tipBox.setPadding(new Insets(5, 0, 0, 0));

        Label tipLabel = new Label("Thiết lập giới hạn 0 giờ 0 phút sẽ vô hiệu hóa giới hạn.");
        tipLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic;");

        tipBox.getChildren().add(tipLabel);
        mainLayout.getChildren().add(tipBox);

        // Buttons
        ButtonType updateButtonType = new ButtonType("Cập nhật", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        // Style buttons
        Button updateButton = (Button) dialogPane.lookupButton(updateButtonType);
        updateButton.getStyleClass().add("update-button");

        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        cancelButton.setText("Hủy");
        cancelButton.getStyleClass().add("cancel-button");

        // Set main content
        dialogPane.setContent(mainLayout);

        setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                LimitType selectedType = limitTypeCombo.getValue();
                long newHours;
                int newMinutes;
                try {
                    newHours = Long.parseLong(hoursField.getText());
                } catch (NumberFormatException e) {
                    newHours = 0;
                }
                try {
                    newMinutes = Integer.parseInt(minutesField.getText());
                } catch (NumberFormatException e) {
                    newMinutes = 0;
                }
                Duration newDuration = Duration.ofHours(newHours).plusMinutes(newMinutes);
                controller.setLimit(target, new Limit(selectedType, newDuration));
            }
            return null;
        });
    }
}