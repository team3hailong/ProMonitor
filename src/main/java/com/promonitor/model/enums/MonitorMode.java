package com.promonitor.model.enums;

public enum MonitorMode {
    NORMAL("Bình thường", "Chỉ hiển thị cảnh báo khi vượt quá giới hạn"),
    STRICT("Nghiêm ngặt", "Chặn ứng dụng khi vượt quá giới hạn thời gian");

    private final String displayName;
    private final String description;

    MonitorMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}