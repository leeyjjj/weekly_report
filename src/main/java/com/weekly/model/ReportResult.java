package com.weekly.model;

public record ReportResult(
        String generalReport,
        boolean teamsSent,
        String teamsError,
        String savedFilePath
) {}
