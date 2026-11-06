package de.exit.jdmx.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Moderner nativer JavaFX Markdown-Renderer im Studio-Dark-Design.
 * <p>
 * Wandelt Markdown-Texte (wie GitHub Release Notes oder Changelogs) direkt in
 * strukturierte, ergonomische JavaFX-Nodes um (Überschriften, Listen, Code-Blöcke,
 * Inline-Code, Fettschrift und anklickbare Hyperlinks) — vollkommen ohne schwere
 * WebKit/Browser-Engines.
 * </p>
 */
public class MarkdownView extends ScrollPane {

    private static final Logger LOGGER = Logger.getLogger(MarkdownView.class.getName());

    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(([^\\)]+)\\)");
    private static final Pattern INLINE_PATTERN = Pattern.compile("(\\*\\*.*?\\*\\*|`.*?`|\\[.*?\\]\\(.*?\\))");

    private final VBox contentBox;

    public MarkdownView() {
        this("");
    }

    public MarkdownView(String markdown) {
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

        getStyleClass().add("markdown-view");
        setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        this.contentBox = new VBox(6);
        contentBox.getStyleClass().add("markdown-content");
        contentBox.setPadding(new Insets(10, 12, 10, 12));
        contentBox.setStyle("-fx-background-color: #121214; -fx-border-color: #27272A; " +
                "-fx-border-width: 1px; -fx-border-radius: 6px; -fx-background-radius: 6px;");

        setContent(contentBox);

        if (markdown != null && !markdown.isBlank()) {
            setMarkdown(markdown);
        }
    }

    /**
     * Parst den übergebenen Markdown-String und befüllt die Ansicht mit formatierten JavaFX-Nodes.
     *
     * @param markdown Roher Markdown-Text
     */
    public void setMarkdown(String markdown) {
        contentBox.getChildren().clear();
        if (markdown == null || markdown.isBlank()) {
            return;
        }

        String[] lines = markdown.lines().toArray(String[]::new);
        boolean inCodeBlock = false;
        StringBuilder codeBuffer = new StringBuilder();
        String codeLanguage = "";

        for (String rawLine : lines) {
            String line = rawLine.stripTrailing();

            // 1. Mehrzeilige Codeblöcke (``` ... ```)
            if (line.trim().startsWith("```")) {
                if (inCodeBlock) {
                    // Codeblock abschließen
                    contentBox.getChildren().add(createCodeBlock(codeBuffer.toString().stripTrailing()));
                    codeBuffer.setLength(0);
                    inCodeBlock = false;
                } else {
                    inCodeBlock = true;
                    codeLanguage = line.trim().substring(3).trim();
                }
                continue;
            }

            if (inCodeBlock) {
                codeBuffer.append(rawLine).append("\n");
                continue;
            }

            // 2. Leere Zeilen überspringen (bzw. kleiner Abstand)
            if (line.trim().isEmpty()) {
                Region spacer = new Region();
                spacer.setMinHeight(4);
                spacer.setPrefHeight(4);
                contentBox.getChildren().add(spacer);
                continue;
            }

            // 3. Trennlinien (--- oder ***)
            if (line.trim().equals("---") || line.trim().equals("***")) {
                contentBox.getChildren().add(createDivider());
                continue;
            }

            // 4. Überschriften (#, ##, ###)
            if (line.startsWith("# ")) {
                contentBox.getChildren().add(createHeading(line.substring(2).trim(), 1));
                continue;
            }
            if (line.startsWith("## ")) {
                contentBox.getChildren().add(createHeading(line.substring(3).trim(), 2));
                continue;
            }
            if (line.startsWith("### ")) {
                contentBox.getChildren().add(createHeading(line.substring(4).trim(), 3));
                continue;
            }
            if (line.startsWith("#### ")) {
                contentBox.getChildren().add(createHeading(line.substring(5).trim(), 4));
                continue;
            }

            // 5. Blockquotes (> Text)
            if (line.startsWith("> ")) {
                contentBox.getChildren().add(createBlockquote(line.substring(2).trim()));
                continue;
            }

            // 6. Listenpunkte (- oder *)
            if (line.trim().startsWith("- ") || line.trim().startsWith("* ")) {
                String itemText = line.trim().substring(2).trim();
                contentBox.getChildren().add(createListItem(itemText));
                continue;
            }

            // 7. Nummerierte Listen (1. , 2. )
            if (line.trim().matches("^\\d+\\.\\s+.*")) {
                int dotIndex = line.indexOf('.');
                String num = line.substring(0, dotIndex + 1).trim();
                String itemText = line.substring(dotIndex + 1).trim();
                contentBox.getChildren().add(createNumberedListItem(num, itemText));
                continue;
            }

            // 8. Normaler Fließtext-Absatz
            contentBox.getChildren().add(createParagraph(line));
        }

        // Falls offener Codeblock am Dateiende
        if (inCodeBlock && !codeBuffer.isEmpty()) {
            contentBox.getChildren().add(createCodeBlock(codeBuffer.toString().stripTrailing()));
        }
    }

    private Node createHeading(String text, int level) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("markdown-h" + level);
        lbl.setWrapText(true);

        switch (level) {
            case 1 -> lbl.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #F3F4F6; -fx-padding: 6 0 2 0;");
            case 2 -> lbl.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #38BDF8; -fx-padding: 5 0 2 0;");
            case 3 -> lbl.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 600; -fx-text-fill: #E5E7EB; -fx-padding: 4 0 1 0;");
            default -> lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #9CA3AF; -fx-padding: 2 0 1 0;");
        }
        return lbl;
    }

    private Node createDivider() {
        Region line = new Region();
        line.getStyleClass().add("markdown-divider");
        line.setMinHeight(1);
        line.setPrefHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: #27272A;");
        VBox.setMargin(line, new Insets(4, 0, 4, 0));
        return line;
    }

    private Node createCodeBlock(String code) {
        VBox box = new VBox();
        box.getStyleClass().add("markdown-code-block");
        box.setPadding(new Insets(8, 12, 8, 12));
        box.setStyle("-fx-background-color: #0A0A0C; -fx-border-color: #27272A; " +
                "-fx-border-width: 1px; -fx-border-radius: 4px; -fx-background-radius: 4px;");

        Label lbl = new Label(code);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-font-family: 'Consolas', 'JetBrains Mono', 'Courier New', monospace; " +
                "-fx-font-size: 11px; -fx-text-fill: #38BDF8;");
        box.getChildren().add(lbl);
        return box;
    }

    private Node createBlockquote(String quote) {
        HBox box = new HBox(8);
        box.getStyleClass().add("markdown-quote");
        box.setPadding(new Insets(6, 10, 6, 10));
        box.setStyle("-fx-background-color: #161922; -fx-border-color: #38BDF8; -fx-border-width: 0 0 0 3px; " +
                "-fx-background-radius: 2px;");

        TextFlow flow = new TextFlow();
        parseInlineMarkdown(quote, flow);
        box.getChildren().add(flow);
        return box;
    }

    private Node createListItem(String text) {
        HBox itemBox = new HBox(8);
        itemBox.setAlignment(Pos.TOP_LEFT);
        itemBox.setPadding(new Insets(1, 0, 1, 8));

        Label bullet = new Label("•");
        bullet.setStyle("-fx-text-fill: #38BDF8; -fx-font-size: 13px; -fx-font-weight: bold;");

        TextFlow flow = new TextFlow();
        HBox.setHgrow(flow, Priority.ALWAYS);
        parseInlineMarkdown(text, flow);

        itemBox.getChildren().addAll(bullet, flow);
        return itemBox;
    }

    private Node createNumberedListItem(String number, String text) {
        HBox itemBox = new HBox(6);
        itemBox.setAlignment(Pos.TOP_LEFT);
        itemBox.setPadding(new Insets(1, 0, 1, 8));

        Label numLabel = new Label(number);
        numLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-min-width: 18px;");

        TextFlow flow = new TextFlow();
        HBox.setHgrow(flow, Priority.ALWAYS);
        parseInlineMarkdown(text, flow);

        itemBox.getChildren().addAll(numLabel, flow);
        return itemBox;
    }

    private Node createParagraph(String text) {
        TextFlow flow = new TextFlow();
        flow.getStyleClass().add("markdown-paragraph");
        parseInlineMarkdown(text, flow);
        return flow;
    }

    /**
     * Parst Inline-Elemente wie **fett**, `code` und [link](url).
     */
    private void parseInlineMarkdown(String input, TextFlow flow) {
        if (input == null || input.isEmpty()) {
            return;
        }

        Matcher matcher = INLINE_PATTERN.matcher(input);
        int lastIndex = 0;

        while (matcher.find()) {
            if (matcher.start() > lastIndex) {
                String plain = input.substring(lastIndex, matcher.start());
                flow.getChildren().add(createPlainText(plain));
            }

            String match = matcher.group();
            if (match.startsWith("**") && match.endsWith("**") && match.length() >= 4) {
                String boldText = match.substring(2, match.length() - 2);
                flow.getChildren().add(createBoldText(boldText));
            } else if (match.startsWith("`") && match.endsWith("`") && match.length() >= 2) {
                String codeText = match.substring(1, match.length() - 1);
                flow.getChildren().add(createInlineCodeText(codeText));
            } else if (match.startsWith("[") && match.contains("](") && match.endsWith(")")) {
                Matcher linkMatch = LINK_PATTERN.matcher(match);
                if (linkMatch.matches()) {
                    String title = linkMatch.group(1);
                    String url = linkMatch.group(2);
                    flow.getChildren().add(createLinkText(title, url));
                } else {
                    flow.getChildren().add(createPlainText(match));
                }
            } else {
                flow.getChildren().add(createPlainText(match));
            }

            lastIndex = matcher.end();
        }

        if (lastIndex < input.length()) {
            flow.getChildren().add(createPlainText(input.substring(lastIndex)));
        }
    }

    private Text createPlainText(String text) {
        Text t = new Text(text);
        t.setStyle("-fx-fill: #D1D5DB; -fx-font-size: 11.5px;");
        return t;
    }

    private Text createBoldText(String text) {
        Text t = new Text(text);
        t.setStyle("-fx-fill: #FFFFFF; -fx-font-size: 11.5px; -fx-font-weight: bold;");
        return t;
    }

    private Text createInlineCodeText(String text) {
        Text t = new Text(" " + text + " ");
        t.setStyle("-fx-fill: #38BDF8; -fx-font-family: 'Consolas', 'JetBrains Mono', 'Courier New', monospace; " +
                "-fx-font-size: 11px; -fx-font-weight: bold;");
        return t;
    }

    private Text createLinkText(String title, String url) {
        Text t = new Text(title);
        t.setStyle("-fx-fill: #38BDF8; -fx-underline: true; -fx-font-size: 11.5px;");
        t.setCursor(Cursor.HAND);
        t.setOnMouseClicked(e -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                } else {
                    new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Konnte URL nicht öffnen: " + url, ex);
            }
        });
        return t;
    }

    public VBox getContentBox() {
        return contentBox;
    }
}
