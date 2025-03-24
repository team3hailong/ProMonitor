package com.promonitor.controller;

import com.promonitor.model.interfaces.IConfigurable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

import com.promonitor.model.enums.MonitorMode;
import com.promonitor.model.enums.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserSettings implements IConfigurable {
    private static final Logger logger = LoggerFactory.getLogger(UserSettings.class);

    private static final String SETTINGS_FILENAME = "promonitor_settings.properties";
    private static final String USER_HOME = System.getProperty("user.home");
    private static final String CONFIG_DIR = Paths.get(USER_HOME, ".promonitor").toString();

    private NotificationType notificationType;
    private boolean notificationsEnabled;
    private String soundAlertPath;
    private int warningThresholdMinutes; 

    private boolean startAtLogin;
    private boolean minimizeToTray;
    private boolean autoStartMonitoring;
    private MonitorMode monitorMode;

    public UserSettings() {
        this.notificationType = NotificationType.POPUP;
        this.notificationsEnabled = true;
        this.soundAlertPath = "default_alert.wav";
        this.warningThresholdMinutes = 5;
        this.startAtLogin = false;
        this.minimizeToTray = true;
        this.autoStartMonitoring = true;
        this.monitorMode = MonitorMode.NORMAL;
    }

    @Override
    public boolean loadSettings() {
        File configFile = new File(CONFIG_DIR, SETTINGS_FILENAME);

        if (!configFile.exists()) {
            File configDir = new File(CONFIG_DIR);
            if (!configDir.exists() && !configDir.mkdirs()) {
                logger.error("Không thể tạo thư mục load: {}", CONFIG_DIR);
            }
            return saveSettings();
        }

        Properties properties = new Properties();

        try (FileInputStream fis = new FileInputStream(configFile)) {
            properties.load(fis);

            try {
                this.notificationType = NotificationType.valueOf(
                        properties.getProperty("notificationType", NotificationType.POPUP.name())
                );
            } catch (IllegalArgumentException e) {
                this.notificationType = NotificationType.POPUP;
            }

            this.notificationsEnabled = Boolean.parseBoolean(
                    properties.getProperty("notificationsEnabled", "true")
            );
            this.soundAlertPath = properties.getProperty("soundAlertPath", "default_alert.wav");
            this.warningThresholdMinutes = Integer.parseInt(
                    properties.getProperty("warningThresholdMinutes", "5")
            );
            this.startAtLogin = Boolean.parseBoolean(
                    properties.getProperty("startAtLogin", "false")
            );
            this.minimizeToTray = Boolean.parseBoolean(
                    properties.getProperty("minimizeToTray", "true")
            );
            this.autoStartMonitoring = Boolean.parseBoolean(
                    properties.getProperty("autoStartMonitoring", "true")
            );

            try {
                this.monitorMode = MonitorMode.valueOf(
                        properties.getProperty("monitorMode", MonitorMode.NORMAL.name())
                );
            } catch (IllegalArgumentException e) {
                this.monitorMode = MonitorMode.NORMAL;
            }

            return true;
        } catch (IOException | IllegalArgumentException e) {
            logger.error("Lỗi khi tải cài đặt", e);
            return false;
        }
    }

    @Override
    public boolean saveSettings() {
        File configDir = new File(CONFIG_DIR);
        if (!configDir.exists() && !configDir.mkdirs()) {
            logger.error("Không thể tạo thư mục: {}", CONFIG_DIR);
        }

        File configFile = new File(configDir, SETTINGS_FILENAME);
        Properties properties = new Properties();

        properties.setProperty("notificationType", notificationType.name());
        properties.setProperty("notificationsEnabled", String.valueOf(notificationsEnabled));
        properties.setProperty("soundAlertPath", soundAlertPath);
        properties.setProperty("warningThresholdMinutes", String.valueOf(warningThresholdMinutes));

        properties.setProperty("startAtLogin", String.valueOf(startAtLogin));
        properties.setProperty("minimizeToTray", String.valueOf(minimizeToTray));
        properties.setProperty("autoStartMonitoring", String.valueOf(autoStartMonitoring));
        properties.setProperty("monitorMode", monitorMode.name());

        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            properties.store(fos, "ProMonitor User Settings");
            return true;
        } catch (IOException e) {
            logger.error("Lỗi khi lưu cài đặt", e);
            return false;
        }
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public String getSoundAlertPath() {
        return soundAlertPath;
    }

    public void setSoundAlertPath(String soundAlertPath) {
        this.soundAlertPath = soundAlertPath;
    }

    public int getWarningThresholdMinutes() {
        return warningThresholdMinutes;
    }

    public void setWarningThresholdMinutes(int warningThresholdMinutes) {
        this.warningThresholdMinutes = warningThresholdMinutes;
    }

    public boolean isStartAtLogin() {
        return startAtLogin;
    }

    public void setStartAtLogin(boolean startAtLogin) {
        this.startAtLogin = startAtLogin;
    }

    public boolean isMinimizeToTray() {
        return minimizeToTray;
    }

    public void setMinimizeToTray(boolean minimizeToTray) {
        this.minimizeToTray = minimizeToTray;
    }

    public boolean isAutoStartMonitoring() {
        return autoStartMonitoring;
    }

    public void setAutoStartMonitoring(boolean autoStartMonitoring) {
        this.autoStartMonitoring = autoStartMonitoring;
    }

    public MonitorMode getMonitorMode() {
        return monitorMode;
    }

    public void setMonitorMode(MonitorMode monitorMode) {
        this.monitorMode = monitorMode;
    }
}