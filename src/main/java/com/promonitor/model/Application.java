package com.promonitor.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    private final StringProperty name = new SimpleStringProperty();
    private final IntegerProperty processId = new SimpleIntegerProperty();
    private final StringProperty executablePath = new SimpleStringProperty();
    private static final String DEFAULT_ICON_PATH = "/images/default-app.png";
    private Image icon;

    public Application(String name, int processId) {
        this.name.set(name);
        this.processId.set(processId);
        getExecutablePath(processId).ifPresent(this.executablePath::set);
        if (executablePath.get() != null && !executablePath.get().isEmpty()) {
            this.icon = getIconFromExecutablePath(executablePath.get());
        }
    }

    public Application(String name, int processId, String executablePath) {
        this(name, processId);
        this.executablePath.set(executablePath);
    }

    public String getName() {
        return name.get();
    }

    public int getProcessId() {
        return processId.get();
    }

    public String getExecutablePath() {
        return executablePath.get();
    }

    private Optional<String> getExecutablePath(int processId) {
        return ProcessHandle.of(processId)
                .flatMap(handle -> handle.info().command());
    }

    public Image getIcon() {
        return icon;
    }

    public void setIcon(Image icon) {
        this.icon = icon;
    }

    private Image getIconFromExecutablePath(String execPath) {
        try {
            File file = new File(execPath);
            if (!file.exists()) {
                return getDefaultIcon();
            }

            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                try {
                    //logger.info("Đang thử trích xuất icon từ {} bằng phương thức khác", execPath);
                    return extractWindowsIconUsingSwing(file);
                } catch (Exception e) {
                    logger.debug("Không thể trích xuất icon sử dụng phương thức nâng cao: {}", e.getMessage());
                }
            } else if (os.contains("mac")) {
                String appName = file.getName();
                if (appName.endsWith(".app")) {
                    File iconFile = new File(file, "Contents/Resources/AppIcon.icns");
                    if (iconFile.exists()) {
                        try {
                            return new Image(iconFile.toURI().toString());
                        } catch (Exception e) {
                            logger.debug("Không thể tải biểu tượng từ file .icns", e);
                        }
                    }
                }
            }

            return getDefaultIcon();

        } catch (Exception e) {
            logger.error("Lỗi khi lấy biểu tượng từ {}: {}", execPath, e.getMessage());
            return getDefaultIcon();
        }
    }

    private Image extractWindowsIconUsingSwing(File file) {
        try {
            javax.swing.Icon icon = javax.swing.filechooser.FileSystemView.getFileSystemView().getSystemIcon(file);

            if (icon != null) {
                java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(
                        icon.getIconWidth(), icon.getIconHeight(),
                        java.awt.image.BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics g = bufferedImage.createGraphics();
                icon.paintIcon(null, g, 0, 0);
                g.dispose();

                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                javax.imageio.ImageIO.write(bufferedImage, "png", out);
                out.flush();
                java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(out.toByteArray());
                return new Image(in);
            }
        } catch (Exception e) {
            logger.warn("Không thể trích xuất icon sử dụng Swing: {}", e.getMessage());
        }
        return null;
    }

    private Image getDefaultIcon() {
        try {
            return new Image(Objects.requireNonNull(getClass().getResourceAsStream(DEFAULT_ICON_PATH)));
        } catch (Exception e) {
            logger.warn("Không thể tải biểu tượng mặc định: {}", e.getMessage());
            return null;
        }
    }

    public ImageView createIconImageView(double width, double height) {
        Image img = icon;
        ImageView imageView = new ImageView(img);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        imageView.setPreserveRatio(true);
        return imageView;
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