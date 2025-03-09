package com.promonitor.model.enums;

public enum ReportType {
    DAILY("Báo cáo hàng ngày"),
    WEEKLY("Báo cáo hàng tuần"),
    CUSTOM("Báo cáo tùy chỉnh");

    private final String displayName;

    ReportType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}