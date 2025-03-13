package com.promonitor.view;

import com.promonitor.controller.MainController;
import com.promonitor.model.Application;
import com.promonitor.model.Limit;
import com.promonitor.model.TimeTracker;
import com.promonitor.view.LimitsView;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.List;
import java.util.stream.Collectors;

public class ApplicationsView {
    private final MainController controller;
    private BorderPane content;
    private TableView<AppUsageData> appsTable;
    private LimitsView limitsView;

    public ApplicationsView(MainController controller, LimitsView limitsView) {
        this.controller = controller;
        this.limitsView = limitsView;
        createContent();
    }

    private void createContent() {
        content = new BorderPane();
        content.setPadding(new Insets(15));
        content.getStyleClass().add("applications-view");

        Label titleLabel = new Label("Quản lý ứng dụng");
        titleLabel.getStyleClass().add("view-title");
        HBox titleBox = new HBox(titleLabel);
        titleBox.setPadding(new Insets(0, 0, 15, 0));
        content.setTop(titleBox);

        appsTable = new TableView<>();
        appsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<AppUsageData, String> nameCol = new TableColumn<>("Tên ứng dụng");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(200);

        TableColumn<AppUsageData, String> pidCol = new TableColumn<>("PID");
        pidCol.setCellValueFactory(new PropertyValueFactory<>("processId"));
        pidCol.setPrefWidth(70);

        TableColumn<AppUsageData, String> timeCol = new TableColumn<>("Thời gian sử dụng");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("usageTime"));
        timeCol.setPrefWidth(150);

        TableColumn<AppUsageData, String> limitCol = new TableColumn<>("Giới hạn");
        limitCol.setCellValueFactory(new PropertyValueFactory<>("limitInfo"));
        limitCol.setPrefWidth(150);

        TableColumn<AppUsageData, String> actionCol = new TableColumn<>("Thao tác");
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button setLimitBtn = new Button("Đặt giới hạn");

            {
                setLimitBtn.setOnAction(event -> {
                    AppUsageData data = getTableView().getItems().get(getIndex());
                    if(!data.getLimitInfo().equals("Không giới hạn")) {
                        LimitsView.LimitInfo limitInfo = new LimitsView.LimitInfo(
                                data.getApplication(),  // Assuming AppUsageData has getApplication() method
                                data.getApplication().getName(),// Or however you get the application name
                                "Ứng dụng",
                                data.getLimitInfo(),
                                null
                        );
                        limitsView.editLimit(limitInfo);
                    }
                    else
                        limitsView.createNewLimit();
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(setLimitBtn);
                }
            }
        });
        actionCol.setPrefWidth(120);

        appsTable.getColumns().addAll(nameCol, pidCol, timeCol, limitCol, actionCol);

        content.setCenter(appsTable);
        updateData();
    }

    public void updateData() {
        List<TimeTracker> trackers = controller.getAllTimeTrackers();
        List<AppUsageData> appDataList = trackers.stream()
                .map(tracker -> {
                    Application app = tracker.getApplication();
                    String name = app.getName();
                    int pid = app.getProcessId();
                    String usageTime = tracker.getFormattedTotalTime();
                    Limit limit = controller.getLimit(app);

                    return new AppUsageData(app, name, pid, usageTime, limit);
                })
                .collect(Collectors.toList());

        appsTable.setItems(FXCollections.observableArrayList(appDataList));
    }

    public Node getContent() {
        return content;
    }

    public static class AppUsageData {
        private final Application application;
        private final String name;
        private final int processId;
        private final String usageTime;
        private final String limitInfo;
        private final Limit limit;

        public AppUsageData(Application application, String name, int processId, String usageTime, Limit limit) {
            this.application = application;
            this.name = name;
            this.processId = processId;
            this.usageTime = usageTime;
            this.limit = limit;
            this.limitInfo = limit != null ?
                limit.getType().getDisplayName() : "Không giới hạn";
        }

        public Application getApplication() {
            return application;
        }

        public String getName() {
            return name;
        }

        public int getProcessId() {
            return processId;
        }

        public String getUsageTime() {
            return usageTime;
        }

        public String getLimitInfo() {
            return limitInfo;
        }
        public Limit getLimit() {
            return limit;
        }
    }
}