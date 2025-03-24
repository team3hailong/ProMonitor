package com.promonitor.view;

import com.promonitor.controller.MainController;
import com.promonitor.model.Application;
import com.promonitor.model.Limit;
import com.promonitor.model.TimeTracker;
import com.promonitor.util.DataStorage;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class ApplicationsView {
    private final MainController controller;
    private final LimitsView limitsView;
    private BorderPane content;
    private FilteredList<DataStorage.AppUsageData> filteredData;
    private final ObservableList<DataStorage.AppUsageData> masterData = FXCollections.observableArrayList();
    private final DataStorage dataStorage;
    private Label totalAppsLabel;
    private Label limitedAppsLabel;
    private Label topUsageLabel;

    public ApplicationsView(MainController controller, LimitsView limitsView) {
        this.controller = controller;
        this.limitsView = limitsView;
        this.dataStorage = controller.getDataStorage();
        createContent();
        loadSavedData();
    }

    private void createContent() {
        content = new BorderPane();
        content.setPadding(new Insets(15));
        content.getStyleClass().add("applications-view");

        GridPane headerGrid = new GridPane();
        headerGrid.setHgap(10);
        headerGrid.setVgap(2);

        FontAwesomeIconView appsIcon = new FontAwesomeIconView(FontAwesomeIcon.WINDOWS);
        appsIcon.setGlyphSize(20);
        appsIcon.setFill(Color.valueOf("#4a6bff"));

        StackPane iconContainer = new StackPane(appsIcon);
        iconContainer.setAlignment(Pos.CENTER);
        iconContainer.setMinHeight(40);

        headerGrid.add(iconContainer, 0, 0, 1, 2);

        Label titleLabel = new Label("Quản lý ứng dụng");
        titleLabel.getStyleClass().add("view-title");
        headerGrid.add(titleLabel, 1, 0);

        Label descriptionLabel = new Label("Theo dõi và quản lý thời gian sử dụng ứng dụng");
        descriptionLabel.getStyleClass().add("description-text");
        headerGrid.add(descriptionLabel, 1, 1);

        HBox statsContainer = createStatsCards();
        statsContainer.getStyleClass().add("stats-container");

        HBox toolsBar = new HBox(10);
        toolsBar.getStyleClass().add("tools-bar");

        StackPane searchPane = new StackPane();
        searchPane.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Tìm kiếm ứng dụng...");
        searchField.setPrefWidth(280);
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> updateSearchFilter(newValue));

        FontAwesomeIconView searchIcon = new FontAwesomeIconView(FontAwesomeIcon.SEARCH);
        searchIcon.setGlyphSize(13);
        searchIcon.getStyleClass().add("search-icon");
        StackPane.setMargin(searchIcon, new Insets(0, 0, 0, 12));

        searchPane.getChildren().addAll(searchField, searchIcon);

        Label filterLabel = new Label("Hiển thị:");
        filterLabel.getStyleClass().add("filter-label");

        ComboBox<String> filterCombo = new ComboBox<>(
                FXCollections.observableArrayList("Tất cả ứng dụng", "Có giới hạn", "Không giới hạn")
        );
        filterCombo.setValue("Tất cả ứng dụng");
        filterCombo.getStyleClass().add("filter-combobox");

        filterCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            switch (newVal) {
                case "Tất cả ứng dụng":
                    filteredData.setPredicate(app -> true);
                    break;
                case "Có giới hạn":
                    filteredData.setPredicate(app -> !app.getLimitInfo().equals("Không giới hạn"));
                    break;
                case "Không giới hạn":
                    filteredData.setPredicate(app -> app.getLimitInfo().equals("Không giới hạn"));
                    break;
            }
        });

        Button scanButton = new Button("Quét ứng dụng");
        scanButton.getStyleClass().add("action-button");

        FontAwesomeIconView scanIcon = new FontAwesomeIconView(FontAwesomeIcon.REFRESH);
        scanIcon.setGlyphSize(12);
        scanIcon.setFill(Color.WHITE);
        scanButton.setGraphic(scanIcon);
        scanButton.setGraphicTextGap(8);

        scanButton.setOnAction(e -> updateData());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolsBar.getChildren().addAll(searchPane, filterLabel, filterCombo, spacer, scanButton);

        VBox topContainer = new VBox(15);
        topContainer.getChildren().addAll(headerGrid, statsContainer, toolsBar);
        content.setTop(topContainer);

        TableView<DataStorage.AppUsageData> appsTable = createAppsTable();

        Label placeholderLabel = new Label("Không có ứng dụng nào được theo dõi");
        placeholderLabel.getStyleClass().add("placeholder-label");

        FontAwesomeIconView emptyIcon = new FontAwesomeIconView(FontAwesomeIcon.INFO_CIRCLE);
        emptyIcon.setGlyphSize(16);
        emptyIcon.setFill(Color.valueOf("#9aa0a6"));
        placeholderLabel.setGraphic(emptyIcon);
        placeholderLabel.setGraphicTextGap(10);

        appsTable.setPlaceholder(placeholderLabel);

        content.setCenter(appsTable);
    }

    private VBox createStatsCard(FontAwesomeIcon iconType, String titleText, Label statLabel, String... additionalLabelClasses) {
        // Tạo container của thẻ stats
        VBox card = new VBox(5);
        card.getStyleClass().add("stats-card");
        card.setPrefWidth(200);

        // Tạo header chứa icon và tiêu đề
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        FontAwesomeIconView iconView = new FontAwesomeIconView(iconType);
        iconView.setGlyphSize(16);
        iconView.getStyleClass().add("stats-icon");

        Label title = new Label(titleText);
        title.getStyleClass().add("stats-title");

        header.getChildren().addAll(iconView, title);

        // Thêm các style phụ cho label nếu cần
        statLabel.getStyleClass().add("stats-value");
        if (additionalLabelClasses != null && additionalLabelClasses.length > 0) {
            statLabel.getStyleClass().addAll(additionalLabelClasses);
        }

        // Ghép header và label vào thẻ stats
        card.getChildren().addAll(header, statLabel);

        return card;
    }

    private HBox createStatsCards() {
        HBox statsContainer = new HBox(15);

        totalAppsLabel = new Label("0");
        limitedAppsLabel = new Label("0");
        topUsageLabel = new Label("--");

        VBox totalAppsCard = createStatsCard(FontAwesomeIcon.TH_LARGE, "Tổng số ứng dụng", totalAppsLabel);
        VBox limitedAppsCard = createStatsCard(FontAwesomeIcon.CLOCK_ALT, "Ứng dụng có giới hạn", limitedAppsLabel);
        VBox topUsageCard = createStatsCard(FontAwesomeIcon.BAR_CHART, "Sử dụng nhiều nhất", topUsageLabel, "time-medium");

        statsContainer.getChildren().addAll(totalAppsCard, limitedAppsCard, topUsageCard);

        return statsContainer;
    }

    private TableView<DataStorage.AppUsageData> createAppsTable() {
        TableView<DataStorage.AppUsageData> appsTable = new TableView<>();
        appsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<DataStorage.AppUsageData, String> nameCol = new TableColumn<>("Tên ứng dụng");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(250);
        nameCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(10);
                    hbox.setAlignment(Pos.CENTER_LEFT);

                    FontAwesomeIconView appIcon = new FontAwesomeIconView(FontAwesomeIcon.WINDOW_MAXIMIZE);
                    appIcon.setGlyphSize(14);
                    appIcon.setFill(Color.valueOf("#4a6bff"));

                    Label nameLabel = new Label(item);

                    hbox.getChildren().addAll(appIcon, nameLabel);
                    setGraphic(hbox);
                    setText(null);
                }
            }
        });

        TableColumn<DataStorage.AppUsageData, String> pidCol = new TableColumn<>("PID");
        pidCol.setCellValueFactory(new PropertyValueFactory<>("processId"));
        pidCol.setPrefWidth(80);
        pidCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<DataStorage.AppUsageData, String> timeCol = new TableColumn<>("Thời gian sử dụng");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("usageTime"));
        timeCol.setPrefWidth(150);
        timeCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(8);
                    hbox.setAlignment(Pos.CENTER_LEFT);

                    FontAwesomeIconView timeIcon = new FontAwesomeIconView(FontAwesomeIcon.CLOCK_ALT);
                    timeIcon.setGlyphSize(12);

                    Label timeLabel = new Label(item);

                    // Color coding based on usage time
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        DataStorage.AppUsageData data = getTableRow().getItem();
                        long hours = data.getUsageTimeMillis() / (1000 * 60 * 60);

                        if (hours >= 3) {
                            timeIcon.setFill(Color.valueOf("#e74c3c"));
                            timeLabel.getStyleClass().add("time-high");
                        } else if (hours >= 1) {
                            timeIcon.setFill(Color.valueOf("#f39c12"));
                            timeLabel.getStyleClass().add("time-medium");
                        } else {
                            timeIcon.setFill(Color.valueOf("#27ae60"));
                            timeLabel.getStyleClass().add("time-low");
                        }
                    }

                    hbox.getChildren().addAll(timeIcon, timeLabel);
                    setGraphic(hbox);
                    setText(null);
                }
            }
        });

        TableColumn<DataStorage.AppUsageData, String> limitCol = new TableColumn<>("Giới hạn");
        limitCol.setCellValueFactory(new PropertyValueFactory<>("limitInfo"));
        limitCol.setPrefWidth(150);
        limitCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    if (item.equals("Không giới hạn")) {
                        badge.getStyleClass().add("no-limit-badge");
                    } else {
                        badge.getStyleClass().add("limit-badge");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<DataStorage.AppUsageData, String> actionCol = new TableColumn<>("Thao tác");
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final HBox buttonsBox = new HBox(8);
            private final Button setLimitBtn = new Button();
            private final Button viewDetailsBtn = new Button();

            {
                setLimitBtn.getStyleClass().add("action-button");
                setLimitBtn.setText("Đặt giới hạn");

                FontAwesomeIconView limitIcon = new FontAwesomeIconView(FontAwesomeIcon.CLOCK_ALT);
                limitIcon.setGlyphSize(12);
                limitIcon.setFill(Color.WHITE);
                setLimitBtn.setGraphic(limitIcon);
                setLimitBtn.setGraphicTextGap(5);

                setLimitBtn.setOnAction(event -> {
                    DataStorage.AppUsageData data = getTableView().getItems().get(getIndex());
                    if(!data.getLimitInfo().equals("Không giới hạn")) {
                        LimitsView.LimitInfo limitInfo = new LimitsView.LimitInfo(
                                data.getApplication(),
                                data.getApplication().getName(),
                                "Ứng dụng",
                                data.getLimitInfo(),
                                null
                        );
                        limitsView.editLimit(limitInfo);
                    }
                    else {
                        limitsView.createNewLimit();
                    }
                });

                // View details button
                viewDetailsBtn.getStyleClass().add("action-button-small");

                FontAwesomeIconView detailsIcon = new FontAwesomeIconView(FontAwesomeIcon.INFO_CIRCLE);
                detailsIcon.setGlyphSize(12);
                detailsIcon.setFill(Color.valueOf("#4a6bff"));
                viewDetailsBtn.setGraphic(detailsIcon);

                Tooltip detailsTooltip = new Tooltip("Xem chi tiết");
                Tooltip.install(viewDetailsBtn, detailsTooltip);

                viewDetailsBtn.setOnAction(event -> {
                    DataStorage.AppUsageData data = getTableView().getItems().get(getIndex());
                    showAppDetails(data);
                });

                buttonsBox.setAlignment(Pos.CENTER);
                buttonsBox.getChildren().addAll(setLimitBtn, viewDetailsBtn);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    DataStorage.AppUsageData data = getTableView().getItems().get(getIndex());
                    if(!data.getLimitInfo().equals("Không giới hạn")) {
                        setLimitBtn.setText("Sửa giới hạn");
                    } else {
                        setLimitBtn.setText("Đặt giới hạn");
                    }
                    setGraphic(buttonsBox);
                }
            }
        });
        actionCol.setPrefWidth(180);

        appsTable.getColumns().addAll(nameCol, pidCol, timeCol, limitCol, actionCol);

        // Set up filtered and sorted data
        filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<DataStorage.AppUsageData> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(appsTable.comparatorProperty());
        appsTable.setItems(sortedData);

        return appsTable;
    }

    private void showAppDetails(DataStorage.AppUsageData data) {
        // Create a dialog to show app details
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Chi tiết ứng dụng");
        dialog.setHeaderText("Chi tiết về " + data.getName());

        // Set the button types
        ButtonType closeButtonType = new ButtonType("Đóng", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(closeButtonType);

        // Create content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        FontAwesomeIconView appIcon = new FontAwesomeIconView(FontAwesomeIcon.WINDOW_MAXIMIZE);
        appIcon.setGlyphSize(48);
        appIcon.setFill(Color.valueOf("#4a6bff"));
        StackPane iconPane = new StackPane(appIcon);

        Label nameLabel = new Label("Tên ứng dụng:");
        Label nameValue = new Label(data.getName());
        nameValue.setStyle("-fx-font-weight: bold;");

        Label pidLabel = new Label("Mã tiến trình (PID):");
        Label pidValue = new Label(String.valueOf(data.getProcessId()));

        Label pathLabel = new Label("Đường dẫn:");
        Label pathValue = new Label(data.getExecutablePath());
        pathValue.setWrapText(true);

        Label timeLabel = new Label("Thời gian sử dụng:");
        Label timeValue = new Label(data.getUsageTime());

        Label limitLabel = new Label("Giới hạn:");
        Label limitValue = new Label(data.getLimitInfo());

        Label dateLabel = new Label("Ngày theo dõi:");
        Label dateValue = new Label(data.getDate());

        grid.add(iconPane, 0, 0, 2, 1);
        grid.add(nameLabel, 0, 1);
        grid.add(nameValue, 1, 1);
        grid.add(pidLabel, 0, 2);
        grid.add(pidValue, 1, 2);
        grid.add(pathLabel, 0, 3);
        grid.add(pathValue, 1, 3);
        grid.add(timeLabel, 0, 4);
        grid.add(timeValue, 1, 4);
        grid.add(limitLabel, 0, 5);
        grid.add(limitValue, 1, 5);
        grid.add(dateLabel, 0, 6);
        grid.add(dateValue, 1, 6);

        dialog.getDialogPane().setContent(grid);

        // Show the dialog
        dialog.showAndWait();
    }

    public void updateData() {
        List<TimeTracker> trackers = controller.getAllTimeTrackers();
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        List<DataStorage.AppUsageData> appDataList = trackers.stream()
                .map(tracker -> {
                    Application app = tracker.getApplication();
                    String name = app.getName();
                    int pid = app.getProcessId();
                    String usageTime = tracker.getFormattedTotalTime();
                    Limit limit = controller.getLimit(app);

                    DataStorage.AppUsageData appData = new DataStorage.AppUsageData(
                            name,
                            pid,
                            app.getExecutablePath(),
                            usageTime,
                            limit != null ? limit.getType().getDisplayName() : "Không giới hạn",
                            today
                    );
                    appData.calculateUsageTimeMillis();
                    appData.setApplication(app);
                    return appData;
                })
                .toList();

        masterData.clear();
        masterData.addAll(appDataList);

        saveDataToJson();
        updateStatistics();
    }

    private void updateStatistics() {
        if (totalAppsLabel != null) {
            totalAppsLabel.setText(String.valueOf(masterData.size()));
        }

        if (limitedAppsLabel != null) {
            long limitedCount = masterData.stream()
                    .filter(app -> !app.getLimitInfo().equals("Không giới hạn"))
                    .count();
            limitedAppsLabel.setText(String.valueOf(limitedCount));
        }

        if (topUsageLabel != null) {
            masterData.stream()
                    .max(Comparator.comparingLong(DataStorage.AppUsageData::getUsageTimeMillis))
                    .ifPresentOrElse(
                            app -> topUsageLabel.setText(app.getName() + " (" + app.getUsageTime() + ")"),
                            () -> topUsageLabel.setText("--")
                    );
        }
    }

    private void saveDataToJson() {
        List<DataStorage.AppUsageData> jsonData = new ArrayList<>(masterData);

        if (!jsonData.isEmpty()) dataStorage.saveAppUsageData(jsonData);
    }

    private void loadSavedData() {
        List<DataStorage.AppUsageData> savedData = dataStorage.getTodayUsageData();

        if (savedData != null && !savedData.isEmpty()) {
            List<DataStorage.AppUsageData> convertedData = new ArrayList<>();
            for (DataStorage.AppUsageData item : savedData) {
                Application app = new Application(item.getName(), item.getProcessId(), item.getExecutablePath());

                Limit limit = controller.getLimit(app);

                DataStorage.AppUsageData appData = new DataStorage.AppUsageData(
                        item.getName(),
                        item.getProcessId(),
                        item.getExecutablePath(),
                        item.getUsageTime(),
                        limit != null ? limit.getType().getDisplayName() : "Không giới hạn",
                        item.getDate()
                );

                appData.calculateUsageTimeMillis();

                appData.setApplication(app);

                convertedData.add(appData);
            }
            masterData.addAll(convertedData);
        }

        updateStatistics();
    }

    public Node getContent() {
        return content;
    }

    private void updateSearchFilter(String searchText) {
        filteredData.setPredicate(app -> {
            if (searchText == null || searchText.isEmpty()) {
                return true;
            }

            String lowerCaseFilter = searchText.toLowerCase();
            if (app.getName().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            }
            if (String.valueOf(app.getProcessId()).contains(lowerCaseFilter)) {
                return true;
            }
            return app.getLimitInfo().toLowerCase().contains(lowerCaseFilter);
        });
    }
}