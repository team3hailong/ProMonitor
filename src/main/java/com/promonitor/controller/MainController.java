package com.promonitor.controller;

import com.promonitor.model.*;
import com.promonitor.model.enums.ReportType;
import com.promonitor.util.DataStorage;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private final User currentUser;
    private final Monitor monitor;
    private final LimitManager limitManager;
    private final Notifier notifier;
    private DataStorage dataStorage;
    private final LocalDateTime currentTime;

    private final ObservableList<Application> applicationList = FXCollections.observableArrayList();
    private final ObservableList<ApplicationGroup> groupList = FXCollections.observableArrayList();
    private final BooleanProperty monitoringActive = new SimpleBooleanProperty(false);

    public MainController(User user, LocalDateTime currentTime) {
        this.currentUser = user;
        this.currentTime = currentTime;
        this.limitManager = new LimitManager();
        this.notifier = new Notifier(user.getSettings());
        this.monitor = new Monitor(limitManager, notifier, user.getSettings());

        initializeDataStorage();
        loadSavedData();
    }

    private void initializeDataStorage() {
        try {
            dataStorage = new DataStorage(currentUser.getUserId());
            logger.info("Đã khởi tạo kho dữ liệu cho người dùng: {}", currentUser.getUserName());
        } catch (Exception e) {
            logger.error("Không thể khởi tạo kho dữ liệu", e);
            showErrorAlert("Lỗi Khởi Tạo", "Không thể khởi tạo kho dữ liệu: " + e.getMessage());
        }
    }

    private void loadSavedData() {
        try {
            List<ApplicationGroup> savedGroups = dataStorage.loadApplicationGroups();
            if (savedGroups != null && !savedGroups.isEmpty()) {
                groupList.addAll(savedGroups);
                logger.info("Đã tải {} nhóm ứng dụng", savedGroups.size());
            }

            dataStorage.loadLimits(limitManager);
            logger.info("Đã tải các giới hạn thời gian");

        } catch (Exception e) {
            logger.error("Lỗi khi tải dữ liệu đã lưu", e);
            showErrorAlert("Lỗi Tải Dữ Liệu", "Không thể tải dữ liệu đã lưu: " + e.getMessage());
        }
    }

    public void startMonitoring() {
        if (!monitor.isMonitoring()) {
            monitor.startMonitoring();
            monitoringActive.set(true);
            logger.info("Đã bắt đầu theo dõi ứng dụng");
        }
    }

    public void stopMonitoring() {
        if (monitor.isMonitoring()) {
            monitor.stopMonitoring();
            monitoringActive.set(false);
            logger.info("Đã dừng theo dõi ứng dụng");
        }
    }

    public void setLimit(Application app, Limit limit) {
        limitManager.setLimit(app, limit);
        saveData();
        logger.info("Đã đặt giới hạn cho ứng dụng: {}", app.getName());
    }

    public void setLimit(ApplicationGroup group, Limit limit) {
        limitManager.setLimit(group, limit);
        saveData();
        logger.info("Đã đặt giới hạn cho nhóm: {}", group.getName());
    }

    public ApplicationGroup createGroup(String name) {
        ApplicationGroup newGroup = new ApplicationGroup(name);
        groupList.add(newGroup);
        saveData();
        logger.info("Đã tạo nhóm mới: {}", name);
        return newGroup;
    }

    public void deleteGroup(ApplicationGroup group) {
        boolean removed = groupList.remove(group);
        if (removed) {
            limitManager.removeLimit(group);
            saveData();
            logger.info("Đã xóa nhóm: {}", group.getName());
        }
    }

    public void addToGroup(ApplicationGroup group, Application app) {
        boolean added = group.addApplication(app);
        if (added) {
            saveData();
            logger.info("Đã thêm ứng dụng {} vào nhóm {}", app.getName(), group.getName());
        }
    }

    public void removeFromGroup(ApplicationGroup group, Application app) {
        boolean removed = group.removeApplication(app);
        if (removed) {
            saveData();
            logger.info("Đã xóa ứng dụng {} khỏi nhóm {}", app.getName(), group.getName());
        }
    }

    public Report createReport(ReportType reportType) {
        Report report = new Report(reportType, currentUser);
        report.setData(monitor.getAllTimeTrackers());
        logger.info("Đã tạo báo cáo loại: {}", reportType.getDisplayName());
        return report;
    }

    public Report createCustomReport(LocalDate startDate, LocalDate endDate) {
        Report report = new Report(ReportType.CUSTOM, currentUser);
        report.setDateRange(startDate, endDate);
        report.setData(monitor.getAllTimeTrackers());
        logger.info("Đã tạo báo cáo tùy chỉnh từ {} đến {}", startDate, endDate);
        return report;
    }

    public void saveData() {
        try {
            dataStorage.saveApplicationGroups(new ArrayList<>(groupList));

            dataStorage.saveLimits(limitManager.getAllLimits());

            currentUser.saveSettings();

            logger.info("Đã lưu tất cả dữ liệu");
        } catch (Exception e) {
            logger.error("Lỗi khi lưu dữ liệu", e);
            showErrorAlert("Lỗi Lưu Dữ Liệu", "Không thể lưu dữ liệu: " + e.getMessage());
        }
    }

    public ObservableList<Application> getApplications() {
        List<Application> currentApps = monitor.getAllTrackedApplications();
        applicationList.setAll(currentApps);
        return applicationList;
    }

    public ObservableList<ApplicationGroup> getGroups() {
        return groupList;
    }

    public Limit getLimit(Application app) {
        return limitManager.getLimit(app);
    }

    public Limit getLimit(ApplicationGroup group) {
        return limitManager.getLimit(group);
    }

    public void setMainStage(Stage stage) {
        notifier.setMainStage(stage);
    }

    public boolean confirmExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận thoát");
        alert.setHeaderText("Bạn có chắc chắn muốn thoát?");
        alert.setContentText("Tất cả dữ liệu chưa lưu sẽ bị mất.");

        Optional<ButtonType> result = alert.showAndWait();
        return result.orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    public void shutdownApp() {
        stopMonitoring();
        saveData();
        notifier.cleanup();
        monitor.cleanup();
        logger.info("Ứng dụng đã đóng thành công");
    }

    public BooleanProperty monitoringActiveProperty() {
        return monitoringActive;
    }

    public Duration getTotalComputerUsageTime() {
        return monitor.getTotalComputerUsageTime();
    }

    public User getCurrentUser() {
        return currentUser;
    }
    public Duration getApplicationUsageTime(Application app) {
        for (TimeTracker tracker : monitor.getAllTimeTrackers()) {
            if (tracker.getApplication().equals(app)) {
                return tracker.getTotalTime();
            }
        }
        return Duration.ZERO;
    }

    public List<TimeTracker> getAllTimeTrackers() {
        return monitor.getAllTimeTrackers();
    }

    public List<TimeTracker> getTopApplications(int count) {
        return monitor.getAllTimeTrackers().stream()
                .sorted((t1, t2) -> -Long.compare(t1.getTotalTimeInSeconds(), t2.getTotalTimeInSeconds()))
                .limit(count)
                .collect(Collectors.toList());
    }

    public boolean updateUserSettings(UserSettings settings) {
        currentUser.getSettings().setNotificationType(settings.getNotificationType());
        currentUser.getSettings().setNotificationsEnabled(settings.isNotificationsEnabled());
        currentUser.getSettings().setSoundAlertPath(settings.getSoundAlertPath());
        currentUser.getSettings().setWarningThresholdMinutes(settings.getWarningThresholdMinutes());
        currentUser.getSettings().setStartAtLogin(settings.isStartAtLogin());
        currentUser.getSettings().setMinimizeToTray(settings.isMinimizeToTray());
        currentUser.getSettings().setAutoStartMonitoring(settings.isAutoStartMonitoring());
        currentUser.getSettings().setMonitorMode(settings.getMonitorMode());

        boolean saved = currentUser.saveSettings();
        if (saved) {
            logger.info("Đã cập nhật và lưu cài đặt người dùng");
        } else {
            logger.error("Không thể lưu cài đặt người dùng");
        }
        return saved;
    }

    private void showErrorAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public LocalDateTime getCurrentTime() {
        return currentTime;
    }

    public LimitManager getLimitManager() {
        return limitManager;
    }

}