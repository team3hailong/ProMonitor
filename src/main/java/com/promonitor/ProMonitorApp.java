package com.promonitor;

import com.promonitor.controller.MainController;
import com.promonitor.model.User;
import com.promonitor.view.MainView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ProMonitorApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(ProMonitorApp.class);

    private MainController mainController;
    private User currentUser;

    private static final String CURRENT_USER = "team3hailong";
    private static final String CURRENT_TIME = "2025-03-07 12:46:44";

    @Override
    public void start(Stage primaryStage) {
        logger.info("Khởi động ứng dụng ProMonitor");
        logger.info("Người dùng hiện tại: {}", CURRENT_USER);
        logger.info("Thời gian hiện tại: {}", CURRENT_TIME);

        try {
            currentUser = new User(CURRENT_USER);

            LocalDateTime parsedTime = LocalDateTime.parse(CURRENT_TIME,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            mainController = new MainController(currentUser, parsedTime);

            MainView mainView = new MainView(mainController);
            Scene scene = new Scene(mainView.getRoot(), 1024, 768);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            primaryStage.setTitle("ProMonitor - Quản lý thời gian sử dụng máy tính");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);

            mainView.setStage(primaryStage);

            primaryStage.setOnCloseRequest(event -> {
                if (!mainController.confirmExit()) {
                    event.consume();
                }
            });

            if (currentUser.getSettings().isAutoStartMonitoring()) {
                mainController.startMonitoring();
            }

            primaryStage.show();
        } catch (Exception e) {
            logger.error("Khởi động ứng dụng thất bại", e);
        }
    }

    @Override
    public void stop() {
        logger.info("Đóng ứng dụng ProMonitor");

        // Lưu cài đặt và dừng theo dõi
        try {
            if (mainController != null) {
                mainController.shutdownApp();
            }
        } catch (Exception e) {
            logger.error("Lỗi khi đóng ứng dụng", e);
        }

        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}