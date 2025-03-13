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

public class LimitEditDialog extends Dialog<Limit> {
    private final MainController controller;
    private final Application target;
    private final Limit originalLimit;

    private ComboBox<LimitType> limitTypeCombo;
    private TextField hoursField;
    private TextField minutesField;



    public LimitEditDialog(LimitsView.LimitInfo limitInfo, MainController controller) {
        this.controller = controller;
        this.target = (Application) limitInfo.getTarget();
        // Lấy giới hạn hiện có từ LimitManager dựa trên đối tượng
        this.originalLimit = controller.getLimitManager().getLimit(target);

        setTitle("Chỉnh sửa giới hạn");
        setHeaderText("Chỉnh sửa giới hạn cho: " + limitInfo.getTargetName());

        ButtonType updateButtonType = new ButtonType("Cập nhật", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        limitTypeCombo = new ComboBox<>();
        limitTypeCombo.setItems(FXCollections.observableArrayList(LimitType.values()));
        limitTypeCombo.setValue(originalLimit.getType());

        hoursField = new TextField();
        minutesField = new TextField();
        Duration duration = originalLimit.getValue();
        long hours = duration.toHours();
        int minutes = duration.toMinutesPart();
        hoursField.setText(String.valueOf(hours));
        minutesField.setText(String.valueOf(minutes));

        grid.add(new Label("Loại giới hạn:"), 0, 0);
        grid.add(limitTypeCombo, 1, 0);
        grid.add(new Label("Giới hạn (giờ):"), 0, 1);
        grid.add(hoursField, 1, 1);
        grid.add(new Label("Giới hạn (phút):"), 0, 2);
        grid.add(minutesField, 1, 2);

        getDialogPane().setContent(grid);

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