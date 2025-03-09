package com.promonitor.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;

import java.util.Objects;

public class Application {
    private final StringProperty name = new SimpleStringProperty();
    private final IntegerProperty processId = new SimpleIntegerProperty();
    private final StringProperty executablePath = new SimpleStringProperty();
    private Image icon;
    private boolean isActive;

    public Application(String name, int processId) {
        this.name.set(name);
        this.processId.set(processId);
        this.isActive = true;
    }

    public Application(String name, int processId, String executablePath) {
        this(name, processId);
        this.executablePath.set(executablePath);
    }

    public Application(String name, int processId, String executablePath, Image icon) {
        this(name, processId, executablePath);
        this.icon = icon;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public int getProcessId() {
        return processId.get();
    }

    public void setProcessId(int processId) {
        this.processId.set(processId);
    }

    public IntegerProperty processIdProperty() {
        return processId;
    }

    public String getExecutablePath() {
        return executablePath.get();
    }

    public void setExecutablePath(String path) {
        this.executablePath.set(path);
    }

    public StringProperty executablePathProperty() {
        return executablePath;
    }

    public Image getIcon() {
        return icon;
    }

    public void setIcon(Image icon) {
        this.icon = icon;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Application that = (Application) o;
        return processId.get() == that.processId.get() &&
                Objects.equals(name.get(), that.name.get()) &&
                Objects.equals(executablePath.get(), that.executablePath.get());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.get(), processId.get(), executablePath.get());
    }

    @Override
    public String toString() {
        return name.get() + " (PID: " + processId.get() + ")";
    }

    public String getUniqueId() {
        return name.get() + "_" + processId.get();
    }
}