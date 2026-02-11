package com.weekly.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class ReportRequest {

    private Long userId;

    private String userName;

    private Long teamId;

    private String newTeamName;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate periodStart;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate periodEnd;

    private Long generalTemplateId;

    @NotBlank(message = "업무 내용을 입력해주세요")
    @Size(min = 10, message = "업무 내용을 10자 이상 입력해주세요")
    private String rawText;

    public ReportRequest() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public String getNewTeamName() { return newTeamName; }
    public void setNewTeamName(String newTeamName) { this.newTeamName = newTeamName; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public Long getGeneralTemplateId() { return generalTemplateId; }
    public void setGeneralTemplateId(Long generalTemplateId) { this.generalTemplateId = generalTemplateId; }

}
