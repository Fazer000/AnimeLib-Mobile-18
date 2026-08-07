package com.example.animelib.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommentDocBuilder {

    private static final Pattern SPOILER_PATTERN = Pattern.compile(
            "\\[spoiler(?:=([^\\]]+))?\\]([\\s\\S]*?)\\[/spoiler\\]|\\|\\|([\\s\\S]*?)\\|\\|",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern FORMAT_PATTERN = Pattern.compile(
            "\\[b\\]([\\s\\S]*?)\\[/b\\]|\\*\\*([\\s\\S]*?)\\*\\*|" +
            "\\[i\\]([\\s\\S]*?)\\[/i\\]|\\*([\\s\\S]*?)\\*|" +
            "\\[u\\]([\\s\\S]*?)\\[/u\\]|" +
            "\\[s\\]([\\s\\S]*?)\\[/s\\]|~~([\\s\\S]*?)~~",
            Pattern.CASE_INSENSITIVE
    );

    public static JsonObject buildDoc(String rawText) {
        JsonObject doc = new JsonObject();
        doc.addProperty("type", "doc");

        JsonArray contentArray = new JsonArray();

        if (rawText == null || rawText.trim().isEmpty()) {
            JsonObject paragraph = new JsonObject();
            paragraph.addProperty("type", "paragraph");
            paragraph.add("content", new JsonArray());
            contentArray.add(paragraph);
            doc.add("content", contentArray);
            return doc;
        }

        String normalized = rawText.replace("\r\n", "\n");
        String[] lines = normalized.split("\n");

        for (String line : lines) {
            String trimmedLine = line.trim();

            if (trimmedLine.startsWith("[spoilerblock") && trimmedLine.endsWith("[/spoilerblock]")) {
                String visibleText = "спойлер";
                String innerContent = "";
                int firstClose = trimmedLine.indexOf(']');
                if (firstClose > 0) {
                    String tagHeader = trimmedLine.substring(1, firstClose);
                    if (tagHeader.startsWith("spoilerblock=")) {
                        visibleText = tagHeader.substring(13).trim();
                        if (visibleText.isEmpty()) visibleText = "спойлер";
                    }
                    innerContent = trimmedLine.substring(firstClose + 1, trimmedLine.length() - 15);
                }

                JsonObject spoilerBlock = new JsonObject();
                spoilerBlock.addProperty("type", "spoilerBlock");

                JsonObject attrs = new JsonObject();
                attrs.addProperty("visibleText", visibleText);
                spoilerBlock.add("attrs", attrs);

                JsonArray sbContent = new JsonArray();
                JsonObject paragraph = new JsonObject();
                paragraph.addProperty("type", "paragraph");
                paragraph.add("content", parseLineContent(innerContent));
                sbContent.add(paragraph);

                spoilerBlock.add("content", sbContent);
                contentArray.add(spoilerBlock);
            } else if (trimmedLine.startsWith("[quote]") && trimmedLine.endsWith("[/quote]")) {
                String quoteText = trimmedLine.substring(7, trimmedLine.length() - 8);
                JsonObject blockquote = new JsonObject();
                blockquote.addProperty("type", "blockquote");

                JsonArray bqContent = new JsonArray();
                JsonObject paragraph = new JsonObject();
                paragraph.addProperty("type", "paragraph");
                paragraph.add("content", parseLineContent(quoteText));
                bqContent.add(paragraph);

                blockquote.add("content", bqContent);
                contentArray.add(blockquote);
            } else if (trimmedLine.startsWith("> ")) {
                String quoteText = trimmedLine.substring(2);
                JsonObject blockquote = new JsonObject();
                blockquote.addProperty("type", "blockquote");

                JsonArray bqContent = new JsonArray();
                JsonObject paragraph = new JsonObject();
                paragraph.addProperty("type", "paragraph");
                paragraph.add("content", parseLineContent(quoteText));
                bqContent.add(paragraph);

                blockquote.add("content", bqContent);
                contentArray.add(blockquote);
            } else {
                JsonObject paragraph = new JsonObject();
                paragraph.addProperty("type", "paragraph");
                paragraph.add("content", parseLineContent(line));
                contentArray.add(paragraph);
            }
        }

        doc.add("content", contentArray);
        return doc;
    }

    private static JsonArray parseLineContent(String line) {
        JsonArray nodes = new JsonArray();
        if (line == null || line.isEmpty()) {
            return nodes;
        }

        Matcher matcher = SPOILER_PATTERN.matcher(line);
        int lastIndex = 0;

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            if (start > lastIndex) {
                String textBefore = line.substring(lastIndex, start);
                parseFormattedText(textBefore, nodes);
            }

            String visibleText = "спойлер";
            String hiddenText = "";

            if (matcher.group(1) != null) {
                visibleText = matcher.group(1);
                hiddenText = matcher.group(2) != null ? matcher.group(2) : "";
            } else if (matcher.group(2) != null) {
                hiddenText = matcher.group(2);
            } else if (matcher.group(3) != null) {
                hiddenText = matcher.group(3);
            }

            JsonObject spoilerNode = new JsonObject();
            spoilerNode.addProperty("type", "spoilerInline");

            JsonObject attrs = new JsonObject();
            attrs.addProperty("visibleText", (visibleText != null && !visibleText.trim().isEmpty()) ? visibleText : "спойлер");
            spoilerNode.add("attrs", attrs);

            JsonArray spoilerContent = new JsonArray();
            parseFormattedText(hiddenText, spoilerContent);
            if (spoilerContent.size() == 0) {
                JsonObject defaultTextNode = createTextNode("спойлер", null);
                spoilerContent.add(defaultTextNode);
            }
            spoilerNode.add("content", spoilerContent);

            nodes.add(spoilerNode);

            lastIndex = end;
        }

        if (lastIndex < line.length()) {
            String textAfter = line.substring(lastIndex);
            parseFormattedText(textAfter, nodes);
        }

        return nodes;
    }

    private static void parseFormattedText(String text, JsonArray targetArray) {
        if (text == null || text.isEmpty()) return;

        Matcher matcher = FORMAT_PATTERN.matcher(text);
        int lastIndex = 0;

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            if (start > lastIndex) {
                String before = text.substring(lastIndex, start);
                addTextNodeIfNotEmpty(before, null, targetArray);
            }

            String matchedContent = "";
            List<String> marks = new ArrayList<>();

            if (matcher.group(1) != null) {
                matchedContent = matcher.group(1);
                marks.add("bold");
            } else if (matcher.group(2) != null) {
                matchedContent = matcher.group(2);
                marks.add("bold");
            } else if (matcher.group(3) != null) {
                matchedContent = matcher.group(3);
                marks.add("italic");
            } else if (matcher.group(4) != null) {
                matchedContent = matcher.group(4);
                marks.add("italic");
            } else if (matcher.group(5) != null) {
                matchedContent = matcher.group(5);
                marks.add("underline");
            } else if (matcher.group(6) != null) {
                matchedContent = matcher.group(6);
                marks.add("strike");
            } else if (matcher.group(7) != null) {
                matchedContent = matcher.group(7);
                marks.add("strike");
            }

            addTextNodeIfNotEmpty(matchedContent, marks, targetArray);
            lastIndex = end;
        }

        if (lastIndex < text.length()) {
            String after = text.substring(lastIndex);
            addTextNodeIfNotEmpty(after, null, targetArray);
        }
    }

    private static void addTextNodeIfNotEmpty(String text, List<String> marks, JsonArray targetArray) {
        if (text == null || text.isEmpty()) return;
        JsonObject textNode = createTextNode(text, marks);
        targetArray.add(textNode);
    }

    private static JsonObject createTextNode(String text, List<String> marks) {
        JsonObject textNode = new JsonObject();
        textNode.addProperty("type", "text");
        textNode.addProperty("text", text);

        if (marks != null && !marks.isEmpty()) {
            JsonArray marksArray = new JsonArray();
            for (String markType : marks) {
                JsonObject markObj = new JsonObject();
                markObj.addProperty("type", markType);
                marksArray.add(markObj);
            }
            textNode.add("marks", marksArray);
        }

        return textNode;
    }
}
