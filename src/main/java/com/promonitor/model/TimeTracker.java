package com.promonitor.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class TimeTracker {
    private final Application application;
    private final ObjectProperty<Duration> totalTime = new SimpleObjectProperty<>(Duration.ZERO);
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean isRunning;
    private LocalDateTime lastActiveTime;

    public TimeTracker(Application application) {
        this.application = application;
        this.totalTime.set(Duration.ZERO);
        this.isRunning = false;
    }

    public void startTracking() {
        if (!isRunning) {
            startTime = LocalDateTime.now();
            lastActiveTime = startTime;
            isRunning = true;
        }
    }

    public void stopTracking() {
        if (isRunning) {
            endTime = LocalDateTime.now();
            updateTotalTime();
            isRunning = false;
        }
    }

    private void updateTotalTime() {
        if (startTime != null && endTime != null) {
            Duration sessionDuration = Duration.between(lastActiveTime, endTime);
            totalTime.set(totalTime.get().plus(sessionDuration));
        }
    }

    public Duration getTotalTime() {
        if (isRunning) {
            LocalDateTime now = LocalDateTime.now();
            Duration currentSessionTime = Duration.between(lastActiveTime, now);
            return totalTime.get().plus(currentSessionTime);
        }
        return totalTime.get();
    }

    public long getTotalTimeInSeconds() {
        return getTotalTime().getSeconds();
    }

    public long getTotalTimeInMinutes() {
        return getTotalTime().toMinutes();
    }

    public long getTotalTimeInHours() {
        return getTotalTime().toHours();
    }

    public String getFormattedTotalTime() {
        Duration duration = getTotalTime();
        long hours = duration.toHours();
        int minutes = duration.toMinutesPart();
        int seconds = duration.toSecondsPart();

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
    
    public Application getApplication() {
        return application;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void addSavedTime(Duration savedTime) {
        if (savedTime != null && !savedTime.isZero()) {
            totalTime.set(totalTime.get().plus(savedTime));
        }
    }

    public void setStartTimeForDate(LocalDate date) {
        this.startTime = LocalDateTime.of(date, LocalTime.MIDNIGHT);
        this.endTime = LocalDateTime.of(date, LocalTime.MAX);
    }

    @Override
    public String toString() {
        return "TimeTracker: " + application + " - " + getFormattedTotalTime() +
                (isRunning ? " (đang chạy)" : "");
    }
}