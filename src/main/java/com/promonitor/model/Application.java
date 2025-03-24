package com.promonitor.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    private final StringProperty name = new SimpleStringProperty();
    private final IntegerProperty processId = new SimpleIntegerProperty();
    private final StringProperty executablePath = new SimpleStringProperty();
    @JsonIgnore
    private Image icon;

    public Application(String name, int processId) {
        this.name.set(name);
        this.processId.set(processId);
    }

    public Application(String name, int processId, String executablePath) {
        this(name, processId);
        this.executablePath.set(executablePath);
    }

    public Application(String name, int processId, String executablePath, Image icon) {
        this(name, processId, executablePath);
        this.icon = icon;
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public int getProcessId() {
        return processId.get();
    }

    public String getExecutablePath() {
        return executablePath.get();
    }

    public Image getIcon() {
        return icon;
    }

    public void setIcon(Image icon) {
        this.icon = icon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Application that = (Application) o;

        return Objects.equals(name.get(), that.name.get()) && 
               Objects.equals(executablePath.get(), that.executablePath.get());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.get(), executablePath.get());
    }

    @Override
    public String toString() {
        return name.get() + " (PID: " + processId.get() + ")";
    }

    public String getUniqueId() {
        String path = executablePath.get();
        if (path != null && !path.isEmpty()) {
            return name.get() + "_" + path.hashCode();
        }
        return name.get();
    }

    public void terminate() {
        String os = System.getProperty("os.name").toLowerCase();

        try {
            Process process;
            if (os.contains("win")) {
                process = Runtime.getRuntime().exec("taskkill /F /PID " + this.getProcessId());
            } else if (os.contains("mac") || os.contains("nix") || os.contains("nux")) {
                process = Runtime.getRuntime().exec("kill -9 " + this.getProcessId());
            } else {
                logger.warn("Hệ điều hành không được hỗ trợ: {}", os);
                return;
            }

            int exitCode = process.waitFor();
            boolean success = exitCode == 0;

            if (success) {
                logger.info("Đã kết thúc ứng dụng {} (PID: {})", this.getName(), this.getProcessId());
            } else {
                logger.warn("Không thể kết thúc ứng dụng {} (PID: {})", this.getName(), this.getProcessId());
            }

        } catch (IOException | InterruptedException e) {
            logger.error("Lỗi khi kết thúc ứng dụng {}", this.getName(), e);
        }
    }

    public static List<Application> getRunningApplications() {
        List<Application> applications = new ArrayList<>();
        String os = System.getProperty("os.name").toLowerCase();

        try {
            Process process = null;
            BufferedReader reader = null;
            String line;

            if (os.contains("win")) {
                process = Runtime.getRuntime().exec("tasklist /FO CSV /NH");
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\",\"");
                    if (parts.length >= 2) {
                        String name = parts[0].replace("\"", "");
                        String pidStr = parts[1].replace("\"", "");
                        try {
                            int pid = Integer.parseInt(pidStr);
                            applications.add(new Application(name, pid));
                        } catch (NumberFormatException e) {
                            // Bỏ qua nếu không phân tích được PID
                        }
                    }
                }
            } else if (os.contains("mac") || os.contains("nix") || os.contains("nux")) {
                process = Runtime.getRuntime().exec("ps -e -o pid,comm");
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                reader.readLine();

                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    String[] parts = line.split("\\s+", 2);
                    if (parts.length == 2) {
                        try {
                            int pid = Integer.parseInt(parts[0]);
                            String name = parts[1];
                            applications.add(new Application(name, pid));
                        } catch (NumberFormatException e) {
                            // Do nothing
                        }
                    }
                }
            } else {
                logger.warn("Hệ điều hành không hỗ trợ thu thập ứng dụng: {}", os);
            }

            assert reader != null;
            reader.close();
            process.waitFor();

        } catch (IOException | InterruptedException e) {
            logger.error("Lỗi khi lấy danh sách tiến trình", e);
        }

        return applications;
    }

    public static List<Application> findRunningApplicationsByName(String name) {
        List<Application> matchingApps = new ArrayList<>();
        List<Application> allApps = getRunningApplications();

        String searchLower = name.toLowerCase();
        for (Application app : allApps) {
            if (app.getName().toLowerCase().contains(searchLower)) {
                matchingApps.add(app);
            }
        }

        return matchingApps;
    }

}