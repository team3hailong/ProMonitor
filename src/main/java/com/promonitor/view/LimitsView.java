package com.promonitor.view;

import com.promonitor.controller.MainController;
import com.promonitor.model.*;
import com.promonitor.model.enums.LimitType;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LimitsView {
    private final MainController controller;
    private BorderPane content;
    private TableView<LimitInfo> limitsTable;

    public LimitsView(MainController controller) {
        this.controller = controller;
        createContent();
    }

    private void createContent() {
        content = new BorderPane();
        content.setPadding(new Insets(15));
        content.getStyleClass().add("limits-view");

        // Tiêu đề
        Label titleLabel = new Label("Quản lý giới hạn thời gian");
        titleLabel.getStyleClass().add("view-title");
        content.setTop(titleLabel);

        // Bảng giới hạn
        limitsTable = new TableView<>();
        limitsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<LimitInfo, String> targetCol = new TableColumn<>("Đối tượng");
        targetCol.setCellValueFactory(cellData -> cellData.getValue().targetNameProperty());
        targetCol.setPrefWidth(200);

        TableColumn<LimitInfo, String> typeCol = new TableColumn<>("Loại đối tượng");
        typeCol.setCellValueFactory(cellData -> cellData.getValue().targetTypeProperty());
        typeCol.setPrefWidth(120);

        TableColumn<LimitInfo, String> limitTypeCol = new TableColumn<>("Loại giới hạn");
        limitTypeCol.setCellValueFactory(cellData -> cellData.getValue().limitTypeProperty());
        limitTypeCol.setPrefWidth(150);

        TableColumn<LimitInfo, String> limitValueCol = new TableColumn<>("Giá trị giới hạn");
        limitValueCol.setCellValueFactory(cellData -> cellData.getValue().limitValueProperty());
        limitValueCol.setPrefWidth(150);

        TableColumn<LimitInfo, Button> actionsCol = new TableColumn<>("Thao tác");
        actionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Sửa");
            private final Button deleteBtn = new Button("Xóa");
            private final HBox box = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.setOnAction(event -> editLimit(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(event -> deleteLimit(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Button item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });
        actionsCol.setPrefWidth(120);

        limitsTable.getColumns().addAll(targetCol, typeCol, limitTypeCol, limitValueCol, actionsCol);

        VBox.setVgrow(limitsTable, Priority.ALWAYS);
        content.setCenter(limitsTable);

        Button addLimitBtn = new Button("Thêm giới hạn mới");
        addLimitBtn.setOnAction(e -> createNewLimit());

        HBox buttonBar = new HBox(10);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));
        buttonBar.getChildren().add(addLimitBtn);
        content.setBottom(buttonBar);

        loadLimits();
    }

    public void loadLimits() {
        Map<Object, Limit> allLimits = controller.getLimitManager().getAllLimits();
        List<LimitInfo> limitInfos = new ArrayList<>();

        for (Map.Entry<Object, Limit> entry : allLimits.entrySet()) {
            Object target = entry.getKey();
            Limit limit = entry.getValue();

            String targetName;
            String targetType;

            if (target instanceof Application app) {
                targetName = app.getName();
                targetType = "Ứng dụng";
            } else if (target instanceof ApplicationGroup group) {
                targetName = group.getName();
                targetType = "Nhóm";
            } else {
                continue; // Bỏ qua các loại không hỗ trợ
            }

            String limitType = limit.getType().getDisplayName();
            String limitValue;

            if (limit.getType() == LimitType.SCHEDULE) {
                limitValue = "Theo lịch trình";
            } else {
                Duration value = limit.getValue();
                long hours = value.toHours();
                int minutes = value.toMinutesPart();
                limitValue = String.format("%d giờ %d phút", hours, minutes);
            }

            limitInfos.add(new LimitInfo(target, targetName, targetType, limitType, limitValue));
        }

        limitsTable.setItems(FXCollections.observableArrayList(limitInfos));
    }

    public void createNewLimit() {
        LimitCreationDialog dialog = new LimitCreationDialog(controller);
        dialog.showAndWait();
        loadLimits();
    }

    public void editLimit(LimitInfo limitInfo) {
        LimitEditDialog dialog = new LimitEditDialog(limitInfo, controller);
        dialog.showAndWait();
        loadLimits();
    }

    private void deleteLimit(LimitInfo limitInfo) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận xóa");
        alert.setHeaderText("Xóa giới hạn cho: " + limitInfo.getTargetName());
        alert.setContentText("Bạn có chắc chắn muốn xóa giới hạn này?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Object target = limitInfo.getTarget();

                if (target instanceof Application) {
                    controller.getLimitManager().removeLimit((Application) target);
                } else if (target instanceof ApplicationGroup) {
                    controller.getLimitManager().removeLimit((ApplicationGroup) target);
                }

                loadLimits();
            }
        });
    }

    public Node getContent() {
        return content;
    }

    public static class LimitInfo {
        private final Object target;
        private final javafx.beans.property.StringProperty targetName;
        private final javafx.beans.property.StringProperty targetType;
        private final javafx.beans.property.StringProperty limitType;
        private final javafx.beans.property.StringProperty limitValue;

        public LimitInfo(Object target, String targetName, String targetType, String limitType, String limitValue) {
            this.target = target;
            this.targetName = new javafx.beans.property.SimpleStringProperty(targetName);
            this.targetType = new javafx.beans.property.SimpleStringProperty(targetType);
            this.limitType = new javafx.beans.property.SimpleStringProperty(limitType);
            this.limitValue = new javafx.beans.property.SimpleStringProperty(limitValue);
        }

        public Object getTarget() {
            return target;
        }

        public String getTargetName() {
            return targetName.get();
        }

        public javafx.beans.property.StringProperty targetNameProperty() {
            return targetName;
        }

        public String getTargetType() {
            return targetType.get();
        }

        public javafx.beans.property.StringProperty targetTypeProperty() {
            return targetType;
        }

        public String getLimitType() {
            return limitType.get();
        }

        public javafx.beans.property.StringProperty limitTypeProperty() {
            return limitType;
        }

        public String getLimitValue() {
            return limitValue.get();
        }

        public javafx.beans.property.StringProperty limitValueProperty() {
            return limitValue;
        }
    }
}
