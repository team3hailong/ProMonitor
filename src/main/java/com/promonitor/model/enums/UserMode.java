package com.promonitor.model.enums;

public enum UserMode {
    DEFAULT("Mặc định", "Chế độ theo dõi thông thường với các tính năng tiêu chuẩn"),
    WORK("Công việc", "Tối ưu hóa cho theo dõi thời gian làm việc và năng suất"),
    CHILDREN("Trẻ em", "Kiểm soát chặt chẽ với tính năng giới hạn thời gian và chặn ứng dụng");

    private final String displayName;
    private final String description;

    UserMode(String displayName, String description) {
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
