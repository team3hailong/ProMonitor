package com.promonitor.view;

import com.promonitor.controller.MainController;
import com.promonitor.model.enums.MonitorMode;
import com.promonitor.model.enums.NotificationType;
import com.promonitor.controller.UserSettings;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SettingsView {
    private final MainController controller;
    private BorderPane content;
    private UserSettings currentSettings;

    private ComboBox<NotificationType> notificationTypeCombo;
    private CheckBox notificationsEnabledCheck;
    private TextField soundPathField;
    private Spinner<Integer> warningThresholdSpinner;
    private CheckBox startAtLoginCheck;
    private CheckBox minimizeToTrayCheck;
    private CheckBox autoStartMonitoringCheck;
    private ComboBox<MonitorMode> monitorModeCombo;

    public SettingsView(MainController controller) {
        this.controller = controller;
        this.currentSettings = controller.getCurrentUser().getSettings();
        createContent();
    }

    private void createContent() {
        content = new BorderPane();
        content.setPadding(new Insets(15));
        content.getStyleClass().add("settings-view");

        Label titleLabel = new Label("Cài đặt người dùng");
        titleLabel.getStyleClass().add("view-title");
        content.setTop(titleLabel);

        VBox settingsBox = new VBox(20);
        settingsBox.setPadding(new Insets(10, 0, 0, 0));

        settingsBox.getChildren().add(createUserInfoSection());

        TitledPane notificationPane = createNotificationSettingsPane();

        TitledPane generalPane = createGeneralSettingsPane();

        Accordion accordion = new Accordion();
        accordion.getPanes().addAll(notificationPane, generalPane);
        accordion.setExpandedPane(notificationPane);

        settingsBox.getChildren().add(accordion);
        VBox.setVgrow(accordion, Priority.ALWAYS);

        Button saveButton = new Button("Lưu cài đặt");
        saveButton.getStyleClass().add("action-button");
        saveButton.setOnAction(e -> saveSettings());

        HBox buttonBox = new HBox(saveButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        settingsBox.getChildren().add(buttonBox);

        content.setCenter(settingsBox);

        loadSettings();
    }

    private GridPane createUserInfoSection() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(0, 0, 10, 0));

        addLabelAndValue(grid, 0, "Người dùng:", controller.getCurrentUser().getUserName());
        addLabelAndValue(grid, 1, "Ngày tạo:", controller.getCurrentUser().getCreatedDate().toString());

        LocalDateTime currentTime = LocalDateTime.parse("2025-03-07 12:44:07",
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        addLabelAndValue(grid, 2, "Thời gian hiện tại:",
                currentTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

        return grid;
    }

    private void addLabelAndValue(GridPane grid, int row, String labelText, String value) {
        Label label = new Label(labelText);
        label.getStyleClass().add("setting-label");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("setting-value");

        grid.add(label, 0, row);
        grid.add(valueLabel, 1, row);
    }

    private TitledPane createNotificationSettingsPane() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(10));

        Label typeLabel = new Label("Kiểu thông báo:");
        notificationTypeCombo = new ComboBox<>(FXCollections.observableArrayList(NotificationType.values()));
        notificationTypeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(NotificationType type) {
                return type != null ? type.getDisplayName() : "";
            }

            @Override
            public NotificationType fromString(String string) {
                return null; // Not used
            }
        });

        HBox typeBox = new HBox(10, typeLabel, notificationTypeCombo);
        typeBox.setAlignment(Pos.CENTER_LEFT);

        notificationsEnabledCheck = new CheckBox("Bật thông báo");

        Label soundLabel = new Label("Âm thanh cảnh báo:");
        soundPathField = new TextField();
        soundPathField.setEditable(false);
        soundPathField.setPrefWidth(300);

        Button browseButton = new Button("Chọn...");
        browseButton.setOnAction(e -> browseSoundFile());

        HBox soundBox = new HBox(10, soundLabel, soundPathField, browseButton);
        soundBox.setAlignment(Pos.CENTER_LEFT);

        Label thresholdLabel = new Label("Cảnh báo trước (phút):");
        warningThresholdSpinner = new Spinner<>(1, 30, 5);
        warningThresholdSpinner.setEditable(true);
        warningThresholdSpinner.setPrefWidth(80);

        HBox thresholdBox = new HBox(10, thresholdLabel, warningThresholdSpinner);
        thresholdBox.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(typeBox, notificationsEnabledCheck, soundBox, thresholdBox);

        return new TitledPane("Cài đặt thông báo", box);
    }

    private TitledPane createGeneralSettingsPane() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(10));

        startAtLoginCheck = new CheckBox("Tự động khởi động khi đăng nhập vào hệ thống");

        minimizeToTrayCheck = new CheckBox("Thu nhỏ vào thanh tác vụ thay vì đóng ứng dụng");

        autoStartMonitoringCheck = new CheckBox("Tự động bắt đầu theo dõi khi khởi động ứng dụng");

        Label modeLabel = new Label("Chế độ theo dõi:");
        monitorModeCombo = new ComboBox<>(FXCollections.observableArrayList(MonitorMode.values()));
        monitorModeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(MonitorMode mode) {
                return mode != null ? mode.getDisplayName() : "";
            }

            @Override
            public MonitorMode fromString(String string) {
                return null; // Not used
            }
        });

        HBox modeBox = new HBox(10, modeLabel, monitorModeCombo);
        modeBox.setAlignment(Pos.CENTER_LEFT);

        Label modeDescLabel = new Label();
        modeDescLabel.setWrapText(true);
        modeDescLabel.getStyleClass().add("description-text");

        monitorModeCombo.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        modeDescLabel.setText(newVal.getDescription());
                    }
                }
        );

        box.getChildren().addAll(startAtLoginCheck, minimizeToTrayCheck, autoStartMonitoringCheck,
                modeBox, modeDescLabel);

        return new TitledPane("Cài đặt chung", box);
    }

    private void browseSoundFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn file âm thanh cảnh báo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Audio Files", "*.wav", "*.mp3", "*.aac")
        );

        File selectedFile = fileChooser.showOpenDialog(content.getScene().getWindow());
        if (selectedFile != null) {
            soundPathField.setText(selectedFile.getAbsolutePath());
        }
    }

    private void loadSettings() {
        // Cài đặt thông báo
        notificationTypeCombo.getSelectionModel().select(currentSettings.getNotificationType());
        notificationsEnabledCheck.setSelected(currentSettings.isNotificationsEnabled());
        soundPathField.setText(currentSettings.getSoundAlertPath());
        warningThresholdSpinner.getValueFactory().setValue(currentSettings.getWarningThresholdMinutes());

        // Cài đặt chung
        startAtLoginCheck.setSelected(currentSettings.isStartAtLogin());
        minimizeToTrayCheck.setSelected(currentSettings.isMinimizeToTray());
        autoStartMonitoringCheck.setSelected(currentSettings.isAutoStartMonitoring());
        monitorModeCombo.getSelectionModel().select(currentSettings.getMonitorMode());
    }

    private void saveSettings() {
        UserSettings newSettings = new UserSettings();

        newSettings.setNotificationType(notificationTypeCombo.getValue());
        newSettings.setNotificationsEnabled(notificationsEnabledCheck.isSelected());
        newSettings.setSoundAlertPath(soundPathField.getText());
        newSettings.setWarningThresholdMinutes(warningThresholdSpinner.getValue());

        newSettings.setStartAtLogin(startAtLoginCheck.isSelected());
        newSettings.setMinimizeToTray(minimizeToTrayCheck.isSelected());
        newSettings.setAutoStartMonitoring(autoStartMonitoringCheck.isSelected());
        newSettings.setMonitorMode(monitorModeCombo.getValue());

        boolean saved = controller.updateUserSettings(newSettings);

        if (saved) {
            showInfo("Lưu cài đặt thành công", "Cài đặt của bạn đã được lưu thành công.");
            this.currentSettings = controller.getCurrentUser().getSettings();
        } else {
            showError("Lưu cài đặt thất bại", "Đã xảy ra lỗi khi lưu cài đặt.");
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public Node getContent() {
        return content;
    }
}