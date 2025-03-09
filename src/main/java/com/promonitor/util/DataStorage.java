package com.promonitor.util;

import com.promonitor.controller.LimitManager;
import com.promonitor.model.*;
import com.promonitor.model.enums.LimitType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

public class DataStorage {
    private static final Logger logger = LoggerFactory.getLogger(DataStorage.class);

    private final String userId;
    private final String dataDir;

    private static final String GROUPS_FILE = "application_groups.dat";
    private static final String LIMITS_FILE = "limits.dat";

    public DataStorage(String userId) {
        this.userId = userId;
        this.dataDir = System.getProperty("user.home") + File.separator +
                ".promonitor" + File.separator + userId;

        // Tạo thư mục lưu trữ nếu chưa tồn tại
        createDataDirectory();
    }

    private void createDataDirectory() {
        File dir = new File(dataDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                logger.info("Đã tạo thư mục lưu trữ dữ liệu: {}", dataDir);
            } else {
                logger.error("Không thể tạo thư mục lưu trữ dữ liệu: {}", dataDir);
            }
        }
    }

    public boolean saveApplicationGroups(List<ApplicationGroup> groups) {
        String filePath = dataDir + File.separator + GROUPS_FILE;

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {

            List<SerializableGroup> serializableGroups = new ArrayList<>();

            for (ApplicationGroup group : groups) {
                SerializableGroup sGroup = new SerializableGroup();
                sGroup.name = group.getName();

                for (Application app : group.getApplications()) {
                    SerializableApplication sApp = new SerializableApplication();
                    sApp.name = app.getName();
                    sApp.processId = app.getProcessId();
                    sApp.executablePath = app.getExecutablePath();

                    sGroup.applications.add(sApp);
                }

                serializableGroups.add(sGroup);
            }

            oos.writeObject(serializableGroups);
            logger.debug("Đã lưu {} nhóm ứng dụng", groups.size());
            return true;
        } catch (IOException e) {
            logger.error("Lỗi khi lưu nhóm ứng dụng", e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public List<ApplicationGroup> loadApplicationGroups() {
        String filePath = dataDir + File.separator + GROUPS_FILE;
        File file = new File(filePath);

        if (!file.exists()) {
            logger.debug("File nhóm ứng dụng không tồn tại");
            return new ArrayList<>();
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            List<SerializableGroup> serializableGroups = (List<SerializableGroup>) ois.readObject();
            List<ApplicationGroup> groups = new ArrayList<>();

            for (SerializableGroup sGroup : serializableGroups) {
                ApplicationGroup group = new ApplicationGroup(sGroup.name);

                // Khôi phục các ứng dụng trong nhóm
                for (SerializableApplication sApp : sGroup.applications) {
                    Application app = new Application(sApp.name, sApp.processId, sApp.executablePath);
                    group.addApplication(app);
                }

                groups.add(group);
            }

            logger.debug("Đã tải {} nhóm ứng dụng", groups.size());
            return groups;
        } catch (IOException | ClassNotFoundException e) {
            logger.error("Lỗi khi tải nhóm ứng dụng", e);
            return new ArrayList<>();
        }
    }

    public boolean saveLimits(Map<Object, Limit> limitsMap) {
        String filePath = dataDir + File.separator + LIMITS_FILE;

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {

            List<SerializableLimit> serializableLimits = new ArrayList<>();

            for (Map.Entry<Object, Limit> entry : limitsMap.entrySet()) {
                SerializableLimit sLimit = new SerializableLimit();
                Limit limit = entry.getValue();

                sLimit.type = limit.getType().name();
                sLimit.durationSeconds = limit.getValue().getSeconds();

                if (entry.getKey() instanceof Application) {
                    Application app = (Application) entry.getKey();
                    sLimit.targetType = "APPLICATION";
                    sLimit.targetName = app.getName();
                    sLimit.targetId = String.valueOf(app.getProcessId());
                } else if (entry.getKey() instanceof ApplicationGroup) {
                    ApplicationGroup group = (ApplicationGroup) entry.getKey();
                    sLimit.targetType = "GROUP";
                    sLimit.targetName = group.getName();
                }
                if (limit.getType() == LimitType.SCHEDULE && limit.getSchedule() != null) {
                    sLimit.scheduleData = serializeSchedule(limit.getSchedule());
                }

                serializableLimits.add(sLimit);
            }

            oos.writeObject(serializableLimits);
            logger.debug("Đã lưu {} giới hạn thời gian", limitsMap.size());
            return true;
        } catch (IOException e) {
            logger.error("Lỗi khi lưu giới hạn thời gian", e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public boolean loadLimits(LimitManager limitManager) {
        String filePath = dataDir + File.separator + LIMITS_FILE;
        File file = new File(filePath);

        if (!file.exists()) {
            logger.debug("File giới hạn không tồn tại");
            return false;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            List<SerializableLimit> serializableLimits = (List<SerializableLimit>) ois.readObject();

            for (SerializableLimit sLimit : serializableLimits) {
                LimitType limitType = LimitType.valueOf(sLimit.type);
                Duration value = Duration.ofSeconds(sLimit.durationSeconds);
                Limit limit = new Limit(limitType, value);

                // Khôi phục lịch trình nếu có
                if (limitType == LimitType.SCHEDULE && sLimit.scheduleData != null) {
                    Schedule schedule = deserializeSchedule(sLimit.scheduleData);
                    limit.setSchedule(schedule);
                }

                // Áp dụng giới hạn cho đối tượng tương ứng
                if ("APPLICATION".equals(sLimit.targetType)) {
                    Application app = new Application(sLimit.targetName,
                            Integer.parseInt(sLimit.targetId));
                    limitManager.setLimit(app, limit);
                } else if ("GROUP".equals(sLimit.targetType)) {
                    // Tìm nhóm trong danh sách đã tải
                    List<ApplicationGroup> groups = loadApplicationGroups();
                    for (ApplicationGroup group : groups) {
                        if (group.getName().equals(sLimit.targetName)) {
                            limitManager.setLimit(group, limit);
                            break;
                        }
                    }
                }
            }

            logger.debug("Đã tải {} giới hạn thời gian", serializableLimits.size());
            return true;
        } catch (IOException | ClassNotFoundException e) {
            logger.error("Lỗi khi tải giới hạn thời gian", e);
            return false;
        }
    }

    // Các phương thức hỗ trợ để chuyển đổi Schedule sang/từ dạng serializable
    private Map<String, String> serializeSchedule(Schedule schedule) {
        Map<String, String> data = new HashMap<>();
        // Thực hiện tuần tự hóa lịch trình
        // Implementation would go here
        return data;
    }

    private Schedule deserializeSchedule(Map<String, String> data) {
        Schedule schedule = new Schedule();
        return schedule;
    }

    // Các lớp hỗ trợ để lưu dữ liệu có thể serialize
    private static class SerializableApplication implements Serializable {
        private static final long serialVersionUID = 1L;
        String name;
        int processId;
        String executablePath;
    }

    private static class SerializableGroup implements Serializable {
        private static final long serialVersionUID = 1L;
        String name;
        List<SerializableApplication> applications = new ArrayList<>();
    }

    private static class SerializableLimit implements Serializable {
        private static final long serialVersionUID = 1L;
        String type;
        long durationSeconds;
        String targetType; // "APPLICATION" hoặc "GROUP"
        String targetName;
        String targetId;
        Map<String, String> scheduleData;
    }
}