package com.twadeclark.tailwindtrader;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;

import java.util.Arrays;

public class TextAreaHolder {
    private TextArea textArea;
    private StringBuilder messageBuilder;
    private int maxLength = 1000;

    public TextAreaHolder(TextArea textArea) {
        this.textArea = textArea;
        this.messageBuilder = new StringBuilder();
        if (textArea != null) { textArea.setTooltip(new Tooltip(textArea.getText())); }
    }

    public void appendTrunc(String text) {
        if (textArea == null) { return; }

        Platform.runLater(() -> {
            messageBuilder.append("\n" + text);
            truncateIfNeeded();
            textArea.setText(messageBuilder.toString());
        });
        Platform.runLater(() -> {
            textArea.appendText("\n");
        });
    }

    private void truncateIfNeeded() {
        String[] lines = messageBuilder.toString().split("\n");
        int lineCount = lines.length;
        if (lineCount > maxLength) {
            int startIndex = lineCount - maxLength;
            messageBuilder = new StringBuilder(String.join("\n", Arrays.copyOfRange(lines, startIndex, lineCount)));
        }
    }

}
