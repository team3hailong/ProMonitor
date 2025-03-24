package com.promonitor.controller;

import com.promonitor.model.*;
import com.promonitor.model.enums.ReportType;
import com.promonitor.service.ReportService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    private final AtomicBoolean pendingChanges = new AtomicBoolean(false);

    private final User currentUser;
    private final Monitor monitor;
    private final LimitManager limitManager;
    private Notifier notifier;
    private DataStorage dataStorage;
    private final ReportService reportService;

    private final ObservableList<Application> applicationList = FXCollections.observableArrayList();
    private final ObservableList<ApplicationGroup> groupList = FXCollections.observableArrayList();
    private final BooleanProperty monitoringActive = new SimpleBooleanProperty(false);

    public MainController(User user) {
        this.currentUser = user;
        this.limitManager = new LimitManager();
        this.notifier = new Notifier(user.getSettings());
        this.monitor = new Monitor(limitManager, notifier, user.getSettings());
        this.reportService = new ReportService(this);

        try {
            dataStorage = DataStorage.getInstance();
            logger.info("Đã khởi tạo kho dữ liệu cho người dùng: {}", currentUser.getUserName());
            loadSavedData();
        } catch (Exception e) {
            logger.error("Không thể khởi tạo kho dữ liệu", e);
            showErrorAlert("Lỗi Khởi Tạo", "Không thể khởi tạo kho dữ liệu: " + e.getMessage());
        }

        startAutoSave();
    }

    private void startAutoSave() {
        Thread autoSaveThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                if (pendingChanges.getAndSet(false)) {
                    saveData();
                }

            }
        });
        autoSaveThread.setDaemon(true);
        autoSaveThread.start();
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

    public void setLimit(Object target, Limit limit) {
        if (target instanceof Application app) {
            limitManager.setLimit(app, limit);
        } else if (target instanceof ApplicationGroup group) {
            limitManager.setLimit(group, limit);
        }
        markChanged();
        logger.info("Đã đặt giới hạn thành công");
    }

    public ApplicationGroup createGroup(String name) {
        ApplicationGroup newGroup = new ApplicationGroup(name);
        groupList.add(newGroup);
        markChanged();
        logger.info("Đã tạo nhóm mới: {}", name);
        return newGroup;
    }

    public void deleteGroup(ApplicationGroup group) {
        if (groupList.remove(group)) {
            limitManager.removeLimit(group);
            markChanged();
            logger.info("Đã xóa nhóm: {}", group.getName());
        }
    }

    public void updateGroup(ApplicationGroup group, Application app, boolean add) {
        boolean changed = add ? group.addApplication(app) : group.removeApplication(app);
        if (changed) {
            markChanged();
            logger.info("{} ứng dụng {} {} nhóm {}",
                    add ? "Đã thêm" : "Đã xóa", app.getName(),
                    add ? "vào" : "khỏi", group.getName());
        }
    }

    // Report generation methods
    public Report createReport(ReportType reportType) {
        return reportService.createReport(reportType);
    }

    public Report createCustomReport(LocalDate startDate, LocalDate endDate) {
        return reportService.createCustomReport(startDate, endDate);
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

    private void markChanged() {
        pendingChanges.set(true);
    }

    // Getters for UI components
    public ObservableList<Application> getApplications() {
        List<Application> currentApps = monitor.getAllTrackedApplications();
        applicationList.setAll(currentApps);
        return applicationList;
    }

    public ObservableList<ApplicationGroup> getGroups() {
        return groupList;
    }

    public Limit getLimit(Object target) {
        return (target instanceof Application) ?
                limitManager.getLimit((Application)target) :
                limitManager.getLimit((ApplicationGroup)target);
    }

    public void setMainStage(Stage stage) {
        notifier.setMainStage(stage);
    }

    public boolean confirmExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận thoát");
        alert.setHeaderText("Bạn có chắc chắn muốn thoát?");
        alert.setContentText("Tất cả dữ liệu chưa lưu sẽ bị mất.");
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    public void shutdownApp() {
        stopMonitoring();
        saveData();
        notifier.cleanup();
        monitor.cleanup();
        logger.info("Ứng dụng đã đóng thành công");
    }

    public boolean updateUserSettings(UserSettings settings) {
        UserSettings currentSettings = currentUser.getSettings();
        currentSettings.setNotificationType(settings.getNotificationType());
        currentSettings.setNotificationsEnabled(settings.isNotificationsEnabled());
        currentSettings.setSoundAlertPath(settings.getSoundAlertPath());
        currentSettings.setWarningThresholdMinutes(settings.getWarningThresholdMinutes());
        currentSettings.setStartAtLogin(settings.isStartAtLogin());
        currentSettings.setMinimizeToTray(settings.isMinimizeToTray());
        currentSettings.setAutoStartMonitoring(settings.isAutoStartMonitoring());
        currentSettings.setMonitorMode(settings.getMonitorMode());

        boolean saved = currentUser.saveSettings();
        logger.info(saved ? "Đã cập nhật và lưu cài đặt người dùng" : "Không thể lưu cài đặt người dùng");
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

    public void updateAlertSound(String soundPath){
        notifier.updateAlertSound(soundPath);
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

    public List<TimeTracker> getAllTimeTrackers() {
        return monitor.getAllTimeTrackers();
    }

    public DataStorage getDataStorage() {
        return dataStorage;
    }

    public LimitManager getLimitManager() {
        return limitManager;
    }

    public Notifier getNotifier() {
        return notifier;
    }
}