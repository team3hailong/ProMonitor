package com.promonitor.model;

import com.promonitor.controller.UserSettings;

import java.time.LocalDate;
import java.util.UUID;


public class User {
    private final String userId;
    private String userName;
    private final UserSettings settings;
    private final LocalDate createdDate;

    public User(String userName) {
        this.userId = UUID.randomUUID().toString();
        this.userName = userName;
        this.settings = new UserSettings();
        this.settings.loadSettings();
        this.createdDate = LocalDate.now();
    }

    /**
     * Lưu cài đặt người dùng
     * @return true nếu lưu thành công
     */
    public boolean saveSettings() {
        return settings.saveSettings();
    }

    // Getters và setters
    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public UserSettings getSettings() {
        return settings;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    @Override
    public String toString() {
        return userName;
    }
}