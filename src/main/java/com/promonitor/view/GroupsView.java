package com.promonitor.view;

import com.promonitor.controller.MainController;
import com.promonitor.model.Application;
import com.promonitor.model.ApplicationGroup;
import com.promonitor.model.Limit;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.util.Optional;

public class GroupsView {
    private final MainController controller;
    private BorderPane content;
    private ListView<ApplicationGroup> groupsListView;
    private ListView<Application> groupAppsListView;
    private ComboBox<Application> appComboBox;
    private Label selectedGroupLabel;
    private ApplicationGroup selectedGroup;

    public GroupsView(MainController controller) {
        this.controller = controller;
        createContent();
    }

    private void createContent() {
        content = new BorderPane();
        content.setPadding(new Insets(15));
        content.getStyleClass().add("groups-view");

        Label titleLabel = new Label("Quản lý nhóm ứng dụng");
        titleLabel.getStyleClass().add("view-title");
        content.setTop(titleLabel);

        SplitPane splitPane = new SplitPane();

        VBox leftPane = createLeftPane();

        VBox rightPane = createRightPane();

        splitPane.getItems().addAll(leftPane, rightPane);
        splitPane.setDividerPositions(0.3);
        SplitPane.setResizableWithParent(leftPane, false);

        content.setCenter(splitPane);

        // Cập nhật dữ liệu ban đầu
        updateGroupList();
    }

    private VBox createLeftPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(10));

        Label groupsLabel = new Label("Danh sách nhóm");
        groupsLabel.getStyleClass().add("section-header");

        groupsListView = new ListView<>();
        groupsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ApplicationGroup group, boolean empty) {
                super.updateItem(group, empty);
                if (empty || group == null) {
                    setText(null);
                } else {
                    setText(group.getName() + " (" + group.getApplicationCount() + ")");
                }
            }
        });

        groupsListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> handleGroupSelection(newVal));

        HBox buttonBox = new HBox(10);

        Button newGroupBtn = new Button("Tạo nhóm");
        newGroupBtn.setOnAction(e -> createNewGroup());

        Button deleteGroupBtn = new Button("Xóa nhóm");
        deleteGroupBtn.setOnAction(e -> deleteSelectedGroup());
        deleteGroupBtn.disableProperty().bind(
                groupsListView.getSelectionModel().selectedItemProperty().isNull());

        buttonBox.getChildren().addAll(newGroupBtn, deleteGroupBtn);
        VBox.setVgrow(groupsListView, Priority.ALWAYS);
        pane.getChildren().addAll(groupsLabel, groupsListView, buttonBox);

        return pane;
    }

    private VBox createRightPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(10));

        selectedGroupLabel = new Label("Chưa chọn nhóm");
        selectedGroupLabel.getStyleClass().add("group-title");

        HBox addAppBox = new HBox(10);
        addAppBox.setAlignment(Pos.CENTER_LEFT);

        Label addAppLabel = new Label("Thêm ứng dụng:");
        appComboBox = new ComboBox<>();
        appComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Application app) {
                return app != null ? app.getName() : "";
            }

            @Override
            public Application fromString(String string) {
                return null; // Not used
            }
        });
        appComboBox.setPromptText("Chọn ứng dụng");

        Button addAppBtn = new Button("Thêm");
        addAppBtn.setOnAction(e -> addAppToSelectedGroup());
        addAppBtn.disableProperty().bind(appComboBox.getSelectionModel().selectedItemProperty().isNull());

        addAppBox.getChildren().addAll(addAppLabel, appComboBox, addAppBtn);

        // Danh sách ứng dụng trong nhóm
        Label appsInGroupLabel = new Label("Ứng dụng trong nhóm:");
        appsInGroupLabel.getStyleClass().add("section-header");

        groupAppsListView = new ListView<>();
        groupAppsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Application app, boolean empty) {
                super.updateItem(app, empty);
                if (empty || app == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(app.getName());

                    Button removeBtn = new Button("Xóa");
                    removeBtn.setOnAction(e -> removeAppFromGroup(app));

                    setGraphic(removeBtn);
                }
            }
        });

        // Button giới hạn nhóm
        Button setLimitBtn = new Button("Đặt giới hạn cho nhóm");
        setLimitBtn.setOnAction(e -> setGroupLimit());

        // Layout
        VBox.setVgrow(groupAppsListView, Priority.ALWAYS);
        pane.getChildren().addAll(selectedGroupLabel, addAppBox, appsInGroupLabel,
                groupAppsListView, setLimitBtn);

        return pane;
    }

    private void handleGroupSelection(ApplicationGroup group) {
        selectedGroup = group;

        if (group != null) {
            selectedGroupLabel.setText(group.getName());
            groupAppsListView.setItems(group.getApplications());

            // Cập nhật combobox với các ứng dụng không thuộc nhóm
            updateAppComboBox();
        } else {
            selectedGroupLabel.setText("Chưa chọn nhóm");
            groupAppsListView.setItems(FXCollections.observableArrayList());
        }
    }

    private void updateAppComboBox() {
        if (selectedGroup == null) {
            appComboBox.setItems(FXCollections.observableArrayList());
            return;
        }

        // Lấy tất cả ứng dụng đang được theo dõi
        ObservableList<Application> allApps = controller.getApplications();

        // Lọc các ứng dụng không thuộc nhóm hiện tại
        ObservableList<Application> availableApps = allApps.filtered(
                app -> !selectedGroup.getApplications().contains(app)
        );

        appComboBox.setItems(availableApps);
    }

    private void createNewGroup() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Tạo nhóm mới");
        dialog.setHeaderText("Nhập tên cho nhóm mới");
        dialog.setContentText("Tên nhóm:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                ApplicationGroup newGroup = controller.createGroup(name.trim());
                updateGroupList();
                groupsListView.getSelectionModel().select(newGroup);
            }
        });
    }

    private void deleteSelectedGroup() {
        if (selectedGroup == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận xóa");
        alert.setHeaderText("Xóa nhóm: " + selectedGroup.getName());
        alert.setContentText("Bạn có chắc chắn muốn xóa nhóm này?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            controller.deleteGroup(selectedGroup);
            updateGroupList();
        }
    }

    private void addAppToSelectedGroup() {
        if (selectedGroup == null) return;

        Application selectedApp = appComboBox.getSelectionModel().getSelectedItem();
        if (selectedApp != null) {
            controller.addToGroup(selectedGroup, selectedApp);
            updateAppComboBox();
        }
    }

    private void removeAppFromGroup(Application app) {
        if (selectedGroup == null || app == null) return;

        controller.removeFromGroup(selectedGroup, app);
        updateAppComboBox();
    }

    /**
     * Đặt giới hạn thời gian cho nhóm
     */
    private void setGroupLimit() {
        if (selectedGroup == null) return;

        // Trong thực tế sẽ mở một dialog để cấu hình giới hạn
        // LimitSettingDialog dialog = new LimitSettingDialog(selectedGroup, controller);
        // dialog.showAndWait();

        // Trong phiên bản đơn giản, chỉ hiển thị một thông báo
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Đặt giới hạn nhóm");
        alert.setHeaderText("Chức năng đặt giới hạn");
        alert.setContentText("Chức năng đặt giới hạn cho nhóm sẽ được triển khai trong phiên bản tiếp theo.");
        alert.showAndWait();
    }

    private void updateGroupList() {
        groupsListView.setItems(controller.getGroups());
    }

    public Node getContent() {
        return content;
    }
}