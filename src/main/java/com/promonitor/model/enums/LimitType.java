package com.promonitor.model.enums;

public enum LimitType {
    DAILY("Giới hạn hàng ngày"),
    WEEKLY("Giới hạn hàng tuần"),
    SCHEDULE("Giới hạn theo lịch");

    private final String displayName;

    LimitType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
