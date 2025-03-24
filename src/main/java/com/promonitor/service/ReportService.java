package com.promonitor.service;

import com.promonitor.controller.MainController;
import com.promonitor.model.*;
import com.promonitor.model.enums.ReportType;
import com.promonitor.util.DataStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ReportService {
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    private final MainController mainController;

    public ReportService(MainController mainController) {
        this.mainController = mainController;
    }

    public Report createReport(ReportType reportType) {
        Report report = new Report(reportType, mainController.getCurrentUser());

        LocalDate startDate;
        LocalDate endDate = LocalDate.now();

        startDate = switch (reportType) {
            case WEEKLY -> LocalDate.now().minusDays(6); // Last 7 days including today
            case MONTHLY -> LocalDate.now().minusDays(29);
            default -> LocalDate.now();
        };

        List<TimeTracker> historicalData = getHistoricalTimeTrackers(startDate, endDate);
        report.setData(new ArrayList<>(historicalData));
        report.setDateRange(startDate, endDate);
        logger.info("Đã tạo báo cáo loại: {}", reportType.getDisplayName());
        return report;
    }

    public Report createCustomReport(LocalDate startDate, LocalDate endDate) {
        Report report = new Report(ReportType.CUSTOM, mainController.getCurrentUser());
        List<TimeTracker> historicalData = getHistoricalTimeTrackers(startDate, endDate);
        report.setData(new ArrayList<>(historicalData));
        report.setDateRange(startDate, endDate);
        logger.info("Đã tạo báo cáo tùy chỉnh từ {} đến {}", startDate, endDate);
        return report;
    }

    public List<TimeTracker> getHistoricalTimeTrackers(LocalDate startDate, LocalDate endDate) {
        List<TimeTracker> result = new ArrayList<>();

        try {
            DataStorage dataStorage = mainController.getDataStorage();
            String startDateStr = startDate.toString();
            String endDateStr = endDate.toString();

            List<DataStorage.AppUsageData> historicalData =
                    dataStorage.getUsageDataForDateRange(startDateStr, endDateStr);

            Set<String> currentAppIds = mainController.getAllTimeTrackers().stream()
                    .map(tt -> tt.getApplication().getUniqueId())
                    .collect(Collectors.toSet());

            for (DataStorage.AppUsageData appData : historicalData) {
                String appId = appData.getName() + "_" + appData.getProcessId();
                if (currentAppIds.contains(appId)) {
                    continue;
                }

                Application app = new Application(
                        appData.getName(),
                        appData.getProcessId(),
                        appData.getExecutablePath()
                );

                TimeTracker tracker = new TimeTracker(app);
                tracker.addSavedTime(Duration.ofMillis(appData.getUsageTimeMillis()));

                if (appData.getDate() != null) {
                    tracker.setStartTimeForDate(LocalDate.parse(appData.getDate()));
                }

                result.add(tracker);
                currentAppIds.add(appId);
            }

            logger.debug("Loaded {} historical time trackers from storage", result.size());
        } catch (Exception e) {
            logger.error("Error loading historical time trackers", e);
        }

        return result;
    }
}