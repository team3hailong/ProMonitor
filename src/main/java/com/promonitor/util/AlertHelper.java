package com.promonitor.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.Objects;
import java.util.Optional;


public class AlertHelper {
    public static boolean createConfirmationContent(Label headerLabel, Label messageLabel) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(AlertHelper.class.getResource("/css/limit-dialog.css")).toExternalForm());
        dialog.setTitle("Xác nhận xóa");

        ButtonType deleteButton = new ButtonType("Xóa", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(cancelButton, deleteButton);

        Button deleteBtn = (Button) dialog.getDialogPane().lookupButton(deleteButton);
        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(cancelButton);
        cancelBtn.getStyleClass().add("cancel-button");
        deleteBtn.getStyleClass().add("delete-button");

        VBox content = new VBox(20);
        content.setPadding(new Insets(25));
        content.setStyle("-fx-background-color: white;");
        content.setMinWidth(420);
        content.setMaxWidth(420);
        content.setAlignment(Pos.CENTER);

        FontAwesomeIconView warningIcon = new FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_TRIANGLE);
        warningIcon.setGlyphSize(48);
        warningIcon.setFill(Color.valueOf("#f39c12"));

        DropShadow iconShadow = new DropShadow(10, Color.color(0, 0, 0, 0.2));
        warningIcon.setEffect(iconShadow);

        headerLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        headerLabel.setAlignment(Pos.CENTER);
        headerLabel.setPadding(new Insets(15, 0, 5, 0));

        Rectangle separator = new Rectangle(380, 1);
        separator.setFill(Color.valueOf("#ecf0f1"));
        separator.setOpacity(0.7);

        messageLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #34495e;");
        messageLabel.setAlignment(Pos.CENTER);
        messageLabel.setWrapText(true);
        messageLabel.setPadding(new Insets(15, 10, 15, 10));

        content.getChildren().addAll(warningIcon, headerLabel, separator, messageLabel);

        dialog.getDialogPane().setContent(content);

        dialog.getDialogPane().setHeader(null);
        dialog.getDialogPane().setGraphic(null);


        DropShadow dialogShadow = new DropShadow(15, Color.color(0, 0, 0, 0.3));
        dialog.getDialogPane().setEffect(dialogShadow);
        Optional<ButtonType> result = dialog.showAndWait();
        return result.isPresent() && result.get() == deleteButton;
    }

    public static void showToast(Node parentNode, String message, ToastType type) {
        if (parentNode.getScene() == null) return;
        Pane root = findRootPane(parentNode.getScene());
        if (root == null) return;

        HBox toast = new HBox(15);
        toast.setPadding(new Insets(15, 25, 15, 25));
        toast.setStyle("-fx-background-color: white; -fx-background-radius: 5px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.26), 10, 0.12, -1, 2);");
        toast.setAlignment(Pos.CENTER_LEFT);

        FontAwesomeIcon icon;
        Color color = switch (type) {
            case SUCCESS -> {
                icon = FontAwesomeIcon.CHECK_CIRCLE;
                yield Color.valueOf("#2ecc71");
            }
            case WARNING -> {
                icon = FontAwesomeIcon.EXCLAMATION_TRIANGLE;
                yield Color.valueOf("#f39c12");
            }
            case ERROR -> {
                icon = FontAwesomeIcon.TIMES_CIRCLE;
                yield Color.valueOf("#e74c3c");
            }
            default -> {
                icon = FontAwesomeIcon.INFO_CIRCLE;
                yield Color.valueOf("#3498db");
            }
        };
        FontAwesomeIconView iconView = new FontAwesomeIconView(icon);
        iconView.setGlyphSize(100);
        iconView.setFill(color);

//        Label messageLabel = new Label(message);
//        messageLabel.setStyle("-fx-text-fill: #444; -fx-font-size: 10px;");
//        messageLabel.setWrapText(true);
//        messageLabel.setPrefWidth(400);
//        messageLabel.setMinWidth(Region.USE_COMPUTED_SIZE);
//        HBox.setHgrow(messageLabel, Priority.ALWAYS);
//        messageLabel.setTextOverrun(OverrunStyle.CLIP);

        toast.getChildren().addAll(iconView);

        root.getChildren().add(toast);
        if (root instanceof StackPane) {
            StackPane.setAlignment(toast, Pos.BOTTOM_CENTER);
            StackPane.setMargin(toast, new Insets(0, 0, 30, 0));
        } else {
            toast.setLayoutX((root.getWidth() - toast.getMaxWidth()) / 2);
            toast.setLayoutY(root.getHeight() - 100);
        }

        toast.setOpacity(0);
        toast.setTranslateY(20);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toast);
        fadeIn.setToValue(1);
        fadeIn.play();

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(200), toast);
        slideIn.setToY(0);
        slideIn.play();

        PauseTransition delay = new PauseTransition(Duration.seconds(4));
        delay.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toast);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(evt -> root.getChildren().remove(toast));
            fadeOut.play();

            TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), toast);
            slideOut.setToY(20);
            slideOut.play();
        });
        delay.play();
    }

    private static Pane findRootPane(Scene scene) {
        if (scene.getRoot() instanceof Pane) {
            return (Pane) scene.getRoot();
        }
        return null;
    }

    public enum ToastType {
        INFO, SUCCESS, WARNING, ERROR
    }
}