package com.promonitor.view;

import com.promonitor.controller.MainController;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MainView {
    private final MainController controller;
    private BorderPane root;

    private DashboardView dashboardView;
    private ApplicationsView applicationsView;

    private Label statusLabel;
    private Button startStopButton;
    private Label timerLabel;
    private Label currentTimeLabel;

    private Timer uiUpdateTimer;

    public MainView(MainController controller) {
        this.controller = controller;
        initializeUI();
    }

    private void initializeUI() {
        root = new BorderPane();
        root.getStyleClass().add("main-container");

        dashboardView = new DashboardView(controller);
        LimitsView limitsView = new LimitsView(controller);
        ReportsView reportsView = new ReportsView(controller);
        SettingsView settingsView = new SettingsView(controller);
        applicationsView = new ApplicationsView(controller, limitsView);
        GroupsView groupsView = new GroupsView(controller, limitsView);

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

    private VBox createSideBar(Node applicationsContent,
                               Node groupsContent, Node limitsContent,
                               Node reportsContent, Node settingsContent) {
        VBox sideBar = new VBox();
        sideBar.getStyleClass().add("side-bar");
        sideBar.setPrefWidth(180);
        sideBar.setSpacing(5);
        sideBar.setPadding(new Insets(15, 0, 15, 0));

        HBox logoBox = new HBox(10);
        logoBox.setAlignment(Pos.CENTER_LEFT);
        logoBox.setPadding(new Insets(0, 0, 15, 15));

        ImageView logoView = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.png"))));
        logoView.setFitHeight(32);
        logoView.setFitWidth(32);

        Label appName = new Label("ProMonitor");
        appName.getStyleClass().add("sidebar-app-title");

        logoBox.getChildren().addAll(logoView, appName);

        Separator logoDivider = new Separator();
        logoDivider.getStyleClass().add("sidebar-separator");

        ToggleGroup sidebarToggle = new ToggleGroup();

        //ToggleButton dashboardBtn = createSidebarButton("Dashboard", FontAwesomeIcon.DASHBOARD, dashboardContent, sidebarToggle);
        ToggleButton appsBtn = createSidebarButton("Ứng dụng", FontAwesomeIcon.WINDOWS, applicationsContent, sidebarToggle);
        ToggleButton groupsBtn = createSidebarButton("Nhóm", FontAwesomeIcon.OBJECT_GROUP, groupsContent, sidebarToggle);
        ToggleButton limitsBtn = createSidebarButton("Giới hạn", FontAwesomeIcon.CLOCK_ALT, limitsContent, sidebarToggle);
        ToggleButton reportsBtn = createSidebarButton("Báo cáo", FontAwesomeIcon.BAR_CHART, reportsContent, sidebarToggle);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Separator settingsDivider = new Separator();
        settingsDivider.getStyleClass().add("sidebar-separator");

        ToggleButton settingsBtn = createSidebarButton("Cài đặt", FontAwesomeIcon.COGS, settingsContent, sidebarToggle);

        appsBtn.setSelected(true);

        sideBar.getChildren().addAll(
                logoBox,
                logoDivider,
                //dashboardBtn,
                appsBtn,
                groupsBtn,
                limitsBtn,
                reportsBtn,
                spacer,
                settingsDivider,
                settingsBtn
        );

        return sideBar;
    }

    private ToggleButton createSidebarButton(String text, FontAwesomeIcon icon, Node content, ToggleGroup group) {
        ToggleButton button = new ToggleButton(text);
        button.getStyleClass().add("sidebar-button");
        button.setToggleGroup(group);
        button.setPrefWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);

        FontAwesomeIconView iconView = new FontAwesomeIconView(icon);
        iconView.setGlyphSize(16);
        iconView.setFill(Color.valueOf("#b0bec5"));
        iconView.getStyleClass().add("sidebar-icon");

        button.setGraphic(iconView);
        button.setGraphicTextGap(15);

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

        HBox timeBox = new HBox(8);
        timeBox.setAlignment(Pos.CENTER);

        FontAwesomeIconView clockIcon = new FontAwesomeIconView(FontAwesomeIcon.CALENDAR);
        clockIcon.setGlyphSize(16);
        clockIcon.setFill(Color.valueOf("#78909c"));

        currentTimeLabel = new Label();
        currentTimeLabel.getStyleClass().add("current-time-label");
        updateCurrentTime();

        timeBox.getChildren().addAll(clockIcon, currentTimeLabel);

        HBox userBox = new HBox(10);
        userBox.setAlignment(Pos.CENTER);

        StackPane avatar = new StackPane();
        Circle clipCircle = new Circle(16);
        clipCircle.setCenterX(16);
        clipCircle.setCenterY(16);

        FontAwesomeIconView userIcon = new FontAwesomeIconView(FontAwesomeIcon.USER);
        userIcon.setGlyphSize(20);
        userIcon.setFill(Color.valueOf("#4a6bff"));

        StackPane avatarContent = new StackPane(userIcon);
        avatarContent.setClip(clipCircle);
        avatarContent.getStyleClass().add("user-avatar");
        avatarContent.setMinSize(32, 32);
        avatarContent.setMaxSize(32, 32);

        avatar.getChildren().add(avatarContent);

        VBox userInfo = new VBox(2);
        userInfo.setAlignment(Pos.CENTER_LEFT);

        Label userLabel = new Label(controller.getCurrentUser().getUserName());
        userLabel.getStyleClass().add("user-name");

        Label userRole = new Label("Người dùng");
        userRole.getStyleClass().add("user-role");

        userInfo.getChildren().addAll(userLabel, userRole);

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

        // Spacers to position elements
        Region spacerLeft = new Region();
        HBox.setHgrow(spacerLeft, Priority.ALWAYS);

        Region spacerRight = new Region();
        HBox.setHgrow(spacerRight, Priority.ALWAYS);

        topBar.getChildren().addAll(timeBox, spacerLeft, userBox, spacerRight, startStopButton, timerBox);

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

    private void updateCurrentTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E, dd/MM/yyyy HH:mm");
        String formattedDateTime = LocalDateTime.now().format(formatter);
        currentTimeLabel.setText(formattedDateTime);
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
                    updateTimer();
                    updateCurrentTime();
                    dashboardView.updateData();
                    applicationsView.updateData();
                });
            }
        }, 0, 1000);
    }

    private void updateTimer() {
        Duration totalTime = controller.getTotalComputerUsageTime();
        long hours = totalTime.toHours();
        int minutes = totalTime.toMinutesPart();
        int seconds = totalTime.toSecondsPart();

        timerLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

    public void setStage(Stage stage) {
        controller.setMainStage(stage);
    }

    public BorderPane getRoot() {
        return root;
    }
}