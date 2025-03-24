package com.promonitor.view;

import com.promonitor.controller.MainController;
import com.promonitor.model.enums.MonitorMode;
import com.promonitor.model.enums.NotificationType;
import com.promonitor.controller.UserSettings;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.File;

public class SettingsView {
    private final MainController controller;
    private BorderPane content;
    private UserSettings currentSettings;
    private BorderPane toastArea;

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

        GridPane headerGrid = new GridPane();
        headerGrid.setHgap(10);
        headerGrid.setVgap(2);
        headerGrid.getStyleClass().add("settings-header");

        FontAwesomeIconView settingsIcon = new FontAwesomeIconView(FontAwesomeIcon.COGS);
        settingsIcon.setGlyphSize(30);
        settingsIcon.setFill(Color.valueOf("#4a6bff"));
        StackPane iconContainer = new StackPane(settingsIcon);
        iconContainer.setMinHeight(40);
        headerGrid.add(iconContainer, 0, 0, 1, 2);

        Label titleLabel = new Label("Cài đặt người dùng");
        titleLabel.getStyleClass().add("view-title");
        headerGrid.add(titleLabel, 1, 0);

        Label descriptionLabel = new Label("Tùy chỉnh các thiết lập cá nhân cho ứng dụng");
        descriptionLabel.getStyleClass().add("description-text");
        headerGrid.add(descriptionLabel, 1, 1);

        content.setTop(headerGrid);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("settings-scroll-pane");

        VBox settingsBox = new VBox(15);
        settingsBox.setPadding(new Insets(10, 0, 0, 0));

        settingsBox.getChildren().add(createUserInfoSection());

        VBox notificationCard = createNotificationSettingsCard();
        VBox generalCard = createGeneralSettingsCard();

        settingsBox.getChildren().addAll(notificationCard, generalCard);

        BorderPane bottomArea = new BorderPane();

        Button saveButton = new Button("Lưu cài đặt");
        saveButton.getStyleClass().add("save-button");

        FontAwesomeIconView saveIcon = new FontAwesomeIconView(FontAwesomeIcon.SAVE);
        saveIcon.setGlyphSize(14);
        saveIcon.setFill(Color.WHITE);
        saveButton.setGraphic(saveIcon);
        saveButton.setGraphicTextGap(8);

        saveButton.setOnAction(e -> saveSettings());

        HBox buttonBox = new HBox(saveButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        // Toast area for notifications
        toastArea = new BorderPane();
        toastArea.setPadding(new Insets(0, 0, 10, 0));
        toastArea.setVisible(false);

        bottomArea.setTop(toastArea);
        bottomArea.setBottom(buttonBox);

        settingsBox.getChildren().add(bottomArea);

        scrollPane.setContent(settingsBox);
        content.setCenter(scrollPane);

        loadSettings();
    }

    private VBox createUserInfoSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("user-info-section");

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        FontAwesomeIconView userIcon = new FontAwesomeIconView(FontAwesomeIcon.USER);
        userIcon.setGlyphSize(16);
        userIcon.setFill(Color.valueOf("#4a6bff"));
        userIcon.getStyleClass().add("section-icon");

        Label sectionTitle = new Label("Thông tin người dùng");
        sectionTitle.getStyleClass().add("settings-category");

        headerBox.getChildren().addAll(userIcon, sectionTitle);

        HBox userInfoBox = new HBox(15);
        userInfoBox.setAlignment(Pos.CENTER_LEFT);

        StackPane avatarContainer = new StackPane();
        avatarContainer.getStyleClass().add("user-avatar");
        avatarContainer.setMinWidth(64);
        avatarContainer.setMinHeight(64);
        avatarContainer.setMaxWidth(64);
        avatarContainer.setMaxHeight(64);

        FontAwesomeIconView avatarIcon = new FontAwesomeIconView(FontAwesomeIcon.USER_CIRCLE);
        avatarIcon.setGlyphSize(48);
        avatarIcon.setFill(Color.valueOf("#4a6bff"));
        avatarContainer.getChildren().add(avatarIcon);

