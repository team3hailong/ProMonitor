package com.promonitor.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.Map;

public class Schedule {
    private final Map<DayOfWeek, TimeRange> scheduledTimes;

    public Schedule() {
        this.scheduledTimes = new EnumMap<>(DayOfWeek.class);
    }

    public void setTimeForDay(DayOfWeek day, LocalTime startTime, LocalTime endTime) {
        scheduledTimes.put(day, new TimeRange(startTime, endTime));
    }

    public void removeDay(DayOfWeek day) {
        scheduledTimes.remove(day);
    }

    public boolean isCurrentTimeWithinSchedule() {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        LocalTime now = LocalTime.now();

        if (scheduledTimes.containsKey(today)) {
            TimeRange range = scheduledTimes.get(today);
            return range.contains(now);
        }

        return false;
    }

    public TimeRange getTimeForDay(DayOfWeek day) {
        return scheduledTimes.get(day);
    }

    public static class TimeRange {
        private final LocalTime startTime;
        private final LocalTime endTime;

        public TimeRange(LocalTime startTime, LocalTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public boolean contains(LocalTime time) {
            return !time.isBefore(startTime) && !time.isAfter(endTime);
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        @Override
        public String toString() {
            return startTime + " - " + endTime;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Lịch trình: ");
        for (Map.Entry<DayOfWeek, TimeRange> entry : scheduledTimes.entrySet()) {
            sb.append("\n  ")
                    .append(getDayName(entry.getKey()))
                    .append(": ")
                    .append(entry.getValue());
        }
        return sb.toString();
    }

    private String getDayName(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "Thứ Hai";
            case TUESDAY -> "Thứ Ba";
            case WEDNESDAY -> "Thứ Tư";
            case THURSDAY -> "Thứ Năm";
            case FRIDAY -> "Thứ Sáu";
            case SATURDAY -> "Thứ Bảy";
            case SUNDAY -> "Chủ Nhật";
        };
    }
}