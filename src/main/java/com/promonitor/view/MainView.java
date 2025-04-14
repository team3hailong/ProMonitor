package com.promonitor.view;

import com.promonitor.controller.MainController;

import com.promonitor.model.enums.UserMode;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.time.Duration;
import java.util.*;

public class MainView {
    private final MainController controller;
    private ApplicationsView applicationsView;
    private Node helpContent;        
    private BorderPane root;

    private FontAwesomeIconView userIcon;
    private Label userRoleLabel;
    private UserMode userRole;

    private Label statusLabel;
    private Button startStopButton;
    private Label timerLabel;
    private Button collapseBtn;

    private Timer uiUpdateTimer;

    public MainView(MainController controller) {
        this.controller = controller;
        this.userRole = controller.getCurrentUser().getSettings()
                .getUserMode();
        initializeUI();
    }

    private void initializeUI() {
        root = new BorderPane();
        root.getStyleClass().add("main-container");

        LimitsView limitsView = new LimitsView(controller);
        ReportsView reportsView = new ReportsView(controller);
        SettingsView settingsView = createSettingsView();
        applicationsView = new ApplicationsView(controller, limitsView);
        GroupsView groupsView = new GroupsView(controller, limitsView);
        helpContent = new HelpView().getContent();

        VBox sideBar = createSideBar(applicationsView.getContent(),
                groupsView.getContent(), limitsView.getContent(),
                reportsView.getContent(), settingsView.getContent());

        HBox statusBar = createStatusBar();

        root.setLeft(sideBar);
        root.setTop(createTopBar());
        root.setBottom(statusBar);

        root.setCenter(applicationsView.getContent());

        startUIUpdates();
    }

