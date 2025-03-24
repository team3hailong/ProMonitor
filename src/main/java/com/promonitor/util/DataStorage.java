package com.promonitor.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.promonitor.controller.LimitManager;
import com.promonitor.model.*;
import com.promonitor.model.enums.LimitType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class DataStorage {
    private static final Logger logger = LoggerFactory.getLogger(DataStorage.class);

    private final String dataDir;
    private static final String GROUPS_FILE = "application_groups.json";
    private static final String LIMITS_FILE = "limits.json";
    private static final String APP_USAGE_FILE = "app_usage.json";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat(DATE_FORMAT);

    private final Gson gson;
    private static DataStorage instance;

    private DataStorage() {
        this.dataDir = System.getProperty("user.home") + File.separator + ".promonitor";
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        createDataDirectory();
    }

    public static DataStorage getInstance() {
        if (instance == null) {
            instance = new DataStorage();
        }
        return instance;
    }

    private void createDataDirectory() {
        File dir = new File(dataDir);
        if (!dir.exists() && !dir.mkdirs()) {
            logger.error("Cannot create data directory: {}", dataDir);
        }
    }

    private String getFilePath(String fileName) {
        return dataDir + File.separator + fileName;
    }

    // -------------------- Application Groups --------------------

    public void saveApplicationGroups(List<ApplicationGroup> groups) {
        List<GroupData> groupsData = groups.stream()
                .map(group -> new GroupData(
                        group.getName(),
                        group.getApplications().stream()
                                .map(app -> new AppData(app.getName(), app.getProcessId(), app.getExecutablePath()))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        writeJsonToFile(groupsData, GROUPS_FILE, "application groups");
    }

    public List<ApplicationGroup> loadApplicationGroups() {
        List<GroupData> groupsData = readJsonFromFile(
                new TypeToken<List<GroupData>>(){}.getType(),
                GROUPS_FILE,
                "application groups",
                Collections.emptyList()
        );

        return groupsData.stream()
                .map(groupData -> {
                    ApplicationGroup group = new ApplicationGroup(groupData.name);
                    groupData.applications.forEach(appData ->
                            group.addApplication(new Application(appData.name, appData.processId, appData.executablePath))
                    );
                    return group;
                })
                .collect(Collectors.toList());
    }

    // -------------------- Limits --------------------

    public void saveLimits(Map<Object, Limit> limitsMap) {
        List<LimitData> limitsData = limitsMap.entrySet().stream()
                .map(entry -> {
                    Limit limit = entry.getValue();
                    Object target = entry.getKey();

                    String targetType = null;
                    String targetName = null;
                    String targetId = null;

                    if (target instanceof Application app) {
                        targetType = "APPLICATION";
                        targetName = app.getName();
                        targetId = String.valueOf(app.getProcessId());
                    } else if (target instanceof ApplicationGroup group) {
                        targetType = "GROUP";
                        targetName = group.getName();
                    }

                    Map<String, String> scheduleData = null;
                    if (limit.getType() == LimitType.SCHEDULE && limit.getSchedule() != null) {
                        scheduleData = new HashMap<>();
                        // Add schedule serialization logic here if needed
                    }

                    return new LimitData(
                            limit.getType().name(),
                            limit.getValue().getSeconds(),
                            targetType,
                            targetName,
                            targetId,
                            scheduleData
                    );
                })
                .collect(Collectors.toList());

        writeJsonToFile(limitsData, LIMITS_FILE, "limits");
    }

    public void loadLimits(LimitManager limitManager) {
        List<LimitData> limitsData = readJsonFromFile(
                new TypeToken<List<LimitData>>(){}.getType(),
                LIMITS_FILE,
                "limits",
                Collections.emptyList()
        );

        if (limitsData.isEmpty()) return;

        Map<String, ApplicationGroup> groupsCache = loadApplicationGroups().stream()
                .collect(Collectors.toMap(ApplicationGroup::getName, group -> group));

        limitsData.forEach(limitData -> {
            try {
                LimitType limitType = LimitType.valueOf(limitData.type);
                Duration value = Duration.ofSeconds(limitData.durationSeconds);
                Limit limit = new Limit(limitType, value);

                if (limitType == LimitType.SCHEDULE && limitData.scheduleData != null) {
                    Schedule schedule = new Schedule();
                    // Add schedule deserialization logic here if needed
                    limit.setSchedule(schedule);
                }

                if ("APPLICATION".equals(limitData.targetType)) {
                    Application app = new Application(limitData.targetName,
                            Integer.parseInt(limitData.targetId));
                    limitManager.setLimit(app, limit);
                } else if ("GROUP".equals(limitData.targetType) &&
                        limitData.targetName != null &&
                        groupsCache.containsKey(limitData.targetName)) {
                    limitManager.setLimit(groupsCache.get(limitData.targetName), limit);
                }
            } catch (Exception e) {
                logger.error("Error processing limit: {}", limitData.targetName, e);
            }
        });
    }

    // -------------------- App Usage Data --------------------

    public void saveAppUsageData(List<AppUsageData> appUsageData) {
        if (appUsageData.isEmpty()) return;

        appUsageData.forEach(data -> {
            if (data.getUsageTimeMillis() <= 0) {
                data.calculateUsageTimeMillis();
            }
        });

        Map<String, List<AppUsageData>> newDataByDate = appUsageData.stream()
                .collect(Collectors.groupingBy(AppUsageData::getDate));

        Map<String, List<AppUsageData>> existingDataByDate = loadUsageDataMap();

        newDataByDate.forEach((date, newDateData) -> {
            if (existingDataByDate.containsKey(date)) {
                List<AppUsageData> existingDateData = existingDataByDate.get(date);
                Map<String, AppUsageData> mergedDataMap = new HashMap<>();

                existingDateData.forEach(existingApp -> {
                    String key = existingApp.getName() + "_" + existingApp.getExecutablePath();
                    mergedDataMap.put(key, existingApp);
                });

                newDateData.forEach(newApp -> {
                    String key = newApp.getName() + "_" + newApp.getExecutablePath();
                    mergedDataMap.put(key, newApp);
                });

                existingDataByDate.put(date, new ArrayList<>(mergedDataMap.values()));
            } else {
                existingDataByDate.put(date, newDateData);
            }
        });

        writeJsonToFile(existingDataByDate, APP_USAGE_FILE, "app usage data");
    }

    private Map<String, List<AppUsageData>> loadUsageDataMap() {
        return readJsonFromFile(
                new TypeToken<Map<String, List<AppUsageData>>>(){}.getType(),
                APP_USAGE_FILE,
                "app usage data",
                new HashMap<>()
        );
    }

    public List<AppUsageData> loadAppUsageData() {
        Map<String, List<AppUsageData>> usageByDate = loadUsageDataMap();

        return usageByDate.values().stream()
                .flatMap(Collection::stream)
                .peek(data -> {
                    if (data.getUsageTimeMillis() <= 0) {
                        data.calculateUsageTimeMillis();
                    }
                })
                .collect(Collectors.toList());
    }

    public List<AppUsageData> getUsageDataInTimeRange(String startDate, String endDate) {
        try {
            Date start = DATE_FORMATTER.parse(startDate);
            Date end = DATE_FORMATTER.parse(endDate);

            return loadUsageDataMap().entrySet().stream()
                    .filter(entry -> {
                        try {
                            Date dateKey = DATE_FORMATTER.parse(entry.getKey());
                            return (dateKey.equals(start) || dateKey.after(start)) &&
                                    (dateKey.equals(end) || dateKey.before(end));
                        } catch (ParseException e) {
                            logger.error("Error parsing date: {}", entry.getKey(), e);
                            return false;
                        }
                    })
                    .flatMap(entry -> entry.getValue().stream())
                    .collect(Collectors.toList());

        } catch (ParseException e) {
            logger.error("Error parsing date range: {} to {}", startDate, endDate, e);
            return Collections.emptyList();
        }
    }

    public List<AppUsageData> getUsageDataForDateRange(String startDate, String endDate) {
        return getUsageDataInTimeRange(startDate, endDate);
    }

    public List<AppUsageData> getTodayUsageData() {
        String today = DATE_FORMATTER.format(new Date());
        Map<String, List<AppUsageData>> usageByDate = loadUsageDataMap();

        if (usageByDate.containsKey(today)) {
            return usageByDate.get(today).stream()
                    .peek(data -> {
                        if (data.getUsageTimeMillis() <= 0) {
                            data.calculateUsageTimeMillis();
                        }
                    })
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    public List<AppUsageData> getCurrentWeekUsageData() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        String startDate = DATE_FORMATTER.format(cal.getTime());

        cal.add(Calendar.DAY_OF_WEEK, 6);
        String endDate = DATE_FORMATTER.format(cal.getTime());

        return getUsageDataInTimeRange(startDate, endDate);
    }

    public void updateAppUsageForToday(Application app, String usageTime, String limitInfo) {
        updateAppUsageForDate(app, usageTime, limitInfo, DATE_FORMATTER.format(new Date()));
    }

    public void updateAppUsageForDate(Application app, String usageTime, String limitInfo, String date) {
        Map<String, List<AppUsageData>> usageByDate = loadUsageDataMap();

        // Get or create the list for the date
        List<AppUsageData> dateData = usageByDate.computeIfAbsent(date, k -> new ArrayList<>());

        // Try to find and update existing app data
        boolean found = false;
        for (AppUsageData data : dateData) {
            if (data.getName().equals(app.getName()) &&
                    data.getExecutablePath().equals(app.getExecutablePath())) {
                data.setUsageTime(usageTime);
                data.setLimitInfo(limitInfo);
                data.calculateUsageTimeMillis();
                found = true;
                break;
            }
        }

        // Add new entry if not found
        if (!found) {
            AppUsageData newData = new AppUsageData(
                    app.getName(), app.getProcessId(), app.getExecutablePath(),
                    usageTime, limitInfo, date
            );
            dateData.add(newData);
        }

        writeJsonToFile(usageByDate, APP_USAGE_FILE, "app usage data");
    }

    public void updateAppUsageAcrossDays(Application app, Date startTime, Date endTime, String limitInfo) {
        if (startTime.after(endTime)) {
            logger.error("Start time is after end time: {} > {}", startTime, endTime);
            return;
        }

        // Calculate usage for each day in the range
        Map<String, Long> dailyUsageMillis = calculateDailyUsage(startTime, endTime);

        // Update each day's data
        dailyUsageMillis.forEach((dateStr, usageMillis) -> {
            String usageTime = formatMillisToTime(usageMillis);
            updateAppUsageForDate(app, usageTime, limitInfo, dateStr);
        });
    }

    private Map<String, Long> calculateDailyUsage(Date startTime, Date endTime) {
        Map<String, Long> dailyUsageMillis = new HashMap<>();
        String startDateStr = DATE_FORMATTER.format(startTime);
        String endDateStr = DATE_FORMATTER.format(endTime);

        // If same day, simple calculation
        if (startDateStr.equals(endDateStr)) {
            dailyUsageMillis.put(startDateStr, endTime.getTime() - startTime.getTime());
            return dailyUsageMillis;
        }

        Calendar currentDay = Calendar.getInstance();
        currentDay.setTime(startTime);

        Calendar endOfDay = Calendar.getInstance();
        endOfDay.setTime(startTime);
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);

        while (currentDay.getTime().before(endTime)) {
            String currentDateStr = DATE_FORMATTER.format(currentDay.getTime());
            long usageInDay;

            if (endOfDay.getTime().before(endTime)) {
                // Full day usage
                usageInDay = endOfDay.getTimeInMillis() - currentDay.getTimeInMillis() + 1;
            } else {
                // Partial day (last day)
                usageInDay = endTime.getTime() - currentDay.getTimeInMillis();
            }

            dailyUsageMillis.put(currentDateStr, usageInDay);

            // Move to next day
            currentDay.add(Calendar.DATE, 1);
            currentDay.set(Calendar.HOUR_OF_DAY, 0);
            currentDay.set(Calendar.MINUTE, 0);
            currentDay.set(Calendar.SECOND, 0);
            currentDay.set(Calendar.MILLISECOND, 0);

            endOfDay.setTime(currentDay.getTime());
            endOfDay.set(Calendar.HOUR_OF_DAY, 23);
            endOfDay.set(Calendar.MINUTE, 59);
            endOfDay.set(Calendar.SECOND, 59);
            endOfDay.set(Calendar.MILLISECOND, 999);
        }

        return dailyUsageMillis;
    }

    private String formatMillisToTime(long millis) {
        long seconds = millis / 1000;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    // -------------------- Generic JSON I/O --------------------

    private <T> T readJsonFromFile(Type type, String fileName, String description, T defaultValue) {
        String filePath = getFilePath(fileName);
        File file = new File(filePath);

        if (!file.exists()) {
            logger.debug("{} file does not exist: {}", description, filePath);
            return defaultValue;
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8)) {
            T result = gson.fromJson(reader, type);
            return result != null ? result : defaultValue;
        } catch (IOException e) {
            logger.error("Error reading {} from file: {}", description, filePath, e);
            return defaultValue;
        }
    }

    private <T> void writeJsonToFile(T data, String fileName, String description) {
        String filePath = getFilePath(fileName);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            logger.error("Error saving {} to file: {}", description, filePath, e);
        }
    }

    // -------------------- Data Classes --------------------

    private record AppData(String name, int processId, String executablePath) {
    }

    private record GroupData(String name, List<AppData> applications) {
    }

    private record LimitData(String type, long durationSeconds, String targetType, String targetName, String targetId,
                             Map<String, String> scheduleData) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AppUsageData implements Serializable {
        private String name;
        private int processId;
        private String executablePath;
        private String usageTime;
        private String limitInfo;
        private String date;
        private transient Application application;
        private long usageTimeMillis;

        @JsonCreator
        public AppUsageData(
                @JsonProperty("name") String name,
                @JsonProperty("processId") int processId,
                @JsonProperty("executablePath") String executablePath,
                @JsonProperty("usageTime") String usageTime,
                @JsonProperty("limitInfo") String limitInfo,
                @JsonProperty("date") String date
        ) {
            this.name = name;
            this.processId = processId;
            this.executablePath = executablePath;
            this.usageTime = usageTime;
            this.limitInfo = limitInfo;
            this.date = date;
            calculateUsageTimeMillis();
        }

        @JsonIgnore
        public AppUsageData(String name, int processId, String executablePath, String usageTime, String limitInfo) {
            this(name, processId, executablePath, usageTime, limitInfo, DATE_FORMATTER.format(new Date()));
        }

        public void calculateUsageTimeMillis() {
            try {
                String[] parts = usageTime.split(":");
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                int seconds = Integer.parseInt(parts[2]);
                this.usageTimeMillis = (hours * 3600L + minutes * 60L + seconds) * 1000L;
            } catch (Exception e) {
                logger.warn("Error converting time string '{}': {}", usageTime, e.getMessage());
                this.usageTimeMillis = 0;
            }
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getProcessId() { return processId; }
        public void setProcessId(int processId) { this.processId = processId; }

        public String getExecutablePath() { return executablePath; }
        public void setExecutablePath(String executablePath) { this.executablePath = executablePath; }

        public String getUsageTime() { return usageTime; }
        public void setUsageTime(String usageTime) { this.usageTime = usageTime; }

        public String getLimitInfo() { return limitInfo; }
        public void setLimitInfo(String limitInfo) { this.limitInfo = limitInfo; }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public Application getApplication() { return application; }
        public void setApplication(Application application) { this.application = application; }

        public long getUsageTimeMillis() { return usageTimeMillis; }
        public void setUsageTimeMillis(long usageTimeMillis) { this.usageTimeMillis = usageTimeMillis; }

        @Override
        public String toString() {
            return "AppUsageData{name='" + name + "', processId=" + processId + '}';
        }
    }
}