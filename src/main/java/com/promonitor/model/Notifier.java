package com.promonitor.model;

import com.promonitor.controller.UserSettings;
import com.promonitor.model.enums.NotificationType;
import com.promonitor.model.interfaces.IConfigurable;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Notifier implements IConfigurable {
    private static final Logger logger = LoggerFactory.getLogger(Notifier.class);

    private final UserSettings userSettings;
    private TrayIcon trayIcon;
    private boolean traySupported;
    private MediaPlayer alertSound;
    private final String CONFIG_FILE = "notifier_config.properties";
    private Timer blinkTimer;
    private Stage mainStage; // Tham chiếu đến cửa sổ chính

    public Notifier(UserSettings userSettings) {
        this.userSettings = userSettings;
        initializeTray();
        loadAlertSound();
        loadSettings();
    }

    private void initializeTray() {
        try {
            if (SystemTray.isSupported()) {
                traySupported = true;
                trayIcon = new TrayIcon(Toolkit.getDefaultToolkit().getImage("icon.png"), "ProMonitor");
                trayIcon.setImageAutoSize(true);
                SystemTray.getSystemTray().add(trayIcon);
                logger.info("Đã khởi tạo System Tray thành công");
            } else {
                traySupported = false;
                logger.warn("Hệ thống không hỗ trợ System Tray");
            }
        } catch (Exception e) {
            traySupported = false;
            logger.error("Khởi tạo System Tray thất bại", e);
        }
    }

    private void loadAlertSound() {
        try {
            File soundFile = new File(userSettings.getSoundAlertPath());
            if (soundFile.exists()) {
                Media sound = new Media(soundFile.toURI().toString());
                alertSound = new MediaPlayer(sound);
                logger.debug("Đã tải âm thanh cảnh báo: {}", soundFile.getPath());
            } else {
                try {
                    String defaultSoundPath = Objects.requireNonNull(getClass().getResource("/sounds/alert.wav")).toExternalForm();
                    Media defaultSound = new Media(defaultSoundPath);
                    alertSound = new MediaPlayer(defaultSound);
                    logger.debug("Đã tải âm thanh cảnh báo mặc định");
                } catch (Exception e) {
                    logger.error("Không thể tải âm thanh cảnh báo mặc định", e);
                }
            }
        } catch (Exception e) {
            logger.error("Lỗi khi tải âm thanh cảnh báo", e);
        }
    }

    public void setMainStage(Stage stage) {
        this.mainStage = stage;
    }

    public void notify(String message, String title, NotificationType type) {
        if (!userSettings.isNotificationsEnabled()) {
            return;
        }

        logger.debug("Hiển thị thông báo: {}, loại: {}", title, type);

        switch (type) {
            case SOUND:
                playAlertSound();
                break;
            case TASKBAR_ICON:
                blinkTaskbarIcon();
                break;
            case ALL:
                showPopupNotification(message, title);
                playAlertSound();
                blinkTaskbarIcon();
                break;
            default:
                showPopupNotification(message, title);
                break;
        }
    }

    private void showPopupNotification(String message, String title) {
        if (traySupported) {
            trayIcon.displayMessage(title, message, MessageType.INFO);
        } else {
            // Sử dụng JavaFX Alert nếu không hỗ trợ system tray
            Platform.runLater(() -> {
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.show();
            });
        }
    }

    private void playAlertSound() {
        try {
            if (alertSound != null) {
                alertSound.stop();
                alertSound.seek(javafx.util.Duration.ZERO);
                alertSound.play();
            }
        } catch (Exception e) {
            logger.error("Lỗi khi phát âm thanh cảnh báo", e);
        }
    }

    private void blinkTaskbarIcon() {
        if (blinkTimer != null && blinkTimer.isRunning()) {
            return;
        }

        if (mainStage != null) {
            Platform.runLater(() -> {
                mainStage.setIconified(false);
                mainStage.toFront();
                mainStage.requestFocus();
            });
        }

        final int[] counter = {0};
        blinkTimer = new Timer(500, evt -> {
            if (mainStage != null) {
                Platform.runLater(() -> mainStage.toFront());
            }

            counter[0]++;
            if (counter[0] > 10) {
                blinkTimer.stop();
            }
        });
        blinkTimer.start();
    }

    public boolean updateAlertSound(String soundPath) {
        try {
            File soundFile = new File(soundPath);
            if (soundFile.exists()) {
                if (alertSound != null) {
                    alertSound.stop();
                    alertSound.dispose();
                }

                Media sound = new Media(soundFile.toURI().toString());
                alertSound = new MediaPlayer(sound);

                userSettings.setSoundAlertPath(soundPath);
                userSettings.saveSettings();

                logger.info("Đã cập nhật âm thanh cảnh báo: {}", soundPath);
                return true;
            }
            logger.warn("File âm thanh không tồn tại: {}", soundPath);
            return false;
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật âm thanh cảnh báo", e);
            return false;
        }
    }

    @Override
    public boolean loadSettings() {
        try {
            File configFile = new File(CONFIG_FILE);
            if (!configFile.exists()) {
                return saveSettings();
            }

            Properties properties = new Properties();
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);

                // Tải các cài đặt tùy chỉnh ở đây nếu cần
                // Ví dụ: alertVolume = Integer.parseInt(properties.getProperty("alertVolume", "100"));

                return true;
            }
        } catch (IOException e) {
            logger.error("Lỗi khi tải cài đặt thông báo", e);
            return false;
        }
    }

    @Override
    public boolean saveSettings() {
        try {
            Properties properties = new Properties();

            // Lưu các cài đặt tùy chỉnh ở đây nếu cần
            // Ví dụ: properties.setProperty("alertVolume", String.valueOf(alertVolume));

            try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
                properties.store(fos, "ProMonitor Notifier Configuration");
                return true;
            }
        } catch (IOException e) {
            logger.error("Lỗi khi lưu cài đặt thông báo", e);
            return false;
        }
    }

    public void cleanup() {
        if (alertSound != null) {
            alertSound.stop();
            alertSound.dispose();
        }

        if (traySupported) {
            try {
                SystemTray.getSystemTray().remove(trayIcon);
            } catch (Exception e) {
                logger.error("Lỗi khi xóa biểu tượng khỏi system tray", e);
            }
        }

        if (blinkTimer != null && blinkTimer.isRunning()) {
            blinkTimer.stop();
        }
    }
}