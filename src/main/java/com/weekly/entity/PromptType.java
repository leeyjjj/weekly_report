package com.weekly.entity;

public enum PromptType {
    GENERAL("일반 주간보고"),
    TEAM_AGGREGATE("팀 통합보고");

    private final String displayName;

    PromptType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
