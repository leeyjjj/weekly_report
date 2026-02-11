package com.weekly.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdaptiveCardBuilder {

    public static Map<String, Object> build(String generalReport) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        List<Map<String, Object>> bodyElements = new ArrayList<>();

        // Header
        bodyElements.add(textBlock("Weekly Report - " + dateStr, "large", "bolder", null));

        // General Report
        bodyElements.add(textBlock("주간보고", "medium", "bolder", "accent", true));
        bodyElements.add(textBlock(generalReport, null, null, null));

        // Assemble adaptive card
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("$schema", "http://adaptivecards.io/schemas/adaptive-card.json");
        card.put("type", "AdaptiveCard");
        card.put("version", "1.4");
        card.put("body", bodyElements);

        Map<String, Object> attachment = new LinkedHashMap<>();
        attachment.put("contentType", "application/vnd.microsoft.card.adaptive");
        attachment.put("content", card);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "message");
        payload.put("attachments", List.of(attachment));

        return payload;
    }

    public static Map<String, Object> buildReservation(String roomName, String title,
                                                          String time, String userName, String attendees) {
        List<Map<String, Object>> bodyElements = new ArrayList<>();

        bodyElements.add(textBlock("회의실 예약 알림", "large", "bolder", null));
        bodyElements.add(textBlock("회의실: " + roomName, "medium", "bolder", "accent", true));
        bodyElements.add(textBlock("제목: " + title, null, null, null));
        bodyElements.add(textBlock("시간: " + time, null, null, null));
        bodyElements.add(textBlock("예약자: " + userName, null, null, null));
        if (attendees != null && !attendees.isBlank()) {
            bodyElements.add(textBlock("참석자: " + attendees, null, null, null));
        }

        Map<String, Object> card = new LinkedHashMap<>();
        card.put("$schema", "http://adaptivecards.io/schemas/adaptive-card.json");
        card.put("type", "AdaptiveCard");
        card.put("version", "1.4");
        card.put("body", bodyElements);

        Map<String, Object> attachment = new LinkedHashMap<>();
        attachment.put("contentType", "application/vnd.microsoft.card.adaptive");
        attachment.put("content", card);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "message");
        payload.put("attachments", List.of(attachment));

        return payload;
    }

    private static Map<String, Object> textBlock(String text, String size, String weight, String color) {
        return textBlock(text, size, weight, color, false);
    }

    private static Map<String, Object> textBlock(String text, String size, String weight, String color, boolean separator) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("type", "TextBlock");
        block.put("text", text);
        block.put("wrap", true);
        if (size != null) block.put("size", size);
        if (weight != null) block.put("weight", weight);
        if (color != null) block.put("color", color);
        if (separator) block.put("separator", true);
        return block;
    }
}
