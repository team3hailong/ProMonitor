package com.promonitor.view;

import com.promonitor.util.AlertHelper;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javafx.scene.web.WebView;

public class HelpView {
    private BorderPane content;
    private TextFlow textFlow;
    private ProgressIndicator loadingIndicator;
    private boolean isLoading = false;

    private static final String README_URL = "https://raw.githubusercontent.com/team3hailong/ProMonitor/master/README.md";
    private static final String FALLBACK_README =
            """
                    # ProMonitor
                    
                    Ứng dụng theo dõi thời gian sử dụng các phần mềm trên máy tính.
                    
                    ## Tính năng chính
                    
                    - Theo dõi thời gian sử dụng các ứng dụng
                    - Phân loại ứng dụng theo nhóm
                    - Thiết lập giới hạn thời gian sử dụng
                    - Thông báo khi sắp hết thời gian
                    - Báo cáo thống kê sử dụng
                    
                    ## Hướng dẫn sử dụng
                    
                    Xem hướng dẫn đầy đủ tại https://github.com/team3hailong/ProMonitor""";

    public HelpView() {
        createContent();
        loadReadmeContent();
    }

    private void createContent() {
        content = new BorderPane();
        content.setPadding(new Insets(15));
        content.getStyleClass().add("help-view");

        GridPane headerGrid = new GridPane();
        headerGrid.setHgap(10);
        headerGrid.setVgap(2);
        headerGrid.getStyleClass().add("help-header");

        FontAwesomeIconView helpIcon = new FontAwesomeIconView(FontAwesomeIcon.QUESTION_CIRCLE);
        helpIcon.setGlyphSize(30);
        helpIcon.setFill(Color.valueOf("#4a6bff"));
        StackPane iconContainer = new StackPane(helpIcon);
        iconContainer.setMinHeight(40);
        headerGrid.add(iconContainer, 0, 0, 1, 2);

        Label titleLabel = new Label("Hướng dẫn sử dụng");
        titleLabel.getStyleClass().add("view-title");
        headerGrid.add(titleLabel, 1, 0);

        Label descriptionLabel = new Label("Thông tin chi tiết về cách sử dụng ứng dụng ProMonitor");
        descriptionLabel.getStyleClass().add("description-text");
        headerGrid.add(descriptionLabel, 1, 1);

        Button refreshButton = new Button();
        refreshButton.getStyleClass().add("refresh-button");
        FontAwesomeIconView refreshIcon = new FontAwesomeIconView(FontAwesomeIcon.REFRESH);
        refreshIcon.setGlyphSize(14);
        refreshButton.setGraphic(refreshIcon);
        refreshButton.setTooltip(new Tooltip("Tải lại nội dung hướng dẫn"));

        refreshButton.setOnAction(e -> {
            if (!isLoading) {
                loadReadmeContent();
            }
        });

        headerGrid.add(refreshButton, 2, 0, 1, 2);
        GridPane.setHalignment(refreshButton, javafx.geometry.HPos.RIGHT);
        GridPane.setMargin(refreshButton, new Insets(0, 0, 0, 20));

        content.setTop(headerGrid);

        // Create content area for displaying README
        VBox contentBox = new VBox(10);
        contentBox.setPadding(new Insets(10));

        textFlow = new TextFlow();
        textFlow.setLineSpacing(1.5);
        textFlow.prefWidthProperty().bind(contentBox.widthProperty().subtract(20));

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(60, 60);

        StackPane contentArea = new StackPane();
        contentBox.getChildren().add(textFlow);
        contentArea.getChildren().addAll(contentBox, loadingIndicator);
        StackPane.setAlignment(loadingIndicator, Pos.CENTER);

        ScrollPane scrollPane = new ScrollPane(contentArea);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("help-scroll-pane");

        content.setCenter(scrollPane);
    }

