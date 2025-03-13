package com.promonitor.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.time.Duration;
import java.time.LocalDateTime;

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

    public void updateActiveTime() {
        if (isRunning) {
            LocalDateTime now = LocalDateTime.now();
            Duration sessionDuration = Duration.between(lastActiveTime, now);
            totalTime.set(totalTime.get().plus(sessionDuration));

            lastActiveTime = now;
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

    public ObjectProperty<Duration> totalTimeProperty() {
        return totalTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public String toString() {
        return "TimeTracker: " + application + " - " + getFormattedTotalTime() +
                (isRunning ? " (đang chạy)" : "");
    }
}