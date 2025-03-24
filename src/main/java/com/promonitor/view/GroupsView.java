package com.promonitor.view;

import com.promonitor.controller.MainController;
import com.promonitor.model.Application;
import com.promonitor.model.ApplicationGroup;

import com.promonitor.util.AlertHelper;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

import java.util.Objects;
import java.util.Optional;

public class GroupsView {
    private final MainController controller;
    private final LimitsView limitsView;
    private BorderPane content;
    private ListView<ApplicationGroup> groupsListView;
    private ListView<Application> groupAppsListView;
    private ComboBox<Application> appComboBox;
    private Label selectedGroupLabel;
    private ApplicationGroup selectedGroup;

    public GroupsView(MainController controller, LimitsView limitsView) {
        this.controller = controller;
        this.limitsView = limitsView;
        createContent();
    }

    private void createContent() {
        content = new BorderPane();
        content.setPadding(new Insets(15));
        content.getStyleClass().add("groups-view");

        GridPane headerGrid = new GridPane();
        headerGrid.setHgap(10);
        headerGrid.setVgap(0);
        headerGrid.setAlignment(Pos.CENTER_LEFT);

        FontAwesomeIconView titleIcon = new FontAwesomeIconView(FontAwesomeIcon.OBJECT_GROUP);
        titleIcon.setGlyphSize(30);
        titleIcon.setFill(Color.valueOf("#4a6bff"));
        titleIcon.getStyleClass().add("icon-with-text");
        titleIcon.setTranslateY(-5);

        StackPane iconContainer = new StackPane(titleIcon);
        iconContainer.setAlignment(Pos.CENTER);
        iconContainer.setMinHeight(48);

        headerGrid.add(iconContainer, 0, 0, 1, 2);

        Label titleLabel = new Label("Quản lý nhóm ứng dụng");
        titleLabel.getStyleClass().add("view-title");
        headerGrid.add(titleLabel, 1, 0);

        Label descriptionLabel = new Label("Nhóm các ứng dụng để quản lý và thiết lập giới hạn thời gian chung");
        descriptionLabel.getStyleClass().add("description-text");
        headerGrid.add(descriptionLabel, 1, 1);

        headerGrid.setPadding(new Insets(0, 0, 10, 0));

        content.setTop(headerGrid);

        SplitPane splitPane = new SplitPane();

        VBox leftPane = createLeftPane();
        VBox rightPane = createRightPane();

        splitPane.getItems().addAll(leftPane, rightPane);
        splitPane.setDividerPositions(0.32);
        SplitPane.setResizableWithParent(leftPane, false);

        content.setCenter(splitPane);
        updateGroupList();
    }