    private SettingsView createSettingsView() {
        SettingsView settingsView = new SettingsView(controller);
        settingsView.getUserModeComboProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("UserMode changed: " + oldVal + " -> " + newVal);
            if (newVal != null) {
                updateUserDisplay(newVal);
            }
        });

        return settingsView;
    }

    private boolean isExpanded = true;
    private final double EXPANDED_WIDTH = 180;

    VBox createSideBar(Node applicationsContent, Node groupsContent, Node limitsContent,
                       Node reportsContent, Node settingsContent) {
        VBox sideBar = new VBox();
        sideBar.getStyleClass().add("side-bar");
        sideBar.setPrefWidth(EXPANDED_WIDTH);
        sideBar.setSpacing(5);
        sideBar.setPadding(new Insets(15, 0, 15, 0));

        HBox logoBox = new HBox(10);
        logoBox.setAlignment(Pos.CENTER_LEFT);
        logoBox.setPadding(new Insets(0, 0, 15, 15));
        logoBox.getStyleClass().add("logo-box");

        ImageView logoView = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.png"))));
        logoView.setFitHeight(32);
        logoView.setFitWidth(32);

        Label appName = new Label("ProMonitor");
        appName.getStyleClass().add("sidebar-app-title");

        logoBox.getChildren().addAll(logoView, appName);

        Separator logoDivider = new Separator();
        logoDivider.getStyleClass().add("sidebar-separator");

        ToggleGroup sidebarToggle = new ToggleGroup();

        ToggleButton appsBtn = createSidebarButton("Ứng dụng", FontAwesomeIcon.WINDOWS, applicationsContent, sidebarToggle);
        ToggleButton groupsBtn = createSidebarButton("Nhóm", FontAwesomeIcon.OBJECT_GROUP, groupsContent, sidebarToggle);
        ToggleButton limitsBtn = createSidebarButton("Giới hạn", FontAwesomeIcon.CLOCK_ALT, limitsContent, sidebarToggle);
        ToggleButton reportsBtn = createSidebarButton("Báo cáo", FontAwesomeIcon.BAR_CHART, reportsContent, sidebarToggle);

        // Add help button before the spacer
        ToggleButton helpBtn = createSidebarButton("Hướng dẫn", FontAwesomeIcon.QUESTION_CIRCLE, helpContent, sidebarToggle);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Separator settingsDivider = new Separator();
        settingsDivider.getStyleClass().add("sidebar-separator");

        ToggleButton settingsBtn = createSidebarButton("Cài đặt", FontAwesomeIcon.COGS, settingsContent, sidebarToggle);

        collapseBtn = createCollapseButton();
        appsBtn.setSelected(true);

        sideBar.getChildren().addAll(
                logoBox,
                logoDivider,
                appsBtn,
                groupsBtn,
                limitsBtn,
                reportsBtn,
                helpBtn,
                spacer,
                settingsDivider,
                settingsBtn,
                new Separator(),
                collapseBtn
        );

        return sideBar;
    }

    private Button createCollapseButton() {
        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setPrefWidth(EXPANDED_WIDTH);
        buttonBox.setMaxHeight(15);
        buttonBox.setPadding(new Insets(0, 0, 0, 5));

        Button collapseBtn = new Button();
        collapseBtn.getStyleClass().add("sidebar-button");

        FontAwesomeIconView collapseIcon = new FontAwesomeIconView(FontAwesomeIcon.ANGLE_DOUBLE_LEFT);
        collapseIcon.setGlyphSize(18);
        collapseIcon.getStyleClass().add("sidebar-icon");

        Label collapseText = new Label("");

        buttonBox.getChildren().addAll(collapseIcon, collapseText);
        collapseBtn.setGraphic(buttonBox);

        collapseBtn.setOnAction(e -> toggleSidebar());

        return collapseBtn;
    }

    private void toggleSidebar() {
        VBox sideBar = (VBox) collapseBtn.getParent();
        isExpanded = !isExpanded;

        Timeline timeline = new Timeline();
        KeyValue width;

        if (isExpanded) {
            width = new KeyValue(sideBar.prefWidthProperty(), EXPANDED_WIDTH);

            for (Node node : sideBar.getChildren()) {
                if (node instanceof ToggleButton) {
                    HBox box = (HBox) ((ToggleButton) node).getGraphic();
                    if (box.getChildren().size() > 1) {
                        box.getChildren().get(1).setVisible(true);
                        box.getChildren().get(1).setManaged(true);
                    }
                }
            }

            HBox collapseBox = (HBox) collapseBtn.getGraphic();
            ((FontAwesomeIconView) collapseBox.getChildren().get(0)).setIcon(FontAwesomeIcon.ANGLE_DOUBLE_LEFT);
        } else {
            width = new KeyValue(sideBar.prefWidthProperty(), 60);

            for (Node node : sideBar.getChildren()) {
                if (node instanceof ToggleButton) {
                    HBox box = (HBox) ((ToggleButton) node).getGraphic();
                    if (box.getChildren().size() > 1) {
                        box.getChildren().get(1).setVisible(false);
                        box.getChildren().get(1).setManaged(false);
                    }
                }
            }

            HBox collapseBox = (HBox) collapseBtn.getGraphic();
            ((FontAwesomeIconView) collapseBox.getChildren().get(0)).setIcon(FontAwesomeIcon.ANGLE_DOUBLE_RIGHT);
            ((Label) collapseBox.getChildren().get(1)).setText("");
        }

        KeyFrame keyFrame = new KeyFrame(javafx.util.Duration.millis(300), width);
        timeline.getKeyFrames().add(keyFrame);
        timeline.play();

        HBox logoBox = (HBox) sideBar.getChildren().get(0);
        if (logoBox.getChildren().size() > 1) {
            logoBox.getChildren().get(1).setVisible(isExpanded);
            logoBox.getChildren().get(1).setManaged(isExpanded);
        }

        for (Node node : sideBar.getChildren()) {
            if (node instanceof ToggleButton) {
                HBox box = (HBox) ((ToggleButton) node).getGraphic();
                box.setAlignment(isExpanded ? Pos.CENTER_LEFT : Pos.CENTER);
            }
        }
    }

    private ToggleButton createSidebarButton(String text, FontAwesomeIcon icon, Node content, ToggleGroup toggleGroup) {
        ToggleButton button = new ToggleButton();
        button.getStyleClass().add("sidebar-button");
        button.setToggleGroup(toggleGroup);
        button.setMaxWidth(Double.MAX_VALUE);

        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setSpacing(10);

        FontAwesomeIconView iconView = new FontAwesomeIconView(icon);
        iconView.setGlyphSize(16);
        iconView.getStyleClass().add("sidebar-icon");

        Label label = new Label(text);

        buttonBox.getChildren().addAll(iconView, label);
        button.setGraphic(buttonBox);

        button.setOnAction(e -> {
            if (button.isSelected()) {
                root.setCenter(content);
            }
        });

        return button;
    }

    private HBox createTopBar() {
        HBox topBar = new HBox();
        topBar.getStyleClass().add("top-bar");
        topBar.setPadding(new Insets(10, 15, 10, 15));
        topBar.setSpacing(15);
        topBar.setAlignment(Pos.CENTER);

        HBox userBox = new HBox(10);
        userBox.setAlignment(Pos.CENTER);

        StackPane avatar = new StackPane();
        Circle clipCircle = new Circle(16);
        clipCircle.setCenterX(16);
        clipCircle.setCenterY(16);

        UserMode currentMode = controller.getCurrentUser().getSettings().getUserMode();
        this.userIcon = new FontAwesomeIconView(getUserIconForMode(currentMode));  // Use the class field
        this.userIcon.setGlyphSize(20);
        this.userIcon.setFill(Color.valueOf("#4a6bff"));

        StackPane avatarContent = new StackPane(this.userIcon);
        avatarContent.setClip(clipCircle);
        avatarContent.getStyleClass().add("user-avatar");
        avatarContent.setMinSize(32, 32);
        avatarContent.setMaxSize(32, 32);

        avatar.getChildren().add(avatarContent);

        VBox userInfo = new VBox(2);
        userInfo.setAlignment(Pos.CENTER_LEFT);

        Label userLabel = new Label("Chế độ");
        userLabel.getStyleClass().add("user-name");

        String roleText = userRole.getDisplayName();
        userRoleLabel = new Label(roleText);
        userInfo.getChildren().addAll(userLabel, userRoleLabel);

        userBox.getChildren().addAll(avatar, userInfo);

        startStopButton = new Button("Bắt đầu theo dõi");
        startStopButton.getStyleClass().addAll("action-button", "start-button");

        FontAwesomeIconView playIcon = new FontAwesomeIconView(FontAwesomeIcon.PLAY);
        playIcon.setGlyphSize(12);
        playIcon.setFill(Color.WHITE);
        startStopButton.setGraphic(playIcon);
        startStopButton.setGraphicTextGap(8);

        startStopButton.setOnAction(e -> toggleMonitoring());

        controller.monitoringActiveProperty().addListener((obs, oldVal, newVal) -> updateStartStopButton(newVal));

        HBox timerBox = new HBox(8);
        timerBox.setAlignment(Pos.CENTER);
        timerBox.getStyleClass().add("timer-box");

        FontAwesomeIconView timerIcon = new FontAwesomeIconView(FontAwesomeIcon.CLOCK_ALT);
        timerIcon.setGlyphSize(14);
        timerIcon.setFill(Color.valueOf("#4a6bff"));

        timerLabel = new Label("00:00:00");
        timerLabel.getStyleClass().add("timer-label");

        timerBox.getChildren().addAll(timerIcon, timerLabel);

        Region spacer= new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar.getChildren().addAll(userBox, spacer, startStopButton, timerBox);

        return topBar;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(8);
        statusBar.getStyleClass().add("status-bar");
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setAlignment(Pos.CENTER_LEFT);

        FontAwesomeIconView statusIcon = new FontAwesomeIconView(FontAwesomeIcon.INFO_CIRCLE);
        statusIcon.setGlyphSize(12);
        statusIcon.setFill(Color.valueOf("#78909c"));

        statusLabel = new Label("Sẵn sàng");
        statusLabel.getStyleClass().add("status-label");

        statusBar.getChildren().addAll(statusIcon, statusLabel);

        return statusBar;
    }

    private void updateStartStopButton(boolean isMonitoring) {
        FontAwesomeIconView icon;
        if (isMonitoring) {
            startStopButton.setText("Dừng theo dõi");
            startStopButton.getStyleClass().remove("start-button");
            startStopButton.getStyleClass().add("stop-button");

            icon = new FontAwesomeIconView(FontAwesomeIcon.STOP);
            statusLabel.setText("Đang theo dõi");
        } else {
            startStopButton.setText("Bắt đầu theo dõi");
            startStopButton.getStyleClass().remove("stop-button");
            startStopButton.getStyleClass().add("start-button");

            icon = new FontAwesomeIconView(FontAwesomeIcon.PLAY);
            statusLabel.setText("Đã dừng theo dõi");
        }

        icon.setGlyphSize(12);
        icon.setFill(Color.WHITE);
        startStopButton.setGraphic(icon);
    }

    private void toggleMonitoring() {
        if (controller.monitoringActiveProperty().get()) {
            controller.stopMonitoring();
        } else {
            controller.startMonitoring();
        }
    }

    private void startUIUpdates() {
        if (uiUpdateTimer != null) {
            uiUpdateTimer.cancel();
        }

        uiUpdateTimer = new Timer(true);
        uiUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (timerLabel != null) updateTimer();
                    if (applicationsView != null) applicationsView.updateData();
                });
            }
        }, 0, 1000);
    }

    private void updateTimer() {
        if (timerLabel == null || controller == null) return;
        Duration totalTime = controller.getTotalComputerUsageTime();
        long hours = totalTime.toHours();
        int minutes = totalTime.toMinutesPart();
        int seconds = totalTime.toSecondsPart();

        timerLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

    public void setStage(Stage stage) {
        controller.setMainStage(stage);
    }

    private FontAwesomeIcon getUserIconForMode(UserMode mode) {
        if (mode == null) {
            return FontAwesomeIcon.USER;
        }
        String modeName = mode.name().toLowerCase();
        if (modeName.equals("children")) {
            return FontAwesomeIcon.CHILD;
        } else if (modeName.contains("work")) {
            return FontAwesomeIcon.BRIEFCASE;
        } else { // default
            return FontAwesomeIcon.USER;
        }
    }

    public void updateUserDisplay(UserMode newMode) {
        userRoleLabel.setText(newMode.getDisplayName());
        userIcon.setIcon(getUserIconForMode(newMode));

        this.userRole = newMode;
    }

    public BorderPane getRoot() {
        return root;
    }
}