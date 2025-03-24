package com.promonitor.model;

import com.promonitor.model.enums.ReportType;
import com.promonitor.model.interfaces.IReportable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Report implements IReportable {
    private static final Logger logger = LoggerFactory.getLogger(Report.class);

    private final ReportType reportType;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<TimeTracker> data;
    private Map<String, Object> reportData;
    private final User user;

    public Report(ReportType reportType, User user) {
        this.reportType = reportType;
        this.user = user;
        this.data = new ArrayList<>();
        this.reportData = new HashMap<>();

        switch (reportType) {
            case DAILY:
                this.startDate = LocalDate.now();
                this.endDate = LocalDate.now();
                break;
            case WEEKLY:
                LocalDate today = LocalDate.now();
                this.startDate = today.minusDays(today.getDayOfWeek().getValue() - 1); // Ngày đầu tiên của tuần
                this.endDate = today;
                break;
            case CUSTOM:
                break;
        }
    }

    public void setDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu không thể sau ngày kết thúc");
        }
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void setData(List<TimeTracker> data) {
        this.data = new ArrayList<>(data);
    }

    public boolean generateReport() {
        if (data.isEmpty()) {
            logger.warn("Không thể tạo báo cáo: Không có dữ liệu");
            return false;
        }

        try {
            reportData = generateReportData();
            logger.info("Đã tạo báo cáo loại: {}, thời gian: {} đến {}",
                    reportType.getDisplayName(), startDate, endDate);
            return true;
        } catch (Exception e) {
            logger.error("Lỗi khi tạo báo cáo", e);
            return false;
        }
    }

    public ObservableList<PieChart.Data> generateChartData() {
        if (reportData.isEmpty()) {
            generateReport();
        }

        List<Map<String, Object>> appUsage = (List<Map<String, Object>>) reportData.get("appUsageData");

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        for (Map<String, Object> app : appUsage) {
            String appName = (String) app.get("appName");
            long minutes = (Long) app.get("usageMinutes");

            if (minutes >= 1) {
                pieChartData.add(new PieChart.Data(appName, minutes));
            }
        }

        return pieChartData;
    }

    public List<String> generateSuggestions() {
        List<String> suggestions = new ArrayList<>();

        if (reportData.isEmpty()) {
            generateReport();
        }

        List<Map<String, Object>> appUsage = (List<Map<String, Object>>) reportData.get("appUsageData");

        long totalMinutes = (Long) reportData.get("totalUsageMinutes");

        List<Map<String, Object>> topApps = appUsage.stream()
                .sorted((a, b) -> Long.compare((Long)b.get("usageMinutes"), (Long)a.get("usageMinutes")))
                .limit(3)
                .toList();

        if (!topApps.isEmpty()) {
            Map<String, Object> topApp = topApps.get(0);
            String appName = (String) topApp.get("appName");
            long minutes = (Long) topApp.get("usageMinutes");
            double percentage = (double) minutes / totalMinutes * 100;

            if (percentage > 50) {
                suggestions.add("Bạn dành hơn " + String.format("%.1f", percentage) +
                        "% thời gian của mình cho " + appName + ". Hãy cân nhắc đa dạng hóa hoạt động của bạn.");
            }

            if (topApps.size() >= 2) {
                Map<String, Object> secondApp = topApps.get(1);
                long secondAppMinutes = (Long) secondApp.get("usageMinutes");

                if (minutes > secondAppMinutes * 3) {
                    suggestions.add("Thời gian sử dụng " + appName + " cao hơn nhiều so với các ứng dụng khác. " +
                            "Điều này có thể ảnh hưởng đến sự cân bằng hoạt động của bạn.");
                }
            }
        }

        if (totalMinutes > 240) {
            suggestions.add("Thời gian sử dụng máy tính hàng ngày của bạn vượt quá 4 giờ. Hãy nhớ nghỉ ngơi định kỳ.");
        }

        if (totalMinutes > 120) {
            suggestions.add("Nên tránh sử dụng máy tính liên tục quá 2 giờ. Hãy nghỉ ngơi ít nhất 15 phút giữa các phiên làm việc.");

        }
        if(!suggestions.isEmpty()) {
            suggestions.add("Danh sách các quy tắc nên được áp dụng:");
            suggestions.add("Quy tắc 20-20-20: Cứ mỗi 20 phút, nhìn vào một vật ở khoảng cách 20 feet trong 20 giây.");
            suggestions.add("Quy tắc 70-30: 70% cho công việc/học tập, 30% cho giải trí.");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("Thời gian sử dụng máy tính của bạn có vẻ cân đối. Tốt lắm!");
            suggestions.add("Hãy tiếp tục theo dõi các mẫu sử dụng của bạn để đạt hiệu suất tối ưu.");
        }

        return suggestions;
    }

    public String exportReport(String format) {
        if (reportData.isEmpty()) {
            generateReport();
        }

        try {
            String fileName = "ProMonitor_" + reportType.name() + "_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            if (format.equalsIgnoreCase("PDF")) {
                return exportToPDF(fileName);
            } else if (format.equalsIgnoreCase("CSV")) {
                return exportToCSV(fileName);
            } else {
                throw new IllegalArgumentException("Định dạng không được hỗ trợ: " + format);
            }
        } catch (Exception e) {
            logger.error("Lỗi khi xuất báo cáo", e);
            return null;
        }
    }

    @Override
    public Map<String, Object> generateReportData() {
        Map<String, Object> reportData = new HashMap<>();

        // Lưu thông tin cơ bản của báo cáo
        reportData.put("reportType", reportType);
        reportData.put("startDate", startDate);
        reportData.put("endDate", endDate);
        reportData.put("username", user.getUserName());

        List<TimeTracker> filteredData = data.stream()
                .filter(tt -> {
                    // Handle null start time (shouldn't happen but just in case)
                    if (tt.getStartTime() == null) {
                        return false;
                    }
                    
                    LocalDate trackDate = tt.getStartTime().toLocalDate();
                    return !trackDate.isBefore(startDate) && !trackDate.isAfter(endDate);
                })
                .toList();

        Duration totalUsage = Duration.ZERO;
        for (TimeTracker tt : filteredData) {
            totalUsage = totalUsage.plus(tt.getTotalTime());
        }

        reportData.put("totalUsageTime", formatDuration(totalUsage));
        reportData.put("totalUsageMinutes", totalUsage.toMinutes());

        Map<String, Duration> appUsageMap = new HashMap<>();
        for (TimeTracker tt : filteredData) {
            String appName = tt.getApplication().getName();
            Duration appTime = tt.getTotalTime();

            if (appUsageMap.containsKey(appName)) {
                appUsageMap.put(appName, appUsageMap.get(appName).plus(appTime));
            } else {
                appUsageMap.put(appName, appTime);
            }
        }

        List<Map<String, Object>> appUsageData = new ArrayList<>();
        for (Map.Entry<String, Duration> entry : appUsageMap.entrySet()) {
            Map<String, Object> appData = new HashMap<>();
            appData.put("appName", entry.getKey());
            appData.put("usageTime", formatDuration(entry.getValue()));
            appData.put("usageMinutes", entry.getValue().toMinutes());
            appData.put("usageHours", entry.getValue().toHours());
            appUsageData.add(appData);
        }

        appUsageData.sort((a, b) -> Long.compare((Long)b.get("usageMinutes"), (Long)a.get("usageMinutes")));

        reportData.put("appUsageData", appUsageData);

        List<Map<String, Object>> dailyBreakdown = generateDailyBreakdown(filteredData);
        reportData.put("dailyBreakdown", dailyBreakdown);

        reportData.put("generatedAt", LocalDateTime.now());

        return reportData;
    }
    
    private List<Map<String, Object>> generateDailyBreakdown(List<TimeTracker> trackers) {
        Map<LocalDate, Map<String, Duration>> dailyData = new HashMap<>();
        
        // Group data by date and app
        for (TimeTracker tracker : trackers) {
            if (tracker.getStartTime() == null) continue;
            
            LocalDate date = tracker.getStartTime().toLocalDate();
            String appName = tracker.getApplication().getName();
            Duration duration = tracker.getTotalTime();
            
            dailyData.computeIfAbsent(date, k -> new HashMap<>());
                Map<String, Duration> appMap = dailyData.get(date);
            
                appMap.put(appName, appMap.getOrDefault(appName, Duration.ZERO).plus(duration));
            }
        
        // Convert to list format
        List<Map<String, Object>> result = new ArrayList<>();
        for (LocalDate date : dailyData.keySet().stream().sorted().toList()) {
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toString());
            
            Map<String, Duration> appMap = dailyData.get(date);
            Duration totalForDay = Duration.ZERO;
            for (Duration d : appMap.values()) {
                totalForDay = totalForDay.plus(d);
            }
            dayData.put("totalTime", formatDuration(totalForDay));
            
            List<Map<String, Object>> appList = new ArrayList<>();
            for (Map.Entry<String, Duration> entry : appMap.entrySet()) {
                Map<String, Object> appData = new HashMap<>();
                appData.put("appName", entry.getKey());
                appData.put("usageTime", formatDuration(entry.getValue()));
                appData.put("usageMinutes", entry.getValue().toMinutes());
                appList.add(appData);
            }
            
            // Sort apps by usage time
            appList.sort((a, b) -> Long.compare(
                (Long) b.get("usageMinutes"), 
                (Long) a.get("usageMinutes")
            ));
            
            dayData.put("apps", appList);
            result.add(dayData);
        }
        
        return result;
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        int minutes = duration.toMinutesPart();

        return String.format("%d giờ, %d phút", hours, minutes);
    }

    private String exportToPDF(String fileName) throws Exception {
        String filePath = fileName + ".pdf";

        URL boldFontUrl = Report.class.getResource("/fonts/timesbi.ttf");
        URL normalFontUrl = Report.class.getResource("/fonts/timesi.ttf");

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            PDType0Font boldFont = PDType0Font.load(document, new File(Objects.requireNonNull(boldFontUrl).toURI()));
            PDType0Font normalFont = PDType0Font.load(document, new File(Objects.requireNonNull(normalFontUrl).toURI()));

            // Tiêu đề
            contentStream.beginText();
            contentStream.setFont(boldFont, 16);
            contentStream.newLineAtOffset(50, 750);
            contentStream.showText("Báo Cáo Sử Dụng ProMonitor - " + reportType.getDisplayName());
            contentStream.endText();

            // Khoảng thời gian
            contentStream.beginText();
            contentStream.setFont(normalFont, 12);
            contentStream.newLineAtOffset(50, 720);
            contentStream.showText("Khoảng thời gian: " + startDate + " đến " + endDate);
            contentStream.endText();

            // Người dùng
            contentStream.beginText();
            contentStream.setFont(normalFont, 12);
            contentStream.newLineAtOffset(50, 700);
            contentStream.showText("Người dùng: " + user.getUserName());
            contentStream.endText();

            // Tổng thời gian sử dụng
            contentStream.beginText();
            contentStream.setFont(boldFont, 12);
            contentStream.newLineAtOffset(50, 670);
            contentStream.showText("Tổng thời gian sử dụng: " + reportData.get("totalUsageTime"));
            contentStream.endText();

            // Thời gian sử dụng ứng dụng
            contentStream.beginText();
            contentStream.setFont(boldFont, 12);
            contentStream.newLineAtOffset(50, 640);
            contentStream.showText("Thời gian sử dụng ứng dụng:");
            contentStream.endText();

            List<Map<String, Object>> appUsage = (List<Map<String, Object>>) reportData.get("appUsageData");

            float y = 620;
            for (Map<String, Object> app : appUsage) {
                contentStream.beginText();
                contentStream.setFont(normalFont, 10);
                contentStream.newLineAtOffset(70, y);
                contentStream.showText("• " + app.get("appName") + ": " + app.get("usageTime"));
                contentStream.endText();
                y -= 20;

                if (y < 100) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    y = 750;
                }
            }

            List<String> suggestions = generateSuggestions();

            y -= 30;
            contentStream.beginText();
            contentStream.setFont(boldFont, 12);
            contentStream.newLineAtOffset(50, y);
            contentStream.showText("Gợi ý tối ưu hóa thời gian sử dụng:");
            contentStream.endText();

            y -= 20;
            for (String suggestion : suggestions) {
                contentStream.beginText();
                contentStream.setFont(normalFont, 10);
                contentStream.newLineAtOffset(70, y);
                contentStream.showText("• " + suggestion);
                contentStream.endText();
                y -= 20;

                if (y < 100) {
                    // Tạo trang mới nếu hết không gian
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    PDPageContentStream newContentStream = new PDPageContentStream(document, page);
                    contentStream.close();
                    contentStream = newContentStream;
                    y = 750;
                }
            }

            // Thông tin tạo báo cáo
            y -= 40;
            contentStream.beginText();
            contentStream.setFont(normalFont, 8);
            contentStream.newLineAtOffset(50, y);
            contentStream.showText("Báo cáo được tạo vào: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            contentStream.endText();
            contentStream.close();
            document.save(filePath);
        }

        logger.info("Đã xuất báo cáo PDF: {}", filePath);
        return filePath;
    }

    private String exportToCSV(String fileName) throws Exception {
        String filePath = fileName + ".csv";

        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Ứng dụng,Thời gian sử dụng (phút),Thời gian sử dụng (giờ),Thời gian sử dụng\n");

        List<Map<String, Object>> appUsage = (List<Map<String, Object>>) reportData.get("appUsageData");

        for (Map<String, Object> app : appUsage) {
            csvContent.append(app.get("appName")).append(",")
                    .append(app.get("usageMinutes")).append(",")
                    .append(app.get("usageHours")).append(",")
                    .append(app.get("usageTime")).append("\n");
        }
        String bom = "\uFEFF";
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
            writer.write(bom);
            writer.write(csvContent.toString());
        }

        logger.info("Đã xuất báo cáo CSV: {}", filePath);
        return filePath;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Map<String, Object> getReportData() {
        if (reportData.isEmpty()) {
            generateReport();
        }
        return reportData;
    }
}