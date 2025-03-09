package com.promonitor.view;

import com.promonitor.controller.MainController;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainView {
    private static final Logger logger = LoggerFactory.getLogger(MainView.class);

    private final MainController controller;
    private BorderPane root;
    private TabPane tabPane;

    // Các view thành phần
    private DashboardView dashboardView;
    private ApplicationsView applicationsView;
    private GroupsView groupsView;
    private LimitsView limitsView;
    private ReportsView reportsView;
    private SettingsView settingsView;

    // Các thành phần UI khác
    private Label statusLabel;
    private Button startStopButton;
    private Label timerLabel;

    private Timer uiUpdateTimer;

    public MainView(MainController controller) {
        this.controller = controller;
        initializeUI();
    }

    /**
     * Khởi tạo giao diện người dùng
     */
    private void initializeUI() {
        root = new BorderPane();
        root.getStyleClass().add("main-container");

        // Tạo các view thành phần
        dashboardView = new DashboardView(controller);
        applicationsView = new ApplicationsView(controller);
        groupsView = new GroupsView(controller);
        limitsView = new LimitsView(controller);
        reportsView = new ReportsView(controller);
        settingsView = new SettingsView(controller);

        // Tạo TabPane để chứa các view
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Tạo các tab
        Map<String, Node> tabContents = new HashMap<>();
        tabContents.put("Dashboard", dashboardView.getContent());
        tabContents.put("Ứng dụng", applicationsView.getContent());
        tabContents.put("Nhóm", groupsView.getContent());
        tabContents.put("Giới hạn", limitsView.getContent());
        tabContents.put("Báo cáo", reportsView.getContent());
        tabContents.put("Cài đặt", settingsView.getContent());

        for (Map.Entry<String, Node> entry : tabContents.entrySet()) {
            Tab tab = new Tab(entry.getKey());
            tab.setContent(entry.getValue());
            tabPane.getTabs().add(tab);
        }

        HBox statusBar = createStatusBar();

        root.setTop(createTopBar());
        root.setCenter(tabPane);
        root.setBottom(statusBar);

        startUIUpdates();
    }

    private HBox createTopBar() {
        HBox topBar = new HBox();
        topBar.getStyleClass().add("top-bar");
        topBar.setPadding(new Insets(10, 15, 10, 15));
        topBar.setSpacing(10);

        ImageView logoView = new ImageView(new Image(getClass().getResourceAsStream("/images/logo.png")));
        logoView.setFitHeight(40);
        logoView.setFitWidth(40);

        Label titleLabel = new Label("ProMonitor");
        titleLabel.getStyleClass().add("app-title");

        VBox userInfo = new VBox();
        userInfo.setSpacing(5);

        Label userLabel = new Label("Người dùng: " + controller.getCurrentUser().getUserName());
        userLabel.getStyleClass().add("user-info");

        userInfo.getChildren().add(userLabel);

        topBar.getChildren().addAll(logoView, titleLabel, new Separator(javafx.geometry.Orientation.VERTICAL), userInfo);
        HBox.setHgrow(userInfo, Priority.ALWAYS);

        startStopButton = new Button("Dừng theo dõi");
        startStopButton.getStyleClass().add("action-button");
        startStopButton.setOnAction(e -> toggleMonitoring());

        controller.monitoringActiveProperty().addListener((obs, oldVal, newVal) -> {
            updateStartStopButton(newVal);
        });

        timerLabel = new Label("00:00:00");
        timerLabel.getStyleClass().add("timer-label");

        topBar.getChildren().addAll(startStopButton, timerLabel);

        return topBar;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox();
        statusBar.getStyleClass().add("status-bar");
        statusBar.setPadding(new Insets(5, 10, 5, 10));

        statusLabel = new Label("Sẵn sàng");

        statusBar.getChildren().add(statusLabel);

        return statusBar;
    }

    private void updateStartStopButton(boolean isMonitoring) {
        if (isMonitoring) {
            startStopButton.setText("Dừng theo dõi");
            startStopButton.getStyleClass().remove("start-button");
            startStopButton.getStyleClass().add("stop-button");
            statusLabel.setText("Đang theo dõi");
        } else {
            startStopButton.setText("Bắt đầu theo dõi");
            startStopButton.getStyleClass().remove("stop-button");
            startStopButton.getStyleClass().add("start-button");
            statusLabel.setText("Đã dừng theo dõi");
        }
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
                    updateTimer();
                    dashboardView.updateData();
                    applicationsView.updateData();
                });
            }
        }, 0, 1000); // Cập nhật mỗi giây
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

    public void cleanup() {
        if (uiUpdateTimer != null) {
            uiUpdateTimer.cancel();
        }
    }
}