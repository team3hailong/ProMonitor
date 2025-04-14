package com.promonitor.model.enums;

public enum ReportType {
    DAILY("Báo cáo hôm nay"),
    WEEKLY("Báo cáo 7 ngày"),
    MONTHLY("Báo cáo 30 ngày"),
    CUSTOM("Báo cáo tùy chỉnh");

    private final String displayName;

    ReportType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}