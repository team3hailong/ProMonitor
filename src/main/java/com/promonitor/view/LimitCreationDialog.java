package com.promonitor.view;

import com.promonitor.controller.MainController;
import com.promonitor.model.Application;
import com.promonitor.model.ApplicationGroup;
import com.promonitor.model.Limit;
import com.promonitor.model.enums.LimitType;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public class LimitCreationDialog extends Dialog<Limit> {

    private final MainController controller;
    private final ComboBox<Object> targetCombo;
    private final ComboBox<LimitType> limitTypeCombo;
    private final TextField hoursField;
    private final TextField minutesField;
    private Object selectedTarget;

    public LimitCreationDialog(MainController controller, boolean isGroup) {
        this.controller = controller;
        setTitle("Tạo giới hạn mới");
        DialogPane dialogPane = getDialogPane();
        dialogPane.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/limit-dialog.css")).toExternalForm());
        dialogPane.getStyleClass().add("limit-creation-dialog");

        Stage stage = (Stage) dialogPane.getScene().getWindow();
        stage.setMinWidth(450);
        stage.setMinHeight(400);

        Text headerText = new Text("Thiết lập giới hạn thời gian");
        headerText.setFont(Font.font("System", FontWeight.BOLD, 16));
        headerText.getStyleClass().add("dialog-header");

        ButtonType createButtonType = new ButtonType("Tạo", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        Button createButton = (Button) dialogPane.lookupButton(createButtonType);
        createButton.getStyleClass().add("create-button");

        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        cancelButton.setText("Hủy");
        cancelButton.getStyleClass().add("cancel-button");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(25));
        grid.getStyleClass().add("limit-form");

        ColumnConstraints column1 = new ColumnConstraints();
        column1.setPercentWidth(40);

        ColumnConstraints column2 = new ColumnConstraints();
        column2.setPercentWidth(60);
        column2.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(column1, column2);

        Label targetTypeLabel = new Label("Loại đối tượng:");

        ComboBox<String> targetTypeCombo = new ComboBox<>();
        targetTypeCombo.setItems(FXCollections.observableArrayList("Ứng dụng", "Nhóm"));
        targetTypeCombo.getStyleClass().add("combo-box-styled");
        targetTypeCombo.setMaxWidth(Double.MAX_VALUE);
        String groupText = (isGroup ? "Nhóm" : "Ứng dụng");
        targetTypeCombo.setValue(groupText);

        Label targetLabel = new Label("Đối tượng:");

        targetCombo = new ComboBox<>();
        targetCombo.getStyleClass().add("combo-box-styled");
        targetCombo.setMaxWidth(Double.MAX_VALUE);
        updateTargetCombo(groupText);

        targetTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateTargetCombo(newVal));

        Label limitTypeLabel = new Label("Loại giới hạn:");

        limitTypeCombo = new ComboBox<>();
        limitTypeCombo.setItems(FXCollections.observableArrayList(LimitType.values()));
        limitTypeCombo.setValue(LimitType.DAILY);
        limitTypeCombo.getStyleClass().add("combo-box-styled");
        limitTypeCombo.setMaxWidth(Double.MAX_VALUE);

        Label timeLabel = new Label("Giới hạn thời gian:");

        // Time input fields in HBox for better layout
        HBox timeInputBox = new HBox(10);
        timeInputBox.getStyleClass().add("time-input-box");

        hoursField = new TextField();
        hoursField.setPromptText("Giờ");
        hoursField.getStyleClass().add("time-field");
        hoursField.setPrefWidth(80);

        Label hoursLabel = new Label("giờ");
        hoursLabel.getStyleClass().add("time-unit-label");

        minutesField = new TextField();
        minutesField.setPromptText("Phút");
        minutesField.getStyleClass().add("time-field");
        minutesField.setPrefWidth(80);

        Label minutesLabel = new Label("phút");
        minutesLabel.getStyleClass().add("time-unit-label");

        timeInputBox.getChildren().addAll(hoursField, hoursLabel, minutesField, minutesLabel);

        // Add form elements to grid
        int row = 0;
        grid.add(headerText, 0, row++, 2, 1);
        grid.add(new Separator(), 0, row++, 2, 1);
        grid.add(targetTypeLabel, 0, row);
        grid.add(targetTypeCombo, 1, row++);
        grid.add(targetLabel, 0, row);
        grid.add(targetCombo, 1, row++);
        grid.add(limitTypeLabel, 0, row);
        grid.add(limitTypeCombo, 1, row++);
        grid.add(timeLabel, 0, row);
        grid.add(timeInputBox, 1, row++);

        hoursField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                hoursField.setText(newValue.replaceAll("\\D", ""));
            }
        });

        minutesField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                minutesField.setText(newValue.replaceAll("\\D", ""));
            }
            if (!newValue.isEmpty()) {
                int val = Integer.parseInt(newValue);
                if (val > 59) minutesField.setText("59");
            }
        });

        dialogPane.setContent(grid);

        setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                LimitType selectedLimitType = limitTypeCombo.getValue();
                long hours = 0;
                int minutes = 0;
                try {
                    hours = Long.parseLong(hoursField.getText().isEmpty() ? "0" : hoursField.getText());
                } catch (NumberFormatException e) {
                    // mặc định 0 nếu không hợp lệ
                }
                try {
                    minutes = Integer.parseInt(minutesField.getText().isEmpty() ? "0" : minutesField.getText());
                } catch (NumberFormatException e) {
                    // mặc định 0 nếu không hợp lệ
                }
                Duration duration = Duration.ofHours(hours).plusMinutes(minutes);

                selectedTarget = targetCombo.getValue();

                controller.setLimit(selectedTarget, new Limit(selectedLimitType, duration));
            }
            return null;
        });
    }

    private void updateTargetCombo(String targetType) {
        if ("Ứng dụng".equals(targetType)) {
            List<Application> apps = controller.getApplications();
            targetCombo.setItems(FXCollections.observableArrayList(apps));
            if (!apps.isEmpty()) {
                targetCombo.setValue(apps.get(0));
            }
        } else if ("Nhóm".equals(targetType)) {
            List<ApplicationGroup> groups = controller.getGroups();
            targetCombo.setItems(FXCollections.observableArrayList(groups));
            if (!groups.isEmpty()) {
                targetCombo.setValue(groups.get(0));
            }
        }
    }
}