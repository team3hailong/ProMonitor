package com.promonitor.model;

import com.promonitor.model.enums.LimitType;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Limit {
    private final LimitType type;
    private final Duration value;
    private Schedule schedule;
    private LocalDateTime lastReset;

    public Limit(LimitType type, Duration value) {
        this.type = type;
        this.value = value;
        this.lastReset = LocalDateTime.now();

        if (type == LimitType.SCHEDULE) {
            this.schedule = new Schedule();
        }
    }

    public Limit(LimitType type, long hours, long minutes) {
        this(type, Duration.ofHours(hours).plusMinutes(minutes));
    }

    public boolean isExceeded(Duration usageTime) {
        if (type == LimitType.SCHEDULE) {
            return !schedule.isCurrentTimeWithinSchedule();
        }

        return usageTime.compareTo(value) > 0;
    }

    public boolean needsReset() {
        LocalDateTime now = LocalDateTime.now();

        switch (type) {
            case DAILY:
                return !now.toLocalDate().equals(lastReset.toLocalDate());
            case WEEKLY:
                LocalDate nowDate = now.toLocalDate();
                LocalDate lastResetDate = lastReset.toLocalDate();
                LocalDate startOfWeekNow = nowDate.minusDays(nowDate.getDayOfWeek().getValue() - 1);
                LocalDate startOfWeekLastReset = lastResetDate.minusDays(lastResetDate.getDayOfWeek().getValue() - 1);
                return !startOfWeekNow.equals(startOfWeekLastReset);
            default:
                return false;
        }
    }

    public void reset() {
        lastReset = LocalDateTime.now();
    }

    public Duration getRemainingTime(Duration usageTime) {
        if (type == LimitType.SCHEDULE) {
            return Duration.ZERO;
        }

        if (usageTime.compareTo(value) >= 0) {
            return Duration.ZERO;
        }

        return value.minus(usageTime);
    }

    public LimitType getType() {
        return type;
    }

    public Duration getValue() {
        return value;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    @Override
    public String toString() {
        if (type == LimitType.SCHEDULE) {
            return "Giới hạn lịch trình: " + schedule;
        } else {
            long hours = value.toHours();
            long minutes = value.toMinutesPart();
            return type.getDisplayName() + ": " + hours + "h " + minutes + "m";
        }
    }
}