    private void loadReadmeContent() {
        isLoading = true;
        loadingIndicator.setVisible(true);
        textFlow.setVisible(false);

        CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(README_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()))) {
                        return reader.lines().collect(Collectors.joining("\n"));
                    }
                } else {
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).thenAcceptAsync(markdownContent -> javafx.application.Platform.runLater(() -> {
            if (markdownContent == null) {
                // Use fallback content if we couldn't load from GitHub
                formatMarkdownToTextFlow(FALLBACK_README, textFlow);

                // Show error notification
                AlertHelper.showToast(
                        content,
                        "Không thể tải nội dung từ GitHub, hiển thị nội dung cơ bản",
                        AlertHelper.ToastType.WARNING
                );
            } else {
                formatMarkdownToTextFlow(markdownContent, textFlow);
            }

            loadingIndicator.setVisible(false);
            textFlow.setVisible(true);
            isLoading = false;
        }));
    }

    private void formatMarkdownToTextFlow(String markdown, TextFlow textFlow) {
        textFlow.getChildren().clear();

        String[] lines = markdown.split("\n");

        boolean inCodeBlock = false;
        boolean inListBlock = false;
        boolean skipSection = false;
        StringBuilder codeBlockContent = new StringBuilder();
        String codeBlockLanguage = "";

        for (String line : lines) {
            if (line.startsWith("## Ảnh chụp màn hình")) {
                skipSection = true;
                continue;
            }

            // Check if we're exiting the screenshot section (next heading)
            if (skipSection && (line.startsWith("# ") || line.startsWith("## ") || line.startsWith("### "))) {
                skipSection = false;
            }

            if (skipSection) {
                continue;
            }

            if (line.startsWith("```")) {
                if (!inCodeBlock) {
                    inCodeBlock = true;
                    codeBlockContent = new StringBuilder();

                    if (line.length() > 3) {
                        codeBlockLanguage = line.substring(3).trim();
                    } else {
                        codeBlockLanguage = "";
                    }
                } else {
                    inCodeBlock = false;

                    VBox codeBlockContainer = new VBox();
                    codeBlockContainer.setSpacing(0);
                    codeBlockContainer.getStyleClass().add("code-block");
                    codeBlockContainer.setStyle("-fx-background-color: #f0f0f0; " +
                            "-fx-border-color: #ddd; " +
                            "-fx-border-width: 1px; " +
                            "-fx-border-radius: 4px; " +
                            "-fx-background-radius: 4px;");

                    HBox codeHeader = new HBox();
                    codeHeader.setAlignment(Pos.CENTER_LEFT);
                    codeHeader.setPadding(new Insets(5, 10, 5, 10));
                    codeHeader.setSpacing(10);
                    codeHeader.setStyle("-fx-background-color: #e0e0e0; " +
                            "-fx-border-width: 0 0 1 0; " +
                            "-fx-border-color: #ddd;");

                    Label languageLabel = new Label(codeBlockLanguage.isEmpty() ? "code" : codeBlockLanguage);
                    languageLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 12px;");

                    Button copyButton = new Button("Copy");
                    copyButton.getStyleClass().add("copy-button");
                    copyButton.setStyle("-fx-background-color: #4a6bff; " +
                            "-fx-text-fill: white; " +
                            "-fx-padding: 2px 8px; " +
                            "-fx-cursor: hand; " +
                            "-fx-background-radius: 3px;");

                    final String codeToCopy = codeBlockContent.toString().trim();

                    copyButton.setOnAction(e -> {
                        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                        content.putString(codeToCopy);
                        javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);


                        copyButton.setText("Copied!");
                        copyButton.setStyle("-fx-background-color: #43a047; " +
                                "-fx-text-fill: white; " +
                                "-fx-padding: 2px 8px; " +
                                "-fx-cursor: hand; " +
                                "-fx-background-radius: 3px;");

                        new Thread(() -> {
                            try {
                                Thread.sleep(2000);
                                javafx.application.Platform.runLater(() -> {
                                    copyButton.setText("Copy");
                                    copyButton.setStyle("-fx-background-color: #4a6bff; " +
                                            "-fx-text-fill: white; " +
                                            "-fx-padding: 2px 8px; " +
                                            "-fx-cursor: hand; " +
                                            "-fx-background-radius: 3px;");
                                });
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }).start();
                    });

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    codeHeader.getChildren().addAll(languageLabel, spacer, copyButton);

                    TextFlow codeFlow = new TextFlow();
                    codeFlow.setPadding(new Insets(10));

                    Text codeText = new Text(codeBlockContent.toString());
                    codeText.setFont(Font.font("Monospaced", 13));
                    codeText.setStyle("-fx-fill: #333;");
                    codeFlow.getChildren().add(codeText);

                    // Add components to the container
                    codeBlockContainer.getChildren().addAll(codeHeader, codeFlow);

                    // Add the code block to the main text flow with spacing
                    textFlow.getChildren().add(new Text("\n"));
                    textFlow.getChildren().add(codeBlockContainer);
                    textFlow.getChildren().add(new Text("\n\n"));
                }
                continue;
            }

            // Add content to code block if we're in one
            if (inCodeBlock) {
                codeBlockContent.append(line).append("\n");
                continue;
            }

            // Handle headers
            if (line.startsWith("# ")) {
                Text header = new Text(line.substring(2) + "\n\n");
                header.setFont(Font.font("System", FontWeight.BOLD, 24));
                header.setFill(Color.valueOf("#4a6bff"));
                textFlow.getChildren().add(header);
            } else if (line.startsWith("## ")) {
                Text header = new Text(line.substring(3) + "\n");
                header.setFont(Font.font("System", FontWeight.BOLD, 20));
                header.setFill(Color.valueOf("#555"));
                textFlow.getChildren().add(header);
            } else if (line.startsWith("### ")) {
                Text header = new Text(line.substring(4) + "\n\n");
                header.setFont(Font.font("System", FontWeight.BOLD, 18));
                header.setFill(Color.valueOf("#666"));
                textFlow.getChildren().add(header);
            }
            // Handle horizontal rule
            else if (line.matches("^-{3,}$") || line.matches("^\\*{3,}$")) {
                Region separator = new Region();
                separator.setMaxWidth(Double.MAX_VALUE);
                separator.setPrefHeight(1);
                separator.setStyle("-fx-background-color: #e0e0e0;");
                textFlow.getChildren().addAll(new Text("\n"), separator, new Text("\n\n"));
            }
            // Handle list items
            else if (line.matches("^- .*") || line.matches("^\\* .*")) {
                // Extract and process the content part
                String listItemContent = line.substring(2);

                HBox listItemContainer = new HBox(5); // 5px spacing between bullet and content

                // Add bullet point
                Text bulletPoint = new Text("• ");
                bulletPoint.setFont(Font.font("System", 14));

                // Create text flow for the content (to handle potential formatting)
                TextFlow itemContentFlow = new TextFlow();

                // Process the content for bold formatting
                processRichTextLine(listItemContent, itemContentFlow);

                // Add components to the container
                listItemContainer.getChildren().addAll(bulletPoint, itemContentFlow);

                // Add padding at the bottom of each list item for better separation
                listItemContainer.setPadding(new Insets(0, 0, 5, 0)); // 5px bottom padding

                // Add the list item to the main text flow
                textFlow.getChildren().add(listItemContainer);
                textFlow.getChildren().add(new Text("\n")); // ensure line break after item

                inListBlock = true;
            } else if (line.matches("^\\d+\\. .*")) {
                // Find the number and the text
                Pattern pattern = Pattern.compile("^(\\d+)\\. (.*)$");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String number = matcher.group(1);
                    String text = matcher.group(2);

                    Text numberText = new Text(number + ". ");
                    numberText.setFont(Font.font("System", FontWeight.BOLD, 14));

                    Text contentText = new Text(text + "\n");
                    contentText.setFont(Font.font("System", 14));

                    textFlow.getChildren().addAll(numberText, contentText);
                    inListBlock = true;
                }
            }
            // Handle empty lines after list
            else if (line.trim().isEmpty() && inListBlock) {
                Text emptyLine = new Text("\n");
                textFlow.getChildren().add(emptyLine);
                inListBlock = false;
            }
            // Handle links - look for [text](url) pattern
            else if (line.contains("[") && line.contains("](") && line.contains(")")) {
                processLineWithLinks(line, textFlow);
            }
            else {
                if (!line.trim().isEmpty()) {
                    Text paragraph = new Text(line + "\n\n");
                    renderParagraphWithStyle(textFlow, line, paragraph);
                } else {
                    if (line.trim().isEmpty()) {
                        Text emptyLine = new Text("\n");
                        textFlow.getChildren().add(emptyLine);

                    } else {
                        processRichTextLine(line + "\n\n", textFlow);
                    }
                }
            }
        }
    }

    private void renderParagraphWithStyle(TextFlow textFlow, String line, Text paragraph) {
        paragraph.setFont(Font.font("System", 14));

        if (line.trim().startsWith(">")) {
            paragraph.setFill(Color.valueOf("#666"));
            paragraph.setFont(Font.font("System", FontWeight.LIGHT, 14));
            paragraph.setStyle("-fx-background-color: #f8f8f8; -fx-padding: 10px;");
        }

        textFlow.getChildren().add(paragraph);
    }

    private void processRichTextLine(String line, TextFlow textFlow) {
        // Process bold formatting (**text** or __text__)
        Pattern boldPattern = Pattern.compile("(\\*\\*|__)(.+?)(\\*\\*|__)");
        Matcher boldMatcher = boldPattern.matcher(line);

        int lastIndex = 0;
        List<Node> lineNodes = new ArrayList<>();

        // Find and process all bold text
        while (boldMatcher.find()) {
            // Add text before the bold part
            if (boldMatcher.start() > lastIndex) {
                Text beforeBold = new Text(line.substring(lastIndex, boldMatcher.start()));
                beforeBold.setFont(Font.font("System", 14));
                lineNodes.add(beforeBold);
            }

            // Add bold text
            String boldText = boldMatcher.group(2);
            Text boldTextNode = new Text(boldText);
            boldTextNode.setFont(Font.font("System", FontWeight.BOLD, 14));
            lineNodes.add(boldTextNode);

            lastIndex = boldMatcher.end();
        }

        // Add remaining text after the last bold part
        if (lastIndex < line.length()) {
            Text afterBold = new Text(line.substring(lastIndex));
            afterBold.setFont(Font.font("System", 14));
            lineNodes.add(afterBold);
        }

        // If no bold text was found, add the whole line as regular text
        if (lineNodes.isEmpty()) {
            Text paragraphText = new Text(line);
            renderParagraphWithStyle(textFlow, line, paragraphText);
        } else {
            textFlow.getChildren().addAll(lineNodes);
        }
    }

    private void processLineWithLinks(String line, TextFlow textFlow) {
        // Find all [text](url) patterns in the line
        Pattern linkPattern = Pattern.compile("\\[(.*?)]\\((.*?)\\)");
        Matcher matcher = linkPattern.matcher(line);

        int lastIndex = 0;
        List<Node> lineNodes = new ArrayList<>();

        // Process each link in the line
        while (matcher.find()) {
            // Add text before the link
            if (matcher.start() > lastIndex) {
                Text beforeLink = new Text(line.substring(lastIndex, matcher.start()));
                beforeLink.setFont(Font.font("System", 14));
                lineNodes.add(beforeLink);
            }

            // Extract link text and URL
            String linkText = matcher.group(1);
            String linkUrl = matcher.group(2);

            // Kiểm tra nếu URL là của YouTube
            if ((linkUrl.contains("youtube.com") || linkUrl.contains("youtu.be"))&&
                    extractYouTubeVideoId(linkUrl) != null && !extractYouTubeVideoId(linkUrl).isEmpty()) {
                // Trích xuất video ID
                String videoId = extractYouTubeVideoId(linkUrl);
                String embedUrl = "https://www.youtube.com/embed/" + videoId;
                WebView webView = new WebView();
                webView.getEngine().load(embedUrl);
                // Cài đặt kích thước của video (16:9)
                webView.setPrefSize(480, 270);
                webView.setMinSize(480, 270);
                webView.setPickOnBounds(true);
                // Thêm khung hiển thị video vào danh sách
                lineNodes.add(webView);
            } else {
                // Nếu không phải link YouTube, tạo Hyperlink như cũ
                Hyperlink link = new Hyperlink(linkText);
                link.setFont(Font.font("System", 14));
                link.setPadding(new Insets(0));
                link.setStyle("-fx-border-color: transparent; -fx-underline: true;");
                link.setOnAction(e -> {
                    try {
                        Desktop.getDesktop().browse(new URI(linkUrl));
                        link.setStyle("-fx-text-fill: blue;");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                lineNodes.add(link);
            }

            lastIndex = matcher.end();
        }

        // Thêm phần text sau cùng
        if (lastIndex < line.length()) {
            Text afterLinks = new Text(line.substring(lastIndex) + "\n\n");
            afterLinks.setFont(Font.font("System", 14));
            lineNodes.add(afterLinks);
        } else {
            lineNodes.add(new Text("\n\n"));
        }

        textFlow.getChildren().addAll(lineNodes);
    }

    // Phương thức phụ trợ để trích xuất video ID từ URL YouTube
    private String extractYouTubeVideoId(String url) {
        String videoId = "";
        try {
            if (url.contains("watch?v=")) {
                // VD: https://www.youtube.com/watch?v=VIDEO_ID
                int index = url.indexOf("watch?v=") + 8;
                int ampIndex = url.indexOf('&', index);
                if (ampIndex == -1) {
                    videoId = url.substring(index);
                } else {
                    videoId = url.substring(index, ampIndex);
                }
            } else if (url.contains("youtu.be/")) {
                // VD: https://youtu.be/VIDEO_ID
                int index = url.indexOf("youtu.be/") + 9;
                int endIndex = url.indexOf('?', index);
                if (endIndex == -1) {
                    videoId = url.substring(index);
                } else {
                    videoId = url.substring(index, endIndex);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return videoId;
    }

    public Node getContent() {
        return content;
    }
}