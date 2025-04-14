package com.promonitor.controller;

import com.promonitor.model.interfaces.IConfigurable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;

import com.promonitor.model.enums.MonitorMode;
import com.promonitor.model.enums.NotificationType;
import com.promonitor.model.enums.UserMode;
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
    private UserMode userMode;
    
    // Store settings for each user mode
    private final Map<UserMode, ModeSettings> modeSettingsMap;

    public UserSettings() {
        this.notificationType = NotificationType.POPUP;
        this.notificationsEnabled = true;
        this.soundAlertPath = "default_alert.wav";
        this.warningThresholdMinutes = 5;
        this.startAtLogin = false;
        this.minimizeToTray = true;
        this.autoStartMonitoring = true;
        this.monitorMode = MonitorMode.NORMAL;
        this.userMode = UserMode.DEFAULT;
        this.modeSettingsMap = new HashMap<>();
        
        // Initialize with default settings for each mode
        initializeDefaultModeSettings();
    }
    
    private void initializeDefaultModeSettings() {
        // Default mode settings
        ModeSettings defaultSettings = new ModeSettings(
                NotificationType.POPUP, true, "default_alert.wav", 5, MonitorMode.NORMAL
        );
        
        // Work mode settings
        ModeSettings workSettings = new ModeSettings(
                NotificationType.POPUP, true, "default_alert.wav", 10, MonitorMode.NORMAL
        );
        
        // Children mode settings
        ModeSettings childrenSettings = new ModeSettings(
                NotificationType.ALL, true, "default_alert.wav", 3, MonitorMode.STRICT
        );
        
        modeSettingsMap.put(UserMode.DEFAULT, defaultSettings);
        modeSettingsMap.put(UserMode.WORK, workSettings);
        modeSettingsMap.put(UserMode.CHILDREN, childrenSettings);
    }
    
    /**
     * Saves current settings to the appropriate mode before switching
     */
    public void saveModeSettings(UserMode mode) {
        ModeSettings settings = modeSettingsMap.getOrDefault(mode, new ModeSettings());
        settings.notificationType = this.notificationType;
        settings.notificationsEnabled = this.notificationsEnabled;
        settings.soundAlertPath = this.soundAlertPath;
        settings.warningThresholdMinutes = this.warningThresholdMinutes;
        settings.monitorMode = this.monitorMode;
        modeSettingsMap.put(mode, settings);
    }
    
    /**
     * Apply settings for the selected user mode
     */
    public void applyModeSettings(UserMode mode) {
        ModeSettings settings = modeSettingsMap.get(mode);
        if (settings != null) {
            this.notificationType = settings.notificationType;
            this.notificationsEnabled = settings.notificationsEnabled;
            this.soundAlertPath = settings.soundAlertPath;
            this.warningThresholdMinutes = settings.warningThresholdMinutes;
            this.monitorMode = settings.monitorMode;
        }
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

            try {
                this.userMode = UserMode.valueOf(
                        properties.getProperty("userMode", UserMode.DEFAULT.name())
                );
            } catch (IllegalArgumentException e) {
                this.userMode = UserMode.DEFAULT;
            }
            
            // Load mode-specific settings
            loadModeSettings(properties);

            return true;
        } catch (IOException | IllegalArgumentException e) {
            logger.error("Lỗi khi tải cài đặt", e);
            return false;
        }
    }
    
    private void loadModeSettings(Properties properties) {
        for (UserMode mode : UserMode.values()) {
            String prefix = "mode." + mode.name() + ".";
            
            ModeSettings settings = new ModeSettings();
            try {
                settings.notificationType = NotificationType.valueOf(
                    properties.getProperty(prefix + "notificationType", 
                        modeSettingsMap.getOrDefault(mode, new ModeSettings()).notificationType.name())
                );
            } catch (IllegalArgumentException e) {
                // Use default for this mode if error
                settings.notificationType = modeSettingsMap.getOrDefault(
                    mode, new ModeSettings()).notificationType;
            }
            
            settings.notificationsEnabled = Boolean.parseBoolean(
                properties.getProperty(prefix + "notificationsEnabled", 
                    String.valueOf(modeSettingsMap.getOrDefault(mode, new ModeSettings()).notificationsEnabled))
            );
            
            settings.soundAlertPath = properties.getProperty(prefix + "soundAlertPath", 
                modeSettingsMap.getOrDefault(mode, new ModeSettings()).soundAlertPath);
            
            settings.warningThresholdMinutes = Integer.parseInt(
                properties.getProperty(prefix + "warningThresholdMinutes", 
                    String.valueOf(modeSettingsMap.getOrDefault(mode, new ModeSettings()).warningThresholdMinutes))
            );
            
            try {
                settings.monitorMode = MonitorMode.valueOf(
                    properties.getProperty(prefix + "monitorMode", 
                        modeSettingsMap.getOrDefault(mode, new ModeSettings()).monitorMode.name())
                );
            } catch (IllegalArgumentException e) {
                // Use default for this mode if error
                settings.monitorMode = modeSettingsMap.getOrDefault(
                    mode, new ModeSettings()).monitorMode;
            }
            
            modeSettingsMap.put(mode, settings);
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
        properties.setProperty("userMode", userMode.name());
        
        // Save current settings to current mode before saving
        saveModeSettings(userMode);
        
        // Save mode-specific settings
        for (Map.Entry<UserMode, ModeSettings> entry : modeSettingsMap.entrySet()) {
            UserMode mode = entry.getKey();
            ModeSettings settings = entry.getValue();
            String prefix = "mode." + mode.name() + ".";
            
            properties.setProperty(prefix + "notificationType", settings.notificationType.name());
            properties.setProperty(prefix + "notificationsEnabled", String.valueOf(settings.notificationsEnabled));
            properties.setProperty(prefix + "soundAlertPath", settings.soundAlertPath);
            properties.setProperty(prefix + "warningThresholdMinutes", String.valueOf(settings.warningThresholdMinutes));
            properties.setProperty(prefix + "monitorMode", settings.monitorMode.name());
        }

        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            properties.store(fos, "ProMonitor User Settings");
            return true;
        } catch (IOException e) {
            logger.error("Lỗi khi lưu cài đặt", e);
            return false;
        }
    }
    
    /**
     * Inner class to store mode-specific settings
     */
    private static class ModeSettings {
        private NotificationType notificationType;
        private boolean notificationsEnabled;
        private String soundAlertPath;
        private int warningThresholdMinutes;
        private MonitorMode monitorMode;
        
        public ModeSettings() {
            this.notificationType = NotificationType.POPUP;
            this.notificationsEnabled = true;
            this.soundAlertPath = "default_alert.wav";
            this.warningThresholdMinutes = 5;
            this.monitorMode = MonitorMode.NORMAL;
        }
        
        public ModeSettings(NotificationType notificationType, boolean notificationsEnabled,
                          String soundAlertPath, int warningThresholdMinutes, MonitorMode monitorMode) {
            this.notificationType = notificationType;
            this.notificationsEnabled = notificationsEnabled;
            this.soundAlertPath = soundAlertPath;
            this.warningThresholdMinutes = warningThresholdMinutes;
            this.monitorMode = monitorMode;
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

    public UserMode getUserMode() {
        return userMode;
    }

    public void setUserMode(UserMode userMode) {
        this.userMode = userMode;
    }
}