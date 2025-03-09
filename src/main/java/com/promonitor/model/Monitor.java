package com.promonitor.model;

import com.promonitor.controller.LimitManager;
import com.promonitor.controller.UserSettings;
import com.promonitor.model.enums.LimitType;
import com.promonitor.model.enums.MonitorMode;
import com.promonitor.model.interfaces.IReportable;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Monitor implements IReportable {
    private static final Logger logger = LoggerFactory.getLogger(Monitor.class);

    private final Map<String, Application> runningApplications; // ID -> Application
    private final Map<String, TimeTracker> timeTrackers; // ID -> TimeTracker
    private final Map<ApplicationGroup, Duration> groupUsageMap; // Group -> Total duration

    private final LimitManager limitManager;
    private final Notifier notifier;
    private final UserSettings userSettings;

    private boolean monitoring;
    private ScheduledExecutorService monitorExecutor;

    private LocalDateTime monitoringStartTime;
    private LocalDateTime lastUpdateTime;

    private String activeWindowId;
    private Application activeApplication;

    private static final int UPDATE_INTERVAL_MS = 1000;
    private static final User32 user32 = User32.INSTANCE;

    public Monitor(LimitManager limitManager, Notifier notifier, UserSettings userSettings) {
        this.runningApplications = new ConcurrentHashMap<>();
        this.timeTrackers = new ConcurrentHashMap<>();
        this.groupUsageMap = new ConcurrentHashMap<>();
        this.limitManager = limitManager;
        this.notifier = notifier;
        this.userSettings = userSettings;
        this.monitoring = false;
    }

    public void startMonitoring() {
        if (monitoring) {
            logger.info("Hệ thống theo dõi đã đang chạy");
            return;
        }

        monitoring = true;
        monitoringStartTime = LocalDateTime.now();
        lastUpdateTime = monitoringStartTime;

        logger.info("Bắt đầu theo dõi ứng dụng vào lúc: {}", monitoringStartTime);

        monitorExecutor = Executors.newSingleThreadScheduledExecutor();
        monitorExecutor.scheduleAtFixedRate(
                this::updateActiveWindow, 0, UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public void stopMonitoring() {
        if (!monitoring) {
            return;
        }

        monitoring = false;
        logger.info("Dừng theo dõi ứng dụng");

        if (monitorExecutor != null) {
            monitorExecutor.shutdown();
            try {
                if (!monitorExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    monitorExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                monitorExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            monitorExecutor = null;
        }

        for (TimeTracker tracker : timeTrackers.values()) {
            if (tracker.isRunning()) {
                tracker.stopTracking();
            }
        }
    }

    private void updateActiveWindow() {
        try {
            HWND activeWindow = user32.GetForegroundWindow();
            if (activeWindow == null) {
                return;
            }

            int[] processId = new int[1];
            user32.GetWindowThreadProcessId(activeWindow, processId);

            char[] windowText = new char[512];
            user32.GetWindowText(activeWindow, windowText, 512);
            String windowTitle = Native.toString(windowText).trim();
            String executablePath = "";

            Application currentApp = new Application(
                    windowTitle.isEmpty() ? "Unknown" : windowTitle,
                    processId[0],
                    executablePath
            );

            String currentAppId = currentApp.getUniqueId();

            runningApplications.put(currentAppId, currentApp);

            // Kiểm tra xem có thay đổi ứng dụng active không
            if (!currentAppId.equals(activeWindowId)) {

                if (activeWindowId != null && timeTrackers.containsKey(activeWindowId)) {
                    TimeTracker previousTracker = timeTrackers.get(activeWindowId);
                    previousTracker.updateActiveTime();
                }

                activeWindowId = currentAppId;
                activeApplication = currentApp;
                if (!timeTrackers.containsKey(currentAppId)) {
                    TimeTracker newTracker = new TimeTracker(currentApp);
                    timeTrackers.put(currentAppId, newTracker);
                    newTracker.startTracking();
                    logger.debug("Bắt đầu theo dõi ứng dụng mới: {}", currentApp.getName());
                }
            }

            lastUpdateTime = LocalDateTime.now();
            checkLimits();

        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật cửa sổ đang hoạt động", e);
        }
    }
    private void checkLimits() {
        if (activeApplication == null || !monitoring) {
            return;
        }

        try {
            updateGroupUsage();

            String activeAppId = activeApplication.getUniqueId();
            TimeTracker tracker = timeTrackers.get(activeAppId);

            if (tracker == null) {
                return;
            }

            Duration usageTime = tracker.getTotalTime();

            boolean appLimitExceeded = limitManager.isLimitExceeded(activeApplication, usageTime);

            boolean groupLimitExceeded = limitManager.isGroupLimitExceeded(activeApplication, groupUsageMap);

            // Xử lý khi vượt quá giới hạn
            if (appLimitExceeded || groupLimitExceeded) {
                handleLimitExceeded(tracker, appLimitExceeded);
            } else {
                // Kiểm tra cảnh báo trước (nếu sắp vượt quá giới hạn)
                checkWarningThreshold(tracker);
            }

        } catch (Exception e) {
            logger.error("Lỗi khi kiểm tra giới hạn thời gian", e);
        }
    }

    private void updateGroupUsage() {
        // Reset group usage
        groupUsageMap.clear();

        // Tính tổng thời gian sử dụng cho từng nhóm
        for (TimeTracker tracker : timeTrackers.values()) {
            Application app = tracker.getApplication();
            Duration appUsage = tracker.getTotalTime();

            // Duyệt qua tất cả các giới hạn để tìm các nhóm chứa ứng dụng này
            for (Map.Entry<Object, Limit> entry : limitManager.getAllLimits().entrySet()) {
                if (entry.getKey() instanceof ApplicationGroup) {
                    ApplicationGroup group = (ApplicationGroup) entry.getKey();
                    if (group.containsApplication(app)) {
                        Duration currentGroupUsage = groupUsageMap.getOrDefault(group, Duration.ZERO);
                        groupUsageMap.put(group, currentGroupUsage.plus(appUsage));
                    }
                }
            }
        }
    }

    private void handleLimitExceeded(TimeTracker tracker, boolean isAppLimit) {
        Application app = tracker.getApplication();
        String appName = app.getName();

        String message;
        if (isAppLimit) {
            message = "Thời gian sử dụng cho " + appName + " đã vượt quá giới hạn.";
        } else {
            message = appName + " thuộc nhóm đã vượt quá giới hạn thời gian sử dụng.";
        }

        if (userSettings.getMonitorMode() == MonitorMode.STRICT) {
            // Chế độ nghiêm ngặt: Hiển thị thông báo và chặn ứng dụng
            message += " Ứng dụng sẽ bị tạm dừng theo chế độ nghiêm ngặt.";
            notifier.notify(message, "Đã vượt quá giới hạn thời gian", userSettings.getNotificationType());

            // TODO: Thực hiện chặn ứng dụng (có thể cần một phương pháp platform-specific)
            // Ví dụ: minimizeApplication(app.getProcessId());
            logger.info("Ứng dụng {} bị chặn do vượt quá giới hạn (chế độ nghiêm ngặt)", appName);
        } else {
            notifier.notify(message, "Đã vượt quá giới hạn thời gian", userSettings.getNotificationType());
            logger.info("Ứng dụng {} đã vượt quá giới hạn (chế độ bình thường)", appName);
        }
    }

    private void checkWarningThreshold(TimeTracker tracker) {
        Application app = tracker.getApplication();
        Limit limit = limitManager.getLimit(app);

        if (limit != null && limit.getType() != LimitType.SCHEDULE) {
            Duration usageTime = tracker.getTotalTime();
            Duration remaining = limit.getRemainingTime(usageTime);

            int warningThresholdSeconds = userSettings.getWarningThresholdMinutes() * 60;

            if (remaining.getSeconds() <= warningThresholdSeconds && remaining.getSeconds() > 0) {
                String message = "Cảnh báo: Thời gian sử dụng " + app.getName() +
                        " sẽ đạt đến giới hạn trong " +
                        (remaining.toMinutes() > 0 ? remaining.toMinutes() + " phút" :
                                remaining.getSeconds() + " giây");

                notifier.notify(message, "Cảnh báo giới hạn thời gian", userSettings.getNotificationType());
                logger.info("Đã hiển thị cảnh báo giới hạn cho {}: còn {} giây",
                        app.getName(), remaining.getSeconds());
            }
        }
    }

    public List<Application> getAllTrackedApplications() {
        List<Application> apps = new ArrayList<>();
        for (TimeTracker tracker : timeTrackers.values()) {
            apps.add(tracker.getApplication());
        }
        return apps;
    }

    public List<TimeTracker> getAllTimeTrackers() {
        return new ArrayList<>(timeTrackers.values());
    }

    public Duration getTotalComputerUsageTime() {
        if (monitoringStartTime == null) {
            return Duration.ZERO;
        }
        return Duration.between(monitoringStartTime, LocalDateTime.now());
    }

    public boolean isMonitoring() {
        return monitoring;
    }

    @Override
    public Map<String, Object> generateReportData() {
        Map<String, Object> reportData = new HashMap<>();

        reportData.put("reportStartTime", monitoringStartTime);
        reportData.put("reportEndTime", lastUpdateTime);
        reportData.put("totalMonitoringTime",
                Duration.between(monitoringStartTime, lastUpdateTime).toString());

        List<Map<String, Object>> appData = new ArrayList<>();
        for (TimeTracker tracker : timeTrackers.values()) {
            Map<String, Object> appInfo = new HashMap<>();
            Application app = tracker.getApplication();

            appInfo.put("name", app.getName());
            appInfo.put("processId", app.getProcessId());
            appInfo.put("totalTimeFormatted", tracker.getFormattedTotalTime());
            appInfo.put("totalTimeSeconds", tracker.getTotalTimeInSeconds());
            appInfo.put("totalTimeMinutes", tracker.getTotalTimeInMinutes());
            appInfo.put("totalTimeHours", tracker.getTotalTimeInHours());

            appData.add(appInfo);
        }
        reportData.put("applicationData", appData);

        List<Map<String, Object>> groupData = new ArrayList<>();
        for (Map.Entry<ApplicationGroup, Duration> entry : groupUsageMap.entrySet()) {
            Map<String, Object> groupInfo = new HashMap<>();
            ApplicationGroup group = entry.getKey();
            Duration duration = entry.getValue();

            groupInfo.put("name", group.getName());
            groupInfo.put("appCount", group.getApplicationCount());
            groupInfo.put("totalTimeSeconds", duration.getSeconds());
            groupInfo.put("totalTimeMinutes", duration.toMinutes());
            groupInfo.put("totalTimeHours", duration.toHours());

            groupData.add(groupInfo);
        }
        reportData.put("groupData", groupData);

        return reportData;
    }

    @Override
    public LocalDateTime getReportStartTime() {
        return monitoringStartTime;
    }

    @Override
    public LocalDateTime getReportEndTime() {
        return lastUpdateTime != null ? lastUpdateTime : LocalDateTime.now();
    }

    public void cleanup() {
        stopMonitoring();
    }
}