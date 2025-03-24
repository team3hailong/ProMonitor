package com.promonitor.model;

import com.promonitor.controller.UserSettings;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


public class User {
    private String userName;
    private final UserSettings settings;
    private final LocalDate createdDate;

    public User(String userName) {
        this.userName = userName;
        this.settings = new UserSettings();
        this.settings.loadSettings();
        this.createdDate = LocalDate.now();
    }

    public boolean saveSettings() {
        return settings.saveSettings();
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

    public String getCreatedDate() {
        return createdDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    @Override
    public String toString() {
        return userName;
    }
}