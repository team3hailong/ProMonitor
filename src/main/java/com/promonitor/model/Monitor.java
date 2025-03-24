package com.promonitor.model;

import com.promonitor.controller.LimitManager;
import com.promonitor.controller.UserSettings;
import com.promonitor.model.enums.LimitType;
import com.promonitor.model.enums.MonitorMode;
import com.promonitor.model.interfaces.IReportable;
import com.promonitor.util.DataStorage;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Monitor implements IReportable {
    private static final Logger logger = LoggerFactory.getLogger(Monitor.class);

    public final Map<String, TimeTracker> timeTrackers = new ConcurrentHashMap<>();
    private final Map<ApplicationGroup, Duration> groupUsageMap = new ConcurrentHashMap<>();
    private final Map<String, DataStorage.AppUsageData> savedAppData = new HashMap<>();
    private final Set<String> blockedApplications = new HashSet<>();

    private static final int UPDATE_INTERVAL_MS = 1000;
    private static final User32 user32 = User32.INSTANCE;

    private final LimitManager limitManager;
    private final Notifier notifier;
    private final UserSettings userSettings;

    private boolean monitoring;
    private ScheduledExecutorService monitorExecutor;
    private ScheduledExecutorService blockingMonitor;
    private LocalDateTime monitoringStartTime;
    private LocalDateTime lastUpdateTime;
    private String currentDate;
    private String activeWindowId;
    private Application activeApplication;

    public Monitor(LimitManager limitManager, Notifier notifier, UserSettings userSettings) {
        this.limitManager = limitManager;
        this.notifier = notifier;
        this.userSettings = userSettings;
        loadSavedAppData();
    }

    private void loadSavedAppData() {
        try {
            List<DataStorage.AppUsageData> loadedData = DataStorage.getInstance().getTodayUsageData();
            currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

            savedAppData.clear();
            for (DataStorage.AppUsageData data : loadedData) {
                savedAppData.put(data.getName(), data);
            }

            logger.info("Đã tải dữ liệu theo dõi ứng dụng ngày {} từ file: {} ứng dụng", currentDate, savedAppData.size());
        } catch (Exception e) {
            logger.error("Lỗi khi tải dữ liệu theo dõi ứng dụng", e);
        }
    }

    public void startMonitoring() {
        if (monitoring) {
            logger.info("Hệ thống theo dõi đã đang chạy");
            return;
        }

        monitoring = true;
        monitoringStartTime = LocalDateTime.now();
        lastUpdateTime = monitoringStartTime;
        currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        logger.info("Bắt đầu theo dõi ứng dụng vào lúc: {}", monitoringStartTime);

        loadSavedAppData();
        monitorExecutor = Executors.newSingleThreadScheduledExecutor();
        monitorExecutor.scheduleAtFixedRate(this::updateActiveWindow, 0, UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS);
        monitorExecutor.scheduleAtFixedRate(this::saveAppData, 1, 5, TimeUnit.MINUTES);
    }

    public void stopMonitoring() {
        if (!monitoring) {
            return;
        }

        monitoring = false;
        logger.info("Dừng theo dõi ứng dụng");

        saveAppData();
        shutdownExecutor(monitorExecutor);

        for (TimeTracker tracker : timeTrackers.values()) {
            if (tracker.isRunning()) {
                tracker.stopTracking();
            }
        }
    }

    private void shutdownExecutor(ScheduledExecutorService executor) {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void updateActiveWindow() {
        try {
            checkAndResetForNewDay();

            HWND activeWindow = user32.GetForegroundWindow();
            if (activeWindow == null) {
                return;
            }

            int processId = getProcessId(activeWindow);
            String windowTitle = getWindowTitle(activeWindow);
            Application currentApp = new Application(windowTitle.isEmpty() ? "Unknown" : windowTitle, processId, "");

            String currentAppId = currentApp.getUniqueId();
            if (!currentAppId.equals(activeWindowId)) {
                switchActiveWindow(currentApp, currentAppId);
            }

            lastUpdateTime = LocalDateTime.now();
            checkLimits();

        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật cửa sổ đang hoạt động", e);
        }
    }

    private void checkAndResetForNewDay() {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        if (currentDate != null && !today.equals(currentDate)) {
            logger.info("Phát hiện ngày mới: {}. Thiết lập lại bộ theo dõi.", today);
            saveAppData();
            resetTrackers();
            currentDate = today;
            loadSavedAppData();
        }
    }

    private void resetTrackers() {
        for (TimeTracker tracker : timeTrackers.values()) {
            if (tracker.isRunning()) {
                tracker.stopTracking();
            }
        }
        timeTrackers.clear();
        monitoringStartTime = LocalDateTime.now();
    }

    private int getProcessId(HWND activeWindow) {
        IntByReference processIdRef = new IntByReference();
        user32.GetWindowThreadProcessId(activeWindow, processIdRef);
        return processIdRef.getValue();
    }

    private String getWindowTitle(HWND activeWindow) {
        char[] windowText = new char[512];
        user32.GetWindowText(activeWindow, windowText, 512);
        return Native.toString(windowText).trim();
    }

    private void switchActiveWindow(Application currentApp, String currentAppId) {
        if (activeWindowId != null && timeTrackers.containsKey(activeWindowId)) {
            timeTrackers.get(activeWindowId).stopTracking();
        }

        activeWindowId = currentAppId;
        activeApplication = currentApp;

        timeTrackers.computeIfAbsent(currentAppId, k -> {
            TimeTracker newTracker = new TimeTracker(currentApp);
            DataStorage.AppUsageData savedData = savedAppData.get(currentAppId);
            if (savedData != null) {
                newTracker.addSavedTime(Duration.ofMillis(savedData.getUsageTimeMillis()));
                logger.debug("Sử dụng dữ liệu đã lưu cho ứng dụng {}: {} giây", currentApp.getName(), savedData.getUsageTimeMillis() / 1000);
            }
            newTracker.startTracking();
            logger.debug("Bắt đầu theo dõi ứng dụng mới: {}", currentApp.getName());
            return newTracker;
        }).startTracking();
    }

    private void checkLimits() {
        if (activeApplication == null || !monitoring) {
            return;
        }

        try {
            updateGroupUsage();
            TimeTracker tracker = timeTrackers.get(activeApplication.getUniqueId());
            if (tracker == null) {
                return;
            }

            Duration usageTime = tracker.getTotalTime();
            boolean appLimitExceeded = limitManager.isLimitExceeded(activeApplication, usageTime);
            boolean groupLimitExceeded = limitManager.isGroupLimitExceeded(activeApplication, groupUsageMap);

            if (appLimitExceeded || groupLimitExceeded) {
                handleLimitExceeded(tracker, appLimitExceeded);
            } else {
                checkWarningThreshold(tracker);
            }

        } catch (Exception e) {
            logger.error("Lỗi khi kiểm tra giới hạn thời gian", e);
        }
    }

    private void updateGroupUsage() {
        groupUsageMap.clear();
        for (TimeTracker tracker : timeTrackers.values()) {
            Application app = tracker.getApplication();
            Duration appUsage = tracker.getTotalTime();
            limitManager.getAllLimits().entrySet().stream()
                    .filter(entry -> entry.getKey() instanceof ApplicationGroup group && group.containsApplication(app))
                    .forEach(entry -> groupUsageMap.merge((ApplicationGroup) entry.getKey(), appUsage, Duration::plus));
        }
    }

    private void handleLimitExceeded(TimeTracker tracker, boolean isAppLimit) {
        Application app = tracker.getApplication();
        String message = isAppLimit ? "Thời gian sử dụng cho " + app.getName() + " đã vượt quá giới hạn." :
                app.getName() + " thuộc nhóm đã vượt quá giới hạn thời gian sử dụng.";

        if (userSettings.getMonitorMode() == MonitorMode.STRICT) {
            message += " Ứng dụng sẽ bị tạm dừng theo chế độ nghiêm ngặt.";
            notifier.notify(message, "Đã vượt quá giới hạn thời gian", userSettings.getNotificationType());
            blockApplication(app);
            logger.info("Ứng dụng {} bị chặn do vượt quá giới hạn (chế độ nghiêm ngặt)", app.getName());
        }
        notifier.notify(message, "Đã vượt quá giới hạn thời gian", userSettings.getNotificationType());
    }

    public void blockApplication(Application app) {
        blockedApplications.add(app.getName());
        if (app.getName().contains(" - ")) {
            Arrays.stream(app.getName().split(" - "))
                    .map(String::trim)
                    .filter(trimmedPart -> !trimmedPart.isEmpty())
                    .forEach(this::addBlockedApplication);
        }

        app.terminate();
        startBlockingMonitor();
    }

    private void addBlockedApplication(String name) {
        if (name.contains("Google")) {
            name = "chrome.exe";
        }
        blockedApplications.add(name);
    }

    private void startBlockingMonitor() {
        if (blockingMonitor == null || blockingMonitor.isShutdown()) {
            blockingMonitor = Executors.newSingleThreadScheduledExecutor();
            blockingMonitor.scheduleAtFixedRate(this::checkAndBlockRestartedApps, 0, 2, TimeUnit.SECONDS);
        }
    }

    private void checkAndBlockRestartedApps() {
        blockedApplications.forEach(blockedAppName ->
                Application.findRunningApplicationsByName(blockedAppName).forEach(instance -> {
                    instance.terminate();
                    logger.info("Phát hiện và chặn ứng dụng {} đang cố mở lại", blockedAppName);
                    notifier.notify(
                            "Ứng dụng " + blockedAppName + " đã bị chặn do vượt quá giới hạn thời gian",
                            "Không thể mở lại ứng dụng",
                            userSettings.getNotificationType()
                    );
                })
        );
    }

    public void unblockApplication(Application app) {
        blockedApplications.remove(app.getName());
        logger.info("Đã bỏ chặn ứng dụng: {}", app.getName());

        if (blockedApplications.isEmpty() && blockingMonitor != null) {
            blockingMonitor.shutdown();
        }
    }

    private void checkWarningThreshold(TimeTracker tracker) {
        Application app = tracker.getApplication();
        Limit limit = limitManager.getLimit(app);

        if (limit != null && limit.getType() != LimitType.SCHEDULE) {
            Duration usageTime = tracker.getTotalTime();
            Duration remaining = limit.getRemainingTime(usageTime);

            int warningThresholdSeconds = userSettings.getWarningThresholdMinutes() * 60;

            if (remaining.getSeconds() <= warningThresholdSeconds && remaining.getSeconds() > 0 && remaining.getSeconds() % 10 == 0) {
                String message = "Cảnh báo: Thời gian sử dụng " + app.getName() +
                        " sẽ đạt đến giới hạn trong " +
                        (remaining.toMinutes() > 0 ? remaining.toMinutes() + " phút" : remaining.getSeconds() + " giây");

                notifier.notify(message, "Bạn ơi đừng nghiện nữa", userSettings.getNotificationType());
                logger.info("Đã hiển thị cảnh báo giới hạn cho {}: còn {} giây", app.getName(), remaining.getSeconds());
            }
        }
    }

    public List<Application> getAllTrackedApplications() {
        return timeTrackers.values().stream().map(TimeTracker::getApplication).collect(Collectors.toList());
    }

    public List<TimeTracker> getAllTimeTrackers() {
        return new ArrayList<>(timeTrackers.values());
    }

    public Duration getTotalComputerUsageTime() {
        return monitoringStartTime == null ? Duration.ZERO : Duration.between(monitoringStartTime, LocalDateTime.now());
    }

    public boolean isMonitoring() {
        return monitoring;
    }

    @Override
    public Map<String, Object> generateReportData() {
        return generateReportData(monitoringStartTime, lastUpdateTime != null ? lastUpdateTime : LocalDateTime.now());
    }

    public Map<String, Object> generateReportData(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> reportData = new HashMap<>();

        reportData.put("reportStartTime", startTime);
        reportData.put("reportEndTime", endTime);
        reportData.put("totalMonitoringTime", Duration.between(startTime, endTime).toString());

        Map<String, TimeTracker> combinedTrackers = new HashMap<>(timeTrackers);
        addHistoricalDataToTrackers(combinedTrackers, startTime, endTime);

        reportData.put("applicationData", generateApplicationData(combinedTrackers));
        reportData.put("groupData", generateGroupData(combinedTrackers));

        return reportData;
    }

    private void addHistoricalDataToTrackers(Map<String, TimeTracker> combinedTrackers, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            List<DataStorage.AppUsageData> historicalData = DataStorage.getInstance()
                    .getUsageDataForDateRange(dateFormat.format(Date.from(startTime.atZone(java.time.ZoneId.systemDefault()).toInstant())),
                            dateFormat.format(Date.from(endTime.atZone(java.time.ZoneId.systemDefault()).toInstant())));

            historicalData.forEach(appData -> {
                String appId = appData.getName();
                if (!timeTrackers.containsKey(appId)) {
                    Application histApp = new Application(appData.getName(), appData.getProcessId(), appData.getExecutablePath());
                    TimeTracker histTracker = new TimeTracker(histApp);
                    histTracker.addSavedTime(Duration.ofMillis(appData.getUsageTimeMillis()));
                    combinedTrackers.put(appId, histTracker);
                }
            });
        } catch (Exception e) {
            logger.error("Lỗi khi tải dữ liệu lịch sử cho báo cáo", e);
        }
    }

    private List<Map<String, Object>> generateApplicationData(Map<String, TimeTracker> combinedTrackers) {
        return combinedTrackers.values().stream().map(tracker -> {
            Application app = tracker.getApplication();
            Map<String, Object> appInfo = new HashMap<>();
            appInfo.put("name", app.getName());
            appInfo.put("processId", app.getProcessId());
            appInfo.put("totalTimeFormatted", tracker.getFormattedTotalTime());
            appInfo.put("totalTimeSeconds", tracker.getTotalTimeInSeconds());
            appInfo.put("totalTimeMinutes", tracker.getTotalTimeInMinutes());
            appInfo.put("totalTimeHours", tracker.getTotalTimeInHours());
            return appInfo;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> generateGroupData(Map<String, TimeTracker> combinedTrackers) {
        Map<ApplicationGroup, Duration> combinedGroupUsage = new HashMap<>();
        combinedTrackers.values().forEach(tracker -> {
            Application app = tracker.getApplication();
            Duration appUsage = tracker.getTotalTime();
            limitManager.getAllLimits().entrySet().stream()
                    .filter(entry -> entry.getKey() instanceof ApplicationGroup group && group.containsApplication(app))
                    .forEach(entry -> combinedGroupUsage.merge((ApplicationGroup) entry.getKey(), appUsage, Duration::plus));
        });

        return combinedGroupUsage.entrySet().stream().map(entry -> {
            ApplicationGroup group = entry.getKey();
            Duration duration = entry.getValue();
            Map<String, Object> groupInfo = new HashMap<>();
            groupInfo.put("name", group.getName());
            groupInfo.put("appCount", group.getApplicationCount());
            groupInfo.put("totalTimeSeconds", duration.getSeconds());
            groupInfo.put("totalTimeMinutes", duration.toMinutes());
            groupInfo.put("totalTimeHours", duration.toHours());
            return groupInfo;
        }).collect(Collectors.toList());
    }

    private void saveAppData() {
        try {
            List<DataStorage.AppUsageData> dataToSave = timeTrackers.values().stream().map(tracker -> {
                Application app = tracker.getApplication();
                Limit limit = limitManager.getLimit(app);
                return new DataStorage.AppUsageData(app.getName(), app.getProcessId(), app.getExecutablePath(), tracker.getFormattedTotalTime(),
                        limit != null ? limit.getType().getDisplayName() : "Không giới hạn");
            }).collect(Collectors.toList());

            DataStorage.getInstance().saveAppUsageData(dataToSave);
            logger.debug("Đã lưu dữ liệu cho {} ứng dụng", dataToSave.size());
        } catch (Exception e) {
            logger.error("Lỗi khi lưu dữ liệu theo dõi ứng dụng", e);
        }
    }

    public void cleanup() {
        saveAppData();
        stopMonitoring();
    }
}