    private VBox createLeftPane() {
        VBox pane = new VBox(15);
        pane.setPadding(new Insets(10));
        pane.getStyleClass().add("left-pane");

        HBox groupsHeaderBox = new HBox(8);
        groupsHeaderBox.setAlignment(Pos.CENTER_LEFT);

        FontAwesomeIconView groupsIcon = new FontAwesomeIconView(FontAwesomeIcon.FOLDER_OPEN);
        groupsIcon.setGlyphSize(16);
        groupsIcon.setFill(Color.valueOf("#4a6bff"));

        Label groupsLabel = new Label("Danh sách nhóm");
        groupsLabel.getStyleClass().add("section-header");

        groupsHeaderBox.getChildren().addAll(groupsIcon, groupsLabel);

        TextField searchField = new TextField();
        searchField.setPromptText("Tìm kiếm nhóm...");
        searchField.getStyleClass().add("search-field");

        FontAwesomeIconView searchIcon = new FontAwesomeIconView(FontAwesomeIcon.SEARCH);
        searchIcon.setGlyphSize(12);
        searchIcon.setFill(Color.valueOf("#9aa0a6"));
        StackPane searchIconPane = new StackPane(searchIcon);
        searchIconPane.setStyle("-fx-padding: 0 0 0 8;");

        searchField.textProperty().addListener((obs, oldText, newText) -> filterGroups(newText));

        groupsListView = new ListView<>();
        groupsListView.getStyleClass().add("groups-list-view");
        groupsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ApplicationGroup group, boolean empty) {
                super.updateItem(group, empty);
                if (empty || group == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox cellBox = new HBox(10);
                    cellBox.setAlignment(Pos.CENTER_LEFT);

                    FontAwesomeIconView folderIcon = new FontAwesomeIconView(FontAwesomeIcon.FOLDER);
                    folderIcon.setGlyphSize(16);
                    folderIcon.setFill(Color.valueOf("#e67e22"));

                    VBox textBox = new VBox(2);
                    Label nameLabel = new Label(group.getName());
                    nameLabel.getStyleClass().add("group-name-label");

                    Label countLabel = new Label(group.getApplicationCount() + " ứng dụng");
                    countLabel.getStyleClass().add("group-count-label");

                    textBox.getChildren().addAll(nameLabel, countLabel);
                    cellBox.getChildren().addAll(folderIcon, textBox);

                    setGraphic(cellBox);
                    setText(null);
                }
            }
        });

        groupsListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> handleGroupSelection(newVal));

        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(5, 0, 0, 0));

        Button newGroupBtn = new Button("Tạo nhóm");
        newGroupBtn.getStyleClass().add("primary-button");

        FontAwesomeIconView addIcon = new FontAwesomeIconView(FontAwesomeIcon.PLUS);
        addIcon.setGlyphSize(12);
        addIcon.setFill(Color.WHITE);
        newGroupBtn.setGraphic(addIcon);
        newGroupBtn.setGraphicTextGap(8);

        newGroupBtn.setOnAction(e -> createNewGroup());

        Button deleteGroupBtn = new Button("Xóa nhóm");
        deleteGroupBtn.getStyleClass().add("danger-button");

        FontAwesomeIconView deleteIcon = new FontAwesomeIconView(FontAwesomeIcon.TRASH);
        deleteIcon.setGlyphSize(12);
        deleteIcon.setFill(Color.WHITE);
        deleteGroupBtn.setGraphic(deleteIcon);
        deleteGroupBtn.setGraphicTextGap(8);

        deleteGroupBtn.setOnAction(e -> deleteSelectedGroup());
        deleteGroupBtn.disableProperty().bind(
                groupsListView.getSelectionModel().selectedItemProperty().isNull());

        buttonBox.getChildren().addAll(newGroupBtn, deleteGroupBtn);

        VBox.setVgrow(groupsListView, Priority.ALWAYS);
        pane.getChildren().addAll(groupsHeaderBox, searchField, groupsListView, buttonBox);

        return pane;
    }

    private VBox createRightPane() {
        VBox pane = new VBox(15);
        pane.setPadding(new Insets(10));
        pane.getStyleClass().add("right-pane");

        HBox groupTitleBox = new HBox();
        groupTitleBox.setAlignment(Pos.CENTER_LEFT);

        FontAwesomeIconView groupIcon = new FontAwesomeIconView(FontAwesomeIcon.FOLDER_OPEN);
        groupIcon.setGlyphSize(18);
        groupIcon.setFill(Color.valueOf("#e67e22"));

        selectedGroupLabel = new Label("Chưa chọn nhóm");
        selectedGroupLabel.getStyleClass().add("group-title");
        selectedGroupLabel.setGraphic(groupIcon);
        selectedGroupLabel.setGraphicTextGap(10);

        HBox addAppBox = new HBox(10);
        addAppBox.setAlignment(Pos.CENTER_LEFT);

        appComboBox = new ComboBox<>();
        appComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Application app) {
                return app != null ? app.getName() : "";
            }

            @Override
            public Application fromString(String string) {
                return null;
            }
        });
        appComboBox.setPromptText("Chọn ứng dụng");
        appComboBox.setPrefWidth(250);
        appComboBox.getStyleClass().add("app-combo-box");

        Button addAppBtn = new Button("Thêm vào nhóm");
        addAppBtn.getStyleClass().add("primary-button");

        FontAwesomeIconView plusIcon = new FontAwesomeIconView(FontAwesomeIcon.PLUS);
        plusIcon.setGlyphSize(12);
        plusIcon.setFill(Color.WHITE);
        addAppBtn.setGraphic(plusIcon);
        addAppBtn.setGraphicTextGap(8);

        addAppBtn.setOnAction(e -> addAppToSelectedGroup());
        addAppBtn.disableProperty().bind(appComboBox.getSelectionModel().selectedItemProperty().isNull());

        addAppBox.getChildren().addAll(appComboBox, addAppBtn);

        HBox appsInGroupHeaderBox = new HBox(8);
        appsInGroupHeaderBox.setAlignment(Pos.CENTER_LEFT);

        FontAwesomeIconView appsIcon = new FontAwesomeIconView(FontAwesomeIcon.LIST);
        appsIcon.setGlyphSize(14);
        appsIcon.setFill(Color.valueOf("#4a6bff"));

        Label appsInGroupLabel = new Label("Ứng dụng trong nhóm");
        appsInGroupLabel.getStyleClass().add("section-header");

        appsInGroupHeaderBox.getChildren().addAll(appsIcon, appsInGroupLabel);

        groupAppsListView = new ListView<>();
        groupAppsListView.getStyleClass().add("apps-list-view");
        groupAppsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Application app, boolean empty) {
                super.updateItem(app, empty);
                if (empty || app == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox cellBox = new HBox();
                    cellBox.setAlignment(Pos.CENTER_LEFT);
                    cellBox.setSpacing(10);

                    FontAwesomeIconView appIcon = new FontAwesomeIconView(FontAwesomeIcon.WINDOW_MAXIMIZE);
                    appIcon.setGlyphSize(14);
                    appIcon.setFill(Color.valueOf("#27ae60"));

                    Label appNameLabel = new Label(app.getName());
                    appNameLabel.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(appNameLabel, Priority.ALWAYS);

                    Button removeBtn = new Button();
                    removeBtn.getStyleClass().add("icon-button");

                    FontAwesomeIconView removeIcon = new FontAwesomeIconView(FontAwesomeIcon.TIMES);
                    removeIcon.setGlyphSize(12);
                    removeIcon.setFill(Color.valueOf("#e74c3c"));
                    removeBtn.setGraphic(removeIcon);

                    Tooltip tooltip = new Tooltip("Xóa khỏi nhóm");
                    Tooltip.install(removeBtn, tooltip);

                    removeBtn.setOnAction(e -> removeAppFromGroup(app));

                    cellBox.getChildren().addAll(appIcon, appNameLabel, removeBtn);
                    setGraphic(cellBox);
                    setText(null);
                }
            }
        });

        // Empty placeholder for apps list
        Label emptyPlaceholder = new Label("Không có ứng dụng nào trong nhóm");
        emptyPlaceholder.getStyleClass().add("empty-placeholder");

        FontAwesomeIconView emptyIcon = new FontAwesomeIconView(FontAwesomeIcon.INFO_CIRCLE);
        emptyIcon.setGlyphSize(16);
        emptyIcon.setFill(Color.valueOf("#9aa0a6"));

        emptyPlaceholder.setGraphic(emptyIcon);
        emptyPlaceholder.setGraphicTextGap(8);

        groupAppsListView.setPlaceholder(emptyPlaceholder);

        // Set group limit button
        Button setLimitBtn = new Button("Đặt giới hạn cho nhóm");
        setLimitBtn.getStyleClass().add("accent-button");

        FontAwesomeIconView limitIcon = new FontAwesomeIconView(FontAwesomeIcon.CLOCK_ALT);
        limitIcon.setGlyphSize(12);
        limitIcon.setFill(Color.WHITE);
        setLimitBtn.setGraphic(limitIcon);
        setLimitBtn.setGraphicTextGap(8);

        setLimitBtn.setOnAction(e -> setGroupLimit());
        setLimitBtn.disableProperty().bind(
                groupsListView.getSelectionModel().selectedItemProperty().isNull());

        // Empty right pane placeholder
        VBox emptyRightPane = new VBox();
        emptyRightPane.setAlignment(Pos.CENTER);
        emptyRightPane.setPadding(new Insets(50, 0, 0, 0));
        emptyRightPane.setVisible(false);

        FontAwesomeIconView bigEmptyIcon = new FontAwesomeIconView(FontAwesomeIcon.FOLDER_OPEN_ALT);
        bigEmptyIcon.setGlyphSize(48);
        bigEmptyIcon.setFill(Color.valueOf("#dadce0"));

        Label selectGroupLabel = new Label("Chọn một nhóm từ danh sách");
        selectGroupLabel.getStyleClass().add("empty-right-pane-label");

        Label instructionLabel = new Label("Hoặc tạo một nhóm mới để bắt đầu");
        instructionLabel.getStyleClass().add("empty-right-pane-instruction");

        emptyRightPane.getChildren().addAll(bigEmptyIcon, selectGroupLabel, instructionLabel);

        // Add all components to right pane
        VBox groupContentPane = new VBox(15);
        groupContentPane.getChildren().addAll(
                groupTitleBox,
                addAppBox,
                appsInGroupHeaderBox,
                groupAppsListView,
                setLimitBtn
        );

        StackPane rightStackPane = new StackPane();
        rightStackPane.getChildren().addAll(emptyRightPane, groupContentPane);

        VBox.setVgrow(groupAppsListView, Priority.ALWAYS);
        pane.getChildren().add(rightStackPane);

        // Show/hide right pane content based on selection
        groupsListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            boolean hasSelection = newVal != null;
            groupContentPane.setVisible(hasSelection);
            emptyRightPane.setVisible(!hasSelection);
        });

        // Initialize with empty state
        groupContentPane.setVisible(false);
        emptyRightPane.setVisible(true);

        return pane;
    }

    private void filterGroups(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            updateGroupList();
            return;
        }

        ObservableList<ApplicationGroup> allGroups = controller.getGroups();

        ObservableList<ApplicationGroup> filtered = allGroups.filtered(
                group -> group.getName().toLowerCase().contains(searchText.toLowerCase())
        );

        groupsListView.setItems(filtered);
    }

    private void handleGroupSelection(ApplicationGroup group) {
        selectedGroup = group;

        if (group != null) {
            selectedGroupLabel.setText(group.getName());
            groupAppsListView.setItems(group.getApplications());
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

        ObservableList<Application> allApps = controller.getApplications();

        ObservableList<Application> availableApps = allApps.filtered(
                app -> !selectedGroup.getApplications().contains(app)
        );

        appComboBox.setItems(availableApps);
    }

    private void createNewGroup() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Tạo nhóm mới");
        dialog.setHeaderText("Nhập tên cho nhóm mới");

        ButtonType createButtonType = new ButtonType("Tạo", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Tên nhóm");

        grid.add(new Label("Tên nhóm:"), 0, 0);
        grid.add(nameField, 1, 0);

        dialog.getDialogPane().setContent(grid);

        Node createButton = dialog.getDialogPane().lookupButton(createButtonType);
        createButton.setDisable(true);

        nameField.textProperty().addListener((observable, oldValue, newValue) -> createButton.setDisable(newValue.trim().isEmpty()));

        dialog.getDialogPane().getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/style.css")).toExternalForm());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                return nameField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                ApplicationGroup newGroup = controller.createGroup(name.trim());
                updateGroupList();
                groupsListView.getSelectionModel().select(newGroup);
                updateStatsLabels();
            }
        });
    }

    private void deleteSelectedGroup() {
        if (selectedGroup == null) return;

        Label headerLabel = new Label("Xóa nhóm ứng dụng");
        headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label messageLabel = new Label(
                "Bạn có chắc chắn muốn xóa nhóm \"" + selectedGroup.getName() + "\"?\n" +
                        "Các ứng dụng trong nhóm sẽ không bị xóa, nhưng mọi giới hạn được thiết lập cho nhóm này sẽ bị hủy bỏ."
        );
        messageLabel.setStyle("-fx-font-size: 14px;");
        messageLabel.setWrapText(true);

        if (AlertHelper.createConfirmationContent(headerLabel, messageLabel)) {
            controller.deleteGroup(selectedGroup);
            updateGroupList();
            updateStatsLabels();
        }
    }

    private void addAppToSelectedGroup() {
        if (selectedGroup == null) return;

        Application selectedApp = appComboBox.getSelectionModel().getSelectedItem();
        if (selectedApp != null) {
            controller.updateGroup(selectedGroup, selectedApp, true);
            updateAppComboBox();

            showNotification("Đã thêm " + selectedApp.getName() + " vào nhóm " + selectedGroup.getName(), "success");
        }
    }

    private void removeAppFromGroup(Application app) {
        if (selectedGroup == null || app == null) return;

        controller.updateGroup(selectedGroup, app, false);
        updateAppComboBox();

        showNotification("Đã xóa " + app.getName() + " khỏi nhóm " + selectedGroup.getName(), "info");
    }

    private void setGroupLimit() {
        if (selectedGroup == null) return;
        LimitCreationDialog dialog = new LimitCreationDialog(controller, true);
        dialog.showAndWait();
        limitsView.loadLimits();
    }

    private void updateGroupList() {
        groupsListView.setItems(controller.getGroups());
        updateStatsLabels();
    }

    private void updateStatsLabels() {
        Label totalGroupsLabel = (Label) content.lookup("#totalGroupsLabel");
        if (totalGroupsLabel != null) {
            totalGroupsLabel.setText("Tổng số nhóm: " + controller.getGroups().size());
        }

        int totalApps = 0;
        for (ApplicationGroup group : controller.getGroups()) {
            totalApps += group.getApplicationCount();
        }

        Label totalAppsLabel = (Label) content.lookup("#totalAppsLabel");
        if (totalAppsLabel != null) {
            totalAppsLabel.setText("Tổng số ứng dụng trong nhóm: " + totalApps);
        }
    }

    private void showNotification(String message, String type) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setTitle("Thông báo");

        FontAwesomeIcon icon;
        Color iconColor;

        if (type.equals("success")) {
            icon = FontAwesomeIcon.CHECK_CIRCLE;
            iconColor = Color.valueOf("#2ecc71");
        } else {
            icon = FontAwesomeIcon.INFO_CIRCLE;
            iconColor = Color.valueOf("#3498db");
        }

        FontAwesomeIconView iconView = new FontAwesomeIconView(icon);
        iconView.setGlyphSize(24);
        iconView.setFill(iconColor);

        HBox content = new HBox(15, iconView, new Label(message));
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(10));

        alert.getDialogPane().setContent(content);
        alert.getDialogPane().getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/style.css")).toExternalForm());

        alert.show();
    }

    public Node getContent() {
        return content;
    }
}