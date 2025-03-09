package com.promonitor.model.enums;

public enum NotificationType {
    POPUP("Thông báo pop-up"),
    SOUND("Âm thanh cảnh báo"),
    TASKBAR_ICON("Nhấp nháy biểu tượng trên thanh tác vụ"),
    ALL("Tất cả các loại thông báo");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}