        VBox userDetailsBox = new VBox(5);

        Label userName = new Label(controller.getCurrentUser().getUserName());
        userName.getStyleClass().add("user-name");

        Label userMeta = new Label("Ngày tạo: " + controller.getCurrentUser().getCreatedDate());
        userMeta.getStyleClass().add("user-meta");

        userDetailsBox.getChildren().addAll(userName, userMeta);

        userInfoBox.getChildren().addAll(avatarContainer, userDetailsBox);

        section.getChildren().addAll(headerBox, userInfoBox);
        return section;
    }

    private VBox createNotificationSettingsCard() {
        VBox card = new VBox(15);
        card.getStyleClass().add("settings-card");

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        FontAwesomeIconView notificationIcon = new FontAwesomeIconView(FontAwesomeIcon.BELL);
        notificationIcon.setGlyphSize(16);
        notificationIcon.setFill(Color.valueOf("#4a6bff"));
        notificationIcon.getStyleClass().add("section-icon");

        Label cardTitle = new Label("Cài đặt thông báo");
        cardTitle.getStyleClass().add("settings-card-title");

        headerBox.getChildren().addAll(notificationIcon, cardTitle);

        Region divider = new Region();
        divider.getStyleClass().add("settings-card-divider");
        divider.setPrefHeight(1);
        divider.setMaxWidth(Double.MAX_VALUE);

        notificationsEnabledCheck = new CheckBox("Bật thông báo");

        HBox typeBox = new HBox(10);
        typeBox.getStyleClass().add("settings-row");

        Label typeLabel = new Label("Kiểu thông báo:");
        typeLabel.getStyleClass().add("setting-label");

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
        HBox.setHgrow(notificationTypeCombo, Priority.ALWAYS);

        typeBox.getChildren().addAll(typeLabel, notificationTypeCombo);

        VBox soundBox = new VBox(5);

        HBox soundInputBox = new HBox(10);
        soundInputBox.getStyleClass().add("settings-row");

        Label soundLabel = new Label("Âm thanh cảnh báo:");
        soundLabel.getStyleClass().add("setting-label");

        soundPathField = new TextField();
        soundPathField.setEditable(false);
        HBox.setHgrow(soundPathField, Priority.ALWAYS);

        Button browseButton = new Button("Chọn...");
        browseButton.getStyleClass().add("browse-button");

        FontAwesomeIconView folderIcon = new FontAwesomeIconView(FontAwesomeIcon.FOLDER_OPEN);
        folderIcon.setGlyphSize(12);
        folderIcon.setFill(Color.valueOf("#5f6368"));
        browseButton.setGraphic(folderIcon);
        browseButton.setGraphicTextGap(5);

        browseButton.setOnAction(e -> browseSoundFile());

        soundInputBox.getChildren().addAll(soundLabel, soundPathField, browseButton);

        Label soundDesc = new Label("Chọn file âm thanh để phát khi cảnh báo (định dạng .wav).");
        soundDesc.getStyleClass().add("description-text");

        soundBox.getChildren().addAll(soundInputBox, soundDesc);

        HBox thresholdBox = new HBox(10);
        thresholdBox.getStyleClass().add("settings-row");

        Label thresholdLabel = new Label("Cảnh báo trước (phút):");
        thresholdLabel.getStyleClass().add("setting-label");

        warningThresholdSpinner = new Spinner<>(1, 30, 5);
        warningThresholdSpinner.setEditable(true);
        warningThresholdSpinner.setPrefWidth(80);

        thresholdBox.getChildren().addAll(thresholdLabel, warningThresholdSpinner);

        Label thresholdDesc = new Label("Thời gian hiện cảnh báo trước khi hết hạn giới hạn");
        thresholdDesc.getStyleClass().add("description-text");

        card.getChildren().addAll(headerBox, divider, notificationsEnabledCheck, typeBox, soundBox, thresholdBox, thresholdDesc);

        return card;
    }

    private VBox createGeneralSettingsCard() {
        VBox card = new VBox(15);
        card.getStyleClass().add("settings-card");

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        FontAwesomeIconView generalIcon = new FontAwesomeIconView(FontAwesomeIcon.GEARS);
        generalIcon.setGlyphSize(16);
        generalIcon.setFill(Color.valueOf("#4a6bff"));
        generalIcon.getStyleClass().add("section-icon");

        Label cardTitle = new Label("Cài đặt chung");
        cardTitle.getStyleClass().add("settings-card-title");

        headerBox.getChildren().addAll(generalIcon, cardTitle);

        Region divider = new Region();
        divider.getStyleClass().add("settings-card-divider");
        divider.setPrefHeight(1);
        divider.setMaxWidth(Double.MAX_VALUE);

        VBox startupBox = new VBox(10);

        Label startupLabel = new Label("Tùy chọn khởi động");
        startupLabel.getStyleClass().add("settings-category");

        startAtLoginCheck = new CheckBox("Tự động khởi động khi đăng nhập vào hệ thống");
        minimizeToTrayCheck = new CheckBox("Thu nhỏ vào thanh tác vụ thay vì đóng ứng dụng");
        autoStartMonitoringCheck = new CheckBox("Tự động bắt đầu theo dõi khi khởi động ứng dụng");

        startupBox.getChildren().addAll(startupLabel, startAtLoginCheck, minimizeToTrayCheck, autoStartMonitoringCheck);

        VBox monitorBox = new VBox(10);

        Label monitorLabel = new Label("Cài đặt theo dõi");
        monitorLabel.getStyleClass().add("settings-category");

        HBox modeBox = new HBox(10);
        modeBox.getStyleClass().add("settings-row");

        Label modeLabel = new Label("Chế độ theo dõi:");
        modeLabel.getStyleClass().add("setting-label");

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
        HBox.setHgrow(monitorModeCombo, Priority.ALWAYS);

        modeBox.getChildren().addAll(modeLabel, monitorModeCombo);

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

        monitorBox.getChildren().addAll(monitorLabel, modeBox, modeDescLabel);

        card.getChildren().addAll(headerBox, divider, startupBox, new Region() {{setMinHeight(10);}}, monitorBox);

        return card;
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
            controller.updateAlertSound(selectedFile.getAbsolutePath());
        }
    }

    private void loadSettings() {
        notificationTypeCombo.getSelectionModel().select(currentSettings.getNotificationType());
        notificationsEnabledCheck.setSelected(currentSettings.isNotificationsEnabled());
        soundPathField.setText(currentSettings.getSoundAlertPath());
        warningThresholdSpinner.getValueFactory().setValue(currentSettings.getWarningThresholdMinutes());

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
            showToast("Cài đặt của bạn đã được lưu thành công", "success");
            this.currentSettings = controller.getCurrentUser().getSettings();
        } else {
            showToast("Đã xảy ra lỗi khi lưu cài đặt", "error");
        }
    }

    private void showToast(String message, String type) {
        HBox toastBox = new HBox(10);
        toastBox.setAlignment(Pos.CENTER_LEFT);
        toastBox.getStyleClass().add(type.equals("success") ? "toast-success" : "toast-error");

        FontAwesomeIconView icon;
        if (type.equals("success")) {
            icon = new FontAwesomeIconView(FontAwesomeIcon.CHECK_CIRCLE);
        } else {
            icon = new FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_CIRCLE);
        }
        icon.setGlyphSize(16);
        icon.getStyleClass().add("toast-icon");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("toast-message");

        toastBox.getChildren().addAll(icon, messageLabel);

        toastArea.setCenter(toastBox);
        toastArea.setVisible(true);

        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(event -> toastArea.setVisible(false));
        pause.play();
    }

    public Node getContent() {
        return content;
    }
}