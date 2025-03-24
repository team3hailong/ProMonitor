package com.promonitor.view;

import com.promonitor.controller.MainController;
import com.promonitor.model.*;
import com.promonitor.model.enums.LimitType;

import com.promonitor.util.AlertHelper;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;

import java.time.Duration;
import java.util.*;

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
        content.setPadding(new Insets(20));
        content.getStyleClass().add("limits-view");

        VBox topSection = new VBox(15);

        GridPane headerGrid = new GridPane();
        headerGrid.setHgap(10);
        headerGrid.setVgap(2);
        headerGrid.setAlignment(Pos.CENTER_LEFT);

        FontAwesomeIconView headerIcon = new FontAwesomeIconView(FontAwesomeIcon.CLOCK_ALT);
        headerIcon.setGlyphSize(30);
        headerIcon.setFill(Color.valueOf("#4a6bff"));

        StackPane iconContainer = new StackPane(headerIcon);
        iconContainer.setAlignment(Pos.CENTER);
        iconContainer.setMinHeight(40);

        headerGrid.add(iconContainer, 0, 0, 1, 2);

        Label titleLabel = new Label("Quản lý giới hạn thời gian");
        titleLabel.getStyleClass().add("view-title");
        headerGrid.add(titleLabel, 1, 0);

        Label descriptionLabel = new Label("Thiết lập giới hạn thời gian cho ứng dụng và nhóm ứng dụng");
        descriptionLabel.getStyleClass().add("description-text");
        headerGrid.add(descriptionLabel, 1, 1);

        Region spacer = new Region();
        GridPane.setHgrow(spacer, Priority.ALWAYS);
        headerGrid.add(spacer, 2, 0, 1, 2);

        Button addLimitBtn = new Button();
        addLimitBtn.getStyleClass().addAll("add-button");

        FontAwesomeIconView addIcon = new FontAwesomeIconView(FontAwesomeIcon.PLUS);
        addIcon.setGlyphSize(16);
        addIcon.setFill(Color.WHITE);
        addLimitBtn.setGraphic(addIcon);

        Tooltip addTooltip = new Tooltip("Thêm giới hạn mới");
        addTooltip.getStyleClass().add("custom-tooltip");
        Tooltip.install(addLimitBtn, addTooltip);

        addLimitBtn.setOnAction(e -> createNewLimit());
        headerGrid.add(addLimitBtn, 3, 0, 1, 2);

        GridPane.setValignment(addLimitBtn, VPos.CENTER);

        headerGrid.setPadding(new Insets(0, 0, 10, 0));

        topSection.getChildren().addAll(headerGrid);
        content.setTop(topSection);

        VBox tableContainer = new VBox(15);
        tableContainer.getStyleClass().add("table-container");
        tableContainer.setPadding(new Insets(10, 0, 0, 0));

        limitsTable = new TableView<>();
        limitsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        limitsTable.getStyleClass().add("limits-table");

        TableColumn<LimitInfo, String> targetCol = new TableColumn<>("Đối tượng");
        targetCol.setCellValueFactory(cellData -> cellData.getValue().targetNameProperty());
        targetCol.setCellFactory(createTargetCellFactory());
        targetCol.setPrefWidth(220);

        TableColumn<LimitInfo, String> typeCol = new TableColumn<>("Loại");
        typeCol.setCellValueFactory(cellData -> cellData.getValue().targetTypeProperty());
        typeCol.setCellFactory(createTypeCellFactory());
        typeCol.setPrefWidth(120);

        TableColumn<LimitInfo, String> limitTypeCol = new TableColumn<>("Loại giới hạn");
        limitTypeCol.setCellValueFactory(cellData -> cellData.getValue().limitTypeProperty());
        limitTypeCol.setCellFactory(createLimitTypeCellFactory());
        limitTypeCol.setPrefWidth(150);

        TableColumn<LimitInfo, String> limitValueCol = new TableColumn<>("Giá trị giới hạn");
        limitValueCol.setCellValueFactory(cellData -> cellData.getValue().limitValueProperty());
        limitValueCol.setCellFactory(createLimitValueCellFactory());
        limitValueCol.setPrefWidth(150);

        TableColumn<LimitInfo, Button> actionsCol = new TableColumn<>("Thao tác");
        actionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button();
            private final Button deleteBtn = new Button();
            private final HBox box = new HBox(8, editBtn, deleteBtn);

            {
                FontAwesomeIconView editIcon = new FontAwesomeIconView(FontAwesomeIcon.EDIT);
                editIcon.setGlyphSize(14);
                editIcon.setFill(Color.valueOf("#4a6bff"));
                editBtn.setGraphic(editIcon);
                editBtn.getStyleClass().add("icon-button");
                editBtn.setTooltip(new Tooltip("Chỉnh sửa"));

                FontAwesomeIconView deleteIcon = new FontAwesomeIconView(FontAwesomeIcon.TRASH);
                deleteIcon.setGlyphSize(14);
                deleteIcon.setFill(Color.valueOf("#e74c3c"));
                deleteBtn.setGraphic(deleteIcon);
                deleteBtn.getStyleClass().add("icon-button");
                deleteBtn.setTooltip(new Tooltip("Xóa"));

                box.setAlignment(Pos.CENTER);

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

        assert limitsTable != null;
        limitsTable.getColumns().addAll(targetCol, typeCol, limitTypeCol, limitValueCol, actionsCol);

        Label placeholderLabel = new Label("Chưa có giới hạn nào được thiết lập");
        placeholderLabel.getStyleClass().add("empty-table-label");

        FontAwesomeIconView emptyIcon = new FontAwesomeIconView(FontAwesomeIcon.INFO_CIRCLE);
        emptyIcon.setGlyphSize(24);
        emptyIcon.setFill(Color.valueOf("#dadce0"));

        VBox placeholderBox = new VBox(10);
        placeholderBox.setAlignment(Pos.CENTER);
        placeholderBox.getChildren().addAll(emptyIcon, placeholderLabel);
        limitsTable.setPlaceholder(placeholderBox);

        VBox.setVgrow(limitsTable, Priority.ALWAYS);

        tableContainer.getChildren().add(limitsTable);
        assert content != null;
        content.setCenter(tableContainer);

        loadLimits();
    }

    private Callback<TableColumn<LimitInfo, String>, TableCell<LimitInfo, String>> createTargetCellFactory() {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox hBox = new HBox(10);
                    hBox.setAlignment(Pos.CENTER_LEFT);

                    FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.WINDOW_MAXIMIZE);
                    icon.setGlyphSize(14);
                    icon.setFill(Color.valueOf("#4a6bff"));

                    Label label = new Label(item);
                    hBox.getChildren().addAll(icon, label);

                    setGraphic(hBox);
                    setText(null);
                }
            }
        };
    }

    private Callback<TableColumn<LimitInfo, String>, TableCell<LimitInfo, String>> createTypeCellFactory() {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox hBox = new HBox(10);
                    hBox.setAlignment(Pos.CENTER_LEFT);

                    FontAwesomeIconView icon;
                    Color iconColor;

                    if (item.equals("Ứng dụng")) {
                        icon = new FontAwesomeIconView(FontAwesomeIcon.DESKTOP);
                        iconColor = Color.valueOf("#27ae60");
                    } else {
                        icon = new FontAwesomeIconView(FontAwesomeIcon.OBJECT_GROUP);
                        iconColor = Color.valueOf("#e67e22");
                    }

                    icon.setGlyphSize(14);
                    icon.setFill(iconColor);

                    Label label = new Label(item);
                    hBox.getChildren().addAll(icon, label);

                    setGraphic(hBox);
                    setText(null);
                }
            }
        };
    }

    private Callback<TableColumn<LimitInfo, String>, TableCell<LimitInfo, String>> createLimitTypeCellFactory() {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox hBox = new HBox(10);
                    hBox.setAlignment(Pos.CENTER_LEFT);

                    FontAwesomeIconView icon;
                    Color iconColor;

                    if (item.contains("ngày")) {
                        icon = new FontAwesomeIconView(FontAwesomeIcon.CALENDAR_CHECK_ALT);
                        iconColor = Color.valueOf("#3498db");
                    } else if (item.contains("tuần")) {
                        icon = new FontAwesomeIconView(FontAwesomeIcon.CALENDAR_ALT);
                        iconColor = Color.valueOf("#9b59b6");
                    } else {
                        icon = new FontAwesomeIconView(FontAwesomeIcon.CLOCK_ALT);
                        iconColor = Color.valueOf("#e74c3c");
                    }

                    icon.setGlyphSize(14);
                    icon.setFill(iconColor);

                    Label label = new Label(item);
                    hBox.getChildren().addAll(icon, label);

                    setGraphic(hBox);
                    setText(null);
                }
            }
        };
    }

    private Callback<TableColumn<LimitInfo, String>, TableCell<LimitInfo, String>> createLimitValueCellFactory() {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox hBox = new HBox(10);
                    hBox.setAlignment(Pos.CENTER_LEFT);

                    FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.HOURGLASS_HALF);
                    icon.setGlyphSize(14);
                    icon.setFill(Color.valueOf("#7f8c8d"));

                    Label label = new Label(item);

                    // Add badge styling for schedule type
                    if (item.equals("Theo lịch trình")) {
                        Label badge = new Label(item);
                        badge.getStyleClass().add("schedule-badge");
                        hBox.getChildren().addAll(icon, badge);
                    } else {
                        hBox.getChildren().addAll(icon, label);
                    }

                    setGraphic(hBox);
                    setText(null);
                }
            }
        };
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

        // Update stats
        updateStats();
    }

    private void updateStats() {
        int appLimits = 0;
        int groupLimits = 0;

        for (LimitInfo info : limitsTable.getItems()) {
            if (info.getTargetType().equals("Ứng dụng")) {
                appLimits++;
            } else if (info.getTargetType().equals("Nhóm")) {
                groupLimits++;
            }
        }

        // Update counters
        Label appCountLabel = (Label) content.lookup("#countAppLimits");
        if (appCountLabel != null) {
            appCountLabel.setText(String.valueOf(appLimits));
        }

        Label groupCountLabel = (Label) content.lookup("#countGroupLimits");
        if (groupCountLabel != null) {
            groupCountLabel.setText(String.valueOf(groupLimits));
        }
    }

    public void createNewLimit() {
        LimitCreationDialog dialog = new LimitCreationDialog(controller, false);
        dialog.showAndWait();
        loadLimits();
    }

    public void editLimit(LimitInfo limitInfo) {
        LimitEditDialog dialog = new LimitEditDialog(limitInfo, controller);
        dialog.showAndWait();
        loadLimits();
    }

    private void deleteLimit(LimitInfo limitInfo) {
        String targetName = limitInfo.getTargetName();
        Label headerLabel = new Label("Xóa giới hạn thời gian");
        Label messageLabel = new Label("Bạn có chắc chắn muốn xóa giới hạn thời gian của:\n\"" +
                targetName + "\"?\n\nHành động này không thể hoàn tác.");

        if (AlertHelper.createConfirmationContent(headerLabel, messageLabel)) {
            Object target = limitInfo.getTarget();

            if (target instanceof Application) {
                controller.getLimitManager().removeLimit((Application) target);
            } else if (target instanceof ApplicationGroup) {
                controller.getLimitManager().removeLimit((ApplicationGroup) target);
            }

            loadLimits();
            showNotification();
        }
    }

    private void showNotification() {
        AlertHelper.showToast(this.getContent(), "Giới hạn đã được xóa thành công", AlertHelper.ToastType.SUCCESS);
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

        public javafx.beans.property.StringProperty limitTypeProperty() {
            return limitType;
        }

        public javafx.beans.property.StringProperty limitValueProperty() {
            return limitValue;
        }
    }
}