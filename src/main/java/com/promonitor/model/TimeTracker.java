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
    private LocalDateTime lastActiveTime; // Thời điểm cuối cùng ứng dụng được active

    public TimeTracker(Application application) {
        this.application = application;
        this.totalTime.set(Duration.ZERO);
        this.isRunning = false;
    }

    /**
     * Bắt đầu theo dõi thời gian
     */
    public void startTracking() {
        if (!isRunning) {
            startTime = LocalDateTime.now();
            lastActiveTime = startTime;
            isRunning = true;
        }
    }

    /**
     * Dừng theo dõi thời gian
     */
    public void stopTracking() {
        if (isRunning) {
            endTime = LocalDateTime.now();
            updateTotalTime();
            isRunning = false;
        }
    }

    /**
     * Ghi nhận thời điểm ứng dụng được kích hoạt
     */
    public void updateActiveTime() {
        if (isRunning) {
            // Cập nhật thời gian tích lũy từ lần active trước đến giờ
            LocalDateTime now = LocalDateTime.now();
            Duration sessionDuration = Duration.between(lastActiveTime, now);
            totalTime.set(totalTime.get().plus(sessionDuration));

            // Cập nhật thời điểm active mới
            lastActiveTime = now;
        }
    }

    /**
     * Cập nhật tổng thời gian sử dụng
     */
    private void updateTotalTime() {
        if (startTime != null && endTime != null) {
            Duration sessionDuration = Duration.between(lastActiveTime, endTime);
            totalTime.set(totalTime.get().plus(sessionDuration));
        }
    }

    /**
     * Tính toán tổng thời gian sử dụng
     * @return Tổng thời gian sử dụng
     */
    public Duration getTotalTime() {
        if (isRunning) {
            LocalDateTime now = LocalDateTime.now();
            Duration currentSessionTime = Duration.between(lastActiveTime, now);
            return totalTime.get().plus(currentSessionTime);
        }
        return totalTime.get();
    }

    /**
     * Lấy thời gian sử dụng theo giây
     * @return Số giây sử dụng
     */
    public long getTotalTimeInSeconds() {
        return getTotalTime().getSeconds();
    }

    /**
     * Lấy thời gian sử dụng theo phút
     * @return Số phút sử dụng
     */
    public long getTotalTimeInMinutes() {
        return getTotalTime().toMinutes();
    }

    /**
     * Lấy thời gian sử dụng theo giờ
     * @return Số giờ sử dụng
     */
    public long getTotalTimeInHours() {
        return getTotalTime().toHours();
    }

    /**
     * Định dạng thời gian sử dụng thành chuỗi dễ đọc (HH:MM:SS)
     * @return Chuỗi thời gian đã định dạng
     */
    public String getFormattedTotalTime() {
        Duration duration = getTotalTime();
        long hours = duration.toHours();
        int minutes = duration.toMinutesPart();
        int seconds = duration.toSecondsPart();

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    // Getter
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