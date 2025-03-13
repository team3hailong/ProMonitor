package com.promonitor.view;

import com.promonitor.controller.MainController;

import com.promonitor.model.Application;
import com.promonitor.model.ApplicationGroup;
import com.promonitor.model.Limit;
import com.promonitor.model.enums.LimitType;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.time.Duration;
import java.util.List;

public class LimitCreationDialog extends Dialog<Limit> {

    private final MainController controller;
    private ComboBox<String> targetTypeCombo;
    private ComboBox<Object> targetCombo;
    private ComboBox<LimitType> limitTypeCombo;
    private TextField hoursField;
    private TextField minutesField;
    private Object selectedTarget;
    private String selectedTargetType;

    public LimitCreationDialog(MainController controller) {
        this.controller = controller;
        setTitle("Tạo giới hạn mới");
        setHeaderText("Thiết lập giới hạn thời gian");

        ButtonType createButtonType = new ButtonType("Tạo", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        targetTypeCombo = new ComboBox<>();
        targetTypeCombo.setItems(FXCollections.observableArrayList("Ứng dụng", "Nhóm"));
        targetTypeCombo.setValue("Ứng dụng");

        targetCombo = new ComboBox<>();
        updateTargetCombo("Ứng dụng");

        targetTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateTargetCombo(newVal));

        limitTypeCombo = new ComboBox<>();
        limitTypeCombo.setItems(FXCollections.observableArrayList(LimitType.values()));
        limitTypeCombo.setValue(LimitType.DAILY);

        hoursField = new TextField();
        hoursField.setPromptText("Giờ");
        minutesField = new TextField();
        minutesField.setPromptText("Phút");

        grid.add(new Label("Loại đối tượng:"), 0, 0);
        grid.add(targetTypeCombo, 1, 0);
        grid.add(new Label("Đối tượng:"), 0, 1);
        grid.add(targetCombo, 1, 1);
        grid.add(new Label("Loại giới hạn:"), 0, 2);
        grid.add(limitTypeCombo, 1, 2);
        grid.add(new Label("Giới hạn (giờ):"), 0, 3);
        grid.add(hoursField, 1, 3);
        grid.add(new Label("Giới hạn (phút):"), 0, 4);
        grid.add(minutesField, 1, 4);

        getDialogPane().setContent(grid);

        setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                LimitType selectedLimitType = limitTypeCombo.getValue();
                long hours = 0;
                int minutes = 0;
                try {
                    hours = Long.parseLong(hoursField.getText());
                } catch (NumberFormatException e) {
                    // mặc định 0 nếu không hợp lệ
                }
                try {
                    minutes = Integer.parseInt(minutesField.getText());
                } catch (NumberFormatException e) {
                    // mặc định 0 nếu không hợp lệ
                }
                Duration duration = Duration.ofHours(hours).plusMinutes(minutes);

                selectedTarget = targetCombo.getValue();
                selectedTargetType = targetTypeCombo.getValue();

                if(selectedTargetType.equals("Ứng dụng"))
                    controller.setLimit((Application) selectedTarget, new Limit(selectedLimitType, duration));
                else
                    controller.setLimit((ApplicationGroup) selectedTarget, new Limit(selectedLimitType, duration));
            }
            return null;
        });
    }

    public Object getSelectedTarget() {
        return selectedTarget;
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